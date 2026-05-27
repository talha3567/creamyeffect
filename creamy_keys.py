import os
import random
import json
import threading
import sys
import tkinter as tk
from tkinter import ttk, messagebox

# Try to import external dependencies
try:
    import pygame.mixer
    from pynput import keyboard, mouse
    import pystray
    from pystray import MenuItem as item
    from PIL import Image, ImageDraw
except ImportError as e:
    import sys
    # Basic check for tkinter since we need it for the error box
    try:
        import tkinter as tk
        from tkinter import messagebox
        root = tk.Tk()
        root.withdraw()
        messagebox.showerror("Hata / Error",
            f"Gerekli kütüphaneler eksik!\nLütfen şu komutu çalıştırın:\n\n"
            f"pip install pynput pygame pystray Pillow\n\n"
            f"Detay: {e}")
    except ImportError:
        print(f"Hata: Gerekli kütüphaneler eksik! Lütfen 'pip install pynput pygame pystray Pillow' komutunu calistirin. Detay: {e}")
    sys.exit(1)

# Constants
CONFIG_FILE = "config.json"
SOUNDS_DIR = "sounds"

def resource_path(relative_path):
    """ Get absolute path to resource, works for dev and for PyInstaller """
    try:
        base_path = sys._MEIPASS
    except Exception:
        base_path = os.path.abspath(".")
    return os.path.join(base_path, relative_path)

class CreamyKeysApp:
    def __init__(self):
        self.load_config()
        self.init_audio()
        self.load_sound_packs()

        # Tracking pressed keys to prevent spam (repeat suppression)
        self.pressed_keys = set()

        self.start_listeners()
        self.setup_gui()
        self.setup_tray()

    def load_config(self):
        self.config = {
            "kb_enabled": True,
            "mouse_enabled": True,
            "keyboard_pack": "cherrymx_black_pbt",
            "mouse_pack": "cherrymx_blue_abs_2",
            "volume": 0.5
        }
        self.config_path = os.path.join(os.path.dirname(os.path.abspath(sys.argv[0])), CONFIG_FILE)
        if os.path.exists(self.config_path):
            try:
                with open(self.config_path, "r") as f:
                    self.config.update(json.load(f))
            except Exception as e:
                print(f"Error loading config: {e}")

    def save_config(self):
        try:
            with open(self.config_path, "w") as f:
                json.dump(self.config, f, indent=4)
        except Exception as e:
            print(f"Error saving config: {e}")

    def init_audio(self):
        pygame.mixer.init(frequency=44100, size=-16, channels=2, buffer=512)
        pygame.mixer.set_num_channels(32)

    def load_sound_packs(self):
        self.packs = {}
        base_sounds_dir = resource_path(SOUNDS_DIR)
        if not os.path.exists(base_sounds_dir):
            print(f"Warning: {base_sounds_dir} directory not found.")
            return

        for pack_name in os.listdir(base_sounds_dir):
            pack_path = os.path.join(base_sounds_dir, pack_name)
            if os.path.isdir(pack_path):
                sounds = []
                for file in os.listdir(pack_path):
                    if file.endswith(".ogg"):
                        try:
                            sound_obj = pygame.mixer.Sound(os.path.join(pack_path, file))
                            sounds.append(sound_obj)
                        except Exception as e:
                            print(f"Error loading sound {file}: {e}")
                if sounds:
                    self.packs[pack_name] = sounds

        print(f"Loaded {len(self.packs)} sound packs into memory.")

    def play_sound(self, pack_type="keyboard"):
        if pack_type == "keyboard" and not self.config["kb_enabled"]:
            return
        if pack_type == "mouse" and not self.config["mouse_enabled"]:
            return

        pack_name = self.config["keyboard_pack"] if pack_type == "keyboard" else self.config["mouse_pack"]
        if pack_name in self.packs:
            sound = random.choice(self.packs[pack_name])
            try:
                sound.set_volume(self.config["volume"])
                sound.play()
            except Exception as e:
                print(f"Error playing sound: {e}")

    def on_press(self, key):
        # Key suppression logic: only play if the key isn't already marked as pressed
        if key not in self.pressed_keys:
            self.pressed_keys.add(key)
            self.play_sound("keyboard")

    def on_release(self, key):
        if key in self.pressed_keys:
            self.pressed_keys.remove(key)

    def on_click(self, x, y, button, pressed):
        if pressed:
            self.play_sound("mouse")

    def start_listeners(self):
        self.key_listener = keyboard.Listener(on_press=self.on_press, on_release=self.on_release)
        self.mouse_listener = mouse.Listener(on_click=self.on_click)
        self.key_listener.daemon = True
        self.mouse_listener.daemon = True
        self.key_listener.start()
        self.mouse_listener.start()

    def setup_gui(self):
        self.root = tk.Tk()
        self.root.title("CreamyKeys")
        self.root.geometry("400x420")

        # Minimize to tray instead of closing
        self.root.protocol('WM_DELETE_WINDOW', self.hide_window)

        main_frame = ttk.Frame(self.root, padding="10")
        main_frame.pack(fill=tk.BOTH, expand=True)

        # Granular Sound Controls
        self.kb_enabled_var = tk.BooleanVar(value=self.config["kb_enabled"])
        ttk.Checkbutton(main_frame, text="Keyboard Sounds (Klavye Sesi)", variable=self.kb_enabled_var, command=self.update_granular_enabled).pack(pady=5)

        self.mouse_enabled_var = tk.BooleanVar(value=self.config["mouse_enabled"])
        ttk.Checkbutton(main_frame, text="Mouse Sounds (Fare Sesi)", variable=self.mouse_enabled_var, command=self.update_granular_enabled).pack(pady=5)

        # Volume Slider
        ttk.Label(main_frame, text="Volume (Ses Seviyesi)").pack(pady=2)
        self.volume_var = tk.DoubleVar(value=self.config["volume"])
        self.volume_scale = ttk.Scale(main_frame, from_=0.0, to=1.0, variable=self.volume_var, orient=tk.HORIZONTAL, command=self.update_volume)
        self.volume_scale.pack(fill=tk.X, pady=5)
        self.volume_scale.bind("<ButtonRelease-1>", lambda e: self.save_config())

        # Keyboard Pack Dropdown
        ttk.Label(main_frame, text="Keyboard Sound Pack (Klavye Paketi)").pack(pady=2)
        self.kb_pack_var = tk.StringVar(value=self.config["keyboard_pack"])
        self.kb_dropdown = ttk.Combobox(main_frame, textvariable=self.kb_pack_var, values=list(self.packs.keys()), state="readonly")
        self.kb_dropdown.pack(fill=tk.X, pady=5)
        self.kb_dropdown.bind("<<ComboboxSelected>>", self.update_kb_pack)

        # Mouse Pack Dropdown
        ttk.Label(main_frame, text="Mouse Sound Pack (Fare Paketi)").pack(pady=2)
        self.mouse_pack_var = tk.StringVar(value=self.config["mouse_pack"])
        self.mouse_dropdown = ttk.Combobox(main_frame, textvariable=self.mouse_pack_var, values=list(self.packs.keys()), state="readonly")
        self.mouse_dropdown.pack(fill=tk.X, pady=5)
        self.mouse_dropdown.bind("<<ComboboxSelected>>", self.update_mouse_pack)

        # Test Buttons
        test_frame = ttk.Frame(main_frame)
        test_frame.pack(fill=tk.X, pady=10)
        ttk.Button(test_frame, text="Test Keyboard", command=lambda: self.play_sound("keyboard")).pack(side=tk.LEFT, padx=5, expand=True)
        ttk.Button(test_frame, text="Test Mouse", command=lambda: self.play_sound("mouse")).pack(side=tk.LEFT, padx=5, expand=True)

        ttk.Button(main_frame, text="Hide to Tray (Arka Plana At)", command=self.hide_window).pack(pady=10)
        ttk.Button(main_frame, text="Exit Completely (Tamamen Kapat)", command=self.exit_app).pack(pady=5)

    def create_image(self):
        # Generate a simple icon for the tray
        image = Image.new('RGB', (64, 64), color='white')
        dc = ImageDraw.Draw(image)
        dc.rectangle((16, 16, 48, 48), fill='blue')
        return image

    def setup_tray(self):
        menu = (item('Show (Goster)', self.show_window), item('Exit (Cikis)', self.exit_app))
        self.tray_icon = pystray.Icon("creamy_keys", self.create_image(), "CreamyKeys", menu)
        # Run tray in a separate thread
        self.tray_thread = threading.Thread(target=self.tray_icon.run)
        self.tray_thread.daemon = True
        self.tray_thread.start()

    def hide_window(self):
        self.root.withdraw()

    def show_window(self):
        self.root.after(0, self.root.deiconify)

    def update_granular_enabled(self):
        self.config["kb_enabled"] = self.kb_enabled_var.get()
        self.config["mouse_enabled"] = self.mouse_enabled_var.get()
        self.save_config()

    def update_volume(self, val):
        self.config["volume"] = float(val)

    def update_kb_pack(self, event):
        self.config["keyboard_pack"] = self.kb_pack_var.get()
        self.save_config()

    def update_mouse_pack(self, event):
        self.config["mouse_pack"] = self.mouse_pack_var.get()
        self.save_config()

    def exit_app(self):
        self.save_config()
        if hasattr(self, 'tray_icon'):
            self.tray_icon.stop()
        self.root.destroy()
        os._exit(0)

    def run(self):
        self.root.mainloop()

if __name__ == "__main__":
    try:
        app = CreamyKeysApp()
        if len(sys.argv) > 1 and sys.argv[1] == "--test":
            print("Testing initialization...")
            import time
            time.sleep(1)
            print("Test complete.")
        else:
            app.run()
    except Exception as e:
        import traceback
        error_msg = traceback.format_exc()
        try:
            root = tk.Tk()
            root.withdraw()
            messagebox.showerror("Hata / Error", f"Uygulama başlatılırken bir hata oluştu:\n\n{error_msg}")
        except:
            print(f"Hata: {error_msg}")
        sys.exit(1)

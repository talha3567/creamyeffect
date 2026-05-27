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
except ImportError as e:
    root = tk.Tk()
    root.withdraw()
    messagebox.showerror("Hata / Error",
        f"Gerekli kütüphaneler eksik!\nLütfen şu komutu çalıştırın:\n\n"
        f"pip install pynput pygame\n\n"
        f"Detay: {e}")
    sys.exit(1)

# Constants
CONFIG_FILE = "config.json"
SOUNDS_DIR = "sounds"

def resource_path(relative_path):
    """ Get absolute path to resource, works for dev and for PyInstaller """
    try:
        # PyInstaller creates a temp folder and stores path in _MEIPASS
        base_path = sys._MEIPASS
    except Exception:
        base_path = os.path.abspath(".")
    return os.path.join(base_path, relative_path)

class CreamyKeysApp:
    def __init__(self):
        self.load_config()
        self.init_audio()
        self.load_sound_packs()
        self.start_listeners()
        self.setup_gui()

    def load_config(self):
        self.config = {
            "enabled": True,
            "keyboard_pack": "cherrymx_black_pbt",
            "mouse_pack": "cherrymx_blue_abs_2",
            "volume": 0.5
        }
        # Look for config next to the executable/script, not in _MEIPASS
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
        # Use resource_path to find sounds directory
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
                            # PRE-LOAD sounds into memory
                            sound_obj = pygame.mixer.Sound(os.path.join(pack_path, file))
                            sounds.append(sound_obj)
                        except Exception as e:
                            print(f"Error loading sound {file}: {e}")
                if sounds:
                    self.packs[pack_name] = sounds

        print(f"Loaded {len(self.packs)} sound packs into memory.")

    def play_sound(self, pack_type="keyboard"):
        if not self.config["enabled"]:
            return

        pack_name = self.config["keyboard_pack"] if pack_type == "keyboard" else self.config["mouse_pack"]
        if pack_name in self.packs:
            # Randomly pick a PRE-LOADED sound object
            sound = random.choice(self.packs[pack_name])
            try:
                sound.set_volume(self.config["volume"])
                sound.play()
            except Exception as e:
                print(f"Error playing sound: {e}")

    def on_press(self, key):
        self.play_sound("keyboard")

    def on_click(self, x, y, button, pressed):
        if pressed:
            self.play_sound("mouse")

    def start_listeners(self):
        self.key_listener = keyboard.Listener(on_press=self.on_press)
        self.mouse_listener = mouse.Listener(on_click=self.on_click)
        self.key_listener.daemon = True
        self.mouse_listener.daemon = True
        self.key_listener.start()
        self.mouse_listener.start()

    def setup_gui(self):
        self.root = tk.Tk()
        self.root.title("CreamyKeys")
        self.root.geometry("400x380")

        main_frame = ttk.Frame(self.root, padding="10")
        main_frame.pack(fill=tk.BOTH, expand=True)

        # Enabled Checkbox
        self.enabled_var = tk.BooleanVar(value=self.config["enabled"])
        ttk.Checkbutton(main_frame, text="Enable Sounds", variable=self.enabled_var, command=self.update_enabled).pack(pady=5)

        # Volume Slider
        ttk.Label(main_frame, text="Volume").pack(pady=2)
        self.volume_var = tk.DoubleVar(value=self.config["volume"])
        self.volume_scale = ttk.Scale(main_frame, from_=0.0, to=1.0, variable=self.volume_var, orient=tk.HORIZONTAL, command=self.update_volume)
        self.volume_scale.pack(fill=tk.X, pady=5)
        # Bind release to save config so it doesn't spam disk
        self.volume_scale.bind("<ButtonRelease-1>", lambda e: self.save_config())

        # Keyboard Pack Dropdown
        ttk.Label(main_frame, text="Keyboard Sound Pack").pack(pady=2)
        self.kb_pack_var = tk.StringVar(value=self.config["keyboard_pack"])
        self.kb_dropdown = ttk.Combobox(main_frame, textvariable=self.kb_pack_var, values=list(self.packs.keys()), state="readonly")
        self.kb_dropdown.pack(fill=tk.X, pady=5)
        self.kb_dropdown.bind("<<ComboboxSelected>>", self.update_kb_pack)

        # Mouse Pack Dropdown
        ttk.Label(main_frame, text="Mouse Sound Pack").pack(pady=2)
        self.mouse_pack_var = tk.StringVar(value=self.config["mouse_pack"])
        self.mouse_dropdown = ttk.Combobox(main_frame, textvariable=self.mouse_pack_var, values=list(self.packs.keys()), state="readonly")
        self.mouse_dropdown.pack(fill=tk.X, pady=5)
        self.mouse_dropdown.bind("<<ComboboxSelected>>", self.update_mouse_pack)

        # Test Buttons
        test_frame = ttk.Frame(main_frame)
        test_frame.pack(fill=tk.X, pady=10)
        ttk.Button(test_frame, text="Test Keyboard Sound", command=lambda: self.play_sound("keyboard")).pack(side=tk.LEFT, padx=5, expand=True)
        ttk.Button(test_frame, text="Test Mouse Sound", command=lambda: self.play_sound("mouse")).pack(side=tk.LEFT, padx=5, expand=True)

        ttk.Button(main_frame, text="Save & Exit", command=self.exit_app).pack(pady=20)

    def update_enabled(self):
        self.config["enabled"] = self.enabled_var.get()
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
        root = tk.Tk()
        root.withdraw()
        messagebox.showerror("Hata / Error", f"Uygulama başlatılırken bir hata oluştu:\n\n{e}")
        sys.exit(1)

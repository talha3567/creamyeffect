# CreamyKeys Standalone

This is a standalone version of the Minecraft mod "CreamyKeys" that adds mechanical keyboard and mouse clicking sounds to your entire computer.

## How to use

1.  **Install Python** (if you want to run from source).
2.  **Install dependencies**:
    ```bash
    pip install pynput pygame
    ```
3.  **Run the application**:
    ```bash
    python creamy_keys.py
    ```

## How to create .exe (Windows)

To convert this script into a standalone `.exe` file, follow these steps:

1.  **Install PyInstaller**:
    ```bash
    pip install pyinstaller
    ```
2.  **Build the executable**:
    Run the following command in the terminal:
    ```bash
    pyinstaller --noconsole --onefile --add-data "sounds;sounds" creamy_keys.py
    ```
    - `--noconsole`: Prevents a terminal window from opening when you run the app.
    - `--onefile`: Bundles everything into a single `.exe` file.
    - `--add-data "sounds;sounds"`: Includes the sound assets in the executable.

The final `.exe` will be located in the `dist/` folder.

## Features

- **Global Key Sounds**: Works in any application, not just Minecraft.
- **Sound Selection**: Choose from various mechanical switch types (Cherry MX Black, Blue, Brown, Red).
- **Volume Control**: Adjust the loudness of the clicks.
- **Mouse Clicks**: Separate sound set for mouse buttons.
- **Settings Persistence**: Saves your preferences in `config.json`.

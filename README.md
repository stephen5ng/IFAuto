# Interactive Fiction for Android Auto

This project is an Android Auto application designed to play interactive fiction (text adventures) using voice input and output. It features a built-in Z-Machine interpreter, allowing you to play classic games like Zork.

## How to Build and Run

### 1. Prerequisites
- **Android Studio**: Download and install [Android Studio](https://developer.android.com/studio).
- **Android SDK**: Ensure you have API 34 installed.
- **DHU (Desktop Head Unit)**: Required to simulate the car screen on your computer.
  - Install via SDK Manager > SDK Tools > **Android Auto Desktop Head Unit Emulator**.
  - Located at: `[SDK_PATH]/extras/google/auto/desktop-head-unit`.

### 2. Running in the Emulator (Recommended for Testing)
1. **Create a Phone Emulator**: Use a Pixel 7 (or similar) with **Google Play Store** support (API 30+). **Note: Do NOT use the "Android Auto" app from the Play Store; it is for legacy devices.**
2. **Setup Android Auto in Emulator (Android 10+)**:
   - Launch the emulator.
   - Go to **Settings > Connected devices > Connection preferences > Android Auto**.
   - (If missing, search for "Android Auto" in the Settings search bar and look for the one with the "cog" icon, not the Play Store icon).
   - Scroll to the very bottom and tap **Version** 10 times to enable Developer Mode.
   - Tap the three dots (top right) > **Start head unit server**.
3. **Connect the DHU**:
   - In your computer's terminal: `adb forward tcp:5277 tcp:5277`.
   - Run the DHU: `./desktop-head-unit` (from the SDK path mentioned above).
4. **Deploy from Android Studio**:
   - Select the running emulator in the device list.
   - Click **Run**.
   - The app will appear in the DHU window (the "car screen").

### 3. Running on a Physical Device
1. **Enable Unknown Sources**: 
   - Open Android Auto settings on your phone (same path as above).
   - Enable Developer Mode (tap Version 10 times).
   - In Developer Settings, check **Unknown sources**.
2. **Deploy**: Connect via USB and click **Run** in Android Studio.

### Troubleshooting
- **"App isn't compatible" in Play Store**: Ignore this. Android Auto is now a system component. You do not need to "install" or "open" it from the Play Store. Use the path in **Settings** instead.
- **DHU Blank Screen**: Run `adb forward tcp:5277 tcp:5277` again and restart the DHU.
- **Check Architecture**: Ensure your emulator is using an x86_64 or arm64 image that matches your computer's architecture.

## Adding Games
1. Obtain a `.z3`, `.z5`, or `.z8` file (e.g., `zork1.z3`).
2. Place the file in `app/src/main/assets/`.
3. Update the filename in `GameScreen.kt` (currently defaults to `zork1.z3`).

## Features
- **Z-Machine Interpreter**: Built-in support for standard Z-code games.
- **Voice Output**: Uses Text-to-Speech (TTS) to narrate the story.
- **Voice Input**: Tap the microphone icon in the car to speak commands (e.g., "Go North", "Take lamp").
- **Shortcut Buttons**: Quick-access buttons for common commands like "Look" and "Inventory".

## Attribution
The Z-Machine interpreter (`zengine/` directory) is based on [TextFiction](https://github.com/onyxbits/TextFiction) by Onyxbits, used under the **Artistic License 2.0**.

**Changes made for IFAuto:**
- Adapted package structure for Android/Gradle integration.
- Added array bounds safety checks in `ZState.java`.
- Fixed instruction implementations in `ZInstruction5.java` for improved stability.
- Added Kotlin wrapper for Android Auto integration.

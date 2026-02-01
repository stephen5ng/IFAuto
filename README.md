# Interactive Fiction for Android Auto

This project is an Android Auto application designed to play interactive fiction (text adventures) using voice input and output. It features a built-in Z-Machine interpreter, allowing you to play classic games like Zork.

## How to Build and Run

Since this is a native Android project, you need to compile it using the Android SDK.

### 1. Prerequisites
- **Android Studio**: Download and install [Android Studio](https://developer.android.com/studio).
- **Android SDK**: Ensure you have API 34 installed (managed via Android Studio's SDK Manager).

### 2. Opening the Project
1. Open Android Studio.
2. Select **Open** and navigate to the `blazing-cluster` directory.
3. Wait for the Gradle sync to complete. It will automatically download the necessary dependencies (Car App Library, etc.).

### 3. Adding Games
To play existing Z-Machine games:
1. Obtain a `.z3`, `.z5`, or `.z8` file (e.g., `zork1.z3`).
2. Place the file in the `app/src/main/assets/` directory.
3. Rename it to `minizork.z3` (or update the filename in `GameScreen.kt`).

### 4. Running on Your Car / Phone
Android Auto has strict safety requirements. To run this app (which is currently "untrusted"):
1. **Enable Developer Mode on Phone**:
   - Open Android Auto settings on your phone.
   - Scroll to the bottom and tap **Version** 10 times until a "Developer mode enabled" toast appears.
   - Tap the three dots (menu) in the top right -> **Developer settings**.
   - Check **Unknown sources**.
2. **Deploy**:
   - Connect your phone via USB.
   - Select your phone in Android Studio's device dropdown.
   - Click **Run** (the green play button).
3. **In the Car**:
   - The app should appear in your Android Auto app drawer.
   - Note: You may need to start the "Head unit server" from the Android Auto developer settings on the phone if you are testing with the Desktop Head Unit (DHU).

## Features
- **Z-Machine Interpreter**: Built-in support for standard Z-code games.
- **Voice Output**: Uses Android's Text-to-Speech (TTS) to narrate the story.
- **Voice Commands**: "Look", "North", "Open Mailbox", etc., work via voice or the car's screen.
- **JSON Engine**: A fallback engine for simple, custom-scripted stories.

## Attribution

The Z-Machine interpreter (`zengine/` directory) is based on [TextFiction](https://github.com/onyxbits/TextFiction) by Onyxbits, used under the **Artistic License 2.0**.

**Changes made for IFAuto:**
- Adapted package structure for Android/Gradle integration
- Added array bounds safety checks in `ZState.java`
- Fixed instruction implementations in `ZInstruction5.java` for improved stability

See the git history for detailed changes:
- **Commit `cd6b148`**: Original TextFiction code (unmodified except package name)
- **Commit `ceb5400`**: IFAuto-specific modifications

A fork of TextFiction is maintained at [stephen5ng/TextFiction](https://github.com/stephen5ng/TextFiction) for reference.

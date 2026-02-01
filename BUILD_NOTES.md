# Build and Setup Notes

This document records the lessons learned regarding setting up the Android Auto development environment and building the project.

## Android Auto Emulator Setup ("Error 14" Fix)

### The Issue
When trying to connect the Desktop Head Unit (DHU) to an Android Emulator, you may stand a red error screen on the DHU:
> "Communication error 14 - Your car's software didn't pass Android Auto security checks"

### The Cause
Newer versions of Android Auto (2024+) perform strict security handshakes.
- **Android 14 (API 34)** emulator images often fail this check due to missing Play Store certifications or "stub" versions of system apps.
- **Outdated Apps**: If the main "Google" app or "Google Maps" on the emulator is old, the security check fails.
- **System Clock**: If the emulator time does not match the host time, SSL handshakes fail.

### The Reliable Fix
Use an **Android 13 (API 33)** emulator image. It is the current "stable standard" for Android Auto development and bypasses the stricter checks found in API 34+ images.

**Recommended Emulator Configuration (for Apple Silicon/M-Series):**
*   **Hardware:** Pixel 6 or Pixel 7
*   **Release Name:** Tiramisu (Android 13) - API 33
*   **Architecture:** **arm64-v8a** (Crucial: do not use x86 on M1/M2/M3 chips)
*   **Target:** **Google Play** (Must have the Play Store icon)

**Setup Steps:**
1.  **Boot the Emulator.**
2.  **Sign in to the Play Store** immediately.
3.  **Update Core Apps:** Search for and update **Google Maps** and the **Google** app. This is often the missing key to passing security.
4.  **Update Android Auto:** Update it via the Play Store.
    *   *Note: If Play Store says "Incompatible", sideload the `arm64` APK from APKMirror.*
5.  **Enable Developer Mode:**
    *   Open Android Auto Settings.
    *   Scroll to "Version", tap it 10 times.
    *   3-dots Menu -> Developer Settings -> **Start Head Unit Server**.
6.  **Run DHU on Mac:**
    ```bash
    # Forward the port
    adb forward tcp:5277 tcp:5277
    
    # Run the utility
    ~/Library/Android/sdk/extras/google/auto/desktop-head-unit/desktop-head-unit
    ```

---

## Building the Project

### Java version
The build requires a compatible Java Runtime. If you do not have a standalone JDK installed, use the JetBrains Runtime (JBR) bundled with Android Studio.

**Build Command:**
```bash
export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"
./gradlew assembleDebug
```

### Installation
To install the APK on a specific device (especially when an emulator is also running):

1.  **Find the Device ID:**
    ```bash
    adb devices
    # Example output: 31041JEHN17892  device
    ```

2.  **Install:**
    ```bash
    adb -s 31041JEHN17892 install -r app/build/outputs/apk/debug/app-debug.apk
    ```

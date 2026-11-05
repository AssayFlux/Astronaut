# Build and install an Astronaut APK

## CI versus manual APK testing

GitHub Actions runs Android JVM/unit tests, Android emulator instrumentation smoke tests, and assembles/uploads a debug APK artifact. Physical installation and real-device inference/performance testing remain the owner's responsibility.

The repository currently contains `gradle/wrapper/gradle-wrapper.properties` but does not contain `gradlew`, `gradlew.bat` or `gradle-wrapper.jar`. Do not run `./gradlew` or `gradlew.bat` until a complete wrapper is committed. Use an installed Gradle 8.13 distribution or Android Studio's compatible Gradle integration.

**Application ID:** `org.astronaut.agi`  
**Version name:** `0.1.0`  
**Version code:** `7`  
**Minimum Android:** API 26  
**Compile SDK:** 36  
**Java:** 17

## 1. Install build prerequisites

Install:

- JDK 17
- Android SDK Platform 36
- Android SDK Build-Tools 36.0.0
- Gradle 8.13

Accept Android SDK licenses in Android Studio's SDK Manager (or with `sdkmanager --licenses`). Ensure `ANDROID_HOME` or `ANDROID_SDK_ROOT` points to your SDK installation and that `platform-tools` is on PATH if you plan to use `adb`.

Verify:

```bash
java -version
gradle --version
adb version
```

The Java output should report version 17 and Gradle should report 8.13.

## 2. Run unit tests

From the repository root:

```bash
gradle --no-daemon :app:testDebugUnitTest
```

Fix any local environment or test failure before continuing.

## 3. Assemble the debug APK

```bash
gradle --no-daemon :app:assembleDebug
```

Expected output:

```text
app/build/outputs/apk/debug/app-debug.apk
```

No model weights are bundled in this APK. Import a compatible `.litertlm` model after installation.

## 4. Install and verify on a device

Enable Developer options and USB debugging, connect the device, then run:

```bash
adb devices
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

Verify the package is installed:

```bash
adb shell pm list packages | grep org.astronaut.agi
```

On Windows PowerShell, replace `grep` with `Select-String`.

## Android Studio alternative

Open the repository root in Android Studio, allow Gradle sync to complete, select the `app` run configuration and the `debug` build variant, then choose **Build > Build Bundle(s) / APK(s) > Build APK(s)**. Use JDK 17 and Gradle 8.13 in the IDE's Gradle settings.

## CI and local test coverage

The Android workflow runs `:app:testDebugUnitTest`, `:app:connectedDebugAndroidTest` on an API 35 emulator, and `:app:assembleDebug`. The instrumentation smoke tests check the stable application ID, app-private storage and core permission declarations. The separate backend workflow runs pytest.

## What this build does not prove

A successful compile/unit-test run does not certify:

- LiteRT-LM model compatibility or inference performance
- Multimodal image understanding on a supported model
- Offline speech recognition/TTS packs on your device
- SQLCipher migration, recovery or backup behavior on upgrade
- Memory behavior with large models or attachments
- Model catalog downloads/resume against a production HTTP Range server

These require physical-device testing. The first APK is for development/testing, not a production release.

## Signed release

Production distribution requires an owner-controlled keystore and the separate **Signed Android release** workflow. Follow [RELEASE_SIGNING.md](RELEASE_SIGNING.md). Never commit keystores or passwords.

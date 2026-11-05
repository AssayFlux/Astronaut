# Build and install an Astronaut APK

## Recommended: GitHub Actions

The repository does not currently commit the Gradle wrapper scripts, so the reproducible path for a user without a local Gradle installation is the GitHub Actions workflow.

1. Open [Actions](https://github.com/AssayFlux/Astronaut/actions).
2. Choose **Android build**.
3. Select **Run workflow** (or push a branch / open a pull request targeting `main`).
4. Wait for the Android job to complete successfully.
5. Open that run and find **Artifacts**.
6. Download `astronaut-debug-apk` and unzip it.
7. Install `app-debug.apk` on the Android device.

If Android blocks installation, allow installation from the file manager/browser used to open the APK. The app ID is `org.astronaut.agi`.

## Local build

Install JDK 17, Android SDK platform 36, build-tools 36.0.0 and Gradle 8.13. Then run from the repository root:

```bash
gradle --no-daemon :app:testDebugUnitTest
gradle --no-daemon :app:assembleDebug
```

Output:

```
app/build/outputs/apk/debug/app-debug.apk
```

## What the build does not include

- No model weights are bundled. Import a compatible `.litertlm` model after installing the APK.
- A successful CI build does not validate LiteRT inference, speech packs, camera/image quality, encryption migration or memory behavior on a physical device.
- The debug APK is suitable for testing. Production distribution should use the signed-release workflow and an owner-controlled keystore.

## Signed release

Follow [RELEASE_SIGNING.md](RELEASE_SIGNING.md). The signing workflow requires the four owner-configured GitHub secrets listed there. Do not upload the keystore or passwords into source control, issues or chat.

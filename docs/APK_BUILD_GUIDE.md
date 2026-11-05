# Build and install an Astronaut APK

## CI versus manual APK testing

GitHub Actions runs Android JVM/unit tests only. It does not assemble or upload an APK. APK assembly and installation are intentionally left to the owner for manual testing after the implementation steps are complete.

The repository does not currently commit the Gradle wrapper scripts, so install JDK 17, Android SDK platform 36, build-tools 36.0.0 and Gradle 8.13 before running the local commands below.

The app ID is `org.astronaut.agi`.

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

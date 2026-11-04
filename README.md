# Astronaut — Offline AI Messenger foundation

Android app ID: org.offlineai.messenger. Kotlin, Jetpack Compose, API 26+, compileSdk 35, JDK 17.

## Included
- Gradle Kotlin DSL Android project skeleton and six-section Compose shell.
- Room conversation/message entities and DAO.
- Model capability/provider contracts, catalog, task classifier and guarded two-model collaboration orchestration.
- Backend endpoint and training executor contracts, dataset metadata and LoRA configuration.
- Basic task-router unit tests.

## Not yet complete
Production llama.cpp/LiteRT-LM adapters and JNI binaries; model downloads/import/checksums; persistence wiring to UI; full SAF attachment processing; streaming/cancellation/peer review; document/audio/video pipelines; dataset parser and actual training execution; security hardening; full tests and device verification. The app intentionally does not fabricate model output or training progress.

## Build
Install JDK 17, Android SDK platform 35/build tools and Gradle 8.9-compatible. Run:
```sh
gradle :app:testDebugUnitTest
gradle :app:assembleDebug
```
APK: app/build/outputs/apk/debug/app-debug.apk.

## Termux
Termux is not an officially supported Android Gradle host. Install JDK 17, git, unzip, clang, cmake and ninja. Configure JAVA_HOME, ANDROID_HOME and ANDROID_SDK_ROOT; install platform 35/build tools. If SDK host tools fail under Android/Bionic, use Debian/Ubuntu proot or an ARM64 Linux build host. Native inference also needs a compatible arm64-v8a NDK build; none is bundled here.

## Backend and training
The mobile app is intended for inference and dataset preparation. Full LoRA/QLoRA generally requires suitable GPU hardware. Training contracts are present, but execution requires a real local trainer or explicitly configured backend. Backend connections must be opt-in and disclose data transfer.

# Astronaut — Offline AI Messenger

Android application ID: org.offlineai.messenger · Kotlin · Jetpack Compose · Room · API 26+.

## Current APK milestone

- Persistent local conversations and message history.
- Import .litertlm models into app-private storage.
- Run local text generation through Google's LiteRT-LM runtime on the CPU.
- Load/unload an imported model from the Models screen.
- Local project list / workspace.
- No chat upload, sign-in or account requirement.

## First-run model setup

The APK intentionally does not contain a 586 MB language model. Download a compatible model once, then import it in Astronaut > Models. After the model is installed, text generation runs on-device without internet.

Recommended starter model: [Qwen3-0.6B LiteRT-LM (586 MB)](https://huggingface.co/litert-community/Qwen3-0.6B/blob/main/Qwen3-0.6B.litertlm). Its current model card documents the .litertlm format and LiteRT-LM 0.18.0+ compatibility. The app currently uses LiteRT-LM 0.18.1.

1. Download Qwen3-0.6B.litertlm to your phone.
2. Open Astronaut, select Models and tap Import .litertlm model.
3. Select the downloaded file and tap Load model.
4. Return to Chats and send a message. The model runs locally.

A 0.6B model is a starter, not a frontier-scale assistant. Actual speed and memory use depend on the phone. The app uses the CPU backend as a conservative compatibility default.

## Build

Requirements: JDK 17, Android SDK platform 35, Gradle 8.13.

    gradle :app:testDebugUnitTest :app:assembleDebug

APK: app/build/outputs/apk/debug/app-debug.apk.

GitHub Actions also builds and uploads the debug APK as an artifact after successful tests.

## Current limitations

This is the first offline text-chat milestone, not the full Astronaut product. Attachment files are currently stored as message references and are not yet parsed into model context. Multi-model peer review, model downloads inside the app, vision/audio, streaming UI rendering, encryption-at-rest and release signing are still pending.

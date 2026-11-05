# Astronaut

<p align="center">
  <img src="Assets/astronauts-icon.jpg" alt="Astronaut — two astronauts holding hands" width="180" />
</p>

<h3 align="center">Private AI. On your device.</h3>
<p align="center">
  <strong>Chat · Code · Create · Work offline</strong>
</p>
<p align="center">
  <a href="https://github.com/AssayFlux/Astronaut/actions"><img src="https://img.shields.io/github/actions/workflow/status/AssayFlux/Astronaut/android-build.yml?branch=main&label=Android%20CI" alt="Android CI" /></a>
  <img src="https://img.shields.io/badge/Android-8.0%2B-3DDC84?logo=android&logoColor=white" alt="Android 8.0+" />
  <img src="https://img.shields.io/badge/version-0.1.0-7C3AED" alt="Version 0.1.0" />
  <a href="LICENSE"><img src="https://img.shields.io/github/license/AssayFlux/Astronaut" alt="License" /></a>
</p>

Astronaut is an Android AI workspace built around **on-device inference, private local data and user-controlled models**. Run supported LiteRT-LM models locally or optionally connect to an OpenAI-compatible HTTPS provider.

## Highlights

- Offline AI chat with compatible `.litertlm` models.
- Local model downloads, imports and SHA-256 verification.
- Private chat history using Room and SQLCipher.
- Document ingestion, on-device OCR and image labels.
- Local retrieval with BM25 and optional ONNX embeddings.
- Voice input and text-to-speech using Android services.
- Developer workspace for selected project files.
- Explicitly configured online providers; no silent cloud fallback.

> **Status:** Active development. Image generation, GGUF/llama.cpp inference and a full IDE are not currently integrated. Model compatibility depends on its runtime and device resources. Models are not bundled in the APK.

## Build

**Requirements:** JDK 17, Android SDK 36 and Gradle 8.13.

```bash
gradle --no-daemon :app:testDebugUnitTest
gradle --no-daemon :app:assembleDebug
```

APK: `app/build/outputs/apk/debug/app-debug.apk`  
Application ID: `org.astronaut.agi` · Version: `0.1.0`

## Documentation

[Architecture](docs/PRODUCT_ARCHITECTURE.md) · [Feature status](docs/FEATURE_STATUS.md) · [APK build guide](docs/APK_BUILD_GUIDE.md) · [Release signing](docs/RELEASE_SIGNING.md)

## License

See [LICENSE](LICENSE).

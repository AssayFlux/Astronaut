# Astronaut

<p align="center">
  <img src="Assets/Icon.svg" alt="Astronaut app icon" width="180" />
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

Astronaut is an Android AI workspace focused on **on-device inference, private local data, and user-controlled models**. Run supported LiteRT-LM models locally or optionally connect to an OpenAI-compatible HTTPS provider.

## Highlights

- **Offline AI chat** with imported, compatible `.litertlm` models.
- **Private history** stored in a Room database protected by SQLCipher.
- **Model manager** with local imports, downloads and SHA-256 verification.
- **Files and images** with document ingestion, on-device OCR and image labels.
- **Local retrieval** with BM25 and optional ONNX embeddings.
- **Voice tools** using available Android speech and text-to-speech services.
- **Developer workspace** for browsing and editing selected project files.
- **Optional online mode** — explicit provider configuration; no silent cloud fallback.

> **Development status:** Active development. Image generation, GGUF/llama.cpp inference and a full IDE are not currently integrated. Model support depends on its format, runtime and device resources. No models are bundled in the APK.

## Build

**Requirements:** JDK 17, Android SDK 36 and Gradle 8.13.

```bash
gradle --no-daemon :app:testDebugUnitTest
gradle --no-daemon :app:assembleDebug
```

APK output: `app/build/outputs/apk/debug/app-debug.apk`

Install on a connected device:

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

The Android application ID is `org.astronaut.agi` (version `0.1.0`). The optional Python training service is documented in [backend/README.md](backend/README.md).

## Documentation

[Architecture](docs/PRODUCT_ARCHITECTURE.md) · [Feature status](docs/FEATURE_STATUS.md) · [APK build guide](docs/APK_BUILD_GUIDE.md) · [Release signing](docs/RELEASE_SIGNING.md)

## License

See [LICENSE](LICENSE).

# Astronaut

<p align="center">
  <img src="Assets/Icon.svg" alt="Astronaut app icon" width="160" />
</p>

<h3 align="center">Private AI. On your device.</h3>
<p align="center"><strong>Chat · Code · Create · Work offline</strong></p>

<p align="center">
  <img src="https://img.shields.io/badge/Android-8.0%2B-3DDC84?logo=android&logoColor=white" alt="Android 8.0+" />
  <img src="https://img.shields.io/badge/version-0.1.0-7C3AED" alt="Version 0.1.0" />
</p>

Astronaut is an Android AI workspace for **private, on-device AI**. Run supported LiteRT-LM models locally, manage your models and data, and optionally connect an online provider.

## Features

- Offline chat with compatible `.litertlm` models
- Model downloads, local imports and SHA-256 verification
- Encrypted local chat history
- Document ingestion, OCR and image labelling
- Local retrieval and voice input
- Optional, explicitly configured online providers

> **Status:** Active development. Model compatibility depends on the runtime and device. Image generation and GGUF/llama.cpp inference are not currently integrated. Models are not bundled in the APK.

## Build

**Requirements:** JDK 17 · Android SDK 36 · Gradle 8.13

```bash
gradle --no-daemon :app:testDebugUnitTest
gradle --no-daemon :app:assembleDebug
```

APK: `app/build/outputs/apk/debug/app-debug.apk`  
Application ID: `org.astronaut.agi` · Version: `0.1.0`

[Architecture](docs/PRODUCT_ARCHITECTURE.md) · [Feature status](docs/FEATURE_STATUS.md) · [Build guide](docs/APK_BUILD_GUIDE.md) · [License](LICENSE)

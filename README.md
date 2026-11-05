# Astronaut

<p align="center">
  <img src="Assets/Icon.svg" alt="Astronaut app icon" width="144" />
</p>

<h3 align="center">Private AI. On your device.</h3>
<p align="center"><strong>Chat · Code · Understand · Work offline</strong></p>

<p align="center">
  <img src="https://img.shields.io/badge/Android-8.0%2B-3DDC84?logo=android&logoColor=white" alt="Android 8.0+" />
  <img src="https://img.shields.io/badge/version-0.1.0-7C3AED" alt="Version 0.1.0" />
  <img src="https://img.shields.io/badge/license-MIT-64748B" alt="MIT license" />
</p>

Astronaut is an offline-first Android AI workspace. It keeps conversations and imported models on-device, runs supported LiteRT-LM models locally, and offers optional tools for documents, image understanding, retrieval, voice and development workflows.

## What is included

| Area | Current implementation |
|---|---|
| Private chat | Persistent conversations, local inference and encrypted local database |
| Model library | Local import, HTTPS downloads, SHA-256 verification, signed catalog validation, versioned installs and rollback |
| Documents | PDF, DOCX and text extraction with bounded processing |
| Images | OCR and image labels; multimodal image input through compatible LiteRT-LM models |
| Retrieval | BM25 search with optional ONNX MiniLM embeddings and matching vocabulary |
| Voice | Android speech recognition and offline TTS voice discovery |
| Workspace | Local projects and a SAF-scoped text editor |
| Collaboration | Sequential review using two distinct local models |
| Optional training | Separate FastAPI/Celery backend for dataset validation and training job management |

## Runtime support is explicit

The current Android build includes LiteRT-LM for compatible `.litertlm` chat/vision models and ONNX Runtime for the supported embedding path. GGUF/llama.cpp, diffusion image generation, Whisper and Sherpa-ONNX artifacts may be described or downloaded by the catalog, but their execution runtimes are **not bundled** in this build. A model file extension alone never means it can run.

Image generation and general-purpose GGUF inference are not currently integrated. Models are not bundled in the APK.

## Requirements

- Android 8.0 / API 26 or newer
- JDK 17
- Android SDK Platform 36 and Build-Tools 36.0.0
- Gradle 8.13

## Build and test

Run from the repository root:

```bash
gradle --no-daemon :app:testDebugUnitTest
gradle --no-daemon :app:assembleDebug
```

The Android CI workflow also runs emulator instrumentation smoke tests and assembles the debug APK. Backend tests run separately with pytest.

Expected APK: `app/build/outputs/apk/debug/app-debug.apk`

See [APK build and installation](docs/APK_BUILD_GUIDE.md) for device setup and manual verification.

## Signed model catalog setup

The catalog is intentionally disabled until the publisher provisions a real Ed25519 key and catalog endpoint. For a release build, supply:

- `ASTRONAUT_CATALOG_URL`: HTTPS URL of the signed catalog envelope.
- `ASTRONAUT_CATALOG_PUBLIC_KEY`: Base64 X.509 Ed25519 public key.

These are Gradle properties, for example in a local untracked `~/.gradle/gradle.properties` or protected CI configuration. The app needs only the public key. Keep the private signing key in a protected publisher environment; never commit it or package it in the APK. See [model catalog and key management](docs/MODEL_ECOSYSTEM.md).

## Release signing

Production signing uses an owner-controlled Android keystore and the separate Signed Android release workflow. Configure the four required GitHub Actions secrets as documented in [release signing](docs/RELEASE_SIGNING.md). Never commit a keystore or passwords.

## Identity

- Android application ID: `org.astronaut.agi`
- Android Gradle namespace: `org.offlineai.messenger`
- Version name: `0.1.0`
- Version code: `7`

The application ID and version name are intentionally preserved.

## Documentation

- [Product architecture](docs/PRODUCT_ARCHITECTURE.md)
- [Feature and validation status](docs/FEATURE_STATUS.md)
- [Repository audit](docs/REPO_AUDIT.md)
- [Model catalog, compatibility and signing](docs/MODEL_ECOSYSTEM.md)
- [APK build and installation](docs/APK_BUILD_GUIDE.md)
- [Release signing](docs/RELEASE_SIGNING.md)
- [Security and privacy review](docs/SECURITY_PRIVACY.md)
- [Offline features](docs/OFFLINE_FEATURES.md)
- [Adding AI runtimes](docs/ADDING_AI_RUNTIMES.md)

## Development status

Astronaut is under active development. CI can verify compilation and automated tests, but it cannot certify a particular model on a particular phone. LiteRT-LM inference, image Q&A, offline speech packs, storage recovery, RAM use, battery use and thermal behavior still require physical-device validation.

Contributions that improve the existing offline-first architecture, tests, accessibility and runtime compatibility are welcome.

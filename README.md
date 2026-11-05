# THE  ASTRONAUT

<p align="center">
  <img src="Assets/icon.svg" alt="Astronaut app icon" width="300" />
</p>

<p align="center"><strong>Private AI. On your device.</strong></p>
<p align="center">An offline-first Android AI workspace for chat, coding, documents and local models.</p>

---

## Overview

Astronaut is an Android application designed to keep conversations, imported models and workspace data on the device. It combines a clean, card-based interface with local AI inference, model management, document tools and optional developer/training workflows.

The app supports compatible LiteRT-LM models for local text inference and multimodal image understanding, plus an ONNX-based embedding path for retrieval. Remote AI and the optional training backend are opt-in features; local chat does not silently fall back to cloud inference.

## At a glance

| Item | Details |
|---|---|
| Platform | Android |
| Application ID | `org.astronaut.agi` |
| Version | `0.1.0` (version code `7`) |
| Minimum Android | Android 8.0 (API 26) |
| Compile SDK | 36 |
| Language and UI | Kotlin, Jetpack Compose |
| Build system | Gradle wrapper 8.13 |
| Java toolchain | JDK 17 |
| Local inference | LiteRT-LM for supported models |
| Retrieval | BM25 with optional ONNX MiniLM embeddings |
| Database | Room with SQLCipher |
| License | MIT |

## Features

| Area | Included functionality |
|---|---|
| AI chat | On-device conversations, persistent chat history, response-style preferences and local inference |
| Model hub | Model import, HTTPS downloads, SHA-256 verification, signed catalog validation, versioned installs and rollback controls |
| Model safety | Runtime compatibility checks, Android API checks and memory preflight estimates |
| Documents | PDF, DOCX and text extraction with bounded processing |
| Image understanding | OCR, image labels and multimodal input through compatible LiteRT-LM models |
| Knowledge retrieval | Local BM25 search and optional ONNX embedding-based hybrid retrieval |
| Voice | Android speech recognition, offline language-pack controls and local TTS voice selection/preview |
| Workspace | Local projects, conversations and a SAF-scoped text editor |
| Model collaboration | Sequential review using two distinct compatible local models |
| Developer Studio | Project-folder browsing and bounded UTF-8 text editing |
| Training Studio | Dataset validation and training-job management through an optional backend |
| Personalisation | Light/dark themes, five accent colours and concise, balanced or detailed response styles |

## Runtime and model compatibility

A model being downloadable or smaller than the import limit does **not** mean that it can execute on the device. The app only loads artifacts supported by an installed runtime adapter and compatible model metadata.

| Model / artifact type | Current status | Notes |
|---|---|---|
| LiteRT-LM (`.litertlm`) | Runtime integrated | Supports compatible local text and multimodal models |
| ONNX | Runtime integrated for embeddings | MiniLM-compatible embedding models and matching vocabulary are required for semantic retrieval |
| GGUF | Catalog/import representation only | llama.cpp Android runtime is not bundled |
| Diffusion / Safetensors bundles | Catalog/import representation only | No executable on-device image-generation runtime is bundled |
| Whisper / Sherpa-ONNX | Catalog/import representation only | Independent speech runtimes are not bundled |
| TFLite | Catalog format represented | Availability depends on a dedicated compatible adapter |

**Not currently included:** general-purpose GGUF inference, diffusion image generation, a full IDE with Git/build/shell execution, or model weights bundled in the APK. Do not treat a catalog entry or file extension as proof of runtime support.

## Privacy and security

| Principle | Implementation |
|---|---|
| Local-first | Chats and imported model files are stored locally |
| Encrypted app data | SQLCipher database with Android Keystore-backed key handling |
| Verified downloads | SHA-256 checksum verification and resumable HTTPS downloads |
| Trusted catalog | Ed25519 signature verification; publisher must configure a real public key and catalog endpoint |
| Optional cloud use | Explicitly configured remote provider; no automatic fallback from local inference |
| Backend connectivity | Optional and user-configured; private chat data is not silently uploaded |
| Secrets | Signing keys, keystores and passwords must remain outside the repository |

The signed model catalog is disabled until the publisher provisions a real Ed25519 key pair and configures the catalog URL and public key. The private signing key must remain in a protected publisher environment.

## Requirements

| Requirement | Version / value |
|---|---|
| Android SDK Platform | 36 |
| Android Build-Tools | 36.0.0 |
| JDK | 17 |
| Gradle | Use the committed wrapper (8.13) |
| Android device | API 26 or newer |

## Build a debug APK

Run these commands from the repository root. The command below assembles the debug APK only; it does not run emulator instrumentation tests or any workflow.

```bash
chmod +x ./gradlew
./gradlew --no-daemon :app:assembleDebug
```

Expected output:

```text
app/build/outputs/apk/debug/app-debug.apk
```

For Termux-specific setup and copy/install commands, see `docs/APK_BUILD_GUIDE.md`.

### Optional: run local unit tests separately

```bash
./gradlew --no-daemon :app:testDebugUnitTest
```

### Install with ADB

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

No AI model weights are bundled in the APK. Import a compatible model after installing the app.

## Optional signed model catalog configuration

Provide these Gradle properties only after a publisher has created and secured the catalog:

| Property | Purpose |
|---|---|
| `ASTRONAUT_CATALOG_URL` | HTTPS URL of the signed catalog envelope |
| `ASTRONAUT_CATALOG_PUBLIC_KEY` | Base64 X.509 Ed25519 public key |

For local builds, place the values in an untracked `~/.gradle/gradle.properties`. For GitHub Actions, use `ORG_GRADLE_PROJECT_ASTRONAUT_CATALOG_URL` and `ORG_GRADLE_PROJECT_ASTRONAUT_CATALOG_PUBLIC_KEY`. Never store the private signing key in Gradle properties, the repository or the APK.

## Project documentation

| Document | Description |
|---|---|
| `docs/PRODUCT_ARCHITECTURE.md` | Product boundaries, architecture and data ownership |
| `docs/FEATURE_STATUS.md` | Implemented code paths and outstanding validation |
| `docs/REPO_AUDIT.md` | Repository audit and engineering notes |
| `docs/MODEL_ECOSYSTEM.md` | Model catalog, compatibility and signing |
| `docs/APK_BUILD_GUIDE.md` | APK build, install and device verification |
| `docs/RELEASE_SIGNING.md` | Production signing setup |
| `docs/SECURITY_PRIVACY.md` | Security and privacy review |
| `docs/STEP_1_VALIDATION.md` | Validation matrix |
| `docs/OFFLINE_FEATURES.md` | Offline capability notes |
| `docs/ADDING_AI_RUNTIMES.md` | Runtime adapter integration guide |

## Development and validation status

Astronaut is under active development. Implemented code paths are not the same as completed device certification.

| Validation area | Status |
|---|---|
| Android compilation and automated tests | Available through the repository build/CI configuration |
| LiteRT-LM model compatibility and inference | Requires testing with supported model artifacts on physical devices |
| Multimodal image understanding | Requires a compatible model and physical-device validation |
| Offline speech recognition and TTS | Depends on Android speech services, OEM support and installed language packs |
| Download resume and catalog publishing | Requires testing against production HTTPS/Range hosting and a provisioned signed catalog |
| Large-model memory, thermal and battery behaviour | Requires physical-device profiling |
| Production signing and release approval | Requires owner-controlled keystore and final review |

The optional training backend requires separate deployment and configuration. A successful APK build does not validate model compatibility, offline speech packs, device performance or backend availability.

## Contributing

Contributions are welcome in areas such as runtime compatibility, accessibility, security, documentation and automated tests. Please keep offline-first behaviour explicit, verify model artifacts before loading, and document device-specific limitations rather than claiming unsupported capabilities.

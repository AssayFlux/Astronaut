# Astronaut

<p align="center">
  <img src="Assets/Icon.svg" alt="Astronaut app icon" width="156" />
</p>

<h3 align="center">Your privacy-first AI workspace for Android</h3>
<p align="center">
  <strong>Chat · Code · Understand · Work offline</strong>
</p>
<p align="center">
  <a href="https://github.com/AssayFlux/Astronaut/actions"><img src="https://img.shields.io/github/actions/workflow/status/AssayFlux/Astronaut/android-build.yml?branch=main&label=Android%20CI" alt="Android CI" /></a>
  <a href="https://github.com/AssayFlux/Astronaut/blob/main/LICENSE"><img src="https://img.shields.io/github/license/AssayFlux/Astronaut" alt="License" /></a>
  <img src="https://img.shields.io/badge/Android-8.0%2B-3DDC84?logo=android&logoColor=white" alt="Android 8.0+" />
  <img src="https://img.shields.io/badge/version-0.1.0-6478FF" alt="Version 0.1.0" />
  <img src="https://img.shields.io/badge/Kotlin-Android-7F52FF?logo=kotlin&logoColor=white" alt="Kotlin" />
</p>

Astronaut is an Android-first AI application built around **on-device inference, private local data, and user-controlled model selection**. Run compatible language models on your device, keep conversations in an encrypted local database, and optionally connect to an OpenAI-compatible HTTPS service when you choose.

> **Project status:** active development. Some model formats are catalog/import formats only until their corresponding execution runtime is integrated. See [Current capabilities](#current-capabilities-and-limitations) before selecting a model.

## At a glance

| | |
|---|---|
| Android application ID | `org.astronaut.agi` |
| Current app version | `0.1.0` (versionCode `7`) |
| Minimum Android | Android 8.0 (API 26) |
| Build stack | Kotlin · Jetpack Compose · Room · SQLCipher |
| Local inference | LiteRT-LM |
| Optional retrieval | ONNX Runtime embeddings |
| Optional online mode | OpenAI-compatible HTTPS API |

## Features

- **On-device AI chat** — import and run compatible `.litertlm` models locally.
- **Private conversation storage** — Room database encrypted with SQLCipher and Android Keystore-managed key material.
- **Model management** — import, verify, load and unload local model artifacts; downloads support publisher-provided SHA-256 verification.
- **Document and image workflows** — ingest PDF, DOCX, text and image attachments; use on-device OCR and image labels, with multimodal model input when the selected model supports it.
- **Local knowledge retrieval** — encrypted attachment chunks, BM25 ranking and optional ONNX embedding-based hybrid retrieval.
- **Voice support** — Android on-device speech recognition and locally installed text-to-speech voices, subject to device language packs.
- **Developer workspace** — browse and edit bounded UTF-8 files in a user-selected project folder.
- **Optional online provider** — explicit opt-in to an OpenAI-compatible HTTPS endpoint; no silent cloud fallback.
- **Multi-model review** — sequential comparison across multiple compatible imported local models.

## Current capabilities and limitations

| Capability | Status |
|---|---|
| Local text chat with compatible LiteRT-LM models | Available |
| Encrypted local chat history | Available |
| OCR and image labels | Available through ML Kit |
| Multimodal image understanding | Requires a compatible imported model |
| ONNX embedding retrieval | Available for supported embedding models |
| Device speech recognition / TTS | Depends on Android provider and offline language packs |
| OpenAI-compatible online chat | Optional; user-configured and opt-in |
| GGUF / llama.cpp inference | Not integrated yet |
| Offline diffusion image generation | Not integrated yet |
| Whisper.cpp / Sherpa-ONNX inference | Not integrated yet |
| Full IDE, shell and integrated Git | Not implemented |

No model is bundled in the APK. A model file being smaller than the catalog's 15 GB artifact ceiling does **not** guarantee that it will fit in device memory or be supported by an installed runtime.

## Build the Android app

### Requirements

- JDK 17
- Android SDK Platform 36
- Android SDK Build-Tools 36.0.0
- Gradle 8.13
- Accepted Android SDK licenses

The repository currently includes Gradle wrapper properties but not the wrapper scripts/JAR. Use an installed Gradle 8.13 distribution until a complete wrapper is added.

```bash
java -version
gradle --version
gradle --no-daemon :app:testDebugUnitTest
gradle --no-daemon :app:assembleDebug
```

The debug APK is generated at:

```text
app/build/outputs/apk/debug/app-debug.apk
```

Install on a connected device:

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

You can also open the repository root in Android Studio, sync Gradle, select the `app` configuration and `debug` variant, then choose **Build > Build Bundle(s) / APK(s) > Build APK(s)**.

## Model and privacy notes

- Import a compatible model before using local inference; Astronaut does not bundle model weights.
- Local conversations and imported files remain on-device unless you explicitly use an online provider or otherwise share them.
- Online mode uses HTTPS and requires user configuration and opt-in. Review provider terms and privacy policies before sending prompts.
- Model compatibility depends on the actual file format, runtime, tokenizer, device memory and hardware acceleration support. The artifact-size limit is not a runtime guarantee.

## Optional training backend

The training control plane is a separately hosted Python service, not part of the Android APK. See [backend/README.md](backend/README.md) for the FastAPI, Celery, Redis and GPU-worker setup.

## CI and release signing

The **Android build** workflow runs JVM/unit tests. The **Signed Android release** workflow requires owner-managed signing secrets before it can produce a signed release APK.

See [docs/RELEASE_SIGNING.md](docs/RELEASE_SIGNING.md). Never commit a keystore or signing passwords to the repository.

## Documentation

- [Feature status and validation matrix](docs/FEATURE_STATUS.md)
- [Product architecture](docs/PRODUCT_ARCHITECTURE.md)
- [Offline feature notes](docs/OFFLINE_FEATURES.md)
- [Backend training architecture](docs/BACKEND_TRAINING_ARCHITECTURE.md)
- [APK build guide](docs/APK_BUILD_GUIDE.md)
- [Database encryption](docs/DATABASE_ENCRYPTION.md)
- [Release signing](docs/RELEASE_SIGNING.md)

## Contributing

Issues and pull requests are welcome. Please include device/Android details, model format and runtime information, reproduction steps, and relevant logs when reporting a bug.

## License

See [LICENSE](LICENSE).

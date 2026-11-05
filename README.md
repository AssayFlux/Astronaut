# Astronaut — Offline AI Messenger

**Android application ID:** `org.astronaut.agi`  
**Stack:** Kotlin, Jetpack Compose, Room + SQLCipher, LiteRT-LM, ML Kit  
**Minimum Android:** API 26 (Android 8.0)  
**Compile SDK:** 36 · **JDK:** 17 · **Gradle:** 8.13

Astronaut is an Android-first offline AI application. The Android app performs local inference with an imported LiteRT-LM model. The optional training control plane is a separately hosted Python service; it is not embedded in the APK.

## Current Android capabilities

- Local chat history and workspace conversations persisted in Room.
- SQLCipher-backed encrypted database with Android Keystore-managed key material and explicit legacy database migrations.
- Import, store, load and unload compatible `.litertlm` models in app-private storage.
- Local text generation through LiteRT-LM.
- Attachment ingestion for PDF (page-rendered OCR), DOCX, UTF-8 text and images.
- On-device ML Kit OCR and image-label classification. Image labels are not open-ended visual question answering.
- Local attachment retrieval with BM25-style lexical ranking and source/chunk labels. This is not embedding/vector semantic search.
- Offline speech recognition through Android's on-device recognizer (Android 12+ and installed offline language pack) and locally installed TTS voices.
- HTTPS model download with publisher-supplied SHA-256 verification, bounded redirects and file-size limits.
- Sequential local multi-model review when at least two compatible local models are imported. Models run one after another to reduce peak RAM use.
- Dataset JSONL validation and a training screen for connecting to the optional training API.
- Local project/workspace records and project-scoped assistant conversations.

## Important limitations

- No language model is bundled in the APK. Import a compatible `.litertlm` model before using local chat.
- The app's image feature provides OCR and object/category labels, not visual question answering, image caption generation or chart reasoning.
- Retrieval is offline lexical BM25 ranking over stored attachment text, not neural embedding search.
- Offline voice depends on the Android device's on-device speech service and downloaded offline voice/language packs.
- Collaboration requires two distinct compatible local models and is sequential, not parallel autonomous agents.
- The training backend is an optional self-hosted GPU service. It supports LoRA/QLoRA adapter training on a configured CUDA worker; exported PEFT adapters are not directly importable LiteRT-LM models and require a separate conversion/export step.
- Workspace support is not a full IDE: there is no integrated source editor, arbitrary shell, Git client or autonomous Android build environment inside the phone app.
- CI and unit tests cannot replace physical-device testing, especially for LiteRT model compatibility, offline speech availability, memory pressure and SQLCipher migration behavior.

## Build the APK

### Local build

Requirements: JDK 17, Android SDK platform 36 and build-tools 36.0.0, and Gradle 8.13.

```bash
gradle --no-daemon :app:testDebugUnitTest
gradle --no-daemon :app:assembleDebug
```

Debug APK:

`app/build/outputs/apk/debug/app-debug.apk`

### Build through GitHub Actions

1. Open the repository's **Actions** tab.
2. Select **Android build**.
3. Choose **Run workflow** and select the branch.
4. Wait for the **Unit tests and APKs** job to finish successfully.
5. Open the run's **Artifacts** section and download `astronaut-debug-apk`.
6. Extract the ZIP; install `app-debug.apk` on your Android device.

The workflow runs unit tests, assembles the debug APK and validates the release variant. It also uploads an unsigned release artifact when Gradle produces it.

## Signed release APK

A release signature belongs to the app owner. Create and protect your own Android keystore; never commit it to Git. Configure these GitHub Actions repository secrets before running the **Signed Android release** workflow:

- `ANDROID_KEYSTORE_BASE64`: base64-encoded keystore file
- `ANDROID_KEYSTORE_PASSWORD`
- `ANDROID_KEY_ALIAS`
- `ANDROID_KEY_PASSWORD`

See [docs/RELEASE_SIGNING.md](docs/RELEASE_SIGNING.md). Without those secrets, use the debug APK for testing; do not distribute an unsigned release as a production build.

## Optional GPU training backend

See [backend/README.md](backend/README.md) for the self-hosted FastAPI + Celery + Redis setup, API-key configuration, local model directory requirements, CUDA worker setup and adapter export limitations.

## Project documentation

- [Feature status and validation matrix](docs/FEATURE_STATUS.md)
- [Product architecture](docs/PRODUCT_ARCHITECTURE.md)
- [Offline feature notes](docs/OFFLINE_FEATURES.md)
- [Backend training architecture](docs/BACKEND_TRAINING_ARCHITECTURE.md)
- [APK build guide](docs/APK_BUILD_GUIDE.md)
- [Database encryption](docs/DATABASE_ENCRYPTION.md)
- [Release signing](docs/RELEASE_SIGNING.md)

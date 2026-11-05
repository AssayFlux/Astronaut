# Astronaut — Offline AI Messenger

**Android application ID:** `org.astronaut.agi`  
**App version:** `0.1.0` (release tag: `v0.1.0`; Android versionCode: `7`)  
**Stack:** Kotlin, Jetpack Compose, Room + SQLCipher, LiteRT-LM, ML Kit  
**Minimum Android:** API 26 (Android 8.0)  
**Compile SDK:** 36 · **JDK:** 17 · **Gradle:** 8.13

Astronaut is an Android-first offline AI application. The Android app performs local inference with an imported LiteRT-LM model. The optional training control plane is a separately hosted Python service; it is not embedded in the APK.

## Current Android capabilities

- Local chat history and workspace conversations persisted in Room.
- SQLCipher-backed encrypted database with Android Keystore-managed key material and explicit legacy database migrations.
- Import, store, load and unload compatible `.litertlm` models in app-private storage.
- Local text generation through LiteRT-LM.
- Attachment ingestion for PDF, DOCX, UTF-8 text and images.
- On-device ML Kit OCR and image labels, with a LiteRT-LM multimodal image-input path for compatible imported models.
- Local attachment retrieval with encrypted chunks, lexical BM25 ranking and optional ONNX WordPiece embeddings/hybrid ranking when a compatible embedding model and vocabulary are supplied.
- Offline speech recognition through Android's on-device recognizer and locally installed TTS voices; availability depends on device services and offline language packs.
- HTTPS model download with publisher-supplied SHA-256 verification, bounded redirects and resumable transfers when the server supports HTTP Range.
- Sequential local multi-model review when at least two compatible local models are imported.
- Dataset JSONL validation and a training screen for connecting to the optional training API.
- SAF-scoped Developer Studio for browsing a user-selected project folder and editing bounded UTF-8 text files.

## Important limitations

- No language model or embedding model is bundled in the APK. Import compatible model artifacts before using local chat or semantic retrieval.
- Multimodal image understanding requires a compatible user-imported LiteRT-LM model; OCR and image labels are separate on-device features.
- Offline voice depends on Android's speech provider and downloaded offline language/voice packs; OEM behavior varies.
- Multi-model review is sequential, not parallel autonomous agents.
- The optional training backend is a self-hosted GPU service. PEFT adapters are not directly importable LiteRT-LM models and need a separately selected/validated conversion tool.
- Developer Studio is not a full IDE: integrated Git operations, arbitrary shell execution and remote build/test orchestration are not implemented.
- Unit tests do not replace physical-device testing for model compatibility, offline speech, memory pressure, image understanding or SQLCipher migration behavior.

## Build the debug APK locally

### Prerequisites

- JDK 17
- Android SDK Platform 36
- Android SDK Build-Tools 36.0.0
- Gradle 8.13
- Android SDK licenses accepted

The repository currently contains `gradle-wrapper.properties` but does **not** contain the Gradle wrapper scripts or wrapper JAR. Therefore use an installed Gradle 8.13 command (or import the project into Android Studio with a compatible Gradle installation); `./gradlew` will not work until a complete wrapper is added.

Check your Java and Gradle versions:

```bash
java -version
gradle --version
```

From the repository root, run:

```bash
gradle --no-daemon :app:testDebugUnitTest
gradle --no-daemon :app:assembleDebug
```

The APK is created at:

```text
app/build/outputs/apk/debug/app-debug.apk
```

To install on a connected Android device with USB debugging enabled:

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

In Android Studio, open the repository root, allow Gradle sync to finish, select the `app` configuration and the `debug` build variant, then use **Build > Build Bundle(s) / APK(s) > Build APK(s)**.

## CI behavior

The **Android build** GitHub Actions workflow runs JVM/unit tests only. It intentionally does not assemble or upload APKs. The separate **Signed Android release** workflow assembles a signed release APK only when the owner has configured the required signing secrets.

## Signed release APK

A release signature belongs to the app owner. Create and protect your own Android keystore; never commit it to Git. Configure these GitHub Actions repository secrets before running the **Signed Android release** workflow:

- `ANDROID_KEYSTORE_BASE64`: base64-encoded keystore file
- `ANDROID_KEYSTORE_PASSWORD`
- `ANDROID_KEY_ALIAS`
- `ANDROID_KEY_PASSWORD`

See [docs/RELEASE_SIGNING.md](docs/RELEASE_SIGNING.md). Use the debug APK for initial testing; do not distribute an unsigned release as a production build.

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

# Astronaut — Offline AI Messenger

Android application ID: org.astronaut.agi · Kotlin · Jetpack Compose · Room · API 26+.

## Existing app milestone
- Persistent local conversations and message history.
- Import .litertlm models into app-private storage.
- Run local text generation through Google's LiteRT-LM runtime on the CPU.
- Load/unload an imported model from the Models screen.
- Local project list / workspace.

## Offline feature services added on feat/offline-multimodal-platform
See docs/OFFLINE_FEATURES.md and docs/FEATURE_STATUS.md.

The new service layer provides offline OCR-based document/image text extraction (PDF, DOCX, UTF-8 text), local lexical retrieval, Android speech recognition/TTS wrappers, verified model-download helpers, JSONL dataset inspection, and a sequential multi-provider collaboration engine.

These are integration-ready services, not a claim that every capability is now exposed end-to-end in the UI. OCR is not semantic image understanding; local lexical search is not embedding-based RAG; dataset inspection is not fine-tuning; and collaboration requires multiple registered real providers.

## First-run model setup
The APK does not bundle a language model. Import a compatible .litertlm model in Astronaut > Models. Once loaded, text generation runs locally.

Recommended starter model: Qwen3-0.6B LiteRT-LM (see its Hugging Face model card and license). A 0.6B model is a starter; speed and memory use depend on the phone.

## Build
Requirements: JDK 17, Android SDK platform 35, Gradle 8.13.

    gradle :app:testDebugUnitTest :app:assembleDebug

APK: app/build/outputs/apk/debug/app-debug.apk.

## Validation
Service unit tests are included for lexical retrieval and JSONL dataset inspection. PDF OCR, speech recognition, model downloads and actual LiteRT inference still require Android runtime/device testing. No release signing or database encryption-at-rest is claimed.

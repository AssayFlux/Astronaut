# Repository audit — 2026-11-05

This audit supersedes earlier snapshots in this file. It reflects the current `main` tree inspected on 2026-11-05 and the Android/backend CI workflows. It distinguishes code presence from verified runtime behavior.

## Repository and release identity

- Android application ID: `org.astronaut.agi`.
- Android Gradle namespace: `org.offlineai.messenger` (internal Kotlin package namespace; it does not change the installed application ID).
- Intended initial product release: `v0.1.0`.
- Android `versionCode` is monotonic. It is advanced to 7 while setting `versionName` to `0.1.0`, so users with the previously built code 6 APK can install this corrected version as an upgrade. Do not reset versionCode to 1 after distributing code 6.
- No model weights are bundled in the APK.
- The Gradle wrapper scripts are not committed. GitHub Actions installs Gradle 8.13 explicitly; local builds need a compatible Gradle installation.

## Implemented Android capabilities

| Area | Current implementation | Important boundary |
|---|---|---|
| UI | Jetpack Compose screens for chats, AI Hub, models, training, workspace and settings | Not a full IDE |
| Chat | Room persistence and conversation-scoped history | Verify upgrade/restore on physical devices |
| Local database | SQLCipher with Keystore-managed key material and explicit migrations | Device migration, recovery and backup behavior still require testing |
| Inference | LiteRT-LM runtime for imported local `.litertlm` models | Model format/runtime compatibility and memory limits require device testing |
| Model acquisition | Import flow and HTTPS download with SHA-256 verification | No signed curated catalog; user must trust the URL/checksum source |
| Documents | PDF page OCR, DOCX extraction, bounded text reads | OCR does not recover all layouts/tables reliably |
| Images | ML Kit OCR and image labels plus LiteRT-LM multimodal Content.ImageFile path with bounded image preprocessing | Requires a compatible user-designated multimodal .litertlm artifact; physical-device image reasoning and memory validation remain outstanding |
| Retrieval | BM25 fallback plus optional ONNX WordPiece embeddings, hybrid cosine/BM25 ranking, encrypted persistent chunks and source/page labels | Requires a compatible user-imported ONNX model and matching vocab; validate model compatibility, quality and device performance |
| Voice | Android speech-recognition and TTS integration | Offline support depends on device speech services and installed offline language/voice packs |
| Collaboration | Sequential two-model review and coordinator synthesis | Requires multiple compatible imported models; not parallel autonomous agents |
| Training UI | Dataset inspection and training API client/status UI | Requires separately deployed backend |
| Workspace | Project records and project-scoped assistant conversations | No integrated source editor, Git client, shell or build/test agent |

## Optional training backend

- FastAPI control plane with API-key authentication for dataset and job endpoints.
- JSONL upload validation, dataset hashes, job records, job status/cancellation and artifact download.
- Optional Celery/Redis worker and CUDA-only PEFT LoRA/QLoRA training code path.
- The first trainer hardcodes Llama-style `q_proj` and `v_proj` target modules; other model families require explicit support.
- Training output is a PEFT adapter plus manifest. It is not a LiteRT-LM model and cannot be imported directly into the Android app.
- The backend is self-hosted and is not part of the Android APK.

## Build and test evidence

- Android CI workflow runs JVM unit tests only; APK assembly and installation are intentionally manual.
- Backend CI runs pytest.
- A successful GitHub Actions build proves compilation and the tests that ran; it does not prove offline inference, speech, migration, camera/OCR quality, thermal behavior or low-memory stability on a phone.
- Production signing requires the owner's keystore and GitHub Actions secrets.

## Outstanding engineering work

1. Semantic RAG validation: test the new ONNX embedding path against known MiniLM-compatible exports, improve tokenizer/model coverage, add retrieval quality benchmarks and strengthen source citations.
2. Vision validation: verify supported multimodal .litertlm artifacts, benchmark image Q&A quality, and measure memory/thermal behavior on target devices.
3. Offline voice independence: managed ASR/TTS model assets and explicit offline-engine availability checks instead of relying solely on system speech services.
4. Model ecosystem: signed catalog, provenance, runtime compatibility metadata, model license display, safe updates and rollback.
5. Training-to-mobile pipeline: architecture-aware LoRA targets, evaluation, adapter merge/conversion, quantization and validated LiteRT-LM export.
6. Developer Studio: project file browser/editor, explicit user-approved writes, Git status/diff/commit, and controlled build/test execution.
7. Production readiness: instrumentation tests, migration/restore tests, security/privacy review, dependency/license review, crash/performance monitoring and a physical-device matrix.

## Audit caveats

This is a source and CI configuration audit, not a formal penetration test or a certification of runtime behavior. Treat the boundaries above as release blockers where they affect the intended product promise. Update this file when an implementation and its tests are actually merged; do not mark planned features as complete.

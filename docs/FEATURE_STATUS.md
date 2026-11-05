# Feature status

## Existing application
- [x] Android Gradle/Kotlin/Compose skeleton
- [x] Room entities and local conversation persistence
- [x] LiteRT-LM text runtime integration (physical-device validation pending)
- [x] Local model import and load/unload
- [x] Local project workspace
- [x] Task classification and collaboration contracts

## Added offline feature services (this branch)
- [x] PDF page rendering + offline OCR extraction
- [x] Image OCR using bundled ML Kit Latin text recognizer (not semantic vision)
- [x] DOCX and UTF-8 text extraction
- [x] Local lexical RAG chunking and retrieval (not vector embeddings)
- [x] Android speech input/output wrappers (offline packs/device validation pending)
- [x] HTTPS model download helper with license gate and SHA-256 verification
- [x] JSONL dataset validation and deterministic split report
- [x] Sequential multi-provider collaboration runner (provider registration/UI integration pending)
- [x] Unit tests for local retrieval and dataset validation

## Still not complete
- [ ] Integrate document/image extraction and RAG into chat composer, persistence and model prompt context
- [ ] Semantic image understanding / VQA model runtime
- [ ] Audio recording UI, runtime microphone permission flow, playback controls and offline pack validation
- [ ] Model catalog UI, signed catalog, download progress/cancel/retry and automatic checksum-to-import workflow
- [ ] Register multiple real local model providers and wire collaboration into chat with peer review/synthesis
- [ ] Streaming UI, stop/retry/regenerate
- [ ] Persistent embedding/vector store and citation-aware RAG
- [ ] Actual LoRA/QLoRA training executor and training dashboard; current dataset inspector does not train
- [ ] Database encryption-at-rest and Keystore key lifecycle
- [ ] Production release signing, backup/security review
- [ ] CI build, unit/instrumentation test results and physical-device verification

Unchecked features are not complete. Service-layer code is not equivalent to a user-facing, end-to-end feature.

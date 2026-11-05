# Feature status and validation matrix

This file distinguishes implemented code from behavior that still requires hardware or external infrastructure. A checked item means the code path exists; it does not imply device certification.

## Android app

| Capability | Code status | Remaining validation / limitation |
|---|---|---|
| Kotlin/Compose Android app, ID `org.astronaut.agi` | Implemented | Build and install on target devices |
| Persistent chats and workspace conversations | Implemented | Exercise restore, deletion and upgrade paths on device |
| SQLCipher database and Keystore key management | Implemented | Test fresh install, legacy migration, interrupted migration and backup/restore |
| LiteRT-LM model import/load/unload and local text inference | Implemented | Test supported model files, token generation, low-RAM behavior and lifecycle on device |
| PDF, DOCX, text and image attachment extraction | Implemented | OCR accuracy and large-file behavior require device tests |
| Image OCR and ML Kit image labels | Implemented | Not visual Q&A, captioning, chart reading or diagram reasoning |
| Local attachment retrieval | Implemented (lexical BM25) | Not neural embedding/vector semantic search; source labels are chunk-level |
| Offline speech input and TTS wrappers/UI | Implemented | Requires Android 12+ on-device recognizer and installed offline language/TTS voices |
| SHA-256 verified HTTPS model download | Implemented | User must obtain checksum from a trusted publisher; no signed curated catalog |
| Sequential two-model review | Implemented | Requires two compatible imported models; sequential execution, not parallel agents |
| Dataset validation and training API client/UI | Implemented | Training API must be configured and reachable |
| Workspace/project assistant | Implemented | Not a full IDE, shell, Git client or safe autonomous build agent |

## Optional training backend

| Capability | Code status | Remaining validation / limitation |
|---|---|---|
| Authenticated FastAPI dataset/job API | Implemented | Self-host and configure API key/TLS/firewall |
| JSONL validation and dataset registration | Implemented | Operational quotas, retention and multi-user authorization remain |
| Celery/Redis GPU worker | Implemented | Must be deployed on a compatible CUDA host |
| LoRA/QLoRA adapter training | Implemented code path | Validate model architecture, GPU/VRAM, dependency versions and real training run |
| Adapter artifact and manifest | Implemented | PEFT adapter is not a directly importable `.litertlm`; conversion pipeline remains separate |

## Build and release

- [x] Android unit-test and debug-APK GitHub Actions workflow
- [x] Release-variant assembly validation in CI
- [x] Manual workflow dispatch for APK builds
- [x] Debug APK artifact upload
- [x] Separate signed-release workflow
- [ ] Owner-provided keystore and GitHub signing secrets
- [ ] Physical-device test matrix and performance profiling
- [ ] Final production security/privacy review and release sign-off

## Remaining product work (not represented as completed)

1. Neural embedding model + persistent vector index for semantic RAG.
2. A genuine local vision-language model integration for image Q&A, charts and diagrams.
3. Broader offline ASR/TTS model management beyond Android system speech services.
4. Signed model catalog with compatibility metadata, provenance and automatic update policy.
5. Adapter conversion/export into a runtime-compatible LiteRT-LM artifact.
6. Full Developer Studio: constrained project file browser/editor, Git operations, build/test orchestration and explicit user approval for writes/commands.
7. More robust app instrumentation tests, migration tests, model compatibility tests and device performance tests.

**Do not describe the items above as completed until their implementation and validation are present.**

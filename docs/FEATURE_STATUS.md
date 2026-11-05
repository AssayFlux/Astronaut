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
| Image OCR and ML Kit labels plus LiteRT-LM multimodal image input | Implemented code path | Requires a user-designated compatible multimodal .litertlm model; physical-device vision validation remains outstanding |
| Local attachment retrieval | BM25 fallback plus optional ONNX WordPiece embeddings, hybrid cosine/BM25 ranking, encrypted persistent chunks and page labels | Requires a compatible user-imported ONNX embedding model + matching vocab; model compatibility and quality need device validation |
| Offline speech input/output, language-pack discovery/download controls (Android 13+), and offline TTS voice selection | Implemented code path | Platform speech provider and local packs vary by OEM; physical airplane-mode and voice-quality validation remain outstanding |
| SHA-256 verified HTTPS model download | Implemented | Publisher checksum must come from a trusted source; existing downloader does not yet support resumable partial files |
| Signed model catalog parser, metadata validation, device compatibility checks, immutable versioned install store and rollback primitives | Implemented service layer | Publisher public key provisioning and Compose catalog browsing/install UI are not wired yet; actual model runtime compatibility requires device tests |
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
| Adapter artifact and manifest | Implemented | PEFT adapter is not a directly importable `.litertlm` |
| Mobile export packaging and provenance manifest | Implemented | Packages an artifact produced by an external converter; conversion algorithm and tensor/runtime validation remain external |

## Build and release

- [x] Android unit-test GitHub Actions workflow (APK assembly is manual)
- [ ] Manual debug/release APK assembly and installation by the owner
- [ ] Production signing with owner-provided keystore and CI secrets
- [x] Separate signed-release workflow
- [ ] Physical-device test matrix and performance profiling
- [ ] Final production security/privacy review and release sign-off

## Remaining product work (not represented as completed)

1. Validate the new ONNX semantic RAG path with a known MiniLM-compatible model on physical devices; add more tokenizer/model export variants and quality benchmarks.
2. Validate the LiteRT-LM vision path with supported multimodal model artifacts; benchmark image Q&A for charts, diagrams, screenshots and photographs on target devices.
3. Validate platform-managed offline ASR language packs and TTS voice selection on target devices; consider bundled independent ASR/TTS runtimes only if device coverage requires them.
4. Signed model catalog with compatibility metadata, provenance and automatic update policy.
5. Adapter conversion/export into a runtime-compatible LiteRT-LM artifact.
6. Full Developer Studio: constrained project file browser/editor, Git operations, build/test orchestration and explicit user approval for writes/commands.
7. More robust app instrumentation tests, migration tests, model compatibility tests and device performance tests.

**Do not describe the items above as completed until their implementation and validation are present.**

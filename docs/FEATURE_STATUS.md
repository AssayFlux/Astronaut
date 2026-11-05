# Feature status and validation matrix

This file distinguishes implemented code from behavior that still requires hardware or external infrastructure. A checked item means the code path exists; it does not imply device certification.

## Android app

| Capability | Code status | Remaining validation / limitation |
|---|---|---|
| Kotlin/Compose Android app, ID `org.astronaut.agi` | Implemented | Build and install on target devices |
| Persistent chats and workspace conversations | Implemented | Exercise restore, deletion and upgrade paths on device |
| SQLCipher database and Keystore key management | Implemented | Test fresh install, legacy migration, interrupted migration and backup/restore |
| LiteRT-LM model import/load/unload and local text inference | Implemented, with Android memory-pressure unload handling | Test supported model files, token generation, low-RAM behavior and lifecycle on device |
| PDF, DOCX, text and image attachment extraction | Implemented | OCR accuracy and large-file behavior require device tests |
| Image OCR and ML Kit labels plus LiteRT-LM multimodal image input | Implemented code path | Requires a user-designated compatible multimodal .litertlm model; physical-device vision validation remains outstanding |
| Local attachment retrieval | BM25 fallback plus optional ONNX WordPiece embeddings, hybrid cosine/BM25 ranking, encrypted persistent chunks and page labels | Requires a compatible user-imported ONNX embedding model + matching vocab; model compatibility and quality need device validation |
| Offline speech input/output, language-pack discovery/download controls (Android 13+), and offline TTS voice selection | Implemented code path | Platform speech provider and local packs vary by OEM; physical airplane-mode and voice-quality validation remain outstanding |
| SHA-256 verified HTTPS model download | Implemented with persistent Range resume, cancellation-aware transfer/hash loops and checksum verification | Resume depends on server Range support; interrupted, cancelled and resumed transfers still need device/network testing |
| Signed model catalog, device compatibility checks, immutable versioned install store | Implemented and connected to Models UI; Ed25519 verification uses an API 26-compatible provider | Publisher must generate/protect a real signing key and configure Gradle properties; automatic background update checks remain; rollback UI is implemented but needs real-version validation; actual model runtime compatibility requires device tests |
| Sequential two-model review | Implemented | Requires two compatible imported models; sequential execution, not parallel agents |
| Dataset validation and training API client/UI | Implemented | Training API must be configured and reachable |
| Developer Studio | Implemented SAF-scoped project folder browser and explicit UTF-8 text editor with bounded file sizes | Git operations, build/test orchestration and on-device shell execution are not implemented |

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

- [x] Android JVM unit-test GitHub Actions workflow\n- [x] Android emulator instrumentation smoke tests in CI\n- [x] Debug APK assembly and artifact upload in CI\n- [x] Backend pytest CI workflow
- [x] Security/privacy source checklist and model publisher key lifecycle documentation
- [ ] Manual physical-device APK installation and validation by the owner
- [ ] Production signing with owner-provided keystore and CI secrets
- [x] Separate signed-release workflow
- [ ] Physical-device test matrix and performance profiling
- [ ] Final production security/privacy review and release sign-off

## Remaining product work (not represented as completed)

1. Validate the new ONNX semantic RAG path with a known MiniLM-compatible model on physical devices; add more tokenizer/model export variants and quality benchmarks.
2. Validate the LiteRT-LM vision path with supported multimodal model artifacts; benchmark image Q&A for charts, diagrams, screenshots and photographs on target devices.
3. Validate platform-managed offline ASR language packs and TTS voice selection on target devices; consider bundled independent ASR/TTS runtimes only if device coverage requires them.
4. Generate and securely provision the publisher Ed25519 key, publish a signed catalog, and configure ASTRONAUT_CATALOG_URL / ASTRONAUT_CATALOG_PUBLIC_KEY for release builds.
5. Add background catalog update checks; validate the implemented install/rollback UI and resumable downloads against real range-capable hosts.
6. Integrate a supported PEFT-to-LiteRT-LM converter when a compatible, licensed converter is selected; validate converted artifacts and inference.
7. Extend Developer Studio with authenticated Git operations and remote build/test orchestration; keep command execution behind explicit approvals and a trusted build runner.
8. Add instrumentation, migration, model compatibility and device performance tests; complete physical-device QA and release security review.

**Do not describe the items above as completed until their implementation and validation are present.**

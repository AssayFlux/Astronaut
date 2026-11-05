# Step 1 validation matrix

This document separates automated repository checks from behavior that still requires the owner's physical Android device.

## Automated checks

| Check | Coverage | What it proves |
|---|---|---|
| Android JVM tests | Catalog signature gating and metadata validation, routing, collaboration, document/RAG utilities, voice contracts and API clients | Pure/unit-level behavior for covered cases |
| Android instrumentation smoke tests | Installed package identity, app-private storage, disabled platform backup and core manifest permission declarations | Basic packaged-app invariants on an Android emulator |
| Android debug assembly | `:app:assembleDebug` | The debug APK compiles and packages in CI |
| Backend pytest | FastAPI authentication, JSONL upload/listing, job gating, path containment and input validation | Covered backend API behavior in CI |
| Signed release workflow | Required keystore secrets, release build, `apksigner verify`, package ID and version identity | The release artifact is signed and carries the expected app identity when owner secrets are configured |

## Model catalog and downloads

- Catalog envelopes are bounded and signature-gated before metadata parsing.
- Model IDs, versions, HTTPS URLs, file names, format/runtime pairs, checksum syntax, capability sets, architecture, quantization, Android API, RAM and artifact size are validated.
- Catalog and manual downloads require HTTPS and a caller-provided SHA-256. Embedded URL credentials and fragments are rejected.
- Range resume is conditional on server support. Cancellation is checked during transfer and checksum reads; partial downloads remain private for later resume.
- A model may be downloadable while its execution runtime is unavailable. UI status must not imply that unsupported formats are runnable.
- Model installation and checksum work are moved off the UI thread; the active LiteRT-LM engine is unloaded when Android reports running-low memory pressure.

## Not certified by CI

The following require physical-device tests with actual model artifacts:

1. LiteRT-LM model loading, prompt formatting, token streaming, context behavior and clean unload/reload.
2. Real catalog/manual downloads, interrupted Range resume, cancellation, low storage and checksum mismatch.
3. Multimodal image understanding with a publisher-confirmed vision model.
4. PDF/DOCX extraction, OCR accuracy, large-file limits and source/page attribution.
5. ONNX tokenizer compatibility and retrieval quality against a labelled test set.
6. Offline speech recognition and TTS in airplane mode with installed device language packs.
7. SQLCipher fresh install, migration, interrupted migration, key invalidation and recovery.
8. Peak RAM, storage pressure, battery drain, startup latency, token throughput and thermal throttling on the target phone.
9. Release keystore ownership, backup, certificate continuity and production privacy/security sign-off.

## Release identity

- Application ID: `org.astronaut.agi`
- Version name: `0.1.0`
- Version code: `7`

A passing CI run is a build/test gate, not a claim that every model or optional feature works on every Android device.

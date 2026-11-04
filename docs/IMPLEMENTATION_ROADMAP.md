# Astronaut — Implementation Roadmap and Acceptance Gates

## Phase 0 — Architecture and build baseline
- Confirm Gradle/AGP/Kotlin/JDK/SDK compatibility.
- Establish CI for lint, unit tests and debug APK.
- Add version catalog, dependency locking and Room schema export.
- Split into modules incrementally, not as empty placeholders.
Acceptance: clean checkout builds reproducibly and tests pass.

## Phase 1 — Real messenger vertical slice
- Room-backed conversation/message repositories.
- ViewModels and lifecycle-aware Compose state.
- Create/select/rename/archive/delete conversations.
- Message send/edit/delete/copy/reply and search.
- Empty/loading/error states and robust process-death recovery.
Acceptance: UI tests plus database tests prove messages survive app restart.

## Phase 2 — Local model inference
- Runtime adapter API with cancellation, streaming Flow, load/unload and health.
- First llama.cpp Android integration using official Android binding/native build.
- Model import and GGUF metadata validation; checksums and licenses.
- Model loading diagnostics and memory budget.
Acceptance: a real installed model generates a response offline on target hardware.

## Phase 3 — Orchestration and collaboration
- Capability taxonomy and task classifier.
- Model selection based on real runtime probes and resource budget.
- Two distinct independent participants; peer review; coordinator synthesis.
- Persist per-stage trace, durations, failures and disagreements.
Acceptance: integration tests prove two real participants executed; unavailable paths report unavailable without fake success.

## Phase 4 — Attachments and knowledge
- SAF multi-select and persistable URI grants.
- Format-specific parsers with explicit supported/unsupported outcomes.
- Local indexing, chunking, embeddings and citations to source pages/locations.
- Image/audio/video processing only where a compatible local model exists.
Acceptance: fixture-based parser tests and airplane-mode end-to-end tests.

## Phase 5 — Developer Studio
- Project/folder/file data model and safe file operations.
- Editor and syntax-aware preview.
- API client with environments, secret redaction and request history.
- Git integration only after explicit credential setup.
- Optional remote development agent for builds/containers; never pretend a phone shell is a production build farm.
Acceptance: project persistence, safe export, API request tests and secret-redaction tests.

## Phase 6 — Training Studio and backend
- FastAPI control plane, database migrations, auth and object storage.
- Dataset upload, preview, validation and deterministic splits.
- Durable queue and isolated GPU worker.
- First SFT + LoRA recipe, evaluation, packaging and signed manifest.
- Android job monitoring and artifact install.
Acceptance: integration test submits a real small training job on a supported GPU runner, evaluates it, packages an adapter and installs/verifies the artifact.

## Phase 7 — Hardening and release
- Encryption, biometric lock, backup/restore, permissions review.
- Thermal/memory tests on Moto G54 Android 15.
- Offline, low-storage, interrupted download, worker crash and migration tests.
- Accessibility, performance and release signing.
Acceptance: published test report, APK checksum, reproducible build steps and explicit known limitations.

## Release discipline

A feature is complete only when implementation, automated tests, user-visible failure handling and device/worker validation exist. UI-only contracts are marked planned, not done. Keep each phase compiling and independently reviewable.

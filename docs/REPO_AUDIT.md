# Repository audit — 2026-11-05

This source audit reflects the repository state reviewed on 2026-11-05. It separates implemented code from features that still require external configuration or physical-device validation.

## Repository and release identity

- Android application ID: `org.astronaut.agi`.
- Android Gradle namespace: `org.offlineai.messenger` (internal Kotlin namespace; it does not change the installed application ID).
- Product version name: `0.1.0`; Android `versionCode`: `7`. Keep the version name at `0.1.0`; versionCode must remain monotonic for upgrades.
- Minimum Android API: 26; target API: 35; compile API: 36.
- Model weights are not bundled in the APK.
- Gradle 8.13 wrapper scripts and JAR are committed; local builds use the pinned wrapper distribution.

## Implemented Android capabilities and boundaries

| Area | Code present | Remaining boundary |
|---|---|---|
| UI | Jetpack Compose screens for chat, AI Hub, models, collaboration, training, workspace and settings | Modernized chat history selection, message surfaces, theme accents and speech selection labels; continue accessibility and physical-device layout validation |
| Chat and storage | Room persistence, conversation-scoped history, SQLCipher and Keystore-managed key material | Upgrade, interrupted migration, restore and key-recovery testing |
| Local inference | LiteRT-LM engine for local `.litertlm` models | Only compatible artifacts run; model load, memory, thermal and lifecycle behavior need device testing |
| Model acquisition | HTTPS download, resumable Range transfers, SHA-256 verification, local import | Test interruption/resume and checksum failures against real hosts |
| Signed model catalogue | Ed25519 signature verification, catalogue validation, device requirements, versioned installs and rollback UI | Publisher key and trusted catalogue URL/public key must be provisioned; no production catalogue is configured by default |
| Runtime ecosystem | Runtime adapter contracts and built-in LiteRT-LM / ONNX embedding adapters | A catalogue entry or accepted file extension does not mean its runtime is installed. GGUF/llama.cpp, diffusion/MNN, Whisper and Sherpa-ONNX execution are not bundled as general inference runtimes |
| Documents and images | PDF/DOCX/text extraction, OCR, image labels, bounded preprocessing and a LiteRT-LM multimodal image-input path | OCR layout fidelity and image Q&A require real file/model/device validation |
| Retrieval | BM25 fallback, optional ONNX WordPiece embeddings, hybrid ranking, encrypted chunks and source/page labels | Requires a compatible embedding model and matching vocabulary; quality benchmarks and device performance remain |
| Voice | Android speech recognition and TTS integration with offline-language/voice discovery | Actual offline support depends on OEM speech services and installed offline packs |
| Collaboration | Sequential two-model review and coordinator synthesis | Needs two compatible models; this is not parallel autonomous-agent execution |
| Developer Studio | User-selected SAF folder browser and bounded UTF-8 text editor | No integrated Git client, shell, build/test runner or full IDE |
| Training | Android dataset/status client and separate FastAPI/Celery training backend | Backend must be deployed separately; PEFT adapter output is not directly importable as a `.litertlm` model |

## Backend boundaries

- FastAPI control plane includes API-key authenticated dataset/job endpoints, JSONL validation, job status/cancellation and artifact download.
- Optional Celery/Redis worker contains a CUDA-oriented PEFT LoRA/QLoRA path.
- The current trainer uses Llama-style `q_proj` and `v_proj` targets; other architectures need explicit target-module support and validation.
- Mobile export packages an already-converted artifact with provenance metadata. It does not implement Google's LiteRT-LM converter or validate internal tensor semantics.
- The backend is self-hosted and is not part of the Android APK.

## Build and test evidence

- Android CI runs JVM unit tests, assembles a debug APK and uploads that APK as a workflow artifact.
- Backend CI runs pytest.
- CI compilation and unit tests do not certify inference, speech, migrations, OCR quality, thermal behavior, low-memory stability or device-specific compatibility.
- Production release signing requires an owner-controlled keystore and correctly provisioned CI secrets.

## Remaining engineering work

1. Configure and publish a real signed model catalogue; protect the Ed25519 private key and document key rotation/revocation.
2. Extend Android instrumentation coverage to database creation/migrations, model import/download, lifecycle and runtime permission flows; the current smoke tests cover application identity, private storage and declared permissions.
3. Validate LiteRT-LM text and multimodal models on target devices; profile peak RAM, startup, token speed, battery and thermal behavior.
4. Benchmark retrieval against a labelled query/document set; validate tokenizer compatibility and source attribution.
5. Test airplane-mode speech recognition and TTS on supported OEMs; decide whether a bundled offline speech engine is needed for broader coverage.
6. Complete a security/privacy and dependency-license review, including online-provider disclosure and secret handling.
7. Extend Developer Studio only with explicit user approval for file writes and any future Git/build execution.
8. Perform owner-run physical APK install/upgrade tests and release signing.

## Audit caveats

This is a source/configuration review, not a penetration test or runtime certification. A supported file extension, catalogue record or code path must not be presented as proof that a particular model or feature works on a given phone. Update this audit only when implementation and validation evidence exist.

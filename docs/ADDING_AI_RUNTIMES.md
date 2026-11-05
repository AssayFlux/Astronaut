# Adding AI engines and tools to Astronaut

Astronaut uses an adapter boundary so the chat UI and model catalogue do not need to know the implementation details of every AI engine.

## What the 15 GB limit means

The import policy is a maximum artifact size, not a promise that the phone can execute every artifact below that size. Android, the app, model weights, token/KV cache, image buffers and native runtime all share physical RAM. A model can pass the file-size check and still fail the peak-RAM check. The app must show the reason and must never silently switch to an online service.

The compatibility contract requires an estimated peak RAM value for on-device models. That estimate should be conservative and include runtime overhead and context/cache memory. Actual device tests remain authoritative.

## Supported task categories

- Text chat and coding
- Vision and image understanding
- Text-to-image and image-to-image generation
- Speech recognition and speech synthesis
- Embeddings, retrieval and reranking
- Classification and other task-specific inference

A task is available only when an installed adapter explicitly declares support for it.

## Model format to runtime mapping

| Artifact format | Typical runtime adapter | Workload |
|---|---|---|
| LiteRT-LM | Existing LiteRT-LM adapter | Supported local chat/multimodal models |
| GGUF | llama.cpp Android/JNI adapter | Quantized chat and coding |
| ONNX | ONNX Runtime Android adapter | Embeddings, vision, audio and classifiers supported by each graph |
| TFLite | LiteRT/TFLite adapter | Mobile vision/audio/classification |
| Safetensors / diffusion bundle | Dedicated diffusion adapter (for example MNN/NCNN where model support is verified) | Image generation |
| Whisper model / Sherpa-ONNX | Whisper.cpp or Sherpa-ONNX adapter | Speech recognition |
| OpenAI-compatible endpoint | Remote HTTP adapter | Optional online inference |

Implemented adapters in this branch: LiteRT-LM chat/vision wraps the existing LiteRT-LM runtime; ONNX embeddings wrap the existing MiniLM-compatible ONNX embedder and return a typed vector. The generic registry can discover these adapters. GGUF, diffusion, Whisper and Sherpa are recognized catalog formats but their execution engines are not yet bundled.

## Adapter implementation checklist

1. Implement `AiRuntimeAdapter` in `ai/plugins/AiToolContracts.kt`.
2. Give the adapter a stable `runtimeId`, supported artifact formats and supported task set.
3. Parse and validate a manifest; reject unknown formats, unsupported tasks, invalid SHA-256 values and unsupported Android API levels.
4. Verify downloaded/imported artifacts against their SHA-256 before loading.
5. Estimate peak RAM and compare it with current available memory. Treat this as a safety gate, not a guarantee.
6. Load one heavyweight on-device runtime at a time by default. Support cancellation and deterministic unload.
7. Stream progress/results, surface native errors, and record model/runtime versions and inference latency.
8. Add unit tests plus real-device tests for install, load, inference, cancellation, unload, process recreation and memory pressure.
9. Register the adapter in the app's trusted registry and expose only its declared capabilities in the UI.

## Security and distribution

A model file is data, never executable plugin code. Runtime adapters must be compiled into the signed app or delivered through a separately trusted, signed Android feature-module mechanism. Do not download and execute arbitrary JAR, DEX, native libraries or scripts from model URLs.

The OpenAI-compatible remote chat adapter and Online AI screen are implemented in this branch. It requires explicit opt-in, HTTPS, stores the API key as AES-GCM ciphertext with a non-exportable Android Keystore key, bounds response size, disables redirects, and never falls back automatically. The current chat integration sends only the current text prompt (plus the response-style system instruction); it does not send chat history, project notes, documents or attachments. Provider terms, free quotas and retention remain provider-controlled.

## Current implementation status

Implemented in this branch:
- Generic task, artifact, manifest and runtime adapter contracts plus a trusted in-app registry.
- LiteRT-LM adapter and ONNX embedding adapter for existing app engines.
- Android API/available-memory preflight helper, SHA-256 verification contract and 15 GB artifact cap.
- Signed catalog parsing and verified downloads for LiteRT-LM, GGUF, ONNX, TFLite, diffusion bundles and Whisper/Sherpa artifacts. Unsupported runtimes are visibly blocked from the current LiteRT-LM Load action.
- OpenAI-compatible online chat configuration, explicit online toggle, encrypted API key storage and no-history/no-attachment remote request policy.

Not yet implemented as executable on-device engines: llama.cpp/GGUF, MNN/NCNN diffusion image generation, Whisper.cpp and Sherpa-ONNX. Their artifact types can be represented/downloaded, but the app must not claim those files can run until native libraries/adapters, ABI packaging, model-specific metadata and real-device tests are completed. The compatibility checks are preflight gates, not guarantees that a model below 15 GB fits a 12 GB phone.

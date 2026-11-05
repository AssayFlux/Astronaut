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

These are adapter targets, not a claim that every runtime is already bundled. The existing LiteRT-LM and ONNX features remain the working baseline. Each additional runtime must be integrated and tested separately.

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

Remote adapters must be visibly marked Online, require explicit opt-in, use HTTPS, protect credentials with Android Keystore-backed storage, and disclose what prompt/history/files leave the device. No automatic online fallback is allowed.

## Current implementation status

The generic task/format/manifest/runtime contract and compatibility/SHA-256 checks have been added. This is the extension point for future engines; it does not itself bundle llama.cpp, a diffusion engine, Whisper or an online provider. Those require their own runtime dependencies, native ABI packaging, provider implementations, UI registration and device validation.

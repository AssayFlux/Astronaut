# Astronaut baseline and AI ecosystem pathway

## Baseline protection

The working repository baseline for this improvement series is the signed `main` commit:

- Repository: `AssayFlux/Astronaut`
- Baseline commit: `0509f75405335af58287b535f453b5f803f5095e`
- Baseline tree: `118d862022adb976d5fd787ef1c3a57465b55a1e`
- Android application ID: `org.astronaut.agi`
- Version: `0.1.0`

All improvements are developed on a feature branch from this baseline. Do not rewrite or replace the baseline. Preserve working functionality and keep versionName at 0.1.0 unless the owner requests otherwise.

## Immediate improvements in this branch

1. Chat deletion is exposed in the chat list with a confirmation dialog. The conversation's messages are deleted before its conversation row.
2. The selected local model path is persisted in app preferences. On process recreation, Astronaut looks up the imported model and reloads it into LiteRT-LM, reporting a clear error if restoration fails.
3. Personalization settings now include concise, balanced, or detailed response style and a dark appearance preference. Preferences remain local to the device.
4. Response style is added to the user's prompt context; it does not alter or fine-tune model weights.

## Multi-format integration update

The universal AI adapter contracts, trusted runtime registry, built-in LiteRT-LM and ONNX embedding adapters, Android memory/API preflight helper, multi-format signed catalog validation, and 15 GB verified-download ceiling are now present in the current feature branch. The model library blocks Load for artifacts whose runtime is not installed; it does not try to feed GGUF, diffusion or Whisper files into LiteRT-LM.

An OpenAI-compatible online chat provider is also connected to an Online AI screen. Online mode is explicit opt-in, HTTPS-only, uses an Android Keystore AES-GCM credential store, sends only the current text prompt and response-style instruction, and never silently falls back. It does not upload previous messages, project notes, documents or attachments.

Still requiring real engine work: llama.cpp/JNI for GGUF, a mobile diffusion backend for image generation, and Whisper.cpp/Sherpa-ONNX for dedicated offline ASR. A catalog entry or successful download is not proof that an engine can execute it.

## Why a loaded model can disappear

A model's weights and runtime engine live in the Android app process. Android can terminate that process under memory pressure, after a crash, or when reclaiming resources. No ordinary in-process reference can prevent process termination. Astronaut therefore persists the selected model identity and restores the runtime on the next process start. Do not claim that a model remains resident after Android has killed the process.

Keep the runtime loaded across normal chat turns while the process is alive. Do not instantiate and close a new runtime for each ordinary message. The existing `LiteRtModelProvider` currently owns a temporary runtime for sequential collaboration; keep that behavior isolated to collaboration until the provider lifecycle is redesigned.

## Target model/runtime architecture

Use a registry of capability-aware providers. Every installed model record must declare:

- artifact format and architecture;
- compatible runtime and runtime version;
- capabilities (chat, code, vision, audio, embeddings, image generation);
- tokenizer/chat template and context limit where applicable;
- quantization, license and source;
- SHA-256, file size, RAM estimate and minimum Android API;
- local/online classification and privacy boundary.

A file extension or model-family name is not proof of compatibility. Validate metadata, checksum and a small real inference smoke test before showing a model as ready.

### Runtime roadmap

| Order | Runtime / provider | Intended workloads | Integration rule |
|---|---|---|---|
| 1 | LiteRT-LM (existing) | Supported on-device chat and multimodal models | Preserve the working path; add model lifecycle and restore tests |
| 2 | llama.cpp Android | GGUF chat and coding models | Add native ABI packaging, JNI adapter, cancellation, streaming and RAM tests |
| 3 | ONNX Runtime (existing dependency) | Embeddings and supported vision/classification models | Keep task-specific adapters; validate each model's tensor/tokenizer contract |
| 4 | Diffusion provider (MNN/NCNN or another validated Android runtime) | Text-to-image and image-to-image | Separate image-generation capability and artifact format from chat models |
| 5 | Whisper.cpp / Sherpa-ONNX | Offline speech recognition | Integrate only after APK size, ABI and language-pack strategy is measured |
| 6 | Ollama-compatible remote/local endpoint | Optional external model server | Treat as an API endpoint, not as a native Android model format |

Do not add every native runtime in one change. Integrate one vertical slice at a time: artifact import -> compatibility -> provider -> UI selection -> inference -> cancellation/unload -> tests.

## Online model testing

Online providers must be explicitly opt-in and separate from offline mode. Start with a generic OpenAI-compatible chat-completions adapter so multiple providers can be tested without hardcoding one vendor. Required controls:

- provider base URL, model ID and API key;
- visible Online/Offline mode indicator on every conversation;
- explicit disclosure before sending a prompt or attachment off-device;
- HTTPS-only endpoints by default, with certificate validation;
- API keys stored using Android Keystore-backed encryption (never plain preferences or source code);
- request timeout, cancellation, bounded response size and clear error states;
- never silently fall back from offline to online;
- allow free-tier endpoints, but show provider rate limits, data policies and model availability as provider-controlled rather than promising free access.

Do not send private chat history or attachments to an online endpoint unless the user explicitly chooses to include them.

## Model comparison and best-result workflow

Support sequential model evaluation on memory-constrained phones:

1. User selects two or more compatible models and one task.
2. Run each model sequentially, unloading the previous engine only when needed.
3. Save each raw answer, model/version, runtime, latency and failure status.
4. Compare answers using a selected compatible judge model, or show a side-by-side comparison if no judge is available.
5. Never claim a consensus or "best answer" unless the comparison actually ran; preserve disagreements and uncertainty.
6. Let the user choose the final answer to copy into the conversation.

The default phone policy is one heavyweight model resident at a time. Add an explicit advanced setting only after device memory/thermal tests justify multi-resident inference.

## Required validation before merging

- Build debug APK with the repository's documented Gradle command.
- Install and launch on the owner's Dimensity 7050 / 12 GB RAM device.
- Create, switch, delete selected and delete non-selected conversations; verify messages are removed.
- Load a model, send several messages, background/foreground the app, and verify the model stays available while the process survives.
- Force-stop and relaunch; verify the selected model is reloaded from its saved path.
- Remove a saved model and verify the app clears stale active-model preferences.
- Test concise/balanced/detailed prompts and light/dark settings across app restart.
- Run airplane-mode tests to ensure online providers are never called implicitly.
- Run Android unit tests and review memory, thermal, cancellation and database behavior.

A code change is not device-certified until the above physical-device checks pass.

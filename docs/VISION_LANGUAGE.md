# Offline vision-language support

Astronaut can send image pixels to a compatible local LiteRT-LM multimodal model. This is separate from the existing ML Kit OCR and image-label hints.

## Requirements

- Import a local multimodal `.litertlm` bundle whose publisher explicitly documents image input (for example, a supported Gemma 3n multimodal export).
- In **Models**, select **Enable vision** on that model, then load it again. This opts the runtime into LiteRT-LM's vision backend.
- Attach a supported image in chat and ask a question. The app sends the prepared image file and prompt to LiteRT-LM using `Content.ImageFile` and `Content.Text`.
- Keep the model on device. No image, prompt, or chat content is sent to a server by this path.

A normal text-only model must not be marked as vision-capable. The app does not infer multimodal support from a filename or model family. A model that does not support the multimodal API will fail visibly when loaded or when image inference is attempted.

## Image safety and memory limits

- The selected content URI is decoded locally.
- The image is downsampled so its longest edge is at most 1536 pixels.
- The JPEG sent to the runtime is limited to 8 MiB; larger encoded inputs are rejected.
- Temporary prepared image files live in the app cache and are deleted after inference.
- Original attachment URI and extracted text/labels remain governed by the chat's attachment storage behavior.
- These are input bounds, not a guarantee that every model fits in device RAM. Model memory use, thermal behavior, latency, and output quality still require physical-device validation.

## Fallback behavior

When no vision-capable model is enabled, Astronaut continues text inference and supplies extracted OCR and ML Kit label hints where available. The prompt explicitly tells the text model that it has not visually inspected the image. This fallback is not image question answering.

## Runtime API

LiteRT-LM multimodal input uses:

```kotlin
Contents.of(
    Content.ImageFile(preparedImage.absolutePath),
    Content.Text(prompt)
)
```

The runtime config enables `visionBackend` only when the user explicitly marks a model as vision-capable. See the official [LiteRT-LM Android guide](https://developers.google.com/edge/litert-lm/android#multi-modality) for supported content types and model requirements.

## Validation still required

- Confirm the chosen .litertlm artifact is a supported image-input model.
- Run JVM tests and manually assemble/install the APK.
- Test airplane-mode image Q&A on target devices using small and large JPEG/PNG/HEIC files.
- Measure peak memory, cold-load time, first-token latency, sustained latency, thermal throttling, and process-death recovery.
- Test corrupt images, unsupported formats, model mismatch, cancellation, and low-storage conditions.
- Evaluate charts, diagrams, screenshots, and natural photographs against a fixed prompt/answer benchmark. Do not describe image reasoning as production-validated until these tests pass.

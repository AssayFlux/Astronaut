# Astronaut — Offline AI Messenger

Android application ID: `org.offlineai.messenger` · Kotlin · Jetpack Compose · Room · API 26+ · JDK 17.

## Implemented foundation

- Compose application with Chats, Models, Workspace, Training and Settings sections.
- Room persistence for conversations, messages, imported model-file references and workspace project metadata.
- Android document-picker workflows for message attachments and local model files.
- Local project create/delete workflow.
- Model capability, provider and two-model collaboration contracts.
- FastAPI control plane for JSONL dataset upload, structural validation, SHA-256 digest, dataset listing, training-job metadata and status.
- Optional Celery GPU worker with Transformers + PEFT LoRA/QLoRA training path, adapter output and manifest.
- Backend tests plus GitHub Actions workflows for backend tests and Android build/APK artifact.

## What is not complete yet

This is an actively developed foundation, **not yet a finished AI messenger**. The Android app does not yet bundle or connect a working llama.cpp or LiteRT-LM inference runtime, token streaming, real model downloads, attachment content extraction, an end-to-end multi-model execution pipeline, encrypted database storage or biometric lock. Imported model entries are file references, not proof of runtime compatibility.

The backend training worker is optional and requires a separate Linux/NVIDIA CUDA host, Redis, and the packages in `backend/requirements-training.txt`. Training is blocked unless both `ASTRONAUT_ENABLE_TRAINING=1` and `ASTRONAUT_REDIS_URL` are configured. The first trainer is a LoRA/QLoRA adapter MVP for JSONL data and common Llama-style `q_proj`/`v_proj` modules; it does not yet implement held-out evaluation, DPO, artifact signing, or production isolation/authentication. The API should remain localhost-only until security hardening is added.

No account sign-in, analytics upload, automatic chat upload or automatic backend connection is included. Chat data and metadata remain in the app's local Room database unless the user exports or shares them through Android.

## Android build

Requirements: JDK 17, Android SDK platform 35, Android build tools and Gradle 8.9-compatible.

```bash
gradle :app:testDebugUnitTest
gradle :app:assembleDebug
```

APK output: `app/build/outputs/apk/debug/app-debug.apk`. GitHub Actions also builds and uploads a debug APK artifact when its Android workflow succeeds.

## Backend development

```bash
cd backend
python -m venv .venv
. .venv/bin/activate
pip install -r requirements.txt
PYTHONPATH=. pytest -q
uvicorn astronaut_api.main:app --host 127.0.0.1 --port 8080
```

For the optional GPU worker, see [backend/README.md](backend/README.md).

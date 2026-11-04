# Astronaut — Offline AI Messenger

Android application ID: `org.offlineai.messenger` · Kotlin · Jetpack Compose · Room · API 26+ · JDK 17.

## Implemented in the current foundation

- Android Compose application with Chats, Models, Workspace, Training and Settings sections.
- Room persistence for conversations, messages, imported model-file references and workspace project metadata.
- File selection and persisted URI references for message attachments and local model files.
- Local project create/delete workflow.
- Model-provider and capability-routing contracts, plus guarded two-model collaboration orchestration contracts.
- FastAPI training control-plane foundation: JSONL dataset upload, structural validation, SHA-256 digest, dataset listing, training request metadata and job status.
- Backend tests and GitHub Actions workflow.

## Important limitations

This is an actively developed foundation, **not yet a complete AI messenger**. The current Android build does not include a working llama.cpp or LiteRT-LM inference runtime, token streaming, a real model downloader, full attachment parsing, encryption-at-rest, or a functioning multi-model execution pipeline. The model library records user-selected file references; it does not prove that a model can be loaded.

The training API currently validates and records datasets and job requests. It intentionally marks jobs `blocked` because a durable, isolated GPU training worker is not included yet. It does not claim that training has started or completed. Authentication, TLS deployment, durable queueing, worker isolation, quotas and signed model artifacts are required before production use.

No account sign-in, analytics upload, automatic chat upload or automatic backend connection is included. Chat data and metadata remain in the app's local Room database unless the user exports or shares them through Android.

## Android build

Requirements: JDK 17, Android SDK platform 35, Android build tools and Gradle 8.9-compatible.

```bash
gradle :app:testDebugUnitTest
gradle :app:assembleDebug
```

APK output: `app/build/outputs/apk/debug/app-debug.apk`.

## Backend development

```bash
cd backend
python -m venv .venv
. .venv/bin/activate
pip install -r requirements.txt
PYTHONPATH=. pytest -q
uvicorn astronaut_api.main:app --host 127.0.0.1 --port 8080
```

API docs are served at `/docs`. The development server should remain bound to localhost until authentication and TLS are configured.

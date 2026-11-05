# Astronaut Training API

Self-hosted API for JSONL dataset registration and optional LoRA/QLoRA training. The API is a control plane; the Celery worker performs GPU training on a separate machine.

## API development

```bash
python -m venv .venv
. .venv/bin/activate
pip install -r requirements.txt
uvicorn astronaut_api.main:app --host 127.0.0.1 --port 8080
```

## GPU worker setup

Use a Linux host with a compatible NVIDIA CUDA stack and install the optional ML dependencies:

```bash
pip install -r requirements-training.txt
export ASTRONAUT_DATA_DIR=/srv/astronaut-data
export ASTRONAUT_REDIS_URL=redis://127.0.0.1:6379/0
export ASTRONAUT_ENABLE_TRAINING=1
celery -A astronaut_api.worker.celery_app worker --loglevel=INFO
```

Start Redis and the API with the same data directory and environment variables. Both processes must see the same dataset and SQLite paths; for multiple workers or production, replace SQLite with PostgreSQL and use shared object storage.

The first trainer supports JSONL `text`, `messages`, and `prompt` records and PEFT LoRA/QLoRA adapters. It requires CUDA and uses common Llama-style `q_proj`/`v_proj` target modules; model families with different layer names need configuration changes. It saves an adapter and manifest, not a merged standalone model.

## Security

Bind the development API to localhost. Before remote deployment, add authentication, TLS, durable database migrations, worker isolation, resource quotas, audit logs, dataset retention controls, signed artifact manifests and per-user authorization. Do not expose Redis or the Celery worker to the public internet.

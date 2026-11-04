# Astronaut Training API
Self-hosted control plane for JSONL dataset registration and training job lifecycle. Android remains the offline-first client; GPU training runs on a separately provisioned host.

## Development
Python 3.11+:
```bash
python -m venv .venv
. .venv/bin/activate
pip install -r requirements.txt
uvicorn astronaut_api.main:app --host 127.0.0.1 --port 8080
```
Open `/docs` for the API. The API starts in metadata-only mode. Training is disabled unless explicitly enabled on a secured GPU host.

## Security
- Android does not connect automatically; backend configuration must be explicit.
- Uploaded datasets are stored under the configured data directory.
- The included API is a development control plane, not a public production deployment.
- Before production, add authentication, TLS termination, a durable queue, isolated workers, quotas, audit logs and artifact signing.

from __future__ import annotations
import json, os, sqlite3
from pathlib import Path
from celery import Celery

DATA_DIR = Path(os.getenv("ASTRONAUT_DATA_DIR", "./astronaut-data")).resolve()
DB_PATH = DATA_DIR / "astronaut.sqlite3"
ARTIFACTS = DATA_DIR / "artifacts"
REDIS_URL = os.getenv("ASTRONAUT_REDIS_URL", "redis://127.0.0.1:6379/0")
celery_app = Celery("astronaut", broker=REDIS_URL, backend=REDIS_URL)
celery_app.conf.update(task_track_started=True, task_serializer="json", result_serializer="json", accept_content=["json"])

def update_job(job_id: str, state: str, message: str) -> None:
    with sqlite3.connect(DB_PATH) as db:
        db.execute("UPDATE jobs SET state=?, message=?, progress=? WHERE id=?", (state, message, 100.0 if state == "completed" else 0.0, job_id))
        db.commit()

@celery_app.task(name="astronaut.train")
def train_job(job_id: str) -> dict:
    with sqlite3.connect(DB_PATH) as db:
        row = db.execute("SELECT dataset_id, base_model, config FROM jobs WHERE id=?", (job_id,)).fetchone()
        if row is None:
            raise ValueError("Training job not found")
        dataset_id, base_model, config_json = row
        dataset = db.execute("SELECT path FROM datasets WHERE id=?", (dataset_id,)).fetchone()
        if dataset is None:
            raise ValueError("Training dataset not found")
    config = json.loads(config_json)
    update_job(job_id, "running", "GPU worker started training")
    try:
        from astronaut_api.trainer import train_lora
        result = train_lora(dataset[0], base_model, config, str(ARTIFACTS / job_id))
        update_job(job_id, "completed", "Training completed; adapter and manifest saved")
        return result
    except Exception as exc:
        update_job(job_id, "failed", f"Training failed: {type(exc).__name__}: {exc}")
        raise

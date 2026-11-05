from fastapi.testclient import TestClient
from astronaut_api import main

def make_client(tmp_path, monkeypatch):
    data = tmp_path / "data"
    datasets = data / "datasets"
    datasets.mkdir(parents=True)
    monkeypatch.setattr(main, "DATA_DIR", data)
    monkeypatch.setattr(main, "DATASETS", datasets)
    monkeypatch.setattr(main, "DB_PATH", data / "test.sqlite3")
    monkeypatch.delenv("ASTRONAUT_ENABLE_TRAINING", raising=False)
    monkeypatch.delenv("ASTRONAUT_REDIS_URL", raising=False)
    main.init_db()
    return TestClient(main.app)

def test_upload_list_and_job_is_blocked_without_worker(tmp_path, monkeypatch):
    client = make_client(tmp_path, monkeypatch)
    payload = b'{"text":"hello world"}\n{"prompt":"write a test","completion":"assert true"}\n'
    response = client.post("/v1/datasets", files={"file": ("examples.jsonl", payload, "application/jsonl")})
    assert response.status_code == 200
    dataset = response.json()
    assert dataset["rows"] == 2
    assert len(dataset["sha256"]) == 64
    assert client.get("/v1/datasets").json()[0]["id"] == dataset["id"]
    job = client.post("/v1/training/jobs", json={"dataset_id": dataset["id"], "base_model": "local/base"})
    assert job.status_code == 200
    assert job.json()["state"] == "blocked"
    assert "worker is not configured" in job.json()["message"].lower()

def test_invalid_jsonl_is_rejected(tmp_path, monkeypatch):
    client = make_client(tmp_path, monkeypatch)
    response = client.post("/v1/datasets", files={"file": ("broken.jsonl", b'not-json\n', "application/jsonl")})
    assert response.status_code == 422

def test_non_jsonl_is_rejected(tmp_path, monkeypatch):
    client = make_client(tmp_path, monkeypatch)
    response = client.post("/v1/datasets", files={"file": ("data.csv", b"a,b\n1,2\n", "text/csv")})
    assert response.status_code == 415

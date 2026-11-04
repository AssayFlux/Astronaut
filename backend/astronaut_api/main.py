from __future__ import annotations
import hashlib, importlib.util, json, os, sqlite3, uuid
from datetime import datetime, timezone
from pathlib import Path
from typing import Any
from fastapi import FastAPI, File, HTTPException, UploadFile
from pydantic import BaseModel, Field

DATA_DIR=Path(os.getenv("ASTRONAUT_DATA_DIR","./astronaut-data")).resolve()
DATASETS=DATA_DIR/"datasets"
DB_PATH=DATA_DIR/"astronaut.sqlite3"
DATASETS.mkdir(parents=True,exist_ok=True)
app=FastAPI(title="Astronaut Training API",version="0.2.0",description="Opt-in dataset and fine-tuning control plane")

def connect()->sqlite3.Connection:
 db=sqlite3.connect(DB_PATH);db.row_factory=sqlite3.Row;return db
def init_db()->None:
 with connect() as db:
  db.executescript("""
  CREATE TABLE IF NOT EXISTS datasets(id TEXT PRIMARY KEY,name TEXT NOT NULL,filename TEXT NOT NULL,path TEXT NOT NULL,sha256 TEXT NOT NULL,bytes INTEGER NOT NULL,rows INTEGER NOT NULL,created_at TEXT NOT NULL);
  CREATE TABLE IF NOT EXISTS jobs(id TEXT PRIMARY KEY,dataset_id TEXT NOT NULL,base_model TEXT NOT NULL,method TEXT NOT NULL,state TEXT NOT NULL,progress REAL NOT NULL,message TEXT NOT NULL,config TEXT NOT NULL,created_at TEXT NOT NULL,FOREIGN KEY(dataset_id) REFERENCES datasets(id));
  """)
@app.on_event("startup")
def startup()->None:init_db()
def now()->str:return datetime.now(timezone.utc).isoformat()

class DatasetInfo(BaseModel):
 id:str;name:str;filename:str;sha256:str;bytes:int;rows:int;created_at:str
class TrainRequest(BaseModel):
 dataset_id:str
 base_model:str=Field(min_length=1,max_length=512)
 method:str=Field(default="lora",pattern="^(lora|qlora)$")
 epochs:int=Field(default=1,ge=1,le=10)
 learning_rate:float=Field(default=0.0002,gt=0,le=0.01)
 rank:int=Field(default=8,ge=1,le=128)
 output_name:str=Field(default="astronaut-adapter",min_length=1,max_length=80)

def validate_jsonl(path:Path)->int:
 count=0
 with path.open("r",encoding="utf-8") as handle:
  for line_no,line in enumerate(handle,1):
   if not line.strip():continue
   try:row=json.loads(line)
   except json.JSONDecodeError as exc:raise HTTPException(422,f"Invalid JSON on line {line_no}: {exc.msg}") from exc
   if not isinstance(row,dict) or not any(k in row for k in ("text","messages","prompt")):
    raise HTTPException(422,f"Line {line_no} must be an object with text, messages, or prompt")
   count+=1
 if count==0:raise HTTPException(422,"Dataset contains no usable records")
 return count

@app.get("/health")
def health()->dict[str,str]:return {"status":"ok","mode":"metadata-and-optional-worker"}

@app.post("/v1/datasets",response_model=DatasetInfo)
async def upload_dataset(file:UploadFile=File(...))->DatasetInfo:
 filename=Path(file.filename or "dataset.jsonl").name
 if not filename.lower().endswith(".jsonl"):raise HTTPException(415,"Only JSONL datasets are supported in this API version")
 dataset_id=str(uuid.uuid4());target=DATASETS/f"{dataset_id}.jsonl";digest=hashlib.sha256();size=0
 try:
  with target.open("wb") as out:
   while chunk:=await file.read(1024*1024):
    size+=len(chunk)
    if size>512*1024*1024:raise HTTPException(413,"Dataset exceeds 512 MiB limit")
    digest.update(chunk);out.write(chunk)
  rows=validate_jsonl(target)
 except Exception:
  target.unlink(missing_ok=True);raise
 record=DatasetInfo(id=dataset_id,name=Path(filename).stem,filename=filename,sha256=digest.hexdigest(),bytes=size,rows=rows,created_at=now())
 with connect() as db:db.execute("INSERT INTO datasets VALUES(?,?,?,?,?,?,?,?)",(record.id,record.name,record.filename,str(target),record.sha256,record.bytes,record.rows,record.created_at))
 return record

@app.get("/v1/datasets",response_model=list[DatasetInfo])
def list_datasets()->list[DatasetInfo]:
 with connect() as db:rows=db.execute("SELECT * FROM datasets ORDER BY created_at DESC").fetchall()
 return [DatasetInfo(id=r["id"],name=r["name"],filename=r["filename"],sha256=r["sha256"],bytes=r["bytes"],rows=r["rows"],created_at=r["created_at"]) for r in rows]

@app.post("/v1/training/jobs")
def create_training_job(request:TrainRequest)->dict[str,Any]:
 with connect() as db:
  if db.execute("SELECT id FROM datasets WHERE id=?",(request.dataset_id,)).fetchone() is None:raise HTTPException(404,"Dataset not found")
  job_id=str(uuid.uuid4())
  worker_enabled=os.getenv("ASTRONAUT_ENABLE_TRAINING")=="1" and bool(os.getenv("ASTRONAUT_REDIS_URL"))
  state="blocked";message="Request recorded; training worker is not configured."
  if worker_enabled:
   state="queued";message="Queued for the configured GPU worker."
  db.execute("INSERT INTO jobs VALUES(?,?,?,?,?,?,?,?,?)",(job_id,request.dataset_id,request.base_model,request.method,state,0.0,message,request.model_dump_json(),now()))
 if worker_enabled:
  try:
   from astronaut_api.worker import celery_app
   celery_app.send_task("astronaut.train",args=[job_id])
  except Exception as exc:
   state="blocked";message=f"Could not enqueue training job: {type(exc).__name__}"
   with connect() as db:db.execute("UPDATE jobs SET state=?,message=? WHERE id=?",(state,message,job_id))
 return {"id":job_id,"state":state,"progress":0,"message":message}

@app.get("/v1/training/jobs/{job_id}")
def get_training_job(job_id:str)->dict[str,Any]:
 with connect() as db:row=db.execute("SELECT * FROM jobs WHERE id=?",(job_id,)).fetchone()
 if row is None:raise HTTPException(404,"Training job not found")
 return {"id":row["id"],"dataset_id":row["dataset_id"],"base_model":row["base_model"],"method":row["method"],"state":row["state"],"progress":row["progress"],"message":row["message"],"created_at":row["created_at"]}

@app.get("/v1/system/capabilities")
def capabilities()->dict[str,Any]:
 return {"training_worker_configured":os.getenv("ASTRONAUT_ENABLE_TRAINING")=="1" and bool(os.getenv("ASTRONAUT_REDIS_URL")),"torch_installed":importlib.util.find_spec("torch") is not None,"transformers_installed":importlib.util.find_spec("transformers") is not None,"trl_installed":importlib.util.find_spec("trl") is not None,"peft_installed":importlib.util.find_spec("peft") is not None}

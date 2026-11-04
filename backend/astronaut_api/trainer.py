from __future__ import annotations
import hashlib, json
from datetime import datetime, timezone
from pathlib import Path
from typing import Any

def _record_to_text(row: dict[str, Any]) -> str:
    if isinstance(row.get("text"), str):
        return row["text"]
    if isinstance(row.get("messages"), list):
        parts = []
        for message in row["messages"]:
            if isinstance(message, dict):
                role = str(message.get("role", "user"))
                content = str(message.get("content", ""))
                parts.append(f"{role}: {content}")
        return "\n".join(parts)
    if "prompt" in row:
        return f"User: {row.get('prompt', '')}\nAssistant: {row.get('completion', '')}"
    raise ValueError("Each JSONL record must contain text, messages, or prompt")

def train_lora(dataset_path: str, base_model: str, config: dict[str, Any], output_dir: str) -> dict[str, Any]:
    # Heavy ML dependencies are imported only inside the GPU worker process.
    import torch
    from datasets import Dataset
    from peft import LoraConfig, get_peft_model, prepare_model_for_kbit_training
    from transformers import (
        AutoModelForCausalLM, AutoTokenizer, BitsAndBytesConfig,
        DataCollatorForLanguageModeling, Trainer, TrainingArguments
    )

    method = config["method"]
    if method == "qlora" and not torch.cuda.is_available():
        raise RuntimeError("QLoRA requires a CUDA-capable GPU worker")
    if not torch.cuda.is_available():
        raise RuntimeError("This training worker requires a CUDA GPU; CPU training is intentionally disabled")

    records = []
    with Path(dataset_path).open("r", encoding="utf-8") as handle:
        for line in handle:
            if line.strip():
                records.append({"text": _record_to_text(json.loads(line))})
    if not records:
        raise ValueError("Dataset contains no training records")

    tokenizer = AutoTokenizer.from_pretrained(base_model, use_fast=True)
    if tokenizer.pad_token is None:
        tokenizer.pad_token = tokenizer.eos_token

    load_kwargs: dict[str, Any] = {"device_map": "auto", "torch_dtype": torch.float16}
    if method == "qlora":
        load_kwargs["quantization_config"] = BitsAndBytesConfig(
            load_in_4bit=True,
            bnb_4bit_quant_type="nf4",
            bnb_4bit_compute_dtype=torch.float16,
            bnb_4bit_use_double_quant=True,
        )
    model = AutoModelForCausalLM.from_pretrained(base_model, **load_kwargs)
    if method == "qlora":
        model = prepare_model_for_kbit_training(model)
    model.config.use_cache = False
    adapter = LoraConfig(
        r=int(config["rank"]),
        lora_alpha=int(config["rank"]) * 2,
        lora_dropout=0.05,
        bias="none",
        task_type="CAUSAL_LM",
        target_modules=["q_proj", "v_proj"],
    )
    model = get_peft_model(model, adapter)

    dataset = Dataset.from_list(records)
    def tokenize(batch: dict[str, list[str]]) -> dict[str, Any]:
        return tokenizer(batch["text"], truncation=True, max_length=2048)
    tokenized = dataset.map(tokenize, batched=True, remove_columns=["text"])
    output = Path(output_dir)
    output.mkdir(parents=True, exist_ok=True)
    args = TrainingArguments(
        output_dir=str(output / "checkpoints"),
        num_train_epochs=int(config["epochs"]),
        learning_rate=float(config["learning_rate"]),
        per_device_train_batch_size=1,
        gradient_accumulation_steps=4,
        logging_steps=10,
        save_strategy="epoch",
        save_total_limit=2,
        report_to="none",
        fp16=True,
        gradient_checkpointing=True,
        optim="adamw_torch",
    )
    trainer = Trainer(
        model=model,
        args=args,
        train_dataset=tokenized,
        data_collator=DataCollatorForLanguageModeling(tokenizer=tokenizer, mlm=False),
    )
    trainer.train()
    model.save_pretrained(output / "adapter")
    tokenizer.save_pretrained(output / "adapter")
    digest = hashlib.sha256(Path(dataset_path).read_bytes()).hexdigest()
    manifest = {
        "base_model": base_model,
        "method": method,
        "rank": int(config["rank"]),
        "dataset_sha256": digest,
        "record_count": len(records),
        "created_at": datetime.now(timezone.utc).isoformat(),
        "format": "peft-adapter",
    }
    (output / "manifest.json").write_text(json.dumps(manifest, indent=2), encoding="utf-8")
    return {"artifact_dir": str(output / "adapter"), "manifest": str(output / "manifest.json"), "record_count": len(records)}

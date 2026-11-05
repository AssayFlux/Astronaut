# Offline feature implementation notes

This branch adds reusable on-device feature services while keeping the existing LiteRT-LM chat path intact.

## Implemented in this branch
- Local document ingestion: UTF-8 text, DOCX text extraction with external-entity protections, PDF page rendering and offline OCR, and image OCR using the bundled ML Kit Latin text-recognition model.
- A deterministic lexical retrieval index with chunking, overlap, source labels, ranked retrieval, and prompt-context formatting. This is local keyword retrieval, not vector/embedding RAG.
- Android speech recognition configured to prefer offline mode and Android TTS wrappers. Availability and voice-pack offline status depend on the device's installed speech services.
- HTTPS model download queue helper that requires explicit license acceptance and a pinned SHA-256 before enqueueing, plus post-download checksum verification.
- JSONL dataset validation and deterministic train/validation counts for a future training workflow.
- Sequential local multi-provider collaboration runner. It reports missing providers and peer failures and never fabricates model participation.

## Not claimed complete
- Semantic image understanding / visual question answering: OCR is not image understanding.
- Full document ingestion UI, persistent vector database, embeddings, citations, or automatic chat-context integration.
- Audio UI, runtime permission flow, and confirmation that the device has an offline speech pack.
- In-app model catalog UI, signed catalog metadata, download lifecycle UI, and automatic import after checksum verification.
- Multi-model collaboration is not end-to-end available until at least two real model providers and catalog registration are wired into the app. The current app only instantiates one LiteRT-LM runtime.
- Actual LoRA/QLoRA training, GPU training executor, or training dashboard. The dataset inspector is preparation/validation only.
- Database encryption-at-rest, keystore key lifecycle, release signing, and production security audit.

## Device validation
Run unit tests and assemble the APK on JDK 17 / Android SDK 35. Then test OCR with airplane mode enabled, PDF/DOCX parsing, speech with offline language packs, large downloads with checksum mismatch, and low-RAM cancellation on physical devices. Do not label any device-dependent feature fully validated before those tests pass.

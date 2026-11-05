# Semantic RAG setup

Astronaut supports optional local semantic retrieval alongside its existing BM25 lexical fallback.

## What is implemented

- ONNX Runtime runs an embedding model locally on Android; document text and vectors are not sent to a server.
- WordPiece tokenization uses a matching \`vocab.txt\`.
- Token embeddings are mean-pooled with the attention mask; already-pooled rank-2 outputs are also accepted.
- Embeddings are normalized and compared with cosine similarity.
- Hybrid ranking combines semantic similarity with BM25 lexical scores.
- Chunks are stored in the SQLCipher-backed Room database, scoped to a conversation to prevent cross-chat retrieval.
- PDF \`[Page N]\` markers are retained as page metadata and included in retrieved context.
- If no embedding model is configured, chat continues to use the existing local BM25 retrieval path.

## Model files required

Import both files in **Models → Semantic document search**:

1. An ONNX sentence-embedding model compatible with a BERT/MiniLM-style WordPiece tokenizer.
2. The exact matching \`vocab.txt\` used to train/export that model.

The ONNX model must:
- Accept \`input_ids\` and \`attention_mask\` as int64 tensors with shape \`[1, 128]\`.
- Optionally accept \`token_type_ids\` with the same shape.
- Return either token embeddings \`[1, sequence, dimensions]\` (mean-pooled by Astronaut) or a pooled vector \`[1, dimensions]\`.
- Support sequence length 128. The current runtime uses a maximum sequence length of 128.

The vocabulary must contain \`[UNK]\`, \`[CLS]\`, \`[SEP]\`, and preferably \`[PAD]\`. Do not mix a model and vocabulary from different releases. The current implementation is specifically a WordPiece/MiniLM-style path; it is not a general ONNX model loader.

## Privacy and storage

- Model and vocabulary files are copied into app-private storage.
- Chunk text and embedding vectors are persisted in the encrypted Room database.
- Each chat's index is scoped by its conversation ID.
- No model weights are bundled in the APK and no network calls are made by the embedding runtime.
- Removing an attachment from the chat UI does not yet automatically purge its stored semantic chunks; explicit index cleanup and retention controls remain follow-up work.

## Validation still required

- Run Android CI and JVM tests after the ONNX Runtime dependency is added.
- Test with a known compatible ONNX export and matching vocabulary.
- Measure retrieval precision/recall on a representative document set.
- Validate inference latency, peak RAM, thermal impact and model storage on physical devices.
- Add support for more tokenizer families and model output conventions before describing this as a universal embedding-model interface.

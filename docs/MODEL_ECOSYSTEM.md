# Model catalog, installation and mobile export

## Android model catalog contract

Astronaut accepts a signed catalog envelope with two Base64 fields:

```json
{
  "payload": "<base64 of the exact UTF-8 catalog JSON bytes>",
  "signature": "<base64 Ed25519 signature over those exact payload bytes>"
}
```

The catalog payload schema version is 1:

```json
{
  "schema_version": 1,
  "generated_at": "2026-11-05T00:00:00Z",
  "models": [{
    "id": "publisher-model-small",
    "name": "Publisher Model Small",
    "publisher": "Publisher",
    "version": "1.0.0",
    "description": "Model description",
    "license": "Apache-2.0",
    "license_url": "https://publisher.example/license",
    "download_url": "https://publisher.example/model.litertlm",
    "sha256": "<64 hex characters>",
    "file_name": "model-small.litertlm",
    "format": "litertlm",
    "runtime": "litertlm",
    "min_sdk": 26,
    "min_ram_mb": 2048,
    "size_bytes": 123456789,
    "capabilities": ["chat"],
    "architecture": "transformer",
    "quantization": "int4"
  }]
}
```

The parser verifies the envelope signature before parsing any catalog records, limits envelope and payload sizes, rejects duplicate IDs, non-HTTPS download/license URLs, invalid checksums, unsafe filenames, unsupported runtime/format values, excessive model sizes and invalid device requirements. `Ed25519CatalogVerifier` takes an X.509-encoded Ed25519 public key. The application/release owner must provision a publisher key through a trusted release configuration before treating any remote catalog as trusted. No publisher key or third-party model listing is fabricated or embedded by this change.

## Compatibility and installation

- `ModelCompatibilityChecker` compares Android API level, physical RAM, free private storage and the supported LiteRT-LM format.
- `ModelInstallStore` copies only checksum-matching files into app-private version directories.
- Each version has an install manifest with publisher, license, version, file name, byte count, checksum and install time.
- Versions are immutable: a version cannot be silently replaced by different bytes.
- The active-version pointer is updated atomically. Previous versions are retained for rollback.
- A version can only be removed when it is not active.
- The store does not automatically load or activate an untrusted model. Runtime loading still needs the app's existing LiteRT-LM model-load path.

The catalog, compatibility and install store are reusable application services. This change does not yet connect a publisher key or add a catalog-browsing screen to the Compose UI; the existing manual HTTPS URL + SHA-256 flow remains available. UI wiring and publisher onboarding are follow-up work, not claimed as complete.

## Training adapter to mobile artifact

The training backend produces PEFT LoRA/QLoRA adapter artifacts. A PEFT adapter is not a LiteRT-LM model. The external conversion toolchain must merge/convert the base model and adapter into a compatible `.litertlm` artifact.

`backend/astronaut_api/mobile_export.py` packages an already-converted artifact with a provenance manifest. It records the source adapter SHA-256, converted artifact SHA-256, converter name/version, license, architecture, quantization, minimum device requirements and capabilities. It validates the adapter ZIP against path traversal/symlinks and enforces an 8 GiB artifact ceiling. It does **not** pretend to implement Google's model conversion algorithm or validate internal LiteRT-LM tensor semantics.

Example:

```bash
python -m astronaut_api.mobile_export \
  --converted-model /path/from/external-converter/model.litertlm \
  --adapter-zip /path/to/training-adapter.zip \
  --metadata-json export-metadata.json \
  --output-dir ./mobile-release
```

The output contains the model file and `manifest.json`. A release publisher must sign the catalog payload with its private Ed25519 key and publish the catalog and artifact over HTTPS. Never place the private signing key in the Android application or repository.

## Validation boundary

Automated tests cover catalog metadata validation/signature gating and export packaging/provenance. Device-specific memory estimates, real model initialization, compatibility with each model's architecture/quantization, actual third-party conversion and inference quality require real artifacts and manual Android testing.

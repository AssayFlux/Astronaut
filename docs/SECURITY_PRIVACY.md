# Security and privacy review

## Current controls visible in source

- Android application ID remains `org.astronaut.agi`; the internal Gradle namespace is `org.offlineai.messenger`.
- Android manifest requests `INTERNET` and `RECORD_AUDIO`. It does not request broad shared-storage access or camera access. User-selected files are accessed through Android's Storage Access Framework.
- Application backup is disabled with `android:allowBackup="false"`.
- Local database encryption uses SQLCipher and Keystore-managed key material; migration, recovery and device-upgrade behavior still need hardware testing.
- Online inference is optional and requires explicit user configuration/enablement. API key material is held by the encrypted secret store, not ordinary preference values.
- Model catalog metadata is trusted only after Ed25519 signature verification. Model downloads require HTTPS and a SHA-256 match before installation.
- Model files are stored in app-private storage. Partial downloads are not exposed as installed/inference-ready models.
- Catalog and model download sizes, redirects, file names, URLs, runtime-format pairs and device requirements are bounded/validated.
- CI instrumentation smoke tests assert package identity, private storage placement, disabled backup and the current core permission declarations.

## Publisher key management

The Android app contains only a Base64 X.509 Ed25519 **public** key and the HTTPS catalog URL at build time. The signing private key must remain in a protected publisher environment (preferably a dedicated offline or tightly controlled signing workflow).

Current catalog schema supports one pinned publisher key per build. It does not yet implement multiple trusted keys, key IDs, in-band key rotation, revocation lists or transparency logs. Therefore:

1. Generate the Ed25519 key pair in a controlled environment and back up the private key securely.
2. Configure the public key as `ASTRONAUT_CATALOG_PUBLIC_KEY` and the HTTPS endpoint as `ASTRONAUT_CATALOG_URL` in protected Gradle/CI configuration.
3. Sign the exact UTF-8 payload bytes before Base64-encoding the payload and signature into the envelope.
4. Keep signing private-key access separate from the Android app build where practical; never commit the key or include it in an APK.
5. If the signing key is rotated or compromised, issue an app update with the replacement pinned public key. Existing app builds cannot trust a new key automatically.
6. Revoke catalog entries by publishing a newly signed catalog that removes or supersedes them. This does not protect users whose pinned signing key itself has been compromised; those users need the replacement app build.

Do not invent or ship a production publisher key. The catalog remains unavailable until the owner provisions a real key and endpoint.

## Release signing

The Android APK signing keystore is separate from the model-catalog Ed25519 signing key. Keep it outside the repository, restrict access, maintain secure backups and configure only the documented GitHub Actions secrets. Losing the Android signing key can prevent updates under the same package identity.

## Permission and data handling

- Request microphone permission only when the user invokes voice input.
- Do not add broad file permissions for attachments; continue using SAF pickers and persist URI grants only when required.
- Keep network features visibly opt-in. Before any future dataset upload or synchronization, show the destination and selected data and require confirmation.
- Do not log API keys, model URLs containing credentials, prompts, attachment text or decrypted database content.
- Document retention/deletion behavior for chat history, attachments, retrieval indexes, downloaded models and training artifacts.

## Required before production release

- [ ] Review every manifest permission and exported component after dependency/feature changes.
- [ ] Verify SQLCipher fresh-install, migration, interrupted-migration and recovery paths on supported Android versions.
- [ ] Verify API-key encryption, replacement, deletion and behavior after Keystore invalidation.
- [ ] Test HTTPS certificate validation, redirect handling, download cancellation/resume and checksum-failure cleanup.
- [ ] Review dependency versions, licenses, known vulnerabilities and native library provenance.
- [ ] Confirm release APK signature, certificate fingerprint, package ID and version.
- [ ] Exercise airplane mode, offline speech, large attachments, low storage, low memory and process death on physical devices.
- [ ] Complete a privacy notice and a data-retention/deletion review before public distribution.

This document is a source-level checklist, not a penetration-test report or a certification. Passing CI does not replace a security review on the final release artifact and target devices.

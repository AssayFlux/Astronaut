# Android release signing

The repository intentionally contains no signing key. A release APK can only be produced after the owner creates a keystore and stores its values as GitHub Actions secrets.

## 1. Create and back up a release keystore

Run locally on a trusted machine:

```bash
keytool -genkeypair -v \
  -keystore astronaut-release.jks \
  -alias astronaut \
  -keyalg RSA -keysize 4096 \
  -validity 10000
```

Back up the keystore and passwords securely. Losing the key can prevent publishing updates under the same Android application identity. Never commit the keystore or passwords.

## 2. Configure GitHub Actions secrets

Add these repository secrets:

- `ANDROID_KEYSTORE_BASE64`: base64 encoding of the complete JKS file (single line).
- `ANDROID_KEYSTORE_PASSWORD`: keystore password.
- `ANDROID_KEY_ALIAS`: alias used above.
- `ANDROID_KEY_PASSWORD`: private-key password.

Linux/macOS base64 example:

```bash
base64 < astronaut-release.jks | tr -d '\n'
```

## 3. Build a signed release

Run the **Signed Android release** workflow manually, or push a version tag beginning with `v`. The workflow refuses to run without all four secrets and uploads `astronaut-signed-release-apk`.

The ordinary Android CI continues to build and upload a debug APK. A green debug build is not a signed production release.

## Remaining release checks

Before publishing, test installation/upgrade on physical devices, verify the signing certificate and application ID, review permissions and data handling, and complete the final production security review. The Room database uses SQLCipher with a Keystore-managed key and explicit legacy migrations; verify migration, recovery and backup behavior on supported Android versions before release.

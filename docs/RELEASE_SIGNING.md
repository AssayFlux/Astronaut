# Android release signing

The repository intentionally contains no signing key. Release signing is performed by the separate **Signed Android release** GitHub Actions workflow after the owner supplies a keystore and configures repository secrets. The regular **Android build** workflow runs unit tests only; it does not build or upload APKs.

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

Run the **Signed Android release** workflow manually from the Actions tab, or push a version tag beginning with `v`. For tag-triggered releases, the workflow verifies that the tag version matches Android's `versionName` (currently `0.1.0`). The workflow refuses to run without all four secrets and uploads `astronaut-signed-release-apk`.

The release workflow builds `:app:assembleRelease`; it does not run as part of ordinary unit-test CI. The release APK is intended for distribution only after physical-device QA and security review.

## Local signed release build

The Android Gradle configuration also accepts these environment variables:

- `ANDROID_KEYSTORE_PATH`
- `ANDROID_KEYSTORE_PASSWORD`
- `ANDROID_KEY_ALIAS`
- `ANDROID_KEY_PASSWORD`

Set all four locally, then run `gradle :app:assembleRelease`. Keep the keystore outside the repository and do not paste passwords into shell history or source files.

## Remaining release checks

Before publishing, test fresh installation and upgrade on physical devices, verify the signing certificate and application ID, review permissions and data handling, and complete the final production security review. The Room database uses SQLCipher with a Keystore-managed key and explicit legacy migrations; verify migration, recovery and backup behavior on supported Android versions before release.

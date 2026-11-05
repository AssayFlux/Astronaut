# Encrypted local database

Astronaut now uses SQLCipher for the Room database. A random 256-bit database passphrase is generated locally and wrapped by a non-exportable AES-GCM key in Android Keystore. Only the wrapped passphrase is stored in private SharedPreferences.

## Existing installations

On the first launch after this update:

1. The legacy plaintext `astronaut-local.db` is opened through Room and migrated through the current schema migrations.
2. A temporary SQLCipher database is created in the app's private database directory.
3. The four Room tables are copied in a single transaction.
4. Row counts are checked for conversations, messages, local models and workspace projects.
5. The temporary encrypted file is promoted to `astronaut-secure.db`.
6. The encrypted database is opened successfully and a migration marker is committed.
7. Only then is the old plaintext database deleted.

If copying or verification fails, the legacy database is retained and the temporary encrypted copy is discarded. If the process dies before the migration marker is committed, the next launch repeats the copy from the still-canonical legacy database.

## Key loss

The SQLCipher passphrase is wrapped by Android Keystore. If the Keystore key is lost while the encrypted database remains, the app refuses to silently generate a replacement key because that would make existing chats unreadable. Reinstalling the app or clearing app data removes the local database and its Keystore key; there is no cloud recovery.

## Device validation required

Before production release, test all of the following on a physical device:

- Fresh install creates an encrypted database and persists/reloads chats.
- Upgrade from a v1/v2/v3/v4/v5 plaintext database preserves all rows.
- Kill the app during first migration, relaunch, and confirm the legacy DB is retained until a successful encrypted open.
- Switch conversations, attach documents, create projects and import models after migration.
- Confirm the resulting database file is not readable as a plain SQLite database.
- Confirm app uninstall/data-clear behaviour is understood.

The Android CI compile and unit tests cannot validate Android Keystore behaviour or the one-time database-file conversion. Keep a user-controlled backup before installing this migration build.

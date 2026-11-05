# Repository audit — 2026-11-05

## Findings
- PR #3 merged successfully; that only confirms GitHub accepted the source changes, not that Android compiled or tests passed.
- The repository had no discoverable .github/workflows/android.yml, so there was no Android CI run associated with the merge commit.
- The added dataset inspector uses org.json. JVM unit tests need a real org.json implementation rather than Android's mockable platform stubs; add org.json as a test dependency.
- Text and DOCX readers previously called readText() before take(), which did not bound memory consumption. Image decoding also used full-resolution bitmaps. These paths now use bounded reads and sampled image decoding.
- LocalRagIndex now validates chunk/overlap configuration and retrieval limits.
- The app's Compose screen still only exposes Chats, Models, Workspace and Settings. New OCR/RAG/audio/download/collaboration/training services are not wired into the chat UI.
- Existing chat attachment handling stores a URI/name reference; it does not extract attachment content or add it to model prompts.
- The current app instantiates one LiteRT-LM runtime. There is no second provider registered in the application, so actual peer collaboration cannot run.
- Model downloads have no catalogue UI or trusted signed catalogue, and no download-to-checksum-to-import workflow is connected.
- Speech services have no runtime permission/UI flow. Offline recognition is dependent on installed device speech services.
- Dataset inspection is not training. No fine-tuning executor or dashboard exists.
- DatabaseProvider uses fallbackToDestructiveMigration; schema upgrades can delete user data. This must be replaced with explicit Room migrations before production.
- No wrapper script is present at repository root; CI uses a pinned Gradle installation instead.

## Verification status
This audit is based on source inspection and GitHub workflow metadata. No successful Gradle build, test report, APK artifact, or physical-device test has been observed. Do not mark the app production-ready until CI and device tests pass.

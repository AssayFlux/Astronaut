# Astronaut — UX and Visual Design Specification

## Product personality

A premium, calm, precise AI messenger. Familiar chat ergonomics, but not a WhatsApp clone. Use a warm paper surface, deep forest-green primary accent, restrained borders, strong typography and high information clarity. Provide light, dark and paper themes.

## Information architecture

Primary bottom navigation:
1. Chats
2. AI Hub
3. Models
4. Workspace
5. Activity
6. Settings

Workspace landing page contains Projects, Files, Backend Developer Studio and AI Training Studio. This keeps specialist tools discoverable without overcrowding the main navigation.

## Chat screen

- Top app bar: conversation title, model/collaboration indicator, search and overflow actions.
- Message timeline: distinct user/assistant surfaces, timestamps, reply threading and attachment cards.
- Assistant response header: final synthesis state plus expandable collaboration details.
- Collaboration detail: actual participant model IDs, capability, runtime, start/end time, duration, status, review notes and failures. Only display data recorded by the execution engine.
- Composer: multiline input, attach, voice, send/stop. Support draft preservation and keyboard insets.
- Streaming: render incremental text, show cancel, persist partial output on cancellation with a cancelled status.
- Empty state: explain local model setup and offer model library navigation; do not show a fabricated AI reply.

## AI Hub

Show task categories, current router policy, installed compatible models, resource budget, runtime health and manual override. Provide a transparent routing trace with reasons such as missing vision capability or memory budget exceeded.

## Model Library

Catalog filters: modality, runtime, architecture, size, quantization, installed status and license. Model detail includes exact source, revision, checksum, file size, RAM estimate with uncertainty, context length, supported modalities, runtime requirement, license and installation state. Download UI must expose progress, pause/resume, retry and checksum verification.

## Workspace

Project list with folders and files. Each project has overview, chat, file tree, generated artifacts, backend environment and history. File operations are explicit; never overwrite source files without confirmation. Preview before export or delete.

## Backend Developer Studio

Workspace tabs:
- Files: project tree and editor.
- API Client: request builder, headers, body, response, latency and saved requests.
- Terminal/Tasks: only a sandboxed, explicitly provisioned execution environment; never execute arbitrary project code inside the Android app process.
- Logs: structured local logs with secrets redacted.
- Environments: local/dev/staging profiles; secrets are masked and stored securely.
- Git: status/diff/commit/push only after the user configures credentials and confirms.

Android phone UI should prioritize browsing, editing, API inspection and task monitoring. Heavy builds, containers, databases and server processes belong on a connected workstation or optional remote dev host.

## AI Training Studio

Wizard steps:
1. Choose base model and verify its license/runtime.
2. Import dataset from SAF or configured storage.
3. Preview and redact data; inspect schema and sample records.
4. Validate formatting, duplicates, leakage, train/validation split and safety.
5. Choose SFT or supported preference-training recipe.
6. Choose LoRA/QLoRA settings and hardware target.
7. Estimate resources and confirm local-vs-remote execution.
8. Submit job and monitor real worker events.
9. Review evaluation against baseline and held-out set.
10. Approve, package, sign and install adapter/model.

Training status must distinguish queued, validating, running, checkpointing, evaluating, packaging, completed, failed and cancelled. A queued job is not a running job.

## Accessibility and responsive behavior

Support font scaling, screen readers, keyboard navigation, high contrast, touch targets and compact layouts. Use adaptive navigation on tablets and landscape. Respect system light/dark settings and reduce motion preferences.

## Interaction quality bar

Every visible action must have a real outcome, disabled explanation or explicit not-yet-implemented state. No dead controls, fake progress bars, hardcoded model availability or demo-generated answers in production.

# Architecture
Target pipeline: Compose UI -> repository -> task router -> model catalog -> local model providers -> independent outputs -> peer review -> coordinator synthesis -> Room persistence.
Providers must validate model/runtime compatibility, report actual capabilities and failures, support cancellation/unload and never fabricate participation. Schedule sequentially on memory-constrained devices.
Training: local dataset preparation may be supported; real LoRA/QLoRA requires an actual trainer and adequate hardware or user-configured backend. No backend is preconfigured.
Room schema v1 is defined; add explicit migrations before schema changes.

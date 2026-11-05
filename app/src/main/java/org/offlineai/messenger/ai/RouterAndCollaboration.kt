package org.offlineai.messenger.ai
enum class TaskKind { GENERAL, PROGRAMMING, DOCUMENT, IMAGE, AUDIO, CREATIVE }
data class RoutingDecision(val task: TaskKind, val required: Set<Capability>)
class TaskRouter {
 fun classify(prompt: String): RoutingDecision {
  val p = prompt.lowercase()
  return when {
   listOf("debug", "code", "function", "python", "kotlin", "compile").any(p::contains) -> RoutingDecision(TaskKind.PROGRAMMING, setOf(Capability.CODE))
   listOf("photo", "image", "picture").any(p::contains) -> RoutingDecision(TaskKind.IMAGE, setOf(Capability.VISION))
   listOf("transcribe", "audio", "speech").any(p::contains) -> RoutingDecision(TaskKind.AUDIO, setOf(Capability.AUDIO_TRANSCRIPTION))
   listOf("pdf", "document", "paper").any(p::contains) -> RoutingDecision(TaskKind.DOCUMENT, setOf(Capability.CHAT))
   else -> RoutingDecision(TaskKind.GENERAL, setOf(Capability.CHAT))
  }
 }
}
class ModelCatalog {
 private val models = linkedMapOf<String, ModelDescriptor>()
 fun register(model: ModelDescriptor) { models[model.id] = model }
 fun all() = models.values.toList()
 fun compatible(required: Set<Capability>) = all().filter { it.installed && it.capabilities.containsAll(required) }
}
sealed interface CollaborationResult {
 data class Success(val participants: List<ModelResult>, val finalAnswer: String): CollaborationResult
 data class Unavailable(val reason: String): CollaborationResult
}
class CollaborationEngine(private val providers: List<LocalModelProvider>, private val catalog: ModelCatalog) {
 suspend fun execute(request: InferenceRequest, required: Set<Capability>, coordinator: LocalModelProvider): CollaborationResult {
  val candidates = catalog.compatible(required).take(2)
  if (candidates.size < 2) return CollaborationResult.Unavailable("Two installed compatible models are required.")
  val outputs = mutableListOf<ModelResult>()
  for (model in candidates) {
   val provider = providers.firstOrNull { it.providerId == model.runtimeId }
    ?: return CollaborationResult.Unavailable("A required runtime is not registered.")
   if (!provider.isAvailable(model)) return CollaborationResult.Unavailable("A selected model is unavailable.")
   outputs += provider.generate(model, request)
   provider.unload(model.id)
  }
  val coordinatorModel = catalog.all().firstOrNull { it.installed && it.runtimeId == coordinator.providerId && Capability.CHAT in it.capabilities }
   ?: return CollaborationResult.Unavailable("No installed coordinator model is available.")
  val joined = outputs.joinToString("\n\n") { "[${it.modelId}]\n${it.text}" }
  val final = coordinator.generate(coordinatorModel, request.copy(prompt = "Synthesize these independent answers and identify disagreements:\n" + joined))
  return CollaborationResult.Success(outputs, final.text)
 }
}

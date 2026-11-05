package org.offlineai.messenger.ai.plugins

import android.content.Context
import org.offlineai.messenger.ai.LiteRtRuntime
import org.offlineai.messenger.features.OnnxWordPieceEmbedder
import java.io.File

/** Adapter around Astronaut's existing LiteRT-LM engine; no duplicate runtime is created per request. */
class LiteRtChatAdapter(context: Context) : AiRuntimeAdapter {
    private val runtime = LiteRtRuntime(context.applicationContext)
    private var loadedTasks: Set<AiTask> = emptySet()

    override val runtimeId: String = "litert-lm"
    override val supportedFormats = setOf(ModelArtifactFormat.LITERT_LM)
    override val supportedTasks = setOf(AiTask.CHAT, AiTask.CODE, AiTask.VISION)

    override suspend fun load(manifest: AiModelManifest, artifact: File) {
        require(manifest.artifactFormat in supportedFormats) { "LiteRT-LM adapter requires a .litertlm artifact." }
        require(supportedTasks.containsAll(manifest.tasks)) { "LiteRT-LM adapter does not support all declared tasks." }
        require(AiModelCompatibility.verifyArtifact(artifact, manifest.sha256)) { "Model artifact is missing or its SHA-256 does not match." }
        runtime.load(artifact.absolutePath, enableVision = AiTask.VISION in manifest.tasks)
        loadedTasks = manifest.tasks
    }

    override suspend fun infer(request: AiInferenceRequest): AiInferenceResult {
        require(request.task in loadedTasks) { "Loaded LiteRT-LM model does not declare " + request.task + " capability." }
        val prompt = request.prompt?.takeIf { it.isNotBlank() }
            ?: error("A non-empty prompt is required.")
        val answer = if (request.inputFile != null) {
            require(request.task == AiTask.VISION && AiTask.VISION in loadedTasks) { "This model is not configured for vision." }
            runtime.generateWithImage(request.inputFile.absolutePath, prompt)
        } else runtime.generate(prompt)
        return AiInferenceResult(text = answer, metadata = mapOf("runtime" to runtimeId))
    }

    override suspend fun unload() {
        runtime.unload()
        loadedTasks = emptySet()
    }
}

/** Adapter for the existing MiniLM-compatible ONNX embedding implementation. */
class OnnxEmbeddingAdapter(private val vocabularyFile: File) : AiRuntimeAdapter {
    private var embedder: OnnxWordPieceEmbedder? = null

    override val runtimeId: String = "onnx-runtime-embeddings"
    override val supportedFormats = setOf(ModelArtifactFormat.ONNX)
    override val supportedTasks = setOf(AiTask.EMBEDDINGS)

    override suspend fun load(manifest: AiModelManifest, artifact: File) {
        require(manifest.artifactFormat == ModelArtifactFormat.ONNX) { "ONNX adapter requires an ONNX graph." }
        require(manifest.tasks == setOf(AiTask.EMBEDDINGS)) { "This adapter currently supports embeddings only." }
        require(AiModelCompatibility.verifyArtifact(artifact, manifest.sha256)) { "ONNX artifact is missing or its SHA-256 does not match." }
        require(vocabularyFile.isFile) { "Import the matching WordPiece vocab.txt before loading this model." }
        embedder?.close()
        embedder = OnnxWordPieceEmbedder(
            artifact, vocabularyFile,
            manifest.id + ":" + artifact.length() + ":" + vocabularyFile.length()
        )
    }

    override suspend fun infer(request: AiInferenceRequest): AiInferenceResult {
        require(request.task == AiTask.EMBEDDINGS) { "ONNX embedding adapter cannot run " + request.task + "." }
        val text = request.prompt?.takeIf { it.isNotBlank() } ?: error("Text to embed is required.")
        val vector = embedder?.embed(text) ?: error("Load an ONNX embedding model first.")
        return AiInferenceResult(
            metadata = mapOf("runtime" to runtimeId, "dimensions" to vector.size.toString()),
            embedding = vector
        )
    }

    override suspend fun unload() {
        embedder?.close()
        embedder = null
    }
}

/** Creates adapters only for engines that are actually shipped by this app build. */
object BuiltInAiRuntimeFactory {
    fun create(context: Context, embeddingVocabulary: File? = null): AiRuntimeRegistry {
        val adapters = mutableListOf<AiRuntimeAdapter>(LiteRtChatAdapter(context))
        if (embeddingVocabulary?.isFile == true) adapters += OnnxEmbeddingAdapter(embeddingVocabulary)
        return AiRuntimeRegistry(adapters)
    }
}

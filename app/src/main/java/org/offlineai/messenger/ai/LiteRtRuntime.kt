package org.offlineai.messenger.ai

import android.content.Context
import com.google.ai.edge.litertlm.Backend
import com.google.ai.edge.litertlm.Conversation
import com.google.ai.edge.litertlm.Engine
import com.google.ai.edge.litertlm.EngineConfig
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.withContext
import java.io.Closeable
import java.io.File
import java.util.concurrent.Executors

/** One LiteRT-LM engine, owned on a dedicated thread. Inference is fully on-device. */
class LiteRtRuntime(context: Context) : Closeable {
    private val cacheDir = File(context.cacheDir, "litert-cache").apply { mkdirs() }
    private val dispatcher = Executors.newSingleThreadExecutor { task ->
        Thread(task, "astronaut-litert").apply { isDaemon = true }
    }.asCoroutineDispatcher()
    private var engine: Engine? = null
    private var conversation: Conversation? = null
    private var closed = false

    suspend fun load(modelPath: String) = withContext(dispatcher) {
        check(!closed)
        closeOnWorker()
        val file = File(modelPath)
        require(file.isFile && file.canRead()) { "Model file is missing or unreadable" }
        val candidate = Engine(EngineConfig(
            modelPath = file.absolutePath,
            backend = Backend.CPU(),
            cacheDir = cacheDir.absolutePath
        ))
        candidate.initialize()
        try {
            val session = candidate.createConversation()
            engine = candidate
            conversation = session
        } catch (error: Throwable) {
            candidate.close()
            throw error
        }
    }

    suspend fun generate(prompt: String): String = withContext(dispatcher) {
        check(!closed)
        val session = conversation ?: error("Load a model before chatting")
        buildString {
            session.sendMessageAsync(prompt).collect { chunk -> append(chunk.toString()) }
        }.trim()
    }

    suspend fun unload() = withContext(dispatcher) { closeOnWorker() }

    private fun closeOnWorker() {
        conversation?.let { runCatching { it.close() } }
        conversation = null
        engine?.let { runCatching { it.close() } }
        engine = null
    }

    override fun close() {
        if (closed) return
        closed = true
        try {
            kotlinx.coroutines.runBlocking(dispatcher) { closeOnWorker() }
        } finally {
            dispatcher.close()
        }
    }
}

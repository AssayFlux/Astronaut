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

/**
 * One LiteRT-LM engine on a dedicated thread.
 *
 * A fresh Conversation is created for every request. Conversation history is supplied
 * explicitly by the caller from the selected chat's persisted messages. This prevents
 * hidden in-memory state leaking when the user switches chats or restarts the process.
 */
class LiteRtRuntime(context: Context) : Closeable {
    private val cacheDir = File(context.cacheDir, "litert-cache").apply { mkdirs() }
    private val dispatcher = Executors.newSingleThreadExecutor { task ->
        Thread(task, "astronaut-litert").apply { isDaemon = true }
    }.asCoroutineDispatcher()
    private var engine: Engine? = null
    private var closed = false

    suspend fun load(modelPath: String) = withContext(dispatcher) {
        check(!closed) { "Runtime is closed" }
        closeEngineOnWorker()
        val file = File(modelPath)
        require(file.isFile && file.canRead()) { "Model file is missing or unreadable" }
        val candidate = Engine(EngineConfig(
            modelPath = file.absolutePath,
            backend = Backend.CPU(),
            cacheDir = cacheDir.absolutePath
        ))
        try {
            candidate.initialize()
            engine = candidate
        } catch (error: Throwable) {
            runCatching { candidate.close() }
            throw error
        }
    }

    suspend fun generate(prompt: String): String = withContext(dispatcher) {
        check(!closed) { "Runtime is closed" }
        require(prompt.isNotBlank()) { "Prompt cannot be empty" }
        val activeEngine = engine ?: error("Load a model before chatting")
        val session: Conversation = activeEngine.createConversation()
        try {
            buildString {
                session.sendMessageAsync(prompt).collect { chunk -> append(chunk.toString()) }
            }.trim()
        } finally {
            runCatching { session.close() }
        }
    }

    suspend fun unload() = withContext(dispatcher) { closeEngineOnWorker() }

    private fun closeEngineOnWorker() {
        engine?.let { runCatching { it.close() } }
        engine = null
    }

    override fun close() {
        if (closed) return
        closed = true
        try {
            kotlinx.coroutines.runBlocking(dispatcher) { closeEngineOnWorker() }
        } finally {
            dispatcher.close()
        }
    }
}

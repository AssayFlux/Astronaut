package org.offlineai.messenger.ai

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CollaborationEngineTest {
    private class FakeProvider : LocalModelProvider {
        override val providerId = "fake"
        val generated = mutableListOf<String>()
        override suspend fun isAvailable(model: ModelDescriptor) = true
        override suspend fun generate(model: ModelDescriptor, request: InferenceRequest): ModelResult {
            generated += model.name
            return ModelResult(model.name, "answer from " + model.name, 12L)
        }
        override suspend fun unload(modelId: String) = Unit
    }

    @Test fun requiresTwoDistinctInstalledModels() = runBlocking {
        val provider = FakeProvider()
        val catalog = ModelCatalog()
        catalog.register(ModelDescriptor("one", "One", provider.providerId, setOf(Capability.CHAT), true, localPath = "/models/one"))
        val result = CollaborationEngine(listOf(provider), catalog).execute(
            InferenceRequest("Explain offline AI", 1L), setOf(Capability.CHAT), provider
        )
        assertTrue(result is CollaborationResult.Unavailable)
        assertTrue(provider.generated.isEmpty())
    }

    @Test fun runsTwoModelsAndThenCoordinator() = runBlocking {
        val provider = FakeProvider()
        val catalog = ModelCatalog()
        catalog.register(ModelDescriptor("one", "One", provider.providerId, setOf(Capability.CHAT), true, localPath = "/models/one"))
        catalog.register(ModelDescriptor("two", "Two", provider.providerId, setOf(Capability.CHAT), true, localPath = "/models/two"))
        val progress = mutableListOf<String>()
        val result = CollaborationEngine(listOf(provider), catalog).execute(
            InferenceRequest("Compare these approaches", 2L), setOf(Capability.CHAT), provider
        ) { progress += it }

        assertTrue(result is CollaborationResult.Success)
        result as CollaborationResult.Success
        assertEquals(2, result.participants.size)
        assertEquals(listOf("One", "Two", "One"), provider.generated)
        assertEquals(3, progress.size)
        assertEquals("answer from One", result.finalAnswer)
    }

    @Test fun duplicatePathsDoNotCountAsTwoModels() = runBlocking {
        val provider = FakeProvider()
        val catalog = ModelCatalog()
        catalog.register(ModelDescriptor("one", "One", provider.providerId, setOf(Capability.CHAT), true, localPath = "/models/shared"))
        catalog.register(ModelDescriptor("two", "Duplicate copy", provider.providerId, setOf(Capability.CHAT), true, localPath = "/models/shared"))
        val result = CollaborationEngine(listOf(provider), catalog).execute(
            InferenceRequest("Check", 3L), setOf(Capability.CHAT), provider
        )
        assertTrue(result is CollaborationResult.Unavailable)
    }
}

package org.offlineai.messenger.features

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class OfflineVoiceControllerTest {
    @Test fun normalizesLanguageTags() {
        assertEquals("en-US", OfflineVoiceController.normalizeLanguageTag("en-us"))
        assertEquals("hi-IN", OfflineVoiceController.normalizeLanguageTag("hi-IN"))
        assertEquals("bn-IN", OfflineVoiceController.normalizeLanguageTag("bn-IN"))
    }

    @Test fun acceptsLanguageTagsWithScriptSubtags() {
        assertEquals("zh-Hans-CN", OfflineVoiceController.normalizeLanguageTag("zh-hans-cn"))
    }

    @Test fun rejectsBlankLanguageTags() {
        val error = runCatching { OfflineVoiceController.normalizeLanguageTag("") }.exceptionOrNull()
        assertTrue(error is IllegalArgumentException)
    }

    @Test fun rejectsMalformedLanguageTags() {
        val error = runCatching { OfflineVoiceController.normalizeLanguageTag("en_US") }.exceptionOrNull()
        assertTrue(error is IllegalArgumentException)
    }

    @Test fun rejectsWhitespaceInLanguageTags() {
        val error = runCatching { OfflineVoiceController.normalizeLanguageTag("en US") }.exceptionOrNull()
        assertTrue(error is IllegalArgumentException)
    }
}

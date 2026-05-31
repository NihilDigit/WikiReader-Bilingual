package org.nsh07.wikireader.translation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TranslationRepositoryTest {
    @Test
    fun translationConfig_canTranslate_requiresEnabledAndProviderFields() {
        val config = TranslationConfig(
            enabled = true,
            apiKey = "sk-test",
            baseUrl = TranslationConfig.DEFAULT_BASE_URL,
            model = TranslationConfig.DEFAULT_MODEL,
            targetLang = TranslationConfig.DEFAULT_TARGET_LANG,
            userId = TranslationConfig.DEFAULT_USER_ID,
            maxConcurrency = TranslationConfig.DEFAULT_MAX_CONCURRENCY
        )

        assertTrue(config.canTranslate)
        assertFalse(config.copy(enabled = false).canTranslate)
        assertFalse(config.copy(apiKey = "").canTranslate)
        assertFalse(config.copy(baseUrl = "").canTranslate)
        assertFalse(config.copy(model = "").canTranslate)
    }

    @Test
    fun sanitizeDeepSeekUserId_acceptsDocumentedCharactersOnly() {
        assertEquals("wikireader-bilingual_1", sanitizeDeepSeekUserId("wikireader-bilingual_1"))
        assertNull(sanitizeDeepSeekUserId(""))
        assertNull(sanitizeDeepSeekUserId("wiki reader"))
        assertNull(sanitizeDeepSeekUserId("wiki.reader"))
    }

    @Test
    fun clampTranslationConcurrency_staysInsideSmallLocalBounds() {
        assertEquals(1, clampTranslationConcurrency(0))
        assertEquals(4, clampTranslationConcurrency(4))
        assertEquals(16, clampTranslationConcurrency(200))
    }

    @Test
    fun estimateTranslationMaxTokens_hasFloorAndCeiling() {
        assertEquals(512L, estimateTranslationMaxTokens(""))
        assertEquals(4096L, estimateTranslationMaxTokens("a".repeat(20_000)))
    }
}

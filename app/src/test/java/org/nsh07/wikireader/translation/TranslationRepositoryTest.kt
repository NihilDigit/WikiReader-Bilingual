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

    @Test
    fun normalizeOpenAiBaseUrl_addsV1OnlyForBareHosts() {
        assertEquals("https://api.deepseek.com/v1/", normalizeOpenAiBaseUrl("https://api.deepseek.com"))
        assertEquals("https://api.deepseek.com/v1/", normalizeOpenAiBaseUrl("https://api.deepseek.com/v1"))
        assertEquals("https://api.deepseek.com/v1/", normalizeOpenAiBaseUrl(" https://api.deepseek.com/v1/ "))
        assertEquals(
            "https://example.com/openai/v1/",
            normalizeOpenAiBaseUrl("https://example.com/openai/v1")
        )
    }

    @Test
    fun translationCacheKey_changesForProviderModelLanguageAndText() {
        val config = TranslationConfig(
            enabled = true,
            apiKey = "sk-test",
            baseUrl = TranslationConfig.DEFAULT_BASE_URL,
            model = TranslationConfig.DEFAULT_MODEL,
            targetLang = TranslationConfig.DEFAULT_TARGET_LANG,
            userId = TranslationConfig.DEFAULT_USER_ID,
            maxConcurrency = TranslationConfig.DEFAULT_MAX_CONCURRENCY
        )
        val key = translationCacheKey("content", "Hello", "en", "zh", config)

        assertEquals(key, translationCacheKey("content", "Hello", "en", "zh", config))
        assertTrue(key.length == 64)
        assertFalse(key == translationCacheKey("title", "Hello", "en", "zh", config))
        assertFalse(key == translationCacheKey("content", "Hello!", "en", "zh", config))
        assertFalse(key == translationCacheKey("content", "Hello", "en", "ja", config))
        assertFalse(key == translationCacheKey("content", "Hello", "en", "zh", config.copy(model = "other")))
    }

    @Test
    fun translationRetryDelayMillis_usesExponentialBackoff() {
        assertEquals(500L, translationRetryDelayMillis(0))
        assertEquals(500L, translationRetryDelayMillis(1))
        assertEquals(1_000L, translationRetryDelayMillis(2))
        assertEquals(2_000L, translationRateLimitRetryDelayMillis(1))
        assertEquals(4_000L, translationRateLimitRetryDelayMillis(2))
    }

    @Test
    fun shouldDisableThinking_forDeepSeekV4Only() {
        val config = TranslationConfig(
            enabled = true,
            apiKey = "sk-test",
            baseUrl = TranslationConfig.DEFAULT_BASE_URL,
            model = TranslationConfig.DEFAULT_MODEL,
            targetLang = TranslationConfig.DEFAULT_TARGET_LANG,
            userId = TranslationConfig.DEFAULT_USER_ID,
            maxConcurrency = TranslationConfig.DEFAULT_MAX_CONCURRENCY
        )

        assertTrue(shouldDisableThinking(config))
        assertTrue(shouldDisableThinking(config.copy(model = "deepseek-v4-flash", baseUrl = "https://api.deepseek.com")))
        assertFalse(shouldDisableThinking(config.copy(model = "deepseek-chat", baseUrl = "https://api.deepseek.com")))
        assertFalse(shouldDisableThinking(config.copy(model = "other", baseUrl = "https://example.com/v1")))
    }

    @Test
    fun vocabularyExplanationPrompt_focusesOnMeaningInContext() {
        val system = vocabularyExplanationSystemPrompt("en", "zh")
        val user = vocabularyExplanationUserPrompt(
            targetText = "trestle",
            sentence = "The railroad trestle crosses the park.",
            context = "Tennessee Plaza and railroad trestle",
            sourceLang = "en",
            targetLang = "zh"
        )

        assertTrue(system.contains("this exact sentence"))
        assertTrue(user.contains("Do not list unrelated dictionary meanings."))
        assertTrue(user.contains("how it works here"))
        assertTrue(user.contains("1-3 short sentences"))
        assertFalse(user.contains("本句含义："))
    }

    @Test
    fun sentenceTranslationPrompt_returnsOnlySentenceTranslation() {
        val system = sentenceTranslationSystemPrompt("en", "zh")
        val user = sentenceTranslationUserPrompt(
            sentence = "The park is located in Nashville.",
            context = "Bicentennial Capitol Mall State Park",
            sourceLang = "en",
            targetLang = "zh"
        )

        assertTrue(system.contains("Output only the zh translation itself."))
        assertTrue(system.contains("Do not include introductions"))
        assertTrue(user.contains("Output only the zh translation itself."))
        assertTrue(user.contains("Nearby context:"))
    }

    @Test
    fun articleDescriptionPrompt_preservesFragmentStyleWithTitleContext() {
        val system = articleDescriptionSystemPrompt("en", "zh")
        val user = articleDescriptionUserPrompt(
            title = "Obsession",
            description = "2025 American horror film by Curry Barker",
            sourceLang = "en",
            targetLang = "zh"
        )

        assertTrue(system.contains("noun phrase or fragment"))
        assertTrue(system.contains("Do not add a subject"))
        assertTrue(system.contains("Output only the zh translation itself."))
        assertTrue(user.contains("Article title for context"))
        assertTrue(user.contains("Preserve the fragment style"))
        assertTrue(user.contains("Do not rewrite it as"))
    }

    @Test
    fun translationOnlyOutputInstruction_bansProviderPreambles() {
        val instruction = translationOnlyOutputInstruction("zh")

        assertTrue(instruction.contains("Output only the zh translation itself."))
        assertTrue(instruction.contains("introductions"))
        assertTrue(instruction.contains("以下是根据您的要求"))
        assertTrue(instruction.contains("Here is the translation"))
    }
}

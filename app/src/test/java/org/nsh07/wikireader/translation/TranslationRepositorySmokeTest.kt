package org.nsh07.wikireader.translation

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test

class TranslationRepositorySmokeTest {
    @Test
    fun configuredProvider_translatesShortText_whenApiKeyProvided() = runBlocking {
        val config = liveTranslationConfig(maxConcurrency = 2)
        val runSmoke = System.getenv("RUN_LIVE_TRANSLATION_SMOKE") == "true"
        assumeTrue("RUN_LIVE_TRANSLATION_SMOKE=true not set; skipping live smoke test", runSmoke)
        assumeTrue("TRANSLATION_API_KEY not set; skipping live smoke test", config.apiKey.isNotBlank())

        val repository = OpenAiCompatibleTranslationRepository(Dispatchers.IO)
        val translated = repository.translateContent(
            text = "Hello.",
            sourceLang = "en",
            targetLang = "zh",
            config = config
        )

        println("Provider live smoke returned ${translated.length} characters")
        assertTrue(translated.isNotBlank())
    }

    @Test
    fun configuredProvider_handlesSmallConcurrentBatch_whenApiKeyProvided() = runBlocking {
        val config = liveTranslationConfig(maxConcurrency = liveTranslationConcurrency())
        val runSmoke = System.getenv("RUN_LIVE_TRANSLATION_CONCURRENCY_SMOKE") == "true"
        assumeTrue(
            "RUN_LIVE_TRANSLATION_CONCURRENCY_SMOKE=true not set; skipping live smoke test",
            runSmoke
        )
        assumeTrue("TRANSLATION_API_KEY not set; skipping live smoke test", config.apiKey.isNotBlank())

        val repository = OpenAiCompatibleTranslationRepository(Dispatchers.IO)
        val inputs = listOf(
            "Bicentennial Capitol Mall is an urban state park.",
            "The plaza contains a map of Tennessee.",
            "The park is located in Nashville.",
            "The amphitheater is used for public events.",
            "The railroad trestle crosses the park.",
            "The court contains a carillon.",
            "The state museum is nearby.",
            "The park receives many visitors."
        ).take(liveTranslationConcurrency().coerceAtLeast(4).coerceAtMost(8))

        val translated = inputs.map { input ->
            async {
                repository.translateContent(
                    text = input,
                    sourceLang = "en",
                    targetLang = "zh",
                    config = config
                )
            }
        }.awaitAll()

        println("Provider concurrency smoke lengths: ${translated.map { it.length }}")
        assertTrue(translated.all { it.isNotBlank() })
    }

    private fun liveTranslationConfig(maxConcurrency: Int): TranslationConfig {
        val explicitApiKey = System.getenv("TRANSLATION_API_KEY")
        val deepSeekApiKey = System.getenv("DEEPSEEK_API_KEY")
        val defaultBaseUrl = when {
            !System.getenv("TRANSLATION_BASE_URL").isNullOrBlank() ->
                System.getenv("TRANSLATION_BASE_URL")
            !deepSeekApiKey.isNullOrBlank() ->
                TranslationConfig.DEFAULT_BASE_URL
            else -> TranslationConfig.DEFAULT_BASE_URL
        }
        val defaultModel = when {
            !System.getenv("TRANSLATION_MODEL").isNullOrBlank() ->
                System.getenv("TRANSLATION_MODEL")
            !deepSeekApiKey.isNullOrBlank() ->
                TranslationConfig.DEFAULT_MODEL
            else -> TranslationConfig.DEFAULT_MODEL
        }

        return TranslationConfig(
            enabled = true,
            apiKey = explicitApiKey ?: deepSeekApiKey ?: "",
            baseUrl = defaultBaseUrl ?: TranslationConfig.DEFAULT_BASE_URL,
            model = defaultModel ?: TranslationConfig.DEFAULT_MODEL,
            targetLang = TranslationConfig.DEFAULT_TARGET_LANG,
            userId = TranslationConfig.DEFAULT_USER_ID,
            maxConcurrency = maxConcurrency
        )
    }

    private fun liveTranslationConcurrency(): Int =
        System.getenv("TRANSLATION_MAX_CONCURRENCY")?.toIntOrNull()
            ?: TranslationConfig.DEFAULT_MAX_CONCURRENCY
}

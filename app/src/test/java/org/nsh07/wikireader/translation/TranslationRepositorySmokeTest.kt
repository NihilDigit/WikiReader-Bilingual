package org.nsh07.wikireader.translation

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test

class TranslationRepositorySmokeTest {
    @Test
    fun deepSeekProvider_translatesShortText_whenApiKeyProvided() = runBlocking {
        val apiKey = System.getenv("DEEPSEEK_API_KEY").orEmpty()
        val runSmoke = System.getenv("RUN_LIVE_TRANSLATION_SMOKE") == "true"
        assumeTrue("RUN_LIVE_TRANSLATION_SMOKE=true not set; skipping live smoke test", runSmoke)
        assumeTrue("DEEPSEEK_API_KEY not set; skipping live smoke test", apiKey.isNotBlank())

        val repository = OpenAiCompatibleTranslationRepository(Dispatchers.IO)
        val translated = repository.translateContent(
            text = "Hello.",
            sourceLang = "en",
            targetLang = "zh",
            config = TranslationConfig(
                enabled = true,
                apiKey = apiKey,
                baseUrl = TranslationConfig.DEFAULT_BASE_URL,
                model = TranslationConfig.DEFAULT_MODEL,
                targetLang = TranslationConfig.DEFAULT_TARGET_LANG,
                userId = TranslationConfig.DEFAULT_USER_ID,
                maxConcurrency = 2
            )
        )

        println("DeepSeek live smoke returned ${translated.length} characters")
        assertTrue(translated.isNotBlank())
    }
}

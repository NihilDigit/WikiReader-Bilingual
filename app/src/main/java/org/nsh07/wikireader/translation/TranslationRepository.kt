package org.nsh07.wikireader.translation

import com.openai.client.okhttp.OpenAIOkHttpClient
import com.openai.core.JsonValue
import com.openai.models.chat.completions.ChatCompletionCreateParams
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import java.time.Duration
import kotlin.math.max
import kotlin.math.min

fun sanitizeDeepSeekUserId(userId: String): String? =
    userId.takeIf { it.matches(Regex("[a-zA-Z0-9\\-_]{1,512}")) }

fun clampTranslationConcurrency(maxConcurrency: Int): Int =
    maxConcurrency.coerceIn(1, 16)

fun estimateTranslationMaxTokens(text: String): Long =
    min(4096L, max(512L, (text.length / 2L) + 512L))

interface TranslationRepository {
    suspend fun translateContent(
        text: String,
        sourceLang: String,
        targetLang: String,
        config: TranslationConfig
    ): String

    suspend fun translateTitle(
        title: String,
        sourceLang: String,
        targetLang: String,
        config: TranslationConfig
    ): String
}

class OpenAiCompatibleTranslationRepository(
    private val ioDispatcher: CoroutineDispatcher
) : TranslationRepository {
    private val semaphores = mutableMapOf<Int, Semaphore>()

    override suspend fun translateContent(
        text: String,
        sourceLang: String,
        targetLang: String,
        config: TranslationConfig
    ): String = translate(
        systemPrompt = "Translate Wikipedia article content from $sourceLang to $targetLang. " +
                "Return only the translated text. Keep paragraph breaks. Do not translate app UI labels. " +
                "Convert wiki markup into readable plain text when needed.",
        text = text,
        config = config
    )

    override suspend fun translateTitle(
        title: String,
        sourceLang: String,
        targetLang: String,
        config: TranslationConfig
    ): String = translate(
        systemPrompt = "Translate this Wikipedia article title from $sourceLang to $targetLang. " +
                "Return only the title, with no explanation.",
        text = title,
        config = config
    )

    private suspend fun translate(
        systemPrompt: String,
        text: String,
        config: TranslationConfig
    ): String = withContext(ioDispatcher) {
        if (!config.canTranslate) return@withContext ""
        if (text.isBlank()) return@withContext ""

        val concurrency = clampTranslationConcurrency(config.maxConcurrency)
        val semaphore = synchronized(semaphores) {
            semaphores.getOrPut(concurrency) { Semaphore(concurrency) }
        }

        semaphore.withPermit {
            val client = OpenAIOkHttpClient.builder()
                .apiKey(config.apiKey)
                .baseUrl(config.baseUrl.trimEnd('/'))
                .timeout(Duration.ofSeconds(45))
                .maxRetries(1)
                .build()

            val maxTokens = estimateTranslationMaxTokens(text)
            val paramsBuilder = ChatCompletionCreateParams.builder()
                .model(config.model)
                .addSystemMessage(systemPrompt)
                .addUserMessage(text)
                .temperature(0.0)
                .apply {
                    @Suppress("DEPRECATION")
                    maxTokens(maxTokens)
                }

            val userId = sanitizeDeepSeekUserId(config.userId)
            if (userId != null) {
                paramsBuilder.putAdditionalBodyProperty("user_id", JsonValue.from(userId))
            }

            val completion = client.chat().completions().create(paramsBuilder.build())
            completion.choices()
                .firstOrNull()
                ?.message()
                ?.content()
                ?.orElse("")
                ?.trim()
                .orEmpty()
        }
    }
}

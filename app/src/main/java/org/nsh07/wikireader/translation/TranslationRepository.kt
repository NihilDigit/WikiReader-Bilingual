package org.nsh07.wikireader.translation

import com.aallam.openai.api.chat.ChatCompletionRequest
import com.aallam.openai.api.chat.ChatMessage
import com.aallam.openai.api.chat.ListContent
import com.aallam.openai.api.chat.TextContent
import com.aallam.openai.api.core.Role
import com.aallam.openai.api.http.Timeout
import com.aallam.openai.api.model.ModelId
import com.aallam.openai.client.OpenAI
import com.aallam.openai.client.OpenAIConfig
import com.aallam.openai.client.OpenAIHost
import com.aallam.openai.client.RetryStrategy
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.addJsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject
import org.nsh07.wikireader.data.TranslationCacheDao
import org.nsh07.wikireader.data.TranslationCacheEntry
import java.net.URI
import java.security.MessageDigest
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.time.Duration.Companion.seconds

fun sanitizeDeepSeekUserId(userId: String): String? =
    userId.takeIf { it.matches(Regex("[a-zA-Z0-9\\-_]{1,512}")) }

fun clampTranslationConcurrency(maxConcurrency: Int): Int =
    maxConcurrency.coerceIn(1, 16)

fun estimateTranslationMaxTokens(text: String): Long =
    min(4096L, max(512L, (text.length / 2L) + 512L))

fun normalizeOpenAiBaseUrl(baseUrl: String): String {
    val trimmed = baseUrl.trim().trimEnd('/')
    val path = runCatching { URI(trimmed).path.orEmpty().trim('/') }.getOrDefault("")
    val normalized =
        if (path.isBlank()) "$trimmed/v1"
        else trimmed
    return "$normalized/"
}

fun shouldDisableThinking(config: TranslationConfig): Boolean =
    config.model.startsWith("deepseek-v4", ignoreCase = true)

fun isDeepSeekProvider(config: TranslationConfig): Boolean =
    config.model.startsWith("deepseek-", ignoreCase = true) ||
            runCatching {
                URI(config.baseUrl.trim()).host.orEmpty()
                    .contains("deepseek.com", ignoreCase = true)
            }.getOrDefault(false)

fun translationCacheKey(
    kind: String,
    text: String,
    sourceLang: String,
    targetLang: String,
    config: TranslationConfig
): String = sha256Hex(
    listOf(
        kind,
        normalizeOpenAiBaseUrl(config.baseUrl),
        config.model.trim(),
        sourceLang,
        targetLang,
        sha256Hex(text)
    ).joinToString(separator = "\u001F")
)

fun sha256Hex(text: String): String =
    MessageDigest.getInstance("SHA-256")
        .digest(text.toByteArray(Charsets.UTF_8))
        .joinToString(separator = "") { "%02x".format(it) }

fun translationRetryDelayMillis(attempt: Int): Long =
    exponentialBackoffMillis(baseMillis = 500L, attempt = attempt)

fun translationRateLimitRetryDelayMillis(attempt: Int): Long =
    exponentialBackoffMillis(baseMillis = 2_000L, attempt = attempt)

private fun exponentialBackoffMillis(baseMillis: Long, attempt: Int): Long =
    (baseMillis * 2.0.pow((attempt.coerceAtLeast(1) - 1).toDouble())).toLong()

fun vocabularyExplanationSystemPrompt(sourceLang: String, targetLang: String): String =
    "You help a $targetLang learner read Wikipedia content written in $sourceLang. " +
            "Explain what a selected word or short phrase means in this exact sentence. " +
            "If it belongs to a longer phrase or named term, mention that briefly. " +
            "Use $targetLang. Keep the answer concise, natural, and easy to read."

fun vocabularyExplanationUserPrompt(
    targetText: String,
    sentence: String,
    context: String,
    sourceLang: String,
    targetLang: String
): String =
    """
    Target word or phrase:
    "$targetText"

    Sentence:
    "$sentence"

    Nearby context:
    "$context"

    Explain what the selected text means in this $sourceLang sentence and how it works here.
    Do not list unrelated dictionary meanings.
    If it is part of a longer phrase, technical term, abbreviation, or named entity, say so briefly.

    Return 1-3 short sentences in $targetLang. No bullets, headings, or dictionary lists.
    """.trimIndent()

fun sentenceTranslationSystemPrompt(sourceLang: String, targetLang: String): String =
    "Translate one Wikipedia sentence from $sourceLang into $targetLang. " +
            "Use the context only to preserve meaning. Return only the natural translation."

fun sentenceTranslationUserPrompt(
    sentence: String,
    context: String,
    sourceLang: String,
    targetLang: String
): String =
    """
    Sentence ($sourceLang):
    "$sentence"

    Nearby context:
    "$context"

    Return only the natural $targetLang translation of the sentence.
    """.trimIndent()

fun articleDescriptionSystemPrompt(sourceLang: String, targetLang: String): String =
    "Translate a short Wikipedia article description from $sourceLang to $targetLang. " +
            "The description is a noun phrase or fragment, not a full sentence. " +
            "Translate it literally and concisely. Do not add a subject, title, verb, " +
            "explanation, punctuation, or surrounding quotation marks."

fun articleDescriptionUserPrompt(
    title: String,
    description: String,
    sourceLang: String,
    targetLang: String
): String =
    """
    Article title for context:
    "$title"

    Description ($sourceLang):
    "$description"

    Return only the $targetLang translation of the description phrase. Preserve the fragment style.
    Do not rewrite it as "$title is ...".
    """.trimIndent()

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

    suspend fun translateDescription(
        title: String,
        description: String,
        sourceLang: String,
        targetLang: String,
        config: TranslationConfig
    ): String

    suspend fun explainVocabulary(
        targetText: String,
        sentence: String,
        context: String,
        sourceLang: String,
        targetLang: String,
        config: TranslationConfig
    ): String

    suspend fun translateSentence(
        sentence: String,
        context: String,
        sourceLang: String,
        targetLang: String,
        config: TranslationConfig
    ): String
}

class OpenAiCompatibleTranslationRepository(
    private val ioDispatcher: CoroutineDispatcher,
    private val translationCacheDao: TranslationCacheDao? = null
) : TranslationRepository {
    private val semaphores = mutableMapOf<Int, Semaphore>()

    override suspend fun translateContent(
        text: String,
        sourceLang: String,
        targetLang: String,
        config: TranslationConfig
    ): String = translate(
        kind = "content",
        systemPrompt = "Translate Wikipedia article content from $sourceLang to $targetLang. " +
                "Return only the translated text. Preserve the input line breaks exactly; do not split, merge, or reorder paragraphs. " +
                "Do not translate app UI labels. " +
                "Do not include citation markers or reference numbers. Convert wiki markup into readable plain text when needed.",
        text = text.cleanArticleTextForTranslation(),
        sourceLang = sourceLang,
        targetLang = targetLang,
        config = config
    )

    override suspend fun translateDescription(
        title: String,
        description: String,
        sourceLang: String,
        targetLang: String,
        config: TranslationConfig
    ): String = translate(
        kind = "description",
        systemPrompt = articleDescriptionSystemPrompt(sourceLang, targetLang),
        text = articleDescriptionUserPrompt(
            title = title,
            description = description.cleanArticleTextForTranslation(),
            sourceLang = sourceLang,
            targetLang = targetLang
        ),
        sourceLang = sourceLang,
        targetLang = targetLang,
        config = config
    )

    override suspend fun explainVocabulary(
        targetText: String,
        sentence: String,
        context: String,
        sourceLang: String,
        targetLang: String,
        config: TranslationConfig
    ): String = translate(
        kind = "vocabulary",
        systemPrompt = vocabularyExplanationSystemPrompt(sourceLang, targetLang),
        text = vocabularyExplanationUserPrompt(
            targetText = targetText,
            sentence = sentence,
            context = context,
            sourceLang = sourceLang,
            targetLang = targetLang
        ),
        sourceLang = sourceLang,
        targetLang = targetLang,
        config = config
    )

    override suspend fun translateSentence(
        sentence: String,
        context: String,
        sourceLang: String,
        targetLang: String,
        config: TranslationConfig
    ): String = translate(
        kind = "sentence",
        systemPrompt = sentenceTranslationSystemPrompt(sourceLang, targetLang),
        text = sentenceTranslationUserPrompt(
            sentence = sentence,
            context = context,
            sourceLang = sourceLang,
            targetLang = targetLang
        ),
        sourceLang = sourceLang,
        targetLang = targetLang,
        config = config
    )

    override suspend fun translateTitle(
        title: String,
        sourceLang: String,
        targetLang: String,
        config: TranslationConfig
    ): String = translate(
        kind = "title",
        systemPrompt = "Translate this Wikipedia article title from $sourceLang to $targetLang. " +
                "Return only the title, with no explanation.",
        text = title,
        sourceLang = sourceLang,
        targetLang = targetLang,
        config = config
    )

    private suspend fun translate(
        kind: String,
        systemPrompt: String,
        text: String,
        sourceLang: String,
        targetLang: String,
        config: TranslationConfig
    ): String = withContext(ioDispatcher) {
        if (!config.canTranslate) return@withContext ""
        if (text.isBlank()) return@withContext ""

        val cacheKey = translationCacheKey(
            kind = kind,
            text = text,
            sourceLang = sourceLang,
            targetLang = targetLang,
            config = config
        )
        translationCacheDao?.get(cacheKey)?.translatedText?.takeIf { it.isNotBlank() }?.let {
            return@withContext it
        }

        val concurrency = clampTranslationConcurrency(config.maxConcurrency)
        val semaphore = synchronized(semaphores) {
            semaphores.getOrPut(concurrency) { Semaphore(concurrency) }
        }

        semaphore.withPermit {
            val client = OpenAI(
                OpenAIConfig(
                    token = config.apiKey,
                    host = OpenAIHost(baseUrl = normalizeOpenAiBaseUrl(config.baseUrl)),
                    timeout = Timeout(socket = 45.seconds),
                    retry = RetryStrategy(maxRetries = 0)
                )
            )

            try {
                val translatedText = requestTranslationWithRetry(
                    client = client,
                    systemPrompt = systemPrompt,
                    text = text,
                    config = config
                )
                if (translatedText.isNotBlank()) {
                    translationCacheDao?.upsert(
                        TranslationCacheEntry(
                            cacheKey = cacheKey,
                            providerBaseUrl = normalizeOpenAiBaseUrl(config.baseUrl),
                            model = config.model.trim(),
                            sourceLang = sourceLang,
                            targetLang = targetLang,
                            kind = kind,
                            textHash = sha256Hex(text),
                            translatedText = translatedText
                        )
                    )
                }
                translatedText
            } finally {
                client.close()
            }
        }
    }

    private suspend fun requestTranslationWithRetry(
        client: OpenAI,
        systemPrompt: String,
        text: String,
        config: TranslationConfig
    ): String {
        val maxTokens = estimateTranslationMaxTokens(text)
        val request = ChatCompletionRequest(
            model = ModelId(config.model),
            messages = listOf(
                ChatMessage(role = Role.System, content = systemPrompt),
                ChatMessage(role = Role.User, content = text)
            ),
            temperature = 0.0,
            maxTokens = maxTokens.toInt(),
            user = sanitizeDeepSeekUserId(config.userId)
        )

        var lastFailure: Throwable? = null
        repeat(TRANSLATION_PROVIDER_ATTEMPTS) { attemptIndex ->
            val attempt = attemptIndex + 1
            try {
                paceProviderRequests()
                val translatedText =
                    if (shouldDisableThinking(config)) {
                        requestTranslationWithThinkingDisabled(
                            systemPrompt = systemPrompt,
                            text = text,
                            config = config
                        )
                    } else {
                        val completion = client.chatCompletion(request)
                        val choice = completion.choices.firstOrNull()
                        choice?.message?.translatedText().orEmpty()
                    }
                if (translatedText.isNotBlank()) return translatedText

                lastFailure = EmptyTranslationResponseException("Empty provider response; attempt=$attempt")
            } catch (e: CancellationException) {
                throw e
            } catch (e: ProviderRateLimitException) {
                lastFailure = e
                if (attempt < TRANSLATION_PROVIDER_ATTEMPTS) {
                    delay(translationRateLimitRetryDelayMillis(attempt))
                    return@repeat
                }
            } catch (e: Exception) {
                lastFailure = e
            }

            if (attempt < TRANSLATION_PROVIDER_ATTEMPTS) {
                delay(translationRetryDelayMillis(attempt))
            }
        }

        throw lastFailure ?: EmptyTranslationResponseException("Empty provider response")
    }

    private fun paceProviderRequests() {
        // DeepSeek throttling is handled by the provider response plus our retry backoff.
    }

    private suspend fun requestTranslationWithThinkingDisabled(
        systemPrompt: String,
        text: String,
        config: TranslationConfig
    ): String {
        val client = HttpClient(CIO)
        return try {
            val response = client.post(normalizeOpenAiBaseUrl(config.baseUrl) + "chat/completions") {
                header(HttpHeaders.Authorization, "Bearer ${config.apiKey}")
                contentType(ContentType.Application.Json)
                setBody(
                    buildJsonObject {
                        put("model", config.model)
                        putJsonArray("messages") {
                            addJsonObject {
                                put("role", "system")
                                put("content", systemPrompt)
                            }
                            addJsonObject {
                                put("role", "user")
                                put("content", text)
                            }
                        }
                        put("temperature", 0.0)
                        put("max_tokens", estimateTranslationMaxTokens(text).toInt())
                        sanitizeDeepSeekUserId(config.userId)?.let { put("user_id", it) }
                        putJsonObject("thinking") {
                            put("type", "disabled")
                        }
                    }.toString()
                )
            }
            val body = response.bodyAsText()
            if (!response.status.isSuccess()) {
                val message = "Provider HTTP ${response.status.value}: ${body.take(240)}"
                if (response.status.value == 429) {
                    throw ProviderRateLimitException(message)
                }
                throw RuntimeException(message)
            }
            val root = Json.parseToJsonElement(body).jsonObject
            root["error"]?.let { throw RuntimeException(it.toString().take(240)) }
            val message = root["choices"]
                ?.jsonArray
                ?.firstOrNull()
                ?.jsonObject
                ?.get("message")
                ?.jsonObject
            message
                ?.get("content")
                ?.jsonPrimitive
                ?.content
                ?.trim()
                .orEmpty()
        } finally {
            client.close()
        }
    }

    private fun ChatMessage.translatedText(): String {
        val plainContent = content?.trim().orEmpty()
        if (plainContent.isNotEmpty()) return plainContent

        return when (val typedContent = messageContent) {
            is TextContent -> typedContent.content.trim()
            is ListContent -> typedContent.content.joinToString(separator = "") { it.toString() }.trim()
            else -> typedContent.toString().trim()
        }
    }

    companion object {
        private const val TRANSLATION_PROVIDER_ATTEMPTS = 3
    }
}

private class EmptyTranslationResponseException(message: String) : RuntimeException(message)
private class ProviderRateLimitException(message: String) : RuntimeException(message)

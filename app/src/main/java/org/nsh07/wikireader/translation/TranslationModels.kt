package org.nsh07.wikireader.translation

enum class BilingualTranslationStatus {
    IDLE,
    LOADING,
    READY,
    ERROR
}

data class BilingualSectionTranslation(
    val status: BilingualTranslationStatus = BilingualTranslationStatus.IDLE,
    val text: String? = null,
    val error: String? = null
)

data class TranslationConfig(
    val enabled: Boolean,
    val apiKey: String,
    val baseUrl: String,
    val model: String,
    val targetLang: String,
    val userId: String,
    val maxConcurrency: Int
) {
    val canTranslate: Boolean
        get() = enabled && apiKey.isNotBlank() && baseUrl.isNotBlank() && model.isNotBlank()

    companion object {
        const val DEFAULT_BASE_URL = "https://api.deepseek.com"
        const val DEFAULT_MODEL = "deepseek-v4-flash"
        const val DEFAULT_TARGET_LANG = "zh"
        const val DEFAULT_USER_ID = "wikireader-bilingual"
        const val DEFAULT_MAX_CONCURRENCY = 4
    }
}

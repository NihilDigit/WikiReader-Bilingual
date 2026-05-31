package org.nsh07.wikireader.ui.settingsScreen.viewModel

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import org.nsh07.wikireader.translation.TranslationConfig

@Immutable
data class PreferencesState(
    val theme: String = "auto",
    val lang: String = "en",
    val fontStyle: String = "sans",
    val colorScheme: String = Color.Companion.White.toString(),
    val fontSize: Int = 16,
    val blackTheme: Boolean = false,
    val dataSaver: Boolean = false,
    val feedEnabled: Boolean = true,
    val expandedSections: Boolean = false,
    val imageBackground: Boolean = false,
    val immersiveMode: Boolean = true,
    val renderMath: Boolean = true,
    val browsingHistory: Boolean = true,
    val searchHistory: Boolean = true,
    val bilingualEnabled: Boolean = false,
    val bilingualBlurBlocks: Boolean = true,
    val bilingualAutoTranslateBlocks: Boolean = true,
    val translationDeepSeekApiKey: String = "",
    val translationBaseUrl: String = TranslationConfig.DEFAULT_BASE_URL,
    val translationModel: String = TranslationConfig.DEFAULT_MODEL,
    val translationTargetLang: String = TranslationConfig.DEFAULT_TARGET_LANG,
    val translationUserId: String = TranslationConfig.DEFAULT_USER_ID,
    val translationMaxConcurrency: Int = TranslationConfig.DEFAULT_MAX_CONCURRENCY,
    val translationTestInProgress: Boolean = false,
    val translationTestMessage: String? = null
)

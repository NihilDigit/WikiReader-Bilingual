package org.nsh07.wikireader.ui.settingsScreen.viewModel

import android.util.Log
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.nsh07.wikireader.WikiReaderApplication
import org.nsh07.wikireader.data.AppDatabaseRepository
import org.nsh07.wikireader.data.AppStatus
import org.nsh07.wikireader.data.PreferencesRepository
import org.nsh07.wikireader.network.HostSelectionInterceptor
import org.nsh07.wikireader.translation.TranslationConfig
import org.nsh07.wikireader.translation.TranslationRepository

class SettingsViewModel(
    private val appStatusFlow: MutableStateFlow<AppStatus>,
    private val preferencesStateMutableFlow: MutableStateFlow<PreferencesState>,
    private val interceptor: HostSelectionInterceptor,
    private val translationRepository: TranslationRepository,
    private val appDatabaseRepository: AppDatabaseRepository,
    private val appPreferencesRepository: PreferencesRepository
) : ViewModel() {

    val preferencesState: StateFlow<PreferencesState> = preferencesStateMutableFlow.asStateFlow()

    init {
        viewModelScope.launch(Dispatchers.IO) {
            val colorScheme = appPreferencesRepository.readStringPreference("color-scheme")
                ?: appPreferencesRepository.saveStringPreference(
                    "color-scheme",
                    Color.White.toString()
                )
            val fontStyle = appPreferencesRepository.readStringPreference("font-style")
                ?: appPreferencesRepository.saveStringPreference("font-style", "sans")
            val lang = appPreferencesRepository.readStringPreference("lang")
                ?: appPreferencesRepository.saveStringPreference("lang", "en")
            val theme = appPreferencesRepository.readStringPreference("theme")
                ?: appPreferencesRepository.saveStringPreference("theme", "auto")
            val fontSize = appPreferencesRepository.readIntPreference("font-size")
                ?: appPreferencesRepository.saveIntPreference("font-size", 16)
            val blackTheme = appPreferencesRepository.readBooleanPreference("black-theme")
                ?: appPreferencesRepository.saveBooleanPreference("black-theme", false)
            val dataSaver = appPreferencesRepository.readBooleanPreference("data-saver")
                ?: appPreferencesRepository.saveBooleanPreference("data-saver", false)
            val feedEnabled = appPreferencesRepository.readBooleanPreference("feed-enabled")
                ?: appPreferencesRepository.saveBooleanPreference("feed-enabled", true)
            val expandedSections =
                appPreferencesRepository.readBooleanPreference("expanded-sections")
                    ?: appPreferencesRepository.saveBooleanPreference("expanded-sections", false)
            val imageBackground = appPreferencesRepository.readBooleanPreference("image-background")
                ?: appPreferencesRepository.saveBooleanPreference("image-background", false)
            val immersiveMode = appPreferencesRepository.readBooleanPreference("immersive-mode")
                ?: appPreferencesRepository.saveBooleanPreference("immersive-mode", true)
            val renderMath = appPreferencesRepository.readBooleanPreference("render-math")
                ?: appPreferencesRepository.saveBooleanPreference("render-math", true)
            val browsingHistory = appPreferencesRepository.readBooleanPreference("browsing-history")
                ?: appPreferencesRepository.saveBooleanPreference("browsing-history", true)
            val searchHistory = appPreferencesRepository.readBooleanPreference("search-history")
                ?: appPreferencesRepository.saveBooleanPreference("search-history", true)
            val bilingualEnabled =
                appPreferencesRepository.readBooleanPreference("bilingual-enabled")
                    ?: appPreferencesRepository.saveBooleanPreference("bilingual-enabled", false)
            val bilingualBlurBlocks =
                appPreferencesRepository.readBooleanPreference("bilingual-blur-blocks")
                    ?: appPreferencesRepository.saveBooleanPreference("bilingual-blur-blocks", true)
            val bilingualAutoTranslateBlocks =
                appPreferencesRepository.readBooleanPreference("bilingual-auto-translate-blocks")
                    ?: appPreferencesRepository.saveBooleanPreference(
                        "bilingual-auto-translate-blocks",
                        true
                    )
            val legacyTranslationApiKey =
                appPreferencesRepository.readStringPreference("translation-api-key").orEmpty()
            val translationDeepSeekApiKey =
                appPreferencesRepository.readStringPreference("translation-deepseek-api-key")
                    ?: appPreferencesRepository.saveStringPreference(
                        "translation-deepseek-api-key",
                        legacyTranslationApiKey
                    )
            val translationTargetLang =
                appPreferencesRepository.readStringPreference("translation-target-lang")
                    ?: appPreferencesRepository.saveStringPreference(
                        "translation-target-lang",
                        TranslationConfig.DEFAULT_TARGET_LANG
                    )
            val translationMaxConcurrency =
                appPreferencesRepository.readIntPreference("translation-max-concurrency")
                    ?: appPreferencesRepository.saveIntPreference(
                        "translation-max-concurrency",
                        TranslationConfig.DEFAULT_MAX_CONCURRENCY
                    )

            preferencesStateMutableFlow.update { currentState ->
                currentState.copy(
                    blackTheme = blackTheme,
                    colorScheme = colorScheme,
                    dataSaver = dataSaver,
                    feedEnabled = feedEnabled,
                    expandedSections = expandedSections,
                    fontSize = fontSize,
                    fontStyle = fontStyle,
                    imageBackground = imageBackground,
                    immersiveMode = immersiveMode,
                    lang = lang,
                    renderMath = renderMath,
                    searchHistory = searchHistory,
                    browsingHistory = browsingHistory,
                    theme = theme,
                    bilingualEnabled = bilingualEnabled,
                    bilingualBlurBlocks = bilingualBlurBlocks,
                    bilingualAutoTranslateBlocks = bilingualAutoTranslateBlocks,
                    translationDeepSeekApiKey = translationDeepSeekApiKey,
                    translationBaseUrl = TranslationConfig.DEFAULT_BASE_URL,
                    translationModel = TranslationConfig.DEFAULT_MODEL,
                    translationTargetLang = translationTargetLang,
                    translationUserId = TranslationConfig.DEFAULT_USER_ID,
                    translationMaxConcurrency = translationMaxConcurrency
                )
            }

            appDatabaseRepository.deleteOldSearchHistory()
            appDatabaseRepository.deleteOldViewHistory()

            interceptor.setHost("$lang.wikipedia.org")

            appStatusFlow.update { AppStatus.INITIALIZED }
        }
    }

    fun onAction(action: SettingsAction) {
        when (action) {
            is SettingsAction.SaveTheme -> viewModelScope.launch {
                preferencesStateMutableFlow.update { currentState ->
                    currentState.copy(theme = action.value)
                }
                appPreferencesRepository.saveStringPreference("theme", action.value)
            }

            is SettingsAction.SaveFontStyle -> viewModelScope.launch {
                preferencesStateMutableFlow.update { currentState ->
                    currentState.copy(fontStyle = action.value)
                }
                appPreferencesRepository.saveStringPreference("font-style", action.value)
            }

            is SettingsAction.SaveLang -> {
                val lang = action.value
                interceptor.setHost("$lang.wikipedia.org")
                preferencesStateMutableFlow.update { currentState ->
                    currentState.copy(lang = lang)
                }
                viewModelScope.launch {
                    appPreferencesRepository.saveStringPreference("lang", lang)
                }
            }

            is SettingsAction.SaveColorScheme -> viewModelScope.launch {
                preferencesStateMutableFlow.update { currentState ->
                    currentState.copy(colorScheme = action.value)
                }
                appPreferencesRepository.saveStringPreference("color-scheme", action.value)
            }

            is SettingsAction.SaveTranslationDeepSeekApiKey -> viewModelScope.launch {
                val apiKey = action.value.trim()
                preferencesStateMutableFlow.update { currentState ->
                    currentState.copy(translationDeepSeekApiKey = apiKey)
                }
                appPreferencesRepository.saveStringPreference("translation-deepseek-api-key", apiKey)
            }

            is SettingsAction.SaveTranslationTargetLang -> viewModelScope.launch {
                preferencesStateMutableFlow.update { currentState ->
                    currentState.copy(translationTargetLang = action.value.trim())
                }
                appPreferencesRepository.saveStringPreference(
                    "translation-target-lang",
                    action.value.trim()
                )
            }

            is SettingsAction.SaveTranslationMaxConcurrency -> viewModelScope.launch {
                val concurrency = action.value.coerceIn(1, 16)
                preferencesStateMutableFlow.update { currentState ->
                    currentState.copy(translationMaxConcurrency = concurrency)
                }
                appPreferencesRepository.saveIntPreference("translation-max-concurrency", concurrency)
            }

            SettingsAction.TestTranslationProvider -> viewModelScope.launch(Dispatchers.IO) {
                val prefs = preferencesState.value
                val config = TranslationConfig(
                    enabled = true,
                    apiKey = prefs.activeTranslationApiKey(),
                    baseUrl = prefs.translationBaseUrl,
                    model = prefs.translationModel,
                    targetLang = prefs.translationTargetLang,
                    userId = prefs.translationUserId,
                    maxConcurrency = 1
                )

                if (!config.canTranslate) {
                    preferencesStateMutableFlow.update { currentState ->
                        currentState.copy(
                            translationTestInProgress = false,
                            translationTestMessage = "Missing API key, base URL, or model."
                        )
                    }
                    return@launch
                }

                preferencesStateMutableFlow.update { currentState ->
                    currentState.copy(
                        translationTestInProgress = true,
                        translationTestMessage = null
                    )
                }

                val result = runCatching {
                    translationRepository.translateContent(
                        text = "Hello.",
                        sourceLang = "en",
                        targetLang = config.targetLang.ifBlank { TranslationConfig.DEFAULT_TARGET_LANG },
                        config = config
                    )
                }

                preferencesStateMutableFlow.update { currentState ->
                    if (result.isSuccess && result.getOrNull().orEmpty().isNotBlank()) {
                        currentState.copy(
                            translationTestInProgress = false,
                            translationTestMessage = "Translation test passed."
                        )
                    } else {
                        val message = sanitizeTranslationError(
                            result.exceptionOrNull()?.message ?: "Empty provider response"
                        )
                        Log.w("Translation", "Settings test failed: $message")
                        currentState.copy(
                            translationTestInProgress = false,
                            translationTestMessage = "Translation test failed: $message"
                        )
                    }
                }
            }

            is SettingsAction.SaveBlackTheme -> viewModelScope.launch {
                preferencesStateMutableFlow.update { currentState ->
                    currentState.copy(blackTheme = action.value)
                }
                appPreferencesRepository.saveBooleanPreference("black-theme", action.value)
            }

            is SettingsAction.SaveBilingualAutoTranslateBlocks -> viewModelScope.launch {
                appPreferencesRepository.saveBooleanPreference(
                    "bilingual-auto-translate-blocks",
                    action.value
                )
                preferencesStateMutableFlow.update { currentState ->
                    currentState.copy(bilingualAutoTranslateBlocks = action.value)
                }
            }

            is SettingsAction.SaveBilingualBlurBlocks -> viewModelScope.launch {
                appPreferencesRepository.saveBooleanPreference(
                    "bilingual-blur-blocks",
                    action.value
                )
                preferencesStateMutableFlow.update { currentState ->
                    currentState.copy(bilingualBlurBlocks = action.value)
                }
            }

            is SettingsAction.SaveBilingualEnabled -> viewModelScope.launch {
                appPreferencesRepository.saveBooleanPreference("bilingual-enabled", action.value)
                preferencesStateMutableFlow.update { currentState ->
                    currentState.copy(bilingualEnabled = action.value)
                }
            }

            is SettingsAction.SaveFontSize -> viewModelScope.launch {
                appPreferencesRepository.saveIntPreference("font-size", action.value)
                preferencesStateMutableFlow.update { currentState ->
                    currentState.copy(fontSize = action.value)
                }
            }

            is SettingsAction.SaveExpandedSections -> viewModelScope.launch {
                appPreferencesRepository.saveBooleanPreference("expanded-sections", action.value)
                preferencesStateMutableFlow.update { currentState ->
                    currentState.copy(expandedSections = action.value)
                }
            }

            is SettingsAction.SaveRenderMath -> viewModelScope.launch {
                appPreferencesRepository.saveBooleanPreference("render-math", action.value)
                preferencesStateMutableFlow.update { currentState ->
                    currentState.copy(renderMath = action.value)
                }
            }

            is SettingsAction.SaveHistory -> viewModelScope.launch {
                appPreferencesRepository.saveBooleanPreference("browsing-history", action.value)
                preferencesStateMutableFlow.update { currentState ->
                    currentState.copy(browsingHistory = action.value)
                }
            }

            is SettingsAction.SaveSearchHistory -> viewModelScope.launch {
                appPreferencesRepository.saveBooleanPreference("search-history", action.value)
                preferencesStateMutableFlow.update { currentState ->
                    currentState.copy(searchHistory = action.value)
                }
            }

            is SettingsAction.SaveDataSaver -> viewModelScope.launch {
                appPreferencesRepository.saveBooleanPreference("data-saver", action.value)
                preferencesStateMutableFlow.update { currentState ->
                    currentState.copy(dataSaver = action.value)
                }
            }

            is SettingsAction.SaveFeedEnabled -> viewModelScope.launch {
                appPreferencesRepository.saveBooleanPreference("feed-enabled", action.value)
                preferencesStateMutableFlow.update { currentState ->
                    currentState.copy(feedEnabled = action.value)
                }
            }

            is SettingsAction.SaveImageBackground -> viewModelScope.launch {
                appPreferencesRepository.saveBooleanPreference("image-background", action.value)
                preferencesStateMutableFlow.update { currentState ->
                    currentState.copy(imageBackground = action.value)
                }
            }

            is SettingsAction.SaveImmersiveMode -> viewModelScope.launch {
                appPreferencesRepository.saveBooleanPreference("immersive-mode", action.value)
                preferencesStateMutableFlow.update { currentState ->
                    currentState.copy(immersiveMode = action.value)
                }
            }

            is SettingsAction.ResetSettings -> viewModelScope.launch {
                try {
                    appPreferencesRepository.resetSettings()
                    preferencesStateMutableFlow.update {
                        PreferencesState()
                    }
                } catch (e: Exception) {
                    Log.e("ViewModel", "Error in restoring settings: ${e.message}")
                    e.printStackTrace()
                }
            }
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val application = (this[APPLICATION_KEY] as WikiReaderApplication)
                val appStatusFlow = application.container.appStatus
                val preferencesStateMutableFlow = application.container.preferencesStateMutableFlow
                val interceptor = application.container.interceptor
                val translationRepository = application.container.translationRepository
                val appPreferencesRepository = application.container.appPreferencesRepository
                val appHistoryRepository = application.container.appDatabaseRepository
                SettingsViewModel(
                    appStatusFlow = appStatusFlow,
                    preferencesStateMutableFlow = preferencesStateMutableFlow,
                    interceptor = interceptor,
                    translationRepository = translationRepository,
                    appPreferencesRepository = appPreferencesRepository,
                    appDatabaseRepository = appHistoryRepository
                )
            }
        }
    }

}

fun PreferencesState.activeTranslationApiKey(): String =
    translationDeepSeekApiKey

private fun sanitizeTranslationError(message: String): String =
    message
        .replace(Regex("sk-[A-Za-z0-9_-]+"), "sk-...")
        .take(240)

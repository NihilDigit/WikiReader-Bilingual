package org.nsh07.wikireader.ui.homeScreen

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TextButton
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.Hyphens
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.compose.ui.util.fastAny
import androidx.compose.ui.util.fastForEach
import com.github.tomtung.latex2unicode.LaTeX2Unicode
import org.nsh07.wikireader.R
import org.nsh07.wikireader.parser.ReferenceData.infoboxTemplates
import org.nsh07.wikireader.translation.BilingualSectionTranslation
import org.nsh07.wikireader.translation.BilingualTextKey
import org.nsh07.wikireader.translation.BilingualTranslationStatus
import org.nsh07.wikireader.translation.isArticleSubheadingNode
import org.nsh07.wikireader.translation.translatableArticleParagraphs
import kotlin.math.roundToInt
import kotlin.text.Typography.nbsp

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun ParsedBodyText(
    body: List<AnnotatedString>,
    lang: String,
    fontSize: Int,
    fontFamily: FontFamily,
    sharedScope: SharedTransitionScope,
    background: Boolean,
    renderMath: Boolean,
    darkTheme: Boolean,
    dataSaver: Boolean,
    onLinkClick: (String) -> Unit,
    onGalleryImageClick: (String, String) -> Unit,
    showRef: (String) -> Unit,
    modifier: Modifier = Modifier,
    checkFirstImage: Boolean = false,
    pageImageUri: String? = null,
    sectionIndex: Int,
    translations: Map<BilingualTextKey, BilingualSectionTranslation>,
    targetLang: String?,
    onRetryTranslation: (BilingualTextKey) -> Unit,
    onExplainText: (String, String, String) -> Unit,
    blurParagraphTranslations: Boolean = true,
    autoTranslateParagraphs: Boolean = true
) {
    val context = LocalContext.current
    val dpi = LocalDensity.current.density

    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = modifier) {
        body.forEachIndexed { itemIndex, it ->
            if (it.startsWith("[[File:")) {
                if (!dataSaver) {
                    val key = BilingualTextKey(sectionIndex, itemIndex, 0)
                    with(sharedScope) {
                        ImageWithCaption(
                            text = it.toString(),
                            lang = lang,
                            fontSize = fontSize,
                            onLinkClick = onLinkClick,
                            onClick = onGalleryImageClick,
                            darkTheme = darkTheme,
                            background = background,
                            checkFirstImage = checkFirstImage,
                            pageImageUri = pageImageUri,
                            captionTranslation = translations[key],
                            targetLang = targetLang,
                            fontFamily = fontFamily,
                            captionTranslationKey = key,
                            onRetryTranslation = onRetryTranslation
                        )
                    }
                }
            } else if (it.startsWith("<gallery")) {
                if (!dataSaver) {
                    Gallery(
                        text = it.toString(),
                        lang = lang,
                        fontSize = fontSize,
                        onClick = onGalleryImageClick,
                        onLinkClick = onLinkClick,
                        background = background,
                        sectionIndex = sectionIndex,
                        itemIndex = itemIndex,
                        translations = translations,
                        targetLang = targetLang,
                        fontFamily = fontFamily,
                        onRetryTranslation = onRetryTranslation
                    )
                }
            } else if (it.toString().lowercase().startsWith("<math")) {
                // Extract LaTeX content properly - between opening tag and closing tag
                val fullText = it.toString()
                val openTagEnd = fullText.indexOf('>')
                val closeTagStart = fullText.lowercase().indexOf("</math>")
                val latexContent = when {
                    openTagEnd != -1 && closeTagStart > openTagEnd -> 
                        fullText.substring(openTagEnd + 1, closeTagStart)
                    openTagEnd != -1 -> 
                        fullText.substring(openTagEnd + 1)
                    else -> fullText
                }
                
                if (renderMath) {
                    EquationImage(
                        context = context,
                        dpi = dpi,
                        latex = remember { latexContent },
                        fontSize = fontSize,
                        darkTheme = darkTheme
                    )
                } else {
                    val converted = try {
                        LaTeX2Unicode.convert(latexContent).replace(' ', nbsp)
                    } catch (e: Exception) {
                        latexContent // Fallback to raw LaTeX if conversion fails
                    }
                    Text(
                        text = converted,
                        fontFamily = FontFamily.Serif,
                        fontSize = (fontSize + 4).sp,
                        lineHeight = (24 * (fontSize / 16.0) + 4).toInt().sp,
                        modifier = Modifier
                            .padding(horizontal = 16.dp)
                    )
                }
            } else if (it.startsWith("{|")) {
                AsyncWikitable(
                    text = it.toString(),
                    fontSize = fontSize,
                    onLinkClick = onLinkClick,
                    showRef = showRef
                )
            } else if (
                infoboxTemplates.fastAny { item -> (it.startsWith(item, true)) }
            ) {
                AsyncInfobox(
                    text = it.toString(),
                    lang = lang,
                    fontSize = fontSize,
                    darkTheme = darkTheme,
                    background = background,
                    onImageClick = onGalleryImageClick,
                    onLinkClick = onLinkClick,
                    showRef = showRef
                )
            } else if (it.isArticleSubheadingNode()) {
                val key = BilingualTextKey(sectionIndex, itemIndex, 0)
                ArticleParagraphText(
                    text = it,
                    fontSize = fontSize,
                    fontFamily = fontFamily,
                    onExplainText = onExplainText
                )
                PlainBilingualTranslationText(
                    translation = translations[key],
                    targetLang = targetLang,
                    fontSize = fontSize - 1,
                    fontFamily = fontFamily,
                    onRetry = { onRetryTranslation(key) },
                    modifier = Modifier.padding(
                        start = 16.dp,
                        end = 16.dp,
                        top = 0.dp,
                        bottom = 12.dp
                    )
                )
            } else {
                val paragraphs = it.translatableArticleParagraphs()
                if (paragraphs.isEmpty()) {
                    ArticleParagraphText(
                        text = it,
                        fontSize = fontSize,
                        fontFamily = fontFamily,
                        onExplainText = onExplainText
                    )
                } else {
                    paragraphs.forEachIndexed { paragraphIndex, paragraph ->
                        val key = BilingualTextKey(sectionIndex, itemIndex, paragraphIndex)
                        ArticleParagraphText(
                            text = paragraph,
                            fontSize = fontSize,
                            fontFamily = fontFamily,
                            onExplainText = onExplainText
                        )
                        val translation = translations[key]
                            ?: if (!autoTranslateParagraphs) {
                                BilingualSectionTranslation(BilingualTranslationStatus.IDLE)
                            } else {
                                null
                            }
                        BilingualTranslationBlock(
                            translation = translation,
                            targetLang = targetLang,
                            fontSize = fontSize,
                            fontFamily = fontFamily,
                            onRetry = { onRetryTranslation(key) },
                            blurred = blurParagraphTranslations && autoTranslateParagraphs
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun PlainBilingualTranslationText(
    translation: BilingualSectionTranslation?,
    targetLang: String?,
    fontSize: Int,
    fontFamily: FontFamily,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
    textAlign: TextAlign = TextAlign.Start
) {
    if (translation == null || targetLang == null) return

    when (translation.status) {
        BilingualTranslationStatus.IDLE -> return
        BilingualTranslationStatus.LOADING -> Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = modifier.fillMaxWidth()
        ) {
            LoadingIndicator(modifier = Modifier.size(18.dp))
            Text(
                text = stringResource(R.string.translationLoading),
                style = typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 8.dp)
            )
        }
        BilingualTranslationStatus.READY -> Text(
            text = translation.text.orEmpty(),
            style = typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = fontSize.sp,
            fontFamily = fontFamily,
            textAlign = textAlign,
            lineHeight = (24 * (fontSize / 16.0)).toInt().sp,
            modifier = modifier.fillMaxWidth()
        )
        BilingualTranslationStatus.ERROR -> Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = modifier.fillMaxWidth()
        ) {
            Text(
                text = stringResource(R.string.translationUnavailable),
                style = typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.weight(1f)
            )
            TextButton(onClick = onRetry) {
                Text(stringResource(R.string.retry))
            }
        }
    }
}

@Composable
private fun ArticleParagraphText(
    text: AnnotatedString,
    fontSize: Int,
    fontFamily: FontFamily,
    onExplainText: (String, String, String) -> Unit
) {
    var layoutResult by remember { mutableStateOf<TextLayoutResult?>(null) }
    val plainText = text.toString()
    var pendingExplanation by remember(plainText) { mutableStateOf<PendingWordExplanation?>(null) }
    var paragraphBoxSize by remember { mutableStateOf(IntSize.Zero) }
    var chipSize by remember { mutableStateOf(IntSize.Zero) }
    val density = LocalDensity.current
    val chipVerticalGapPx = with(density) { 8.dp.toPx() }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(bottom = if (pendingExplanation != null) 48.dp else 0.dp)
            .onSizeChanged { paragraphBoxSize = it }
    ) {
        Text(
            text = text,
            style = typography.bodyLarge.copy(hyphens = Hyphens.Auto),
            fontSize = fontSize.sp,
            fontFamily = fontFamily,
            lineHeight = (24 * (fontSize / 16.0)).toInt().sp,
            onTextLayout = { layoutResult = it },
            modifier = Modifier
                .fillMaxWidth()
                .pointerInput(plainText) {
                    detectTapGestures(
                        onTap = { offset ->
                            pendingExplanation = null
                            val index = layoutResult?.getOffsetForPosition(offset)
                                ?: return@detectTapGestures
                            val word = plainText.wordAt(index) ?: return@detectTapGestures
                            val sentence = plainText.sentenceAt(index) ?: plainText
                            pendingExplanation = PendingWordExplanation(
                                word = word,
                                sentence = sentence,
                                offset = offset
                            )
                        },
                        onDoubleTap = { offset ->
                            pendingExplanation = null
                            val index = layoutResult?.getOffsetForPosition(offset)
                                ?: return@detectTapGestures
                            val sentence = plainText.sentenceAt(index) ?: return@detectTapGestures
                            onExplainText(sentence, plainText, "sentence")
                        }
                    )
                }
        )
        pendingExplanation?.let { pending ->
            AssistChip(
                onClick = {
                    pendingExplanation = null
                    onExplainText(pending.word, pending.sentence, "word")
                },
                label = {
                    Text(stringResource(R.string.explainWord, pending.word))
                },
                colors = AssistChipDefaults.assistChipColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    labelColor = MaterialTheme.colorScheme.onPrimaryContainer
                ),
                elevation = AssistChipDefaults.assistChipElevation(),
                border = null,
                shape = MaterialTheme.shapes.large,
                modifier = Modifier
                    .zIndex(1f)
                    .widthIn(max = with(density) { paragraphBoxSize.width.toDp() })
                    .onSizeChanged { chipSize = it }
                    .offset {
                        val maxX = (paragraphBoxSize.width - chipSize.width).coerceAtLeast(0)
                        val maxY = (paragraphBoxSize.height - chipSize.height).coerceAtLeast(0)
                        val belowY = pending.offset.y + chipVerticalGapPx
                        val aboveY = pending.offset.y - chipVerticalGapPx - chipSize.height
                        val targetY = if (belowY <= maxY) belowY else aboveY

                        IntOffset(
                            x = pending.offset.x.roundToInt().coerceIn(0, maxX),
                            y = targetY.roundToInt().coerceIn(0, maxY)
                        )
                    }
            )
        }
    }
}

private data class PendingWordExplanation(
    val word: String,
    val sentence: String,
    val offset: Offset
)

private fun String.wordAt(index: Int): String? {
    if (isBlank()) return null
    val safeIndex = index.coerceIn(0, lastIndex)
    if (!this[safeIndex].isLetterOrDigit()) return null
    var start = safeIndex
    var end = safeIndex + 1
    while (start > 0 && this[start - 1].isWordChar()) start--
    while (end < length && this[end].isWordChar()) end++
    return substring(start, end).trim().takeIf { it.isNotBlank() }
}

private fun String.sentenceAt(index: Int): String? {
    if (isBlank()) return null
    val safeIndex = index.coerceIn(0, lastIndex)
    var start = safeIndex
    var end = safeIndex + 1
    while (start > 0 && this[start - 1] !in ".!?。！？\n") start--
    while (end < length && this[end] !in ".!?。！？\n") end++
    if (end < length) end++
    return substring(start, end).trim().takeIf { it.isNotBlank() }
}

private fun Char.isWordChar(): Boolean =
    isLetterOrDigit() || this == '\'' || this == '-'

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun BilingualTranslationBlock(
    translation: BilingualSectionTranslation?,
    targetLang: String?,
    fontSize: Int,
    fontFamily: FontFamily,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
    blurred: Boolean = true,
    textAlign: TextAlign = TextAlign.Start
) {
    if (translation == null || targetLang == null) return
    var revealed by rememberSaveable(translation.text) { mutableStateOf(false) }

    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        shape = MaterialTheme.shapes.medium,
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, top = 6.dp, bottom = 18.dp)
            .defaultMinSize(minHeight = 52.dp)
            .clickable(
                enabled = translation.status == BilingualTranslationStatus.READY ||
                        translation.status == BilingualTranslationStatus.IDLE,
                onClick = {
                    if (translation.status == BilingualTranslationStatus.IDLE) onRetry()
                    else revealed = !revealed
                }
            )
    ) {
        Column(
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            BilingualTranslationBlockContent(
                translation = translation,
                revealed = revealed,
                fontSize = fontSize,
                fontFamily = fontFamily,
                onRetry = onRetry,
                blurred = blurred,
                textAlign = textAlign
            )
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun BilingualTranslationBlockContent(
    translation: BilingualSectionTranslation,
    revealed: Boolean,
    fontSize: Int,
    fontFamily: FontFamily,
    onRetry: () -> Unit,
    blurred: Boolean,
    textAlign: TextAlign
) {
    when (translation.status) {
        BilingualTranslationStatus.IDLE -> Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                painter = painterResource(R.drawable.translate),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .padding(end = 8.dp)
                    .size(22.dp)
            )
            Text(
                text = stringResource(R.string.translate),
                style = typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center
            )
        }
        BilingualTranslationStatus.LOADING -> Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            LoadingIndicator(modifier = Modifier.size(20.dp))
            Text(
                text = stringResource(R.string.translationLoading),
                style = typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 10.dp)
            )
        }
        BilingualTranslationStatus.READY -> Text(
            text = translation.text.orEmpty(),
            style = typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = fontSize.sp,
            fontFamily = fontFamily,
            textAlign = textAlign,
            lineHeight = (24 * (fontSize / 16.0)).toInt().sp,
            modifier = Modifier
                .fillMaxWidth()
                .then(if (revealed || !blurred) Modifier else Modifier.blur(6.dp))
        )
        BilingualTranslationStatus.ERROR -> Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.translationUnavailable),
                style = typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.weight(1f)
            )
            TextButton(onClick = onRetry) {
                Text(stringResource(R.string.retry))
            }
        }
    }
}

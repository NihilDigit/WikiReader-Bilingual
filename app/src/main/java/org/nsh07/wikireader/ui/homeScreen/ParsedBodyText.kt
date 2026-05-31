package org.nsh07.wikireader.ui.homeScreen

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.Hyphens
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.fastAny
import androidx.compose.ui.util.fastForEach
import com.github.tomtung.latex2unicode.LaTeX2Unicode
import org.nsh07.wikireader.R
import org.nsh07.wikireader.parser.ReferenceData.infoboxTemplates
import org.nsh07.wikireader.translation.BilingualSectionTranslation
import org.nsh07.wikireader.translation.BilingualTranslationStatus
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
    pageImageUri: String? = null
) {
    val context = LocalContext.current
    val dpi = LocalDensity.current.density

    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = modifier) {
        body.fastForEach {
            if (it.startsWith("[[File:")) {
                if (!dataSaver) {
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
                            pageImageUri = pageImageUri
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
                        background = background
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
            } else {
                Text(
                    text = it,
                    style = typography.bodyLarge.copy(hyphens = Hyphens.Auto),
                    fontSize = fontSize.sp,
                    fontFamily = fontFamily,
                    lineHeight = (24 * (fontSize / 16.0)).toInt().sp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                )
            }
        }
    }
}

@Composable
fun BilingualTranslationBlock(
    translation: BilingualSectionTranslation?,
    targetLang: String?,
    fontSize: Int,
    fontFamily: FontFamily,
    modifier: Modifier = Modifier
) {
    if (translation == null || targetLang == null) return

    val text = when (translation.status) {
        BilingualTranslationStatus.IDLE -> null
        BilingualTranslationStatus.LOADING -> stringResource(R.string.translationLoading)
        BilingualTranslationStatus.READY -> translation.text
        BilingualTranslationStatus.ERROR -> stringResource(R.string.translationUnavailable)
    } ?: return

    Text(
        text = text,
        style = typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        fontSize = fontSize.sp,
        fontFamily = fontFamily,
        lineHeight = (24 * (fontSize / 16.0)).toInt().sp,
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 18.dp)
    )
}

package org.nsh07.wikireader.translation

import androidx.compose.ui.text.AnnotatedString

const val ARTICLE_NODE_TAG = "wikireader-node"
const val ARTICLE_SUBHEADING_NODE = "subheading"

private val skippedArticleNodePrefixes = listOf(
    "[[File:",
    "<gallery",
    "<math",
    "{|",
    "{{",
    "{{infobox",
    "{{taxobox",
    "{{automatic taxobox",
    "{{Картка".lowercase()
)

private val renderedHatnotePrefixes = listOf(
    "main article:",
    "main articles:",
    "see also:",
    "further reading:",
    "further information on ",
    "not to be confused with ",
    "for other uses, see ",
    "this article is about "
)

fun String.asTranslatableArticleText(): String? {
    val text = trim()
    if (text.isBlank()) return null

    val lower = text.lowercase()
    if (skippedArticleNodePrefixes.any { lower.startsWith(it.lowercase()) }) return null
    if (renderedHatnotePrefixes.any { lower.startsWith(it) }) return null
    if (lower.startsWith("for ") && lower.contains(", see ")) return null
    if (lower.contains("\" redirects here; not to be confused with ")) return null

    return text
}

fun AnnotatedString.translatableArticleParagraphs(): List<AnnotatedString> {
    if (toString().asTranslatableArticleText() == null) return emptyList()

    return Regex("[^\\n]+")
        .findAll(toString())
        .mapNotNull { match ->
            val start = match.range.first
            val endInclusive = match.range.last
            val paragraph = subSequence(start, endInclusive + 1)
            if (paragraph.toString().asTranslatableArticleText() == null) null else paragraph
        }
        .toList()
}

fun AnnotatedString.isArticleSubheadingNode(): Boolean =
    getStringAnnotations(ARTICLE_NODE_TAG, 0, length)
        .any { it.item == ARTICLE_SUBHEADING_NODE }

fun String.wikitextSubheadingText(): String? {
    val text = trim()
    val match = Regex("^(={3,6})\\s*(.*?)\\s*\\1$").matchEntire(text) ?: return null
    return match.groupValues[2].trim().takeIf { it.isNotBlank() }
}

fun String.articleImageCaptionText(): String? {
    if (!trimStart().startsWith("[[File:", ignoreCase = true)) return null
    return substringAfter('|', "")
        .substringBefore('|')
        .substringBefore("]]")
        .asTranslatableArticleText()
        ?.cleanArticleTextForTranslation()
}

data class GalleryTextUnit(
    val fileName: String,
    val caption: String,
    val alt: String,
    val translationSource: String
) {
    val visibleCaption: String
        get() = caption.ifBlank { alt }
}

fun String.galleryCaptionTexts(): List<String> {
    return galleryTextUnits().mapNotNull { it.translationSource.takeIf(String::isNotBlank) }
}

fun String.galleryTextUnits(): List<GalleryTextUnit> {
    if (!trimStart().startsWith("<gallery", ignoreCase = true)) return emptyList()
    return substringAfter('>', "")
        .substringBefore("</gallery>")
        .lines()
        .mapNotNull { line -> line.galleryTextUnit() }
}

private fun String.galleryTextUnit(): GalleryTextUnit? {
    val parts = splitWikiPipes()
        .map { it.trim() }
        .filter { it.isNotBlank() }
    val fileName = parts.firstOrNull()?.takeIf { it.isNotBlank() } ?: return null
    val fields = parts.drop(1)
    val alt = fields.firstNotNullOfOrNull { field ->
        field.substringAfter("alt=", missingDelimiterValue = "")
            .takeIf { field.startsWith("alt=", ignoreCase = true) && it.isNotBlank() }
    }.orEmpty()
    val caption = fields
        .filterNot { it.startsWith("alt=", ignoreCase = true) }
        .filterNot { it.isGalleryDisplayOption() }
        .joinToString(separator = " ")
        .cleanArticleTextForTranslation()
    val cleanedAlt = alt.cleanArticleTextForTranslation()
    val translationSource = when {
        caption.isNotBlank() && cleanedAlt.isNotBlank() ->
            "Caption: $caption\nAlt text: $cleanedAlt"
        caption.isNotBlank() -> caption
        cleanedAlt.isNotBlank() -> cleanedAlt
        else -> ""
    }.asTranslatableArticleText()?.cleanArticleTextForTranslation().orEmpty()

    return GalleryTextUnit(
        fileName = fileName,
        caption = caption,
        alt = cleanedAlt,
        translationSource = translationSource
    )
}

private fun String.splitWikiPipes(): List<String> {
    val result = mutableListOf<String>()
    val current = StringBuilder()
    var linkDepth = 0
    var templateDepth = 0
    var i = 0

    while (i < length) {
        when {
            startsWith("[[", i) -> {
                linkDepth++
                current.append("[[")
                i += 2
            }
            startsWith("]]", i) && linkDepth > 0 -> {
                linkDepth--
                current.append("]]")
                i += 2
            }
            startsWith("{{", i) -> {
                templateDepth++
                current.append("{{")
                i += 2
            }
            startsWith("}}", i) && templateDepth > 0 -> {
                templateDepth--
                current.append("}}")
                i += 2
            }
            this[i] == '|' && linkDepth == 0 && templateDepth == 0 -> {
                result += current.toString()
                current.clear()
                i++
            }
            else -> {
                current.append(this[i])
                i++
            }
        }
    }

    result += current.toString()
    return result
}

private fun String.isGalleryDisplayOption(): Boolean {
    val lower = lowercase()
    return lower in setOf(
        "thumb",
        "thumbnail",
        "frame",
        "frameless",
        "border",
        "center",
        "left",
        "right"
    ) || lower.endsWith("px") || lower.startsWith("upright")
}

fun String.cleanArticleTextForTranslation(): String =
    replace(Regex("\\[[0-9]+]"), "")
        .replace(Regex("\\[citation needed]", RegexOption.IGNORE_CASE), "")
        .replace(Regex("(?<=[\\p{L}\\p{N}\\])）])\\s*[0-9]{1,3}(?=\\s|[.,;:!?，。；：！？)]|$)"), "")
        .replace(Regex("[ \\t]{2,}"), " ")
        .trim()

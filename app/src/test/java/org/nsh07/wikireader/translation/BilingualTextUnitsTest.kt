package org.nsh07.wikireader.translation

import androidx.compose.ui.text.AnnotatedString
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BilingualTextUnitsTest {
    @Test
    fun translatableArticleParagraphs_splitsRenderedTextByLine() {
        val paragraphs = AnnotatedString("First paragraph.\nSecond paragraph.\n\nThird paragraph.")
            .translatableArticleParagraphs()
            .map { it.toString() }

        assertEquals(
            listOf("First paragraph.", "Second paragraph.", "Third paragraph."),
            paragraphs
        )
    }

    @Test
    fun translatableArticleParagraphs_skipsNonBodyNodes() {
        val skipped = listOf(
            "[[File:Example.jpg|caption]]",
            "<gallery>Example.jpg</gallery>",
            "<math display=\"block\">x</math>",
            "{| class=\"wikitable\"",
            "{{Infobox park",
            "{{Short description|Urban park}}"
        )

        skipped.forEach {
            assertTrue(AnnotatedString(it).translatableArticleParagraphs().isEmpty())
        }
    }

    @Test
    fun captions_areExtractedForTranslation() {
        assertEquals(
            "A public park in Nashville",
            "[[File:Park.jpg|A public park in Nashville|thumb]]".articleImageCaptionText()
        )
        assertEquals(
            listOf("First caption", "Second caption"),
            "<gallery>\nA.jpg|First caption\nB.jpg|Second caption\n</gallery>".galleryCaptionTexts()
        )
        assertEquals(
            listOf("Caption: First caption\nAlt text: A train bridge"),
            "<gallery>\nA.jpg|First caption|alt=A train bridge\n</gallery>".galleryCaptionTexts()
        )
        assertEquals(
            "A train bridge",
            "<gallery>\nA.jpg|alt=A train bridge\n</gallery>".galleryTextUnits().single().visibleCaption
        )
        assertEquals(
            listOf("A [[railroad|railroad bridge]]"),
            "<gallery>\nA.jpg|A [[railroad|railroad bridge]]\n</gallery>".galleryCaptionTexts()
        )
    }

    @Test
    fun wikitextSubheadingText_detectsWikiHeadingStructureOnly() {
        assertEquals("Site history", "===Site history===".wikitextSubheadingText())
        assertEquals("Early concepts", "==== Early concepts ====".wikitextSubheadingText())
        assertEquals(null, "Site history".wikitextSubheadingText())
        assertEquals(null, "==Top-level section==".wikitextSubheadingText())
        assertEquals(null, "This is a sentence.".wikitextSubheadingText())
    }

    @Test
    fun cleanArticleTextForTranslation_removesCitationMarkers() {
        assertEquals(
            "Bicentennial Mall is an urban park in Nashville.",
            "Bicentennial Mall1 is an urban park in Nashville.[2]".cleanArticleTextForTranslation()
        )
    }

}

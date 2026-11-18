// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: GPL-3.0-or-later

package org.prauga.documents.formats.markdown

import org.junit.Assert.assertEquals
import org.junit.Test

class MarkdownTest {

    @Test
    fun readsHeadingsWithTheirLevel() {
        assertEquals(
            listOf(MarkdownBlock.Heading(1, "Title"), MarkdownBlock.Heading(3, "Part")),
            parseMarkdown("# Title\n### Part")
        )
    }

    @Test
    fun joinsConsecutiveLinesIntoOneParagraph() {
        assertEquals(
            listOf(MarkdownBlock.Paragraph("one two"), MarkdownBlock.Paragraph("three")),
            parseMarkdown("one\n  two\n\nthree")
        )
    }

    @Test
    fun numbersOrderedItemsFromOneAndRestartsAfterABreak() {
        assertEquals(
            listOf(
                MarkdownBlock.ListItem(ordered = true, number = 1, text = "a"),
                MarkdownBlock.ListItem(ordered = true, number = 2, text = "b"),
                MarkdownBlock.Paragraph("text"),
                MarkdownBlock.ListItem(ordered = true, number = 1, text = "c"),
            ),
            parseMarkdown("5. a\n6) b\ntext\n9. c")
        )
    }

    @Test
    fun readsBulletsQuotesAndRules() {
        assertEquals(
            listOf(
                MarkdownBlock.ListItem(ordered = false, number = null, text = "dash"),
                MarkdownBlock.ListItem(ordered = false, number = null, text = "star"),
                MarkdownBlock.Quote("quoted"),
                MarkdownBlock.Rule,
                MarkdownBlock.Rule,
            ),
            parseMarkdown("- dash\n* star\n> quoted\n---\n_ _ _")
        )
    }

    @Test
    fun keepsFencedCodeAsWritten() {
        assertEquals(
            listOf(MarkdownBlock.CodeBlock("# not a heading\n  indented")),
            parseMarkdown("```kotlin\n# not a heading\n  indented\n```")
        )
    }

    @Test
    fun keepsAnUnclosedFenceToTheEnd() {
        assertEquals(listOf(MarkdownBlock.CodeBlock("a\nb")), parseMarkdown("```\na\nb"))
    }
}

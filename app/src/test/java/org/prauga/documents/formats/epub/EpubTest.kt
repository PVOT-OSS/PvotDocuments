// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: GPL-3.0-or-later

package org.prauga.documents.formats.epub

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.prauga.documents.formats.fixture
import org.prauga.documents.formats.richtext.DocImage
import org.prauga.documents.formats.richtext.DocPageBreak
import org.prauga.documents.formats.richtext.DocParagraph
import org.prauga.documents.formats.richtext.DocTable
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class EpubTest {

    @Test
    fun readsChaptersInSpineOrderAndSkipsNonLinearOnes() = parseEpub(fixture("book.epub")).use { book ->
        val text = book.blocks.filterIsInstance<DocParagraph>().map { it.text() }
        assertEquals("Chapter One", text.first())
        assertEquals(listOf("Chapter Two", "The end."), text.takeLast(2))
        assertTrue("Skipped notes" !in text)
        assertEquals(1, book.blocks.count { it == DocPageBreak })
    }

    @Test
    fun keepsHeadingsEmphasisEntitiesAndLinks() = parseEpub(fixture("book.epub")).use { book ->
        val paragraphs = book.blocks.filterIsInstance<DocParagraph>()
        assertEquals(1, paragraphs.first().heading)
        assertEquals(2, paragraphs.first { it.text() == "Chapter Two" }.heading)
        val body = paragraphs.first { it.text().startsWith("Plain") }
        assertEquals("Plain, bold and italic text — with a link.", body.text())
        assertTrue(body.runs.single { it.text == "bold" }.style.bold)
        assertTrue(body.runs.single { it.text == "italic" }.style.italic)
        assertEquals("https://example.org", body.runs.single { it.text == "link" }.style.link)
    }

    @Test
    fun numbersListsFromTheirStartAndReadsTablesAndPictures() = parseEpub(fixture("book.epub")).use { book ->
        val items = book.blocks.filterIsInstance<DocParagraph>().filter { it.list != null }
        assertEquals(listOf("3." to "Third", "4." to "Fourth", "•" to "Dot"), items.map { it.list?.label to it.text() })
        val table = book.blocks.filterIsInstance<DocTable>().single()
        assertEquals(2, table.columnCount)
        val image = book.blocks.filterIsInstance<DocImage>().single()
        assertEquals("OPS/images/red.png", image.path)
        assertEquals("A red square", image.description)
        assertNotNull(book.image(image.path, 100))
    }

    private fun DocParagraph.text(): String = runs.joinToString("") { it.text }
}

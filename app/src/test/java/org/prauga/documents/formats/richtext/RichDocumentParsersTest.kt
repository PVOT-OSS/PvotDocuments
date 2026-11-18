// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: GPL-3.0-or-later

package org.prauga.documents.formats.richtext

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.prauga.documents.formats.fixture
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class RichDocumentParsersTest {

    @Test
    fun readsWordHeadingsBoldListsAndTables() = check(parseDocx(fixture("report.docx")))

    @Test
    fun readsOpenDocumentTextTheSameWay() = check(parseOdt(fixture("report.odt")))

    private fun check(document: RichDocument) = document.use {
        val paragraphs = it.blocks.filterIsInstance<DocParagraph>().filterNot(DocParagraph::isEmpty)
        val heading = paragraphs.first { p -> p.heading != null }
        assertEquals("Quarterly report", heading.text())

        val intro = paragraphs.first { p -> p.text().startsWith("This is a") }
        assertTrue(intro.runs.any { run -> run.text.trim() == "test" && run.style.bold })

        val items = paragraphs.filter { p -> p.list != null }.map { p -> p.text() }
        assertEquals(listOf("One", "Two"), items)

        val table = it.blocks.filterIsInstance<DocTable>().single()
        val cells = table.rows.map { row -> row.map { cell -> cell.text() } }
        assertEquals(listOf(listOf("A", "B"), listOf("1", "2")), cells)
    }

    private fun DocParagraph.text(): String = runs.joinToString("") { it.text }.trim()

    private fun DocTableCell.text() =
        blocks.filterIsInstance<DocParagraph>().joinToString(" ") { p -> p.text() }.trim()
}

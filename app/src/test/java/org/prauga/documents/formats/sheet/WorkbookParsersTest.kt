// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: GPL-3.0-or-later

package org.prauga.documents.formats.sheet

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.prauga.documents.formats.fixture
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class WorkbookParsersTest {

    @Test
    fun readsExcelCellsAndNumbers() = check(parseXlsx(fixture("table.xlsx")))

    @Test
    fun readsOpenDocumentSpreadsheetsTheSameWay() = check(parseOds(fixture("table.ods")))

    private fun check(workbook: Workbook) {
        assertFalse(workbook.truncated)
        val sheet = workbook.sheets.first()
        val grid = (0..2).map { r -> (0..2).map { c -> sheet.row(r)?.cells?.firstOrNull { it.col == c }?.text } }
        assertEquals(
            listOf(listOf("Name", "Qty", "Price"), listOf("Apple", "3", "1.25"), listOf("Pear, green", "5", "0.8")),
            grid
        )
        assertTrue(sheet.row(1)?.cells?.first { it.col == 1 }?.numeric == true)
        assertFalse(sheet.row(1)?.cells?.first { it.col == 0 }?.numeric == true)
    }
}

// SPDX-FileCopyrightText: 2026 Debayan G <debz.exe@gmail.com>
// SPDX-License-Identifier: GPL-3.0-or-later

package org.prauga.documents.formats.sheet

/**
 * One sheet: its non-empty [rows] (sparse, in order), and the size of the area worth showing,
 * trimmed to the last row and column that hold something. Column widths are in "characters" of the
 * default font, as both formats store or can be converted to; hidden columns have width 0.
 */
class Sheet(
    val name: String,
    val rows: List<SheetRow>,
    val rowCount: Int,
    val colCount: Int,
    val colWidths: Map<Int, Float>,
    val defaultColWidth: Float,
    val hiddenRows: Set<Int>,
    val merges: List<CellRange>,
) {
    private val byIndex: Map<Int, SheetRow> = rows.associateBy { it.index }

    fun row(index: Int): SheetRow? = byIndex[index]

    fun colWidth(col: Int): Float = colWidths[col] ?: defaultColWidth
}

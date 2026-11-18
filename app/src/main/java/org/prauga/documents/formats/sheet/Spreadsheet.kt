// SPDX-FileCopyrightText: 2026 Debayan G <debz.exe@gmail.com>
// SPDX-License-Identifier: GPL-3.0-or-later

package org.prauga.documents.formats.sheet

/** Past this many cells a workbook stops loading: each cell is an object in memory, and a phone has
 * far less of it than the desktop the sheet was made on. */
const val MaxCells = 2_000_000

/** Excel's default column width, in characters, when a sheet doesn't say. */
internal const val DefaultColWidthChars = 8.43f

/** "A".."Z", "AA".. for zero-based [col]. */
fun columnName(col: Int): String {
    val name = StringBuilder()
    var n = col + 1
    while (n > 0) {
        val rem = (n - 1) % 26
        name.append('A' + rem)
        n = (n - 1) / 26
    }
    return name.reverse().toString()
}

/** Zero-based column of a cell reference like "AB12", or -1. */
internal fun columnOf(ref: String): Int {
    var col = 0
    var any = false
    for (ch in ref) {
        if (ch !in 'A'..'Z' && ch !in 'a'..'z') break
        col = col * 26 + (ch.uppercaseChar() - 'A' + 1)
        any = true
    }
    return if (any) col - 1 else -1
}

/** Zero-based row of a cell reference like "AB12", or -1. */
internal fun rowOf(ref: String): Int =
    ref.dropWhile { it.isLetter() || it == '$' }.filter { it.isDigit() }.toIntOrNull()?.minus(1) ?: -1

/** "A1:C3" as a range, or null. */
internal fun parseRange(ref: String): CellRange? {
    val parts = ref.replace("$", "").split(':')
    if (parts.size != 2) return null
    val r1 = rowOf(parts[0]); val c1 = columnOf(parts[0])
    val r2 = rowOf(parts[1]); val c2 = columnOf(parts[1])
    if (r1 < 0 || c1 < 0 || r2 < 0 || c2 < 0) return null
    return CellRange(minOf(r1, r2), minOf(c1, c2), maxOf(r1, r2), maxOf(c1, c2))
}

/** A [Sheet] from parsed rows, trimmed to the cells that hold text or a fill, and with merges and
 * hidden rows kept only inside that area. */
internal fun buildSheet(
    name: String,
    rows: List<SheetRow>,
    styles: List<CellStyle>,
    colWidths: Map<Int, Float>,
    defaultColWidth: Float,
    hiddenRows: Set<Int>,
    merges: List<CellRange>,
): Sheet {
    var lastRow = -1
    var lastCol = -1
    val kept = ArrayList<SheetRow>(rows.size)
    for (row in rows) {
        val cells = row.cells.filter { it.text.isNotEmpty() || styles.getOrNull(it.style)?.fill != null }
        if (cells.isEmpty()) continue
        kept.add(if (cells.size == row.cells.size) row else SheetRow(row.index, cells, row.heightPt))
        lastRow = maxOf(lastRow, row.index)
        lastCol = maxOf(lastCol, cells.last().col)
    }
    for (m in merges) {
        if (m.firstRow <= lastRow && m.firstCol <= lastCol) {
            lastCol = maxOf(lastCol, minOf(m.lastCol, lastCol + MergeOverhang))
        }
    }
    return Sheet(
        name = name,
        rows = kept,
        rowCount = lastRow + 1,
        colCount = lastCol + 1,
        colWidths = colWidths,
        defaultColWidth = defaultColWidth,
        hiddenRows = hiddenRows.filterTo(HashSet()) { it <= lastRow },
        merges = merges.filter { it.firstRow <= lastRow && it.firstCol <= lastCol },
    )
}

/** How far a merge starting inside the used area may widen it: a title merged across the page
 * should show in full, but not drag in a thousand empty columns. */
private const val MergeOverhang = 32

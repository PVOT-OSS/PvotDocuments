package org.prauga.documents.formats.richtext

data class DocTable(val rows: List<List<DocTableCell>>) : DocBlock {
    val columnCount: Int get() = rows.maxOfOrNull { row -> row.sumOf { it.colSpan } } ?: 0
}

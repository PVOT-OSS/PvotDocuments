package org.prauga.documents.formats.richtext

/** A table cell: its own blocks (paragraphs, nested tables), how many columns it spans, and its
 * fill. A cell covered by one above it ([covered]) is drawn as part of that one. */
data class DocTableCell(
    val blocks: List<DocBlock>,
    val colSpan: Int = 1,
    val fill: Int? = null,
    val covered: Boolean = false,
)

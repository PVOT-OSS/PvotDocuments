package org.prauga.documents.formats.slides

data class SlideTableCell(
    val text: SlideText,
    val fill: Int?,
    val colSpan: Int = 1,
    val rowSpan: Int = 1,
    val covered: Boolean = false,
)

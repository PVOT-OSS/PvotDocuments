package org.prauga.documents.formats.sheet

/** A cell's look, as the file set it. Colors are the document's own (ARGB), null where the file
 * leaves them to the default, so the viewer can use the theme's colors there instead. */
data class CellStyle(
    val bold: Boolean = false,
    val italic: Boolean = false,
    val underline: Boolean = false,
    val strike: Boolean = false,
    val textColor: Int? = null,
    val fill: Int? = null,
    val align: CellAlign = CellAlign.Auto,
    val wrap: Boolean = false,
) {
    companion object {
        val Default = CellStyle()
    }
}

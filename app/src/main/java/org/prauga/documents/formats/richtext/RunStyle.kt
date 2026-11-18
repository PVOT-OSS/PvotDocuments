package org.prauga.documents.formats.richtext

/** How a run of text looks. Colors are the document's own (ARGB), null where it leaves them to
 * the default. [sizePt] is the font size in points, null for the document's default. */
data class RunStyle(
    val bold: Boolean = false,
    val italic: Boolean = false,
    val underline: Boolean = false,
    val strike: Boolean = false,
    val color: Int? = null,
    val highlight: Int? = null,
    val sizePt: Float? = null,
    val position: TextPosition = TextPosition.Normal,
    val monospace: Boolean = false,
    val link: String? = null,
) {
    companion object {
        val Default = RunStyle()
    }
}

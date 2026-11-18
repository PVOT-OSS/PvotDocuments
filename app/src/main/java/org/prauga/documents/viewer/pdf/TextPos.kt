package org.prauga.documents.viewer.pdf

/** A character in the document: [char] is a Pdfium character index on page [page]. */
internal data class TextPos(val page: Int, val char: Int) : Comparable<TextPos> {
    override fun compareTo(other: TextPos): Int =
        if (page != other.page) page.compareTo(other.page) else char.compareTo(other.char)
}

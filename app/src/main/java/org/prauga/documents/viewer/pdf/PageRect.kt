package org.prauga.documents.viewer.pdf

/** A page box in page-relative fractions: 0..1 across the page's displayed width and height, top
 * to bottom. Independent of how big the page is drawn, so it survives resizing and zoom. */
internal data class PageRect(val left: Float, val top: Float, val right: Float, val bottom: Float) {
    fun contains(x: Float, y: Float): Boolean = x in left..right && y in top..bottom
}

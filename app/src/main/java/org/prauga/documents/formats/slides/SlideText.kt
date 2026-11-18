package org.prauga.documents.formats.slides

/** Text inside a shape: its paragraphs, where they sit vertically, and the shape's inner
 * margins in points. [fontScale] is PowerPoint's "shrink text on overflow" factor. */
data class SlideText(
    val paragraphs: List<SlideParagraph>,
    val anchor: SlideTextAnchor = SlideTextAnchor.Top,
    val insetLeft: Float = 7.2f,
    val insetTop: Float = 3.6f,
    val insetRight: Float = 7.2f,
    val insetBottom: Float = 3.6f,
    val fontScale: Float = 1f,
    val wrap: Boolean = true,
)

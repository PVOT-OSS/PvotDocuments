package org.prauga.documents.formats.slides

/** Something drawn on a slide, in drawing order. */
sealed interface SlideShape {
    val frame: SlideFrame

    /** A shape with an outline, a fill, and optionally text inside. */
    data class Shape(
        override val frame: SlideFrame,
        val geometry: SlideGeometry,
        val fill: SlideFill?,
        val line: SlideLine?,
        val text: SlideText?,
        val flipH: Boolean = false,
        val flipV: Boolean = false,
    ) : SlideShape

    /** A picture from the package, with the fractions cropped off each side. */
    data class Picture(
        override val frame: SlideFrame,
        val path: String,
        val cropLeft: Float = 0f,
        val cropTop: Float = 0f,
        val cropRight: Float = 0f,
        val cropBottom: Float = 0f,
        val description: String? = null,
    ) : SlideShape

    /** A table: column widths and row heights in points, and its cells row by row. */
    data class Table(
        override val frame: SlideFrame,
        val columnWidths: List<Float>,
        val rowHeights: List<Float>,
        val cells: List<List<SlideTableCell>>,
    ) : SlideShape
}

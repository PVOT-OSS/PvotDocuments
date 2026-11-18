package org.prauga.documents.formats.slides

/** One slide: what's behind everything, then its shapes, those from its master and layout
 * included, in drawing order. */
data class Slide(val background: SlideFill?, val shapes: List<SlideShape>)

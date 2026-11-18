package org.prauga.documents.formats.slides

/** A rectangle on a slide, in points from its top-left corner, with a clockwise rotation in
 * degrees about its centre. */
data class SlideFrame(val x: Float, val y: Float, val width: Float, val height: Float, val rotation: Float = 0f)

package org.prauga.documents.viewer.pdf

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.setValue

/** The whole document's zoom and pan — pinch scales the entire scrolling document in place, as a
 * PDF reader does, rather than one page inside its own frame. Offsets are in screen pixels. */
internal class PdfZoomState {
    var scale by mutableFloatStateOf(1f)
    var offsetX by mutableFloatStateOf(0f)
    var offsetY by mutableFloatStateOf(0f)
}

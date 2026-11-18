package org.prauga.documents.viewer.pdf

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.setValue

/** Zoom and pan of the whole scrolling document, not of one page in its frame. Offsets are in
 * screen pixels. */
internal class PdfZoomState {
    var scale by mutableFloatStateOf(1f)
    var offsetX by mutableFloatStateOf(0f)
    var offsetY by mutableFloatStateOf(0f)
}

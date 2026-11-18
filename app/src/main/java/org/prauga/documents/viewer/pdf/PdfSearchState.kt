package org.prauga.documents.viewer.pdf

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/** Shared between the viewer's bars, which open search and edit the query, and [PdfView], which
 * runs the search and scrolls to each match. */
@Stable
class PdfSearchState {
    /** Whether a document is loaded that can be searched; set by [PdfView]. */
    var available by mutableStateOf(false)
        internal set
    var active by mutableStateOf(false)
        private set
    var query by mutableStateOf("")
    var matchCount by mutableIntStateOf(0)
        internal set
    var currentMatch by mutableIntStateOf(0)
        internal set

    /** Height of [PdfSearchBar] from the bottom of the screen, keyboard included, so [PdfView]
     * scrolls matches clear of it. */
    var barHeightPx by mutableIntStateOf(0)
        internal set

    /** Moves to the match [delta] steps away; [PdfView] provides it. */
    internal var step: (delta: Int) -> Unit = {}

    fun open() {
        active = true
    }

    fun close() {
        active = false
        query = ""
    }
}

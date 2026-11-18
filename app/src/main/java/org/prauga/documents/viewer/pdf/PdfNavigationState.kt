// SPDX-FileCopyrightText: 2026 Debayan G <debz.exe@gmail.com>
// SPDX-License-Identifier: GPL-3.0-or-later

package org.prauga.documents.viewer.pdf

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/** Shared between the viewer's bars and [PdfView], which reports the page count, the page on
 * screen and the outline, and provides [goToPage]. */
@Stable
class PdfNavigationState {
    var pageCount by mutableIntStateOf(0)
        internal set

    /** Zero-based index of the page in the middle of the screen. */
    var currentPage by mutableIntStateOf(0)
        internal set

    var outline by mutableStateOf<List<OutlineEntry>>(emptyList())
        internal set

    /** Whether the page indicator is showing: it appears while the document moves, and fades
     * once it has been still for a moment. */
    var indicatorVisible by mutableStateOf(false)
        internal set

    var overviewOpen by mutableStateOf(false)

    internal var goTo: (pageIndex: Int) -> Unit = {}

    /** Scrolls to the top of page [pageIndex] (zero-based). */
    fun goToPage(pageIndex: Int) {
        if (pageIndex in 0 until pageCount) goTo(pageIndex)
    }
}

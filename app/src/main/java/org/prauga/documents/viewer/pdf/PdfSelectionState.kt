// SPDX-FileCopyrightText: 2026 Debayan G <debz.exe@gmail.com>
// SPDX-License-Identifier: GPL-3.0-or-later

package org.prauga.documents.viewer.pdf

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/** Shared between [PdfView], which owns the selection, and [PdfSelectionBar], which offers its
 * actions while something is selected. */
@Stable
class PdfSelectionState {
    var active by mutableStateOf(false)
        internal set

    /** Height of [PdfSelectionBar] from the bottom of the screen, so a selection drag knows
     * where the visible document ends and auto-scrolls before the finger reaches the bar. */
    var barHeightPx by mutableIntStateOf(0)
        internal set

    /** A message for the viewer to show, such as a copy cut short; cleared once shown. */
    var notice by mutableStateOf<String?>(null)

    internal var onCopy: () -> Unit = {}
    internal var onShare: () -> Unit = {}
    internal var onSearch: () -> Unit = {}
    internal var onSelectAll: () -> Unit = {}
}

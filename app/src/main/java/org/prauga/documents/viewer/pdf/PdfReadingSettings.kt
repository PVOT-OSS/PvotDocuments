// SPDX-FileCopyrightText: 2026 Debayan G <debz.exe@gmail.com>
// SPDX-License-Identifier: GPL-3.0-or-later

package org.prauga.documents.viewer.pdf

import androidx.compose.ui.graphics.ColorFilter
import org.prauga.documents.viewer.data.ReadingPosition

/** [pageFilter] is the dark-pages filter, null for normal pages. */
class PdfReadingSettings(
    val initialPosition: ReadingPosition?,
    val onPositionChange: (ReadingPosition) -> Unit,
    val pageFilter: ColorFilter?,
)

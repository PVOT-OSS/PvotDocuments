// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: GPL-3.0-or-later

package org.prauga.documents.viewer

import android.net.Uri
import org.prauga.documents.common.ViewerFormat
import org.prauga.documents.viewer.data.ReadingPosition

/** [initialPosition] is where reading of this document last stopped, if it has been read before. */
data class DocumentViewerUiState(
    val uri: Uri,
    val name: String,
    val mimeType: String,
    val format: ViewerFormat,
    val content: ViewerContentState,
    val darkPdfPages: Boolean,
    val initialPosition: ReadingPosition?,
)

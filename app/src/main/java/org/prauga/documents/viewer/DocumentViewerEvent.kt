// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: GPL-3.0-or-later

package org.prauga.documents.viewer

import org.prauga.documents.viewer.data.ReadingPosition

sealed interface DocumentViewerEvent {
    data object Retry : DocumentViewerEvent
    data class SubmitPassword(val password: String) : DocumentViewerEvent
    data class DarkPdfPages(val enabled: Boolean) : DocumentViewerEvent
    data class ReadingPositionChanged(val position: ReadingPosition) : DocumentViewerEvent
}

package org.prauga.documents.viewer

sealed interface DocumentViewerEvent {
    data class DarkPdfPages(val enabled: Boolean) : DocumentViewerEvent
}

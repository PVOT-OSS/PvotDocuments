package org.prauga.documents.viewer

import org.prauga.documents.common.ViewerFormat
import org.prauga.documents.viewer.data.ReadingPosition

/** [initialPosition] is where reading of this document last stopped, if it has been read before. */
data class DocumentViewerUiState(
    val name: String,
    val format: ViewerFormat,
    val content: ViewerContentState,
    val darkPdfPages: Boolean,
    val initialPosition: ReadingPosition?,
)

package org.prauga.documents.library.data

import org.prauga.documents.common.ViewerTarget

sealed interface OpenResult {
    data class Opened(val target: ViewerTarget) : OpenResult
    data object Unsupported : OpenResult
    data object Unreadable : OpenResult
}

package org.prauga.documents.common

import androidx.annotation.StringRes

sealed interface DocumentEffect {
    data class OpenViewer(val target: ViewerTarget) : DocumentEffect
    data class Share(val documents: List<SharedDocument>) : DocumentEffect
    data class Message(@StringRes val text: Int) : DocumentEffect
}

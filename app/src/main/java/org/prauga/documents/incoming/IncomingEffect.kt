package org.prauga.documents.incoming

import org.prauga.documents.common.ViewerTarget

sealed interface IncomingEffect {
    data class OpenViewer(val target: ViewerTarget) : IncomingEffect
    data object Handled : IncomingEffect
}

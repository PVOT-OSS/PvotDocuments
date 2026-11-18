package org.prauga.documents.viewer

import org.prauga.documents.viewer.data.ViewerContent

sealed interface ViewerContentState {
    data object Loading : ViewerContentState

    /** An encrypted PDF waiting for its password; [wrongPassword] once one has been tried. */
    data class NeedsPassword(val wrongPassword: Boolean, val attempt: Int) : ViewerContentState

    data object Failed : ViewerContentState

    data class Ready(val content: ViewerContent) : ViewerContentState
}

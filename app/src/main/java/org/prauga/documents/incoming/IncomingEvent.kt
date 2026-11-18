package org.prauga.documents.incoming

import android.net.Uri

sealed interface IncomingEvent {
    /** [fallbackName] is the title to use when neither the provider nor [uri] gives a name. */
    data class Handle(val uri: Uri, val fallbackName: String) : IncomingEvent
    data class ChooseAutosave(val enabled: Boolean) : IncomingEvent
}

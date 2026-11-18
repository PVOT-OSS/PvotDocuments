package org.prauga.documents.recent

import org.prauga.documents.common.DocumentKind
import org.prauga.documents.library.data.DocumentEntry

sealed interface RecentEvent {
    data class Open(val document: DocumentEntry) : RecentEvent
    data class Share(val document: DocumentEntry) : RecentEvent
    data class Filter(val kind: DocumentKind?) : RecentEvent
}

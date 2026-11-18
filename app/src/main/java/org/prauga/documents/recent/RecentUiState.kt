package org.prauga.documents.recent

import org.prauga.documents.common.DocumentKind
import org.prauga.documents.library.data.DocumentEntry

/** [kinds] are only the kinds present in [documents], so a filter never comes up empty. */
data class RecentUiState(
    val documents: List<DocumentEntry>,
    val kinds: List<DocumentKind>,
    val selectedKind: DocumentKind?,
    val visibleDocuments: List<DocumentEntry>,
)

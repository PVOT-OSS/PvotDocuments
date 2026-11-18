package org.prauga.documents.manage

import android.net.Uri
import org.prauga.documents.common.DocumentSort
import org.prauga.documents.library.data.FolderEntry
import org.prauga.documents.library.data.ManagedDocument
import org.prauga.documents.library.data.TrashedDocument

/** [visibleDocuments] are [documents] matching [searchQuery], in [sort] order. */
data class ManageUiState(
    val folders: List<FolderEntry>,
    val documents: List<ManagedDocument>,
    val visibleDocuments: List<ManagedDocument>,
    val trashedDocuments: List<TrashedDocument>,
    val gridView: Boolean,
    val searchQuery: String,
    val sort: DocumentSort,
    val selectedUris: Set<Uri>,
)

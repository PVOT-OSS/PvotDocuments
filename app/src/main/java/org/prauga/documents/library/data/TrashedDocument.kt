package org.prauga.documents.library.data

import android.net.Uri

/** A document moved to Recently deleted: its own copy in Documents' trash storage, plus where to
 * put it back. Restoring is only possible while [folderTreeUri] is still a folder the user has
 * added; see [org.prauga.documents.library.data.DocumentsRepository.restoreFromTrash]. */
data class TrashedDocument(
    val document: DocumentEntry,
    val folderTreeUri: Uri,
    val deletedAt: Long
)

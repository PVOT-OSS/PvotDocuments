package org.prauga.documents.library.data

import android.net.Uri

/** A document paired with the folder it was found in, so deleting or restoring it knows where to
 * write. */
data class ManagedDocument(
    val document: DocumentEntry,
    val folderTreeUri: Uri
)

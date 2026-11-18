package org.prauga.documents.library.data

import android.net.Uri

data class ManagedDocument(
    val document: DocumentEntry,
    val folderTreeUri: Uri
)

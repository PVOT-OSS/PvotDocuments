package org.prauga.documents.library.data

import android.net.Uri

/** A folder the user added through the system picker. */
data class FolderEntry(
    val treeUri: Uri,
    val displayName: String
)

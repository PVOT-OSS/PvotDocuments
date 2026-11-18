package org.prauga.documents.library.data

/**
 * [folderUris] are the persisted tree URIs of the folders the user added. [recentDocuments] are
 * the documents opened from anywhere, most recent first; an autosaved one points at the app's own
 * copy. [trashedDocuments] are in Recently deleted, most recent first.
 */
data class Library(
    val folderUris: Set<String>,
    val recentDocuments: List<DocumentEntry>,
    val trashedDocuments: List<TrashedDocument>,
)

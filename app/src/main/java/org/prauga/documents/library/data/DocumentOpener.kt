package org.prauga.documents.library.data

import org.prauga.documents.common.ViewerTarget
import org.prauga.documents.common.viewerFormat
import javax.inject.Inject

/** Records the document at the top of recents unless it has no viewer or can't be read. */
class DocumentOpener @Inject constructor(
    private val documentsRepository: DocumentsRepository,
    private val libraryRepository: LibraryRepository,
) {
    suspend fun open(document: DocumentEntry): OpenResult {
        val format = viewerFormat(document.name, document.mimeType) ?: return OpenResult.Unsupported
        val uri = documentsRepository.readableUri(document) ?: return OpenResult.Unreadable
        libraryRepository.addRecentDocument(document)
        return OpenResult.Opened(ViewerTarget(uri, document.name, format))
    }
}

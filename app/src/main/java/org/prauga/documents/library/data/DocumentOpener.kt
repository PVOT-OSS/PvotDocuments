package org.prauga.documents.library.data

import org.prauga.documents.common.ViewerTarget
import org.prauga.documents.common.viewerFormat
import javax.inject.Inject

/**
 * Gets a [DocumentEntry] ready for the viewer and records it at the top of the recent documents,
 * wherever it was opened from. A format without a viewer, or a document the app can't read, is
 * not recorded.
 */
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

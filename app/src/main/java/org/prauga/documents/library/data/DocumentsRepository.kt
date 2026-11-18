package org.prauga.documents.library.data

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import androidx.core.content.FileProvider
import androidx.documentfile.provider.DocumentFile
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import org.prauga.documents.BuildConfig
import org.prauga.documents.common.coroutines.IoDispatcher
import timber.log.Timber
import java.io.File
import java.io.InputStream
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Reads folders and documents through the Storage Access Framework. Every call is a live query
 * against the provider, not a cache: a tree the user already granted still needs its own read on
 * every call, which is why each one is a suspend function off the main thread.
 */
@Singleton
class DocumentsRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    @IoDispatcher private val io: CoroutineDispatcher,
) {

    /** [treeUri] as a [FolderEntry], or null if it can no longer be opened (such as a removed SD
     * card or a folder the user revoked access to outside the app). */
    suspend fun folder(treeUri: Uri): FolderEntry? = withContext(io) {
        val tree = DocumentFile.fromTreeUri(context, treeUri) ?: return@withContext null
        if (!tree.exists()) return@withContext null
        FolderEntry(treeUri = treeUri, displayName = tree.name ?: treeUri.lastPathSegment.orEmpty())
    }

    /** The files directly inside [treeUri], not its subfolders; phase 1 is one level deep. */
    suspend fun documentsIn(treeUri: Uri): List<DocumentEntry> = withContext(io) {
        val tree = DocumentFile.fromTreeUri(context, treeUri) ?: return@withContext emptyList()
        tree.listFiles()
            .filter { it.isFile }
            .map {
                DocumentEntry(
                    uri = it.uri,
                    name = it.name ?: it.uri.lastPathSegment.orEmpty(),
                    mimeType = it.type ?: "application/octet-stream",
                    sizeBytes = it.length(),
                    lastModified = it.lastModified()
                )
            }
    }

    /**
     * [uri]'s content, decoded as UTF-8 text. Throws if the document can't be read or isn't
     * actually text — the viewer turns that into [com.prauga.pvot.coreui.UiState.Error].
     */
    suspend fun readText(uri: Uri): String = withContext(io) {
        val stream = context.contentResolver.openInputStream(uri)
            ?: error("This document can't be opened right now.")
        stream.use { it.readBytes().toString(Charsets.UTF_8) }
    }

    /**
     * [uri] as a single document (not a tree), such as one received through "Open with" or
     * "Share". Null if it no longer resolves — a one-time grant from the sending app that has
     * since expired, for example.
     */
    suspend fun documentInfo(uri: Uri): DocumentEntry? = withContext(io) {
        val doc = runCatching { DocumentFile.fromSingleUri(context, uri) }.onFailure { Timber.w("Could not resolve a document: %s", it.javaClass.name) }.getOrNull()
            ?: return@withContext null
        if (!doc.exists()) return@withContext null
        DocumentEntry(
            uri = uri,
            name = doc.name ?: uri.lastPathSegment.orEmpty(),
            mimeType = doc.type ?: "application/octet-stream",
            sizeBytes = doc.length(),
            lastModified = doc.lastModified()
        )
    }

    /** Whether [uri]'s content can actually be opened for reading right now. */
    suspend fun canRead(uri: Uri): Boolean = withContext(io) {
        runCatching { context.contentResolver.openInputStream(uri)?.use { true } ?: false }
            .onFailure { Timber.w("Could not open a document: %s", it.javaClass.name) }
            .getOrDefault(false)
    }

    /** [uri]'s MIME type as its provider reports it, or an empty string when it doesn't. */
    suspend fun mimeType(uri: Uri): String = withContext(io) {
        context.contentResolver.getType(uri).orEmpty()
    }

    /**
     * [document]'s URI, safe to hand to another component (an intent, a viewer). A document from
     * an added folder is already a `content://` URI the app holds a grant on; an autosaved copy is
     * a plain `file://` one, which isn't safe to put in an Intent — [FileProvider] turns it into a
     * `content://` URI scoped to just that file instead. Null for a `file://` URI outside the
     * app's own storage, which it can neither read nor share without a storage permission.
     */
    suspend fun readableUri(document: DocumentEntry): Uri? = withContext(io) {
        if (document.uri.scheme != "file") return@withContext document.uri
        val file = File(document.uri.path ?: return@withContext null)
        if (!isOwnFile(context, file)) return@withContext null
        runCatching {
            FileProvider.getUriForFile(context, "${BuildConfig.APPLICATION_ID}.fileprovider", file)
        }.onFailure { Timber.w("Could not share an own copy: %s", it.javaClass.name) }.getOrNull()
    }

    /**
     * Copies [uri] into the app's own storage, under `filesDir/opened/`, so it survives after a
     * transient grant from the sending app expires. Returns a [DocumentEntry] pointing at the
     * copy, named [displayName].
     *
     * If an earlier copy of the same document is already there, that copy is returned instead
     * of writing another one, so repeated "Open with" of one file keeps one copy and one URI.
     * "Same document" means same file name, same size and same SHA-256 of the content: name and
     * size are a free first filter, and the hash is only computed when they already match, so a
     * different file that happens to share both still gets its own copy. When several old copies
     * match (duplicates written by earlier builds), one in [preferredUris] (what Recent already
     * points at) wins, then the newest; the others are left as they are.
     */
    suspend fun copyToAppStorage(
        uri: Uri,
        displayName: String,
        preferredUris: Collection<Uri> = emptyList()
    ): DocumentEntry? =
        existingOpenedCopy(uri, displayName, preferredUris)
            ?: copyIntoOwnStorage(uri, displayName, OpenedDirName)

    /** An existing copy under `filesDir/opened/` with [uri]'s name, size and content, if any.
     * Reads [uri] once, and only when a copy with the same name and size exists. */
    private suspend fun existingOpenedCopy(
        uri: Uri,
        displayName: String,
        preferredUris: Collection<Uri>
    ): DocumentEntry? = withContext(io) {
        val safeName = safeFileName(displayName)
        val size = runCatching { DocumentFile.fromSingleUri(context, uri)?.length() }
            .getOrNull() ?: 0L
        // Providers that don't report a size give 0; then every same-named copy is a candidate
        // and the hash decides.
        val candidates = File(context.filesDir, OpenedDirName)
            .listFiles { dir -> dir.isDirectory }
            .orEmpty()
            .map { File(it, safeName) }
            .filter { it.isFile && (size <= 0L || it.length() == size) }
        if (candidates.isEmpty()) return@withContext null

        val incomingHash = runCatching {
            context.contentResolver.openInputStream(uri)?.use { sha256(it) }
        }.onFailure { Timber.w("Could not hash an incoming document: %s", it.javaClass.name) }.getOrNull() ?: return@withContext null
        val preferredPaths = preferredUris.filter { it.scheme == "file" }.mapNotNull { it.path }.toSet()
        val match = candidates
            .sortedWith(
                compareByDescending<File> { it.path in preferredPaths }
                    .thenByDescending { it.parentFile?.name?.toLongOrNull() ?: 0L }
            )
            .firstOrNull { candidate ->
                runCatching { candidate.inputStream().use { sha256(it) } }.getOrNull()
                    ?.contentEquals(incomingHash) == true
            } ?: return@withContext null

        // A reuse stands in for a fresh copy, which would have been dated now.
        match.setLastModified(System.currentTimeMillis())
        DocumentEntry(
            uri = Uri.fromFile(match),
            name = displayName,
            mimeType = context.contentResolver.getType(uri) ?: "application/octet-stream",
            sizeBytes = match.length(),
            lastModified = match.lastModified()
        )
    }

    private fun sha256(input: InputStream): ByteArray {
        val digest = MessageDigest.getInstance("SHA-256")
        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
        while (true) {
            val read = input.read(buffer)
            if (read < 0) break
            digest.update(buffer, 0, read)
        }
        return digest.digest()
    }

    /**
     * Moves [document] out of its folder and into `filesDir/trash/`: copies it there, then
     * deletes the original. Null if either step fails, in which case nothing changed — a partial
     * copy is removed, and a copy whose original couldn't be deleted is rolled back rather than
     * left as a duplicate.
     */
    suspend fun moveToTrash(document: DocumentEntry): DocumentEntry? {
        val copy = copyIntoOwnStorage(document.uri, document.name, TrashDirName) ?: return null
        val originalDeleted = withContext(io) {
            runCatching { DocumentFile.fromSingleUri(context, document.uri)?.delete() }
                .onFailure { Timber.w("Could not delete a document: %s", it.javaClass.name) }
                .getOrDefault(false) == true
        }
        if (!originalDeleted) {
            withContext(io) { deleteOwnCopy(copy.uri) }
            return null
        }
        return copy
    }

    /**
     * Writes [trashed]'s copy back into [folderTreeUri] as a new document, then removes it from
     * trash. False if the folder no longer resolves (removed, or access revoked outside the app)
     * or the write fails; the trash copy is left in place either way, so nothing is lost.
     */
    suspend fun restoreFromTrash(trashed: DocumentEntry, folderTreeUri: Uri): Boolean =
        withContext(io) {
            val tree = DocumentFile.fromTreeUri(context, folderTreeUri) ?: return@withContext false
            val source = File(requireNotNull(trashed.uri.path))
            if (!source.exists()) return@withContext false
            val restored = tree.createFile(trashed.mimeType, trashed.name)
            val written = restored?.let {
                runCatching {
                    context.contentResolver.openOutputStream(it.uri)?.use { output ->
                        source.inputStream().use { input -> input.copyTo(output) }
                    } != null
                }.onFailure { Timber.w("Could not restore a document: %s", it.javaClass.name) }.getOrDefault(false)
            } ?: false
            if (written) {
                deleteOwnCopy(trashed.uri)
            } else {
                restored?.delete()
            }
            written
        }

    /** Deletes [trashed]'s copy for good. */
    suspend fun deleteForever(trashed: DocumentEntry) =
        withContext(io) { deleteOwnCopy(trashed.uri) }

    /**
     * Renames [document] to [newName]. Some providers change a document's URI on rename rather
     * than keeping it, so this reports success only — the caller re-lists its folder afterward
     * instead of trying to track the new URI itself.
     */
    suspend fun renameDocument(document: DocumentEntry, newName: String): Boolean =
        withContext(io) {
            runCatching {
                DocumentsContract.renameDocument(
                    context.contentResolver,
                    document.uri,
                    newName
                ) != null
            }.onFailure { Timber.w("Could not rename a document: %s", it.javaClass.name) }.getOrDefault(false)
        }

    /**
     * Creates a new, empty document named [name] (with [extension] appended if [name] doesn't
     * already end with it) directly inside [folderTreeUri]. Only plain-text formats — what Phase 1
     * can honestly create without a real DOCX/XLSX writer, which is Phase 3/4 work.
     */
    suspend fun createTextDocument(
        folderTreeUri: Uri,
        name: String,
        extension: String,
        mimeType: String
    ): DocumentEntry? = withContext(io) {
        val tree = DocumentFile.fromTreeUri(context, folderTreeUri) ?: return@withContext null
        val fileName = if (name.endsWith(".$extension", ignoreCase = true)) {
            name
        } else {
            "$name.$extension"
        }
        val created = tree.createFile(mimeType, fileName) ?: return@withContext null
        val written = runCatching {
            context.contentResolver.openOutputStream(created.uri)?.use { it.flush() } != null
        }.onFailure { Timber.w("Could not write a new document: %s", it.javaClass.name) }.getOrDefault(false)
        if (!written) {
            created.delete()
            return@withContext null
        }
        DocumentEntry(
            uri = created.uri,
            name = created.name ?: fileName,
            mimeType = mimeType,
            sizeBytes = created.length(),
            lastModified = created.lastModified()
        )
    }

    /** Copies [uri] into its own timestamped subfolder under `filesDir/$baseDirName/`, so the file
     * on disk keeps [displayName] exactly rather than a deduplicating prefix — the name shared
     * back out through `FileProvider` reads from the file, not from [DocumentEntry.name]. */
    private suspend fun copyIntoOwnStorage(
        uri: Uri,
        displayName: String,
        baseDirName: String
    ): DocumentEntry? = withContext(io) {
        val safeName = safeFileName(displayName)
        val copyDir = File(
            File(context.filesDir, baseDirName),
            System.currentTimeMillis().toString()
        ).apply { mkdirs() }
        val target = File(copyDir, safeName)
        val copied = runCatching {
            context.contentResolver.openInputStream(uri)?.use { input ->
                target.outputStream().use { output -> input.copyTo(output) }
            } != null
        }.onFailure { Timber.w("Could not copy a document: %s", it.javaClass.name) }.getOrDefault(false)
        if (!copied) return@withContext null
        DocumentEntry(
            uri = Uri.fromFile(target),
            name = displayName,
            mimeType = context.contentResolver.getType(uri) ?: "application/octet-stream",
            sizeBytes = target.length(),
            lastModified = target.lastModified()
        )
    }

    private fun safeFileName(displayName: String) = displayName.ifBlank { "document" }

    /** Deletes a `file://` URI from [copyIntoOwnStorage], and its now-empty subfolder. */
    private fun deleteOwnCopy(uri: Uri) {
        val path = uri.path ?: return
        val file = File(path)
        file.delete()
        file.parentFile?.delete()
    }

    private companion object {
        const val OpenedDirName = "opened"
        const val TrashDirName = "trash"
    }
}

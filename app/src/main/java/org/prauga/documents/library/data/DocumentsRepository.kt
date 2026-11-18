// SPDX-FileCopyrightText: 2026 Debayan G <debz.exe@gmail.com>
// SPDX-License-Identifier: GPL-3.0-or-later

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
import java.io.FileNotFoundException
import java.io.InputStream
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton

/** Every call is a live query against the SAF provider, not a cache, run off the main thread. */
@Singleton
class DocumentsRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    @IoDispatcher private val io: CoroutineDispatcher,
) {

    /** Null if the tree can't be opened anymore (a removed SD card, access revoked elsewhere). */
    suspend fun folder(treeUri: Uri): FolderEntry? = withContext(io) {
        val tree = DocumentFile.fromTreeUri(context, treeUri) ?: return@withContext null
        if (!tree.exists()) return@withContext null
        FolderEntry(treeUri = treeUri, displayName = tree.name ?: treeUri.lastPathSegment.orEmpty())
    }

    /** The files directly inside [treeUri], not its subfolders. */
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

    /** Decodes as UTF-8. Throws if the document can't be read. */
    suspend fun readText(uri: Uri): String = withContext(io) {
        val stream = context.contentResolver.openInputStream(uri) ?: throw FileNotFoundException()
        stream.use { it.readBytes().toString(Charsets.UTF_8) }
    }

    /** Null if [uri] no longer resolves, such as an expired one-time grant from the sending app. */
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

    suspend fun canRead(uri: Uri): Boolean = withContext(io) {
        runCatching { context.contentResolver.openInputStream(uri)?.use { true } ?: false }
            .onFailure { Timber.w("Could not open a document: %s", it.javaClass.name) }
            .getOrDefault(false)
    }

    suspend fun mimeType(uri: Uri): String = withContext(io) {
        context.contentResolver.getType(uri).orEmpty()
    }

    /**
     * A URI safe to put in an Intent: a `file://` own copy goes through [FileProvider] as a
     * `content://` URI scoped to that file. Null for a `file://` URI outside the app's own storage,
     * which it can neither read nor share without a storage permission.
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
     * Copies [uri] under `filesDir/opened/` so it survives after a transient grant expires.
     *
     * An earlier copy with the same name, size and SHA-256 is reused instead, so repeated
     * "Open with" of one file keeps one copy and one URI; the hash is only computed when name and
     * size already match. When several old copies match (duplicates written by earlier builds),
     * one in [preferredUris] wins, then the newest; the others are left as they are.
     */
    suspend fun copyToAppStorage(
        uri: Uri,
        displayName: String,
        preferredUris: Collection<Uri> = emptyList()
    ): DocumentEntry? =
        existingOpenedCopy(uri, displayName, preferredUris)
            ?: copyIntoOwnStorage(uri, displayName, OpenedDirName)

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
     * Copies [document] into `filesDir/trash/`, then deletes the original. Null if either step
     * fails; a copy whose original couldn't be deleted is removed rather than left as a duplicate.
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

    /** Writes [trashed] back into [folderTreeUri]; on any failure the trash copy is kept. */
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

    suspend fun deleteForever(trashed: DocumentEntry) =
        withContext(io) { deleteOwnCopy(trashed.uri) }

    /**
     * Some providers change a document's URI on rename, so this reports success only and the
     * caller re-lists the folder instead of tracking the new URI.
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

    /** Creates an empty file; only plain-text formats, since there is no DOCX/XLSX writer. */
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

    /** A timestamped subfolder per copy keeps [displayName] exact on disk, since `FileProvider`
     * shares the file's own name, not [DocumentEntry.name]. */
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
        if (!copied) {
            deleteOwnCopy(Uri.fromFile(target))
            return@withContext null
        }
        DocumentEntry(
            uri = Uri.fromFile(target),
            name = displayName,
            mimeType = context.contentResolver.getType(uri) ?: "application/octet-stream",
            sizeBytes = target.length(),
            lastModified = target.lastModified()
        )
    }

    // The name comes from another app's provider; it must not be able to step out of the copy's
    // own folder into the rest of the app's storage.
    private fun safeFileName(displayName: String): String {
        val name = displayName.substringAfterLast('/').substringAfterLast('\\')
        return if (name.isBlank() || name == "." || name == "..") "document" else name
    }

    /** Also deletes the now-empty subfolder. */
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

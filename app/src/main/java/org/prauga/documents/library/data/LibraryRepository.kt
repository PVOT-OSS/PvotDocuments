// SPDX-FileCopyrightText: 2026 Debayan G <debz.exe@gmail.com>
// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: GPL-3.0-or-later

package org.prauga.documents.library.data

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import androidx.core.content.edit
import androidx.core.net.toUri
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import org.prauga.documents.common.coroutines.ApplicationScope
import org.prauga.documents.common.coroutines.IoDispatcher
import org.prauga.documents.common.preferences.DocumentsPreferences
import timber.log.Timber
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/** [library] is null until the preferences file has been read. */
@Singleton
class LibraryRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val preferences: DocumentsPreferences,
    @ApplicationScope private val scope: CoroutineScope,
    @IoDispatcher private val io: CoroutineDispatcher,
) {
    private val state = MutableStateFlow<Library?>(null)
    val library: StateFlow<Library?> = state.asStateFlow()
    private val writes = Mutex()

    init {
        scope.launch { writes.withLock { state.value = read(preferences.get()) } }
    }

    /** Stores [treeUri] only if a persisted permission on it could be taken. */
    fun addFolder(treeUri: Uri) = write { stored, current ->
        val granted = withContext(io) {
            runCatching {
                context.contentResolver.takePersistableUriPermission(treeUri, FolderPermissionFlags)
            }.onFailure { Timber.w("Could not keep a folder permission: %s", it.javaClass.name) }.isSuccess
        }
        if (granted) stored.edit { putStringSet(KeyFolders, current.folderUris + treeUri.toString()) }
    }

    fun removeFolder(treeUri: Uri) = write { stored, current ->
        withContext(io) {
            runCatching {
                context.contentResolver.releasePersistableUriPermission(treeUri, FolderPermissionFlags)
            }.onFailure { Timber.w("Could not release a folder permission: %s", it.javaClass.name) }
        }
        stored.edit { putStringSet(KeyFolders, current.folderUris - treeUri.toString()) }
    }

    /**
     * Replaces any entry for the same document: same URI (an autosaved copy is reused, see
     * [DocumentsRepository.copyToAppStorage]), or same name and size, for one reached through a
     * different URI or across an autosave setting change. Two of the app's own copies never match
     * on name and size, since they only differ when their content does.
     */
    fun addRecentDocument(entry: DocumentEntry) = write { stored, current ->
        val superseded = current.recentDocuments.filter {
            it.uri == entry.uri || (
                it.name == entry.name &&
                    it.sizeBytes == entry.sizeBytes &&
                    !(it.uri.scheme == "file" && entry.uri.scheme == "file")
                )
        }.toSet()
        val updated = (listOf(entry) + (current.recentDocuments - superseded)).take(MaxRecentDocuments)
        stored.edit { putString(KeyRecent, encodeDocuments(updated)) }
    }

    fun removeRecentDocument(entry: DocumentEntry) = write { stored, current ->
        val updated = current.recentDocuments.filterNot { it.uri == entry.uri }
        stored.edit { putString(KeyRecent, encodeDocuments(updated)) }
    }

    fun addTrashedDocument(entry: TrashedDocument) = write { stored, current ->
        stored.edit { putString(KeyTrash, encodeTrashed(listOf(entry) + current.trashedDocuments)) }
    }

    fun removeTrashedDocument(entry: TrashedDocument) = write { stored, current ->
        stored.edit { putString(KeyTrash, encodeTrashed(current.trashedDocuments - entry)) }
    }

    private fun write(change: suspend (SharedPreferences, Library) -> Unit) {
        scope.launch {
            writes.withLock {
                val stored = preferences.get()
                change(stored, state.value ?: read(stored))
                state.value = read(stored)
            }
        }
    }

    private suspend fun read(stored: SharedPreferences): Library = withContext(io) {
        Library(
            folderUris = stored.getStringSet(KeyFolders, emptySet()).orEmpty(),
            // A file:// entry outside the app's own storage could never be read; older builds
            // recorded some from other apps' "Open with".
            recentDocuments = decodeDocuments(stored.getString(KeyRecent, null)).filter {
                it.uri.scheme != "file" || isOwnFile(context, File(it.uri.path.orEmpty()))
            },
            trashedDocuments = decodeTrashed(stored.getString(KeyTrash, null)),
        )
    }

    private companion object {
        const val KeyFolders = "folder_uris"
        const val KeyRecent = "recent_documents"
        const val KeyTrash = "trashed_documents"
        const val MaxRecentDocuments = 50
        const val FolderPermissionFlags =
            Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
    }
}

private fun JSONObject.putDocumentEntry(entry: DocumentEntry): JSONObject = apply {
    put("uri", entry.uri.toString())
    put("name", entry.name)
    put("mimeType", entry.mimeType)
    put("sizeBytes", entry.sizeBytes)
    put("lastModified", entry.lastModified)
}

private fun JSONObject.getDocumentEntry(): DocumentEntry = DocumentEntry(
    uri = getString("uri").toUri(),
    name = getString("name"),
    mimeType = getString("mimeType"),
    sizeBytes = getLong("sizeBytes"),
    lastModified = getLong("lastModified"),
)

private fun encodeDocuments(entries: List<DocumentEntry>): String {
    val array = JSONArray()
    entries.forEach { array.put(JSONObject().putDocumentEntry(it)) }
    return array.toString()
}

private fun decodeDocuments(json: String?): List<DocumentEntry> {
    if (json.isNullOrBlank()) return emptyList()
    return runCatching {
        val array = JSONArray(json)
        List(array.length()) { array.getJSONObject(it).getDocumentEntry() }
    }.onFailure { Timber.w("Dropping unreadable recent documents: %s", it.javaClass.name) }
        .getOrDefault(emptyList())
}

private fun encodeTrashed(entries: List<TrashedDocument>): String {
    val array = JSONArray()
    entries.forEach { entry ->
        array.put(
            JSONObject().putDocumentEntry(entry.document).apply {
                put("folderTreeUri", entry.folderTreeUri.toString())
                put("deletedAt", entry.deletedAt)
            }
        )
    }
    return array.toString()
}

private fun decodeTrashed(json: String?): List<TrashedDocument> {
    if (json.isNullOrBlank()) return emptyList()
    return runCatching {
        val array = JSONArray(json)
        List(array.length()) { index ->
            val item = array.getJSONObject(index)
            TrashedDocument(
                document = item.getDocumentEntry(),
                folderTreeUri = item.getString("folderTreeUri").toUri(),
                deletedAt = item.getLong("deletedAt"),
            )
        }
    }.onFailure { Timber.w("Dropping unreadable trash entries: %s", it.javaClass.name) }
        .getOrDefault(emptyList())
}

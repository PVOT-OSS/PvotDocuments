package org.prauga.documents.manage

import android.net.Uri
import androidx.core.net.toUri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.prauga.pvot.coreui.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.prauga.documents.R
import org.prauga.documents.common.DocumentEffect
import org.prauga.documents.common.DocumentSort
import org.prauga.documents.common.SharedDocument
import org.prauga.documents.common.WhileScreenSubscribed
import org.prauga.documents.common.sortedByDocument
import org.prauga.documents.library.data.DocumentOpener
import org.prauga.documents.library.data.DocumentsRepository
import org.prauga.documents.library.data.FolderEntry
import org.prauga.documents.library.data.LibraryRepository
import org.prauga.documents.library.data.ManagedDocument
import org.prauga.documents.library.data.OpenResult
import org.prauga.documents.library.data.TrashedDocument
import org.prauga.documents.settings.data.SettingsRepository
import javax.inject.Inject

@HiltViewModel
class ManageViewModel @Inject constructor(
    private val documentsRepository: DocumentsRepository,
    private val libraryRepository: LibraryRepository,
    settingsRepository: SettingsRepository,
    private val documentOpener: DocumentOpener,
) : ViewModel() {

    private val refreshes = MutableStateFlow(0)
    private val query = MutableStateFlow("")
    private val sort = MutableStateFlow(DocumentSort.Name)
    private val selection = MutableStateFlow<Set<Uri>>(emptySet())
    private val effectChannel = Channel<DocumentEffect>(Channel.BUFFERED)
    val effects: Flow<DocumentEffect> = effectChannel.receiveAsFlow()

    // Re-reads every added folder whenever the set changes or after a change to their contents;
    // each folder is a handful of files, so re-listing is simpler than patching the list.
    private val listing: Flow<Listing?> =
        combine(libraryRepository.library.map { it?.folderUris }.distinctUntilChanged(), refreshes) { uris, _ -> uris }
            .map { uris -> uris?.let { list(it) } }

    private val view = combine(query, sort, selection, ::View)

    val state: StateFlow<UiState<ManageUiState>> =
        combine(listing, libraryRepository.library, settingsRepository.settings, view) { listing, library, settings, view ->
            if (listing == null || library == null || settings == null) return@combine UiState.Loading
            UiState.Success(
                ManageUiState(
                    folders = listing.folders,
                    documents = listing.documents,
                    visibleDocuments = listing.documents
                        .filter { it.document.name.contains(view.query, ignoreCase = true) }
                        .sortedByDocument(view.sort) { it.document },
                    trashedDocuments = library.trashedDocuments,
                    gridView = settings.gridView,
                    searchQuery = view.query,
                    sort = view.sort,
                    selectedUris = view.selection,
                )
            )
        }.stateIn(viewModelScope, WhileScreenSubscribed, UiState.Loading)

    fun onEvent(event: ManageEvent) {
        when (event) {
            is ManageEvent.AddFolder -> libraryRepository.addFolder(event.treeUri)
            is ManageEvent.RemoveFolder -> libraryRepository.removeFolder(event.treeUri)
            is ManageEvent.Open -> open(event.document)
            is ManageEvent.Share -> share(listOf(event.document))
            ManageEvent.ShareSelected -> share(selected())
            is ManageEvent.Delete -> delete(listOf(event.document))
            ManageEvent.DeleteSelected -> delete(selected())
            is ManageEvent.Rename -> rename(event.document, event.newName)
            is ManageEvent.Create -> create(event.type, event.name)
            is ManageEvent.Search -> query.value = event.query
            is ManageEvent.Sort -> sort.value = event.sort
            is ManageEvent.ToggleSelection -> selection.update { if (event.uri in it) it - event.uri else it + event.uri }
            is ManageEvent.SelectOnly -> selection.value = setOf(event.uri)
            ManageEvent.SelectAll -> selection.value = visible().map { it.document.uri }.toSet()
            ManageEvent.ClearSelection -> selection.value = emptySet()
            is ManageEvent.Restore -> restore(event.document)
            is ManageEvent.DeleteForever -> deleteForever(event.document)
        }
    }

    private fun visible(): List<ManagedDocument> =
        (state.value as? UiState.Success)?.data?.visibleDocuments.orEmpty()

    private fun selected(): List<ManagedDocument> = visible().filter { it.document.uri in selection.value }

    private fun open(document: ManagedDocument) {
        viewModelScope.launch {
            when (val result = documentOpener.open(document.document)) {
                is OpenResult.Opened -> effectChannel.send(DocumentEffect.OpenViewer(result.target))
                OpenResult.Unsupported -> message(R.string.manage_open_unsupported)
                OpenResult.Unreadable -> message(R.string.viewer_read_failed)
            }
        }
    }

    private fun share(documents: List<ManagedDocument>) {
        viewModelScope.launch {
            val shared = documents.mapNotNull { managed ->
                val document = managed.document
                documentsRepository.readableUri(document)?.let { SharedDocument(it, document.mimeType, document.name) }
            }
            if (shared.isNotEmpty()) effectChannel.send(DocumentEffect.Share(shared))
        }
    }

    private fun delete(documents: List<ManagedDocument>) {
        viewModelScope.launch {
            var failures = 0
            documents.forEach { managed ->
                val trashed = documentsRepository.moveToTrash(managed.document)
                if (trashed != null) {
                    libraryRepository.addTrashedDocument(
                        TrashedDocument(trashed, managed.folderTreeUri, System.currentTimeMillis())
                    )
                } else {
                    failures++
                }
            }
            selection.value = emptySet()
            refreshes.update { it + 1 }
            message(if (failures == 0) R.string.manage_moved_to_trash else R.string.manage_delete_failed)
        }
    }

    private fun rename(document: ManagedDocument, newName: String) {
        viewModelScope.launch {
            if (documentsRepository.renameDocument(document.document, newName)) {
                refreshes.update { it + 1 }
            } else {
                message(R.string.manage_rename_failed)
            }
        }
    }

    private fun create(type: NewFileType, name: String) {
        val folder = (state.value as? UiState.Success)?.data?.folders?.firstOrNull() ?: return
        viewModelScope.launch {
            val created = documentsRepository.createTextDocument(folder.treeUri, name, type.extension, type.mimeType)
            if (created != null) {
                refreshes.update { it + 1 }
                message(R.string.manage_new_created)
            } else {
                message(R.string.manage_new_create_failed)
            }
        }
    }

    private fun restore(trashed: TrashedDocument) {
        viewModelScope.launch {
            if (documentsRepository.restoreFromTrash(trashed.document, trashed.folderTreeUri)) {
                libraryRepository.removeTrashedDocument(trashed)
                refreshes.update { it + 1 }
            } else {
                message(R.string.trash_restore_failed)
            }
        }
    }

    private fun deleteForever(trashed: TrashedDocument) {
        viewModelScope.launch {
            documentsRepository.deleteForever(trashed.document)
            libraryRepository.removeTrashedDocument(trashed)
        }
    }

    private suspend fun message(text: Int) = effectChannel.send(DocumentEffect.Message(text))

    private suspend fun list(folderUris: Set<String>): Listing {
        val folders = folderUris.mapNotNull { documentsRepository.folder(it.toUri()) }
        val documents = folders.flatMap { folder ->
            documentsRepository.documentsIn(folder.treeUri).map { ManagedDocument(it, folder.treeUri) }
        }
        return Listing(folders, documents)
    }

    private data class Listing(val folders: List<FolderEntry>, val documents: List<ManagedDocument>)

    private data class View(val query: String, val sort: DocumentSort, val selection: Set<Uri>)
}

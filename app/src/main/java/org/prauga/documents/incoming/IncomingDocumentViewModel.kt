package org.prauga.documents.incoming

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import org.prauga.documents.common.ViewerTarget
import org.prauga.documents.common.viewerFormat
import org.prauga.documents.library.data.DocumentOpener
import org.prauga.documents.library.data.DocumentsRepository
import org.prauga.documents.library.data.LibraryRepository
import org.prauga.documents.library.data.OpenResult
import org.prauga.documents.settings.data.SettingsRepository
import javax.inject.Inject

/** Asks once whether to copy incoming documents into the app's own storage, then remembers it. */
@HiltViewModel
class IncomingDocumentViewModel @Inject constructor(
    private val documentsRepository: DocumentsRepository,
    private val libraryRepository: LibraryRepository,
    private val settingsRepository: SettingsRepository,
    private val documentOpener: DocumentOpener,
) : ViewModel() {

    private val mutableState = MutableStateFlow<IncomingUiState>(IncomingUiState.Idle)
    val state: StateFlow<IncomingUiState> = mutableState.asStateFlow()
    private val effectChannel = Channel<IncomingEffect>(Channel.BUFFERED)
    val effects: Flow<IncomingEffect> = effectChannel.receiveAsFlow()
    private var pending: Pending? = null

    fun onEvent(event: IncomingEvent) {
        when (event) {
            is IncomingEvent.Handle -> handle(event.uri, event.fallbackName)
            is IncomingEvent.ChooseAutosave -> chooseAutosave(event.enabled)
        }
    }

    private fun handle(uri: Uri, fallbackName: String) {
        viewModelScope.launch {
            val name = documentsRepository.documentInfo(uri)?.name
                ?: uri.lastPathSegment?.substringAfterLast('/')
                ?: fallbackName
            val incoming = Pending(uri, name)
            val autosave = settingsRepository.settings.filterNotNull().first().autosaveOpenedDocuments
            if (autosave == null) {
                pending = incoming
                mutableState.value = IncomingUiState.AskAutosave(name)
            } else {
                open(incoming, autosave)
            }
        }
    }

    private fun chooseAutosave(enabled: Boolean) {
        val incoming = pending ?: return
        pending = null
        mutableState.value = IncomingUiState.Idle
        settingsRepository.updateAutosaveOpenedDocuments(enabled)
        viewModelScope.launch { open(incoming, enabled) }
    }

    private suspend fun open(incoming: Pending, autosave: Boolean) {
        val (uri, name) = incoming
        val saved = if (autosave) {
            val recentUris = libraryRepository.library.filterNotNull().first().recentDocuments.map { it.uri }
            documentsRepository.copyToAppStorage(uri, name, preferredUris = recentUris)
        } else {
            null
        }
        val entry = saved ?: documentsRepository.documentInfo(uri)?.takeIf { documentsRepository.canRead(uri) }
        val target = if (entry != null) {
            (documentOpener.open(entry) as? OpenResult.Opened)?.target
        } else {
            // Nothing to record: a grant that has already expired, say. The viewer still opens,
            // to say it couldn't read the document, rather than the app doing nothing at all.
            viewerFormat(name, documentsRepository.mimeType(uri))?.let { ViewerTarget(uri, name, it) }
        }
        target?.let { effectChannel.send(IncomingEffect.OpenViewer(it)) }
        effectChannel.send(IncomingEffect.Handled)
    }

    private data class Pending(val uri: Uri, val name: String)
}

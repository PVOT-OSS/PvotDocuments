// SPDX-FileCopyrightText: 2026 Debayan G <debz.exe@gmail.com>
// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: GPL-3.0-or-later

package org.prauga.documents.recent

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.prauga.pvot.coreui.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.prauga.documents.R
import org.prauga.documents.common.DocumentEffect
import org.prauga.documents.common.DocumentKind
import org.prauga.documents.common.SharedDocument
import org.prauga.documents.common.WhileScreenSubscribed
import org.prauga.documents.common.documentKind
import org.prauga.documents.library.data.DocumentEntry
import org.prauga.documents.library.data.DocumentOpener
import org.prauga.documents.library.data.DocumentsRepository
import org.prauga.documents.library.data.LibraryRepository
import org.prauga.documents.library.data.OpenResult
import javax.inject.Inject

@HiltViewModel
class RecentViewModel @Inject constructor(
    private val libraryRepository: LibraryRepository,
    private val documentsRepository: DocumentsRepository,
    private val documentOpener: DocumentOpener,
) : ViewModel() {

    private val selectedKind = MutableStateFlow<DocumentKind?>(null)
    private val effectChannel = Channel<DocumentEffect>(Channel.BUFFERED)
    val effects: Flow<DocumentEffect> = effectChannel.receiveAsFlow()

    val state: StateFlow<UiState<RecentUiState>> =
        combine(libraryRepository.library, selectedKind) { library, kind ->
            val documents = library?.recentDocuments ?: return@combine UiState.Loading
            val kinds = documents.map { documentKind(it.name, it.mimeType) }.distinct()
            val selected = kind?.takeIf { it in kinds }
            UiState.Success(
                RecentUiState(
                    documents = documents,
                    kinds = kinds,
                    selectedKind = selected,
                    visibleDocuments = selected?.let { k ->
                        documents.filter { documentKind(it.name, it.mimeType) == k }
                    } ?: documents,
                )
            )
        }.stateIn(viewModelScope, WhileScreenSubscribed, UiState.Loading)

    fun onEvent(event: RecentEvent) {
        when (event) {
            is RecentEvent.Open -> open(event.document)
            is RecentEvent.Share -> share(event.document)
            is RecentEvent.Filter -> selectedKind.value = event.kind
        }
    }

    private fun open(document: DocumentEntry) {
        viewModelScope.launch {
            when (val result = documentOpener.open(document)) {
                is OpenResult.Opened -> effectChannel.send(DocumentEffect.OpenViewer(result.target))
                OpenResult.Unreadable -> {
                    libraryRepository.removeRecentDocument(document)
                    effectChannel.send(DocumentEffect.Message(R.string.recent_unavailable))
                }
            }
        }
    }

    private fun share(document: DocumentEntry) {
        viewModelScope.launch {
            val uri = documentsRepository.readableUri(document) ?: return@launch
            effectChannel.send(DocumentEffect.Share(listOf(SharedDocument(uri, document.mimeType, document.name))))
        }
    }
}

package org.prauga.documents.viewer

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import org.prauga.documents.common.WhileScreenSubscribed
import org.prauga.documents.library.data.DocumentsRepository
import org.prauga.documents.settings.data.SettingsRepository
import javax.inject.Inject

@HiltViewModel
class DocumentViewerViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val documentsRepository: DocumentsRepository,
) : ViewModel() {

    suspend fun readText(uri: Uri): String = documentsRepository.readText(uri)

    val darkPdfPages: StateFlow<Boolean?> = settingsRepository.settings
        .map { it?.darkPdfPages }
        .stateIn(viewModelScope, WhileScreenSubscribed, null)

    fun onEvent(event: DocumentViewerEvent) {
        when (event) {
            is DocumentViewerEvent.DarkPdfPages -> settingsRepository.updateDarkPdfPages(event.enabled)
        }
    }
}

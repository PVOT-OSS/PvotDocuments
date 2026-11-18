package org.prauga.documents.viewer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import org.prauga.documents.settings.data.SettingsRepository
import javax.inject.Inject

@HiltViewModel
class DocumentViewerViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
) : ViewModel() {

    val darkPdfPages: StateFlow<Boolean?> = settingsRepository.settings
        .map { it?.darkPdfPages }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(StopTimeoutMillis), null)

    fun onEvent(event: DocumentViewerEvent) {
        when (event) {
            is DocumentViewerEvent.DarkPdfPages -> settingsRepository.updateDarkPdfPages(event.enabled)
        }
    }

    private companion object {
        const val StopTimeoutMillis = 5_000L
    }
}

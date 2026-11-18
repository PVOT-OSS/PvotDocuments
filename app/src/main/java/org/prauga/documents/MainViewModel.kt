package org.prauga.documents

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.prauga.pvot.coreui.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import org.prauga.documents.settings.data.AppSettings
import org.prauga.documents.settings.data.SettingsRepository
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
) : ViewModel() {

    val state: StateFlow<UiState<AppSettings>> = settingsRepository.settings
        .map { settings -> settings?.let { UiState.Success(it) } ?: UiState.Loading }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(StopTimeoutMillis), UiState.Loading)

    fun onEvent(event: MainEvent) {
        when (event) {
            MainEvent.CompleteOnboarding -> settingsRepository.completeOnboarding()
            is MainEvent.AutosaveOpenedDocuments ->
                settingsRepository.updateAutosaveOpenedDocuments(event.enabled)
        }
    }

    private companion object {
        const val StopTimeoutMillis = 5_000L
    }
}

package org.prauga.documents.settings

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
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
) : ViewModel() {

    val state: StateFlow<UiState<AppSettings>> = settingsRepository.settings
        .map { settings -> settings?.let { UiState.Success(it) } ?: UiState.Loading }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(StopTimeoutMillis), UiState.Loading)

    fun onEvent(event: SettingsEvent) {
        when (event) {
            is SettingsEvent.GridView -> settingsRepository.updateGridView(event.enabled)
            is SettingsEvent.DynamicColor -> settingsRepository.updateDynamicColor(event.enabled)
            is SettingsEvent.GlassIntensity -> settingsRepository.updateGlassIntensity(event.intensity)
            is SettingsEvent.AutosaveOpenedDocuments ->
                settingsRepository.updateAutosaveOpenedDocuments(event.enabled)
        }
    }

    private companion object {
        const val StopTimeoutMillis = 5_000L
    }
}

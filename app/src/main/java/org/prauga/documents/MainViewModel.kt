package org.prauga.documents

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.prauga.pvot.coreui.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import org.prauga.documents.common.WhileScreenSubscribed
import org.prauga.documents.settings.data.AppSettings
import org.prauga.documents.settings.data.SettingsRepository
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    settingsRepository: SettingsRepository,
) : ViewModel() {

    val state: StateFlow<UiState<AppSettings>> = settingsRepository.settings
        .map { settings -> settings?.let { UiState.Success(it) } ?: UiState.Loading }
        .stateIn(viewModelScope, WhileScreenSubscribed, UiState.Loading)
}

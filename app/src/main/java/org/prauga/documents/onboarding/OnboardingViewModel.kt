// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: GPL-3.0-or-later

package org.prauga.documents.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import org.prauga.documents.common.WhileScreenSubscribed
import org.prauga.documents.library.data.LibraryRepository
import org.prauga.documents.settings.data.SettingsRepository
import javax.inject.Inject

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val libraryRepository: LibraryRepository,
    private val settingsRepository: SettingsRepository,
) : ViewModel() {

    // A pick counts as that folder's even when the user chose another one in the picker.
    private val picked = MutableStateFlow<Set<PublicFolder>>(emptySet())

    val state: StateFlow<OnboardingUiState> =
        combine(libraryRepository.library, picked) { library, picked ->
            val folderUris = library?.folderUris.orEmpty()
            OnboardingUiState(
                addedFolders = PublicFolder.entries
                    .filter { it in picked || isPublicFolderAdded(folderUris, it) }
                    .toSet()
            )
        }.stateIn(viewModelScope, WhileScreenSubscribed, OnboardingUiState(emptySet()))

    fun onEvent(event: OnboardingEvent) {
        when (event) {
            is OnboardingEvent.AddFolder -> {
                libraryRepository.addFolder(event.treeUri)
                picked.update { it + event.folder }
            }
            OnboardingEvent.Continue -> settingsRepository.completeOnboarding()
        }
    }
}

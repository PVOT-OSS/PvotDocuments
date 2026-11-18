package org.prauga.documents

sealed interface MainEvent {
    data object CompleteOnboarding : MainEvent
    data class AutosaveOpenedDocuments(val enabled: Boolean) : MainEvent
}

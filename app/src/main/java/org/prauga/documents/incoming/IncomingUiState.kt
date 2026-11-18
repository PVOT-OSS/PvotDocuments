package org.prauga.documents.incoming

sealed interface IncomingUiState {
    data object Idle : IncomingUiState
    data class AskAutosave(val documentName: String) : IncomingUiState
}

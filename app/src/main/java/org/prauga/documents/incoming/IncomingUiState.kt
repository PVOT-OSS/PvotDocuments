// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: GPL-3.0-or-later

package org.prauga.documents.incoming

sealed interface IncomingUiState {
    data object Idle : IncomingUiState
    data class AskAutosave(val documentName: String) : IncomingUiState
}

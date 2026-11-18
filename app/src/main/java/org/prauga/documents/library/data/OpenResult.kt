// SPDX-FileCopyrightText: 2026 Debayan G <debz.exe@gmail.com>
// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: GPL-3.0-or-later

package org.prauga.documents.library.data

import org.prauga.documents.common.ViewerTarget

sealed interface OpenResult {
    data class Opened(val target: ViewerTarget) : OpenResult
    data object Unreadable : OpenResult
}

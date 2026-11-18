// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: GPL-3.0-or-later

package org.prauga.documents.incoming

import org.prauga.documents.common.ViewerTarget

sealed interface IncomingEffect {
    data class OpenViewer(val target: ViewerTarget) : IncomingEffect
    data object Handled : IncomingEffect
}

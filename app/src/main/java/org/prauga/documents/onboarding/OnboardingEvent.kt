// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: GPL-3.0-or-later

package org.prauga.documents.onboarding

import android.net.Uri

sealed interface OnboardingEvent {
    data class AddFolder(val folder: PublicFolder, val treeUri: Uri) : OnboardingEvent
    data object Continue : OnboardingEvent
}

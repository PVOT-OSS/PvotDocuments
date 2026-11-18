package org.prauga.documents.onboarding

import android.net.Uri

sealed interface OnboardingEvent {
    data class AddFolder(val folder: PublicFolder, val treeUri: Uri) : OnboardingEvent
    data object Continue : OnboardingEvent
}

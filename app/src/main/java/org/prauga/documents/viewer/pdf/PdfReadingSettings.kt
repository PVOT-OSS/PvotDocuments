package org.prauga.documents.viewer.pdf

import androidx.compose.ui.graphics.ColorFilter
import org.prauga.documents.viewer.data.ReadingPosition

/** How the reader wants PDFs shown, from the app's preferences: where to start reading, where
 * reading stopped (to remember), and the dark-pages filter, null for normal pages. */
class PdfReadingSettings(
    val initialPosition: ReadingPosition?,
    val onPositionChange: (ReadingPosition) -> Unit,
    val pageFilter: ColorFilter?,
)

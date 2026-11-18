package org.prauga.documents.viewer

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import org.prauga.documents.R

/** What a document is drawn with where it gives no color of its own: the paper, ink and table
 * lines of a printed page, the same in light and dark theme. */
internal object DocumentColors {
    val paper: Color
        @Composable get() = colorResource(R.color.document_paper)

    val darkPaper: Color
        @Composable get() = colorResource(R.color.document_paper_dark)

    val ink: Color
        @Composable get() = colorResource(R.color.document_ink)

    val grid: Color
        @Composable get() = colorResource(R.color.document_grid)
}

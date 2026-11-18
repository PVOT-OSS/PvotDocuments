// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: GPL-3.0-or-later

package org.prauga.documents.viewer

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import org.prauga.documents.R

/** Colors for documents that set none of their own; the same in light and dark theme, like paper. */
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

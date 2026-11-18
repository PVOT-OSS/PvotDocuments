package org.prauga.documents.viewer.pdf

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.res.stringResource
import com.prauga.pvot.designsystem.components.button.PvotButton
import com.prauga.pvot.designsystem.components.button.PvotButtonStyle
import com.prauga.pvot.designsystem.components.pvotGlassSurface
import com.prauga.pvot.designsystem.effects.LocalPvotGlassBackdrop
import com.prauga.pvot.designsystem.effects.PvotGlassDefaults
import com.prauga.pvot.designsystem.effects.PvotGlassMaterial
import com.prauga.pvot.designsystem.theme.PvotSpacing
import org.prauga.documents.R

/** What to do with the selected text, in a glass capsule over the bottom of the document: copy,
 * share, search the document for it, or widen the selection to the whole document. Thick glass,
 * like the search bar, because it sits over white pages. */
@Composable
fun PdfSelectionBar(state: PdfSelectionState, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .onSizeChanged { state.barHeightPx = it.height }
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(horizontal = PvotSpacing.L, vertical = PvotSpacing.M)
            .pvotGlassSurface(
                backdrop = LocalPvotGlassBackdrop.current,
                style = PvotGlassDefaults.materialStyle(PvotGlassMaterial.Thick),
                role = null,
            )
            .padding(PvotSpacing.XS),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PvotButton(
            text = stringResource(R.string.viewer_pdf_copy_selection),
            onClick = { state.onCopy() },
            style = PvotButtonStyle.Text,
        )
        PvotButton(
            text = stringResource(R.string.viewer_pdf_share_selection),
            onClick = { state.onShare() },
            style = PvotButtonStyle.Text,
        )
        PvotButton(
            text = stringResource(R.string.viewer_pdf_search_selection),
            onClick = { state.onSearch() },
            style = PvotButtonStyle.Text,
        )
        PvotButton(
            text = stringResource(R.string.viewer_pdf_select_all),
            onClick = { state.onSelectAll() },
            style = PvotButtonStyle.Text,
        )
    }
}

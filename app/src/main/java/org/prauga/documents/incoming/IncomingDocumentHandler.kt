package org.prauga.documents.incoming

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.prauga.pvot.designsystem.components.PvotDialog
import com.prauga.pvot.designsystem.components.PvotDialogAction
import com.prauga.pvot.designsystem.components.PvotDialogActionStyle
import org.prauga.documents.R
import org.prauga.documents.viewer.DocumentViewerActivity

/** Handles [uri] through [IncomingDocumentViewModel]; [onHandled] runs once it is done. */
@Composable
fun IncomingDocumentHandler(
    uri: Uri,
    onHandled: () -> Unit,
    viewModel: IncomingDocumentViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val fallbackName = stringResource(R.string.incoming_default_name)
    val handled by rememberUpdatedState(onHandled)
    LaunchedEffect(uri) { viewModel.onEvent(IncomingEvent.Handle(uri, fallbackName)) }
    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is IncomingEffect.OpenViewer -> DocumentViewerActivity.start(context, effect.target)
                IncomingEffect.Handled -> handled()
            }
        }
    }
    val uiState by viewModel.state.collectAsStateWithLifecycle()
    (uiState as? IncomingUiState.AskAutosave)?.let { ask ->
        AutosavePromptDialog(
            documentName = ask.documentName,
            onChoose = { viewModel.onEvent(IncomingEvent.ChooseAutosave(it)) }
        )
    }
}

@Composable
private fun AutosavePromptDialog(documentName: String, onChoose: (Boolean) -> Unit) {
    PvotDialog(
        visible = true,
        // The choice is required to proceed; dismissing without one would leave the document
        // half-handled. A default lives in Settings for anyone who skips this by force-closing.
        onDismissRequest = {},
        dismissOnOutsideTap = false,
        title = stringResource(R.string.incoming_autosave_title),
        message = stringResource(R.string.incoming_autosave_message, documentName)
    ) {
        PvotDialogAction(
            label = stringResource(R.string.incoming_autosave_save),
            onClick = { onChoose(true) },
            style = PvotDialogActionStyle.Primary
        )
        PvotDialogAction(
            label = stringResource(R.string.incoming_autosave_dont_save),
            onClick = { onChoose(false) }
        )
    }
}

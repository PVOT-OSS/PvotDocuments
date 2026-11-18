package org.prauga.documents.common

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import kotlinx.coroutines.flow.Flow
import org.prauga.documents.viewer.DocumentViewerActivity

/** Carries out the [DocumentEffect]s a screen's ViewModel sends, as they arrive. */
@Composable
fun DocumentEffects(effects: Flow<DocumentEffect>, onMessage: (String) -> Unit) {
    val context = LocalContext.current
    val resources = LocalResources.current
    val showMessage by rememberUpdatedState(onMessage)
    LaunchedEffect(effects) {
        effects.collect { effect ->
            when (effect) {
                is DocumentEffect.OpenViewer -> DocumentViewerActivity.start(context, effect.target)
                is DocumentEffect.Share -> shareDocuments(context, effect.documents)
                is DocumentEffect.Message -> showMessage(resources.getString(effect.text))
            }
        }
    }
}

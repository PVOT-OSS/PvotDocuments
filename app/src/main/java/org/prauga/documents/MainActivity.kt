package org.prauga.documents

import android.content.ContentResolver
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.prauga.pvot.coreui.UiState
import com.prauga.pvot.designsystem.effects.ProvidePvotGlassIntensity
import com.prauga.pvot.designsystem.platform.PvotBaseActivity
import com.prauga.pvot.designsystem.theme.PvotAppTheme
import dagger.hilt.android.AndroidEntryPoint
import org.prauga.documents.common.parcelableExtraCompat
import org.prauga.documents.home.AppShell
import org.prauga.documents.incoming.IncomingDocumentHandler
import org.prauga.documents.onboarding.OnboardingScreen

@AndroidEntryPoint
class MainActivity : PvotBaseActivity() {

    // A property, not local state: onNewIntent runs outside composition and needs somewhere to
    // put the next URI that the Compose tree is already observing.
    private var incomingUri by mutableStateOf<Uri?>(null)
    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        incomingUri = incomingDocumentUri(intent)
        setPvotContent {
            val uiState by viewModel.state.collectAsStateWithLifecycle()
            val settings = (uiState as? UiState.Success)?.data ?: return@setPvotContent
            PvotAppTheme(dynamicColor = settings.dynamicColor) {
                ProvidePvotGlassIntensity(settings.glassIntensity) {
                    if (settings.onboardingComplete) {
                        AppShell()
                    } else {
                        OnboardingScreen()
                    }
                    incomingUri?.let { uri ->
                        IncomingDocumentHandler(
                            uri = uri,
                            onHandled = { incomingUri = null }
                        )
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        incomingUri = incomingDocumentUri(intent)
    }

    // Only content:// URIs: a file:// URI from another app could name a file in this app's own
    // private storage, which the viewer and autosave would then read on that app's behalf.
    private fun incomingDocumentUri(intent: Intent?): Uri? {
        val uri = when (intent?.action) {
            Intent.ACTION_VIEW -> intent.data
            Intent.ACTION_SEND -> intent.parcelableExtraCompat<Uri>(Intent.EXTRA_STREAM)
            else -> null
        }
        return uri?.takeIf { it.scheme == ContentResolver.SCHEME_CONTENT }
    }
}

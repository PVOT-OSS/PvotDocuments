package org.prauga.documents.onboarding

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.prauga.pvot.designsystem.components.PvotCard
import com.prauga.pvot.designsystem.components.button.PvotButton
import com.prauga.pvot.designsystem.components.button.PvotButtonStyle
import com.prauga.pvot.designsystem.theme.PvotSpacing
import org.prauga.documents.R

/**
 * Shown once, before [org.prauga.documents.home.AppShell]: lets the user grant Documents and
 * Downloads up front, so Manage already has them instead of starting empty. There is no API to
 * grant folder access without the system picker, so each button still opens it, pointed straight
 * at that folder through the picker's initial-URI hint (see [PublicFolder.initialUri]) — one tap
 * to confirm rather than a folder hunt. Continuing is always allowed; neither folder is required.
 */
@Composable
fun OnboardingScreen(
    modifier: Modifier = Modifier,
    viewModel: OnboardingViewModel = hiltViewModel(),
) {
    val uiState by viewModel.state.collectAsStateWithLifecycle()
    OnboardingContent(uiState, viewModel::onEvent, modifier)
}

@Composable
private fun OnboardingContent(
    uiState: OnboardingUiState,
    onEvent: (OnboardingEvent) -> Unit,
    modifier: Modifier,
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(
            start = PvotSpacing.L,
            end = PvotSpacing.L,
            top = PvotSpacing.L + WindowInsets.statusBars.asPaddingValues().calculateTopPadding(),
            bottom = PvotSpacing.L + WindowInsets.navigationBars.asPaddingValues()
                .calculateBottomPadding()
        ),
        verticalArrangement = Arrangement.spacedBy(PvotSpacing.L)
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(PvotSpacing.S)) {
                Text(
                    text = stringResource(R.string.onboarding_title),
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = stringResource(R.string.onboarding_subtitle),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        item {
            FolderOptionCard(
                folder = PublicFolder.Documents,
                added = PublicFolder.Documents in uiState.addedFolders,
                onPicked = { onEvent(OnboardingEvent.AddFolder(PublicFolder.Documents, it)) },
                titleRes = R.string.onboarding_documents_title,
                subtitleRes = R.string.onboarding_documents_subtitle,
                iconRes = R.drawable.ic_file_text
            )
        }
        item {
            FolderOptionCard(
                folder = PublicFolder.Downloads,
                added = PublicFolder.Downloads in uiState.addedFolders,
                onPicked = { onEvent(OnboardingEvent.AddFolder(PublicFolder.Downloads, it)) },
                titleRes = R.string.onboarding_downloads_title,
                subtitleRes = R.string.onboarding_downloads_subtitle,
                iconRes = R.drawable.ic_download
            )
        }
        item {
            PvotButton(
                text = stringResource(R.string.onboarding_continue),
                onClick = { onEvent(OnboardingEvent.Continue) },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun FolderOptionCard(
    folder: PublicFolder,
    added: Boolean,
    onPicked: (Uri) -> Unit,
    titleRes: Int,
    subtitleRes: Int,
    iconRes: Int
) {
    val pickFolder = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocumentTree()
    ) { uri: Uri? ->
        if (uri != null) onPicked(uri)
    }

    PvotCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(PvotSpacing.L),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(PvotSpacing.L)
        ) {
            Icon(
                painterResource(iconRes),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(titleRes),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = stringResource(subtitleRes),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = PvotSpacing.XS)
                )
            }
            PvotButton(
                text = stringResource(
                    if (added) R.string.onboarding_added else R.string.onboarding_allow
                ),
                // OpenDocumentTree's input *is* the initial-URI hint; the picker opens inside
                // `folder` instead of at the storage root.
                onClick = { pickFolder.launch(folder.initialUri) },
                style = if (added) PvotButtonStyle.Tonal else PvotButtonStyle.Filled,
                enabled = !added
            )
        }
    }
}

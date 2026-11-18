package org.prauga.documents.recent

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.prauga.pvot.coreui.UiState
import com.prauga.pvot.designsystem.components.PvotScreen
import com.prauga.pvot.designsystem.components.PvotScreenDefaults
import com.prauga.pvot.designsystem.components.button.PvotIconButton
import com.prauga.pvot.designsystem.components.list.PvotListGroup
import com.prauga.pvot.designsystem.components.list.PvotListRow
import com.prauga.pvot.designsystem.motion.pvotReveal
import com.prauga.pvot.designsystem.theme.PvotSpacing
import org.prauga.documents.R
import org.prauga.documents.common.DocumentEffects
import org.prauga.documents.common.documentKind
import org.prauga.documents.common.formatFileSize
import org.prauga.documents.tools.EmptyTab

/**
 * Documents the app was opened or shared with, most recent first, filterable by type — see
 * [org.prauga.documents.incoming.IncomingDocumentHandler]. Tapping one opens it if its format
 * has a viewer; [onMessage] reports when it doesn't.
 */
@Composable
fun RecentScreen(
    state: LazyListState,
    topPadding: Dp,
    onMessage: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: RecentViewModel = hiltViewModel(),
) {
    DocumentEffects(viewModel.effects, onMessage)
    val uiState by viewModel.state.collectAsStateWithLifecycle()
    val recent = (uiState as? UiState.Success)?.data ?: return
    if (recent.documents.isEmpty()) {
        EmptyTab(state, topPadding, R.drawable.ic_recent, stringResource(R.string.recent_empty), modifier)
        return
    }
    RecentContent(recent, viewModel::onEvent, state, topPadding, modifier)
}

@Composable
private fun RecentContent(
    recent: RecentUiState,
    onEvent: (RecentEvent) -> Unit,
    state: LazyListState,
    topPadding: Dp,
    modifier: Modifier,
) {
    val selectedKind = recent.selectedKind
    val visibleDocuments = recent.visibleDocuments
    PvotScreen(
        modifier = modifier,
        state = state,
        contentPadding = PvotScreenDefaults.contentPadding(top = topPadding)
    ) {
        if (recent.kinds.size > 1) {
            item {
                LazyRow(
                    modifier = Modifier.fillMaxWidth().pvotReveal(0),
                    horizontalArrangement = Arrangement.spacedBy(PvotSpacing.S)
                ) {
                    item {
                        FilterChip(
                            selected = selectedKind == null,
                            onClick = { onEvent(RecentEvent.Filter(null)) },
                            label = { Text(stringResource(R.string.recent_filter_all)) }
                        )
                    }
                    items(recent.kinds) { kind ->
                        FilterChip(
                            selected = selectedKind == kind,
                            onClick = {
                                onEvent(RecentEvent.Filter(if (selectedKind == kind) null else kind))
                            },
                            label = { Text(stringResource(kind.labelRes)) }
                        )
                    }
                }
            }
        }
        if (visibleDocuments.isEmpty()) {
            item {
                Text(
                    text = stringResource(
                        R.string.recent_no_filter_results,
                        selectedKind?.let { stringResource(it.labelRes) }.orEmpty()
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.fillMaxWidth().pvotReveal(1)
                )
            }
        } else {
            item {
                PvotListGroup(modifier = Modifier.fillMaxWidth().pvotReveal(1)) {
                    visibleDocuments.forEach { document ->
                        item {
                            PvotListRow(
                                title = document.name,
                                supportingText = formatFileSize(document.sizeBytes),
                                leading = {
                                    Icon(
                                        painterResource(R.drawable.ic_file_text),
                                        contentDescription = null,
                                        tint = documentKind(document.name, document.mimeType).color()
                                    )
                                },
                                trailing = {
                                    PvotIconButton(
                                        icon = painterResource(R.drawable.ic_share),
                                        contentDescription = stringResource(
                                            R.string.manage_share_document,
                                            document.name
                                        ),
                                        onClick = { onEvent(RecentEvent.Share(document)) },
                                        size = 36.dp
                                    )
                                },
                                onClick = { onEvent(RecentEvent.Open(document)) }
                            )
                        }
                    }
                }
            }
        }
    }
}

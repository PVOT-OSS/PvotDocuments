// SPDX-FileCopyrightText: 2026 Debayan G <debz.exe@gmail.com>
// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: GPL-3.0-or-later

package org.prauga.documents.manage

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.prauga.pvot.coreui.PvotEmptyContent
import com.prauga.pvot.coreui.UiState
import com.prauga.pvot.designsystem.components.PvotBottomSheet
import com.prauga.pvot.designsystem.components.PvotCard
import com.prauga.pvot.designsystem.components.PvotCardDefaults
import com.prauga.pvot.designsystem.components.PvotDialog
import com.prauga.pvot.designsystem.components.PvotDialogAction
import com.prauga.pvot.designsystem.components.PvotDialogActionStyle
import com.prauga.pvot.designsystem.components.PvotScreen
import com.prauga.pvot.designsystem.components.PvotScreenDefaults
import com.prauga.pvot.designsystem.components.PvotSegmentedControl
import com.prauga.pvot.designsystem.components.PvotSheetItem
import com.prauga.pvot.designsystem.components.button.PvotFab
import com.prauga.pvot.designsystem.components.button.PvotIconButton
import com.prauga.pvot.designsystem.components.button.PvotIconButtonDefaults
import com.prauga.pvot.designsystem.components.list.PvotListGroup
import com.prauga.pvot.designsystem.components.list.PvotListRow
import com.prauga.pvot.designsystem.components.textfield.PvotTextField
import com.prauga.pvot.designsystem.motion.pvotReveal
import com.prauga.pvot.designsystem.navigation.LocalPvotNavBarSizes
import com.prauga.pvot.designsystem.theme.PvotSpacing
import org.prauga.documents.R
import org.prauga.documents.common.DocumentEffects
import org.prauga.documents.common.DocumentSort
import org.prauga.documents.common.documentKind
import org.prauga.documents.common.formatFileSize
import org.prauga.documents.library.data.ManagedDocument

/** [onMessage] reports a document with no viewer, and the outcome of other actions. */
@Composable
fun ManageScreen(
    state: LazyListState,
    topPadding: Dp,
    onMessage: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ManageViewModel = hiltViewModel(),
) {
    DocumentEffects(viewModel.effects, onMessage)
    val uiState by viewModel.state.collectAsStateWithLifecycle()
    val manage = (uiState as? UiState.Success)?.data ?: return
    ManageContent(manage, viewModel::onEvent, onMessage, state, topPadding, modifier)
}

@Composable
private fun ManageContent(
    manage: ManageUiState,
    onEvent: (ManageEvent) -> Unit,
    onMessage: (String) -> Unit,
    state: LazyListState,
    topPadding: Dp,
    modifier: Modifier,
) {
    var showTrash by remember { mutableStateOf(false) }
    var actionsFor by remember { mutableStateOf<ManagedDocument?>(null) }
    var renaming by remember { mutableStateOf<ManagedDocument?>(null) }
    var searchQuery by rememberSaveable { mutableStateOf(manage.searchQuery) }
    var showNewSheet by remember { mutableStateOf(false) }
    var creatingType by remember { mutableStateOf<NewFileType?>(null) }
    val scanUnsupportedMessage = stringResource(R.string.manage_scan_unsupported)
    val untitledName = stringResource(R.string.manage_new_untitled)
    val folders = manage.folders
    val documents = manage.documents
    val selectedUris = manage.selectedUris
    val visibleDocuments = manage.visibleDocuments

    val pickFolder = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocumentTree()
    ) { uri: Uri? ->
        if (uri != null) onEvent(ManageEvent.AddFolder(uri))
    }

    if (showTrash) {
        RecentlyDeletedSheet(
            documents = manage.trashedDocuments,
            onDismiss = { showTrash = false },
            onRestore = { onEvent(ManageEvent.Restore(it)) },
            onDeleteForever = { onEvent(ManageEvent.DeleteForever(it)) },
        )
    }

    actionsFor?.let { managed ->
        DocumentActionsSheet(
            document = managed.document,
            onDismiss = { actionsFor = null },
            onShare = { onEvent(ManageEvent.Share(managed)) },
            onRename = { renaming = managed },
            onSelect = { onEvent(ManageEvent.SelectOnly(managed.document.uri)) },
            onDelete = { onEvent(ManageEvent.Delete(managed)) }
        )
    }

    renaming?.let { managed ->
        var newName by remember(managed) { mutableStateOf(managed.document.name) }
        PvotDialog(
            visible = true,
            onDismissRequest = { renaming = null },
            title = stringResource(R.string.manage_rename_title),
            content = {
                PvotTextField(
                    value = newName,
                    onValueChange = { newName = it },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        ) {
            PvotDialogAction(
                label = stringResource(R.string.manage_rename_cancel),
                onClick = { renaming = null }
            )
            PvotDialogAction(
                label = stringResource(R.string.manage_rename_save),
                style = PvotDialogActionStyle.Primary,
                enabled = newName.isNotBlank(),
                onClick = {
                    onEvent(ManageEvent.Rename(managed, newName.trim()))
                    renaming = null
                }
            )
        }
    }

    if (showNewSheet) {
        NewDocumentSheet(
            onDismiss = { showNewSheet = false },
            onPickType = { type -> creatingType = type },
            onScan = { onMessage(scanUnsupportedMessage) }
        )
    }

    creatingType?.let { type ->
        var name by remember(type) { mutableStateOf("") }
        PvotDialog(
            visible = true,
            onDismissRequest = { creatingType = null },
            title = stringResource(type.titleRes),
            content = {
                PvotTextField(
                    value = name,
                    onValueChange = { name = it },
                    placeholder = stringResource(R.string.manage_new_filename_placeholder),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        ) {
            PvotDialogAction(
                label = stringResource(R.string.manage_new_cancel),
                onClick = { creatingType = null }
            )
            PvotDialogAction(
                label = stringResource(R.string.manage_new_create),
                style = PvotDialogActionStyle.Primary,
                onClick = {
                    onEvent(ManageEvent.Create(type, name.trim().ifBlank { untitledName }))
                    creatingType = null
                }
            )
        }
    }

    if (folders.isEmpty()) {
        PvotScreen(
            modifier = modifier,
            state = state,
            contentPadding = PvotScreenDefaults.contentPadding(top = topPadding)
        ) {
            item {
                Box(Modifier.fillParentMaxSize(), contentAlignment = Alignment.Center) {
                    PvotEmptyContent(
                        message = stringResource(R.string.manage_empty),
                        icon = {
                            Icon(painterResource(R.drawable.ic_folder), contentDescription = null)
                        },
                        actionLabel = stringResource(R.string.manage_add_folder),
                        onAction = { pickFolder.launch(null) }
                    )
                }
            }
        }
        return
    }

    Box(modifier = modifier) {
    PvotScreen(
        modifier = Modifier.fillMaxSize(),
        state = state,
        contentPadding = PvotScreenDefaults.contentPadding(top = topPadding)
    ) {
        if (selectedUris.isNotEmpty()) {
            item {
                SelectionToolbar(
                    count = selectedUris.size,
                    onSelectAll = { onEvent(ManageEvent.SelectAll) },
                    onShare = { onEvent(ManageEvent.ShareSelected) },
                    onDelete = { onEvent(ManageEvent.DeleteSelected) },
                    onCancel = { onEvent(ManageEvent.ClearSelection) },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
        item {
            PvotListGroup(
                modifier = Modifier.fillMaxWidth().pvotReveal(0),
                title = stringResource(R.string.manage_section_folders)
            ) {
                folders.forEach { folder ->
                    item {
                        PvotListRow(
                            title = folder.displayName,
                            leading = {
                                Icon(
                                    painterResource(R.drawable.ic_folder),
                                    contentDescription = null
                                )
                            },
                            trailing = {
                                PvotIconButton(
                                    icon = painterResource(R.drawable.ic_x),
                                    contentDescription = stringResource(
                                        R.string.manage_remove_folder,
                                        folder.displayName
                                    ),
                                    onClick = { onEvent(ManageEvent.RemoveFolder(folder.treeUri)) },
                                    size = PvotIconButtonDefaults.SmallSize
                                )
                            }
                        )
                    }
                }
                item {
                    PvotListRow(
                        title = stringResource(R.string.manage_add_folder),
                        leading = {
                            Icon(
                                painterResource(R.drawable.ic_folder_plus),
                                contentDescription = null
                            )
                        },
                        onClick = { pickFolder.launch(null) }
                    )
                }
                item {
                    PvotListRow(
                        title = stringResource(
                            R.string.manage_recently_deleted,
                            manage.trashedDocuments.size
                        ),
                        leading = {
                            Icon(painterResource(R.drawable.ic_trash), contentDescription = null)
                        },
                        onClick = { showTrash = true }
                    )
                }
            }
        }
        if (documents.isNotEmpty()) {
            item {
                PvotTextField(
                    value = searchQuery,
                    onValueChange = {
                        searchQuery = it
                        onEvent(ManageEvent.Search(it))
                    },
                    placeholder = stringResource(R.string.manage_search_placeholder),
                    leadingIcon = {
                        Icon(painterResource(R.drawable.ic_search), contentDescription = null)
                    },
                    trailingIcon = if (searchQuery.isNotEmpty()) {
                        {
                            PvotIconButton(
                                icon = painterResource(R.drawable.ic_x),
                                contentDescription = stringResource(R.string.manage_search_clear),
                                onClick = {
                                    searchQuery = ""
                                    onEvent(ManageEvent.Search(""))
                                },
                                size = dimensionResource(R.dimen.icon_button_compact)
                            )
                        }
                    } else {
                        null
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().pvotReveal(1)
                )
            }
            item {
                PvotSegmentedControl(
                    options = listOf(
                        stringResource(R.string.manage_sort_name),
                        stringResource(R.string.manage_sort_date),
                        stringResource(R.string.manage_sort_size)
                    ),
                    selectedIndex = manage.sort.ordinal,
                    onSelect = { onEvent(ManageEvent.Sort(DocumentSort.entries[it])) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = PvotSpacing.S)
                        .pvotReveal(2)
                )
            }
        }
        if (visibleDocuments.isNotEmpty()) {
            if (manage.gridView) {
                item {
                    FlowRow(
                        modifier = Modifier.fillMaxWidth().pvotReveal(3),
                        horizontalArrangement = Arrangement.spacedBy(PvotSpacing.M),
                        verticalArrangement = Arrangement.spacedBy(PvotSpacing.M)
                    ) {
                        visibleDocuments.forEach { managed ->
                            val selected = managed.document.uri in selectedUris
                            DocumentGridCard(
                                managed = managed,
                                selected = selected,
                                inSelectionMode = selectedUris.isNotEmpty(),
                                onClick = {
                                    if (selectedUris.isNotEmpty()) {
                                        onEvent(ManageEvent.ToggleSelection(managed.document.uri))
                                    } else {
                                        onEvent(ManageEvent.Open(managed))
                                    }
                                },
                                onMore = { actionsFor = managed }
                            )
                        }
                    }
                }
            } else {
                item {
                    PvotListGroup(
                        modifier = Modifier.fillMaxWidth().pvotReveal(3),
                        title = stringResource(R.string.manage_section_documents)
                    ) {
                        visibleDocuments.forEach { managed ->
                            item {
                                val document = managed.document
                                val selected = document.uri in selectedUris
                                PvotListRow(
                                    title = document.name,
                                    supportingText = formatFileSize(document.sizeBytes),
                                    selected = selected,
                                    leading = {
                                        Icon(
                                            painterResource(
                                                if (selected) {
                                                    R.drawable.ic_check_circle_filled
                                                } else {
                                                    R.drawable.ic_file_text
                                                }
                                            ),
                                            contentDescription = null,
                                            tint = if (selected) {
                                                MaterialTheme.colorScheme.primary
                                            } else {
                                                documentKind(document.name, document.mimeType).color()
                                            }
                                        )
                                    },
                                    trailing = if (selectedUris.isEmpty()) {
                                        {
                                            PvotIconButton(
                                                icon = painterResource(R.drawable.ic_more),
                                                contentDescription = stringResource(
                                                    R.string.manage_document_actions,
                                                    document.name
                                                ),
                                                onClick = { actionsFor = managed },
                                                size = PvotIconButtonDefaults.SmallSize
                                            )
                                        }
                                    } else {
                                        null
                                    },
                                    onClick = {
                                        if (selectedUris.isNotEmpty()) {
                                            onEvent(ManageEvent.ToggleSelection(document.uri))
                                        } else {
                                            onEvent(ManageEvent.Open(managed))
                                        }
                                    },
                                    onLongClick = { onEvent(ManageEvent.ToggleSelection(document.uri)) }
                                )
                            }
                        }
                    }
                }
            }
        } else {
            item {
                Text(
                    text = stringResource(
                        if (documents.isEmpty()) {
                            R.string.manage_no_documents
                        } else {
                            R.string.manage_no_search_results
                        }
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.fillMaxWidth().pvotReveal(3)
                )
            }
        }
    }
        PvotFab(
            icon = painterResource(R.drawable.ic_plus),
            onClick = { showNewSheet = true },
            contentDescription = stringResource(R.string.manage_new_document),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(
                    end = PvotSpacing.L,
                    bottom = PvotSpacing.L + LocalPvotNavBarSizes.current.barHeight
                )
        )
    }
}

@Composable
private fun NewDocumentSheet(
    onDismiss: () -> Unit,
    onPickType: (NewFileType) -> Unit,
    onScan: () -> Unit
) {
    PvotBottomSheet(
        visible = true,
        onDismissRequest = onDismiss,
        title = stringResource(R.string.manage_new_document)
    ) {
        NewFileType.entries.forEach { type ->
            PvotSheetItem(
                title = stringResource(type.titleRes),
                icon = {
                    Icon(
                        painterResource(R.drawable.ic_file_text),
                        contentDescription = null
                    )
                },
                onClick = { onDismiss(); onPickType(type) }
            )
        }
        PvotSheetItem(
            title = stringResource(R.string.manage_new_scan),
            icon = { Icon(painterResource(R.drawable.ic_camera), contentDescription = null) },
            onClick = { onDismiss(); onScan() }
        )
    }
}

/** Keeps every action of the list row's overflow menu; in selection mode that corner button
 * becomes the selection mark instead. */
@Composable
private fun DocumentGridCard(
    managed: ManagedDocument,
    selected: Boolean,
    inSelectionMode: Boolean,
    onClick: () -> Unit,
    onMore: () -> Unit
) {
    val document = managed.document
    PvotCard(
        onClick = onClick,
        modifier = Modifier.width(dimensionResource(R.dimen.manage_grid_card_width)),
        colors = if (selected) {
            PvotCardDefaults.colors(
                containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)
            )
        } else {
            PvotCardDefaults.colors()
        }
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(PvotSpacing.S),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                if (inSelectionMode) {
                    Icon(
                        painterResource(
                            if (selected) R.drawable.ic_check_circle_filled else R.drawable.ic_checkbox
                        ),
                        contentDescription = null,
                        tint = if (selected) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        modifier = Modifier.size(dimensionResource(R.dimen.manage_grid_check_size))
                    )
                } else {
                    PvotIconButton(
                        icon = painterResource(R.drawable.ic_more),
                        contentDescription = stringResource(
                            R.string.manage_document_actions,
                            document.name
                        ),
                        onClick = onMore,
                        size = dimensionResource(R.dimen.icon_button_compact)
                    )
                }
            }
            Icon(
                painterResource(R.drawable.ic_file_text),
                contentDescription = null,
                tint = documentKind(document.name, document.mimeType).color(),
                modifier = Modifier.size(dimensionResource(R.dimen.manage_grid_icon_size))
            )
            Text(
                text = document.name,
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = PvotSpacing.XS)
            )
            Text(
                text = formatFileSize(document.sizeBytes),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun SelectionToolbar(
    count: Int,
    onSelectAll: () -> Unit,
    onShare: () -> Unit,
    onDelete: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    PvotListGroup(modifier = modifier) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth().padding(PvotSpacing.L),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(PvotSpacing.XS)
            ) {
                Text(
                    text = pluralStringResource(R.plurals.manage_selected_count, count, count),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
                PvotIconButton(
                    icon = painterResource(R.drawable.ic_checkbox),
                    contentDescription = stringResource(R.string.manage_select_all),
                    onClick = onSelectAll,
                    size = PvotIconButtonDefaults.SmallSize
                )
                PvotIconButton(
                    icon = painterResource(R.drawable.ic_share),
                    contentDescription = stringResource(R.string.manage_share_selected),
                    onClick = onShare,
                    size = PvotIconButtonDefaults.SmallSize
                )
                PvotIconButton(
                    icon = painterResource(R.drawable.ic_trash),
                    contentDescription = stringResource(R.string.manage_delete_selected),
                    onClick = onDelete,
                    size = PvotIconButtonDefaults.SmallSize
                )
                PvotIconButton(
                    icon = painterResource(R.drawable.ic_x),
                    contentDescription = stringResource(R.string.manage_cancel_selection),
                    onClick = onCancel,
                    size = PvotIconButtonDefaults.SmallSize
                )
            }
        }
    }
}

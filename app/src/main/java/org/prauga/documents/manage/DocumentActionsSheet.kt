// SPDX-FileCopyrightText: 2026 Debayan G <debz.exe@gmail.com>
// SPDX-License-Identifier: GPL-3.0-or-later

package org.prauga.documents.manage

import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.prauga.pvot.designsystem.components.PvotBottomSheet
import com.prauga.pvot.designsystem.components.PvotSheetItem
import org.prauga.documents.R
import org.prauga.documents.library.data.DocumentEntry

@Composable
fun DocumentActionsSheet(
    document: DocumentEntry,
    onDismiss: () -> Unit,
    onShare: () -> Unit,
    onRename: () -> Unit,
    onSelect: () -> Unit,
    onDelete: () -> Unit
) {
    PvotBottomSheet(visible = true, onDismissRequest = onDismiss, title = document.name) {
        PvotSheetItem(
            title = stringResource(R.string.manage_share_document_action),
            icon = { Icon(painterResource(R.drawable.ic_share), contentDescription = null) },
            onClick = { onDismiss(); onShare() }
        )
        PvotSheetItem(
            title = stringResource(R.string.manage_rename_document_action),
            icon = { Icon(painterResource(R.drawable.ic_pencil), contentDescription = null) },
            onClick = { onDismiss(); onRename() }
        )
        PvotSheetItem(
            title = stringResource(R.string.manage_select_document_action),
            icon = { Icon(painterResource(R.drawable.ic_checkbox), contentDescription = null) },
            onClick = { onDismiss(); onSelect() }
        )
        PvotSheetItem(
            title = stringResource(R.string.manage_delete_document_action),
            icon = { Icon(painterResource(R.drawable.ic_trash), contentDescription = null) },
            destructive = true,
            onClick = { onDismiss(); onDelete() }
        )
    }
}

// SPDX-FileCopyrightText: 2026 Debayan G <debz.exe@gmail.com>
// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: GPL-3.0-or-later

package org.prauga.documents.manage

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.prauga.pvot.designsystem.components.PvotBottomSheet
import com.prauga.pvot.designsystem.components.button.PvotIconButton
import com.prauga.pvot.designsystem.components.button.PvotIconButtonDefaults
import com.prauga.pvot.designsystem.components.list.PvotListRow
import com.prauga.pvot.designsystem.theme.PvotSpacing
import org.prauga.documents.R
import org.prauga.documents.common.documentKind
import org.prauga.documents.common.formatFileSize
import org.prauga.documents.library.data.TrashedDocument

/** Restoring only works while the original folder is still added; deleting here is for good. */
@Composable
fun RecentlyDeletedSheet(
    documents: List<TrashedDocument>,
    onDismiss: () -> Unit,
    onRestore: (TrashedDocument) -> Unit,
    onDeleteForever: (TrashedDocument) -> Unit,
) {

    PvotBottomSheet(
        visible = true,
        onDismissRequest = onDismiss,
        title = stringResource(R.string.manage_recently_deleted_title)
    ) {
        if (documents.isEmpty()) {
            Text(
                text = stringResource(R.string.trash_empty),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth().padding(PvotSpacing.L)
            )
            return@PvotBottomSheet
        }
        documents.forEach { trashed ->
            val document = trashed.document
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
                    Row(horizontalArrangement = Arrangement.spacedBy(PvotSpacing.XS)) {
                        PvotIconButton(
                            icon = painterResource(R.drawable.ic_restore),
                            contentDescription = stringResource(
                                R.string.trash_restore_document,
                                document.name
                            ),
                            onClick = { onRestore(trashed) },
                            size = PvotIconButtonDefaults.SmallSize
                        )
                        PvotIconButton(
                            icon = painterResource(R.drawable.ic_x),
                            contentDescription = stringResource(
                                R.string.trash_delete_forever,
                                document.name
                            ),
                            onClick = { onDeleteForever(trashed) },
                            size = PvotIconButtonDefaults.SmallSize
                        )
                    }
                }
            )
        }
    }
}

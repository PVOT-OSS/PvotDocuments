// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: GPL-3.0-or-later

package org.prauga.documents.manage

import android.net.Uri
import org.prauga.documents.common.DocumentSort
import org.prauga.documents.library.data.ManagedDocument
import org.prauga.documents.library.data.TrashedDocument

sealed interface ManageEvent {
    data class AddFolder(val treeUri: Uri) : ManageEvent
    data class RemoveFolder(val treeUri: Uri) : ManageEvent
    data class Open(val document: ManagedDocument) : ManageEvent
    data class Share(val document: ManagedDocument) : ManageEvent
    data object ShareSelected : ManageEvent
    data class Delete(val document: ManagedDocument) : ManageEvent
    data object DeleteSelected : ManageEvent
    data class Rename(val document: ManagedDocument, val newName: String) : ManageEvent
    data class Create(val type: NewFileType, val name: String) : ManageEvent
    data class Search(val query: String) : ManageEvent
    data class Sort(val sort: DocumentSort) : ManageEvent
    data class ToggleSelection(val uri: Uri) : ManageEvent
    data class SelectOnly(val uri: Uri) : ManageEvent
    data object SelectAll : ManageEvent
    data object ClearSelection : ManageEvent
    data class Restore(val document: TrashedDocument) : ManageEvent
    data class DeleteForever(val document: TrashedDocument) : ManageEvent
}

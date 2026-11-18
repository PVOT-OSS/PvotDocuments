// SPDX-FileCopyrightText: 2026 Debayan G <debz.exe@gmail.com>
// SPDX-License-Identifier: GPL-3.0-or-later

package org.prauga.documents.library.data

import android.net.Uri

/** [document] is the app's own trash copy; restoring needs [folderTreeUri] to still be added. */
data class TrashedDocument(
    val document: DocumentEntry,
    val folderTreeUri: Uri,
    val deletedAt: Long
)

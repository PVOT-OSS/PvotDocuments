// SPDX-FileCopyrightText: 2026 Debayan G <debz.exe@gmail.com>
// SPDX-License-Identifier: GPL-3.0-or-later

package org.prauga.documents.library.data

import android.net.Uri

data class FolderEntry(
    val treeUri: Uri,
    val displayName: String
)

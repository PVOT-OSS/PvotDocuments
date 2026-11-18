// SPDX-FileCopyrightText: 2026 Debayan G <debz.exe@gmail.com>
// SPDX-License-Identifier: GPL-3.0-or-later

package org.prauga.documents.onboarding

import android.net.Uri
import android.provider.DocumentsContract

/**
 * SAF access can't be granted without the user confirming in the system picker, so these only
 * open the picker inside the folder through [DocumentsContract.EXTRA_INITIAL_URI].
 */
enum class PublicFolder(val folderName: String) {
    Documents("Documents"),
    Downloads("Download");

    val documentId: String get() = "primary:$folderName"

    val initialUri: Uri
        get() = DocumentsContract.buildDocumentUri(ExternalStorageAuthority, documentId)
}

private const val ExternalStorageAuthority = "com.android.externalstorage.documents"

/** A tree URI granted through this folder's picker ends in its [PublicFolder.documentId],
 * URL-encoded. */
fun isPublicFolderAdded(folderUris: Set<String>, folder: PublicFolder): Boolean {
    val encoded = Uri.encode(folder.documentId)
    return folderUris.any { it.endsWith(encoded) || it.endsWith(folder.documentId) }
}

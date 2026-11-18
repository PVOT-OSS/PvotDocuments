// SPDX-FileCopyrightText: 2026 Debayan G <debz.exe@gmail.com>
// SPDX-License-Identifier: GPL-3.0-or-later

package org.prauga.documents.library.data

import android.net.Uri

/** Metadata as the provider reports it; nothing here is read from the file's content. */
data class DocumentEntry(
    val uri: Uri,
    val name: String,
    val mimeType: String,
    val sizeBytes: Long,
    val lastModified: Long
)

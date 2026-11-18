// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: GPL-3.0-or-later

package org.prauga.documents.library.data

/**
 * [recentDocuments] and [trashedDocuments] are most recent first; an autosaved recent document
 * points at the app's own copy.
 */
data class Library(
    val folderUris: Set<String>,
    val recentDocuments: List<DocumentEntry>,
    val trashedDocuments: List<TrashedDocument>,
)

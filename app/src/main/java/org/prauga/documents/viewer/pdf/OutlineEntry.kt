// SPDX-FileCopyrightText: 2026 Debayan G <debz.exe@gmail.com>
// SPDX-License-Identifier: GPL-3.0-or-later

package org.prauga.documents.viewer.pdf

/** One entry of the document's outline (its bookmarks), flattened in reading order; [depth] is 0
 * for top-level entries. */
data class OutlineEntry(val title: String, val pageIndex: Int, val depth: Int)

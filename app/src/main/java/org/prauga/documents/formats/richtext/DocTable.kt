// SPDX-FileCopyrightText: 2026 Debayan G <debz.exe@gmail.com>
// SPDX-License-Identifier: GPL-3.0-or-later

package org.prauga.documents.formats.richtext

data class DocTable(val rows: List<List<DocTableCell>>) : DocBlock {
    val columnCount: Int get() = rows.maxOfOrNull { row -> row.sumOf { it.colSpan } } ?: 0
}

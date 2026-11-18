// SPDX-FileCopyrightText: 2026 Debayan G <debz.exe@gmail.com>
// SPDX-License-Identifier: GPL-3.0-or-later

package org.prauga.documents.formats.sheet

/** A rectangle of cells shown as one, inclusive at both ends. */
data class CellRange(val firstRow: Int, val firstCol: Int, val lastRow: Int, val lastCol: Int)

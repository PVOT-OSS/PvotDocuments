// SPDX-FileCopyrightText: 2026 Debayan G <debz.exe@gmail.com>
// SPDX-License-Identifier: GPL-3.0-or-later

package org.prauga.documents.formats.sheet

/** A row's cells, sparse and in column order. */
class SheetRow(val index: Int, val cells: List<SheetCell>, val heightPt: Float?)

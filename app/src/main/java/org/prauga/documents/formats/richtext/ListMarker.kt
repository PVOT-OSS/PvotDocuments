// SPDX-FileCopyrightText: 2026 Debayan G <debz.exe@gmail.com>
// SPDX-License-Identifier: GPL-3.0-or-later

package org.prauga.documents.formats.richtext

/** A list item's marker, already worked out ("•", "3.", "b)"), and how deeply it's nested,
 * from 0. */
data class ListMarker(val label: String, val level: Int)

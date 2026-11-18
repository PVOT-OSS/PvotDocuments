// SPDX-FileCopyrightText: 2026 Debayan G <debz.exe@gmail.com>
// SPDX-License-Identifier: GPL-3.0-or-later

package org.prauga.documents.formats.richtext

/** A picture stored in the document's package at [path], with its intended size in points when
 * the document gives one. */
data class DocImage(val path: String, val widthPt: Float?, val heightPt: Float?, val description: String?) : DocBlock

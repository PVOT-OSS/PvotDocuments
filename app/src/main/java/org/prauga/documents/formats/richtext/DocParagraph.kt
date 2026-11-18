// SPDX-FileCopyrightText: 2026 Debayan G <debz.exe@gmail.com>
// SPDX-License-Identifier: GPL-3.0-or-later

package org.prauga.documents.formats.richtext

/** A paragraph: [heading] is 1..6 for a heading, null for body text. [indentLevel] is extra
 * indentation, in steps, for text indented without being a list. */
data class DocParagraph(
    val runs: List<TextRun>,
    val heading: Int? = null,
    val align: ParagraphAlign = ParagraphAlign.Start,
    val list: ListMarker? = null,
    val indentLevel: Int = 0,
) : DocBlock {
    val isEmpty: Boolean get() = runs.all { it.text.isBlank() }
}

// SPDX-FileCopyrightText: 2026 Debayan G <debz.exe@gmail.com>
// SPDX-License-Identifier: GPL-3.0-or-later

package org.prauga.documents.formats.markdown

/** [text] in every variant still carries inline markers (`**bold**`, links, ...) for the renderer
 * to style. */
sealed interface MarkdownBlock {
    data class Heading(val level: Int, val text: String) : MarkdownBlock
    data class Paragraph(val text: String) : MarkdownBlock
    data class ListItem(val ordered: Boolean, val number: Int?, val text: String) : MarkdownBlock
    data class CodeBlock(val text: String) : MarkdownBlock
    data class Quote(val text: String) : MarkdownBlock
    data object Rule : MarkdownBlock
}

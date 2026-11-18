// SPDX-FileCopyrightText: 2026 Debayan G <debz.exe@gmail.com>
// SPDX-License-Identifier: GPL-3.0-or-later

package org.prauga.documents.viewer.pdf

/** A link on a page: tapping any of [rects] goes to [destPage] in this document, or opens [uri].
 * A web address written in the text can wrap onto the next line, hence more than one box. */
internal data class PageLink(val rects: List<PageRect>, val destPage: Int?, val uri: String?)

// SPDX-FileCopyrightText: 2026 Debayan G <debz.exe@gmail.com>
// SPDX-License-Identifier: GPL-3.0-or-later

package org.prauga.documents.viewer.data

/** A place in a PDF: the zero-based [page] at the top of the screen, and how far down it the screen
 * starts, as a fraction of the page's height. */
data class ReadingPosition(val page: Int, val offsetFraction: Float)

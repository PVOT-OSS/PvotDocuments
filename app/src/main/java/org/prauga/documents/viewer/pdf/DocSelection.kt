// SPDX-FileCopyrightText: 2026 Debayan G <debz.exe@gmail.com>
// SPDX-License-Identifier: GPL-3.0-or-later

package org.prauga.documents.viewer.pdf

/** A selection that can run across pages. [anchor] is where it started and [focus] where it was
 * dragged to, in either order; [start] and [end] are the two in reading order, both inclusive. */
internal data class DocSelection(val anchor: TextPos, val focus: TextPos) {
    val start: TextPos get() = minOf(anchor, focus)
    val end: TextPos get() = maxOf(anchor, focus)

    /** The selected characters on [page], whose last character index is [lastChar]; null when the
     * selection doesn't reach that page. */
    fun rangeOn(page: Int, lastChar: Int): IntRange? {
        if (page < start.page || page > end.page || lastChar < 0) return null
        val first = if (page == start.page) start.char else 0
        val last = if (page == end.page) end.char else lastChar
        return if (first <= last) first..last else null
    }
}

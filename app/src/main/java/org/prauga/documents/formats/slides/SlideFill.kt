// SPDX-FileCopyrightText: 2026 Debayan G <debz.exe@gmail.com>
// SPDX-License-Identifier: GPL-3.0-or-later

package org.prauga.documents.formats.slides

/** How a shape is filled: a solid ARGB color, a picture stretched over it, or nothing. */
sealed interface SlideFill {
    data class Solid(val color: Int) : SlideFill
    data class Picture(val path: String) : SlideFill
}

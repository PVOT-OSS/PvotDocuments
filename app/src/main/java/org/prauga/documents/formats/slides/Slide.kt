// SPDX-FileCopyrightText: 2026 Debayan G <debz.exe@gmail.com>
// SPDX-License-Identifier: GPL-3.0-or-later

package org.prauga.documents.formats.slides

/** One slide: what's behind everything, then its shapes, those from its master and layout
 * included, in drawing order. */
data class Slide(val background: SlideFill?, val shapes: List<SlideShape>)

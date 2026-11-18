// SPDX-FileCopyrightText: 2026 Debayan G <debz.exe@gmail.com>
// SPDX-License-Identifier: GPL-3.0-or-later

package org.prauga.documents.formats.slides

/** The outline a shape draws: the preset shapes slides mostly use. Anything else is drawn as
 * its bounding rectangle. */
enum class SlideGeometry { Rectangle, RoundRectangle, Ellipse, Line, Triangle, RightArrow, Chevron, Diamond }

// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: GPL-3.0-or-later

package org.prauga.documents.common

import android.net.Uri

/** A document ready for the viewer: a URI it may read, the title to show and how to render it. */
data class ViewerTarget(val uri: Uri, val name: String, val format: ViewerFormat)

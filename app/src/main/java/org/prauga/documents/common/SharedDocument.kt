// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: GPL-3.0-or-later

package org.prauga.documents.common

import android.net.Uri

/** A document to put in a share intent, under a URI another app may be granted. */
data class SharedDocument(val uri: Uri, val mimeType: String, val name: String)

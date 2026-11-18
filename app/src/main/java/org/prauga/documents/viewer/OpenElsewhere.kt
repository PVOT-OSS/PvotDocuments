// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: GPL-3.0-or-later

package org.prauga.documents.viewer

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import org.prauga.documents.MainActivity

/** Offers [uri] to the other apps that can view it; this app takes every type, so it leaves itself out. */
internal fun openElsewhere(context: Context, uri: Uri, mimeType: String, chooserTitle: String) {
    val view = Intent(Intent.ACTION_VIEW)
        .setDataAndType(uri, mimeType.ifEmpty { "*/*" })
        .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    val chooser = Intent.createChooser(view, chooserTitle)
        .putExtra(Intent.EXTRA_EXCLUDE_COMPONENTS, arrayOf(ComponentName(context, MainActivity::class.java)))
    context.startActivity(chooser)
}

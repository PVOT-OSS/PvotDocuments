// SPDX-FileCopyrightText: 2026 Debayan G <debz.exe@gmail.com>
// SPDX-License-Identifier: GPL-3.0-or-later

package org.prauga.documents.viewer

import android.content.Context
import android.content.Intent
import android.os.Bundle
import com.prauga.pvot.designsystem.platform.PvotBaseActivity
import com.prauga.pvot.designsystem.theme.PvotAppTheme
import dagger.hilt.android.AndroidEntryPoint
import org.prauga.documents.common.ViewerTarget

/** Launched only through [start], with a URI, name and format the caller has already resolved. */
@AndroidEntryPoint
class DocumentViewerActivity : PvotBaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setPvotContent {
            PvotAppTheme {
                DocumentViewerScreen(onBack = { finish() })
            }
        }
    }

    companion object {
        internal const val ExtraUri = "uri"
        internal const val ExtraName = "name"
        internal const val ExtraFormat = "format"

        fun start(context: Context, target: ViewerTarget) {
            context.startActivity(
                Intent(context, DocumentViewerActivity::class.java).apply {
                    putExtra(ExtraUri, target.uri.toString())
                    putExtra(ExtraName, target.name)
                    putExtra(ExtraFormat, target.format.name)
                }
            )
        }
    }
}

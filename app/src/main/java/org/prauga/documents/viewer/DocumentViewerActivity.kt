package org.prauga.documents.viewer

import android.content.Context
import android.content.Intent
import android.os.Bundle
import com.prauga.pvot.designsystem.platform.PvotBaseActivity
import com.prauga.pvot.designsystem.theme.PvotAppTheme
import dagger.hilt.android.AndroidEntryPoint
import org.prauga.documents.common.ViewerTarget

/**
 * Hosts one document's viewer, full screen. [start] is the only way in: it carries the URI, name
 * and format the caller already resolved — this activity renders, it doesn't decide what it's
 * able to open.
 */
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

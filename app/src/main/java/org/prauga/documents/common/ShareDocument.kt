package org.prauga.documents.common

import android.content.Context
import android.content.Intent

fun shareDocuments(context: Context, documents: List<SharedDocument>) {
    val single = documents.singleOrNull()
    val intent = if (single != null) {
        Intent(Intent.ACTION_SEND).apply {
            type = single.mimeType
            putExtra(Intent.EXTRA_STREAM, single.uri)
        }
    } else {
        // A mixed selection has no single mime type; "*/*" lets any app that takes multiple files
        // offer itself, rather than narrowing to the first document's type.
        Intent(Intent.ACTION_SEND_MULTIPLE).apply {
            type = documents.map { it.mimeType }.distinct().singleOrNull() ?: "*/*"
            putParcelableArrayListExtra(Intent.EXTRA_STREAM, ArrayList(documents.map { it.uri }))
        }
    }
    intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    if (documents.isNotEmpty()) context.startActivity(Intent.createChooser(intent, single?.name))
}

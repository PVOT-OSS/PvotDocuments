package org.prauga.documents.library.data

import android.content.Context
import java.io.File

internal fun isOwnFile(context: Context, file: File): Boolean {
    val root = context.filesDir.canonicalPath + File.separator
    return runCatching { file.canonicalPath.startsWith(root) }.getOrDefault(false)
}

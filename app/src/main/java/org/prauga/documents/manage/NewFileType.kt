package org.prauga.documents.manage

import androidx.annotation.StringRes
import org.prauga.documents.R

/** A plain-text format the "+" can create empty; Office formats need a writer first. */
enum class NewFileType(val extension: String, val mimeType: String, @StringRes val titleRes: Int) {
    Text("txt", "text/plain", R.string.manage_new_text),
    Markdown("md", "text/markdown", R.string.manage_new_markdown),
    Csv("csv", "text/csv", R.string.manage_new_csv),
}

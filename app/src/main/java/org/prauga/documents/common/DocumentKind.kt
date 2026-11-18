// SPDX-FileCopyrightText: 2026 Debayan G <debz.exe@gmail.com>
// SPDX-License-Identifier: GPL-3.0-or-later

package org.prauga.documents.common

import androidx.annotation.ColorRes
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import org.prauga.documents.R
import org.prauga.documents.formats.slides.Presentation

/**
 * [color] is a fixed per-type color, not a `MaterialTheme.colorScheme` role, so a PDF reads red in
 * any theme; the theme has no role for a file type. [Other] uses the theme's primary instead.
 */
enum class DocumentKind(@param:ColorRes private val colorRes: Int?) {
    Pdf(R.color.document_kind_pdf),
    Word(R.color.document_kind_word),
    Spreadsheet(R.color.document_kind_spreadsheet),
    Presentation(R.color.document_kind_presentation),
    Html(R.color.document_kind_html),
    Book(R.color.document_kind_book),
    PlainText(R.color.document_kind_text),
    Other(null);

    @Composable
    fun color(): Color = colorRes?.let { colorResource(it) } ?: MaterialTheme.colorScheme.primary

    val labelRes: Int
        get() = when (this) {
            Pdf -> R.string.document_kind_pdf
            Word -> R.string.document_kind_word
            Spreadsheet -> R.string.document_kind_spreadsheet
            Presentation -> R.string.document_kind_presentation
            Html -> R.string.document_kind_html
            Book -> R.string.document_kind_book
            PlainText -> R.string.document_kind_text
            Other -> R.string.document_kind_other
        }
}

/** Extension first: a provider sometimes reports [mimeType] as `application/octet-stream`. */
fun documentKind(name: String, mimeType: String): DocumentKind =
    when (name.substringAfterLast('.', missingDelimiterValue = "").lowercase()) {
        "pdf" -> DocumentKind.Pdf
        "doc", "docx", "odt" -> DocumentKind.Word
        "xls", "xlsx", "xlsm", "ods", "csv" -> DocumentKind.Spreadsheet
        "ppt", "pptx", "odp" -> DocumentKind.Presentation
        "html", "htm" -> DocumentKind.Html
        "epub", "mobi", "azw3", "fb2" -> DocumentKind.Book
        "txt", "md", "markdown" -> DocumentKind.PlainText
        else -> mimeTypeKind(mimeType)
    }

private fun mimeTypeKind(mimeType: String): DocumentKind = when {
    mimeType == "application/pdf" -> DocumentKind.Pdf
    mimeType.contains("wordprocessingml") || mimeType == "application/msword" ||
        mimeType == "application/vnd.oasis.opendocument.text" -> DocumentKind.Word
    mimeType.contains("spreadsheetml") || mimeType == "application/vnd.oasis.opendocument.spreadsheet" ||
        mimeType == "text/csv" ||
        mimeType == "application/csv" -> DocumentKind.Spreadsheet
    mimeType.contains("presentationml") || mimeType == "application/vnd.ms-powerpoint" ||
        mimeType == "application/vnd.oasis.opendocument.presentation" -> DocumentKind.Presentation
    mimeType == "text/html" -> DocumentKind.Html
    mimeType == "application/epub+zip" -> DocumentKind.Book
    mimeType.startsWith("text/") -> DocumentKind.PlainText
    else -> DocumentKind.Other
}

package org.prauga.documents.common

import androidx.annotation.ColorRes
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import org.prauga.documents.R
import org.prauga.documents.formats.slides.Presentation

/**
 * The file type a document's icon is tinted by, same idea as the colored file badges in Google
 * Drive, Office and the system share sheet: a PDF reads red, a spreadsheet green, regardless of
 * app theme or dark/light mode. [color] is a fixed brand color, not a `MaterialTheme.colorScheme`
 * role — PvotLib's "tokens, not literals" rule is for UI chrome, and has no role for a file type;
 * [PvotExtendedColors][com.prauga.pvot.designsystem.theme.PvotExtendedColors] only covers success
 * and warning. [Other] is the one exception, reading the theme's primary so an unrecognized type
 * still looks like the rest of the app rather than carrying its own made-up color.
 */
enum class DocumentKind(@param:ColorRes private val colorRes: Int?) {
    Pdf(R.color.document_kind_pdf),
    Word(R.color.document_kind_word),
    Spreadsheet(R.color.document_kind_spreadsheet),
    Presentation(R.color.document_kind_presentation),
    Html(R.color.document_kind_html),
    PlainText(R.color.document_kind_text),
    Other(null);

    @Composable
    fun color(): Color = colorRes?.let { colorResource(it) } ?: MaterialTheme.colorScheme.primary

    /** The label a filter chip shows for this kind, such as on the Recent tab. */
    val labelRes: Int
        get() = when (this) {
            Pdf -> R.string.document_kind_pdf
            Word -> R.string.document_kind_word
            Spreadsheet -> R.string.document_kind_spreadsheet
            Presentation -> R.string.document_kind_presentation
            Html -> R.string.document_kind_html
            PlainText -> R.string.document_kind_text
            Other -> R.string.document_kind_other
        }
}

/** [DocumentKind] for a document, read from its file extension — more reliable than [mimeType],
 * which a provider sometimes reports as the generic `application/octet-stream`. */
fun documentKind(name: String, mimeType: String): DocumentKind =
    when (name.substringAfterLast('.', missingDelimiterValue = "").lowercase()) {
        "pdf" -> DocumentKind.Pdf
        "doc", "docx", "odt" -> DocumentKind.Word
        "xls", "xlsx", "xlsm", "ods", "csv" -> DocumentKind.Spreadsheet
        "ppt", "pptx", "odp" -> DocumentKind.Presentation
        "html", "htm" -> DocumentKind.Html
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
    mimeType.startsWith("text/") -> DocumentKind.PlainText
    else -> DocumentKind.Other
}

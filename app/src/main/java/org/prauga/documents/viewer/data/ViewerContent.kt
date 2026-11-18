package org.prauga.documents.viewer.data

import org.prauga.documents.formats.richtext.RichDocument
import org.prauga.documents.formats.sheet.Workbook
import org.prauga.documents.formats.slides.Presentation
import org.prauga.documents.viewer.pdf.PdfiumDocument

sealed interface ViewerContent {
    data class Text(val text: String, val monospace: Boolean) : ViewerContent
    data class Csv(val rows: List<List<String>>) : ViewerContent
    data class Markdown(val text: String) : ViewerContent
    data class Html(val html: String) : ViewerContent
    data class Spreadsheet(val workbook: Workbook) : ViewerContent
    data class Slides(val presentation: Presentation) : ViewerContent
    data class RichText(val document: RichDocument) : ViewerContent
    data class Pdf internal constructor(internal val document: PdfiumDocument) : ViewerContent
}

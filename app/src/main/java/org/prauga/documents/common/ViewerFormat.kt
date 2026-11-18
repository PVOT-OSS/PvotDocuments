// SPDX-FileCopyrightText: 2026 Debayan G <debz.exe@gmail.com>
// SPDX-License-Identifier: GPL-3.0-or-later

package org.prauga.documents.common

enum class ViewerFormat {
    PlainText,
    Code,
    Json,
    Csv,
    Markdown,
    Html,
    Pdf,
    Docx,
    Odt,
    Spreadsheet,
    Slides,
    Binary,
}

private val CodeExtensions = setOf(
    "kt", "kts", "java", "py", "js", "ts", "jsx", "tsx", "c", "cpp", "h", "hpp", "cs", "go", "rs",
    "rb", "php", "swift", "sh", "bash", "yml", "yaml", "toml", "xml", "gradle", "properties", "ini",
    "css", "scss", "sql"
)

/** Extension first, MIME type as a fallback, for the same reason as [documentKind]. */
fun viewerFormat(name: String, mimeType: String): ViewerFormat {
    val extension = name.substringAfterLast('.', missingDelimiterValue = "").lowercase()
    return when {
        extension == "txt" -> ViewerFormat.PlainText
        extension == "json" -> ViewerFormat.Json
        extension == "csv" -> ViewerFormat.Csv
        extension == "md" || extension == "markdown" -> ViewerFormat.Markdown
        extension == "html" || extension == "htm" -> ViewerFormat.Html
        extension == "pdf" -> ViewerFormat.Pdf
        extension == "docx" -> ViewerFormat.Docx
        extension == "odt" -> ViewerFormat.Odt
        extension == "pptx" || extension == "odp" -> ViewerFormat.Slides
        extension == "xlsx" || extension == "xlsm" || extension == "ods" -> ViewerFormat.Spreadsheet
        extension in CodeExtensions -> ViewerFormat.Code
        mimeType == "application/json" -> ViewerFormat.Json
        mimeType == "text/csv" || mimeType == "application/csv" -> ViewerFormat.Csv
        mimeType == "text/markdown" -> ViewerFormat.Markdown
        mimeType == "text/html" -> ViewerFormat.Html
        mimeType == "application/pdf" -> ViewerFormat.Pdf
        mimeType.contains("wordprocessingml") -> ViewerFormat.Docx
        mimeType == "application/vnd.oasis.opendocument.text" -> ViewerFormat.Odt
        mimeType.contains("presentationml.presentation") ||
            mimeType == "application/vnd.oasis.opendocument.presentation" -> ViewerFormat.Slides
        mimeType.contains("spreadsheetml") || mimeType.equals("application/vnd.ms-excel.sheet.macroEnabled.12", ignoreCase = true) ||
            mimeType == "application/vnd.oasis.opendocument.spreadsheet" -> ViewerFormat.Spreadsheet
        mimeType.startsWith("text/") -> ViewerFormat.PlainText
        else -> ViewerFormat.Binary
    }
}

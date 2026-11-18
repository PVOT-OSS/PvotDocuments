// SPDX-FileCopyrightText: 2026 Debayan G <debz.exe@gmail.com>
// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: GPL-3.0-or-later

package org.prauga.documents.viewer.data

import android.content.Context
import android.net.Uri
import android.os.ParcelFileDescriptor
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import org.prauga.documents.common.ViewerFormat
import org.prauga.documents.common.coroutines.IoDispatcher
import org.prauga.documents.formats.csv.parseCsv
import org.prauga.documents.formats.richtext.parseDocx
import org.prauga.documents.formats.richtext.parseOdt
import org.prauga.documents.formats.sheet.parseOds
import org.prauga.documents.formats.sheet.parseXlsx
import org.prauga.documents.formats.slides.parseOdp
import org.prauga.documents.formats.slides.parsePptx
import org.prauga.documents.library.data.DocumentsRepository
import org.prauga.documents.viewer.pdf.PdfiumDocument
import timber.log.Timber
import java.io.File
import java.io.FileNotFoundException
import java.nio.ByteBuffer
import java.nio.charset.CodingErrorAction
import java.util.zip.ZipFile
import javax.inject.Inject

/**
 * Reads a document as [ViewerFormat] for its viewer. Zipped formats are read out of order, so
 * the document is copied to a private temporary file first; each format is recognized from the
 * zip's contents rather than the name, which a sender may have got wrong.
 */
class DocumentLoader @Inject constructor(
    @ApplicationContext private val context: Context,
    @IoDispatcher private val io: CoroutineDispatcher,
    private val documentsRepository: DocumentsRepository,
) {
    /** Throws [org.prauga.documents.viewer.pdf.PdfPasswordRequiredException] for an encrypted
     * PDF opened without, or with the wrong, [password]. */
    suspend fun load(uri: Uri, format: ViewerFormat, password: String?): ViewerContent = when (format) {
        ViewerFormat.PlainText -> ViewerContent.Text(documentsRepository.readText(uri), monospace = false)
        ViewerFormat.Code -> ViewerContent.Text(documentsRepository.readText(uri), monospace = true)
        ViewerFormat.Json -> ViewerContent.Text(prettyPrintJson(documentsRepository.readText(uri)), monospace = true)
        ViewerFormat.Csv -> ViewerContent.Csv(parseCsv(documentsRepository.readText(uri)))
        ViewerFormat.Markdown -> ViewerContent.Markdown(documentsRepository.readText(uri))
        ViewerFormat.Html -> ViewerContent.Html(documentsRepository.readText(uri))
        ViewerFormat.Pdf -> ViewerContent.Pdf(PdfiumDocument.open(context, uri, password))
        ViewerFormat.Spreadsheet -> ViewerContent.Spreadsheet(withContext(io) { readWorkbook(uri) })
        ViewerFormat.Slides -> ViewerContent.Slides(withContext(io) { readPresentation(uri) })
        ViewerFormat.Docx, ViewerFormat.Odt -> ViewerContent.RichText(withContext(io) { readRichDocument(uri) })
        ViewerFormat.Binary -> readUnknown(uri)
    }

    /** Text when the file reads cleanly as UTF-8 and isn't too large to lay out; its bytes otherwise. */
    private suspend fun readUnknown(uri: Uri): ViewerContent {
        val source = withContext(io) { openBytes(uri) }
        if (source.size in 0..MaxTextBytes && looksLikeText(source.read(0, TextSniffBytes))) {
            source.close()
            return ViewerContent.Text(documentsRepository.readText(uri), monospace = false)
        }
        return ViewerContent.Bytes(source, documentsRepository.mimeType(uri))
    }

    // A provider that streams (a pipe) reports no size and can't seek, so it gets a private copy.
    private fun openBytes(uri: Uri): ByteSource {
        val descriptor = context.contentResolver.openFileDescriptor(uri, "r") ?: throw FileNotFoundException()
        if (descriptor.statSize >= 0) return ByteSource(descriptor, io)
        descriptor.close()
        val copy = copyToTemp(uri, "bytes")
        return ByteSource(ParcelFileDescriptor.open(copy, ParcelFileDescriptor.MODE_READ_ONLY), io, copy)
    }

    private fun copyToTemp(uri: Uri, prefix: String): File {
        val temp = File.createTempFile(prefix, ".zip", context.cacheDir)
        var copied = false
        try {
            val input = context.contentResolver.openInputStream(uri) ?: throw FileNotFoundException()
            input.use { temp.outputStream().use { output -> it.copyTo(output) } }
            copied = true
            return temp
        } finally {
            if (!copied) temp.delete()
        }
    }

    private fun readWorkbook(uri: Uri) = copyToTemp(uri, "sheet").let { temp ->
        try {
            val isOds = ZipFile(temp).use { it.getEntry("content.xml") != null && it.getEntry("xl/workbook.xml") == null }
            if (isOds) parseOds(temp) else parseXlsx(temp)
        } finally {
            temp.delete()
        }
    }

    // The presentation and the document keep their copy open to read pictures as they come into
    // view, and delete it when closed.
    private fun readPresentation(uri: Uri) = copyToTemp(uri, "slides").let { temp ->
        parseOrDelete(temp) {
            val isPptx = ZipFile(temp).use { it.getEntry("ppt/presentation.xml") != null }
            if (isPptx) parsePptx(temp) else parseOdp(temp)
        }
    }

    private fun readRichDocument(uri: Uri) = copyToTemp(uri, "document").let { temp ->
        parseOrDelete(temp) {
            val isWord = ZipFile(temp).use { it.getEntry("word/document.xml") != null }
            if (isWord) parseDocx(temp) else parseOdt(temp)
        }
    }

    private fun <T : Any> parseOrDelete(temp: File, parse: () -> T): T {
        var parsed: T? = null
        try {
            parsed = parse()
            return parsed
        } finally {
            if (parsed == null) temp.delete()
        }
    }

    /** [text] reindented if it parses as JSON, unchanged otherwise: a malformed file still opens. */
    private fun prettyPrintJson(text: String): String {
        val trimmed = text.trim()
        return runCatching {
            when {
                trimmed.startsWith("{") -> JSONObject(trimmed).toString(2)
                trimmed.startsWith("[") -> JSONArray(trimmed).toString(2)
                else -> text
            }
        }.onFailure { Timber.d("Showing JSON as written: %s", it.javaClass.name) }.getOrDefault(text)
    }

    private companion object {
        const val TextSniffBytes = 64 * 1024
        const val MaxTextBytes = 8L * 1024 * 1024

        // No NUL byte and valid UTF-8, allowing a character cut off at the end of the sample.
        fun looksLikeText(sample: ByteArray): Boolean {
            if (sample.any { it == 0.toByte() }) return false
            for (cut in 0..3) {
                if (cut > sample.size) break
                val decoder = Charsets.UTF_8.newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                val decoded = runCatching { decoder.decode(ByteBuffer.wrap(sample, 0, sample.size - cut)) }
                if (decoded.isSuccess) return true
            }
            return false
        }
    }
}

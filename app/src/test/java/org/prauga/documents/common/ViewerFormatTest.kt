package org.prauga.documents.common

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ViewerFormatTest {

    @Test
    fun prefersTheExtensionOverAGenericMimeType() {
        assertEquals(ViewerFormat.Pdf, viewerFormat("Report.PDF", "application/octet-stream"))
        assertEquals(ViewerFormat.Spreadsheet, viewerFormat("budget.xlsm", "application/octet-stream"))
        assertEquals(ViewerFormat.Slides, viewerFormat("deck.odp", ""))
        assertEquals(ViewerFormat.Code, viewerFormat("Main.kt", "text/plain"))
    }

    @Test
    fun fallsBackToTheMimeType() {
        assertEquals(ViewerFormat.Docx, viewerFormat("download", "application/vnd.openxmlformats-officedocument.wordprocessingml.document"))
        assertEquals(ViewerFormat.Csv, viewerFormat("export", "application/csv"))
        assertEquals(ViewerFormat.PlainText, viewerFormat("notes", "text/x-unknown"))
    }

    @Test
    fun hasNoViewerForUnknownBinaries() {
        assertNull(viewerFormat("archive.zip", "application/zip"))
    }

    @Test
    fun tellsKindsApartTheSameWay() {
        assertEquals(DocumentKind.Pdf, documentKind("a.pdf", "application/octet-stream"))
        assertEquals(DocumentKind.Spreadsheet, documentKind("a.csv", ""))
        assertEquals(DocumentKind.Word, documentKind("a", "application/msword"))
        assertEquals(DocumentKind.Other, documentKind("a.bin", "application/octet-stream"))
    }
}

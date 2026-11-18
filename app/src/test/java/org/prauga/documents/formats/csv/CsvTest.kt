package org.prauga.documents.formats.csv

import org.junit.Assert.assertEquals
import org.junit.Test

class CsvTest {

    @Test
    fun splitsRowsAndFields() {
        assertEquals(listOf(listOf("a", "b"), listOf("1", "2")), parseCsv("a,b\n1,2"))
    }

    @Test
    fun keepsCommasNewlinesAndDoubledQuotesInsideQuotes() {
        assertEquals(
            listOf(listOf("x, y", "line\nbreak", "say \"hi\"")),
            parseCsv("\"x, y\",\"line\nbreak\",\"say \"\"hi\"\"\"")
        )
    }

    @Test
    fun ignoresCarriageReturnsAndATrailingNewline() {
        assertEquals(listOf(listOf("a", "b"), listOf("c", "d")), parseCsv("a,b\r\nc,d\r\n"))
    }

    @Test
    fun keepsEmptyFields() {
        assertEquals(listOf(listOf("", "b", "")), parseCsv(",b,"))
    }

    @Test
    fun readsNothingFromEmptyText() {
        assertEquals(emptyList<List<String>>(), parseCsv(""))
    }
}

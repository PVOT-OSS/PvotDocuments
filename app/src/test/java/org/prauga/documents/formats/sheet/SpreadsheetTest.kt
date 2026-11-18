package org.prauga.documents.formats.sheet

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SpreadsheetTest {

    @Test
    fun namesColumnsLikeASpreadsheet() {
        assertEquals(listOf("A", "Z", "AA", "AZ", "BA", "ZZ", "AAA"), listOf(0, 25, 26, 51, 52, 701, 702).map(::columnName))
    }

    @Test
    fun readsColumnAndRowFromAReference() {
        assertEquals(27, columnOf("AB12"))
        assertEquals(11, rowOf("AB12"))
        assertEquals(0, columnOf("a1"))
        assertEquals(-1, columnOf("12"))
        assertEquals(-1, rowOf("AB"))
    }

    @Test
    fun readsRangesInEitherOrderAndWithAbsoluteMarks() {
        assertEquals(CellRange(1, 1, 4, 2), parseRange("B2:C5"))
        assertEquals(CellRange(1, 1, 4, 2), parseRange("C5:B2"))
        assertEquals(CellRange(0, 0, 2, 3), parseRange("\$A\$1:\$D\$3"))
    }

    @Test
    fun refusesWhatIsNotARange() {
        assertNull(parseRange("A1"))
        assertNull(parseRange("A1:B2:C3"))
        assertNull(parseRange("A:B"))
    }
}

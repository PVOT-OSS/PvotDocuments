package org.prauga.documents.formats.sheet

/** A rectangle of cells shown as one, inclusive at both ends. */
data class CellRange(val firstRow: Int, val firstCol: Int, val lastRow: Int, val lastCol: Int)

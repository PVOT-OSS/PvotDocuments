package org.prauga.documents.formats.sheet

/** A row's cells, sparse and in column order. */
class SheetRow(val index: Int, val cells: List<SheetCell>, val heightPt: Float?)

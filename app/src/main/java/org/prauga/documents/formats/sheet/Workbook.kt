package org.prauga.documents.formats.sheet

/** A whole spreadsheet. [truncated] when it was too large to read in full, in which case only its
 * first [MaxCells] cells are shown. */
class Workbook(val sheets: List<Sheet>, val styles: List<CellStyle>, val truncated: Boolean)

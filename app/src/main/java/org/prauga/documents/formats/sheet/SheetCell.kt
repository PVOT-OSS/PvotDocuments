package org.prauga.documents.formats.sheet

/** One cell with something to show. [text] is what the spreadsheet app would display (numbers
 * already formatted); [numeric] decides the default alignment; [style] indexes
 * [Workbook.styles]. */
class SheetCell(val col: Int, val text: String, val style: Int, val numeric: Boolean)

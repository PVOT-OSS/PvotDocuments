package org.prauga.documents.viewer.sheet

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.calculateCentroid
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.rememberScrollableState
import androidx.compose.foundation.gestures.scrollable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import com.prauga.pvot.coreui.PvotEmptyContent
import com.prauga.pvot.designsystem.components.PvotTabRow
import com.prauga.pvot.designsystem.theme.PvotSpacing
import kotlinx.coroutines.launch
import org.prauga.documents.R
import org.prauga.documents.formats.sheet.CellAlign
import org.prauga.documents.formats.sheet.CellRange
import org.prauga.documents.formats.sheet.CellStyle
import org.prauga.documents.formats.sheet.Sheet
import org.prauga.documents.formats.sheet.SheetCell
import org.prauga.documents.formats.sheet.SheetRow
import org.prauga.documents.formats.sheet.Workbook
import org.prauga.documents.formats.sheet.columnName
import org.prauga.documents.viewer.DocumentColors
import kotlin.math.max

@Composable
fun CsvSheetView(rows: List<List<String>>, topPadding: Dp, modifier: Modifier = Modifier) {
    val workbook = remember(rows) { csvWorkbook(rows) }
    SpreadsheetView(workbook, topPadding, modifier)
}

private fun csvWorkbook(rows: List<List<String>>): Workbook {
    val header = CellStyle(bold = true)
    val sheetRows = rows.mapIndexed { r, cells ->
        SheetRow(
            r,
            cells.mapIndexedNotNull { c, text ->
                if (text.isEmpty()) null else SheetCell(c, text, if (r == 0) 1 else 0, text.toDoubleOrNull() != null)
            },
            null,
        )
    }
    val colCount = rows.maxOfOrNull { it.size } ?: 0
    val sheet = Sheet(
        name = "",
        rows = sheetRows.filter { it.cells.isNotEmpty() },
        rowCount = rows.size,
        colCount = colCount,
        colWidths = emptyMap(),
        defaultColWidth = CsvColumnWidthChars,
        hiddenRows = emptySet(),
        merges = emptyList(),
    )
    return Workbook(listOf(sheet), listOf(CellStyle.Default, header), truncated = false)
}

/** CSV has no widths of its own; a little wider than Excel's default suits typical text columns. */
private const val CsvColumnWidthChars = 14f

@Composable
fun SpreadsheetView(workbook: Workbook, topPadding: Dp, modifier: Modifier = Modifier) {
    var selected by remember(workbook) { mutableIntStateOf(0) }
    Column(modifier.fillMaxSize().padding(top = topPadding)) {
        if (workbook.sheets.size > 1) {
            PvotTabRow(
                tabs = workbook.sheets.map { it.name },
                selectedIndex = selected,
                onSelect = { selected = it },
                scrollable = true,
                modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.background),
            )
        }
        val sheet = workbook.sheets.getOrNull(selected)
        if (sheet == null || sheet.rowCount == 0) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                PvotEmptyContent(message = stringResource(R.string.viewer_sheet_empty))
            }
        } else {
            // Keyed on the sheet, so each tab starts at its own top-left.
            key(selected) {
                SheetGrid(sheet, workbook.styles, Modifier.fillMaxSize())
            }
        }
    }
}

/** Sizes at zoom 1, in dp, from the sheet's own units: Excel's widths count characters of the
 * default font, about 7px each plus 5px of padding, and heights are in points. */
private const val PxPerChar = 7f
private const val ColumnPadding = 5f
private val MinRowHeight: Dp
    @Composable get() = dimensionResource(R.dimen.sheet_min_row_height)
private val HeaderHeight: Dp
    @Composable get() = dimensionResource(R.dimen.sheet_header_height)
private val RowHeaderWidth: Dp
    @Composable get() = dimensionResource(R.dimen.sheet_row_header_width)
private const val MinZoom = 0.5f
private const val MaxZoom = 3f

@Composable
private fun SheetGrid(sheet: Sheet, styles: List<CellStyle>, modifier: Modifier) {
    val density = LocalDensity.current
    var zoom by remember(sheet) { mutableFloatStateOf(1f) }
    var scrollX by remember(sheet) { mutableFloatStateOf(0f) }
    var viewport by remember { mutableStateOf(IntSize.Zero) }
    val listState = rememberLazyListState()
    val measurer = rememberTextMeasurer(cacheSize = TextCacheSize)
    val scope = rememberCoroutineScope()

    // Column edges in px at the current zoom; edges[c] is column c's left edge after the row header.
    // A few empty columns follow the used ones, as in a spreadsheet, for text to spill into.
    val columns = sheet.colCount + TrailingColumns
    val edges = remember(sheet, zoom, density) {
        FloatArray(columns + 1).also { e ->
            for (c in 0 until columns) {
                val chars = sheet.colWidth(c)
                val widthDp = if (chars <= 0f) 0f else chars * PxPerChar + ColumnPadding
                e[c + 1] = e[c] + with(density) { widthDp.dp.toPx() } * zoom
            }
        }
    }
    val rowHeaderPx = with(density) { RowHeaderWidth.toPx() } * zoom.coerceAtLeast(1f)
    val headerPx = with(density) { HeaderHeight.toPx() } * zoom.coerceAtLeast(1f)
    val maxScrollX = (edges.last() - (viewport.width - rowHeaderPx)).coerceAtLeast(0f)
    if (scrollX > maxScrollX) scrollX = maxScrollX

    val horizontal = rememberScrollableState { delta ->
        val next = (scrollX - delta).coerceIn(0f, maxScrollX)
        val consumed = scrollX - next
        scrollX = next
        consumed
    }

    val merges = remember(sheet) { MergeIndex(sheet.merges) }
    val look = SheetLook(
        background = MaterialTheme.colorScheme.background,
        text = MaterialTheme.colorScheme.onSurface,
        headerBackground = MaterialTheme.colorScheme.surfaceContainer,
        headerText = MaterialTheme.colorScheme.onSurfaceVariant,
        grid = MaterialTheme.colorScheme.outlineVariant,
        ink = DocumentColors.ink,
        paper = DocumentColors.paper,
        textSize = MaterialTheme.typography.bodySmall.fontSize,
        cellPadding = dimensionResource(R.dimen.sheet_cell_padding),
    )
    val sheetDescription = stringResource(R.string.viewer_sheet_description, sheet.rowCount, sheet.colCount)

    Box(
        modifier
            .clipToBounds()
            .onSizeChanged { viewport = it }
            .semantics { contentDescription = sheetDescription }
            // Pinch zooms the whole grid around the fingers; one finger is left to scrolling.
            .pointerInput(sheet) {
                awaitEachGesture {
                    do {
                        val event = awaitPointerEvent()
                        if (event.changes.count { it.pressed } >= 2) {
                            val factor = event.calculateZoom()
                            if (factor != 1f) {
                                val old = zoom
                                val new = (old * factor).coerceIn(MinZoom, MaxZoom)
                                val focusX = event.calculateCentroid().x - rowHeaderPx
                                scrollX = ((scrollX + focusX) * (new / old) - focusX).coerceAtLeast(0f)
                                zoom = new
                                // Keep roughly the same rows in view as they grow or shrink.
                                val first = listState.firstVisibleItemIndex
                                val offset = (listState.firstVisibleItemScrollOffset * (new / old)).toInt()
                                scope.launch { listState.scrollToItem(first, offset) }
                            }
                            event.changes.forEach { it.consume() }
                        }
                    } while (event.changes.any { it.pressed })
                }
            }
            .scrollable(horizontal, Orientation.Horizontal),
    ) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(top = with(density) { headerPx.toDp() }, bottom = PvotSpacing.L),
        ) {
            items(count = sheet.rowCount, key = { it }) { r ->
                if (r in sheet.hiddenRows) {
                    Spacer(Modifier.height(Dp.Hairline))
                } else {
                    val heightPx = max(
                        with(density) { MinRowHeight.toPx() },
                        sheet.row(r)?.heightPt?.let { with(density) { (it * PtToDp).dp.toPx() } } ?: 0f,
                    ) * zoom
                    Canvas(Modifier.fillMaxWidth().height(with(density) { heightPx.toDp() })) {
                        drawSheetRow(sheet, r, styles, edges, scrollX, rowHeaderPx, zoom, measurer, look, merges)
                    }
                }
            }
        }
        Canvas(Modifier.fillMaxWidth().height(with(density) { headerPx.toDp() })) {
            drawColumnHeader(sheet, edges, scrollX, rowHeaderPx, zoom, measurer, look)
        }
    }
}

/** Points to dp: a point is 1/72 inch, a dp 1/160, so 1pt is about 1.33dp. */
private const val PtToDp = 4f / 3f

private const val TextCacheSize = 512

private const val TrailingColumns = 3

private class SheetLook(
    val background: Color,
    val text: Color,
    val headerBackground: Color,
    val headerText: Color,
    val grid: Color,
    val ink: Color,
    val paper: Color,
    val textSize: TextUnit,
    val cellPadding: Dp,
)

private class MergeIndex(merges: List<CellRange>) {
    private val byStartRow: Map<Int, List<CellRange>> = merges.groupBy { it.firstRow }

    // Every row a merge spans, so a lookup only checks the merges on that row; spans are capped,
    // since a merge down a whole column would otherwise list a million rows.
    private val byRow: Map<Int, List<CellRange>> = HashMap<Int, MutableList<CellRange>>().also { map ->
        for (m in merges) {
            for (r in m.firstRow..minOf(m.lastRow, m.firstRow + MaxMergeRows)) map.getOrPut(r) { ArrayList() }.add(m)
        }
    }

    fun startingAt(row: Int, col: Int): CellRange? = byStartRow[row]?.firstOrNull { it.firstCol == col }

    /** Whether ([row], [col]) is inside a merge but not its top-left cell. */
    fun covered(row: Int, col: Int): Boolean = byRow[row]?.any {
        col in it.firstCol..it.lastCol && !(row == it.firstRow && col == it.firstCol)
    } ?: false

    val isEmpty: Boolean = merges.isEmpty()
}

private const val MaxMergeRows = 10_000

/** First column whose right edge is past [x]. */
private fun firstColumnAt(edges: FloatArray, x: Float): Int {
    var lo = 0
    var hi = edges.size - 2
    while (lo < hi) {
        val mid = (lo + hi) / 2
        if (edges[mid + 1] <= x) lo = mid + 1 else hi = mid
    }
    return lo.coerceAtLeast(0)
}

private fun DrawScope.drawColumnHeader(
    sheet: Sheet,
    edges: FloatArray,
    scrollX: Float,
    rowHeaderPx: Float,
    zoom: Float,
    measurer: TextMeasurer,
    look: SheetLook,
) {
    drawRect(look.headerBackground)
    val style = TextStyle(fontSize = look.textSize * zoom.coerceAtLeast(1f), color = look.headerText, textAlign = TextAlign.Center)
    clipRect(left = rowHeaderPx) {
        var c = firstColumnAt(edges, scrollX)
        while (c < edges.size - 1) {
            val left = rowHeaderPx + edges[c] - scrollX
            if (left > size.width) break
            val width = edges[c + 1] - edges[c]
            if (width > 0f) {
                drawCellText(measurer, columnName(c), style, left, 0f, width, size.height, wrap = false, align = TextAlign.Center)
                drawLine(look.grid, Offset(left + width, 0f), Offset(left + width, size.height))
            }
            c++
        }
    }
    drawLine(look.grid, Offset(0f, size.height), Offset(size.width, size.height))
}

private fun DrawScope.drawSheetRow(
    sheet: Sheet,
    r: Int,
    styles: List<CellStyle>,
    edges: FloatArray,
    scrollX: Float,
    rowHeaderPx: Float,
    zoom: Float,
    measurer: TextMeasurer,
    look: SheetLook,
    merges: MergeIndex,
) {
    val row = sheet.row(r)
    val textSize = look.textSize * zoom
    val pad = look.cellPadding.toPx() * zoom
    clipRect(left = rowHeaderPx) {
        val firstVisible = firstColumnAt(edges, scrollX)
        var c = firstVisible
        while (c < edges.size - 1) {
            val right = rowHeaderPx + edges[c + 1] - scrollX
            if (right - (edges[c + 1] - edges[c]) > size.width) break
            if (merges.isEmpty || !merges.covered(r, c + 1) || merges.covered(r, c)) {
                drawLine(look.grid, Offset(right, 0f), Offset(right, size.height))
            }
            c++
        }
        drawLine(look.grid, Offset(rowHeaderPx, size.height), Offset(size.width, size.height))

        val cells = row?.cells ?: return@clipRect
        // Start from a cell that may spill text into the first visible column.
        var i = cells.binarySearchBy(firstVisible) { it.col }.let { if (it < 0) -it - 1 else it }
        i = (i - 1).coerceAtLeast(0)
        while (i < cells.size) {
            val cell = cells[i]
            val left = rowHeaderPx + edges[cell.col.coerceAtMost(edges.size - 1)] - scrollX
            if (left > size.width) break
            if (cell.col >= edges.size - 1 || (!merges.isEmpty && merges.covered(r, cell.col))) { i++; continue }
            val style = styles.getOrElse(cell.style) { CellStyle.Default }
            val merge = if (merges.isEmpty) null else merges.startingAt(r, cell.col)
            val ownRight = edges[(merge?.lastCol ?: cell.col).coerceAtMost(edges.size - 2) + 1]
            val ownWidth = ownRight - edges[cell.col]
            if (ownWidth <= 0f) { i++; continue }
            style.fill?.let { drawRect(Color(it), Offset(left, 0f), Size(ownWidth, size.height)) }
            if (cell.text.isNotEmpty()) {
                val align = when (style.align) {
                    CellAlign.Start -> TextAlign.Start
                    CellAlign.Center -> TextAlign.Center
                    CellAlign.End -> TextAlign.End
                    CellAlign.Auto -> if (cell.numeric) TextAlign.End else TextAlign.Start
                }
                // Text that doesn't fit spills over empty neighbours to the right, as in a
                // spreadsheet; numbers, wrapped text and centered text stay in their cell.
                val spills = !cell.numeric && !style.wrap && align == TextAlign.Start && merge == null
                val nextCol = cells.getOrNull(i + 1)?.col ?: (edges.size - 1)
                val drawWidth = if (spills) edges[nextCol.coerceAtMost(edges.size - 1)] - edges[cell.col] else ownWidth
                val textStyle = TextStyle(
                    fontSize = textSize,
                    color = readableTextColor(style, look),
                    fontWeight = if (style.bold) FontWeight.Bold else FontWeight.Normal,
                    fontStyle = if (style.italic) FontStyle.Italic else FontStyle.Normal,
                    textDecoration = when {
                        style.underline && style.strike -> TextDecoration.combine(listOf(TextDecoration.Underline, TextDecoration.LineThrough))
                        style.underline -> TextDecoration.Underline
                        style.strike -> TextDecoration.LineThrough
                        else -> null
                    },
                    textAlign = align,
                )
                clipRect(left = left, right = left + drawWidth) {
                    drawCellText(measurer, cell.text, textStyle, left + pad, 0f, drawWidth - pad * 2, size.height, style.wrap, align)
                }
            }
            i++
        }
    }
    drawRect(look.headerBackground, size = Size(rowHeaderPx, size.height))
    drawCellText(
        measurer,
        (r + 1).toString(),
        TextStyle(fontSize = look.textSize * zoom.coerceAtLeast(1f), color = look.headerText, textAlign = TextAlign.Center),
        0f, 0f, rowHeaderPx, size.height, wrap = false, align = TextAlign.Center,
    )
    drawLine(look.grid, Offset(rowHeaderPx, 0f), Offset(rowHeaderPx, size.height))
    drawLine(look.grid, Offset(0f, size.height), Offset(rowHeaderPx, size.height))
}

/** A sheet made on white paper often spells out black text, which would vanish on a dark theme. */
private fun readableTextColor(style: CellStyle, look: SheetLook): Color {
    val background = style.fill?.let { Color(it) } ?: look.background
    val own = style.textColor?.let { Color(it) }
    if (own != null && contrast(own, background) >= MinContrast) return own
    if (style.fill == null) return look.text
    return if (background.luminance() > 0.5f) look.ink else look.paper
}

private fun contrast(a: Color, b: Color): Float {
    val la = a.luminance() + 0.05f
    val lb = b.luminance() + 0.05f
    return if (la > lb) la / lb else lb / la
}

private const val MinContrast = 2.5f

private fun DrawScope.drawCellText(
    measurer: TextMeasurer,
    text: String,
    style: TextStyle,
    left: Float,
    top: Float,
    width: Float,
    height: Float,
    wrap: Boolean,
    align: TextAlign,
) {
    if (width <= 1f || height <= 1f) return
    val layout: TextLayoutResult = measurer.measure(
        text = if (wrap) text else text.replace('\n', ' '),
        style = style.copy(textAlign = align),
        overflow = TextOverflow.Clip,
        softWrap = wrap,
        maxLines = if (wrap) Int.MAX_VALUE else 1,
        constraints = Constraints(minWidth = width.toInt(), maxWidth = width.toInt()),
    )
    val y = if (wrap) top else top + (height - layout.size.height) / 2f
    drawText(layout, topLeft = Offset(left, y))
}

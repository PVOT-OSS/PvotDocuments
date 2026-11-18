package org.prauga.documents.viewer.slides

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.BaselineShift
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withLink
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.em
import com.prauga.pvot.designsystem.theme.PvotSpacing
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.prauga.documents.R
import org.prauga.documents.formats.richtext.ParagraphAlign
import org.prauga.documents.formats.richtext.TextPosition
import org.prauga.documents.formats.slides.Presentation
import org.prauga.documents.formats.slides.Slide
import org.prauga.documents.formats.slides.SlideFill
import org.prauga.documents.formats.slides.SlideFrame
import org.prauga.documents.formats.slides.SlideGeometry
import org.prauga.documents.formats.slides.SlideParagraph
import org.prauga.documents.formats.slides.SlideShape
import org.prauga.documents.formats.slides.SlideText
import org.prauga.documents.formats.slides.SlideTextAnchor
import org.prauga.documents.viewer.DocumentColors
import org.prauga.documents.viewer.pdf.OpenLinkDialog
import org.prauga.documents.viewer.pdf.PdfNavigationState
import org.prauga.documents.viewer.pdf.PdfZoomState
import org.prauga.documents.viewer.pdf.isOpenableLink
import org.prauga.documents.viewer.pdf.pinchZoom
import kotlin.math.roundToInt

/** Opens a link from a slide, after asking. */
private val LocalSlideLink = compositionLocalOf<(String) -> Unit> { {} }

/** Slides are drawn as authored, like PDF pages, rather than recolored for the theme. */
@Composable
fun SlidesView(
    presentation: Presentation,
    topPadding: Dp,
    navigation: PdfNavigationState,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()
    val zoom = remember(presentation) { PdfZoomState() }
    val scope = rememberCoroutineScope()
    var pendingLink by remember(presentation) { mutableStateOf<String?>(null) }
    pendingLink?.let { OpenLinkDialog(it, onDismiss = { pendingLink = null }) }

    DisposableEffect(navigation, presentation) {
        navigation.pageCount = presentation.slides.size
        navigation.currentPage = 0
        navigation.goTo = { page -> scope.launch { listState.animateScrollToItem(page) } }
        onDispose {
            navigation.pageCount = 0
            navigation.indicatorVisible = false
            navigation.goTo = {}
        }
    }
    LaunchedEffect(navigation, presentation) {
        snapshotFlow {
            val info = listState.layoutInfo
            val centre = info.viewportSize.height / 2f - info.viewportStartOffset
            info.visibleItemsInfo.firstOrNull { centre >= it.offset && centre < it.offset + it.size }?.index
                ?: listState.firstVisibleItemIndex
        }.collect { navigation.currentPage = it }
    }
    LaunchedEffect(navigation, presentation) {
        snapshotFlow { listState.firstVisibleItemIndex to listState.firstVisibleItemScrollOffset }.collectLatest {
            navigation.indicatorVisible = true
            delay(IndicatorHideMillis)
            navigation.indicatorVisible = false
        }
    }

    CompositionLocalProvider(LocalSlideLink provides { link -> pendingLink = link }) {
        LazyColumn(
            state = listState,
            modifier = modifier
                .pinchZoom(zoom, listState, scope)
                .graphicsLayer {
                    scaleX = zoom.scale
                    scaleY = zoom.scale
                    translationX = zoom.offsetX
                    translationY = zoom.offsetY
                },
            contentPadding = PaddingValues(top = topPadding + PvotSpacing.M, start = PvotSpacing.M, end = PvotSpacing.M, bottom = PvotSpacing.L),
            verticalArrangement = Arrangement.spacedBy(PvotSpacing.M),
        ) {
            items(count = presentation.slides.size, key = { it }) { index ->
                SlideCard(presentation, presentation.slides[index], index)
            }
        }
    }
}

private const val IndicatorHideMillis = 1500L

@Composable
private fun SlideCard(presentation: Presentation, slide: Slide, index: Int) {
    val description = stringResource(R.string.viewer_slide_description, index + 1, presentation.slides.size)
    BoxWithConstraints(
        Modifier
            .fillMaxWidth()
            .aspectRatio(presentation.width / presentation.height)
            // A thin edge, so a white slide stands apart from a light background.
            .border(dimensionResource(R.dimen.document_rule), MaterialTheme.colorScheme.outlineVariant)
            .clipToBounds()
            .semantics { contentDescription = description },
    ) {
        val density = LocalDensity.current
        val widthPx = with(density) { maxWidth.toPx() }
        // Pixels per point at this slide's on-screen size.
        val scale = widthPx / presentation.width
        val background = slide.background
        val paper = DocumentColors.paper
        Canvas(Modifier.fillMaxSize()) {
            drawRect(if (background is SlideFill.Solid) Color(background.color) else paper)
        }
        if (background is SlideFill.Picture) {
            PictureFill(presentation, background.path, Modifier.fillMaxSize(), widthPx.roundToInt())
        }
        for (shape in slide.shapes) {
            ShapeView(presentation, shape, scale)
        }
    }
}

/** A frame's position and size on screen, rotated about its centre. */
@Composable
private fun Modifier.place(frame: SlideFrame, scale: Float): Modifier {
    val density = LocalDensity.current
    return this
        .offset { IntOffset((frame.x * scale).roundToInt(), (frame.y * scale).roundToInt()) }
        .requiredSize(with(density) { (frame.width * scale).toDp() }, with(density) { (frame.height * scale).toDp() })
        .graphicsLayer { rotationZ = frame.rotation }
}

@Composable
private fun ShapeView(presentation: Presentation, shape: SlideShape, scale: Float) {
    when (shape) {
        is SlideShape.Shape -> Box(Modifier.place(shape.frame, scale)) {
            Canvas(Modifier.fillMaxSize()) {
                scale(scaleX = if (shape.flipH) -1f else 1f, scaleY = if (shape.flipV) -1f else 1f) {
                    drawGeometry(shape, scale)
                }
            }
            (shape.fill as? SlideFill.Picture)?.let { PictureFill(presentation, it.path, Modifier.fillMaxSize(), (shape.frame.width * scale).roundToInt()) }
            shape.text?.let { TextBody(it, scale, Modifier.fillMaxSize()) }
        }
        is SlideShape.Picture -> Box(Modifier.place(shape.frame, scale)) {
            CroppedPicture(presentation, shape, scale)
        }
        is SlideShape.Table -> Box(Modifier.place(shape.frame, scale)) {
            TableView(shape, scale)
        }
    }
}

private fun DrawScope.drawGeometry(shape: SlideShape.Shape, scale: Float) {
    val w = size.width
    val h = size.height
    val fill = (shape.fill as? SlideFill.Solid)?.let { Color(it.color) }
    val line = shape.line
    val stroke = line?.let { Stroke(width = (it.widthPt * scale).coerceAtLeast(1f)) }
    val lineColor = line?.let { Color(it.color) }
    fun path(build: Path.() -> Unit) = Path().apply(build)
    when (shape.geometry) {
        SlideGeometry.Rectangle -> {
            fill?.let { drawRect(it) }
            if (stroke != null && lineColor != null) drawRect(lineColor, style = stroke)
        }
        SlideGeometry.RoundRectangle -> {
            val r = CornerRadius(minOf(w, h) * RoundCornerFraction)
            fill?.let { drawRoundRect(it, cornerRadius = r) }
            if (stroke != null && lineColor != null) drawRoundRect(lineColor, cornerRadius = r, style = stroke)
        }
        SlideGeometry.Ellipse -> {
            fill?.let { drawOval(it) }
            if (stroke != null && lineColor != null) drawOval(lineColor, style = stroke)
        }
        SlideGeometry.Line -> {
            if (lineColor != null) drawLine(lineColor, Offset.Zero, Offset(w, h), strokeWidth = stroke?.width ?: 1f)
        }
        else -> {
            val p = when (shape.geometry) {
                SlideGeometry.Triangle -> path { moveTo(w / 2, 0f); lineTo(w, h); lineTo(0f, h); close() }
                SlideGeometry.Diamond -> path { moveTo(w / 2, 0f); lineTo(w, h / 2); lineTo(w / 2, h); lineTo(0f, h / 2); close() }
                SlideGeometry.RightArrow -> path {
                    val head = minOf(w * 0.4f, h); moveTo(0f, h * 0.25f); lineTo(w - head, h * 0.25f); lineTo(w - head, 0f)
                    lineTo(w, h / 2); lineTo(w - head, h); lineTo(w - head, h * 0.75f); lineTo(0f, h * 0.75f); close()
                }
                else -> path {
                    val tip = minOf(w * 0.3f, h / 2); moveTo(0f, 0f); lineTo(w - tip, 0f); lineTo(w, h / 2)
                    lineTo(w - tip, h); lineTo(0f, h); lineTo(tip, h / 2); close()
                }
            }
            fill?.let { drawPath(p, it) }
            if (stroke != null && lineColor != null) drawPath(p, lineColor, style = stroke)
        }
    }
}

private const val RoundCornerFraction = 0.1667f

@Composable
private fun TextBody(text: SlideText, scale: Float, modifier: Modifier) {
    val density = LocalDensity.current
    fun pt(value: Float): Dp = with(density) { (value * scale).toDp() }
    Box(
        modifier.padding(start = pt(text.insetLeft), top = pt(text.insetTop), end = pt(text.insetRight), bottom = pt(text.insetBottom)),
        contentAlignment = when (text.anchor) {
            SlideTextAnchor.Top -> Alignment.TopStart
            SlideTextAnchor.Middle -> Alignment.CenterStart
            SlideTextAnchor.Bottom -> Alignment.BottomStart
        },
    ) {
        Column(Modifier.fillMaxWidth()) {
            for (paragraph in text.paragraphs) SlideParagraphView(paragraph, scale * text.fontScale, scale, text.wrap)
        }
    }
}

@Composable
private fun SlideParagraphView(paragraph: SlideParagraph, textScale: Float, layoutScale: Float, wrap: Boolean) {
    val ink = DocumentColors.ink
    val density = LocalDensity.current
    val openLink = LocalSlideLink.current
    fun pt(value: Float): Dp = with(density) { (value * layoutScale).toDp() }
    val sizePx = (paragraph.runs.firstOrNull()?.style?.sizePt ?: paragraph.emptySizePt ?: DefaultSizePt) * textScale
    val baseStyle = TextStyle(
        fontSize = with(density) { sizePx.toSp() },
        lineHeight = (LineHeightFactor * paragraph.lineSpacing).em,
        lineHeightStyle = LineHeightStyle(LineHeightStyle.Alignment.Center, LineHeightStyle.Trim.None),
        color = ink,
        textAlign = when (paragraph.align) {
            ParagraphAlign.Start -> TextAlign.Start
            ParagraphAlign.Center -> TextAlign.Center
            ParagraphAlign.End -> TextAlign.End
            ParagraphAlign.Justify -> TextAlign.Justify
        },
    )
    val top = pt(paragraph.spaceBeforePt)
    if (paragraph.runs.isEmpty()) {
        Box(Modifier.padding(top = top).height(with(density) { (sizePx * LineHeightFactor * paragraph.lineSpacing).toDp() }))
        return
    }
    val text = remember(paragraph, textScale) { slideText(paragraph, textScale, density, ink, openLink) }
    val bullet = paragraph.bullet
    // A negative indent hangs the bullet to the left of where the text starts.
    val hang = (-paragraph.indentPt).coerceAtLeast(0f)
    val start = (paragraph.marginLeftPt - hang).coerceAtLeast(0f)
    Row(Modifier.fillMaxWidth().padding(start = pt(start), top = top)) {
        if (bullet != null) {
            Text(
                text = bullet,
                style = baseStyle.copy(color = paragraph.runs.first().style.color?.let { Color(it) } ?: ink, textAlign = TextAlign.Start),
                modifier = if (hang > 0f) Modifier.width(pt(hang)) else Modifier.padding(end = pt(BulletGapPt)),
                softWrap = false,
            )
        } else if (hang > 0f && paragraph.indentPt < 0f) {
            Box(Modifier.width(pt(hang)))
        }
        Text(text = text, style = baseStyle, softWrap = wrap, modifier = Modifier.weight(1f))
    }
}

private const val DefaultSizePt = 18f
private const val LineHeightFactor = 1.2f
private const val BulletGapPt = 6f

private fun slideText(
    paragraph: SlideParagraph,
    textScale: Float,
    density: androidx.compose.ui.unit.Density,
    ink: Color,
    openLink: (String) -> Unit,
): AnnotatedString = buildAnnotatedString {
    for (run in paragraph.runs) {
        val s = run.style
        val span = SpanStyle(
            fontSize = with(density) { ((s.sizePt ?: DefaultSizePt) * textScale).toSp() },
            fontWeight = if (s.bold) FontWeight.Bold else FontWeight.Normal,
            fontStyle = if (s.italic) FontStyle.Italic else FontStyle.Normal,
            textDecoration = when {
                s.underline && s.strike -> TextDecoration.combine(listOf(TextDecoration.Underline, TextDecoration.LineThrough))
                s.underline -> TextDecoration.Underline
                s.strike -> TextDecoration.LineThrough
                else -> null
            },
            color = s.color?.let { Color(it) } ?: ink,
            background = s.highlight?.let { Color(it) } ?: Color.Unspecified,
            baselineShift = when (s.position) {
                TextPosition.Superscript -> BaselineShift.Superscript
                TextPosition.Subscript -> BaselineShift.Subscript
                TextPosition.Normal -> null
            },
            fontFamily = if (s.monospace) FontFamily.Monospace else null,
        )
        val link = s.link?.takeIf(::isOpenableLink)
        if (link != null) {
            withLink(LinkAnnotation.Clickable(link, TextLinkStyles(span)) { openLink(link) }) { append(run.text) }
        } else {
            withStyle(span) { append(run.text) }
        }
    }
}

@Composable
private fun TableView(table: SlideShape.Table, scale: Float) {
    val density = LocalDensity.current
    fun pt(value: Float): Dp = with(density) { (value * scale).toDp() }
    Column(Modifier.border(dimensionResource(R.dimen.document_rule), DocumentColors.grid)) {
        table.cells.forEachIndexed { r, row ->
            Row(Modifier.height(pt(table.rowHeights.getOrElse(r) { 0f }).coerceAtLeast(dimensionResource(R.dimen.document_rule)))) {
                var c = 0
                for (cell in row) {
                    val width = (c until c + cell.colSpan).sumOf { (table.columnWidths.getOrElse(it) { 0f }).toDouble() }.toFloat()
                    c += cell.colSpan
                    if (cell.covered && cell.colSpan == 1 && r > 0 && width > 0f) {
                        // Covered by a cell above: leave its area, without a top border.
                        Box(Modifier.width(pt(width)).fillMaxHeight())
                        continue
                    }
                    Box(
                        Modifier
                            .width(pt(width))
                            .fillMaxHeight()
                            .then(if (cell.fill != null) Modifier.background(Color(cell.fill)) else Modifier)
                            .border(dimensionResource(R.dimen.document_grid_line), DocumentColors.grid),
                    ) {
                        if (!cell.covered) TextBody(cell.text, scale, Modifier.fillMaxSize())
                    }
                }
            }
        }
    }
}

@Composable
private fun PictureFill(presentation: Presentation, path: String, modifier: Modifier, widthPx: Int) {
    val bitmap by produceState<ImageBitmap?>(null, path, widthPx) {
        value = withContext(Dispatchers.IO) { presentation.image(path, widthPx)?.asImageBitmap() }
    }
    val image = bitmap ?: return
    Canvas(modifier) {
        drawImage(image, dstSize = IntSize(size.width.roundToInt(), size.height.roundToInt()))
    }
}

/** A picture with its crop applied: the source rectangle left after cutting the cropped
 * fractions off each side, stretched over the frame. */
@Composable
private fun CroppedPicture(presentation: Presentation, picture: SlideShape.Picture, scale: Float) {
    val visibleFraction = (1f - picture.cropLeft - picture.cropRight).coerceIn(0.05f, 1f)
    val widthPx = (picture.frame.width * scale / visibleFraction).roundToInt()
    val bitmap by produceState<ImageBitmap?>(null, picture.path, widthPx) {
        value = withContext(Dispatchers.IO) { presentation.image(picture.path, widthPx)?.asImageBitmap() }
    }
    val image = bitmap ?: return
    Canvas(Modifier.fillMaxSize().then(picture.description?.let { d -> Modifier.semantics { contentDescription = d } } ?: Modifier)) {
        val srcX = (picture.cropLeft * image.width).roundToInt().coerceIn(0, image.width - 1)
        val srcY = (picture.cropTop * image.height).roundToInt().coerceIn(0, image.height - 1)
        val srcW = ((1f - picture.cropLeft - picture.cropRight) * image.width).roundToInt().coerceIn(1, image.width - srcX)
        val srcH = ((1f - picture.cropTop - picture.cropBottom) * image.height).roundToInt().coerceIn(1, image.height - srcY)
        drawImage(
            image,
            srcOffset = IntOffset(srcX, srcY),
            srcSize = IntSize(srcW, srcH),
            dstSize = IntSize(size.width.roundToInt(), size.height.roundToInt()),
        )
    }
}

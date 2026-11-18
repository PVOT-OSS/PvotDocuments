package org.prauga.documents.viewer

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.Dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.prauga.pvot.coreui.PvotErrorContent
import com.prauga.pvot.coreui.PvotLoadingContent
import com.prauga.pvot.coreui.UiState
import com.prauga.pvot.designsystem.components.PvotScreenScaffold
import com.prauga.pvot.designsystem.components.PvotSnackbarHost
import com.prauga.pvot.designsystem.motion.PvotDurations
import com.prauga.pvot.designsystem.motion.PvotMotion
import com.prauga.pvot.designsystem.navigation.PvotTopBar
import com.prauga.pvot.designsystem.navigation.PvotTopBarAction
import com.prauga.pvot.designsystem.theme.PvotSpacing
import kotlinx.coroutines.flow.filterNotNull
import org.prauga.documents.R
import org.prauga.documents.viewer.data.ReadingPosition
import org.prauga.documents.viewer.data.ViewerContent
import org.prauga.documents.viewer.pdf.DarkPageFilter
import org.prauga.documents.viewer.pdf.GoToPageDialog
import org.prauga.documents.viewer.pdf.PdfContentsSheet
import org.prauga.documents.viewer.pdf.PdfNavigationState
import org.prauga.documents.viewer.pdf.PdfPageIndicator
import org.prauga.documents.viewer.pdf.PdfPasswordPrompt
import org.prauga.documents.viewer.pdf.PdfReadingSettings
import org.prauga.documents.viewer.pdf.PdfSearchBar
import org.prauga.documents.viewer.pdf.PdfSearchState
import org.prauga.documents.viewer.pdf.PdfSelectionBar
import org.prauga.documents.viewer.pdf.PdfSelectionState
import org.prauga.documents.viewer.pdf.PdfView
import org.prauga.documents.viewer.pdf.PdfViewOptionsSheet
import org.prauga.documents.viewer.richtext.RichDocumentView
import org.prauga.documents.viewer.sheet.CsvSheetView
import org.prauga.documents.viewer.sheet.SpreadsheetView
import org.prauga.documents.viewer.slides.SlidesView
import org.prauga.documents.viewer.text.HtmlView
import org.prauga.documents.viewer.text.MarkdownView

/** The document [DocumentViewerViewModel] reads, in the viewer for its format, under a top bar
 * titled with its name. PDFs add search, contents, go-to-page and view options. */
@Composable
fun DocumentViewerScreen(
    onBack: () -> Unit,
    viewModel: DocumentViewerViewModel = hiltViewModel(),
) {
    val uiState by viewModel.state.collectAsStateWithLifecycle()
    val viewer = (uiState as? UiState.Success)?.data
    if (viewer == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { PvotLoadingContent() }
        return
    }
    DocumentViewerContent(viewer, viewModel::onEvent, onBack)
}

@Composable
private fun DocumentViewerContent(
    viewer: DocumentViewerUiState,
    onEvent: (DocumentViewerEvent) -> Unit,
    onBack: () -> Unit,
) {
    val name = viewer.name
    val pdfSearch = remember { PdfSearchState() }
    val pdfNavigation = remember { PdfNavigationState() }
    val pdfSelection = remember { PdfSelectionState() }
    val snackbar = remember { SnackbarHostState() }
    // Collected rather than keyed on the notice: clearing it would change the key and cancel the
    // effect before the snackbar had shown.
    LaunchedEffect(pdfSelection) {
        snapshotFlow { pdfSelection.notice }.filterNotNull().collect { notice ->
            pdfSelection.notice = null
            snackbar.showSnackbar(notice)
        }
    }
    val onPositionChange by rememberUpdatedState<(ReadingPosition) -> Unit> {
        onEvent(DocumentViewerEvent.ReadingPositionChanged(it))
    }
    val pdfReading = remember(viewer.initialPosition, viewer.darkPdfPages) {
        PdfReadingSettings(
            initialPosition = viewer.initialPosition,
            onPositionChange = { onPositionChange(it) },
            pageFilter = if (viewer.darkPdfPages) DarkPageFilter else null,
        )
    }
    var showGoToPage by remember { mutableStateOf(false) }
    var showContents by remember { mutableStateOf(false) }
    var showOptions by remember { mutableStateOf(false) }
    if (showGoToPage) GoToPageDialog(pdfNavigation, onDismiss = { showGoToPage = false })
    if (showContents) PdfContentsSheet(pdfNavigation, onDismiss = { showContents = false })
    if (showOptions) {
        PdfViewOptionsSheet(
            darkPages = viewer.darkPdfPages,
            onDarkPagesChange = { onEvent(DocumentViewerEvent.DarkPdfPages(it)) },
            onPages = { pdfNavigation.overviewOpen = true },
            onGoToPage = { showGoToPage = true },
            onDismiss = { showOptions = false },
        )
    }
    // The top bar's back arrow leaves the page overview first, like the back gesture does.
    val back = { if (pdfNavigation.overviewOpen) pdfNavigation.overviewOpen = false else onBack() }
    val pdfToolsShown = pdfSearch.available && !pdfSearch.active && !pdfNavigation.overviewOpen

    PvotScreenScaffold(
        topBar = {
            PvotTopBar(title = name, onBack = back, compact = true) {
                if (pdfNavigation.outline.isNotEmpty() && pdfToolsShown) {
                    PvotTopBarAction(
                        icon = painterResource(R.drawable.ic_list),
                        contentDescription = stringResource(R.string.viewer_pdf_contents),
                        onClick = { showContents = true }
                    )
                }
                if (pdfToolsShown) {
                    PvotTopBarAction(
                        icon = painterResource(R.drawable.ic_search),
                        contentDescription = stringResource(R.string.viewer_pdf_search),
                        onClick = pdfSearch::open
                    )
                    PvotTopBarAction(
                        icon = painterResource(R.drawable.ic_more),
                        contentDescription = stringResource(R.string.viewer_pdf_more),
                        onClick = { showOptions = true }
                    )
                }
            }
        },
        snackbarHost = { PvotSnackbarHost(snackbar) },
        bottomBar = {
            // The page indicator, the selection actions and the search bar share the bottom:
            // search wins, then a selection, then the indicator.
            AnimatedVisibility(
                visible = !pdfSearch.active && !pdfSelection.active && !pdfNavigation.overviewOpen &&
                    pdfNavigation.indicatorVisible && pdfNavigation.pageCount > 1,
                enter = fadeIn(tween(PvotDurations.Short)),
                exit = fadeOut(tween(PvotDurations.Medium))
            ) {
                PdfPageIndicator(pdfNavigation, onClick = { showGoToPage = true })
            }
            AnimatedVisibility(
                visible = pdfSelection.active && !pdfSearch.active && !pdfNavigation.overviewOpen,
                enter = fadeIn(tween(PvotDurations.Medium, easing = PvotMotion.Settle)) +
                    slideInVertically(tween(PvotDurations.Medium, easing = PvotMotion.Settle)) { it / 2 },
                exit = fadeOut(tween(PvotDurations.Short)) +
                    slideOutVertically(tween(PvotDurations.Short)) { it / 2 }
            ) {
                PdfSelectionBar(pdfSelection)
            }
            AnimatedVisibility(
                visible = pdfSearch.active,
                enter = fadeIn(tween(PvotDurations.Medium, easing = PvotMotion.Settle)) +
                    slideInVertically(tween(PvotDurations.Medium, easing = PvotMotion.Settle)) { it / 2 },
                exit = fadeOut(tween(PvotDurations.Short)) +
                    slideOutVertically(tween(PvotDurations.Short)) { it / 2 }
            ) {
                PdfSearchBar(pdfSearch)
            }
        }
    ) { padding ->
        val topPadding = padding.calculateTopPadding()
        val stateModifier = Modifier.fillMaxSize().padding(top = topPadding)
        when (val content = viewer.content) {
            ViewerContentState.Loading -> Box(stateModifier, contentAlignment = Alignment.Center) {
                PvotLoadingContent()
            }
            is ViewerContentState.NeedsPassword -> PdfPasswordPrompt(
                wrongPassword = content.wrongPassword,
                attempt = content.attempt,
                onSubmit = { onEvent(DocumentViewerEvent.SubmitPassword(it)) },
                modifier = stateModifier,
            )
            ViewerContentState.Failed -> Box(stateModifier, contentAlignment = Alignment.Center) {
                PvotErrorContent(
                    title = stringResource(R.string.viewer_read_failed),
                    message = stringResource(R.string.viewer_read_failed_message),
                    onRetry = { onEvent(DocumentViewerEvent.Retry) },
                )
            }
            is ViewerContentState.Ready -> ViewerBody(
                content = content.content,
                topPadding = topPadding,
                pdfSearch = pdfSearch,
                pdfNavigation = pdfNavigation,
                pdfReading = pdfReading,
                pdfSelection = pdfSelection,
            )
        }
    }
}

@Composable
private fun ViewerBody(
    content: ViewerContent,
    topPadding: Dp,
    pdfSearch: PdfSearchState,
    pdfNavigation: PdfNavigationState,
    pdfReading: PdfReadingSettings,
    pdfSelection: PdfSelectionState,
) {
    val modifier = Modifier.fillMaxSize()
    when (content) {
        is ViewerContent.Pdf -> PdfView(
            document = content.document,
            topPadding = topPadding,
            search = pdfSearch,
            navigation = pdfNavigation,
            reading = pdfReading,
            selection = pdfSelection,
            modifier = modifier,
        )
        is ViewerContent.Spreadsheet -> SpreadsheetView(content.workbook, topPadding, modifier)
        is ViewerContent.Slides -> SlidesView(content.presentation, topPadding, pdfNavigation, modifier)
        is ViewerContent.RichText -> RichDocumentView(content.document, topPadding, modifier)
        is ViewerContent.Csv -> CsvSheetView(content.rows, topPadding, modifier)
        is ViewerContent.Markdown -> MarkdownView(text = content.text, topPadding = topPadding, modifier = modifier)
        is ViewerContent.Html -> HtmlView(html = content.html, topPadding = topPadding, modifier = modifier)
        is ViewerContent.Text -> SelectionContainer {
            Text(
                text = content.text,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground,
                fontFamily = if (content.monospace) FontFamily.Monospace else FontFamily.Default,
                modifier = modifier
                    .padding(top = topPadding)
                    .verticalScroll(rememberScrollState())
                    .padding(PvotSpacing.L)
            )
        }
    }
}

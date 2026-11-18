package org.prauga.documents.viewer

import androidx.core.net.toUri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.prauga.pvot.coreui.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.prauga.documents.common.ViewerFormat
import org.prauga.documents.common.WhileScreenSubscribed
import org.prauga.documents.settings.data.SettingsRepository
import org.prauga.documents.viewer.data.DocumentLoader
import org.prauga.documents.viewer.data.ReadingPosition
import org.prauga.documents.viewer.data.ReadingPositionRepository
import org.prauga.documents.viewer.data.ViewerContent
import org.prauga.documents.viewer.pdf.PdfPasswordRequiredException
import timber.log.Timber
import java.io.Closeable
import javax.inject.Inject

@HiltViewModel
class DocumentViewerViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val documentLoader: DocumentLoader,
    private val readingPositionRepository: ReadingPositionRepository,
    private val settingsRepository: SettingsRepository,
) : ViewModel() {

    private val uri = requireNotNull(savedStateHandle.get<String>(DocumentViewerActivity.ExtraUri)) {
        "DocumentViewerActivity is started only through start()"
    }.toUri()
    private val name = savedStateHandle.get<String>(DocumentViewerActivity.ExtraName).orEmpty()
    private val format = ViewerFormat.valueOf(
        requireNotNull(savedStateHandle.get<String>(DocumentViewerActivity.ExtraFormat)) {
            "DocumentViewerActivity is started only through start()"
        }
    )

    private val content = MutableStateFlow<ViewerContentState>(ViewerContentState.Loading)
    private val reading = MutableStateFlow<Reading?>(null)
    private var password: String? = null
    private var passwordAttempts = 0
    private var loading: Job? = null
    private var opened: Closeable? = null

    val state: StateFlow<UiState<DocumentViewerUiState>> =
        combine(content, reading, settingsRepository.settings) { content, reading, settings ->
            if (reading == null || settings == null) return@combine UiState.Loading
            UiState.Success(
                DocumentViewerUiState(
                    name = name,
                    format = format,
                    content = content,
                    darkPdfPages = settings.darkPdfPages,
                    initialPosition = reading.initialPosition,
                )
            )
        }.stateIn(viewModelScope, WhileScreenSubscribed, UiState.Loading)

    init {
        viewModelScope.launch {
            val key = readingPositionRepository.key(uri, name)
            reading.value = Reading(key, readingPositionRepository.position(key))
        }
        load()
    }

    fun onEvent(event: DocumentViewerEvent) {
        when (event) {
            DocumentViewerEvent.Retry -> load()
            is DocumentViewerEvent.SubmitPassword -> {
                password = event.password
                passwordAttempts++
                load()
            }
            is DocumentViewerEvent.DarkPdfPages -> settingsRepository.updateDarkPdfPages(event.enabled)
            is DocumentViewerEvent.ReadingPositionChanged ->
                reading.value?.let { readingPositionRepository.save(it.key, event.position) }
        }
    }

    private fun load() {
        loading?.cancel()
        loading = viewModelScope.launch {
            content.value = ViewerContentState.Loading
            content.value = try {
                val loaded = documentLoader.load(uri, format, password)
                replaceOpened(loaded.closeable())
                ViewerContentState.Ready(loaded)
            } catch (e: CancellationException) {
                throw e
            } catch (e: PdfPasswordRequiredException) {
                ViewerContentState.NeedsPassword(e.passwordWasGiven, passwordAttempts)
            } catch (e: Exception) {
                Timber.w("Could not read a document: %s", e.javaClass.name)
                ViewerContentState.Failed
            }
        }
    }

    private fun replaceOpened(next: Closeable?) {
        opened?.close()
        opened = next
    }

    override fun onCleared() {
        replaceOpened(null)
    }

    private fun ViewerContent.closeable(): Closeable? = when (this) {
        is ViewerContent.Pdf -> document
        is ViewerContent.Slides -> presentation
        is ViewerContent.RichText -> document
        is ViewerContent.Text, is ViewerContent.Csv, is ViewerContent.Markdown, is ViewerContent.Html,
        is ViewerContent.Spreadsheet -> null
    }

    private data class Reading(val key: String, val initialPosition: ReadingPosition?)
}

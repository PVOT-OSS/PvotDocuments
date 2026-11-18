package org.prauga.documents.settings.data

import android.content.SharedPreferences
import androidx.core.content.edit
import com.prauga.pvot.designsystem.effects.PvotGlassDefaults
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.prauga.documents.common.coroutines.ApplicationScope
import org.prauga.documents.common.preferences.DocumentsPreferences
import javax.inject.Inject
import javax.inject.Singleton

/** [settings] is null until the preferences file has been read. */
@Singleton
class SettingsRepository @Inject constructor(
    private val preferences: DocumentsPreferences,
    @ApplicationScope private val scope: CoroutineScope,
) {
    private val state = MutableStateFlow<AppSettings?>(null)
    val settings: StateFlow<AppSettings?> = state.asStateFlow()
    private val writes = Mutex()

    init {
        scope.launch { writes.withLock { state.value = read(preferences.get()) } }
    }

    fun updateGlassIntensity(intensity: Float) =
        update({ copy(glassIntensity = intensity) }) { putFloat(KeyGlassIntensity, intensity) }

    fun updateDynamicColor(enabled: Boolean) =
        update({ copy(dynamicColor = enabled) }) { putBoolean(KeyDynamicColor, enabled) }

    fun updateGridView(enabled: Boolean) =
        update({ copy(gridView = enabled) }) { putBoolean(KeyGridView, enabled) }

    fun updateDarkPdfPages(enabled: Boolean) =
        update({ copy(darkPdfPages = enabled) }) { putBoolean(KeyDarkPdfPages, enabled) }

    fun updateAutosaveOpenedDocuments(enabled: Boolean) =
        update({ copy(autosaveOpenedDocuments = enabled) }) { putBoolean(KeyAutosave, enabled) }

    fun completeOnboarding() =
        update({ copy(onboardingComplete = true) }) { putBoolean(KeyOnboardingComplete, true) }

    // The in-memory value changes at once so a dragged slider follows the finger; the stored file
    // is then the source of truth again.
    private fun update(change: AppSettings.() -> AppSettings, persist: SharedPreferences.Editor.() -> Unit) {
        state.update { it?.change() }
        scope.launch {
            writes.withLock {
                val stored = preferences.get()
                stored.edit(action = persist)
                state.value = read(stored)
            }
        }
    }

    private fun read(stored: SharedPreferences) = AppSettings(
        glassIntensity = stored.getFloat(KeyGlassIntensity, PvotGlassDefaults.defaultIntensity()),
        dynamicColor = stored.getBoolean(KeyDynamicColor, false),
        gridView = stored.getBoolean(KeyGridView, false),
        darkPdfPages = stored.getBoolean(KeyDarkPdfPages, false),
        autosaveOpenedDocuments = if (stored.contains(KeyAutosave)) stored.getBoolean(KeyAutosave, true) else null,
        onboardingComplete = stored.getBoolean(KeyOnboardingComplete, false),
    )

    private companion object {
        const val KeyGlassIntensity = "glass_intensity"
        const val KeyDynamicColor = "dynamic_color"
        const val KeyGridView = "grid_view"
        const val KeyDarkPdfPages = "dark_pdf_pages"
        const val KeyAutosave = "autosave_opened_documents"
        const val KeyOnboardingComplete = "onboarding_complete"
    }
}

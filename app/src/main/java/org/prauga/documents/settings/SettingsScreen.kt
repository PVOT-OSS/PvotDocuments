package org.prauga.documents.settings

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.prauga.pvot.coreui.PvotSectionHeader
import com.prauga.pvot.coreui.UiState
import com.prauga.pvot.designsystem.components.PvotScreen
import com.prauga.pvot.designsystem.components.PvotScreenDefaults
import com.prauga.pvot.designsystem.components.list.PvotListGroup
import com.prauga.pvot.designsystem.components.list.PvotPreferenceRow
import com.prauga.pvot.designsystem.components.list.PvotSwitchRow
import com.prauga.pvot.designsystem.components.slider.PvotSlider
import com.prauga.pvot.designsystem.effects.PvotGlassDefaults
import com.prauga.pvot.designsystem.effects.PvotGlassIntensity
import com.prauga.pvot.designsystem.motion.pvotReveal
import com.prauga.pvot.designsystem.theme.PvotSpacing
import org.prauga.documents.BuildConfig
import org.prauga.documents.R
import org.prauga.documents.settings.data.AppSettings

@Composable
fun SettingsScreen(
    state: LazyListState,
    topPadding: Dp,
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.state.collectAsStateWithLifecycle()
    val settings = (uiState as? UiState.Success)?.data ?: return
    SettingsContent(settings, viewModel::onEvent, state, topPadding, modifier)
}

@Composable
private fun SettingsContent(
    settings: AppSettings,
    onEvent: (SettingsEvent) -> Unit,
    state: LazyListState,
    topPadding: Dp,
    modifier: Modifier,
) {
    PvotScreen(
        modifier = modifier,
        state = state,
        contentPadding = PvotScreenDefaults.contentPadding(top = topPadding)
    ) {
        item {
            PvotSectionHeader(
                title = stringResource(R.string.settings_section_appearance),
                modifier = Modifier
                    .fillMaxWidth()
                    .pvotReveal(0)
            )
        }
        item {
            PvotListGroup(modifier = Modifier.fillMaxWidth().pvotReveal(1)) {
                item {
                    PvotSwitchRow(
                        title = stringResource(R.string.settings_grid_title),
                        summary = stringResource(R.string.settings_grid_summary),
                        checked = settings.gridView,
                        onCheckedChange = { onEvent(SettingsEvent.GridView(it)) }
                    )
                }
                item {
                    PvotSwitchRow(
                        title = stringResource(R.string.settings_dynamic_color_title),
                        summary = stringResource(R.string.settings_dynamic_color_summary),
                        checked = settings.dynamicColor,
                        onCheckedChange = { onEvent(SettingsEvent.DynamicColor(it)) }
                    )
                }
            }
        }
        item {
            GlassIntensityCard(
                intensity = settings.glassIntensity,
                onIntensityChange = { onEvent(SettingsEvent.GlassIntensity(it)) },
                modifier = Modifier.fillMaxWidth().pvotReveal(2)
            )
        }
        item {
            PvotSectionHeader(
                title = stringResource(R.string.settings_section_open_with),
                modifier = Modifier
                    .fillMaxWidth()
                    .pvotReveal(3)
            )
        }
        item {
            PvotListGroup(modifier = Modifier.fillMaxWidth().pvotReveal(4)) {
                item {
                    PvotSwitchRow(
                        title = stringResource(R.string.settings_autosave_title),
                        summary = stringResource(R.string.settings_autosave_summary),
                        // Null until first chosen; shown as on until the switch pins it.
                        checked = settings.autosaveOpenedDocuments ?: true,
                        onCheckedChange = { onEvent(SettingsEvent.AutosaveOpenedDocuments(it)) }
                    )
                }
            }
        }
        item {
            PvotSectionHeader(
                title = stringResource(R.string.settings_section_about),
                modifier = Modifier
                    .fillMaxWidth()
                    .pvotReveal(5)
            )
        }
        item {
            PvotListGroup(modifier = Modifier.fillMaxWidth().pvotReveal(6)) {
                item {
                    PvotPreferenceRow(
                        title = stringResource(R.string.settings_version_title),
                        value = BuildConfig.VERSION_NAME,
                        showChevron = false,
                        onClick = {}
                    )
                }
            }
        }
    }
}

/** [PvotGlassDefaults.maxIntensity] is the device's own cap, 0 wherever it cannot blur. */
@Composable
private fun GlassIntensityCard(
    intensity: Float,
    onIntensityChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    val max = PvotGlassDefaults.maxIntensity()
    PvotListGroup(
        modifier = modifier,
        title = stringResource(R.string.settings_glass_title)
    ) {
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(PvotSpacing.L)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(glassSubtitleRes()),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = stringResource(glassLabelRes(intensity.coerceAtMost(max))),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                if (max > 0f) {
                    PvotSlider(
                        value = intensity.coerceAtMost(max),
                        onValueChange = onIntensityChange,
                        valueRange = 0f..max,
                        modifier = Modifier.padding(top = PvotSpacing.S)
                    )
                }
                if (PvotGlassIntensity.isReduced()) {
                    Text(
                        text = stringResource(R.string.settings_glass_reduced),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = PvotSpacing.XS)
                    )
                }
            }
        }
    }
}

@Composable
private fun glassSubtitleRes(): Int = when {
    PvotGlassDefaults.isLensSupported -> R.string.settings_glass_subtitle
    PvotGlassDefaults.isBlurSupported -> R.string.settings_glass_subtitle_blur
    else -> R.string.settings_glass_subtitle_flat
}

@StringRes
private fun glassLabelRes(intensity: Float): Int = when {
    intensity <= PvotGlassDefaults.SolidIntensity -> R.string.settings_glass_solid
    intensity <= PvotGlassDefaults.FrostedIntensity + 0.02f -> R.string.settings_glass_frosted
    intensity <= 0.34f -> R.string.settings_glass_light
    intensity <= 0.67f -> R.string.settings_glass_medium
    intensity < 1f -> R.string.settings_glass_rich
    else -> R.string.settings_glass_full
}

// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: GPL-3.0-or-later

package org.prauga.documents.settings

sealed interface SettingsEvent {
    data class GridView(val enabled: Boolean) : SettingsEvent
    data class DynamicColor(val enabled: Boolean) : SettingsEvent
    data class GlassIntensity(val intensity: Float) : SettingsEvent
    data class AutosaveOpenedDocuments(val enabled: Boolean) : SettingsEvent
}

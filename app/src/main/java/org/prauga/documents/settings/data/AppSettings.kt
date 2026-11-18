package org.prauga.documents.settings.data

/**
 * [autosaveOpenedDocuments] is null until the user has answered the prompt shown the first time a
 * document arrives from another app.
 */
data class AppSettings(
    val glassIntensity: Float,
    val dynamicColor: Boolean,
    val gridView: Boolean,
    val darkPdfPages: Boolean,
    val autosaveOpenedDocuments: Boolean?,
    val onboardingComplete: Boolean,
)

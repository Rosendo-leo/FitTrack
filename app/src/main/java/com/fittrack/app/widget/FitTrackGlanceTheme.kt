package com.fittrack.app.widget

import androidx.compose.runtime.Composable
import androidx.glance.GlanceTheme
import androidx.glance.material3.ColorProviders
import com.fittrack.app.ui.theme.FitTrackDarkColors
import com.fittrack.app.ui.theme.FitTrackLightColors

// Paleta do app aplicada aos widgets: acompanha claro/escuro do sistema
private val FitTrackWidgetColors = ColorProviders(
    light = FitTrackLightColors,
    dark = FitTrackDarkColors
)

@Composable
fun FitTrackGlanceTheme(content: @Composable () -> Unit) {
    GlanceTheme(colors = FitTrackWidgetColors, content = content)
}

package com.fittrack.app.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance

// ── Tema escuro — azul céu vibrante + ciano sobre fundo quase-preto neutro ────
val DarkPrimary = Color(0xFF4C9EFF)
val DarkOnPrimary = Color(0xFF06121F)
val DarkPrimaryContainer = Color(0xFF1B3A66)
val DarkOnPrimaryContainer = Color(0xFFD6E8FF)

val DarkSecondary = Color(0xFF3FC1C9)
val DarkOnSecondary = Color(0xFF04191A)
val DarkSecondaryContainer = Color(0xFF124047)
val DarkOnSecondaryContainer = Color(0xFFC7F3F5)

val DarkTertiary = Color(0xFF4ADE9D)
val DarkOnTertiary = Color(0xFF04140D)
val DarkTertiaryContainer = Color(0xFF0F3D2E)
val DarkOnTertiaryContainer = Color(0xFFB8F5D9)

val DarkError = Color(0xFFFF7A7A)
val DarkOnError = Color(0xFF1A0505)
val DarkErrorContainer = Color(0xFF5C1A1A)
val DarkOnErrorContainer = Color(0xFFFFDAD6)

val DarkBackground = Color(0xFF0A0D12)
val DarkOnBackground = Color(0xFFE7EBEF)
val DarkSurface = Color(0xFF0A0D12)
val DarkOnSurface = Color(0xFFE7EBEF)
val DarkSurfaceVariant = Color(0xFF1A2027)
val DarkOnSurfaceVariant = Color(0xFF98A3AF)
val DarkOutline = Color(0xFF39424C)
val DarkOutlineVariant = Color(0xFF212830)

val DarkSurfaceContainerLowest = Color(0xFF060709)
val DarkSurfaceContainerLow = Color(0xFF10141A)
val DarkSurfaceContainer = Color(0xFF141920)
val DarkSurfaceContainerHigh = Color(0xFF1A2029)
val DarkSurfaceContainerHighest = Color(0xFF212832)

// ── Tema claro — off-white frio com índigo profundo ──────────────────────────
val LightPrimary = Color(0xFF4C5FE4)
val LightOnPrimary = Color(0xFFFFFFFF)
val LightPrimaryContainer = Color(0xFFE0E5FF)
val LightOnPrimaryContainer = Color(0xFF101C5C)

val LightSecondary = Color(0xFF7C5CE0)
val LightOnSecondary = Color(0xFFFFFFFF)
val LightSecondaryContainer = Color(0xFFEBE2FF)
val LightOnSecondaryContainer = Color(0xFF2A1560)

val LightTertiary = Color(0xFF0E9F6E)
val LightOnTertiary = Color(0xFFFFFFFF)
val LightTertiaryContainer = Color(0xFFC9F3DF)
val LightOnTertiaryContainer = Color(0xFF04402B)

val LightError = Color(0xFFDC3D43)
val LightOnError = Color(0xFFFFFFFF)
val LightErrorContainer = Color(0xFFFFDEDC)
val LightOnErrorContainer = Color(0xFF5C0F12)

val LightBackground = Color(0xFFF5F6FC)
val LightOnBackground = Color(0xFF171A26)
val LightSurface = Color(0xFFF5F6FC)
val LightOnSurface = Color(0xFF171A26)
val LightSurfaceVariant = Color(0xFFE4E7F3)
val LightOnSurfaceVariant = Color(0xFF565E7A)
val LightOutline = Color(0xFFAAB1CC)
val LightOutlineVariant = Color(0xFFDCE0EE)

val LightSurfaceContainerLowest = Color(0xFFFFFFFF)
val LightSurfaceContainerLow = Color(0xFFFFFFFF)
val LightSurfaceContainer = Color(0xFFEDEFF8)
val LightSurfaceContainerHigh = Color(0xFFE6E9F4)
val LightSurfaceContainerHighest = Color(0xFFDFE3F0)

// ── Alerta (âmbar) — semântica intermediária entre ok (tertiary) e erro ──────
val WarningDark = Color(0xFFFFB454)
val WarningLight = Color(0xFFB45D00)

/** Âmbar de alerta adequado ao tema atual (claro/escuro). */
val ColorScheme.warning: Color
    get() = if (surface.luminance() < 0.5f) WarningDark else WarningLight

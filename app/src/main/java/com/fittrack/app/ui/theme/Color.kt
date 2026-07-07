package com.fittrack.app.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance

// ── Tema escuro — "electric indigo" sobre fundo quase-preto azulado ──────────
val DarkPrimary = Color(0xFF7C9AFF)
val DarkOnPrimary = Color(0xFF0A0C14)
val DarkPrimaryContainer = Color(0xFF2A3670)
val DarkOnPrimaryContainer = Color(0xFFDDE3FF)

val DarkSecondary = Color(0xFFB49CFF)
val DarkOnSecondary = Color(0xFF14091F)
val DarkSecondaryContainer = Color(0xFF3B2A6B)
val DarkOnSecondaryContainer = Color(0xFFEBE2FF)

val DarkTertiary = Color(0xFF4ADE9D)
val DarkOnTertiary = Color(0xFF04140D)
val DarkTertiaryContainer = Color(0xFF0F3D2E)
val DarkOnTertiaryContainer = Color(0xFFB8F5D9)

val DarkError = Color(0xFFFF7A7A)
val DarkOnError = Color(0xFF1A0505)
val DarkErrorContainer = Color(0xFF5C1A1A)
val DarkOnErrorContainer = Color(0xFFFFDAD6)

val DarkBackground = Color(0xFF0A0C12)
val DarkOnBackground = Color(0xFFE9ECF5)
val DarkSurface = Color(0xFF0A0C12)
val DarkOnSurface = Color(0xFFE9ECF5)
val DarkSurfaceVariant = Color(0xFF1C2130)
val DarkOnSurfaceVariant = Color(0xFF9BA3C0)
val DarkOutline = Color(0xFF3A415C)
val DarkOutlineVariant = Color(0xFF232941)

val DarkSurfaceContainerLowest = Color(0xFF07080D)
val DarkSurfaceContainerLow = Color(0xFF11141E)
val DarkSurfaceContainer = Color(0xFF161A27)
val DarkSurfaceContainerHigh = Color(0xFF1C2131)
val DarkSurfaceContainerHighest = Color(0xFF232A3D)

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

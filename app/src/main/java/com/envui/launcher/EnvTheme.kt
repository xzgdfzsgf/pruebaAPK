package com.envui.launcher

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight

data class AccentOption(val name: String, val color: Color)

val AccentOptions = listOf(
    AccentOption("Glaciar", Color(0xFF70C8FF)),
    AccentOption("Lavanda", Color(0xFFB59CFF)),
    AccentOption("Menta", Color(0xFF5EE6B5)),
    AccentOption("Coral", Color(0xFFFF8A7A)),
    AccentOption("Ámbar", Color(0xFFFFC266)),
    AccentOption("Orquídea", Color(0xFFFF7EB6)),
    AccentOption("Grafito", Color(0xFFD6DEE8)),
)

/** Paleta completa calculada a partir de los ajustes del usuario. */
data class EnvPalette(
    val dark: Boolean,
    val accent: Color,
    val onAccent: Color,
    val bgTop: Color,
    val bgBottom: Color,
    val glow: Color,
    val surface: Color,
    val surfaceStrong: Color,
    val border: Color,
    val fg: Color,
    val muted: Color,
)

fun buildPalette(dark: Boolean, accent: Color, glass: Float): EnvPalette {
    val g = glass.coerceIn(0.2f, 0.98f)
    return if (dark) EnvPalette(
        dark = true,
        accent = accent,
        onAccent = Color(0xFF06101E),
        bgTop = Color(0xFF060D1A),
        bgBottom = Color(0xFF12203D),
        glow = accent.copy(alpha = 0.32f),
        surface = Color(0xFF14233A).copy(alpha = g),
        surfaceStrong = Color(0xFF101C30).copy(alpha = (g + 0.2f).coerceAtMost(0.98f)),
        border = Color.White.copy(alpha = 0.09f),
        fg = Color(0xFFEDF4FF),
        muted = Color(0xFF9FB2CC),
    ) else EnvPalette(
        dark = false,
        accent = accent,
        onAccent = Color(0xFF06101E),
        bgTop = Color(0xFFC9DFF6),
        bgBottom = Color(0xFFF3F6FC),
        glow = accent.copy(alpha = 0.30f),
        surface = Color(0xFFF7FAFF).copy(alpha = g),
        surfaceStrong = Color(0xFFFFFFFF).copy(alpha = (g + 0.2f).coerceAtMost(0.98f)),
        border = Color(0xFF0D2240).copy(alpha = 0.08f),
        fg = Color(0xFF0F1F36),
        muted = Color(0xFF55687F),
    )
}

val ClockFonts = listOf("Moderna", "Clásica", "Técnica", "Manuscrita")
val ClockWeights = listOf("Fina", "Ligera", "Normal", "Negrita")
val ClockStyles = listOf("Digital", "Analógico", "Apilado", "Fino", "Neón")
val LayoutNames = listOf("Escritorio", "Minimalista", "Productivo")
val IconShapes = listOf("Redondeado", "Círculo", "Sin fondo")

fun clockFontFamily(index: Int): FontFamily = when (index) {
    1 -> FontFamily.Serif
    2 -> FontFamily.Monospace
    3 -> FontFamily.Cursive
    else -> FontFamily.SansSerif
}

fun clockFontWeight(index: Int): FontWeight = when (index) {
    0 -> FontWeight.Thin
    1 -> FontWeight.Light
    2 -> FontWeight.Normal
    else -> FontWeight.Bold
}

/** Fondos integrados, cada uno con versión oscura y clara para conservar la legibilidad. */
data class GradientPreset(
    val name: String,
    val darkColors: List<Color>,
    val lightColors: List<Color>,
)

val GradientPresets = listOf(
    GradientPreset("ENV", listOf(Color(0xFF060D1A), Color(0xFF12203D)), listOf(Color(0xFFC9DFF6), Color(0xFFF3F6FC))),
    GradientPreset("Aurora", listOf(Color(0xFF041A1F), Color(0xFF0B3B3A), Color(0xFF1D2B5E)), listOf(Color(0xFFCFF3EA), Color(0xFFDDE7FB))),
    GradientPreset("Atardecer", listOf(Color(0xFF1A0B1E), Color(0xFF4A1B35), Color(0xFF8A3B2E)), listOf(Color(0xFFFCE1D4), Color(0xFFF7D6E4))),
    GradientPreset("Océano", listOf(Color(0xFF020B1C), Color(0xFF06305A), Color(0xFF0B6A8C)), listOf(Color(0xFFCDE9F7), Color(0xFFE7F5FB))),
    GradientPreset("Bosque", listOf(Color(0xFF07130C), Color(0xFF173625), Color(0xFF2F4A2A)), listOf(Color(0xFFDCEBD8), Color(0xFFEFF5E9))),
    GradientPreset("Grafito", listOf(Color(0xFF0B0C0F), Color(0xFF1C1F26), Color(0xFF2B2F38)), listOf(Color(0xFFE4E6EA), Color(0xFFF6F7F9))),
    GradientPreset("Volcán", listOf(Color(0xFF120806), Color(0xFF3A1208), Color(0xFF1B1B2E)), listOf(Color(0xFFF6DDD2), Color(0xFFEDE7F3))),
)

val WorldZones = listOf(
    "America/New_York" to "Nueva York",
    "America/Los_Angeles" to "Los Ángeles",
    "America/Mexico_City" to "Ciudad de México",
    "America/Bogota" to "Bogotá",
    "America/Lima" to "Lima",
    "America/Santiago" to "Santiago",
    "America/Argentina/Buenos_Aires" to "Buenos Aires",
    "Pacific/Galapagos" to "Galápagos",
    "Europe/Madrid" to "Madrid",
    "Europe/London" to "Londres",
    "Asia/Tokyo" to "Tokio",
)

val CategoryNames = mapOf(
    0 to "Juegos", 1 to "Música y audio", 2 to "Video", 3 to "Fotos", 4 to "Social",
    5 to "Noticias", 6 to "Mapas y viajes", 7 to "Productividad", 8 to "Accesibilidad",
)

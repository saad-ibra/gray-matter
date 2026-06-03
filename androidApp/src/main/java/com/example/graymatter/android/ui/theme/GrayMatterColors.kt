package com.example.graymatter.android.ui.theme

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

data class GrayMatterThemeColors(
    val background: Color,
    val surface: Color,
    val surfaceInput: Color,
    val surfaceBorder: Color,
    val surfaceCard: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textTertiary: Color,
    val textMuted: Color,
    val primary: Color,
    val onPrimary: Color,
    val neutral900: Color,
    val neutral800: Color,
    val neutral700: Color,
    val neutral600: Color,
    val neutral500: Color,
    val neutral400: Color,
    val error: Color,
    val isLight: Boolean
)

fun lightGrayMatterColors() = GrayMatterThemeColors(
    background = Color(0xFFF6F7F8),
    surface = Color(0xFFFFFFFF),
    surfaceInput = Color(0xFFF4F4F6),
    surfaceBorder = Color(0xFFE0E0E4),
    surfaceCard = Color(0xFFF0F0F2),
    textPrimary = Color(0xFF111111),
    textSecondary = Color(0xFF6B6B73),
    textTertiary = Color(0xFF9E9EA6),
    textMuted = Color(0xFFB0B0B8),
    primary = Color(0xFF111111),
    onPrimary = Color(0xFFFFFFFF),
    neutral900 = Color(0xFFF4F4F5),
    neutral800 = Color(0xFFE4E4E7),
    neutral700 = Color(0xFFD4D4D8),
    neutral600 = Color(0xFFA1A1AA),
    neutral500 = Color(0xFF71717A),
    neutral400 = Color(0xFF52525B),
    error = Color(0xFFDC2626),
    isLight = true
)

fun darkGrayMatterColors() = GrayMatterThemeColors(
    background = Color(0xFF000000),
    surface = Color(0xFF0C0C0C),
    surfaceInput = Color(0xFF0E0E10),
    surfaceBorder = Color(0xFF1E1E22),
    surfaceCard = Color(0xFF080808),
    textPrimary = Color(0xFFFFFFFF),
    textSecondary = Color(0xFFA1A1AA),
    textTertiary = Color(0xFF71717A),
    textMuted = Color(0xFF52525B),
    primary = Color(0xFFFFFFFF),
    onPrimary = Color(0xFF000000),
    neutral900 = Color(0xFF101012),
    neutral800 = Color(0xFF1A1A1E),
    neutral700 = Color(0xFF2A2A30),
    neutral600 = Color(0xFF42424A),
    neutral500 = Color(0xFF62626A),
    neutral400 = Color(0xFFA1A1AA),
    error = Color(0xFFEF4444),
    isLight = false
)

val LocalGrayMatterThemeColors = staticCompositionLocalOf { darkGrayMatterColors() }

/**
 * Gray Matter color palette - Static constants for semantic and non-themed colors.
 * Note: For backgrounds and text, use GrayMatterTheme.colors instead.
 */
object GrayMatterColors {
    // Backgrounds
    val BackgroundDark = Color(0xFF000000)
    val BackgroundLight = Color(0xFFF6F7F8)
    
    // Surfaces
    val SurfaceDark = Color(0xFF0C0C0C)
    val SurfaceInput = Color(0xFF0E0E10)
    val SurfaceBorder = Color(0xFF1E1E22)
    val SurfaceCard = Color(0xFF080808)
    
    // Text colors
    val TextPrimary = Color(0xFFFFFFFF)
    val TextSecondary = Color(0xFFA1A1AA)
    val TextTertiary = Color(0xFF71717A)
    val TextMuted = Color(0xFF52525B)
    
    // Accent colors
    val Primary = Color(0xFFFFFFFF)
    val OnPrimary = Color(0xFF000000)
    
    // New Primary Accent Color
    val CustomizedAccent = Color(0xFF7A3E6A)
    
    // Custom Palette (New Design)
    val AppleGreen = Color(0xFF8EA208)
    val Citrine = Color(0xFFBDBF09)
    val Jonquil = Color(0xFFEFCA08)
    val Gamboge = Color(0xFFF49F0A)
    val CocoaBrown = Color(0xFFD96C06)

    // Semantic Type Colors (User Requested)
    val TypeOpinion = Color(0xFF8E9E5A)
    val TypeBookmark = Color(0xFFC4A84E)
    val TypeLink = Color(0xFF5A9AB8)
    val TypeAnnotation = Color(0xFFC47A5A)
    val TypeLookupMain = Color(0xFFBF5A6A)
    val TypeLookupSavedElsewhere = Color(0xFFC47A8A)
    val TypeTemplate = Color(0xFF7E6A8C)
    val TypeNoteDescription = Color(0xFF788880)
    val TypeVisual = Color(0xFF5A9E8C)

    // Neutral grays
    val Neutral100 = Color(0xFFF4F4F5)
    val Neutral200 = Color(0xFFE4E4E7)
    val Neutral300 = Color(0xFFD4D4D8)
    val Neutral400 = Color(0xFFA1A1AA)
    val Neutral500 = Color(0xFF62626A)
    val Neutral600 = Color(0xFF42424A)
    val Neutral700 = Color(0xFF2A2A30)
    val Neutral800 = Color(0xFF1A1A1E)
    val Neutral900 = Color(0xFF101012)
    val Neutral950 = Color(0xFF050505)
    
    // Functional colors
    val Success = Color(0xFF8EA208)
    val SuccessContainer = Color(0xFF8EA208).copy(alpha = 0.15f)
    val Warning = Color(0xFFFACC15)
    val Error = Color(0xFFEF4444)
    val KnowledgeBlue = Color(0xFF42A5F5)
}

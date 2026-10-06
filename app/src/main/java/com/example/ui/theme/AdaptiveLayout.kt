package com.example.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

enum class StudioWindowWidthClass {
    COMPACT,  // < 600dp (Phones in portrait)
    MEDIUM,   // 600dp..839dp (Foldables unfolded, small tablets, large phones in landscape)
    EXPANDED  // >= 840dp (Tablets, desktop/ChromeOS)
}

enum class StudioWindowHeightClass {
    COMPACT,  // < 480dp (Phones in landscape)
    MEDIUM,   // 480dp..899dp
    EXPANDED  // >= 900dp
}

enum class StudioDeviceFormFactor {
    COMPACT_PORTRAIT,
    COMPACT_LANDSCAPE,
    FOLDABLE,
    TABLET
}

@Immutable
data class StudioAdaptiveInfo(
    val widthDp: Dp,
    val heightDp: Dp,
    val widthClass: StudioWindowWidthClass,
    val heightClass: StudioWindowHeightClass,
    val formFactor: StudioDeviceFormFactor,
    val isLandscape: Boolean,
    val isCompactHeight: Boolean,
    val isWideScreen: Boolean,
    val isLargeFontScale: Boolean,
    val fontScale: Float,
    val gridColumns: Int,
    val horizontalPadding: Dp,
    val contentMaxWidth: Dp
)

@Immutable
data class StudioSemanticPalette(
    val isDark: Boolean,
    val appBackground: Color,
    val topBarBackground: Color,
    val cardSurface: Color,
    val cardElevated: Color,
    val cardBorder: Color,
    val canvasBackground: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textMuted: Color,
    val accentPrimary: Color,
    val accentOnPrimary: Color,
    val accentSecondary: Color,
    val success: Color,
    val warning: Color,
    val error: Color
)

val DarkStudioPalette = StudioSemanticPalette(
    isDark = true,
    appBackground = Color(0xFF0F172A),
    topBarBackground = Color(0xFF0F172A),
    cardSurface = Color(0xFF1E293B),
    cardElevated = Color(0xFF0B1120),
    cardBorder = Color(0xFF334155),
    canvasBackground = Color(0xFF020617),
    textPrimary = Color(0xFFF8FAFC),
    textSecondary = Color(0xFF94A3B8),
    textMuted = Color(0xFF64748B),
    accentPrimary = Color(0xFF38BDF8),
    accentOnPrimary = Color(0xFF0F172A),
    accentSecondary = Color(0xFF6366F1),
    success = Color(0xFF34D399),
    warning = Color(0xFFF59E0B),
    error = Color(0xFFF87171)
)

val LightStudioPalette = StudioSemanticPalette(
    isDark = false,
    appBackground = Color(0xFFF1F5F9),
    topBarBackground = Color(0xFFFFFFFF),
    cardSurface = Color(0xFFFFFFFF),
    cardElevated = Color(0xFFF8FAFC),
    cardBorder = Color(0xFFCBD5E1),
    canvasBackground = Color(0xFFE2E8F0),
    textPrimary = Color(0xFF0F172A),
    textSecondary = Color(0xFF475569),
    textMuted = Color(0xFF64748B),
    accentPrimary = Color(0xFF0284C7),
    accentOnPrimary = Color(0xFFFFFFFF),
    accentSecondary = Color(0xFF4F46E5),
    success = Color(0xFF059669),
    warning = Color(0xFFD97706),
    error = Color(0xFFDC2626)
)

val LocalStudioPalette = staticCompositionLocalOf { DarkStudioPalette }

@Composable
fun rememberStudioAdaptiveInfo(
    overrideWidthDp: Dp? = null,
    overrideHeightDp: Dp? = null
): StudioAdaptiveInfo {
    val config = LocalConfiguration.current
    val density = LocalDensity.current
    val wDp = overrideWidthDp ?: config.screenWidthDp.dp
    val hDp = overrideHeightDp ?: config.screenHeightDp.dp
    val fontScale = density.fontScale

    val widthClass = when {
        wDp < 600.dp -> StudioWindowWidthClass.COMPACT
        wDp < 840.dp -> StudioWindowWidthClass.MEDIUM
        else -> StudioWindowWidthClass.EXPANDED
    }

    val heightClass = when {
        hDp < 480.dp -> StudioWindowHeightClass.COMPACT
        hDp < 900.dp -> StudioWindowHeightClass.MEDIUM
        else -> StudioWindowHeightClass.EXPANDED
    }

    val isLandscape = wDp > hDp
    val isCompactHeight = hDp < 500.dp

    val formFactor = when {
        widthClass == StudioWindowWidthClass.EXPANDED -> StudioDeviceFormFactor.TABLET
        widthClass == StudioWindowWidthClass.MEDIUM && !isCompactHeight -> StudioDeviceFormFactor.FOLDABLE
        isLandscape && isCompactHeight -> StudioDeviceFormFactor.COMPACT_LANDSCAPE
        else -> StudioDeviceFormFactor.COMPACT_PORTRAIT
    }

    val isWideScreen = isLandscape || widthClass != StudioWindowWidthClass.COMPACT
    val isLargeFontScale = fontScale >= 1.3f

    val gridColumns = when {
        fontScale >= 1.7f && wDp < 600.dp -> 1
        wDp >= 1000.dp -> 4
        wDp >= 680.dp -> 3
        else -> 2
    }

    val horizontalPadding = when (widthClass) {
        StudioWindowWidthClass.COMPACT -> if (wDp <= 340.dp) 10.dp else 16.dp
        StudioWindowWidthClass.MEDIUM -> 20.dp
        StudioWindowWidthClass.EXPANDED -> 28.dp
    }

    val contentMaxWidth = when (widthClass) {
        StudioWindowWidthClass.COMPACT -> Dp.Unspecified
        StudioWindowWidthClass.MEDIUM -> 840.dp
        StudioWindowWidthClass.EXPANDED -> 1200.dp
    }

    return StudioAdaptiveInfo(
        widthDp = wDp,
        heightDp = hDp,
        widthClass = widthClass,
        heightClass = heightClass,
        formFactor = formFactor,
        isLandscape = isLandscape,
        isCompactHeight = isCompactHeight,
        isWideScreen = isWideScreen,
        isLargeFontScale = isLargeFontScale,
        fontScale = fontScale,
        gridColumns = gridColumns,
        horizontalPadding = horizontalPadding,
        contentMaxWidth = contentMaxWidth
    )
}

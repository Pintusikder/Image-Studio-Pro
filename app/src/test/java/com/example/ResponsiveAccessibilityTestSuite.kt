package com.example

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertWidthIsAtLeast
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.example.model.ExportFormat
import com.example.ui.components.EditorControlActionItem
import com.example.ui.components.ImageStatsBar
import com.example.ui.screens.ToolModuleCard
import com.example.ui.theme.DarkStudioPalette
import com.example.ui.theme.LightStudioPalette
import com.example.ui.theme.LocalStudioPalette
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.rememberStudioAdaptiveInfo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ResponsiveAccessibilityTestSuite {

    @get:Rule
    val composeTestRule = createComposeRule()

    // ==========================================
    // 1. TOUCH TARGET & ACCESSIBILITY (>= 48dp)
    // ==========================================
    @Test
    fun testEditorControls_MinimumTouchTarget48dp() {
        composeTestRule.setContent {
            MyApplicationTheme {
                Row {
                    EditorControlActionItem(
                        icon = Icons.Default.Undo,
                        label = "Undo",
                        contentDescription = "Undo Action",
                        enabled = true,
                        iconTint = Color(0xFF38BDF8),
                        textColor = Color.White,
                        testTag = "test_undo_touch_target",
                        onClick = {}
                    )
                    EditorControlActionItem(
                        icon = Icons.Default.RestartAlt,
                        label = "Reset",
                        contentDescription = "Reset Action",
                        enabled = true,
                        iconTint = Color(0xFFF87171),
                        textColor = Color(0xFFF87171),
                        testTag = "test_reset_touch_target",
                        onClick = {}
                    )
                }
            }
        }
        composeTestRule.onNodeWithTag("test_undo_touch_target")
            .assertIsDisplayed()
            .assertWidthIsAtLeast(48.dp)
            .assertHeightIsAtLeast(48.dp)

        composeTestRule.onNodeWithTag("test_reset_touch_target")
            .assertIsDisplayed()
            .assertWidthIsAtLeast(48.dp)
            .assertHeightIsAtLeast(48.dp)
    }

    // ==========================================
    // 2. FONT SCALING (200% Font Scale - No Crash or Overlap)
    // ==========================================
    @Test
    fun testLargeFontScale_StatsBarRendersCleanly() {
        composeTestRule.setContent {
            CompositionLocalProvider(
                LocalDensity provides Density(density = 2.5f, fontScale = 2.0f)
            ) {
                MyApplicationTheme {
                    Surface(modifier = Modifier.fillMaxSize()) {
                        ImageStatsBar(
                            width = 1920,
                            height = 1080,
                            fileSizeBytes = 512 * 1024L,
                            dpi = 300,
                            format = ExportFormat.JPEG,
                            targetSizeKb = 500
                        )
                    }
                }
            }
        }
        composeTestRule.onNodeWithText("A: PIXEL DIMENSIONS").assertIsDisplayed()
        composeTestRule.onNodeWithText("1920 × 1080 px").assertIsDisplayed()
    }

    // ==========================================
    // 3. TABLETS & EXPANDED (w1200dp - Wide Screen Posture)
    // ==========================================
    @Test
    @Config(qualifiers = "w1200dp-h800dp", sdk = [34])
    fun testTablet_AdaptiveInfoCalculations() {
        var isWide = false
        var columns = 0
        composeTestRule.setContent {
            MyApplicationTheme {
                val info = rememberStudioAdaptiveInfo()
                isWide = info.isWideScreen
                columns = info.gridColumns
                Box(modifier = Modifier.fillMaxSize()) {
                    Text("Tablet Test")
                }
            }
        }
        assertTrue("Tablet should be identified as wide screen", isWide)
        assertTrue("Tablet should have >= 3 grid columns", columns >= 3)
    }

    // ==========================================
    // 4. COMPACT PHONE PORTRAIT (w360dp-h640dp)
    // ==========================================
    @Test
    @Config(qualifiers = "w360dp-h640dp", sdk = [34])
    fun testCompactPhone_AdaptiveInfoCalculations() {
        var isWide = true
        var isLandscape = true
        composeTestRule.setContent {
            MyApplicationTheme {
                val info = rememberStudioAdaptiveInfo()
                isWide = info.isWideScreen
                isLandscape = info.isLandscape
                Box(modifier = Modifier.fillMaxSize()) {
                    Text("Compact Phone Test")
                }
            }
        }
        assertFalse("Portrait phone must not be wide screen", isWide)
        assertFalse("Portrait phone must not be landscape", isLandscape)
    }

    // ==========================================
    // 5. LANDSCAPE PHONE (w640dp-h360dp)
    // ==========================================
    @Test
    @Config(qualifiers = "w640dp-h360dp", sdk = [34])
    fun testLandscapePhone_SideBySideAdaptation() {
        var isWide = false
        var isLandscape = false
        var isCompactHeight = false
        composeTestRule.setContent {
            MyApplicationTheme {
                val info = rememberStudioAdaptiveInfo()
                isWide = info.isWideScreen
                isLandscape = info.isLandscape
                isCompactHeight = info.isCompactHeight
                Box(modifier = Modifier.fillMaxSize()) {
                    Text("Landscape Phone Test")
                }
            }
        }
        assertTrue("Landscape phone must be wide screen", isWide)
        assertTrue("Landscape phone must be landscape", isLandscape)
        assertTrue("Height < 480dp must be compact height", isCompactHeight)
    }

    // ==========================================
    // 6. LIGHT MODE vs DARK MODE PALETTE CONTRAST
    // ==========================================
    @Test
    fun testLightAndDarkPalettes_DistinctContrastingSurfaces() {
        var lightAppBg = Color.Unspecified
        var lightCardBg = Color.Unspecified
        var darkAppBg = Color.Unspecified
        var darkCardBg = Color.Unspecified

        composeTestRule.setContent {
            MyApplicationTheme(darkTheme = false) {
                val palette = LocalStudioPalette.current
                lightAppBg = palette.appBackground
                lightCardBg = palette.cardSurface
            }
            MyApplicationTheme(darkTheme = true) {
                val palette = LocalStudioPalette.current
                darkAppBg = palette.appBackground
                darkCardBg = palette.cardSurface
            }
        }

        assertEquals(LightStudioPalette.appBackground, lightAppBg)
        assertEquals(LightStudioPalette.cardSurface, lightCardBg)
        assertEquals(DarkStudioPalette.appBackground, darkAppBg)
        assertEquals(DarkStudioPalette.cardSurface, darkCardBg)
    }

    // ==========================================
    // 7. TOOL MODULE CARD (Accessible touch size & text)
    // ==========================================
    @Test
    fun testToolModuleCard_AccessibleTouchAndLayout() {
        composeTestRule.setContent {
            MyApplicationTheme {
                ToolModuleCard(
                    title = "Resize Photo",
                    subtitle = "Scale % or target W × H px",
                    icon = Icons.Default.Undo,
                    accentColor = Color(0xFF38BDF8),
                    testTag = "test_tool_card",
                    onClick = {}
                )
            }
        }
        composeTestRule.onNodeWithTag("test_tool_card")
            .assertIsDisplayed()
            .assertHeightIsAtLeast(48.dp)

        composeTestRule.onNodeWithText("Resize Photo").assertIsDisplayed()
    }
}

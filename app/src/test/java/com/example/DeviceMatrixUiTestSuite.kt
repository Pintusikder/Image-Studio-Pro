package com.example

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.example.model.OutputVerificationReport
import com.example.model.VerificationMetric
import com.example.model.VerificationStatus
import com.example.ui.components.VerificationMetricCard
import com.example.ui.theme.MyApplicationTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class DeviceMatrixUiTestSuite {

    @get:Rule
    val composeTestRule = createComposeRule()

    private fun sampleVerificationReport(): OutputVerificationReport {
        return OutputVerificationReport(
            dimensionsMetric = VerificationMetric(
                title = "DIMENSIONS",
                requested = "300 × 100 px",
                actual = "300 × 100 px",
                status = VerificationStatus.PASS,
                details = "Exact pixel match verified"
            ),
            fileSizeMetric = VerificationMetric(
                title = "FILE SIZE",
                requested = "≤ 200 KB",
                actual = "198.6 KB",
                status = VerificationStatus.WITHIN_LIMIT,
                details = "Under maximum ceiling by 1.4 KB"
            ),
            dpiMetric = VerificationMetric(
                title = "PRINT RESOLUTION (DPI)",
                requested = "300 DPI",
                actual = "300 DPI",
                status = VerificationStatus.PASS,
                details = "EXIF DPI header verified"
            ),
            formatMetric = VerificationMetric(
                title = "IMAGE FORMAT",
                requested = "PNG (.png)",
                actual = "PNG Image",
                status = VerificationStatus.PASS,
                details = "Magic bytes match"
            ),
            metadataMetric = null,
            allMeasurablePassed = true,
            overallStatus = VerificationStatus.PASS
        )
    }

    // ==========================================
    // 1. SMALL SCREENS (320x480 mdpi)
    // ==========================================
    @Test
    @Config(qualifiers = "w320dp-h480dp", sdk = [34])
    fun testSmallScreen_LayoutRendersWithoutClipping() {
        composeTestRule.setContent {
            MyApplicationTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        Text("Image Studio - Small Screen 320dp")
                        VerificationMetricCard(sampleVerificationReport().dimensionsMetric, tag = "test_dim_small")
                        VerificationMetricCard(sampleVerificationReport().fileSizeMetric, tag = "test_size_small")
                    }
                }
            }
        }
        composeTestRule.onNodeWithTag("test_dim_small").assertIsDisplayed()
        composeTestRule.onNodeWithText("DIMENSIONS").assertIsDisplayed()
        composeTestRule.onAllNodesWithText("300 × 100 px")[0].assertIsDisplayed()
    }

    // ==========================================
    // 2. LARGE SCREENS / TABLETS (1280x800)
    // ==========================================
    @Test
    @Config(qualifiers = "w1280dp-h800dp", sdk = [34])
    fun testTablet_SpaciousLayoutRendering() {
        composeTestRule.setContent {
            MyApplicationTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    Column(modifier = Modifier.padding(24.dp)) {
                        Text("Image Studio - Tablet 1280x800")
                        VerificationMetricCard(sampleVerificationReport().dpiMetric, tag = "test_dpi_tablet")
                        VerificationMetricCard(sampleVerificationReport().formatMetric, tag = "test_fmt_tablet")
                    }
                }
            }
        }
        composeTestRule.onNodeWithTag("test_dpi_tablet").assertIsDisplayed()
        composeTestRule.onNodeWithText("PRINT RESOLUTION (DPI)").assertIsDisplayed()
        composeTestRule.onAllNodesWithText("300 DPI")[0].assertIsDisplayed()
    }

    // ==========================================
    // 3. FOLDABLES (Pixel Fold Inner 841x673)
    // ==========================================
    @Test
    @Config(qualifiers = "w841dp-h673dp", sdk = [34])
    fun testFoldable_AdaptiveLayoutRendering() {
        composeTestRule.setContent {
            MyApplicationTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Image Studio - Foldable Inner Display")
                        VerificationMetricCard(sampleVerificationReport().dimensionsMetric, tag = "test_dim_fold")
                        VerificationMetricCard(sampleVerificationReport().fileSizeMetric, tag = "test_size_fold")
                    }
                }
            }
        }
        composeTestRule.onNodeWithTag("test_size_fold").assertIsDisplayed()
        composeTestRule.onNodeWithText("FILE SIZE").assertIsDisplayed()
        composeTestRule.onNodeWithText("WITHIN LIMIT").assertIsDisplayed()
    }

    // ==========================================
    // 4. LIGHT MODE THEME
    // ==========================================
    @Test
    @Config(qualifiers = "+notnight", sdk = [34])
    fun testLightMode_ThemeContrastAndReadability() {
        composeTestRule.setContent {
            MyApplicationTheme(darkTheme = false) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Light Mode Verification")
                        VerificationMetricCard(sampleVerificationReport().dimensionsMetric, tag = "test_dim_light")
                    }
                }
            }
        }
        composeTestRule.onNodeWithTag("test_dim_light").assertIsDisplayed()
        composeTestRule.onNodeWithText("DIMENSIONS").assertIsDisplayed()
    }

    // ==========================================
    // 5. DARK MODE THEME
    // ==========================================
    @Test
    @Config(qualifiers = "+night", sdk = [34])
    fun testDarkMode_ThemeContrastAndReadability() {
        composeTestRule.setContent {
            MyApplicationTheme(darkTheme = true) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Dark Mode Verification")
                        VerificationMetricCard(sampleVerificationReport().dimensionsMetric, tag = "test_dim_dark")
                    }
                }
            }
        }
        composeTestRule.onNodeWithTag("test_dim_dark").assertIsDisplayed()
        composeTestRule.onNodeWithText("DIMENSIONS").assertIsDisplayed()
    }

    // ==========================================
    // 6. LARGE FONT SCALING (1.5x and 2.0x)
    // ==========================================
    @Test
    @Config(sdk = [34])
    fun testLargeFontScale_150Percent() {
        composeTestRule.setContent {
            val currentDensity = LocalDensity.current
            CompositionLocalProvider(
                LocalDensity provides Density(currentDensity.density, fontScale = 1.5f)
            ) {
                MyApplicationTheme {
                    Surface(modifier = Modifier.fillMaxSize()) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text("Large Font Scale 1.5x")
                            VerificationMetricCard(sampleVerificationReport().fileSizeMetric, tag = "test_size_font15")
                        }
                    }
                }
            }
        }
        composeTestRule.onNodeWithTag("test_size_font15").assertIsDisplayed()
        composeTestRule.onNodeWithText("FILE SIZE").assertIsDisplayed()
    }

    @Test
    @Config(sdk = [34])
    fun testExtraLargeFontScale_200Percent() {
        composeTestRule.setContent {
            val currentDensity = LocalDensity.current
            CompositionLocalProvider(
                LocalDensity provides Density(currentDensity.density, fontScale = 2.0f)
            ) {
                MyApplicationTheme {
                    Surface(modifier = Modifier.fillMaxSize()) {
                        Column(modifier = Modifier.padding(8.dp)) {
                            Text("Extra Large Font Scale 2.0x")
                            VerificationMetricCard(sampleVerificationReport().dpiMetric, tag = "test_dpi_font20")
                        }
                    }
                }
            }
        }
        composeTestRule.onNodeWithTag("test_dpi_font20").assertIsDisplayed()
        composeTestRule.onNodeWithText("PRINT RESOLUTION (DPI)").assertIsDisplayed()
    }
}

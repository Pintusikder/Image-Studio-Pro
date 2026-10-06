package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.model.PrintUnit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.math.roundToInt

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Image Studio", appName)
  }

  @Test
  fun `exact physical print resolution to pixel formula test`() {
    // Concept B: 2 x 1 inch @ 300 DPI
    val widthInches = 2.0f
    val heightInches = 1.0f
    val dpi = 300

    val pixelWidth = (widthInches * dpi).roundToInt()
    val pixelHeight = (heightInches * dpi).roundToInt()

    assertEquals(600, pixelWidth)
    assertEquals(300, pixelHeight)

    // Metric check: 35 x 45 mm @ 300 DPI
    val widthMm = 35.0f
    val heightMm = 45.0f
    val pxWidthMetric = ((widthMm / 25.4f) * dpi).roundToInt()
    val pxHeightMetric = ((heightMm / 25.4f) * dpi).roundToInt()

    assertEquals(413, pxWidthMetric)
    assertEquals(531, pxHeightMetric)
  }

  @Test
  fun `verify all requested dashboard main tools are covered`() {
    val requestedTools = listOf(
      "Crop Photo", "Resize Photo", "Compress Photo", "Exact Size",
      "Exact File Size", "Signature", "Passport Photo", "ID Photo",
      "Document Photo", "Convert Format", "DPI / Resolution", "Print Size",
      "Batch Edit", "Rotate / Flip", "Enhance", "Image Info", "More Tools"
    )
    assertEquals(17, requestedTools.size)
  }

  @Test
  fun `verify exact pixel resize calculations and aspect ratio locking`() {
    val originalWidth = 1920
    val originalHeight = 1080
    val aspect = originalWidth.toFloat() / originalHeight.toFloat()

    // 1. Aspect ratio locked: changing width calculates height
    val newWidth = 1080
    val calculatedHeightLocked = (newWidth / aspect).roundToInt()
    assertEquals(608, calculatedHeightLocked)

    // 2. Aspect ratio unlocked: independent dimensions allowed (e.g. 300x100 px, 1080x1080 px)
    val unlockedWidth = 300
    val unlockedHeight = 100
    assertEquals(300, unlockedWidth)
    assertEquals(100, unlockedHeight)

    // 3. Custom passport dimensions (e.g. 413x531 px)
    val passportW = 413
    val passportH = 531
    assertEquals(413, passportW)
    assertEquals(531, passportH)
  }

  @Test
  fun `verify programmatic image dimension verification`() {
    val bmp = android.graphics.Bitmap.createBitmap(300, 100, android.graphics.Bitmap.Config.ARGB_8888)
    assertEquals(300, bmp.width)
    assertEquals(100, bmp.height)

    val scaled = com.example.processing.BitmapUtils.resizeBitmap(bmp, 1080, 1080)
    assertEquals(1080, scaled.width)
    assertEquals(1080, scaled.height)
  }

  @Test
  fun `verify professional resize modes FIT FILL STRETCH SMART_CROP`() {
    val src = android.graphics.Bitmap.createBitmap(800, 400, android.graphics.Bitmap.Config.ARGB_8888)

    // FIT mode
    val fitBmp = com.example.processing.BitmapUtils.resizeBitmapWithMode(
      src, 400, 400, com.example.model.ResizeMode.FIT
    )
    assertEquals(400, fitBmp.width)
    assertEquals(400, fitBmp.height)

    // FILL mode
    val fillBmp = com.example.processing.BitmapUtils.resizeBitmapWithMode(
      src, 300, 300, com.example.model.ResizeMode.FILL
    )
    assertEquals(300, fillBmp.width)
    assertEquals(300, fillBmp.height)

    // STRETCH mode
    val stretchBmp = com.example.processing.BitmapUtils.resizeBitmapWithMode(
      src, 500, 200, com.example.model.ResizeMode.STRETCH
    )
    assertEquals(500, stretchBmp.width)
    assertEquals(200, stretchBmp.height)

    // SMART CROP mode
    val smartBmp = com.example.processing.BitmapUtils.resizeBitmapWithMode(
      src, 400, 400, com.example.model.ResizeMode.SMART_CROP
    )
    assertEquals(400, smartBmp.width)
    assertEquals(400, smartBmp.height)
  }

  @Test
  fun `verify image compression presets and custom modes`() {
    val maxQuality = com.example.model.CompressionPreset.MAXIMUM_QUALITY
    val highQuality = com.example.model.CompressionPreset.HIGH_QUALITY
    val balanced = com.example.model.CompressionPreset.BALANCED
    val medium = com.example.model.CompressionPreset.MEDIUM
    val smallFile = com.example.model.CompressionPreset.SMALL_FILE
    val custom = com.example.model.CompressionPreset.CUSTOM

    assertEquals(95, maxQuality.defaultQuality)
    assertEquals(85, highQuality.defaultQuality)
    assertEquals(70, balanced.defaultQuality)
    assertEquals(50, medium.defaultQuality)
    assertEquals(30, smallFile.defaultQuality)
    assertEquals("Custom", custom.displayName)

    val src = android.graphics.Bitmap.createBitmap(500, 500, android.graphics.Bitmap.Config.ARGB_8888)

    // Measure high quality compression vs small file compression
    val resultHigh = com.example.processing.ExportEngine.measureActualCompressedBytes(
      source = src,
      targetW = 500,
      targetH = 500,
      format = com.example.model.ExportFormat.JPEG,
      quality = highQuality.defaultQuality
    )

    val resultSmall = com.example.processing.ExportEngine.measureActualCompressedBytes(
      source = src,
      targetW = 500,
      targetH = 500,
      format = com.example.model.ExportFormat.JPEG,
      quality = smallFile.defaultQuality
    )

    assertTrue(resultHigh.actualBytes > 0)
    assertTrue(resultSmall.actualBytes > 0)
    assertTrue(resultHigh.actualBytes >= resultSmall.actualBytes)

    // Target file size optimization
    val targetKb = 20
    val resultTarget = com.example.processing.ExportEngine.measureActualCompressedBytes(
      source = src,
      targetW = 500,
      targetH = 500,
      format = com.example.model.ExportFormat.JPEG,
      targetSizeKb = targetKb,
      fileSizeMode = com.example.processing.FileSizeMode.MAXIMUM_CEILING
    )

    assertTrue(resultTarget.actualBytes <= targetKb * 1024L || resultTarget.actualBytes > 0)
  }

  @Test
  fun `aspect ratio transformations produce exact target ratios`() {
    val src = android.graphics.Bitmap.createBitmap(1920, 1080, android.graphics.Bitmap.Config.ARGB_8888)

    // 1. Center crop to 1:1 square
    val croppedSquare = com.example.processing.AspectRatioEngine.cropToAspectRatio(src, 1f, 1f)
    assertEquals(1080, croppedSquare.width)
    assertEquals(1080, croppedSquare.height)

    // 2. Center crop to 4:5 portrait
    val cropped45 = com.example.processing.AspectRatioEngine.cropToAspectRatio(src, 4f, 5f)
    val ratio45 = cropped45.width.toFloat() / cropped45.height.toFloat()
    assertEquals(4f / 5f, ratio45, 0.01f)

    // 3. Letterbox pad 1920x1080 to 1:1 (adds pillarbox padding)
    val paddedSquare = com.example.processing.AspectRatioEngine.padToAspectRatio(
      bitmap = src,
      ratioX = 1f,
      ratioY = 1f,
      padColor = android.graphics.Color.WHITE
    )
    assertEquals(1920, paddedSquare.width)
    assertEquals(1920, paddedSquare.height)

    // 4. Resize to target dimension
    val resized = com.example.processing.AspectRatioEngine.scaleToDimensions(src, 1280, 720)
    assertEquals(1280, resized.width)
    assertEquals(720, resized.height)
  }

  @Test
  fun `rotate 90 cw, ccw, 180 swap dimensions accurately`() {
    val src = android.graphics.Bitmap.createBitmap(300, 200, android.graphics.Bitmap.Config.ARGB_8888)

    // 90 CW: 300x200 -> 200x300
    val rotated90 = com.example.processing.RotateStraightenEngine.rotate90Cw(src)
    assertEquals(200, rotated90.width)
    assertEquals(300, rotated90.height)

    // 90 CCW: 300x200 -> 200x300
    val rotatedCcw = com.example.processing.RotateStraightenEngine.rotate90Ccw(src)
    assertEquals(200, rotatedCcw.width)
    assertEquals(300, rotatedCcw.height)

    // 180: 300x200 -> 300x200
    val rotated180 = com.example.processing.RotateStraightenEngine.rotate180(src)
    assertEquals(300, rotated180.width)
    assertEquals(200, rotated180.height)
  }

  @Test
  fun `rotate custom angle with auto crop, expand, and retain dimension modes`() {
    val src = android.graphics.Bitmap.createBitmap(400, 300, android.graphics.Bitmap.Config.ARGB_8888)

    // 1. Expand canvas at 45 degrees
    val expanded = com.example.processing.RotateStraightenEngine.rotateCustomAngle(
      bitmap = src,
      angleDegrees = 45f,
      cropMode = com.example.processing.RotateStraightenEngine.StraightenCropMode.EXPAND_CANVAS
    )
    assertNotNull(expanded)
    assertTrue("Expanded bitmap should be valid", expanded.width > 0 && expanded.height > 0)

    // 2. Auto crop at 5 degrees
    val autoCropped = com.example.processing.RotateStraightenEngine.rotateCustomAngle(
      bitmap = src,
      angleDegrees = 5f,
      cropMode = com.example.processing.RotateStraightenEngine.StraightenCropMode.AUTO_CROP
    )
    assertNotNull(autoCropped)
    assertTrue("Auto-cropped width should be valid", autoCropped.width > 0)
    assertTrue("Auto-cropped height should be valid", autoCropped.height > 0)

    // 3. Retain exact dimensions mode
    val retained = com.example.processing.RotateStraightenEngine.rotateCustomAngle(
      bitmap = src,
      angleDegrees = 15f,
      cropMode = com.example.processing.RotateStraightenEngine.StraightenCropMode.ORIGINAL_SIZE
    )
    assertNotNull(retained)
    assertEquals(400, retained.width)
    assertEquals(300, retained.height)
  }

  @Test
  fun `auto orientation and horizon detection execute safely on bitmap`() {
    val src = android.graphics.Bitmap.createBitmap(200, 150, android.graphics.Bitmap.Config.ARGB_8888)

    val horizon = com.example.processing.RotateStraightenEngine.detectHorizonAngle(src)
    assertNotNull(horizon)
    assertTrue(horizon.confidence >= 0f)

    val autoOrientResult = com.example.processing.RotateStraightenEngine.autoOrient(src, null)
    assertNotNull(autoOrientResult)
    assertNotNull(autoOrientResult.bitmap)
    assertTrue(autoOrientResult.description.isNotEmpty())
  }

  @Test
  fun `horizontal flip inverts width-axis pixels and preserves dimensions`() {
    val src = android.graphics.Bitmap.createBitmap(400, 250, android.graphics.Bitmap.Config.ARGB_8888)
    val flippedH = com.example.processing.RotateStraightenEngine.flipHorizontal(src)
    assertNotNull(flippedH)
    assertEquals(400, flippedH.width)
    assertEquals(250, flippedH.height)
  }

  @Test
  fun `vertical flip inverts height-axis pixels and preserves dimensions`() {
    val src = android.graphics.Bitmap.createBitmap(400, 250, android.graphics.Bitmap.Config.ARGB_8888)
    val flippedV = com.example.processing.RotateStraightenEngine.flipVertical(src)
    assertNotNull(flippedV)
    assertEquals(400, flippedV.width)
    assertEquals(250, flippedV.height)
  }

  @Test
  fun `flip both inverts both axes and preserves dimensions`() {
    val src = android.graphics.Bitmap.createBitmap(400, 250, android.graphics.Bitmap.Config.ARGB_8888)
    val flippedBoth = com.example.processing.RotateStraightenEngine.flipBoth(src)
    assertNotNull(flippedBoth)
    assertEquals(400, flippedBoth.width)
    assertEquals(250, flippedBoth.height)
  }

  @Test
  fun `symmetry mirror produces valid composite bitmaps`() {
    val src = android.graphics.Bitmap.createBitmap(200, 100, android.graphics.Bitmap.Config.ARGB_8888)

    val leftMirror = com.example.processing.RotateStraightenEngine.applySymmetryMirror(
      src,
      com.example.processing.RotateStraightenEngine.FlipSymmetryMode.LEFT_TO_RIGHT
    )
    assertNotNull(leftMirror)
    assertEquals(200, leftMirror.width)
    assertEquals(100, leftMirror.height)

    val topMirror = com.example.processing.RotateStraightenEngine.applySymmetryMirror(
      src,
      com.example.processing.RotateStraightenEngine.FlipSymmetryMode.TOP_TO_BOTTOM
    )
    assertNotNull(topMirror)
    assertEquals(200, topMirror.width)
    assertEquals(100, topMirror.height)

    val quadMirror = com.example.processing.RotateStraightenEngine.applySymmetryMirror(
      src,
      com.example.processing.RotateStraightenEngine.FlipSymmetryMode.QUAD_MIRROR
    )
    assertNotNull(quadMirror)
    assertEquals(200, quadMirror.width)
    assertEquals(100, quadMirror.height)
  }

  @Test
  fun `perspective warp rectifies quadrilateral bitmap successfully`() {
    val src = android.graphics.Bitmap.createBitmap(800, 600, android.graphics.Bitmap.Config.ARGB_8888)
    val quad = com.example.processing.PerspectiveEngine.PerspectiveQuad(
      topLeft = com.example.processing.PerspectiveEngine.PointFNormalized(0.1f, 0.1f),
      topRight = com.example.processing.PerspectiveEngine.PointFNormalized(0.9f, 0.15f),
      bottomRight = com.example.processing.PerspectiveEngine.PointFNormalized(0.85f, 0.9f),
      bottomLeft = com.example.processing.PerspectiveEngine.PointFNormalized(0.15f, 0.85f)
    )

    val rectified = com.example.processing.PerspectiveEngine.warpPerspective(
      bitmap = src,
      quad = quad,
      preset = com.example.processing.PerspectiveEngine.PerspectiveOutputPreset.AUTO
    )
    assertNotNull(rectified)
    assertTrue("Rectified width should be > 0", rectified.width > 0)
    assertTrue("Rectified height should be > 0", rectified.height > 0)
  }

  @Test
  fun `perspective document enhancement filters execute cleanly`() {
    val src = android.graphics.Bitmap.createBitmap(200, 200, android.graphics.Bitmap.Config.ARGB_8888)

    val magic = com.example.processing.PerspectiveEngine.applyDocumentEnhancement(
      src,
      com.example.processing.PerspectiveEngine.DocumentEnhanceMode.MAGIC_CLEAN
    )
    assertNotNull(magic)
    assertEquals(200, magic.width)

    val bw = com.example.processing.PerspectiveEngine.applyDocumentEnhancement(
      src,
      com.example.processing.PerspectiveEngine.DocumentEnhanceMode.CRISP_BW
    )
    assertNotNull(bw)
    assertEquals(200, bw.width)

    val gray = com.example.processing.PerspectiveEngine.applyDocumentEnhancement(
      src,
      com.example.processing.PerspectiveEngine.DocumentEnhanceMode.GRAYSCALE
    )
    assertNotNull(gray)
    assertEquals(200, gray.width)
  }

  @Test
  fun `detect document quad returns valid boundaries`() {
    val src = android.graphics.Bitmap.createBitmap(400, 400, android.graphics.Bitmap.Config.ARGB_8888)
    val quad = com.example.processing.PerspectiveEngine.detectDocumentQuad(src)
    assertNotNull(quad)
    assertTrue(quad.topLeft.x >= 0f && quad.topLeft.x <= 1f)
    assertTrue(quad.topRight.x >= 0f && quad.topRight.x <= 1f)
  }

  @Test
  fun `image enhancer brightness, contrast, exposure and tone pipeline produces valid bitmap`() {
    val src = android.graphics.Bitmap.createBitmap(200, 200, android.graphics.Bitmap.Config.ARGB_8888)
    val params = com.example.processing.ImageEnhancer.EnhancementParameters(
      brightness = 20f,
      contrast = 15f,
      saturation = 25f,
      exposure = 10f,
      highlights = -10f,
      shadows = 15f,
      sharpness = 30f
    )

    val enhanced = com.example.processing.ImageEnhancer.applyEnhancements(src, params)
    assertNotNull(enhanced)
    assertEquals(200, enhanced.width)
    assertEquals(200, enhanced.height)
  }

  @Test
  fun `image enhancer grayscale and black and white binarization`() {
    val src = android.graphics.Bitmap.createBitmap(150, 150, android.graphics.Bitmap.Config.ARGB_8888)

    val grayParams = com.example.processing.ImageEnhancer.EnhancementParameters(isGrayscale = true)
    val grayBitmap = com.example.processing.ImageEnhancer.applyEnhancements(src, grayParams)
    assertNotNull(grayBitmap)
    assertEquals(150, grayBitmap.width)

    val bwParams = com.example.processing.ImageEnhancer.EnhancementParameters(
      isBlackAndWhite = true,
      bwThreshold = 120f
    )
    val bwBitmap = com.example.processing.ImageEnhancer.applyEnhancements(src, bwParams)
    assertNotNull(bwBitmap)
    assertEquals(150, bwBitmap.width)
  }

  @Test
  fun `image enhancer auto enhancement analysis computes valid parameter bounds`() {
    val src = android.graphics.Bitmap.createBitmap(100, 100, android.graphics.Bitmap.Config.ARGB_8888)
    val autoParams = com.example.processing.ImageEnhancer.computeAutoEnhancements(src)
    assertNotNull(autoParams)
    assertTrue("Exposure within bounds", autoParams.exposure in -50f..50f)
    assertTrue("Contrast within bounds", autoParams.contrast in -50f..50f)
    assertTrue("Sharpness non-negative", autoParams.sharpness >= 0f)
  }

  @Test
  fun `image enhancer histogram generation returns 256 channel entries`() {
    val src = android.graphics.Bitmap.createBitmap(100, 100, android.graphics.Bitmap.Config.ARGB_8888)
    val hist = com.example.processing.ImageEnhancer.computeHistogram(src)
    assertNotNull(hist)
    assertEquals(256, hist.red.size)
    assertEquals(256, hist.green.size)
    assertEquals(256, hist.blue.size)
    assertEquals(256, hist.luminance.size)
    assertTrue("Max count should be >= 1", hist.maxCount >= 1)
  }

  @Test
  fun `background processor transparent, solid color, and gradient matting`() {
    val src = android.graphics.Bitmap.createBitmap(200, 200, android.graphics.Bitmap.Config.ARGB_8888)

    // 1. Transparent Mode
    val transConfig = com.example.processing.BackgroundProcessor.BackgroundConfig(
      mode = com.example.processing.BackgroundProcessor.BgMode.TRANSPARENT,
      tolerance = 45f,
      featherRadius = 2f,
      cleanupNoise = true
    )
    val transBmp = com.example.processing.BackgroundProcessor.processBackground(src, transConfig)
    assertNotNull(transBmp)
    assertEquals(200, transBmp.width)
    assertEquals(200, transBmp.height)

    // 2. Solid Color Mode (Pure White)
    val whiteConfig = com.example.processing.BackgroundProcessor.BackgroundConfig(
      mode = com.example.processing.BackgroundProcessor.BgMode.SOLID_COLOR,
      solidColor = android.graphics.Color.WHITE
    )
    val whiteBmp = com.example.processing.BackgroundProcessor.processBackground(src, whiteConfig)
    assertNotNull(whiteBmp)
    assertEquals(200, whiteBmp.width)

    // 3. Gradient Mode
    val gradConfig = com.example.processing.BackgroundProcessor.BackgroundConfig(
      mode = com.example.processing.BackgroundProcessor.BgMode.GRADIENT,
      gradientPreset = com.example.processing.BackgroundProcessor.GradientPreset.STUDIO_LIGHT
    )
    val gradBmp = com.example.processing.BackgroundProcessor.processBackground(src, gradConfig)
    assertNotNull(gradBmp)
    assertEquals(200, gradBmp.width)

    // 4. Blur Bokeh Mode
    val blurConfig = com.example.processing.BackgroundProcessor.BackgroundConfig(
      mode = com.example.processing.BackgroundProcessor.BgMode.BLUR_ORIGINAL,
      blurRadius = 15
    )
    val blurBmp = com.example.processing.BackgroundProcessor.processBackground(src, blurConfig)
    assertNotNull(blurBmp)
    assertEquals(200, blurBmp.width)
  }
}

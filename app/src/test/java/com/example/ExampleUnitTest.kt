package com.example

import com.example.model.IdDocumentCategory
import com.example.model.PassportPreset
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ExampleUnitTest {
  @Test
  fun addition_isCorrect() {
    assertEquals(4, 2 + 2)
  }

  @Test
  fun idDocumentPresets_haveValidCategoriesAndDimensions() {
    val presets = PassportPreset.PRESETS
    assertTrue("Preset list should not be empty", presets.isNotEmpty())

    val categories = presets.map { it.category }.toSet()
    assertTrue("Should have ID photos", categories.contains(IdDocumentCategory.ID_PHOTO))
    assertTrue("Should have Visa photos", categories.contains(IdDocumentCategory.VISA_PHOTO))
    assertTrue("Should have Application photos", categories.contains(IdDocumentCategory.APPLICATION_PHOTO))
    assertTrue("Should have Job Application photos", categories.contains(IdDocumentCategory.JOB_APPLICATION))
    assertTrue("Should have Exam photos", categories.contains(IdDocumentCategory.EXAM_PHOTO))
    assertTrue("Should have Document photos", categories.contains(IdDocumentCategory.DOCUMENT_PHOTO))

    presets.forEach { preset ->
      assertTrue("Width mm should be positive for ${preset.documentType}", preset.widthMm > 0)
      assertTrue("Height mm should be positive for ${preset.documentType}", preset.heightMm > 0)
      assertTrue("Width px should be positive for ${preset.documentType}", preset.widthPxAt300Dpi > 0)
      assertTrue("Height px should be positive for ${preset.documentType}", preset.heightPxAt300Dpi > 0)
      if (preset.maxFileSizeKb != null) {
        assertTrue("Max file size should be positive for ${preset.documentType}", preset.maxFileSizeKb > 0)
      }
    }
  }

  @Test
  fun examPhotoPresets_includeCommonStandards() {
    val examPresets = PassportPreset.PRESETS.filter { it.category == IdDocumentCategory.EXAM_PHOTO }
    assertTrue("Exam presets should be present", examPresets.isNotEmpty())
    val names = examPresets.map { it.country }
    assertTrue("Should include UPSC", names.contains("UPSC"))
    assertTrue("Should include SSC", names.contains("SSC"))
    assertTrue("Should include NEET / JEE", names.contains("NEET / JEE"))
  }

  @Test
  fun photoSheetPaperPresets_includeRequiredStandards() {
    val presets = com.example.model.SheetPaperPreset.ALL
    val names = presets.map { it.name }

    assertTrue("Should support A4", names.any { it.startsWith("A4") })
    assertTrue("Should support A5", names.any { it.startsWith("A5") })
    assertTrue("Should support Letter", names.any { it.startsWith("Letter") })
    assertTrue("Should support Legal", names.any { it.startsWith("Legal") })
    assertTrue("Should support Custom Paper", presets.any { it.isCustom })

    val a4 = com.example.model.SheetPaperPreset.A4
    assertEquals(210f, a4.widthMm, 1.0f)
    assertEquals(297f, a4.heightMm, 1.0f)

    val letter = com.example.model.SheetPaperPreset.LETTER
    assertEquals(8.5f, letter.widthInches, 0.01f)
    assertEquals(11.0f, letter.heightInches, 0.01f)

    val legal = com.example.model.SheetPaperPreset.LEGAL
    assertEquals(8.5f, legal.widthInches, 0.01f)
    assertEquals(14.0f, legal.heightInches, 0.01f)
  }

  @Test
  fun photoSheetEnums_coverAllRequirements() {
    val cutStyles = com.example.model.CutLineStyle.values()
    assertTrue(cutStyles.contains(com.example.model.CutLineStyle.SOLID))
    assertTrue(cutStyles.contains(com.example.model.CutLineStyle.DASHED))
    assertTrue(cutStyles.contains(com.example.model.CutLineStyle.CORNER_TICKS))
    assertTrue(cutStyles.contains(com.example.model.CutLineStyle.CROSSHAIRS))
    assertTrue(cutStyles.contains(com.example.model.CutLineStyle.NONE))

    val alignments = com.example.model.SheetAlignment.values()
    assertTrue(alignments.contains(com.example.model.SheetAlignment.CENTER))
    assertTrue(alignments.contains(com.example.model.SheetAlignment.TOP_LEFT))
    assertTrue(alignments.contains(com.example.model.SheetAlignment.JUSTIFIED))

    val rotations = com.example.model.PhotoSheetRotation.values()
    assertTrue(rotations.contains(com.example.model.PhotoSheetRotation.ROT_0))
    assertTrue(rotations.contains(com.example.model.PhotoSheetRotation.ROT_90))
    assertTrue(rotations.contains(com.example.model.PhotoSheetRotation.ROT_180))
    assertTrue(rotations.contains(com.example.model.PhotoSheetRotation.ROT_270))

    val exportFormats = com.example.model.ExportFormat.values()
    assertTrue(exportFormats.contains(com.example.model.ExportFormat.JPEG))
    assertTrue(exportFormats.contains(com.example.model.ExportFormat.PNG))
    assertTrue(exportFormats.contains(com.example.model.ExportFormat.PDF))
  }

  @Test
  fun aspectRatioEngine_analyzesDimensionsAndRatiosCorrectly() {
    // 16:9 Landscape
    val res169 = com.example.processing.AspectRatioEngine.analyzeAspectRatio(1920, 1080)
    assertEquals(1920, res169.width)
    assertEquals(1080, res169.height)
    assertEquals("16:9", res169.simplifiedRatioString)
    assertEquals(16, res169.simplifiedX)
    assertEquals(9, res169.simplifiedY)
    assertEquals(16f / 9f, res169.decimalRatio, 0.001f)
    assertEquals(com.example.processing.AspectOrientation.LANDSCAPE, res169.orientation)
    assertEquals("16:9", res169.nearestPreset?.displayRatio)

    // 9:16 Portrait
    val res916 = com.example.processing.AspectRatioEngine.analyzeAspectRatio(1080, 1920)
    assertEquals("9:16", res916.simplifiedRatioString)
    assertEquals(com.example.processing.AspectOrientation.PORTRAIT, res916.orientation)

    // 1:1 Square
    val res11 = com.example.processing.AspectRatioEngine.analyzeAspectRatio(1080, 1080)
    assertEquals("1:1", res11.simplifiedRatioString)
    assertEquals(com.example.processing.AspectOrientation.SQUARE, res11.orientation)

    // 4:3 Photo
    val res43 = com.example.processing.AspectRatioEngine.analyzeAspectRatio(1600, 1200)
    assertEquals("4:3", res43.simplifiedRatioString)
  }

  @Test
  fun aspectRatioEngine_presetsAndCategoriesAreComprehensive() {
    val presets = com.example.processing.AspectRatioEngine.ALL_PRESETS
    assertTrue("Presets must not be empty", presets.isNotEmpty())

    val ratios = presets.map { it.displayRatio }
    assertTrue("Should include 1:1", ratios.contains("1:1"))
    assertTrue("Should include 16:9", ratios.contains("16:9"))
    assertTrue("Should include 9:16", ratios.contains("9:16"))
    assertTrue("Should include 4:5", ratios.contains("4:5"))
    assertTrue("Should include 3:2", ratios.contains("3:2"))
    assertTrue("Should include 4:3", ratios.contains("4:3"))
    assertTrue("Should include 21:9", ratios.contains("21:9"))

    val categories = com.example.processing.AspectRatioEngine.CATEGORIES
    assertTrue(categories.contains("All"))
    assertTrue(categories.contains("Popular"))
    assertTrue(categories.contains("Social"))
    assertTrue(categories.contains("Photo & Print"))
    assertTrue(categories.contains("Display & Cinema"))
  }

  @Test
  fun aspectRatioEngine_dimensionSolverCalculatesLinkedDimensions() {
    // When 16:9 ratio is locked
    val ratioX = 16f
    val ratioY = 9f

    // Setting width to 1280 should give 720
    val heightFromWidth = com.example.processing.AspectRatioEngine.calculateHeightFromWidth(
      width = 1280,
      ratioX = ratioX,
      ratioY = ratioY
    )
    assertEquals(720, heightFromWidth)

    // Setting height to 1080 should give 1920
    val widthFromHeight = com.example.processing.AspectRatioEngine.calculateWidthFromHeight(
      height = 1080,
      ratioX = ratioX,
      ratioY = ratioY
    )
    assertEquals(1920, widthFromHeight)
  }

  @Test
  fun rotateStraightenEngine_inscribedCropCalculation_returnsProperScale() {
    // 0 degrees should have scale 1.0 (no crop needed)
    val scale0 = com.example.processing.RotateStraightenEngine.calculateInscribedCropFraction(1000, 1000, 0f)
    assertEquals(1.0f, scale0, 0.001f)

    // A small 3 degree tilt should return a crop fraction around 0.94 - 0.98
    val scale3 = com.example.processing.RotateStraightenEngine.calculateInscribedCropFraction(1920, 1080, 3f)
    assertTrue("Scale at 3° tilt should be between 0.85 and 0.99", scale3 in 0.85f..0.99f)

    // A 45 degree tilt on a square should calculate inscribed crop factor
    val scale45 = com.example.processing.RotateStraightenEngine.calculateInscribedCropFraction(1000, 1000, 45f)
    assertTrue("Scale at 45° tilt should be less than 0.8", scale45 < 0.8f)
  }

  @Test
  fun rotateStraightenEngine_gridTypeAttributes_areValid() {
    val rotTypes = com.example.processing.RotateStraightenEngine.GridType.values()
    assertEquals(4, rotTypes.size)

    val thirds = com.example.processing.RotateStraightenEngine.GridType.RULE_OF_THIRDS
    assertEquals(3, thirds.cols)
    assertEquals(3, thirds.rows)

    val fine = com.example.processing.RotateStraightenEngine.GridType.FINE_GRID
    assertEquals(6, fine.cols)
    assertEquals(6, fine.rows)
  }

  @Test
  fun rotateStraightenEngine_straightenCropModes_arePresent() {
    val modes = com.example.processing.RotateStraightenEngine.StraightenCropMode.values()
    assertEquals(3, modes.size)
    assertTrue(modes.contains(com.example.processing.RotateStraightenEngine.StraightenCropMode.AUTO_CROP))
    assertTrue(modes.contains(com.example.processing.RotateStraightenEngine.StraightenCropMode.EXPAND_CANVAS))
    assertTrue(modes.contains(com.example.processing.RotateStraightenEngine.StraightenCropMode.ORIGINAL_SIZE))
  }

  @Test
  fun rotateStraightenEngine_horizonDetectionResult_evaluatesLevel() {
    val levelResult = com.example.processing.RotateStraightenEngine.HorizonDetectionResult(
      angleDegrees = 0.1f,
      confidence = 90f,
      description = "Level horizon"
    )
    assertTrue("0.1° should be evaluated as level", levelResult.isLevel)

    val tiltedResult = com.example.processing.RotateStraightenEngine.HorizonDetectionResult(
      angleDegrees = 3.2f,
      confidence = 85f,
      description = "Tilted horizon"
    )
    assertFalse("3.2° should be evaluated as tilted", tiltedResult.isLevel)
  }

  @Test
  fun rotateStraightenEngine_flipSymmetryModes_areAllDefined() {
    val modes = com.example.processing.RotateStraightenEngine.FlipSymmetryMode.values()
    assertEquals(6, modes.size)
    assertTrue(modes.contains(com.example.processing.RotateStraightenEngine.FlipSymmetryMode.NONE))
    assertTrue(modes.contains(com.example.processing.RotateStraightenEngine.FlipSymmetryMode.LEFT_TO_RIGHT))
    assertTrue(modes.contains(com.example.processing.RotateStraightenEngine.FlipSymmetryMode.RIGHT_TO_LEFT))
    assertTrue(modes.contains(com.example.processing.RotateStraightenEngine.FlipSymmetryMode.TOP_TO_BOTTOM))
    assertTrue(modes.contains(com.example.processing.RotateStraightenEngine.FlipSymmetryMode.BOTTOM_TO_TOP))
    assertTrue(modes.contains(com.example.processing.RotateStraightenEngine.FlipSymmetryMode.QUAD_MIRROR))
  }

  @Test
  fun perspectiveEngine_quadCorners_andPresets_workCorrectly() {
    val defaultQuad = com.example.processing.PerspectiveEngine.PerspectiveQuad.default()
    assertEquals(0.08f, defaultQuad.topLeft.x, 0.001f)
    assertEquals(0.08f, defaultQuad.topLeft.y, 0.001f)
    assertEquals(0.92f, defaultQuad.bottomRight.x, 0.001f)
    assertEquals(0.92f, defaultQuad.bottomRight.y, 0.001f)

    val updatedQuad = defaultQuad.withCorner(
      com.example.processing.PerspectiveEngine.PerspectiveCorner.TOP_LEFT,
      com.example.processing.PerspectiveEngine.PointFNormalized(0.12f, 0.15f)
    )
    assertEquals(0.12f, updatedQuad.topLeft.x, 0.001f)
    assertEquals(0.15f, updatedQuad.topLeft.y, 0.001f)

    val dims = com.example.processing.PerspectiveEngine.calculateOutputDimensions(
      bitmapWidth = 1000,
      bitmapHeight = 1500,
      quad = defaultQuad,
      preset = com.example.processing.PerspectiveEngine.PerspectiveOutputPreset.AUTO
    )
    assertTrue("Output width should be positive", dims.first > 0)
    assertTrue("Output height should be positive", dims.second > 0)

    val a4Dims = com.example.processing.PerspectiveEngine.calculateOutputDimensions(
      bitmapWidth = 1000,
      bitmapHeight = 1500,
      quad = defaultQuad,
      preset = com.example.processing.PerspectiveEngine.PerspectiveOutputPreset.A4_PORTRAIT
    )
    assertTrue("A4 height should be greater than width", a4Dims.second > a4Dims.first)

    val presets = com.example.processing.PerspectiveEngine.PerspectiveOutputPreset.values()
    assertTrue("Presets should include A4, US Letter, Receipt, Certificate", presets.size >= 7)
  }

  @Test
  fun imageEnhancer_parameters_neutralCheck_andDefaults() {
    val defaultParams = com.example.processing.ImageEnhancer.EnhancementParameters.DEFAULT
    assertTrue("Default params should be neutral", defaultParams.isNeutral)
    assertEquals(0f, defaultParams.brightness, 0.001f)
    assertEquals(0f, defaultParams.contrast, 0.001f)
    assertEquals(0f, defaultParams.exposure, 0.001f)
    assertEquals(0f, defaultParams.highlights, 0.001f)
    assertEquals(0f, defaultParams.shadows, 0.001f)
    assertEquals(0f, defaultParams.sharpness, 0.001f)
    assertFalse(defaultParams.isGrayscale)
    assertFalse(defaultParams.isBlackAndWhite)

    val modifiedParams = defaultParams.copy(brightness = 15f)
    assertFalse("Modified params should not be neutral", modifiedParams.isNeutral)

    val bwParams = defaultParams.copy(isBlackAndWhite = true, bwThreshold = 140f)
    assertFalse("BW params should not be neutral", bwParams.isNeutral)
    assertEquals(140f, bwParams.bwThreshold, 0.001f)
  }

  @Test
  fun backgroundProcessor_modesAndPresets_coverage() {
    val modes = com.example.processing.BackgroundProcessor.BgMode.values()
    assertTrue(modes.contains(com.example.processing.BackgroundProcessor.BgMode.TRANSPARENT))
    assertTrue(modes.contains(com.example.processing.BackgroundProcessor.BgMode.SOLID_COLOR))
    assertTrue(modes.contains(com.example.processing.BackgroundProcessor.BgMode.GRADIENT))
    assertTrue(modes.contains(com.example.processing.BackgroundProcessor.BgMode.BLUR_ORIGINAL))
    assertTrue(modes.contains(com.example.processing.BackgroundProcessor.BgMode.CUSTOM_IMAGE))

    val presets = com.example.processing.BackgroundProcessor.GradientPreset.values()
    assertTrue("Gradient presets should include Studio Light & Warm Sunset", presets.size >= 5)

    val methods = com.example.processing.BackgroundProcessor.RemovalMethod.values()
    assertTrue(methods.contains(com.example.processing.BackgroundProcessor.RemovalMethod.SMART_EDGE))
    assertTrue(methods.contains(com.example.processing.BackgroundProcessor.RemovalMethod.FLOOD_FILL))
    assertTrue(methods.contains(com.example.processing.BackgroundProcessor.RemovalMethod.LUMINANCE_KEY))

    val defaultConfig = com.example.processing.BackgroundProcessor.BackgroundConfig()
    assertEquals(com.example.processing.BackgroundProcessor.BgMode.SOLID_COLOR, defaultConfig.mode)
    assertEquals(42f, defaultConfig.tolerance, 0.001f)
    assertTrue(defaultConfig.cleanupNoise)
  }

  @Test
  fun metadataPrivacyConfig_policiesAndScrubbingLogic() {
    val keepAll = com.example.model.MetadataPrivacyConfig(
      policy = com.example.model.MetadataPolicy.KEEP_ALL,
      removeGps = false,
      removeCameraInfo = false,
      removeDeviceInfo = false,
      removeDateMetadata = false
    )
    assertFalse(keepAll.removeGps)
    assertFalse(keepAll.removeCameraInfo)
    assertFalse(keepAll.removeDeviceInfo)
    assertFalse(keepAll.removeDateMetadata)
    assertEquals(0, keepAll.scrubbedCategoriesCount)

    val stripAll = com.example.model.MetadataPrivacyConfig(
      policy = com.example.model.MetadataPolicy.STRIP_ALL,
      removeGps = true,
      removeCameraInfo = true,
      removeDeviceInfo = true,
      removeDateMetadata = true
    )
    assertTrue(stripAll.removeGps)
    assertTrue(stripAll.removeCameraInfo)
    assertTrue(stripAll.removeDeviceInfo)
    assertTrue(stripAll.removeDateMetadata)
    assertEquals(4, stripAll.scrubbedCategoriesCount)

    val custom = com.example.model.MetadataPrivacyConfig(
      policy = com.example.model.MetadataPolicy.CUSTOM_SELECTIVE,
      removeGps = true,
      removeCameraInfo = false,
      removeDeviceInfo = true,
      removeDateMetadata = false
    )
    assertTrue(custom.removeGps)
    assertFalse(custom.removeCameraInfo)
    assertTrue(custom.removeDeviceInfo)
    assertFalse(custom.removeDateMetadata)
    assertEquals(2, custom.scrubbedCategoriesCount)
  }

  @Test
  fun exifManager_formatDisclosures_areHonestAndComplete() {
    val jpegDisclosure = com.example.processing.ExifManager.getFormatDisclosure(com.example.model.ExportFormat.JPEG)
    assertTrue(jpegDisclosure.canEmbedExif)
    assertTrue(jpegDisclosure.canStripGps)
    assertTrue(jpegDisclosure.canStripCamera)
    assertTrue(jpegDisclosure.canStripDates)
    assertTrue(jpegDisclosure.technicalHonestyNote.contains("ISO/IEC 10918-1"))

    val pngDisclosure = com.example.processing.ExifManager.getFormatDisclosure(com.example.model.ExportFormat.PNG)
    assertFalse(pngDisclosure.canEmbedExif)
    assertTrue(pngDisclosure.canStripGps)
    assertTrue(pngDisclosure.formatStructuralHeadersRetained.contains("IHDR"))
    assertTrue(pngDisclosure.formatStructuralHeadersRetained.contains("pHYs"))

    val webpLossyDisclosure = com.example.processing.ExifManager.getFormatDisclosure(com.example.model.ExportFormat.WEBP_LOSSY)
    assertTrue(webpLossyDisclosure.canEmbedExif)
    assertTrue(webpLossyDisclosure.canStripGps)

    val pdfDisclosure = com.example.processing.ExifManager.getFormatDisclosure(com.example.model.ExportFormat.PDF)
    assertFalse(pdfDisclosure.canEmbedExif)
    assertTrue(pdfDisclosure.canStripGps)
    assertTrue(pdfDisclosure.formatStructuralHeadersRetained.contains("PDF Document catalog"))
  }

  @Test
  fun exifTags_coverageAndCompleteness() {
    val gpsTags = com.example.processing.ExifManager.GPS_TAGS
    assertTrue("Should include Latitude", gpsTags.contains(androidx.exifinterface.media.ExifInterface.TAG_GPS_LATITUDE))
    assertTrue("Should include Longitude", gpsTags.contains(androidx.exifinterface.media.ExifInterface.TAG_GPS_LONGITUDE))
    assertTrue("Should include Altitude", gpsTags.contains(androidx.exifinterface.media.ExifInterface.TAG_GPS_ALTITUDE))

    val cameraTags = com.example.processing.ExifManager.CAMERA_TAGS
    assertTrue("Should include Make", cameraTags.contains(androidx.exifinterface.media.ExifInterface.TAG_MAKE))
    assertTrue("Should include Model", cameraTags.contains(androidx.exifinterface.media.ExifInterface.TAG_MODEL))

    val deviceTags = com.example.processing.ExifManager.DEVICE_TAGS
    assertTrue("Should include Maker Note", deviceTags.contains(androidx.exifinterface.media.ExifInterface.TAG_MAKER_NOTE))

    val dateTags = com.example.processing.ExifManager.DATE_TAGS
    assertTrue("Should include DateTime", dateTags.contains(androidx.exifinterface.media.ExifInterface.TAG_DATETIME))
    assertTrue("Should include DateTimeOriginal", dateTags.contains(androidx.exifinterface.media.ExifInterface.TAG_DATETIME_ORIGINAL))
  }

  @Test
  fun imageInfoAnalyzer_computesCompleteMetadataAccurately() {
    val context = org.robolectric.RuntimeEnvironment.getApplication()
    val testBitmap = android.graphics.Bitmap.createBitmap(1920, 1080, android.graphics.Bitmap.Config.ARGB_8888)

    val sampleExif = com.example.model.ImageExifData(
      make = "Sony",
      model = "ILCE-7M4",
      lensModel = "FE 24-70mm F2.8 GM II",
      dateTimeOriginal = "2026:09:16 10:24:00",
      dateTimeDigitized = "2026:09:16 10:24:00",
      dateTime = "2026:09:16 10:25:30",
      shutterSpeed = "1/250",
      aperture = "2.8",
      iso = "100",
      focalLength = "50.0",
      software = "Lightroom Classic",
      artist = "Studio Pro",
      copyright = "Copyright 2026 Studio",
      hasGps = true,
      latitude = 37.7749,
      longitude = -122.4194
    )

    val info = com.example.processing.ImageInfoAnalyzer.analyze(
      context = context,
      uri = null,
      bitmap = testBitmap,
      originalWidth = 3840,
      originalHeight = 2160,
      originalFileSize = 2_458_120L,
      originalMime = "image/jpeg",
      originalDpi = 300,
      exif = sampleExif,
      currentDpi = 300,
      detectedFormat = com.example.model.DetectedImageFormat.JPEG
    )

    // 1. Filename & Size
    assertTrue(info.fileName.isNotEmpty())
    assertEquals(2_458_120L, info.fileSizeBytes)
    assertTrue("File size should format in MB", info.formattedFileSize.contains("MB"))

    // 2. Width, Height, Megapixels
    assertEquals(1920, info.width)
    assertEquals(1080, info.height)
    assertEquals(3840, info.originalWidth)
    assertEquals(2160, info.originalHeight)
    assertEquals((1920f * 1080f) / 1_000_000f, info.megapixels, 0.01f)

    // 3. Aspect Ratio
    assertEquals("16:9", info.aspectRatioRatioString)
    assertEquals(16f / 9f, info.aspectRatioDecimal, 0.01f)
    assertEquals("Landscape", info.orientationCategory)
    assertNotNull(info.aspectRatioStandardName)

    // 4. Format & MIME
    assertEquals("JPEG Image", info.formatDisplayName)
    assertEquals("image/jpeg", info.mimeType)

    // 5. DPI & Print Size
    assertEquals(300, info.dpi)
    assertEquals(1920f / 300f, info.printWidthInches, 0.01f)
    assertEquals(1080f / 300f, info.printHeightInches, 0.01f)
    assertEquals((1920f / 300f) * 2.54f, info.printWidthCm, 0.02f)
    assertEquals((1080f / 300f) * 2.54f, info.printHeightCm, 0.02f)

    // 6. Orientation
    assertEquals("Normal (0°)", info.exifOrientationDescription)

    // 7. Color Information
    assertTrue("Color model should describe RGBA", info.colorModel.contains("RGBA"))
    assertTrue("Bit depth should specify 32-bit", info.bitDepth.contains("32-bit"))
    assertTrue(info.dominantColors.isNotEmpty())
    assertNotNull(info.dominantColors.first().hex)

    // 8. EXIF Availability & Creation Details
    assertTrue(info.exifAvailable)
    assertTrue(info.exifTagCount > 0)
    assertTrue(info.hasGps)
    assertTrue(info.hasCameraInfo)
    assertTrue(info.hasDateInfo)
    assertTrue(info.hasAuthorCopyright)
    assertEquals("Sony", info.cameraMake)
    assertEquals("ILCE-7M4", info.cameraModel)
    assertEquals("FE 24-70mm F2.8 GM II", info.lensModel)
    assertEquals("2026:09:16 10:24:00", info.creationDateTimeOriginal)
    assertNotNull(info.exposureSummary)
    assertTrue(info.exposureSummary!!.contains("1/250s"))
    assertNotNull(info.gpsCoordinatesFormatted)
    assertTrue(info.gpsCoordinatesFormatted!!.contains("37.7749° N"))
  }
}


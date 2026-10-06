package com.example

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import androidx.test.core.app.ApplicationProvider
import com.example.domain.usecase.BatchProcessingUseCase
import com.example.model.BatchCompressionMode
import com.example.model.BatchConfig
import com.example.model.BatchFilterOption
import com.example.model.BatchResizeOption
import com.example.model.BatchSortOption
import com.example.model.BatchStatus
import com.example.model.CompressionPreset
import com.example.model.DetectedImageFormat
import com.example.model.ExportFormat
import com.example.model.MetadataPolicy
import com.example.model.NamingMode
import com.example.model.OutputFileConfiguration
import com.example.model.PrintUnit
import com.example.model.ResizeMode
import com.example.processing.BatchProcessingEngine
import com.example.processing.DpiPrintEngine
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.atomic.AtomicBoolean

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class Phase9BatchProcessingTest {

    private lateinit var context: Context
    private lateinit var batchUseCase: BatchProcessingUseCase

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        batchUseCase = BatchProcessingUseCase()
    }

    private fun createTestImageFile(
        name: String,
        width: Int,
        height: Int,
        format: Bitmap.CompressFormat = Bitmap.CompressFormat.JPEG,
        withExifAndGps: Boolean = false
    ): File {
        val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val pixels = IntArray(width * height)
        for (y in 0 until height) {
            for (x in 0 until width) {
                val r = (x * 255) / width.coerceAtLeast(1)
                val g = (y * 255) / height.coerceAtLeast(1)
                val b = ((x + y) * 127) / (width + height).coerceAtLeast(1)
                pixels[y * width + x] = Color.argb(255, r, g, b)
            }
        }
        bmp.setPixels(pixels, 0, width, 0, 0, width, height)

        val file = File(context.cacheDir, name)
        FileOutputStream(file).use { fos ->
            bmp.compress(format, 92, fos)
            fos.flush()
        }
        bmp.recycle()

        if (withExifAndGps && format == Bitmap.CompressFormat.JPEG) {
            val exif = ExifInterface(file.absolutePath)
            exif.setAttribute(ExifInterface.TAG_MAKE, "Canon")
            exif.setAttribute(ExifInterface.TAG_MODEL, "EOS R5")
            exif.setAttribute(ExifInterface.TAG_GPS_LATITUDE, "37/1,46/1,30/1")
            exif.setAttribute(ExifInterface.TAG_GPS_LATITUDE_REF, "N")
            exif.setAttribute(ExifInterface.TAG_GPS_LONGITUDE, "122/1,25/1,10/1")
            exif.setAttribute(ExifInterface.TAG_GPS_LONGITUDE_REF, "W")
            exif.saveAttributes()
        }
        return file
    }

    @Test
    fun test1_MultiSelect_Inspection_Deduplication_Selection_Sort_And_Filter() {
        val fileA = createTestImageFile("alpha_photo.jpg", 1200, 800, Bitmap.CompressFormat.JPEG, withExifAndGps = true)
        val fileB = createTestImageFile("beta_icon.png", 600, 600, Bitmap.CompressFormat.PNG)
        val fileC = createTestImageFile("gamma_banner.jpg", 1920, 1080, Bitmap.CompressFormat.JPEG)

        val uris = listOf(Uri.fromFile(fileA), Uri.fromFile(fileB), Uri.fromFile(fileC), Uri.fromFile(fileA))

        // 1. Inspect with deduplication enabled
        val items = batchUseCase.inspectAndCreateItems(
            context = context,
            uris = uris,
            existingItems = emptyList(),
            skipDuplicates = true
        )
        assertEquals("Duplicate URI should be skipped", 3, items.size)
        assertEquals(DetectedImageFormat.JPEG, items[0].detectedFormat)
        assertEquals(DetectedImageFormat.PNG, items[1].detectedFormat)
        assertTrue("First item should detect EXIF/GPS", items[0].hasExif && items[0].hasGps)
        assertTrue("All items start selected", items.all { it.selected })

        // 2. Selection operations: deselectAll, toggleSelection, invertSelection, selectAll, removeSelected
        val deselected = batchUseCase.deselectAll(items)
        assertTrue(deselected.none { it.selected })

        val toggledOne = batchUseCase.toggleSelection(deselected, deselected[1].id)
        assertEquals(1, toggledOne.count { it.selected })
        assertTrue(toggledOne[1].selected)

        val inverted = batchUseCase.invertSelection(toggledOne)
        assertEquals(2, inverted.count { it.selected })
        assertFalse(inverted[1].selected)

        val remainingAfterRemove = batchUseCase.removeSelected(inverted)
        assertEquals(1, remainingAfterRemove.size)
        assertEquals(items[1].id, remainingAfterRemove[0].id)

        // 3. Sorting & Filtering
        val sortedByRes = batchUseCase.sortItems(items, BatchSortOption.RESOLUTION_DESC)
        assertEquals("gamma_banner.jpg", sortedByRes.first().originalName)
        assertEquals("beta_icon.png", sortedByRes.last().originalName)

        val filteredSelected = batchUseCase.filterItems(toggledOne, BatchFilterOption.SELECTED)
        assertEquals(1, filteredSelected.size)
    }

    @Test
    fun test2_BatchResize_AllModes_And_DoNotUpscale() {
        // 1. Original
        val origDims = batchUseCase.computeTargetDimensions(1600, 1200, BatchConfig(resizeOption = BatchResizeOption.ORIGINAL))
        assertEquals(1600 to 1200, origDims)

        // 2. Percentage (50%)
        val pctDims = batchUseCase.computeTargetDimensions(
            1600,
            1200,
            BatchConfig(resizeOption = BatchResizeOption.PERCENTAGE, scalePercent = 50)
        )
        assertEquals(800 to 600, pctDims)

        // 3. Same Width (1000px proportional)
        val sameWDims = batchUseCase.computeTargetDimensions(
            2000,
            1000,
            BatchConfig(resizeOption = BatchResizeOption.SAME_WIDTH, sameWidth = 1000)
        )
        assertEquals(1000 to 500, sameWDims)

        // 4. Same Height (900px proportional)
        val sameHDims = batchUseCase.computeTargetDimensions(
            1200,
            600,
            BatchConfig(resizeOption = BatchResizeOption.SAME_HEIGHT, sameHeight = 900)
        )
        assertEquals(1800 to 900, sameHDims)

        // 5. Longest Edge (1080px on both landscape and portrait)
        val longestLandscape = batchUseCase.computeTargetDimensions(
            2160,
            1080,
            BatchConfig(resizeOption = BatchResizeOption.LONGEST_EDGE, longestEdge = 1080)
        )
        assertEquals(1080 to 540, longestLandscape)

        val longestPortrait = batchUseCase.computeTargetDimensions(
            1000,
            2000,
            BatchConfig(resizeOption = BatchResizeOption.LONGEST_EDGE, longestEdge = 1000)
        )
        assertEquals(500 to 1000, longestPortrait)

        // 6. Do Not Upscale safeguard
        val noUpscaleDims = batchUseCase.computeTargetDimensions(
            640,
            480,
            BatchConfig(resizeOption = BatchResizeOption.LONGEST_EDGE, longestEdge = 1920, doNotUpscale = true)
        )
        assertEquals(640 to 480, noUpscaleDims)

        // 7. Physical Print Size (4x6 inches @ 300 DPI = 1200x1800 px)
        val printDims = batchUseCase.computeTargetDimensions(
            800,
            1200,
            BatchConfig(
                resizeOption = BatchResizeOption.PHYSICAL_PRINT,
                physicalWidth = 4.0f,
                physicalHeight = 6.0f,
                physicalUnit = PrintUnit.INCHES,
                dpi = 300
            )
        )
        assertEquals(1200 to 1800, printDims)
    }

    @Test
    fun test3_BatchExecution_Compression_FormatConversion_And_Metadata() = runBlocking {
        val f1 = createTestImageFile("batch_src1.jpg", 1000, 800, Bitmap.CompressFormat.JPEG, withExifAndGps = true)
        val f2 = createTestImageFile("batch_src2.png", 800, 600, Bitmap.CompressFormat.PNG)

        val initialItems = batchUseCase.inspectAndCreateItems(
            context = context,
            uris = listOf(Uri.fromFile(f1), Uri.fromFile(f2))
        )

        val config = BatchConfig(
            resizeOption = BatchResizeOption.SAME_WIDTH,
            sameWidth = 500,
            compressionMode = BatchCompressionMode.PRESET,
            compressionPreset = CompressionPreset.BALANCED,
            keepOriginalFormat = false,
            format = ExportFormat.JPEG,
            metadataPolicy = MetadataPolicy.CUSTOM_SELECTIVE,
            removeGps = true,
            removeCameraInfo = false,
            customArtist = "StudioBatchTester",
            customCopyright = "Copyright 2026",
            dpi = 300,
            injectBinaryDpi = true
        )

        val stagesObserved = mutableListOf<String>()
        val results = batchUseCase.executeBatch(
            context = context,
            items = initialItems,
            config = config,
            outputFileConfig = OutputFileConfiguration(
                namingMode = NamingMode.CUSTOM_PATTERN,
                customTemplate = "{name}_batch_{index}"
            ),
            onProgress = { _, _, _, stage, _ ->
                stagesObserved.add(stage)
            }
        )

        assertEquals(2, results.size)
        assertTrue("All items should complete", results.all { it.status == BatchStatus.COMPLETED })
        assertTrue("Progress stages should be emitted", stagesObserved.isNotEmpty())

        // Verify resized dimensions (width = 500, proportional height)
        assertEquals(500, results[0].outputWidth)
        assertEquals(400, results[0].outputHeight)
        assertEquals(500, results[1].outputWidth)
        assertEquals(375, results[1].outputHeight)

        // Verify metadata & binary DPI on first output file
        val outFile1 = File(context.cacheDir, results[0].outputFileName!!)
        assertTrue("Output file should exist in cache", outFile1.exists())
        val exifOut1 = ExifInterface(outFile1.absolutePath)
        val latLong = FloatArray(2)
        assertFalse("GPS coordinates should be scrubbed by CUSTOM_SELECTIVE removeGps=true", exifOut1.getLatLong(latLong))
        assertEquals("StudioBatchTester", exifOut1.getAttribute(ExifInterface.TAG_ARTIST))
        assertEquals("Copyright 2026", exifOut1.getAttribute(ExifInterface.TAG_COPYRIGHT))

        val dpiInspection = DpiPrintEngine.verifyFileDpiAndPhysicalDimensions(
            file = outFile1,
            pixelWidth = 500,
            pixelHeight = 400,
            expectedDpi = 300,
            format = ExportFormat.JPEG
        )
        assertTrue(dpiInspection.isVerified)
        assertEquals(300, dpiInspection.detectedDpiX)

        // Verify summary report
        val summary = batchUseCase.computeSummary(results)
        assertEquals(2, summary.completedItems)
        assertEquals(0, summary.failedItems)
        assertTrue(summary.totalOutputBytes > 0L)
    }

    @Test
    fun test4_BatchCancel_And_RetrySingleAndAll() = runBlocking {
        val f1 = createTestImageFile("cancel_test1.jpg", 600, 400)
        val f2 = createTestImageFile("cancel_test2.jpg", 600, 400)
        val f3 = createTestImageFile("cancel_test3.jpg", 600, 400)

        val initialItems = batchUseCase.inspectAndCreateItems(
            context = context,
            uris = listOf(Uri.fromFile(f1), Uri.fromFile(f2), Uri.fromFile(f3))
        )

        // Trigger cancellation right after the first item finishes
        val cancelFlag = AtomicBoolean(false)
        val afterCancel = batchUseCase.executeBatch(
            context = context,
            items = initialItems,
            config = BatchConfig(resizeOption = BatchResizeOption.PERCENTAGE, scalePercent = 50),
            cancelFlag = cancelFlag,
            onProgress = { updatedList, _, _, _, _ ->
                if (updatedList.count { it.status == BatchStatus.COMPLETED } >= 1) {
                    cancelFlag.set(true)
                }
            }
        )

        assertEquals("First item should have completed before cancel", BatchStatus.COMPLETED, afterCancel[0].status)
        assertEquals("Second item should be marked CANCELLED", BatchStatus.CANCELLED, afterCancel[1].status)
        assertEquals("Third item should be marked CANCELLED", BatchStatus.CANCELLED, afterCancel[2].status)

        // Retry single item (item index 1)
        val singleRetryPrepared = batchUseCase.prepareRetrySingleItem(afterCancel, afterCancel[1].id)
        assertEquals(BatchStatus.PENDING, singleRetryPrepared[1].status)
        assertEquals(1, singleRetryPrepared[1].retryCount)

        val afterSingleRetry = batchUseCase.executeBatch(
            context = context,
            items = singleRetryPrepared,
            config = BatchConfig(resizeOption = BatchResizeOption.PERCENTAGE, scalePercent = 50),
            targetItemId = singleRetryPrepared[1].id
        )
        assertEquals(BatchStatus.COMPLETED, afterSingleRetry[0].status)
        assertEquals(BatchStatus.COMPLETED, afterSingleRetry[1].status)
        assertEquals(BatchStatus.CANCELLED, afterSingleRetry[2].status)

        // Retry all remaining failed/cancelled items
        val retryAllPrepared = batchUseCase.prepareRetryFailedAndCancelled(afterSingleRetry)
        assertEquals(BatchStatus.PENDING, retryAllPrepared[2].status)

        val afterRetryAll = batchUseCase.executeBatch(
            context = context,
            items = retryAllPrepared,
            config = BatchConfig(resizeOption = BatchResizeOption.PERCENTAGE, scalePercent = 50)
        )
        assertTrue("All 3 items should now be COMPLETED", afterRetryAll.all { it.status == BatchStatus.COMPLETED })
        assertNotNull(afterRetryAll[2].outputUri)
    }
}

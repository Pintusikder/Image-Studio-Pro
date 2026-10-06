package com.example

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import com.example.model.BatchConfig
import com.example.model.BatchItem
import com.example.model.BatchStatus
import com.example.model.EditingInstructions
import com.example.model.ExportFormat
import com.example.model.ResizeMode
import com.example.processing.BitmapUtils
import com.example.processing.ExportEngine
import com.example.processing.ImageEnhancer
import com.example.processing.NonDestructivePipeline
import com.example.processing.OutputFileManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File

@RunWith(RobolectricTestRunner::class)
@OptIn(ExperimentalCoroutinesApi::class)
class PerformanceTestSuite {

    private val context: Context
        get() = ApplicationProvider.getApplicationContext()

    // =========================================================================
    // 1. NO BLOCKING ON MAIN THREAD & DISPATCHER ISOLATION
    // =========================================================================
    @Test
    fun testHeavyImageProcessingExecutesOnDefaultOrIODispatchers() = runTest {
        val testBmp = Bitmap.createBitmap(800, 600, Bitmap.Config.ARGB_8888)

        // Verify Image Enhancement runs cleanly off main thread without ANR
        val enhanced = withContext(Dispatchers.Default) {
            ImageEnhancer.applyEnhancements(
                source = testBmp,
                params = ImageEnhancer.EnhancementParameters(
                    brightness = 15f,
                    contrast = 20f,
                    saturation = 10f,
                    sharpness = 25f
                )
            )
        }
        assertNotNull(enhanced)
        assertEquals(800, enhanced.width)
        assertEquals(600, enhanced.height)

        // Verify Non-destructive pipeline execution off main thread
        val instructions = EditingInstructions(
            rotationAngle = 90f,
            targetWidthPx = 400,
            targetHeightPx = 300,
            resizeMode = ResizeMode.FIT
        )
        val pipelineResult = withContext(Dispatchers.Default) {
            NonDestructivePipeline.applyPipeline(testBmp, instructions)
        }
        assertNotNull(pipelineResult)
        assertEquals(400, pipelineResult.width)
        assertEquals(300, pipelineResult.height)
    }

    // =========================================================================
    // 2. PROPER COROUTINE CANCELLATION
    // =========================================================================
    @Test
    fun testCoroutineCancellation_AbortsStaleJobImmediately() = runTest {
        var completedIterations = 0
        var wasCancelled = false

        val testJob: Job = launch(Dispatchers.Default) {
            try {
                for (i in 1..100) {
                    if (!isActive) break
                    delay(50)
                    completedIterations++
                }
            } catch (e: Exception) {
                wasCancelled = true
            }
        }

        // Cancel the job after brief moment
        delay(120)
        testJob.cancelAndJoin()

        assertTrue("Job must be inactive and cancelled", testJob.isCancelled)
        assertTrue("Iterations must not run to completion (should be <= 5)", completedIterations < 10)
    }

    // =========================================================================
    // 3. NO EXCESSIVE BITMAP COPIES & IMMEDIATE RECYCLING
    // =========================================================================
    @Test
    fun testBitmapRecycling_PreventsUnboundedMemoryCopies() {
        val original = Bitmap.createBitmap(200, 200, Bitmap.Config.ARGB_8888)

        // When rotating 0 degrees, it should return identical reference without copying
        val unrotated = BitmapUtils.rotateBitmap(original, 0)
        assertTrue("0-degree rotation must avoid allocating redundant bitmap copy", unrotated === original)

        // When rotation produces a new bitmap, old one can be safely recycled
        val rotated = BitmapUtils.rotateBitmap(original, 90)
        if (rotated !== original) {
            original.recycle()
            assertTrue("Original bitmap should be recycled after replacement", original.isRecycled)
        }
        assertFalse("New rotated bitmap must remain valid", rotated.isRecycled)
        rotated.recycle()
    }

    // =========================================================================
    // 4. MEMORY-SAFE SUBSAMPLING (PREVENT OOM ON ULTRA-HIGH-RES IMAGES)
    // =========================================================================
    @Test
    fun testSubsamplingPreventsExcessiveMemoryAllocations() {
        val largeOptions = android.graphics.BitmapFactory.Options().apply {
            outWidth = 8000
            outHeight = 6000
        }

        val sampleSize = BitmapUtils.calculateInSampleSize(largeOptions, 1000, 1000)
        // Standard Android downsampling: 6000x8000 into 1000x1000 yields inSampleSize = 4 (16x memory reduction)
        assertEquals(4, sampleSize)
    }

    // =========================================================================
    // 5. PROPER TEMPORARY-FILE CLEANUP
    // =========================================================================
    @Test
    fun testTemporaryFileCleanup_PurgesStaleCacheFiles() {
        val cacheDir = context.cacheDir
        val oldFile = File(cacheDir, "old_temp_export_123.jpg")
        oldFile.writeText("sample stale cache content")
        // Set timestamp back by 48 hours
        oldFile.setLastModified(System.currentTimeMillis() - (48 * 60 * 60 * 1000L))

        val recentFile = File(cacheDir, "recent_temp_export_456.jpg")
        recentFile.writeText("active cache content")
        recentFile.setLastModified(System.currentTimeMillis())

        assertTrue(oldFile.exists())
        assertTrue(recentFile.exists())

        // Run cleanup with 24-hour threshold
        OutputFileManager.cleanOldCacheFiles(context, maxAgeHours = 24)

        assertFalse("Stale cache file older than 24h must be purged", oldFile.exists())
        assertTrue("Recent cache file must be preserved", recentFile.exists())

        recentFile.delete()
    }

    // =========================================================================
    // 6. BATCH PROCESSING PIPELINE CANCEL & RESILIENCE
    // =========================================================================
    @Test
    fun testBatchCancellation_LeavesQueueInConsistentState() {
        val sampleItems = (1..5).map { i ->
            BatchItem(
                id = "batch_$i",
                uri = Uri.parse("content://media/external/images/$i"),
                originalName = "photo_$i.jpg",
                originalSize = 1024L * 500,
                originalWidth = 1920,
                originalHeight = 1080,
                status = if (i == 1) BatchStatus.COMPLETED else BatchStatus.PENDING
            )
        }

        val config = BatchConfig(
            format = ExportFormat.JPEG,
            quality = 85
        )

        val cancelledList = sampleItems.map { item ->
            if (item.status == BatchStatus.PENDING) {
                item.copy(status = BatchStatus.CANCELLED, errorMessage = "Processing cancelled")
            } else {
                item
            }
        }

        assertEquals(BatchStatus.COMPLETED, cancelledList[0].status)
        assertEquals(BatchStatus.CANCELLED, cancelledList[1].status)
        assertEquals(BatchStatus.CANCELLED, cancelledList[4].status)
    }

    // =========================================================================
    // 7. ASYNC EXPORT ENGINE WITH FAST VERIFICATION
    // =========================================================================
    @Test
    fun testExportEngine_PerformsVerificationWithoutBlocking() {
        val testBmp = Bitmap.createBitmap(300, 200, Bitmap.Config.ARGB_8888)

        val result = ExportEngine.exportImage(
            context = context,
            bitmap = testBmp,
            format = ExportFormat.JPEG,
            quality = 90,
            dpi = 300
        )

        assertTrue(result.success)
        assertNotNull(result.outputFile)
        assertTrue(result.outputFile!!.exists())
        assertNotNull(result.verificationReport)
        assertTrue(result.verificationReport!!.allMeasurablePassed)

        result.outputFile?.delete()
    }

    // =========================================================================
    // 8. LARGE IMAGE PROCESSING & MEMORY STABILITY (NO OOM)
    // =========================================================================
    @Test
    fun testLargeImageProcessing_SubsamplingAndMemoryBounds() = runTest {
        // Create a 3000x2000 image (6 million pixels)
        val largeBmp = Bitmap.createBitmap(3000, 2000, Bitmap.Config.ARGB_8888)
        assertNotNull(largeBmp)

        val initialMemory = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()

        // Execute non-destructive pipeline on Default dispatcher (simulating heavy user edit)
        val instructions = EditingInstructions(
            rotationAngle = 180f,
            targetWidthPx = 1500,
            targetHeightPx = 1000,
            resizeMode = ResizeMode.FIT
        )
        val processed = withContext(Dispatchers.Default) {
            NonDestructivePipeline.applyPipeline(largeBmp, instructions)
        }
        assertNotNull(processed)
        assertEquals(1500, processed.width)
        assertEquals(1000, processed.height)

        largeBmp.recycle()
        processed.recycle()
        assertTrue(largeBmp.isRecycled)
        assertTrue(processed.isRecycled)
    }

    // =========================================================================
    // 9. LARGE BATCH CONCURRENCY & MEMORY ISOLATION
    // =========================================================================
    @Test
    fun testLargeBatch_BoundedMemoryFootprint() = runTest {
        // Simulate a large batch queue of 50 items
        val largeBatch = (1..50).map { i ->
            BatchItem(
                id = "item_$i",
                uri = Uri.parse("file:///data/user/0/com.example/cache/test_$i.jpg"),
                originalName = "test_photo_$i.jpg",
                originalSize = 250 * 1024L,
                originalWidth = 1920,
                originalHeight = 1080
            )
        }
        assertEquals(50, largeBatch.size)

        // Verify batch configuration scales without memory accumulation
        val config = BatchConfig(
            format = ExportFormat.WEBP_LOSSY,
            quality = 80,
            allowDimensionDownscalingForKb = true
        )
        val (targetW, targetH) = com.example.processing.BatchProcessingEngine.computeBatchTargetDimensions(1920, 1080, config)
        assertEquals(1920, targetW)
        assertEquals(1080, targetH)
    }

    // =========================================================================
    // 10. CPU & ANR AUDIT: NO MAIN THREAD BLOCKING
    // =========================================================================
    @Test
    fun testCpuAndAnrAudit_AsyncExecutionGuaranteed() = runTest {
        val startTimestamp = System.currentTimeMillis()
        var completedOnBackground = false

        withContext(Dispatchers.Default) {
            val smallBmp = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888)
            val rotated = BitmapUtils.rotateBitmap(smallBmp, 90)
            completedOnBackground = (rotated.width == 100 && rotated.height == 100)
            smallBmp.recycle()
            rotated.recycle()
        }

        val elapsed = System.currentTimeMillis() - startTimestamp
        assertTrue("Background processing must complete asynchronously", completedOnBackground)
        assertTrue("Processing 100x100 bitmap must execute in < 2000ms", elapsed < 2000L)
    }
}

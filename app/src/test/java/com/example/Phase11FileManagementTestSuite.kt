package com.example

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AppDatabase
import com.example.data.local.FavoriteItemEntity
import com.example.data.local.FavoriteType
import com.example.data.local.HistoryEntity
import com.example.data.local.UtilityRepository
import com.example.domain.usecase.FileManagementUseCase
import com.example.model.ExportFormat
import com.example.model.NamingMode
import com.example.model.OutputDestinationType
import com.example.model.OutputFileConfiguration
import com.example.model.OverwriteConflictAction
import com.example.processing.OutputFileManager
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
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

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class Phase11FileManagementTestSuite {

    private lateinit var context: Context
    private lateinit var db: AppDatabase
    private lateinit var repository: UtilityRepository
    private lateinit var fileManagementUseCase: FileManagementUseCase
    private lateinit var tempDir: File

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = UtilityRepository(db)
        fileManagementUseCase = FileManagementUseCase()
        tempDir = File(context.cacheDir, "phase11_test_files").apply {
            if (exists()) deleteRecursively()
            mkdirs()
        }
    }

    @After
    fun tearDown() {
        db.close()
        tempDir.deleteRecursively()
    }

    private fun createSampleBitmap(width: Int = 160, height: Int = 120): Bitmap {
        val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        for (y in 0 until height) {
            for (x in 0 until width) {
                bmp.setPixel(x, y, Color.rgb((x * 255) / width, (y * 255) / height, 140))
            }
        }
        return bmp
    }

    @Test
    fun `1 - History logging, favorite toggle, filtering, and sorting work end-to-end`() = runBlocking {
        val id1 = fileManagementUseCase.recordHistory(
            repository = repository,
            item = HistoryEntity(
                title = "alpha",
                outputFileName = "alpha.jpg",
                outputUriString = "${tempDir.absolutePath}/alpha.jpg",
                width = 1200,
                height = 800,
                format = "JPG",
                fileSizeBytes = 45_000L,
                originalFileSizeBytes = 120_000L,
                operationType = "Resize & Export"
            )
        )
        val id2 = fileManagementUseCase.recordHistory(
            repository = repository,
            item = HistoryEntity(
                title = "zebra",
                outputFileName = "zebra.webp",
                outputUriString = "${tempDir.absolutePath}/zebra.webp",
                width = 1080,
                height = 1080,
                format = "WEBP",
                fileSizeBytes = 90_000L,
                originalFileSizeBytes = 200_000L,
                operationType = "Crop & Export"
            )
        )

        assertTrue(id1 > 0)
        assertTrue(id2 > 0)

        val allItems = repository.allHistory.first()
        assertEquals(2, allItems.size)

        // Toggle favorite on alpha.jpg
        val alphaItem = allItems.first { it.outputFileName == "alpha.jpg" }
        val updatedAlpha = fileManagementUseCase.toggleHistoryFavorite(repository, alphaItem)
        assertTrue(updatedAlpha.isFavorite)

        val favsOnly = fileManagementUseCase.filterAndSortHistory(
            allHistory = repository.allHistory.first(),
            category = "FAVORITES"
        )
        assertEquals(1, favsOnly.size)
        assertEquals("alpha.jpg", favsOnly.first().outputFileName)

        // Sort by NAME_ASC
        val sortedByName = fileManagementUseCase.filterAndSortHistory(
            allHistory = repository.allHistory.first(),
            sortMode = FileManagementUseCase.HistorySortMode.NAME_ASC
        )
        assertEquals("alpha.jpg", sortedByName[0].outputFileName)
        assertEquals("zebra.webp", sortedByName[1].outputFileName)

        // Sort by LARGEST_SIZE
        val sortedBySize = fileManagementUseCase.filterAndSortHistory(
            allHistory = repository.allHistory.first(),
            sortMode = FileManagementUseCase.HistorySortMode.LARGEST_SIZE
        )
        assertEquals("zebra.webp", sortedBySize[0].outputFileName)
        assertEquals("alpha.jpg", sortedBySize[1].outputFileName)
    }

    @Test
    fun `2 - Favorites creation, rename, usage tracking, filtering, and removal work end-to-end`() = runBlocking {
        val favId = repository.insertFavorite(
            FavoriteItemEntity(
                type = FavoriteType.CUSTOM_DIMENSION,
                dataKey = "custom_3840_2160",
                title = "4K UHD Wallpaper",
                subtitle = "3840 × 2160 px",
                width = 3840,
                height = 2160,
                extraJson = "{\"width\":3840,\"height\":2160}"
            )
        )
        assertTrue(favId > 0)

        var favorites = repository.allFavoriteItems.first()
        assertEquals(1, favorites.size)
        val initialFav = favorites.first()
        assertEquals("4K UHD Wallpaper", initialFav.title)
        assertEquals(0, initialFav.usageCount)

        // Rename favorite
        val renamedFav = fileManagementUseCase.renameFavoriteItem(
            repository = repository,
            item = initialFav,
            newTitle = "Cinema 4K Master"
        )
        assertEquals("Cinema 4K Master", renamedFav.title)

        // Increment usage count
        val usedFav = fileManagementUseCase.recordFavoriteUsage(repository, renamedFav)
        assertEquals(1, usedFav.usageCount)

        val persistedFav = repository.allFavoriteItems.first().first()
        assertEquals("Cinema 4K Master", persistedFav.title)
        assertEquals(1, persistedFav.usageCount)

        // Filter favorites
        val filtered = fileManagementUseCase.filterFavorites(
            favorites = repository.allFavoriteItems.first(),
            type = FavoriteType.CUSTOM_DIMENSION,
            searchQuery = "Cinema"
        )
        assertEquals(1, filtered.size)

        // Remove favorite
        fileManagementUseCase.deleteFavoriteItem(repository, persistedFav)
        favorites = repository.allFavoriteItems.first()
        assertTrue(favorites.isEmpty())
    }

    @Test
    fun `3 - Presets creation, rename, duplicate, JSON export-import, and deletion work end-to-end`() = runBlocking {
        val created = fileManagementUseCase.createCustomPreset(
            repository = repository,
            name = "E-Commerce Square",
            category = "Custom",
            targetWidth = 1600,
            targetHeight = 1600,
            dpi = 300,
            format = "WEBP",
            maxFileSizeKb = 250,
            quality = 88
        )
        assertTrue(created.id > 0)

        val presets = repository.allPresets.first()
        assertEquals(1, presets.size)
        assertEquals("E-Commerce Square", presets.first().name)

        // Rename preset
        val renamed = fileManagementUseCase.renameCustomPreset(
            repository = repository,
            preset = created,
            newName = "Shopify Product HD"
        )
        assertEquals("Shopify Product HD", renamed.name)

        // Duplicate preset
        val dup = fileManagementUseCase.duplicateCustomPreset(
            repository = repository,
            preset = renamed,
            customName = "Shopify Product HD (Copy)"
        )
        assertTrue(dup.id > 0)
        assertEquals(2, repository.allPresets.first().size)

        // Export & Import JSON
        val exportedJson = fileManagementUseCase.exportPresetsToJson(repository.allPresets.first())
        assertTrue(exportedJson.contains("Shopify Product HD"))

        // Delete presets
        for (p in repository.allPresets.first()) {
            fileManagementUseCase.deleteCustomPreset(repository, p)
        }
        assertTrue(repository.allPresets.first().isEmpty())

        // Re-import from JSON
        val imported = fileManagementUseCase.importPresetsFromJson(repository, exportedJson)
        assertEquals(2, imported.size)
        assertEquals(2, repository.allPresets.first().size)
    }

    @Test
    fun `4 - Save As formats filenames, sanitizes illegal chars, and writes verified output file`() {
        val bitmap = createSampleBitmap(200, 150)
        val outputConfig = OutputFileConfiguration(
            namingMode = NamingMode.EXACT_CUSTOM,
            customExactName = "My Vacation: Photo*2026?",
            prefix = "IMG_",
            suffix = "_print",
            includeDimensions = true,
            includeDpi = false,
            includeTimestamp = false,
            destinationType = OutputDestinationType.PUBLIC_MEDIASTORE,
            overwriteAction = OverwriteConflictAction.AUTO_RENAME_KEEP_BOTH
        )

        val formattedName = OutputFileManager.generateFormattedFilename(
            config = outputConfig,
            originalName = "vacation.jpg",
            width = 200,
            height = 150,
            dpi = 300,
            format = ExportFormat.JPEG
        )
        assertEquals("My Vacation_ Photo_2026.jpg", formattedName)

        val saveResult = fileManagementUseCase.executeSaveAs(
            context = context,
            bitmap = bitmap,
            format = ExportFormat.JPEG,
            quality = 90,
            dpi = 300,
            originalFileName = "vacation.jpg",
            outputConfig = outputConfig
        )

        assertTrue("Save As should succeed: ${saveResult.errorMessage}", saveResult.success)
        assertTrue(saveResult.fileSizeBytes > 0)
        assertEquals(200, saveResult.width)
        assertEquals(150, saveResult.height)
    }

    @Test
    fun `5 - Rename physical file and History record updates disk file and database entry`() = runBlocking {
        val initialFile = File(tempDir, "draft_scan.jpg")
        val bmp = createSampleBitmap(120, 90)
        initialFile.outputStream().use { out ->
            bmp.compress(Bitmap.CompressFormat.JPEG, 85, out)
        }
        assertTrue(initialFile.exists())

        val id = fileManagementUseCase.recordHistory(
            repository = repository,
            item = HistoryEntity(
                title = "draft_scan",
                outputFileName = initialFile.name,
                outputUriString = initialFile.absolutePath,
                width = 120,
                height = 90,
                format = "JPG",
                fileSizeBytes = initialFile.length(),
                originalFileSizeBytes = initialFile.length(),
                operationType = "Save As"
            )
        )
        val item = repository.allHistory.first().first { it.id == id }

        val outcome = fileManagementUseCase.renameHistoryEntry(
            context = context,
            repository = repository,
            item = item,
            newRawName = "final_invoice_2026"
        )

        assertTrue("Rename should succeed", outcome.success)
        assertTrue("Physical rename should succeed", outcome.renameResult.success)
        assertEquals("final_invoice_2026.jpg", outcome.renameResult.newName)
        assertEquals("final_invoice_2026.jpg", outcome.updatedEntity.outputFileName)
        assertFalse("Old file should no longer exist", initialFile.exists())
        assertTrue("Renamed file should exist on disk", File(tempDir, "final_invoice_2026.jpg").exists())
    }

    @Test
    fun `6 - Share builds share intent with valid MIME type and read permission flag`() {
        val testFile = File(tempDir, "share_one.png").apply { writeBytes(ByteArray(256) { 1 }) }

        val shareIntent = OutputFileManager.buildShareIntent(
            context = context,
            targetPathOrUri = testFile.absolutePath,
            mimeType = "image/png"
        )
        assertNotNull(shareIntent)
        assertEquals(Intent.ACTION_SEND, shareIntent?.action)
        assertEquals("image/png", shareIntent?.type)
        assertTrue((shareIntent?.flags ?: 0) and Intent.FLAG_GRANT_READ_URI_PERMISSION != 0)
    }

    @Test
    fun `7 - Delete removes History record and physical file from storage when requested`() = runBlocking {
        val targetFile = File(tempDir, "to_delete.webp")
        targetFile.writeBytes(ByteArray(1024) { 0x42 })
        assertTrue(targetFile.exists())

        fileManagementUseCase.recordHistory(
            repository = repository,
            item = HistoryEntity(
                title = "to_delete",
                outputFileName = targetFile.name,
                outputUriString = targetFile.absolutePath,
                width = 400,
                height = 400,
                format = "WEBP",
                fileSizeBytes = 1024L,
                originalFileSizeBytes = 2048L,
                operationType = "Convert"
            )
        )

        val historyItem = repository.allHistory.first().first()
        val deleteOutcome = fileManagementUseCase.deleteHistoryEntry(
            context = context,
            repository = repository,
            item = historyItem,
            deletePhysicalFile = true
        )

        assertTrue("Record delete should succeed", deleteOutcome.recordDeleted)
        assertTrue("Physical file delete should succeed", deleteOutcome.physicalFileDeleted)
        assertEquals(1024L, deleteOutcome.freedBytes)
        assertFalse("Physical file should be removed from disk", targetFile.exists())
        assertTrue("History table should now be empty", repository.allHistory.first().isEmpty())
    }
}

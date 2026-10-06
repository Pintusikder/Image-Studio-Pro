package com.example

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AppDatabase
import com.example.data.local.FavoriteItemEntity
import com.example.data.local.FavoriteType
import com.example.data.local.HistoryEntity
import com.example.data.local.UtilityRepository
import com.example.domain.usecase.SecurityPrivacyUseCase
import com.example.model.ExportFormat
import com.example.model.MetadataPolicy
import com.example.model.MetadataPrivacyConfig
import com.example.model.OverwriteConflictAction
import com.example.processing.BitmapUtils
import com.example.processing.ExifManager
import com.example.processing.ExportEngine
import com.example.processing.OutputFileManager
import com.example.processing.SecurityPrivacyEngine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import java.io.FileOutputStream

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class Phase12SecurityPrivacyTestSuite {

    private lateinit var context: Context
    private lateinit var db: AppDatabase
    private lateinit var repository: UtilityRepository
    private lateinit var securityPrivacyUseCase: SecurityPrivacyUseCase

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = UtilityRepository(db)
        securityPrivacyUseCase = SecurityPrivacyUseCase()
    }

    @After
    fun tearDown() {
        db.close()
        SecurityPrivacyEngine.purgeAllTemporaryFiles(context, zeroFill = false)
    }

    @Test
    fun `1 - File Access blocks path traversal, NUL bytes, internal DB paths, and allows sandboxed cache`() {
        // 1. Path traversal attempt
        val traversalCheck = SecurityPrivacyEngine.validateSafeFileAccess(
            context = context,
            rawPathOrFile = "${context.cacheDir.absolutePath}/../../etc/passwd"
        )
        assertFalse("Path traversal must be blocked", traversalCheck.isSafe)

        // 2. NUL-byte injection attempt
        val nulCheck = SecurityPrivacyEngine.validateSafeFileAccess(
            context = context,
            rawPathOrFile = "${context.cacheDir.absolutePath}/image\u0000.jpg"
        )
        assertFalse("NUL-byte path must be blocked", nulCheck.isSafe)

        // 3. Restricted internal SQLite database & shared_prefs directories blocked
        val dbPath = File(context.applicationInfo.dataDir, "databases/image_studio_database").absolutePath
        val sharedPrefsPath = File(context.applicationInfo.dataDir, "shared_prefs/prefs.xml").absolutePath
        assertFalse("Internal database path must be blocked", SecurityPrivacyEngine.validateSafeFileAccess(context, dbPath).isSafe)
        assertFalse("Internal shared_prefs path must be blocked", SecurityPrivacyEngine.validateSafeFileAccess(context, sharedPrefsPath).isSafe)

        // 4. OutputFileManager refuses to delete or rename internal database files
        val delAttempt = OutputFileManager.deleteOutputFile(context, dbPath)
        assertFalse("Delete must refuse internal DB path", delAttempt.success)
        val renAttempt = OutputFileManager.renameOutputFile(context, dbPath, "stolen_db")
        assertFalse("Rename must refuse internal DB path", renAttempt.success)

        // 5. Sandboxed cache file is allowed
        val validCacheFile = File(context.cacheDir, "safe_export.jpg").absolutePath
        assertTrue("Sandboxed cache file should be allowed", SecurityPrivacyEngine.validateSafeFileAccess(context, validCacheFile).isSafe)
    }

    @Test
    fun `2 - URI Handling rejects untrusted schemes, internal file URIs, non-media authorities, and non-image streams`() {
        // 1. Reject untrusted schemes
        assertFalse(SecurityPrivacyEngine.validateAndInspectUri(context, Uri.parse("javascript:alert(1)"), verifyImageStream = false).isSafe)
        assertFalse(SecurityPrivacyEngine.validateAndInspectUri(context, Uri.parse("https://evil.example.com/payload.jpg"), verifyImageStream = false).isSafe)
        assertFalse(SecurityPrivacyEngine.validateAndInspectUri(context, Uri.parse("ftp://files.example.com/test.png"), verifyImageStream = false).isSafe)

        // 2. Reject file:// URI targeting internal app database or /proc
        val dbFileUri = Uri.parse("file://${context.applicationInfo.dataDir}/databases/image_studio_database")
        assertFalse(SecurityPrivacyEngine.validateAndInspectUri(context, dbFileUri, verifyImageStream = false).isSafe)
        assertNull("BitmapUtils must refuse to decode internal database URI", BitmapUtils.decodeSampledBitmapFromUri(context, dbFileUri))

        // 3. Reject sensitive non-media content:// providers
        val contactsUri = Uri.parse("content://com.android.contacts/raw_contacts/1")
        val smsUri = Uri.parse("content://sms/inbox")
        assertFalse(SecurityPrivacyEngine.validateAndInspectUri(context, contactsUri, verifyImageStream = false).isSafe)
        assertFalse(SecurityPrivacyEngine.validateAndInspectUri(context, smsUri, verifyImageStream = false).isSafe)

        // 4. Reject corrupted / non-image file pretending to be JPEG
        val fakeImageFile = File(context.cacheDir, "fake_script.jpg").apply {
            writeText("<html><script>malicious()</script></html>")
        }
        val fakeUri = Uri.fromFile(fakeImageFile)
        val streamCheck = SecurityPrivacyEngine.validateAndInspectUri(context, fakeUri, verifyImageStream = true)
        assertFalse("Non-image magic header must be rejected", streamCheck.isSafe)
        assertNull("BitmapUtils must return null for non-image magic bytes", BitmapUtils.decodeSampledBitmapFromUri(context, fakeUri))
    }

    @Test
    fun `3 - Temporary Files audit and zero-fill secure wipe remove sensitive cache residue`() {
        val isolatedDir = SecurityPrivacyEngine.getIsolatedTempDir(context, "shared_temp")
        val tempFile1 = File(isolatedDir, "passport_temp_1.jpg").apply {
            writeBytes(ByteArray(2048) { 0x7F })
        }
        val tempFile2 = File(isolatedDir, "signature_temp_2.png").apply {
            writeBytes(ByteArray(1024) { 0x3C })
        }

        val beforeAudit = securityPrivacyUseCase.auditTempCache(context)
        assertTrue("Audit should detect at least 2 temp files", beforeAudit.totalFileCount >= 2)
        assertTrue("Audit should report at least 3072 bytes", beforeAudit.totalBytes >= 3072L)

        // Zero-fill wipe single file
        val wipedSingle = SecurityPrivacyEngine.secureWipeFile(tempFile1, zeroFill = true)
        assertTrue(wipedSingle)
        assertFalse(tempFile1.exists())

        // Purge all remaining temp files with zero-fill
        val purgeResult = securityPrivacyUseCase.purgeTemporaryCache(context, zeroFill = true, olderThanMs = 0L)
        assertTrue(purgeResult.deletedFileCount >= 1)
        assertTrue(purgeResult.zeroFilledCount >= 1)
        assertFalse(tempFile2.exists())

        val afterAudit = securityPrivacyUseCase.auditTempCache(context)
        assertEquals(0, afterAudit.totalFileCount)
        assertEquals(0L, afterAudit.totalBytes)
    }

    @Test
    fun `4 - Export Security sanitizes filenames, enforces privacy policy, and verifies binary output`() {
        val bmp = Bitmap.createBitmap(160, 120, Bitmap.Config.ARGB_8888).apply {
            eraseColor(Color.rgb(40, 120, 200))
        }

        val exportResult = ExportEngine.exportImage(
            context = context,
            bitmap = bmp,
            format = ExportFormat.JPEG,
            quality = 90,
            customExactFileName = "../unsafe:name*2026?",
            dpi = 300,
            privacyConfig = MetadataPrivacyConfig(policy = MetadataPolicy.STRIP_ALL)
        )

        assertTrue(exportResult.success)
        assertNotNull(exportResult.outputFile)
        assertFalse("Filename must not contain path traversal", exportResult.outputFile!!.name.contains(".."))
        assertTrue("Metadata audit must confirm privatization", exportResult.metadataAudit?.isPrivatized == true)
        assertEquals(160, exportResult.verifiedFileWidth)
        assertEquals(120, exportResult.verifiedFileHeight)
    }

    @Test
    fun `5 - Sharing Security enforces least-privilege read flag, blocks internal files, and scrubs metadata on share`() {
        // 1. Attempt to share restricted internal DB file must return null
        val dbPath = File(context.applicationInfo.dataDir, "databases/image_studio_database").absolutePath
        assertNull("Sharing restricted internal database must be blocked", OutputFileManager.buildShareIntent(context, dbPath))

        // 2. Create a JPEG with sensitive GPS & Camera EXIF
        val sensitiveFile = File(context.cacheDir, "photo_with_gps.jpg")
        val bmp = Bitmap.createBitmap(80, 80, Bitmap.Config.ARGB_8888).apply { eraseColor(Color.GREEN) }
        FileOutputStream(sensitiveFile).use { out ->
            bmp.compress(Bitmap.CompressFormat.JPEG, 92, out)
        }
        val exif = ExifInterface(sensitiveFile.absolutePath)
        exif.setAttribute(ExifInterface.TAG_GPS_LATITUDE, "40/1,42/1,46/1")
        exif.setAttribute(ExifInterface.TAG_GPS_LATITUDE_REF, "N")
        exif.setAttribute(ExifInterface.TAG_GPS_LONGITUDE, "74/1,0/1,22/1")
        exif.setAttribute(ExifInterface.TAG_GPS_LONGITUDE_REF, "W")
        exif.setAttribute(ExifInterface.TAG_MAKE, "PrivatePhoneMaker")
        exif.setAttribute(ExifInterface.TAG_BODY_SERIAL_NUMBER, "SERIAL-12345")
        exif.saveAttributes()

        // Verify source file has GPS before scrubbing
        assertTrue(ExifManager.readExifFromFile(sensitiveFile).hasGps)

        // Create scrubbed share copy
        val scrubbedCopy = securityPrivacyUseCase.createPrivacyScrubbedShareCopy(context, sensitiveFile, ExportFormat.JPEG)
        assertNotNull(scrubbedCopy)
        val scrubbedAudit = ExifManager.auditFileMetadata(scrubbedCopy!!, ExportFormat.JPEG)
        assertTrue("Scrubbed share copy must be 100% privatized", scrubbedAudit.isPrivatized)
        assertFalse("Scrubbed share copy must not contain GPS", scrubbedAudit.hasGps)
        assertFalse("Scrubbed share copy must not contain Camera info", scrubbedAudit.hasCameraInfo)

        // Build Share Intent with scrubMetadataBeforeShare = true
        val shareIntent = OutputFileManager.buildShareIntent(
            context = context,
            targetPathOrUri = sensitiveFile.absolutePath,
            mimeType = "image/jpeg",
            scrubMetadataBeforeShare = true
        )
        assertNotNull(shareIntent)
        assertTrue("Must grant READ URI permission", (shareIntent!!.flags and Intent.FLAG_GRANT_READ_URI_PERMISSION) != 0)
        assertEquals("Must NOT grant WRITE URI permission", 0, shareIntent.flags and Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
    }

    @Test
    fun `6 - Metadata Scrubber strips GPS, Camera, Lens Serial, MakerNote, XMP, and Date tags`() {
        val jpgFile = File(context.cacheDir, "metadata_test.jpg")
        val bmp = Bitmap.createBitmap(64, 64, Bitmap.Config.ARGB_8888).apply { eraseColor(Color.RED) }
        FileOutputStream(jpgFile).use { out ->
            bmp.compress(Bitmap.CompressFormat.JPEG, 90, out)
        }

        val exif = ExifInterface(jpgFile.absolutePath)
        exif.setAttribute(ExifInterface.TAG_GPS_LATITUDE, "51/1,30/1,26/1")
        exif.setAttribute(ExifInterface.TAG_GPS_LATITUDE_REF, "N")
        exif.setAttribute(ExifInterface.TAG_GPS_LONGITUDE, "0/1,7/1,39/1")
        exif.setAttribute(ExifInterface.TAG_GPS_LONGITUDE_REF, "W")
        exif.setAttribute(ExifInterface.TAG_MAKE, "CamBrand")
        exif.setAttribute(ExifInterface.TAG_MODEL, "ProModel X")
        exif.setAttribute(ExifInterface.TAG_LENS_SERIAL_NUMBER, "LENS-777")
        exif.setAttribute(ExifInterface.TAG_BODY_SERIAL_NUMBER, "BODY-888")
        exif.setAttribute(ExifInterface.TAG_DATETIME_ORIGINAL, "2026:09:30 09:15:00")
        exif.setAttribute(ExifInterface.TAG_USER_COMMENT, "Private note")
        exif.saveAttributes()

        // Apply STRIP_ALL
        ExifManager.applyExifToOutputFile(
            outputFile = jpgFile,
            dpi = 300,
            privacyConfig = MetadataPrivacyConfig(policy = MetadataPolicy.STRIP_ALL),
            format = ExportFormat.JPEG
        )

        val audit = ExifManager.auditFileMetadata(jpgFile, ExportFormat.JPEG)
        assertTrue(audit.isPrivatized)
        assertFalse(audit.hasGps)
        assertFalse(audit.hasCameraInfo)
        assertFalse(audit.hasDeviceInfo)
        assertFalse(audit.hasDateMetadata)
        assertFalse(audit.hasAuthorCopyright)
    }

    @Test
    fun `7 & 8 - Permissions and Complete 8-Domain Security & Privacy Audit pass with 100 percent score`() = runBlocking {
        // Populate sample history, favorite, and temp file to test full privacy reset as well
        repository.insertHistory(
            HistoryEntity(
                title = "sample",
                outputFileName = "sample.jpg",
                outputUriString = "${context.cacheDir.absolutePath}/sample.jpg",
                width = 100,
                height = 100,
                format = "JPG"
            )
        )
        repository.insertFavorite(
            FavoriteItemEntity(
                type = FavoriteType.CUSTOM_DIMENSION,
                title = "1080p",
                dataKey = "DIM_1920_1080"
            )
        )
        File(context.cacheDir, "temp_audit.tmp").writeBytes(ByteArray(512) { 1 })

        val auditReport = securityPrivacyUseCase.runCompleteAudit(
            context = context,
            defaultMetadataPolicy = MetadataPolicy.STRIP_ALL,
            defaultOverwriteAction = OverwriteConflictAction.ASK_BEFORE_OVERWRITE
        )

        assertEquals(8, auditReport.domainReports.size)
        assertEquals(SecurityPrivacyEngine.SecurityStatus.SECURE, auditReport.overallStatus)
        assertEquals(100, auditReport.overallScore)
        assertFalse("App must not declare INTERNET permission", auditReport.hasInternetPermission)
        assertFalse("App must not declare Location permissions", auditReport.hasLocationPermission)
        assertFalse("App must not declare broad storage permissions", auditReport.hasBroadStoragePermission)

        // Execute Full Privacy Reset
        val resetResult = securityPrivacyUseCase.performFullPrivacyReset(
            context = context,
            repository = repository,
            clearHistory = true,
            clearFavorites = true,
            purgeTempCache = true
        )
        assertTrue(resetResult.deletedFileCount >= 1)
        assertTrue(repository.allHistory.first().isEmpty())
        assertTrue(repository.allFavoriteItems.first().isEmpty())
        assertEquals(0, securityPrivacyUseCase.auditTempCache(context).totalFileCount)
    }
}

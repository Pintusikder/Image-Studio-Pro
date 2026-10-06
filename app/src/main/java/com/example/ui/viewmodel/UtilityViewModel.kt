package com.example.ui.viewmodel

import android.app.Application
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.RectF
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.local.FavoriteItemEntity
import com.example.data.local.FavoriteType
import com.example.data.local.HistoryEntity
import com.example.data.local.PresetEntity
import com.example.data.local.UtilityRepository
import com.example.data.local.HistoryManager
import com.example.data.local.PresetManager
import com.example.domain.usecase.ImageProcessingRepository
import com.example.model.AppSettings
import com.example.model.AppThemeMode
import com.example.model.BatchCompressionMode
import com.example.model.BatchConfig
import com.example.model.BatchFilterOption
import com.example.model.BatchItem
import com.example.model.BatchResizeOption
import com.example.model.BatchSortOption
import com.example.model.BatchStatus
import com.example.model.BatchSummaryReport
import com.example.model.CompressionPreset
import com.example.model.CropAspectRatio
import com.example.model.CustomCompressionMode
import com.example.model.CutLineStyle
import com.example.model.DetectedImageFormat
import com.example.model.ExportFormat
import com.example.model.FilterPreset
import com.example.model.FormatConversionPair
import com.example.model.IdDocumentCategory
import com.example.model.ImageExifData
import com.example.model.ImageMetadata
import com.example.model.MetadataPolicy
import com.example.model.MetadataPrivacyConfig
import com.example.model.PassportDimensionUnit
import com.example.model.PassportExportTarget
import com.example.model.PassportPreset
import com.example.model.PhotoSheetRotation
import com.example.model.PrintUnit
import com.example.model.ResizeMode
import com.example.model.SheetAlignment
import com.example.model.SheetOrientation
import com.example.model.SheetPaperPreset
import com.example.model.SocialMediaCategory
import com.example.model.SocialMediaPreset
import com.example.model.StandardPrintPreset
import com.example.processing.AspectRatioEngine
import com.example.processing.AspectRatioPreset
import com.example.processing.AspectRatioApplyMode
import com.example.processing.StandardResolution
import com.example.processing.RatioAnalysis
import com.example.processing.BackgroundProcessor
import com.example.processing.BitmapUtils
import com.example.processing.CropEngine
import com.example.processing.CropShape
import com.example.processing.CropGuideGrid
import com.example.processing.CropVerificationReport
import com.example.processing.ResizeEngine
import com.example.processing.ResizeResult
import com.example.processing.ResizeVerificationReport
import com.example.processing.CompressionEngine
import com.example.processing.CompressionResult
import com.example.processing.CompressionVerificationReport
import com.example.domain.usecase.CompressImageUseCase
import com.example.processing.FormatConversionEngine
import com.example.processing.FormatConversionResult
import com.example.processing.FormatConversionVerificationReport
import com.example.domain.usecase.AdvancedEditingUseCase
import com.example.domain.usecase.BatchProcessingUseCase
import com.example.domain.usecase.ConvertFormatUseCase
import com.example.domain.usecase.DpiPrintUseCase
import com.example.domain.usecase.FileManagementUseCase
import com.example.domain.usecase.PassportIdPhotoUseCase
import com.example.domain.usecase.SecurityPrivacyUseCase
import com.example.processing.AdvancedCreativeEngine
import com.example.processing.BatchProcessingEngine
import com.example.processing.DpiPrintEngine
import com.example.processing.PassportIdDocumentEngine
import com.example.processing.SecurityPrivacyEngine
import java.util.concurrent.atomic.AtomicBoolean
import com.example.processing.ExifManager
import com.example.processing.PerspectiveEngine
import com.example.processing.ExportEngine
import com.example.processing.FileSizeEngine
import com.example.processing.FileSizeMode
import com.example.processing.FileSizeOptimizationResult
import com.example.processing.ImageEnhancer
import com.example.processing.PhotoSheetGenerator
import com.example.processing.RotateStraightenEngine
import com.example.processing.SignatureProcessor
import com.example.model.ExactSizePreset
import com.example.model.ExactSizeUnit
import com.example.model.EditorHistorySnapshot
import com.example.model.EditingInstructions
import com.example.model.OutputFileConfiguration
import com.example.model.NamingMode
import com.example.model.OutputDestinationType
import com.example.model.OverwriteConflictAction
import com.example.model.FilenameSanitizationResult
import com.example.model.OutputVerificationReport
import com.example.processing.OutputFileManager
import com.example.processing.NonDestructivePipeline
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.math.abs
import kotlin.math.roundToInt

enum class StudioTab(val label: String) {
    CROP("Crop"),
    RESIZE("Resize"),
    EXACT_SIZE("Exact size"),
    COMPRESS("Compress"),
    CONVERT("Convert"),
    ROTATE("Rotate"),
    FLIP("Flip"),
    STRAIGHTEN("Straighten"),
    PERSPECTIVE("Perspective"),
    DPI("DPI"),
    METADATA("Metadata"),
    ENHANCE("Enhancement"),
    BACKGROUND("Background"),
    // Additional studio tools
    ASPECT_RATIO("Aspect Ratio"),
    PASSPORT("Passport & ID"),
    DOCUMENT("Doc Scanner"),
    SHEET("Photo Sheet"),
    INFO("Image Info");

    companion object {
        val PIXEL_SIZE: StudioTab get() = RESIZE
        val PRINT_SIZE: StudioTab get() = DPI
        val FILE_SIZE: StudioTab get() = COMPRESS
        val FORMAT: StudioTab get() = CONVERT
        val EXIF: StudioTab get() = METADATA
    }
}

data class StudioUiState(
    val currentImageUri: Uri? = null,
    val originalBitmap: Bitmap? = null,
    val workingBitmap: Bitmap? = null,
    val previewBitmap: Bitmap? = null,
    val activeTab: StudioTab = StudioTab.CROP,
    val isLoading: Boolean = false,
    val isExporting: Boolean = false,
    val statusMessage: String? = null,
    val lastExportResult: ExportEngine.ExportResult? = null,
    // Crop
    val selectedCropRatio: CropAspectRatio = CropAspectRatio.FREE,
    val activeCropBounds: RectF = RectF(0.05f, 0.05f, 0.95f, 0.95f),
    val cropShape: CropShape = CropShape.RECTANGLE,
    val cropGuideGrid: CropGuideGrid = CropGuideGrid.RULE_OF_THIRDS,
    val cropCornerRadiusPx: Float = 24f,
    val cropBorderWidthPx: Float = 0f,
    val cropBorderColor: Int = Color.WHITE,
    val lastCropReport: CropVerificationReport? = null,
    // Aspect Ratio Tool
    val aspectRatioLocked: Boolean = true,
    val activeAspectPreset: AspectRatioPreset = AspectRatioEngine.PRESET_16_9,
    val customAspectX: Float = 16f,
    val customAspectY: Float = 9f,
    val customAspectXInput: String = "16",
    val customAspectYInput: String = "9",
    val isCustomAspectSelected: Boolean = false,
    val calcDimensionWidth: Int = 1920,
    val calcDimensionHeight: Int = 1080,
    val calcDimensionWidthInput: String = "1920",
    val calcDimensionHeightInput: String = "1080",
    val aspectApplyMode: AspectRatioApplyMode = AspectRatioApplyMode.CROP,
    val aspectPadColor: Int = Color.WHITE,
    val aspectCategoryFilter: String = "Popular",
    val ratioCalcWInput: String = "1920",
    val ratioCalcHInput: String = "1080",
    // A: Exact Pixel Dimensions
    val targetWidthPx: Int = 1080,
    val targetHeightPx: Int = 1080,
    val keepAspectRatio: Boolean = true,
    val resizeMode: ResizeMode = ResizeMode.FIT,
    val fitBackgroundColor: Int = Color.TRANSPARENT,
    // B: Exact Physical / Print Resolution
    val printUnit: PrintUnit = PrintUnit.INCHES,
    val printWidthPhysical: Float = 4.0f,
    val printHeightPhysical: Float = 6.0f,
    val dpi: Int = 300,
    val selectedPrintPreset: StandardPrintPreset = StandardPrintPreset.PHOTO_4X6,
    val originalReadDpi: Int? = null,
    val customDpiInput: String = "300",
    val printCalculationMode: DpiPrintEngine.PrintCalculationMode = DpiPrintEngine.PrintCalculationMode.PHYSICAL_AND_DPI_TO_PIXELS,
    val printBleedMm: Float = 0f,
    val lastPrintReport: DpiPrintEngine.PrintCalculatorReport? = null,
    val lastDpiReport: DpiPrintEngine.DpiMetadataVerificationReport? = null,
    // Original image metadata for Before/After comparison
    val originalFileSizeBytes: Long = 0,
    val originalMimeType: String = "image/jpeg",
    val originalWidth: Int = 0,
    val originalHeight: Int = 0,
    // C: Exact File Size & Image Compression
    val compressionPreset: CompressionPreset = CompressionPreset.BALANCED,
    val customCompressionMode: CustomCompressionMode = CustomCompressionMode.QUALITY_PERCENT,
    val compressionPercentage: Int = 15,
    val quality: Int = 85,
    val targetSizeKb: Int? = null, // null for manual quality, non-null for exact size
    val fileSizeMode: FileSizeMode = FileSizeMode.MAXIMUM_CEILING,
    val allowDimensionDownscaling: Boolean = true,
    val estimatedBytes: Long = 0,
    val fileSizeOptimizationResult: FileSizeOptimizationResult? = null,
    // Format & Conversion
    val exportFormat: ExportFormat = ExportFormat.JPEG,
    val detectedInputFormat: DetectedImageFormat = DetectedImageFormat.JPEG,
    val hasTransparency: Boolean = false,
    val jpegBackgroundColor: Int = android.graphics.Color.WHITE,
    val jpegBackgroundName: String = "Pure White (#FFFFFF)",
    // Enhancements (Request 26 & Phase 10)
    val brightness: Float = 0f,
    val contrast: Float = 0f,
    val saturation: Float = 0f,
    val exposure: Float = 0f,
    val highlights: Float = 0f,
    val shadows: Float = 0f,
    val sharpness: Float = 0f,
    val warmth: Float = 0f,
    val vignette: Float = 0f,
    val vibrance: Float = 0f,
    val gamma: Float = 1.0f,
    val claheStrength: Float = 0f,
    val isGrayscale: Boolean = false,
    val isBlackAndWhite: Boolean = false,
    val bwThreshold: Float = 128f,
    val activeFilter: FilterPreset = FilterPreset.ORIGINAL,
    val showBeforeAfterSplit: Boolean = false,
    val beforeAfterSplitPosition: Float = 0.5f,
    val isHoldingCompareOriginal: Boolean = false,
    val showEnhanceHistogram: Boolean = true,
    val histogramData: ImageEnhancer.HistogramData? = null,
    val lastEnhancementReport: ImageEnhancer.EnhancementVerificationReport? = null,
    val enhanceUndoStack: List<ImageEnhancer.EnhancementParameters> = emptyList(),
    val enhanceRedoStack: List<ImageEnhancer.EnhancementParameters> = emptyList(),
    // Phase 10 Creative & Diagnostic State
    val watermarkText: String = "© Photo Studio",
    val watermarkOpacity: Int = 60,
    val watermarkTiled: Boolean = false,
    val framePaddingPercent: Float = 4f,
    val frameBorderWidthPx: Float = 4f,
    val frameDropShadow: Boolean = true,
    val colorPaletteSwatches: List<AdvancedCreativeEngine.PaletteSwatch> = emptyList(),
    val qualityReport: AdvancedCreativeEngine.ImageQualityReport? = null,
    val perceptualHashHex: String? = null,
    // Passport, ID & Document Photo
    val selectedIdCategory: IdDocumentCategory = IdDocumentCategory.ALL,
    val selectedPassportPreset: PassportPreset? = PassportPreset.PRESETS.first(),
    val passportWidthPhysical: Float = 35f,
    val passportHeightPhysical: Float = 45f,
    val passportUnit: PassportDimensionUnit = PassportDimensionUnit.MILLIMETERS,
    val passportDpi: Int = 300,
    val customPassportDpiInput: String = "300",
    val passportBgColor: BackgroundProcessor.PassportBgColor = BackgroundProcessor.PassportBgColor.PURE_WHITE,
    val passportTolerance: Float = 42f,
    val passportFineRotation: Float = 0f,
    val passportTargetSizeKb: Int? = null,
    val passportLockAspectRatio: Boolean = true,
    val showBiometricGuides: Boolean = true,
    val showHeadCrownChinGuides: Boolean = true,
    val showEyeLineGuide: Boolean = true,
    val showCenterAxisGuide: Boolean = true,
    val showFaceOvalGuide: Boolean = true,
    val showGridGuide: Boolean = false,
    val passportCopies: Int = 6,
    val passportSheetPaper: SheetPaperPreset = SheetPaperPreset.PHOTO_4X6,
    val passportCutLineStyle: CutLineStyle = CutLineStyle.SOLID,
    val passportExportTarget: PassportExportTarget = PassportExportTarget.SINGLE_PHOTO,
    val passportExportFormat: ExportFormat = ExportFormat.JPEG,
    val passportAutoBiometricCrop: Boolean = false,
    val passportAddThinBorder: Boolean = false,
    val passportSlateConfig: PassportIdDocumentEngine.NameDateSlateConfig = PassportIdDocumentEngine.NameDateSlateConfig(),
    val passportDocumentMode: PassportIdDocumentEngine.DocumentProcessMode = PassportIdDocumentEngine.DocumentProcessMode.ORIGINAL_COLOR,
    val biometricComplianceReport: PassportIdDocumentEngine.BiometricComplianceReport? = null,
    val customPassportPresets: List<PassportPreset> = emptyList(),
    val customSocialPresets: List<SocialMediaPreset> = emptyList(),
    val modifiedSocialPresets: Map<String, SocialMediaPreset> = emptyMap(),
    // Signature
    val signatureInkColor: SignatureProcessor.OutputInkColor = SignatureProcessor.OutputInkColor.PURE_BLACK,
    val signatureTransparentBg: Boolean = false,
    val signatureThreshold: Int = 185,
    val signatureAutoTrim: Boolean = true,
    val signatureStrokeWeightDelta: Int = 0,
    val lastSignatureReport: SignatureProcessor.SignatureVerificationReport? = null,
    // Photo Sheet
    val sheetConfig: PhotoSheetGenerator.SheetConfig = PhotoSheetGenerator.SheetConfig(),
    val sheetResult: PhotoSheetGenerator.SheetResult? = null,
    val sheetExportFormat: ExportFormat = ExportFormat.PDF,
    val sheetPaper: SheetPaperPreset = SheetPaperPreset.A4,
    val sheetCols: Int = 2,
    val sheetRows: Int = 3,
    val sheetCutLines: Boolean = true,
    // EXIF & Privacy
    val exifData: ImageExifData? = null,
    val metadataPrivacyConfig: MetadataPrivacyConfig = MetadataPrivacyConfig(),
    val stripExifOnExport: Boolean = false,
    val customArtist: String = "",
    val customCopyright: String = "",
    val customComment: String = "",
    // Batch (Phase 9)
    val batchItems: List<BatchItem> = emptyList(),
    val isBatchProcessing: Boolean = false,
    val batchConfig: BatchConfig = BatchConfig(),
    val batchCurrentProcessingIndex: Int = -1,
    val batchCurrentFileName: String = "",
    val batchCurrentStage: String = "",
    val batchOverallProgress: Float = 0f,
    val batchElapsedTimeMs: Long = 0L,
    val batchSortOption: BatchSortOption = BatchSortOption.QUEUE_ORDER,
    val batchFilterOption: BatchFilterOption = BatchFilterOption.ALL,
    val batchSkipDuplicates: Boolean = true,
    val batchSummaryReport: BatchSummaryReport = BatchSummaryReport(),
    // Rotate & Straighten (Request 23)
    val rotationAngle: Float = 0f,
    val customAngleInput: String = "0.0",
    val straightenAngle: Float = 0f,
    val straightenCropMode: RotateStraightenEngine.StraightenCropMode = RotateStraightenEngine.StraightenCropMode.AUTO_CROP,
    val straightenBgColor: Int = Color.TRANSPARENT,
    val showGrid: Boolean = true,
    val gridType: RotateStraightenEngine.GridType = RotateStraightenEngine.GridType.RULE_OF_THIRDS,
    val gridOpacity: Float = 0.5f,
    val showHorizonGuide: Boolean = true,
    val horizonGuideYOffset: Float = 0.5f,
    val detectedHorizon: RotateStraightenEngine.HorizonDetectionResult? = null,
    val isDetectingHorizon: Boolean = false,
    val lastOrientationMessage: String? = null,
    // Flip (Request 24)
    val isFlippedHorizontally: Boolean = false,
    val isFlippedVertically: Boolean = false,
    val flipHorizontalCount: Int = 0,
    val flipVerticalCount: Int = 0,
    val flipSymmetryMode: RotateStraightenEngine.FlipSymmetryMode = RotateStraightenEngine.FlipSymmetryMode.NONE,
    val showFlipAxisGuide: Boolean = true,
    // Perspective Correction (Request 25 & Phase 10)
    val perspectiveQuad: PerspectiveEngine.PerspectiveQuad = PerspectiveEngine.PerspectiveQuad.default(),
    val activePerspectiveCorner: PerspectiveEngine.PerspectiveCorner = PerspectiveEngine.PerspectiveCorner.TOP_LEFT,
    val perspectiveVerticalTilt: Float = 0f,
    val perspectiveHorizontalTilt: Float = 0f,
    val showPerspectiveGrid: Boolean = true,
    val perspectiveGridDivisions: Int = 4,
    val perspectiveOutputPreset: PerspectiveEngine.PerspectiveOutputPreset = PerspectiveEngine.PerspectiveOutputPreset.AUTO,
    val perspectiveEnhanceMode: PerspectiveEngine.DocumentEnhanceMode = PerspectiveEngine.DocumentEnhanceMode.ORIGINAL,
    val isPerspectivePreviewActive: Boolean = false,
    val isAutoDetectingCorners: Boolean = false,
    val lastPerspectiveReport: PerspectiveEngine.PerspectiveVerificationReport? = null,
    val lastStraightenReport: RotateStraightenEngine.StraightenVerificationReport? = null,
    // Background Tools (Request 27 & Phase 10)
    val bgConfig: BackgroundProcessor.BackgroundConfig = BackgroundProcessor.BackgroundConfig(),
    val customBgImage: Bitmap? = null,
    val isProcessingBg: Boolean = false,
    val lastBackgroundReport: BackgroundProcessor.BackgroundVerificationReport? = null,
    // Image Information (Request 29)
    val showImageInfoDialog: Boolean = false,
    val originalFileName: String = "",
    // Unified Editor (Request 30)
    val globalUndoStack: List<EditorHistorySnapshot> = emptyList(),
    val globalRedoStack: List<EditorHistorySnapshot> = emptyList(),
    val isBeforeAfterActive: Boolean = false,
    val isFullScreenPreviewOpen: Boolean = false,
    val showSaveAsDialog: Boolean = false,
    // Exact Size (Request 30)
    val exactSizeUnit: ExactSizeUnit = ExactSizeUnit.PX,
    val exactWidthInput: String = "1080",
    val exactHeightInput: String = "1080",
    val exactAspectLocked: Boolean = true,
    val exactSizeMode: ResizeMode = ResizeMode.FIT,
    val exactBgColor: Int = Color.TRANSPARENT,
    // Save As & Output File Management (Request 30, 36)
    val saveAsFileName: String = "edited_image",
    val saveAsFormat: ExportFormat = ExportFormat.JPEG,
    val saveAsQuality: Int = 92,
    val saveAsTargetKb: String = "",
    val saveAsDpi: Int = 300,
    val saveAsPrivacyMode: MetadataPolicy = MetadataPolicy.KEEP_ALL,
    val outputFileConfig: OutputFileConfiguration = OutputFileConfiguration(),
    val filenameSanitizationResult: FilenameSanitizationResult = FilenameSanitizationResult("edited_image", "edited_image", true),
    val showFileConflictDialog: Boolean = false,
    val pendingConflictFileName: String = "",
    // Non-Destructive Editing (Request 31)
    val saveAsOverwriteOriginal: Boolean = false,
    val showOverwriteConfirmDialog: Boolean = false,
    val currentInstructions: EditingInstructions = EditingInstructions(),
    // Settings (Request 48)
    val appSettings: AppSettings = AppSettings(),
    // Exact Output Verification (Requirement 52)
    val showVerificationDialog: Boolean = false,
    val lastVerificationReport: OutputVerificationReport? = null,
    val lastResizeReport: ResizeVerificationReport? = null,
    val lastCompressionReport: CompressionVerificationReport? = null,
    val lastConversionReport: FormatConversionVerificationReport? = null,
    // Phase 12 — Security & Privacy Audit State
    val securityAuditReport: SecurityPrivacyEngine.SecurityPrivacyAuditReport? = null,
    val scrubMetadataBeforeShare: Boolean = false
)

class UtilityViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getDatabase(application)
    private val repository = UtilityRepository(database)
    val historyManager = HistoryManager(database)
    val presetManager = PresetManager(database)
    val imageProcessingRepository = ImageProcessingRepository(application)
    val compressImageUseCase = CompressImageUseCase()
    val convertFormatUseCase = ConvertFormatUseCase()
    val dpiPrintUseCase = DpiPrintUseCase()
    val passportIdPhotoUseCase = PassportIdPhotoUseCase()
    val batchProcessingUseCase = BatchProcessingUseCase()
    val advancedEditingUseCase = AdvancedEditingUseCase()
    val fileManagementUseCase = FileManagementUseCase()
    val securityPrivacyUseCase = SecurityPrivacyUseCase()

    val allHistory = repository.allHistory.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
    )
    val favorites = repository.favorites.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
    )
    val allPresets = repository.allPresets.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
    )
    val allFavoriteItems = repository.allFavoriteItems.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
    )
    val favoriteImages = repository.getFavoritesByType(FavoriteType.IMAGE).stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
    )
    val favoritePresets = repository.getFavoritesByType(FavoriteType.PRESET).stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
    )
    val favoriteTools = repository.getFavoritesByType(FavoriteType.RECENT_TOOL).stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
    )
    val favoriteDimensions = repository.getFavoritesByType(FavoriteType.CUSTOM_DIMENSION).stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
    )
    val favoriteCompressionTargets = repository.getFavoritesByType(FavoriteType.COMPRESSION_TARGET).stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList()
    )

    private val _uiState = MutableStateFlow(StudioUiState())
    val uiState: StateFlow<StudioUiState> = _uiState.asStateFlow()

    private var sizeCalculationJob: kotlinx.coroutines.Job? = null
    private var batchProcessingJob: kotlinx.coroutines.Job? = null
    private var batchCancelFlag = AtomicBoolean(false)

    init {
        viewModelScope.launch {
            try {
                val existing = repository.allPresets.first()
                if (existing.isEmpty()) {
                    val defaultPresets = listOf(
                        PresetEntity(
                            name = "Signature 300x100",
                            targetWidth = 300,
                            targetHeight = 100,
                            dpi = 300,
                            format = "JPG",
                            maxFileSizeKb = 20,
                            quality = 90,
                            cropMode = "FIT"
                        ),
                        PresetEntity(
                            name = "Official ID Photo 600x600",
                            targetWidth = 600,
                            targetHeight = 600,
                            dpi = 300,
                            format = "JPG",
                            maxFileSizeKb = 240,
                            quality = 95,
                            cropMode = "FIT"
                        ),
                        PresetEntity(
                            name = "Instagram Story 1080x1920",
                            targetWidth = 1080,
                            targetHeight = 1920,
                            dpi = 300,
                            format = "JPG",
                            maxFileSizeKb = null,
                            quality = 90,
                            cropMode = "FILL"
                        ),
                        PresetEntity(
                            name = "Web Banner 1200x400",
                            targetWidth = 1200,
                            targetHeight = 400,
                            dpi = 150,
                            format = "WEBP",
                            maxFileSizeKb = 100,
                            quality = 85,
                            cropMode = "FIT"
                        )
                    )
                    defaultPresets.forEach { repository.insertPreset(it) }
                }

                // Initialize Default Favorites (Request 38)
                val existingFavorites = repository.allFavoriteItems.first()
                if (existingFavorites.isEmpty()) {
                    val defaultFavorites = listOf(
                        // 1. Favorite Tools
                        FavoriteItemEntity(
                            type = FavoriteType.RECENT_TOOL,
                            title = "Crop Photo",
                            subtitle = "Free & fixed aspect ratio cropping",
                            dataKey = "CROP"
                        ),
                        FavoriteItemEntity(
                            type = FavoriteType.RECENT_TOOL,
                            title = "Exact Size",
                            subtitle = "Pixel dimensions & scale mode",
                            dataKey = "EXACT_SIZE"
                        ),
                        FavoriteItemEntity(
                            type = FavoriteType.RECENT_TOOL,
                            title = "Compress Photo",
                            subtitle = "Target file size & quality targeting",
                            dataKey = "COMPRESS"
                        ),
                        FavoriteItemEntity(
                            type = FavoriteType.RECENT_TOOL,
                            title = "Perspective Deskew",
                            subtitle = "4-point document correction",
                            dataKey = "PERSPECTIVE"
                        ),
                        FavoriteItemEntity(
                            type = FavoriteType.RECENT_TOOL,
                            title = "Passport & ID",
                            subtitle = "Official standard sizes & biometric guides",
                            dataKey = "PASSPORT"
                        ),

                        // 2. Favorite Custom Dimensions
                        FavoriteItemEntity(
                            type = FavoriteType.CUSTOM_DIMENSION,
                            title = "1920 × 1080 (FHD 16:9)",
                            subtitle = "Full HD Landscape • 300 DPI",
                            dataKey = "DIM_1920_1080_PX",
                            width = 1920,
                            height = 1080,
                            dpi = 300,
                            unit = "PX"
                        ),
                        FavoriteItemEntity(
                            type = FavoriteType.CUSTOM_DIMENSION,
                            title = "1080 × 1350 (Portrait 4:5)",
                            subtitle = "Social Media Feed • 300 DPI",
                            dataKey = "DIM_1080_1350_PX",
                            width = 1080,
                            height = 1350,
                            dpi = 300,
                            unit = "PX"
                        ),
                        FavoriteItemEntity(
                            type = FavoriteType.CUSTOM_DIMENSION,
                            title = "1080 × 1080 (Square 1:1)",
                            subtitle = "Standard Square • 300 DPI",
                            dataKey = "DIM_1080_1080_PX",
                            width = 1080,
                            height = 1080,
                            dpi = 300,
                            unit = "PX"
                        ),
                        FavoriteItemEntity(
                            type = FavoriteType.CUSTOM_DIMENSION,
                            title = "1200 × 630 (Banner 1.91:1)",
                            subtitle = "Web & Open Graph Share",
                            dataKey = "DIM_1200_630_PX",
                            width = 1200,
                            height = 630,
                            dpi = 300,
                            unit = "PX"
                        ),
                        FavoriteItemEntity(
                            type = FavoriteType.CUSTOM_DIMENSION,
                            title = "3840 × 2160 (4K UHD)",
                            subtitle = "Ultra High Definition • 300 DPI",
                            dataKey = "DIM_3840_2160_PX",
                            width = 3840,
                            height = 2160,
                            dpi = 300,
                            unit = "PX"
                        ),

                        // 3. Favorite Compression Targets
                        FavoriteItemEntity(
                            type = FavoriteType.COMPRESSION_TARGET,
                            title = "Signature / Exam (≤20 KB)",
                            subtitle = "Strict upload portal limit • JPEG",
                            dataKey = "COMP_20KB_JPEG",
                            targetFileSizeKb = 20,
                            quality = 85,
                            format = "JPEG"
                        ),
                        FavoriteItemEntity(
                            type = FavoriteType.COMPRESSION_TARGET,
                            title = "Govt Portal / ID (≤50 KB)",
                            subtitle = "Official documents limit • JPEG",
                            dataKey = "COMP_50KB_JPEG",
                            targetFileSizeKb = 50,
                            quality = 88,
                            format = "JPEG"
                        ),
                        FavoriteItemEntity(
                            type = FavoriteType.COMPRESSION_TARGET,
                            title = "Photo Upload (≤100 KB)",
                            subtitle = "Exam & Job portals • JPEG",
                            dataKey = "COMP_100KB_JPEG",
                            targetFileSizeKb = 100,
                            quality = 90,
                            format = "JPEG"
                        ),
                        FavoriteItemEntity(
                            type = FavoriteType.COMPRESSION_TARGET,
                            title = "Job Application (≤200 KB)",
                            subtitle = "Resume & ID documents • JPEG",
                            dataKey = "COMP_200KB_JPEG",
                            targetFileSizeKb = 200,
                            quality = 92,
                            format = "JPEG"
                        ),
                        FavoriteItemEntity(
                            type = FavoriteType.COMPRESSION_TARGET,
                            title = "Email / Web (≤500 KB)",
                            subtitle = "Fast loading web images • WEBP",
                            dataKey = "COMP_500KB_WEBP",
                            targetFileSizeKb = 500,
                            quality = 90,
                            format = "WEBP"
                        ),
                        FavoriteItemEntity(
                            type = FavoriteType.COMPRESSION_TARGET,
                            title = "High Quality Web (≤1 MB)",
                            subtitle = "High fidelity optimization • JPEG",
                            dataKey = "COMP_1024KB_JPEG",
                            targetFileSizeKb = 1024,
                            quality = 95,
                            format = "JPEG"
                        ),

                        // 4. Favorite Presets
                        FavoriteItemEntity(
                            type = FavoriteType.PRESET,
                            title = "Instagram Post (1080×1080)",
                            subtitle = "Social Media • 1:1 Square",
                            dataKey = "PRESET_IG_POST",
                            width = 1080,
                            height = 1080,
                            format = "JPEG"
                        ),
                        FavoriteItemEntity(
                            type = FavoriteType.PRESET,
                            title = "Instagram Story (1080×1920)",
                            subtitle = "Social Media • 9:16 Vertical",
                            dataKey = "PRESET_IG_STORY",
                            width = 1080,
                            height = 1920,
                            format = "JPEG"
                        ),
                        FavoriteItemEntity(
                            type = FavoriteType.PRESET,
                            title = "YouTube Thumbnail (1280×720)",
                            subtitle = "Social Media • 16:9 Landscape",
                            dataKey = "PRESET_YT_THUMB",
                            width = 1280,
                            height = 720,
                            format = "JPEG"
                        ),
                        FavoriteItemEntity(
                            type = FavoriteType.PRESET,
                            title = "US Passport 2×2 in",
                            subtitle = "Passport & Visa • 600×600 px • 300 DPI",
                            dataKey = "PRESET_US_PASSPORT",
                            width = 600,
                            height = 600,
                            dpi = 300,
                            unit = "IN",
                            format = "JPEG"
                        )
                    )
                    defaultFavorites.forEach { repository.insertFavorite(it) }
                }
            } catch (e: Exception) {
                // Ignore initialization failures
            }
            OutputFileManager.cleanOldCacheFiles(getApplication(), maxAgeHours = 12)
            loadSocialPresetsFromStorage()
        }
    }

    fun selectTab(tab: StudioTab) {
        _uiState.value = _uiState.value.copy(activeTab = tab)
        if (tab == StudioTab.SHEET) {
            generatePhotoSheet()
        } else {
            updatePreview()
        }
    }

    fun setShowImageInfoDialog(show: Boolean) {
        _uiState.value = _uiState.value.copy(showImageInfoDialog = show)
    }

    fun toggleImageInfoDialog() {
        _uiState.value = _uiState.value.copy(showImageInfoDialog = !_uiState.value.showImageInfoDialog)
    }

    fun setShowVerificationDialog(show: Boolean) {
        _uiState.value = _uiState.value.copy(showVerificationDialog = show)
    }

    fun openVerificationDialog(report: OutputVerificationReport? = null) {
        val rep = report ?: _uiState.value.lastExportResult?.verificationReport
        _uiState.value = _uiState.value.copy(
            showVerificationDialog = true,
            lastVerificationReport = rep
        )
    }

    // Non-Destructive Editing & Instructions Pipeline (Request 31)
    fun getCurrentEditingInstructions(description: String = ""): EditingInstructions {
        val s = _uiState.value
        return EditingInstructions(
            rotationAngle = s.rotationAngle,
            straightenAngle = s.straightenAngle,
            isFlippedHorizontally = s.isFlippedHorizontally,
            isFlippedVertically = s.isFlippedVertically,
            perspectiveQuad = if (s.perspectiveQuad != PerspectiveEngine.PerspectiveQuad.default()) s.perspectiveQuad else null,
            perspectivePreset = s.perspectiveOutputPreset,
            perspectiveEnhanceMode = s.perspectiveEnhanceMode,
            cropRect = if (s.activeCropBounds.left > 0.001f || s.activeCropBounds.top > 0.001f || s.activeCropBounds.right < 0.999f || s.activeCropBounds.bottom < 0.999f) s.activeCropBounds else null,
            bgConfig = if (s.activeTab == StudioTab.BACKGROUND) s.bgConfig else null,
            enhancementParams = getCurrentEnhancementParams(),
            targetWidthPx = s.targetWidthPx,
            targetHeightPx = s.targetHeightPx,
            resizeMode = s.resizeMode,
            fitBackgroundColor = s.fitBackgroundColor,
            dpi = s.dpi,
            stepDescription = description
        )
    }

    private fun applyInstructionsToState(instructions: EditingInstructions) {
        _uiState.value = _uiState.value.copy(
            rotationAngle = instructions.rotationAngle,
            straightenAngle = instructions.straightenAngle,
            isFlippedHorizontally = instructions.isFlippedHorizontally,
            isFlippedVertically = instructions.isFlippedVertically,
            perspectiveQuad = instructions.perspectiveQuad ?: PerspectiveEngine.PerspectiveQuad.default(),
            perspectiveOutputPreset = instructions.perspectivePreset,
            perspectiveEnhanceMode = instructions.perspectiveEnhanceMode,
            activeCropBounds = instructions.cropRect ?: RectF(0.05f, 0.05f, 0.95f, 0.95f),
            bgConfig = instructions.bgConfig ?: BackgroundProcessor.BackgroundConfig(),
            brightness = instructions.enhancementParams.brightness,
            contrast = instructions.enhancementParams.contrast,
            saturation = instructions.enhancementParams.saturation,
            exposure = instructions.enhancementParams.exposure,
            highlights = instructions.enhancementParams.highlights,
            shadows = instructions.enhancementParams.shadows,
            sharpness = instructions.enhancementParams.sharpness,
            warmth = instructions.enhancementParams.warmth,
            vignette = instructions.enhancementParams.vignette,
            vibrance = instructions.enhancementParams.vibrance,
            gamma = instructions.enhancementParams.gamma,
            claheStrength = instructions.enhancementParams.claheStrength,
            isGrayscale = instructions.enhancementParams.isGrayscale,
            isBlackAndWhite = instructions.enhancementParams.isBlackAndWhite,
            bwThreshold = instructions.enhancementParams.bwThreshold,
            activeFilter = instructions.enhancementParams.filter,
            targetWidthPx = instructions.targetWidthPx ?: _uiState.value.originalBitmap?.width ?: 1080,
            targetHeightPx = instructions.targetHeightPx ?: _uiState.value.originalBitmap?.height ?: 1080,
            resizeMode = instructions.resizeMode,
            fitBackgroundColor = instructions.fitBackgroundColor,
            dpi = instructions.dpi
        )
    }

    private fun renderPipeline(instructions: EditingInstructions) {
        val original = _uiState.value.originalBitmap ?: return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            val rendered = withContext(Dispatchers.Default) {
                NonDestructivePipeline.applyPipeline(original, instructions)
            }
            _uiState.value = _uiState.value.copy(
                workingBitmap = rendered,
                previewBitmap = rendered,
                targetWidthPx = rendered.width,
                targetHeightPx = rendered.height,
                calcDimensionWidth = rendered.width,
                calcDimensionHeight = rendered.height,
                isLoading = false
            )
            calculateEstimatedSize()
        }
    }

    // Unified Editor Controls (Request 30 & 31)
    fun recordHistoryState(description: String) {
        if (_uiState.value.originalBitmap == null) return
        val instructions = getCurrentEditingInstructions(description)
        val snapshot = EditorHistorySnapshot(
            description = description,
            instructions = instructions
        )
        // Store lightweight instructions: up to 30 steps with zero duplicate full-resolution bitmaps
        val newUndo = (_uiState.value.globalUndoStack + snapshot).takeLast(30)
        _uiState.value = _uiState.value.copy(
            globalUndoStack = newUndo,
            globalRedoStack = emptyList(),
            currentInstructions = instructions
        )
    }

    fun undo() {
        val undoStack = _uiState.value.globalUndoStack
        val original = _uiState.value.originalBitmap
        if (undoStack.isEmpty() || original == null) return

        val currentInstructions = getCurrentEditingInstructions("Current")
        val redoSnapshot = EditorHistorySnapshot(
            description = "Redo Step",
            instructions = currentInstructions
        )
        val newRedo = (_uiState.value.globalRedoStack + redoSnapshot).takeLast(30)

        val targetSnapshot = undoStack.last()
        val newUndo = undoStack.dropLast(1)

        applyInstructionsToState(targetSnapshot.instructions)
        _uiState.value = _uiState.value.copy(
            globalUndoStack = newUndo,
            globalRedoStack = newRedo,
            currentInstructions = targetSnapshot.instructions,
            statusMessage = "Undid: ${targetSnapshot.description}"
        )
        renderPipeline(targetSnapshot.instructions)
    }

    fun redo() {
        val redoStack = _uiState.value.globalRedoStack
        val original = _uiState.value.originalBitmap
        if (redoStack.isEmpty() || original == null) return

        val currentInstructions = getCurrentEditingInstructions("Current")
        val undoSnapshot = EditorHistorySnapshot(
            description = "Undo Step",
            instructions = currentInstructions
        )
        val newUndo = (_uiState.value.globalUndoStack + undoSnapshot).takeLast(30)

        val targetSnapshot = redoStack.last()
        val newRedo = redoStack.dropLast(1)

        applyInstructionsToState(targetSnapshot.instructions)
        _uiState.value = _uiState.value.copy(
            globalUndoStack = newUndo,
            globalRedoStack = newRedo,
            currentInstructions = targetSnapshot.instructions,
            statusMessage = "Redid: ${targetSnapshot.description}"
        )
        renderPipeline(targetSnapshot.instructions)
    }

    fun resetToOriginal() {
        val original = _uiState.value.originalBitmap ?: return
        recordHistoryState("Before Reset")
        val defaultInstructions = EditingInstructions()
        applyInstructionsToState(defaultInstructions)
        _uiState.value = _uiState.value.copy(
            workingBitmap = original,
            previewBitmap = original,
            targetWidthPx = original.width,
            targetHeightPx = original.height,
            calcDimensionWidth = original.width,
            calcDimensionHeight = original.height,
            isBeforeAfterActive = false,
            currentInstructions = defaultInstructions,
            statusMessage = "Reset to pristine original"
        )
        resetEnhancements()
        calculateEstimatedSize()
    }

    fun toggleBeforeAfter() {
        _uiState.value = _uiState.value.copy(isBeforeAfterActive = !_uiState.value.isBeforeAfterActive)
    }

    fun setBeforeAfter(active: Boolean) {
        _uiState.value = _uiState.value.copy(isBeforeAfterActive = active)
    }

    fun setFullScreenPreview(show: Boolean) {
        _uiState.value = _uiState.value.copy(isFullScreenPreviewOpen = show)
    }

    fun setShowSaveAsDialog(show: Boolean) {
        _uiState.value = _uiState.value.copy(showSaveAsDialog = show)
    }

    // Exact Size methods (Request 30)
    fun setExactSizeUnit(unit: ExactSizeUnit) {
        val currentW = _uiState.value.exactWidthInput.toFloatOrNull() ?: 1080f
        val currentH = _uiState.value.exactHeightInput.toFloatOrNull() ?: 1080f
        val currentUnit = _uiState.value.exactSizeUnit
        val dpi = _uiState.value.dpi.toFloat()

        val pxW = when (currentUnit) {
            ExactSizeUnit.PX -> currentW
            ExactSizeUnit.IN -> currentW * dpi
            ExactSizeUnit.CM -> (currentW / 2.54f) * dpi
            ExactSizeUnit.MM -> (currentW / 25.4f) * dpi
        }
        val pxH = when (currentUnit) {
            ExactSizeUnit.PX -> currentH
            ExactSizeUnit.IN -> currentH * dpi
            ExactSizeUnit.CM -> (currentH / 2.54f) * dpi
            ExactSizeUnit.MM -> (currentH / 25.4f) * dpi
        }

        val newW = when (unit) {
            ExactSizeUnit.PX -> pxW.roundToInt().toString()
            ExactSizeUnit.IN -> String.format("%.2f", pxW / dpi)
            ExactSizeUnit.CM -> String.format("%.2f", (pxW / dpi) * 2.54f)
            ExactSizeUnit.MM -> String.format("%.1f", (pxW / dpi) * 25.4f)
        }
        val newH = when (unit) {
            ExactSizeUnit.PX -> pxH.roundToInt().toString()
            ExactSizeUnit.IN -> String.format("%.2f", pxH / dpi)
            ExactSizeUnit.CM -> String.format("%.2f", (pxH / dpi) * 2.54f)
            ExactSizeUnit.MM -> String.format("%.1f", (pxH / dpi) * 25.4f)
        }

        _uiState.value = _uiState.value.copy(
            exactSizeUnit = unit,
            exactWidthInput = newW,
            exactHeightInput = newH
        )
    }

    fun setExactWidthInput(input: String) {
        val wVal = input.toFloatOrNull()
        val aspectLocked = _uiState.value.exactAspectLocked
        val currentBmp = _uiState.value.workingBitmap

        var newH = _uiState.value.exactHeightInput
        if (aspectLocked && wVal != null && wVal > 0 && currentBmp != null && currentBmp.width > 0) {
            val ratio = currentBmp.height.toFloat() / currentBmp.width.toFloat()
            newH = when (_uiState.value.exactSizeUnit) {
                ExactSizeUnit.PX -> (wVal * ratio).roundToInt().toString()
                else -> String.format("%.2f", wVal * ratio)
            }
        }
        _uiState.value = _uiState.value.copy(
            exactWidthInput = input,
            exactHeightInput = newH
        )
    }

    fun setExactHeightInput(input: String) {
        val hVal = input.toFloatOrNull()
        val aspectLocked = _uiState.value.exactAspectLocked
        val currentBmp = _uiState.value.workingBitmap

        var newW = _uiState.value.exactWidthInput
        if (aspectLocked && hVal != null && hVal > 0 && currentBmp != null && currentBmp.height > 0) {
            val ratio = currentBmp.width.toFloat() / currentBmp.height.toFloat()
            newW = when (_uiState.value.exactSizeUnit) {
                ExactSizeUnit.PX -> (hVal * ratio).roundToInt().toString()
                else -> String.format("%.2f", hVal * ratio)
            }
        }
        _uiState.value = _uiState.value.copy(
            exactWidthInput = newW,
            exactHeightInput = input
        )
    }

    fun setExactAspectLocked(locked: Boolean) {
        _uiState.value = _uiState.value.copy(exactAspectLocked = locked)
    }

    fun setExactSizeMode(mode: ResizeMode) {
        _uiState.value = _uiState.value.copy(exactSizeMode = mode)
    }

    fun setExactBgColor(color: Int) {
        _uiState.value = _uiState.value.copy(exactBgColor = color)
    }

    fun applyExactPreset(preset: ExactSizePreset) {
        _uiState.value = _uiState.value.copy(
            exactSizeUnit = preset.unit,
            exactWidthInput = if (preset.unit == ExactSizeUnit.PX) preset.width.toInt().toString() else preset.width.toString(),
            exactHeightInput = if (preset.unit == ExactSizeUnit.PX) preset.height.toInt().toString() else preset.height.toString()
        )
    }

    fun applyExactSize() {
        val current = _uiState.value.workingBitmap ?: return
        val unit = _uiState.value.exactSizeUnit
        val dpi = _uiState.value.dpi.toFloat()
        val wFloat = _uiState.value.exactWidthInput.toFloatOrNull() ?: current.width.toFloat()
        val hFloat = _uiState.value.exactHeightInput.toFloatOrNull() ?: current.height.toFloat()

        val targetW = when (unit) {
            ExactSizeUnit.PX -> wFloat.toInt()
            ExactSizeUnit.IN -> (wFloat * dpi).toInt()
            ExactSizeUnit.CM -> ((wFloat / 2.54f) * dpi).toInt()
            ExactSizeUnit.MM -> ((wFloat / 25.4f) * dpi).toInt()
        }.coerceIn(1, 10000)

        val targetH = when (unit) {
            ExactSizeUnit.PX -> hFloat.toInt()
            ExactSizeUnit.IN -> (hFloat * dpi).toInt()
            ExactSizeUnit.CM -> ((hFloat / 2.54f) * dpi).toInt()
            ExactSizeUnit.MM -> ((hFloat / 25.4f) * dpi).toInt()
        }.coerceIn(1, 10000)

        recordHistoryState("Exact Size ${targetW}x${targetH} px")

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            val result = withContext(Dispatchers.Default) {
                ResizeEngine.resize(
                    current,
                    targetW,
                    targetH,
                    _uiState.value.exactSizeMode,
                    _uiState.value.exactBgColor
                )
            }
            val resized = result.bitmap
            _uiState.value = _uiState.value.copy(
                workingBitmap = resized,
                previewBitmap = resized,
                targetWidthPx = resized.width,
                targetHeightPx = resized.height,
                calcDimensionWidth = resized.width,
                calcDimensionHeight = resized.height,
                lastResizeReport = result.verificationReport,
                isLoading = false,
                statusMessage = "Exact size applied: ${resized.width} × ${resized.height} px (${_uiState.value.exactSizeMode.label})"
            )
            calculateEstimatedSize()
        }
    }

    fun applyExactWidth(width: Int) {
        val current = _uiState.value.workingBitmap ?: return
        val targetW = width.coerceIn(1, 10000)
        recordHistoryState("Exact Width ${targetW} px")
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            val result = withContext(Dispatchers.Default) {
                ResizeEngine.resizeExactWidth(current, targetW)
            }
            val resized = result.bitmap
            _uiState.value = _uiState.value.copy(
                workingBitmap = resized,
                previewBitmap = resized,
                targetWidthPx = resized.width,
                targetHeightPx = resized.height,
                calcDimensionWidth = resized.width,
                calcDimensionHeight = resized.height,
                lastResizeReport = result.verificationReport,
                isLoading = false,
                statusMessage = "Exact width applied: ${resized.width} × ${resized.height} px"
            )
            calculateEstimatedSize()
        }
    }

    fun applyExactHeight(height: Int) {
        val current = _uiState.value.workingBitmap ?: return
        val targetH = height.coerceIn(1, 10000)
        recordHistoryState("Exact Height ${targetH} px")
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            val result = withContext(Dispatchers.Default) {
                ResizeEngine.resizeExactHeight(current, targetH)
            }
            val resized = result.bitmap
            _uiState.value = _uiState.value.copy(
                workingBitmap = resized,
                previewBitmap = resized,
                targetWidthPx = resized.width,
                targetHeightPx = resized.height,
                calcDimensionWidth = resized.width,
                calcDimensionHeight = resized.height,
                lastResizeReport = result.verificationReport,
                isLoading = false,
                statusMessage = "Exact height applied: ${resized.width} × ${resized.height} px"
            )
            calculateEstimatedSize()
        }
    }

    // Save As & Output File Management methods (Request 30, 36)
    fun setSaveAsFileName(name: String) {
        val sanitization = OutputFileManager.sanitizeFilename(name)
        _uiState.value = _uiState.value.copy(
            saveAsFileName = name,
            filenameSanitizationResult = sanitization,
            outputFileConfig = _uiState.value.outputFileConfig.copy(
                customExactName = name
            )
        )
    }

    fun updateOutputFileConfig(updater: (OutputFileConfiguration) -> OutputFileConfiguration) {
        val newConfig = updater(_uiState.value.outputFileConfig)
        val sanitization = OutputFileManager.sanitizeFilename(
            if (newConfig.namingMode == NamingMode.EXACT_CUSTOM) newConfig.customExactName else newConfig.customTemplate
        )
        _uiState.value = _uiState.value.copy(
            outputFileConfig = newConfig,
            saveAsFileName = newConfig.customExactName,
            filenameSanitizationResult = sanitization
        )
    }

    fun setOutputFileConfig(config: OutputFileConfiguration) {
        val sanitization = OutputFileManager.sanitizeFilename(
            if (config.namingMode == NamingMode.EXACT_CUSTOM) config.customExactName else config.customTemplate
        )
        _uiState.value = _uiState.value.copy(
            outputFileConfig = config,
            saveAsFileName = config.customExactName,
            filenameSanitizationResult = sanitization
        )
    }

    fun setNamingMode(mode: NamingMode) {
        updateOutputFileConfig { it.copy(namingMode = mode) }
    }

    fun setOutputDestinationType(destinationType: OutputDestinationType) {
        updateOutputFileConfig { it.copy(destinationType = destinationType) }
    }

    fun setSafTreeUri(uri: Uri?, displayName: String? = null) {
        updateOutputFileConfig {
            it.copy(
                safTreeUriString = uri?.toString(),
                safTreeDisplayName = displayName ?: uri?.lastPathSegment?.substringAfterLast(':') ?: "Selected Folder",
                destinationType = if (uri != null) OutputDestinationType.CUSTOM_SAF_DIRECTORY else OutputDestinationType.PUBLIC_MEDIASTORE
            )
        }
    }

    fun setOverwriteConflictAction(action: OverwriteConflictAction) {
        updateOutputFileConfig { it.copy(overwriteAction = action) }
    }

    fun setShowFileConflictDialog(show: Boolean) {
        _uiState.value = _uiState.value.copy(showFileConflictDialog = show)
    }

    fun setSaveAsFormat(format: ExportFormat) {
        _uiState.value = _uiState.value.copy(saveAsFormat = format)
    }

    fun setSaveAsQuality(quality: Int) {
        _uiState.value = _uiState.value.copy(saveAsQuality = quality)
    }

    fun setSaveAsTargetKb(kb: String) {
        _uiState.value = _uiState.value.copy(saveAsTargetKb = kb)
    }

    fun setSaveAsDpi(dpi: Int) {
        _uiState.value = _uiState.value.copy(saveAsDpi = dpi)
    }

    fun setSaveAsPrivacyMode(policy: MetadataPolicy) {
        _uiState.value = _uiState.value.copy(saveAsPrivacyMode = policy)
    }

    fun setSaveAsOverwriteOriginal(overwrite: Boolean) {
        _uiState.value = _uiState.value.copy(saveAsOverwriteOriginal = overwrite)
    }

    fun setShowOverwriteConfirmDialog(show: Boolean) {
        _uiState.value = _uiState.value.copy(showOverwriteConfirmDialog = show)
    }

    fun executeSaveAs(
        forceOverwrite: Boolean = false,
        onConflictDetected: ((String) -> Unit)? = null,
        onComplete: (ExportEngine.ExportResult) -> Unit
    ) {
        val original = _uiState.value.originalBitmap ?: return
        val format = _uiState.value.saveAsFormat
        val quality = _uiState.value.saveAsQuality
        val dpi = _uiState.value.saveAsDpi
        val targetKb = _uiState.value.saveAsTargetKb.toIntOrNull()
        val policy = _uiState.value.saveAsPrivacyMode
        val overwrite = forceOverwrite || _uiState.value.saveAsOverwriteOriginal
        val config = _uiState.value.outputFileConfig

        // Compute resolved safe filename using OutputFileManager
        val resolvedFileName = OutputFileManager.generateFormattedFilename(
            config = config,
            originalName = _uiState.value.originalFileName.ifBlank { "image" },
            width = _uiState.value.targetWidthPx,
            height = _uiState.value.targetHeightPx,
            dpi = dpi,
            format = format
        )
        val baseFileName = resolvedFileName.substringBeforeLast('.')

        // Check for SAF folder conflict if user selected custom folder and action is ASK
        val safUri = config.safTreeUri
        if (!overwrite && config.destinationType == OutputDestinationType.CUSTOM_SAF_DIRECTORY && safUri != null) {
            val exists = OutputFileManager.checkFileExists(
                context = getApplication(),
                destinationType = config.destinationType,
                safTreeUri = safUri,
                displayName = resolvedFileName,
                mimeType = format.mimeType
            )
            if (exists) {
                if (config.overwriteAction == OverwriteConflictAction.ASK_BEFORE_OVERWRITE) {
                    _uiState.value = _uiState.value.copy(
                        showFileConflictDialog = true,
                        pendingConflictFileName = resolvedFileName
                    )
                    onConflictDetected?.invoke(resolvedFileName)
                    return
                }
            }
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isExporting = true)
            val instructions = getCurrentEditingInstructions("Export Pipeline")

            val result = withContext(Dispatchers.IO) {
                // Non-Destructive Flow: Original → Editing Instructions → Exported Image
                val processedBitmap = NonDestructivePipeline.applyPipeline(original, instructions)
                ExportEngine.exportImage(
                    context = getApplication(),
                    bitmap = processedBitmap,
                    format = format,
                    quality = quality,
                    targetSizeKb = targetKb,
                    fileNamePrefix = resolvedFileName,
                    customExactFileName = resolvedFileName,
                    dpi = dpi,
                    sourceUri = _uiState.value.currentImageUri,
                    privacyConfig = MetadataPrivacyConfig(policy = policy),
                    overwriteOriginal = overwrite,
                    destinationType = config.destinationType,
                    safTreeUri = safUri
                )
            }

            _uiState.value = _uiState.value.copy(
                isExporting = false,
                lastExportResult = result,
                lastVerificationReport = result.verificationReport,
                showSaveAsDialog = false,
                showFileConflictDialog = false,
                statusMessage = if (result.success) {
                    if (result.isOverwritten) "Original photo overwritten successfully"
                    else "Saved to ${result.outputFile?.name ?: "Gallery"}"
                } else {
                    "Save error: ${result.errorMessage}"
                }
            )
            if (result.success) {
                recordOperationHistory(
                    title = resolvedFileName,
                    operationType = _uiState.value.activeTab.label,
                    operationDetails = "${result.width}×${result.height} px • ${format.extension.uppercase()} $quality% • $dpi DPI",
                    result = result,
                    sourceOriginalUri = _uiState.value.currentImageUri,
                    originalBmp = original,
                    instructions = instructions
                )
            }
            onComplete(result)
        }
    }

    fun executeSaveDirectlyToDocumentUri(documentUri: Uri, onComplete: (ExportEngine.ExportResult) -> Unit) {
        val original = _uiState.value.originalBitmap ?: return
        val format = _uiState.value.saveAsFormat
        val quality = _uiState.value.saveAsQuality
        val dpi = _uiState.value.saveAsDpi
        val targetKb = _uiState.value.saveAsTargetKb.toIntOrNull()
        val policy = _uiState.value.saveAsPrivacyMode

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isExporting = true)
            val instructions = getCurrentEditingInstructions("Save As Pipeline")

            val result = withContext(Dispatchers.IO) {
                val processedBitmap = NonDestructivePipeline.applyPipeline(original, instructions)
                ExportEngine.exportDirectlyToDocumentUri(
                    context = getApplication(),
                    bitmap = processedBitmap,
                    documentUri = documentUri,
                    format = format,
                    quality = quality,
                    targetSizeKb = targetKb,
                    dpi = dpi,
                    privacyConfig = MetadataPrivacyConfig(policy = policy),
                    sourceUri = _uiState.value.currentImageUri
                )
            }

            _uiState.value = _uiState.value.copy(
                isExporting = false,
                lastExportResult = result,
                lastVerificationReport = result.verificationReport,
                showSaveAsDialog = false,
                statusMessage = if (result.success) "Saved successfully to chosen location!" else "Save error: ${result.errorMessage}"
            )
            if (result.success) {
                val docName = documentUri.lastPathSegment?.substringAfterLast(':') ?: "Document Export"
                recordOperationHistory(
                    title = docName,
                    operationType = "Save As (${_uiState.value.activeTab.label})",
                    operationDetails = "${result.width}×${result.height} px • ${format.extension.uppercase()} $quality% • $dpi DPI",
                    result = result,
                    sourceOriginalUri = _uiState.value.currentImageUri,
                    originalBmp = original,
                    instructions = instructions
                )
            }
            onComplete(result)
        }
    }

    fun shareCurrentResult(context: android.content.Context) {
        val file = _uiState.value.lastExportResult?.outputFile ?: return
        val format = _uiState.value.lastExportResult?.format ?: _uiState.value.exportFormat
        ExportEngine.shareImage(context, file, format.mimeType, "Share Image")
    }

    fun loadSourceImage(uri: Uri) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, statusMessage = "Loading image...")
            try {
                val bitmap = withContext(Dispatchers.IO) {
                    BitmapUtils.decodeSampledBitmapFromUri(getApplication(), uri, 2560, 2560)
                }
                val exif = withContext(Dispatchers.IO) {
                    ExifManager.readExif(getApplication(), uri)
                }
                val header = withContext(Dispatchers.IO) {
                    BitmapUtils.getImageHeader(getApplication(), uri)
                }
                val mimeType = withContext(Dispatchers.IO) {
                    try {
                        getApplication<Application>().contentResolver.getType(uri) ?: "image/jpeg"
                    } catch (e: Exception) {
                        "image/jpeg"
                    }
                }

                val detectedFormat = withContext(Dispatchers.IO) {
                    BitmapUtils.detectFormat(getApplication(), uri)
                }

                if (bitmap != null) {
                    val isTransparent = withContext(Dispatchers.Default) {
                        BitmapUtils.hasTransparency(bitmap)
                    }
                    val origBytes = if (header.fileSizeBytes > 0) header.fileSizeBytes else (bitmap.byteCount / 4).toLong()
                    val origW = if (header.width > 0) header.width else bitmap.width
                    val origH = if (header.height > 0) header.height else bitmap.height
                    val fileName = header.fileName.ifEmpty { BitmapUtils.getFileName(getApplication(), uri) }

                    val origDpi = header.dpi
                    val initialDpi = origDpi ?: 300

                    _uiState.value = _uiState.value.copy(
                        currentImageUri = uri,
                        originalBitmap = bitmap,
                        workingBitmap = bitmap,
                        previewBitmap = bitmap,
                        targetWidthPx = bitmap.width,
                        targetHeightPx = bitmap.height,
                        calcDimensionWidth = bitmap.width,
                        calcDimensionHeight = bitmap.height,
                        calcDimensionWidthInput = bitmap.width.toString(),
                        calcDimensionHeightInput = bitmap.height.toString(),
                        ratioCalcWInput = bitmap.width.toString(),
                        ratioCalcHInput = bitmap.height.toString(),
                        originalFileSizeBytes = origBytes,
                        originalMimeType = mimeType,
                        originalWidth = origW,
                        originalHeight = origH,
                        originalFileName = fileName,
                        detectedInputFormat = detectedFormat,
                        hasTransparency = isTransparent,
                        originalReadDpi = origDpi,
                        dpi = initialDpi,
                        customDpiInput = initialDpi.toString(),
                        exifData = exif,
                        isLoading = false,
                        statusMessage = null
                    )
                    calculateEstimatedSize()
                } else {
                    val friendlyError = when {
                        header.width <= 0 || header.height <= 0 -> "Unable to read image. The file may be damaged, corrupted, or in an unsupported format."
                        else -> "Could not load image. The file could not be read."
                    }
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        statusMessage = friendlyError
                    )
                }
            } catch (e: OutOfMemoryError) {
                System.gc()
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    statusMessage = "Image is too large to fit into available memory. Try downsampling or choosing a smaller photo."
                )
            } catch (e: SecurityException) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    statusMessage = "Permission denied. Access to this file was revoked or restricted by your device."
                )
            } catch (e: java.io.FileNotFoundException) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    statusMessage = "File not found. The image may have been moved, renamed, or deleted from storage."
                )
            } catch (e: Exception) {
                e.printStackTrace()
                val msg = e.localizedMessage?.takeIf { it.isNotBlank() } ?: "An unexpected error occurred while opening the image."
                _uiState.value = _uiState.value.copy(isLoading = false, statusMessage = msg)
            }
        }
    }

    fun setWorkingBitmapDirectly(bitmap: Bitmap, title: String = "Canvas Image") {
        val estimatedOriginalBytes = (bitmap.width * bitmap.height * 3 / 2).toLong()
        _uiState.value = _uiState.value.copy(
            originalBitmap = bitmap,
            workingBitmap = bitmap,
            previewBitmap = bitmap,
            targetWidthPx = bitmap.width,
            targetHeightPx = bitmap.height,
            calcDimensionWidth = bitmap.width,
            calcDimensionHeight = bitmap.height,
            calcDimensionWidthInput = bitmap.width.toString(),
            calcDimensionHeightInput = bitmap.height.toString(),
            ratioCalcWInput = bitmap.width.toString(),
            ratioCalcHInput = bitmap.height.toString(),
            originalFileSizeBytes = estimatedOriginalBytes,
            originalMimeType = "image/jpeg",
            originalWidth = bitmap.width,
            originalHeight = bitmap.height
        )
        calculateEstimatedSize()
    }

    fun setCropBounds(bounds: RectF) {
        _uiState.value = _uiState.value.copy(activeCropBounds = bounds)
    }

    fun setCropRatio(ratio: CropAspectRatio) {
        _uiState.value = _uiState.value.copy(selectedCropRatio = ratio)
    }

    fun setCropShape(shape: CropShape) {
        _uiState.value = _uiState.value.copy(cropShape = shape)
    }

    fun setCropGuideGrid(grid: CropGuideGrid) {
        _uiState.value = _uiState.value.copy(cropGuideGrid = grid)
    }

    fun setCropCornerRadius(radiusPx: Float) {
        _uiState.value = _uiState.value.copy(cropCornerRadiusPx = radiusPx)
    }

    fun invertCropRatio() {
        val current = _uiState.value.selectedCropRatio
        if (current.ratioX != null && current.ratioY != null) {
            val title = if (current.title.contains(":")) {
                "${current.ratioY.toInt()}:${current.ratioX.toInt()}"
            } else {
                "${current.title} (Flipped)"
            }
            val inverted = CropAspectRatio(title, current.ratioY, current.ratioX)
            _uiState.value = _uiState.value.copy(selectedCropRatio = inverted)
        }
    }

    fun setCropBorderWidth(widthPx: Float) {
        _uiState.value = _uiState.value.copy(cropBorderWidthPx = widthPx.coerceIn(0f, 32f))
    }

    fun setCropBorderColor(colorInt: Int) {
        _uiState.value = _uiState.value.copy(cropBorderColor = colorInt)
    }

    fun resetCropBounds() {
        _uiState.value = _uiState.value.copy(
            selectedCropRatio = CropAspectRatio.FREE,
            cropShape = CropShape.RECTANGLE,
            cropBorderWidthPx = 0f,
            activeCropBounds = RectF(0.05f, 0.05f, 0.95f, 0.95f)
        )
    }

    fun applySmartCrop() {
        val current = _uiState.value.workingBitmap ?: return
        val ratio = _uiState.value.selectedCropRatio
        val rx = ratio.ratioX ?: 1f
        val ry = ratio.ratioY ?: 1f
        val smartBounds = CropEngine.calculateSmartCropBounds(current, rx, ry)
        _uiState.value = _uiState.value.copy(
            activeCropBounds = smartBounds,
            statusMessage = "Smart focus framed around focal subject"
        )
    }

    fun applyCrop() {
        val current = _uiState.value.workingBitmap ?: return
        recordHistoryState("Crop")
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            val borderW = _uiState.value.cropBorderWidthPx
            val borderC = _uiState.value.cropBorderColor
            val shape = _uiState.value.cropShape
            val bounds = _uiState.value.activeCropBounds
            val radiusPx = _uiState.value.cropCornerRadiusPx
            val minDim = kotlin.math.min(current.width, current.height).coerceAtLeast(1)
            val radiusFraction = (radiusPx / minDim).coerceIn(0.02f, 0.45f)

            val cropResult = withContext(Dispatchers.Default) {
                CropEngine.crop(
                    source = current,
                    normalizedBounds = bounds,
                    shape = shape,
                    cornerRadiusPx = radiusPx
                )
            }
            val cropped = if (borderW > 0.5f) {
                withContext(Dispatchers.Default) {
                    CropEngine.applyAdvancedCropWithBorder(
                        bitmap = current,
                        cropRectNormalized = bounds,
                        shape = shape,
                        cornerRadiusFraction = radiusFraction,
                        borderWidthPx = borderW,
                        borderColor = borderC
                    )
                }
            } else {
                cropResult.bitmap
            }
            _uiState.value = _uiState.value.copy(
                workingBitmap = cropped,
                previewBitmap = cropped,
                targetWidthPx = cropped.width,
                targetHeightPx = cropped.height,
                lastCropReport = cropResult.verificationReport,
                isLoading = false,
                statusMessage = "Cropped to ${cropped.width}×${cropped.height} px (${cropResult.shape.label})"
            )
            calculateEstimatedSize()
        }
    }

    fun setAspectRatioLock(locked: Boolean) {
        _uiState.value = _uiState.value.copy(aspectRatioLocked = locked)
    }

    fun setAspectPreset(preset: AspectRatioPreset) {
        val currentW = _uiState.value.calcDimensionWidth
        val currentH = if (_uiState.value.aspectRatioLocked) {
            AspectRatioEngine.calculateHeightFromWidth(currentW, preset.ratioX, preset.ratioY)
        } else {
            _uiState.value.calcDimensionHeight
        }
        _uiState.value = _uiState.value.copy(
            activeAspectPreset = preset,
            isCustomAspectSelected = false,
            calcDimensionHeight = currentH,
            calcDimensionHeightInput = currentH.toString(),
            selectedCropRatio = preset.toCropAspectRatio()
        )
    }

    fun setCustomAspect(x: Float, y: Float, xInput: String = x.toString(), yInput: String = y.toString()) {
        val safeX = if (x > 0f) x else 1f
        val safeY = if (y > 0f) y else 1f
        val currentW = _uiState.value.calcDimensionWidth
        val currentH = if (_uiState.value.aspectRatioLocked) {
            AspectRatioEngine.calculateHeightFromWidth(currentW, safeX, safeY)
        } else {
            _uiState.value.calcDimensionHeight
        }
        _uiState.value = _uiState.value.copy(
            isCustomAspectSelected = true,
            customAspectX = safeX,
            customAspectY = safeY,
            customAspectXInput = xInput,
            customAspectYInput = yInput,
            calcDimensionHeight = currentH,
            calcDimensionHeightInput = currentH.toString(),
            selectedCropRatio = CropAspectRatio("Custom $xInput:$yInput", safeX, safeY)
        )
    }

    fun swapAspectOrientation() {
        val state = _uiState.value
        val (newX, newY, newXStr, newYStr) = if (state.isCustomAspectSelected) {
            listOf(state.customAspectY, state.customAspectX, state.customAspectYInput, state.customAspectXInput)
        } else {
            listOf(state.activeAspectPreset.ratioY, state.activeAspectPreset.ratioX, state.activeAspectPreset.ratioY.toInt().toString(), state.activeAspectPreset.ratioX.toInt().toString())
        }
        val xVal = newX as Float
        val yVal = newY as Float
        val currentW = state.calcDimensionHeight
        val currentH = state.calcDimensionWidth
        _uiState.value = state.copy(
            isCustomAspectSelected = true,
            customAspectX = xVal,
            customAspectY = yVal,
            customAspectXInput = newXStr as String,
            customAspectYInput = newYStr as String,
            calcDimensionWidth = currentW,
            calcDimensionHeight = currentH,
            calcDimensionWidthInput = currentW.toString(),
            calcDimensionHeightInput = currentH.toString(),
            selectedCropRatio = CropAspectRatio("Flipped $newXStr:$newYStr", xVal, yVal)
        )
    }

    fun setCalcWidth(width: Int, input: String = width.toString()) {
        val state = _uiState.value
        val safeW = width.coerceAtLeast(1)
        val (ratioX, ratioY) = if (state.isCustomAspectSelected) {
            Pair(state.customAspectX, state.customAspectY)
        } else {
            Pair(state.activeAspectPreset.ratioX, state.activeAspectPreset.ratioY)
        }

        val finalH = if (state.aspectRatioLocked && ratioX > 0f) {
            AspectRatioEngine.calculateHeightFromWidth(safeW, ratioX, ratioY)
        } else {
            state.calcDimensionHeight
        }

        _uiState.value = state.copy(
            calcDimensionWidth = safeW,
            calcDimensionWidthInput = input,
            calcDimensionHeight = finalH,
            calcDimensionHeightInput = finalH.toString()
        )
    }

    fun setCalcHeight(height: Int, input: String = height.toString()) {
        val state = _uiState.value
        val safeH = height.coerceAtLeast(1)
        val (ratioX, ratioY) = if (state.isCustomAspectSelected) {
            Pair(state.customAspectX, state.customAspectY)
        } else {
            Pair(state.activeAspectPreset.ratioX, state.activeAspectPreset.ratioY)
        }

        val finalW = if (state.aspectRatioLocked && ratioY > 0f) {
            AspectRatioEngine.calculateWidthFromHeight(safeH, ratioX, ratioY)
        } else {
            state.calcDimensionWidth
        }

        _uiState.value = state.copy(
            calcDimensionHeight = safeH,
            calcDimensionHeightInput = input,
            calcDimensionWidth = finalW,
            calcDimensionWidthInput = finalW.toString()
        )
    }

    fun applyCalcScale(multiplier: Float) {
        val state = _uiState.value
        val newW = (state.calcDimensionWidth * multiplier).roundToInt().coerceAtLeast(1)
        val newH = (state.calcDimensionHeight * multiplier).roundToInt().coerceAtLeast(1)
        _uiState.value = state.copy(
            calcDimensionWidth = newW,
            calcDimensionWidthInput = newW.toString(),
            calcDimensionHeight = newH,
            calcDimensionHeightInput = newH.toString()
        )
    }

    fun setCalcStandardResolution(res: StandardResolution) {
        _uiState.value = _uiState.value.copy(
            calcDimensionWidth = res.width,
            calcDimensionWidthInput = res.width.toString(),
            calcDimensionHeight = res.height,
            calcDimensionHeightInput = res.height.toString()
        )
    }

    fun setAspectApplyMode(mode: AspectRatioApplyMode) {
        _uiState.value = _uiState.value.copy(aspectApplyMode = mode)
    }

    fun setAspectPadColor(color: Int) {
        _uiState.value = _uiState.value.copy(aspectPadColor = color)
    }

    fun setAspectCategoryFilter(category: String) {
        _uiState.value = _uiState.value.copy(aspectCategoryFilter = category)
    }

    fun setRatioCalcWInput(input: String) {
        _uiState.value = _uiState.value.copy(ratioCalcWInput = input)
    }

    fun setRatioCalcHInput(input: String) {
        _uiState.value = _uiState.value.copy(ratioCalcHInput = input)
    }

    fun applyAspectRatioTransformation(onResult: (Boolean, String) -> Unit) {
        val state = _uiState.value
        val bitmap = state.workingBitmap ?: run {
            onResult(false, "No image loaded")
            return
        }

        val (ratioX, ratioY) = if (state.isCustomAspectSelected) {
            Pair(state.customAspectX, state.customAspectY)
        } else {
            Pair(state.activeAspectPreset.ratioX, state.activeAspectPreset.ratioY)
        }

        viewModelScope.launch {
            recordHistoryState("Aspect Ratio")
            _uiState.value = state.copy(isLoading = true)
            try {
                if (state.aspectApplyMode == AspectRatioApplyMode.INTERACTIVE_CROP) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        activeTab = StudioTab.CROP,
                        selectedCropRatio = if (state.isCustomAspectSelected) {
                            CropAspectRatio("Custom", ratioX, ratioY)
                        } else {
                            state.activeAspectPreset.toCropAspectRatio()
                        }
                    )
                    onResult(true, "Switched to Interactive Crop with target ratio locked")
                    return@launch
                }

                val transformed = withContext(Dispatchers.Default) {
                    when (state.aspectApplyMode) {
                        AspectRatioApplyMode.CROP -> {
                            AspectRatioEngine.cropToAspectRatio(bitmap, ratioX, ratioY)
                        }
                        AspectRatioApplyMode.PAD_LETTERBOX -> {
                            AspectRatioEngine.padToAspectRatio(bitmap, ratioX, ratioY, state.aspectPadColor)
                        }
                        AspectRatioApplyMode.RESIZE_SCALE -> {
                            AspectRatioEngine.scaleToDimensions(bitmap, state.calcDimensionWidth, state.calcDimensionHeight)
                        }
                        AspectRatioApplyMode.INTERACTIVE_CROP -> bitmap
                    }
                }

                _uiState.value = _uiState.value.copy(
                    workingBitmap = transformed,
                    previewBitmap = transformed,
                    targetWidthPx = transformed.width,
                    targetHeightPx = transformed.height,
                    calcDimensionWidth = transformed.width,
                    calcDimensionWidthInput = transformed.width.toString(),
                    calcDimensionHeight = transformed.height,
                    calcDimensionHeightInput = transformed.height.toString(),
                    ratioCalcWInput = transformed.width.toString(),
                    ratioCalcHInput = transformed.height.toString(),
                    isLoading = false,
                    statusMessage = "Applied ${state.aspectApplyMode.label} (${transformed.width}×${transformed.height} px)"
                )
                calculateEstimatedSize()
                onResult(true, "Applied ${state.aspectApplyMode.label} (${transformed.width}×${transformed.height} px)")
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false)
                onResult(false, e.localizedMessage ?: "Failed to apply transformation")
            }
        }
    }

    fun setTargetDimensions(width: Int, height: Int, keepRatio: Boolean = true) {
        val current = _uiState.value.workingBitmap
        val finalW = width.coerceAtLeast(1)
        val finalH = if (keepRatio && current != null && current.width > 0 && current.height > 0) {
            val aspect = current.width.toFloat() / current.height.toFloat()
            (width / aspect).roundToInt().coerceAtLeast(1)
        } else {
            height.coerceAtLeast(1)
        }

        val curDpi = _uiState.value.dpi.coerceAtLeast(1)
        val wInches = finalW.toFloat() / curDpi
        val hInches = finalH.toFloat() / curDpi
        val unit = _uiState.value.printUnit
        val physW = when (unit) {
            PrintUnit.INCHES -> wInches
            PrintUnit.CENTIMETERS -> wInches * 2.54f
            PrintUnit.MILLIMETERS -> wInches * 25.4f
        }
        val physH = when (unit) {
            PrintUnit.INCHES -> hInches
            PrintUnit.CENTIMETERS -> hInches * 2.54f
            PrintUnit.MILLIMETERS -> hInches * 25.4f
        }

        _uiState.value = _uiState.value.copy(
            targetWidthPx = finalW,
            targetHeightPx = finalH,
            keepAspectRatio = keepRatio,
            printWidthPhysical = physW,
            printHeightPhysical = physH
        )
        calculateEstimatedSize()
    }

    fun applyStandardPrintPreset(preset: StandardPrintPreset) {
        if (preset == StandardPrintPreset.CUSTOM) {
            _uiState.value = _uiState.value.copy(selectedPrintPreset = StandardPrintPreset.CUSTOM)
            return
        }
        val targetUnit = preset.defaultUnit
        val w = preset.getWidthInUnit(targetUnit)
        val h = preset.getHeightInUnit(targetUnit)
        val safeDpi = _uiState.value.dpi.coerceAtLeast(1)
        val widthInches = preset.widthInches
        val heightInches = preset.heightInches
        val calculatedW = (widthInches * safeDpi).roundToInt().coerceAtLeast(1)
        val calculatedH = (heightInches * safeDpi).roundToInt().coerceAtLeast(1)

        _uiState.value = _uiState.value.copy(
            selectedPrintPreset = preset,
            printUnit = targetUnit,
            printWidthPhysical = w,
            printHeightPhysical = h,
            targetWidthPx = calculatedW,
            targetHeightPx = calculatedH,
            statusMessage = "Selected preset: ${preset.displayName} (${preset.description})"
        )
        calculateEstimatedSize()
    }

    fun setPrintDimensions(
        width: Float,
        height: Float,
        unit: PrintUnit = _uiState.value.printUnit,
        dpi: Int = _uiState.value.dpi,
        keepRatio: Boolean = _uiState.value.keepAspectRatio
    ) {
        val safeDpi = dpi.coerceAtLeast(1)
        val widthInches = when (unit) {
            PrintUnit.INCHES -> width
            PrintUnit.CENTIMETERS -> width / 2.54f
            PrintUnit.MILLIMETERS -> width / 25.4f
        }
        val heightInches = when (unit) {
            PrintUnit.INCHES -> height
            PrintUnit.CENTIMETERS -> height / 2.54f
            PrintUnit.MILLIMETERS -> height / 25.4f
        }
        val calculatedW = (widthInches * safeDpi).roundToInt().coerceAtLeast(1)
        val calculatedH = (heightInches * safeDpi).roundToInt().coerceAtLeast(1)

        // Check if matches any preset
        val matchedPreset = StandardPrintPreset.values().firstOrNull { preset ->
            preset != StandardPrintPreset.CUSTOM &&
                    kotlin.math.abs(preset.widthInches - widthInches) < 0.05f &&
                    kotlin.math.abs(preset.heightInches - heightInches) < 0.05f
        } ?: StandardPrintPreset.CUSTOM

        _uiState.value = _uiState.value.copy(
            selectedPrintPreset = matchedPreset,
            printUnit = unit,
            printWidthPhysical = width,
            printHeightPhysical = height,
            dpi = safeDpi,
            targetWidthPx = calculatedW,
            targetHeightPx = calculatedH,
            keepAspectRatio = keepRatio
        )
        calculateEstimatedSize()
    }

    fun setDpiForPrint(newDpi: Int) {
        val safeDpi = newDpi.coerceAtLeast(1)
        setPrintDimensions(
            width = _uiState.value.printWidthPhysical,
            height = _uiState.value.printHeightPhysical,
            unit = _uiState.value.printUnit,
            dpi = safeDpi,
            keepRatio = _uiState.value.keepAspectRatio
        )
    }

    fun applyPercentageScale(percentage: Int) {
        val current = _uiState.value.workingBitmap ?: return
        val factor = percentage / 100f
        val newW = (current.width * factor).roundToInt().coerceAtLeast(1)
        val newH = (current.height * factor).roundToInt().coerceAtLeast(1)
        setTargetDimensions(newW, newH, false)
    }

    fun setResizeMode(mode: ResizeMode) {
        _uiState.value = _uiState.value.copy(resizeMode = mode)
        updateResizePreview()
    }

    fun setFitBackgroundColor(color: Int) {
        _uiState.value = _uiState.value.copy(fitBackgroundColor = color)
        updateResizePreview()
    }

    private fun updateResizePreview() {
        val current = _uiState.value.workingBitmap ?: return
        viewModelScope.launch {
            val preview = withContext(Dispatchers.Default) {
                BitmapUtils.resizeBitmapWithMode(
                    current,
                    _uiState.value.targetWidthPx,
                    _uiState.value.targetHeightPx,
                    _uiState.value.resizeMode,
                    _uiState.value.fitBackgroundColor
                )
            }
            _uiState.value = _uiState.value.copy(previewBitmap = preview)
        }
    }

    fun applyResize() {
        val current = _uiState.value.workingBitmap ?: return
        recordHistoryState("Resize (${_uiState.value.resizeMode.label})")
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            val result = withContext(Dispatchers.Default) {
                ResizeEngine.resize(
                    current,
                    _uiState.value.targetWidthPx,
                    _uiState.value.targetHeightPx,
                    _uiState.value.resizeMode,
                    _uiState.value.fitBackgroundColor
                )
            }
            val resized = result.bitmap
            _uiState.value = _uiState.value.copy(
                workingBitmap = resized,
                previewBitmap = resized,
                lastResizeReport = result.verificationReport,
                isLoading = false,
                statusMessage = "Resized (${_uiState.value.resizeMode.label}) to ${resized.width}x${resized.height} px"
            )
            calculateEstimatedSize()
        }
    }

    fun setPrintCalculationMode(mode: DpiPrintEngine.PrintCalculationMode) {
        val current = _uiState.value.workingBitmap
        val report = dpiPrintUseCase.calculatePrintMetrics(
            sourceWidthPx = current?.width ?: _uiState.value.targetWidthPx,
            sourceHeightPx = current?.height ?: _uiState.value.targetHeightPx,
            physicalWidth = _uiState.value.printWidthPhysical,
            physicalHeight = _uiState.value.printHeightPhysical,
            unit = _uiState.value.printUnit,
            targetDpi = _uiState.value.dpi,
            bleedMm = _uiState.value.printBleedMm,
            mode = mode
        )
        _uiState.value = _uiState.value.copy(
            printCalculationMode = mode,
            lastPrintReport = report
        )
    }

    fun setPrintBleedMm(bleedMm: Float) {
        val clamped = bleedMm.coerceIn(0f, 15f)
        val current = _uiState.value.workingBitmap
        val report = dpiPrintUseCase.calculatePrintMetrics(
            sourceWidthPx = current?.width ?: _uiState.value.targetWidthPx,
            sourceHeightPx = current?.height ?: _uiState.value.targetHeightPx,
            physicalWidth = _uiState.value.printWidthPhysical,
            physicalHeight = _uiState.value.printHeightPhysical,
            unit = _uiState.value.printUnit,
            targetDpi = _uiState.value.dpi,
            bleedMm = clamped,
            mode = _uiState.value.printCalculationMode
        )
        _uiState.value = _uiState.value.copy(
            printBleedMm = clamped,
            lastPrintReport = report
        )
    }

    fun applyPrintResize() {
        val current = _uiState.value.workingBitmap ?: return
        recordHistoryState("Print & DPI (${_uiState.value.dpi} DPI)")
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            val resampleResult = withContext(Dispatchers.Default) {
                dpiPrintUseCase.resampleForPrint(
                    source = current,
                    physicalWidth = _uiState.value.printWidthPhysical,
                    physicalHeight = _uiState.value.printHeightPhysical,
                    unit = _uiState.value.printUnit,
                    dpi = _uiState.value.dpi,
                    bleedMm = _uiState.value.printBleedMm,
                    resizeMode = _uiState.value.resizeMode,
                    backgroundColor = if (_uiState.value.fitBackgroundColor == Color.TRANSPARENT) Color.WHITE else _uiState.value.fitBackgroundColor
                )
            }
            val resized = resampleResult.bitmap
            val unitStr = _uiState.value.printUnit.symbol
            val wStr = String.format("%.2f", _uiState.value.printWidthPhysical)
            val hStr = String.format("%.2f", _uiState.value.printHeightPhysical)
            val bleedNote = if (_uiState.value.printBleedMm > 0f) " (+${_uiState.value.printBleedMm.toInt()}mm bleed)" else ""
            _uiState.value = _uiState.value.copy(
                workingBitmap = resized,
                previewBitmap = resized,
                targetWidthPx = resized.width,
                targetHeightPx = resized.height,
                lastPrintReport = resampleResult.report,
                isLoading = false,
                statusMessage = "Print size applied: $wStr × $hStr $unitStr$bleedNote @ ${_uiState.value.dpi} DPI (${resized.width}×${resized.height} px)"
            )
            calculateEstimatedSize()
        }
    }

    fun setCompressionPreset(preset: CompressionPreset) {
        if (preset == CompressionPreset.CUSTOM) {
            _uiState.value = _uiState.value.copy(compressionPreset = CompressionPreset.CUSTOM)
        } else {
            val q = preset.defaultQuality
            val compPercent = 100 - q
            _uiState.value = _uiState.value.copy(
                compressionPreset = preset,
                quality = q,
                compressionPercentage = compPercent,
                targetSizeKb = null
            )
        }
        calculateEstimatedSize()
    }

    fun setQuality(quality: Int) {
        val clampedQ = quality.coerceIn(1, 100)
        val matchedPreset = CompressionPreset.values().firstOrNull { it != CompressionPreset.CUSTOM && it.defaultQuality == clampedQ } ?: CompressionPreset.CUSTOM
        _uiState.value = _uiState.value.copy(
            quality = clampedQ,
            compressionPercentage = 100 - clampedQ,
            compressionPreset = matchedPreset,
            targetSizeKb = null
        )
        calculateEstimatedSize()
    }

    fun setCompressionPercentage(percent: Int) {
        val clampedComp = percent.coerceIn(0, 99)
        val q = (100 - clampedComp).coerceIn(1, 100)
        val matchedPreset = CompressionPreset.values().firstOrNull { it != CompressionPreset.CUSTOM && it.defaultQuality == q } ?: CompressionPreset.CUSTOM
        _uiState.value = _uiState.value.copy(
            compressionPercentage = clampedComp,
            quality = q,
            compressionPreset = matchedPreset,
            targetSizeKb = null
        )
        calculateEstimatedSize()
    }

    fun setCustomCompressionMode(mode: CustomCompressionMode) {
        _uiState.value = _uiState.value.copy(
            customCompressionMode = mode,
            compressionPreset = CompressionPreset.CUSTOM
        )
        when (mode) {
            CustomCompressionMode.MAX_CEILING -> {
                _uiState.value = _uiState.value.copy(fileSizeMode = FileSizeMode.MAXIMUM_CEILING)
            }
            CustomCompressionMode.TARGET_SIZE -> {
                _uiState.value = _uiState.value.copy(fileSizeMode = FileSizeMode.TARGET_CLOSEST)
            }
            CustomCompressionMode.QUALITY_PERCENT, CustomCompressionMode.COMPRESSION_PERCENT -> {
                _uiState.value = _uiState.value.copy(targetSizeKb = null)
            }
        }
        calculateEstimatedSize()
    }

    fun setTargetFileSizeKb(targetKb: Int?) {
        _uiState.value = _uiState.value.copy(
            targetSizeKb = targetKb,
            compressionPreset = CompressionPreset.CUSTOM
        )
        calculateEstimatedSize()
    }

    fun setFileSizeMode(mode: FileSizeMode) {
        _uiState.value = _uiState.value.copy(
            fileSizeMode = mode,
            compressionPreset = CompressionPreset.CUSTOM
        )
        calculateEstimatedSize()
    }

    fun setAllowDimensionDownscaling(allow: Boolean) {
        _uiState.value = _uiState.value.copy(allowDimensionDownscaling = allow)
        calculateEstimatedSize()
    }

    fun setFormat(format: ExportFormat) {
        val current = _uiState.value.workingBitmap
        val report = if (current != null) {
            val res = convertFormatUseCase.convert(
                source = current,
                fromFormat = _uiState.value.detectedInputFormat,
                toFormat = format,
                quality = _uiState.value.quality,
                backgroundColor = _uiState.value.jpegBackgroundColor
            )
            res.verificationReport
        } else null

        _uiState.value = _uiState.value.copy(
            exportFormat = format,
            lastConversionReport = report
        )
        calculateEstimatedSize()
    }

    fun setJpegBackgroundColor(color: Int, name: String) {
        _uiState.value = _uiState.value.copy(
            jpegBackgroundColor = color,
            jpegBackgroundName = name
        )
        calculateEstimatedSize()
    }

    fun applyConversionPair(pair: FormatConversionPair) {
        val current = _uiState.value.workingBitmap
        val report = if (current != null) {
            val res = convertFormatUseCase.convertPair(
                source = current,
                pair = pair,
                quality = _uiState.value.quality,
                backgroundColor = _uiState.value.jpegBackgroundColor
            )
            res.verificationReport
        } else null

        _uiState.value = _uiState.value.copy(
            exportFormat = pair.toFormat,
            lastConversionReport = report,
            statusMessage = "Converted: ${pair.label} (${pair.description})"
        )
        calculateEstimatedSize()
    }

    fun applyFormatConversion(toFormat: ExportFormat, quality: Int = _uiState.value.quality) {
        val current = _uiState.value.workingBitmap ?: return
        val res = convertFormatUseCase.convert(
            source = current,
            fromFormat = _uiState.value.detectedInputFormat,
            toFormat = toFormat,
            quality = quality,
            backgroundColor = _uiState.value.jpegBackgroundColor
        )
        _uiState.value = _uiState.value.copy(
            exportFormat = toFormat,
            quality = quality,
            lastConversionReport = res.verificationReport,
            statusMessage = "Format converted to ${toFormat.displayName} (MIME: ${res.detectedMimeType})"
        )
        calculateEstimatedSize()
    }

    fun setDpi(dpi: Int) {
        val safeDpi = dpi.coerceIn(10, 2400)
        _uiState.value = _uiState.value.copy(
            dpi = safeDpi,
            customDpiInput = safeDpi.toString()
        )
    }

    fun setCustomDpiText(text: String) {
        val filtered = text.filter { it.isDigit() }
        val parsed = filtered.toIntOrNull()
        _uiState.value = _uiState.value.copy(
            customDpiInput = filtered,
            dpi = if (parsed != null && parsed in 10..2400) parsed else _uiState.value.dpi
        )
    }

    fun setEnhancements(
        brightness: Float = _uiState.value.brightness,
        contrast: Float = _uiState.value.contrast,
        saturation: Float = _uiState.value.saturation,
        exposure: Float = _uiState.value.exposure,
        highlights: Float = _uiState.value.highlights,
        shadows: Float = _uiState.value.shadows,
        sharpness: Float = _uiState.value.sharpness,
        warmth: Float = _uiState.value.warmth,
        vignette: Float = _uiState.value.vignette,
        vibrance: Float = _uiState.value.vibrance,
        gamma: Float = _uiState.value.gamma,
        claheStrength: Float = _uiState.value.claheStrength,
        isGrayscale: Boolean = _uiState.value.isGrayscale,
        isBlackAndWhite: Boolean = _uiState.value.isBlackAndWhite,
        bwThreshold: Float = _uiState.value.bwThreshold,
        filter: FilterPreset = _uiState.value.activeFilter,
        recordHistory: Boolean = true
    ) {
        val currentParams = getCurrentEnhancementParams()
        val newParams = ImageEnhancer.EnhancementParameters(
            brightness = brightness,
            contrast = contrast,
            saturation = saturation,
            exposure = exposure,
            highlights = highlights,
            shadows = shadows,
            sharpness = sharpness,
            warmth = warmth,
            vignette = vignette,
            vibrance = vibrance,
            gamma = gamma,
            claheStrength = claheStrength,
            isGrayscale = isGrayscale,
            isBlackAndWhite = isBlackAndWhite,
            bwThreshold = bwThreshold,
            filter = filter
        )

        val newUndoStack = if (recordHistory && currentParams != newParams) {
            (_uiState.value.enhanceUndoStack + currentParams).takeLast(25)
        } else {
            _uiState.value.enhanceUndoStack
        }

        val newRedoStack = if (recordHistory && currentParams != newParams) {
            emptyList()
        } else {
            _uiState.value.enhanceRedoStack
        }

        _uiState.value = _uiState.value.copy(
            brightness = brightness,
            contrast = contrast,
            saturation = saturation,
            exposure = exposure,
            highlights = highlights,
            shadows = shadows,
            sharpness = sharpness,
            warmth = warmth,
            vignette = vignette,
            vibrance = vibrance,
            gamma = gamma,
            claheStrength = claheStrength,
            isGrayscale = isGrayscale,
            isBlackAndWhite = isBlackAndWhite,
            bwThreshold = bwThreshold,
            activeFilter = filter,
            enhanceUndoStack = newUndoStack,
            enhanceRedoStack = newRedoStack
        )
        updatePreview()
    }

    fun applyEnhancementParams(params: ImageEnhancer.EnhancementParameters, recordHistory: Boolean = true) {
        setEnhancements(
            brightness = params.brightness,
            contrast = params.contrast,
            saturation = params.saturation,
            exposure = params.exposure,
            highlights = params.highlights,
            shadows = params.shadows,
            sharpness = params.sharpness,
            warmth = params.warmth,
            vignette = params.vignette,
            vibrance = params.vibrance,
            gamma = params.gamma,
            claheStrength = params.claheStrength,
            isGrayscale = params.isGrayscale,
            isBlackAndWhite = params.isBlackAndWhite,
            bwThreshold = params.bwThreshold,
            filter = params.filter,
            recordHistory = recordHistory
        )
    }

    private fun getCurrentEnhancementParams(): ImageEnhancer.EnhancementParameters {
        val s = _uiState.value
        return ImageEnhancer.EnhancementParameters(
            brightness = s.brightness,
            contrast = s.contrast,
            saturation = s.saturation,
            exposure = s.exposure,
            highlights = s.highlights,
            shadows = s.shadows,
            sharpness = s.sharpness,
            warmth = s.warmth,
            vignette = s.vignette,
            vibrance = s.vibrance,
            gamma = s.gamma,
            claheStrength = s.claheStrength,
            isGrayscale = s.isGrayscale,
            isBlackAndWhite = s.isBlackAndWhite,
            bwThreshold = s.bwThreshold,
            filter = s.activeFilter
        )
    }

    fun autoEnhance() {
        val current = _uiState.value.workingBitmap ?: return
        viewModelScope.launch {
            val autoParams = withContext(Dispatchers.Default) {
                ImageEnhancer.computeAutoEnhancements(current)
            }
            applyEnhancementParams(autoParams, recordHistory = true)
            _uiState.value = _uiState.value.copy(statusMessage = "Auto enhancement applied")
        }
    }

    fun toggleGrayscale() {
        val next = !_uiState.value.isGrayscale
        setEnhancements(
            isGrayscale = next,
            isBlackAndWhite = if (next) false else _uiState.value.isBlackAndWhite
        )
    }

    fun toggleBlackAndWhite() {
        val next = !_uiState.value.isBlackAndWhite
        setEnhancements(
            isBlackAndWhite = next,
            isGrayscale = if (next) false else _uiState.value.isGrayscale
        )
    }

    fun setBwThreshold(thresh: Float) {
        setEnhancements(bwThreshold = thresh, isBlackAndWhite = true)
    }

    fun undoEnhancement() {
        val undoStack = _uiState.value.enhanceUndoStack
        if (undoStack.isEmpty()) return

        val currentParams = getCurrentEnhancementParams()
        val prevParams = undoStack.last()
        val updatedUndo = undoStack.dropLast(1)
        val updatedRedo = (_uiState.value.enhanceRedoStack + currentParams).takeLast(25)

        _uiState.value = _uiState.value.copy(
            brightness = prevParams.brightness,
            contrast = prevParams.contrast,
            saturation = prevParams.saturation,
            exposure = prevParams.exposure,
            highlights = prevParams.highlights,
            shadows = prevParams.shadows,
            sharpness = prevParams.sharpness,
            warmth = prevParams.warmth,
            vignette = prevParams.vignette,
            vibrance = prevParams.vibrance,
            gamma = prevParams.gamma,
            claheStrength = prevParams.claheStrength,
            isGrayscale = prevParams.isGrayscale,
            isBlackAndWhite = prevParams.isBlackAndWhite,
            bwThreshold = prevParams.bwThreshold,
            activeFilter = prevParams.filter,
            enhanceUndoStack = updatedUndo,
            enhanceRedoStack = updatedRedo
        )
        updatePreview()
    }

    fun redoEnhancement() {
        val redoStack = _uiState.value.enhanceRedoStack
        if (redoStack.isEmpty()) return

        val currentParams = getCurrentEnhancementParams()
        val nextParams = redoStack.last()
        val updatedRedo = redoStack.dropLast(1)
        val updatedUndo = (_uiState.value.enhanceUndoStack + currentParams).takeLast(25)

        _uiState.value = _uiState.value.copy(
            brightness = nextParams.brightness,
            contrast = nextParams.contrast,
            saturation = nextParams.saturation,
            exposure = nextParams.exposure,
            highlights = nextParams.highlights,
            shadows = nextParams.shadows,
            sharpness = nextParams.sharpness,
            warmth = nextParams.warmth,
            vignette = nextParams.vignette,
            vibrance = nextParams.vibrance,
            gamma = nextParams.gamma,
            claheStrength = nextParams.claheStrength,
            isGrayscale = nextParams.isGrayscale,
            isBlackAndWhite = nextParams.isBlackAndWhite,
            bwThreshold = nextParams.bwThreshold,
            activeFilter = nextParams.filter,
            enhanceUndoStack = updatedUndo,
            enhanceRedoStack = updatedRedo
        )
        updatePreview()
    }

    fun resetEnhancements() {
        val currentParams = getCurrentEnhancementParams()
        val newUndo = if (!currentParams.isNeutral) {
            (_uiState.value.enhanceUndoStack + currentParams).takeLast(25)
        } else {
            _uiState.value.enhanceUndoStack
        }

        _uiState.value = _uiState.value.copy(
            brightness = 0f,
            contrast = 0f,
            saturation = 0f,
            exposure = 0f,
            highlights = 0f,
            shadows = 0f,
            sharpness = 0f,
            warmth = 0f,
            vignette = 0f,
            vibrance = 0f,
            gamma = 1.0f,
            claheStrength = 0f,
            isGrayscale = false,
            isBlackAndWhite = false,
            bwThreshold = 128f,
            activeFilter = FilterPreset.ORIGINAL,
            enhanceUndoStack = newUndo,
            enhanceRedoStack = emptyList(),
            isHoldingCompareOriginal = false
        )
        updatePreview()
    }

    fun toggleBeforeAfterSplit() {
        _uiState.value = _uiState.value.copy(showBeforeAfterSplit = !_uiState.value.showBeforeAfterSplit)
    }

    fun setBeforeAfterSplitPosition(position: Float) {
        _uiState.value = _uiState.value.copy(beforeAfterSplitPosition = position.coerceIn(0.05f, 0.95f))
    }

    fun setHoldingCompareOriginal(isHolding: Boolean) {
        _uiState.value = _uiState.value.copy(isHoldingCompareOriginal = isHolding)
    }

    fun toggleEnhanceHistogram() {
        _uiState.value = _uiState.value.copy(showEnhanceHistogram = !_uiState.value.showEnhanceHistogram)
    }

    fun applyEnhancementsPermanently() {
        val current = _uiState.value.workingBitmap ?: return
        recordHistoryState("Enhance")
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            val params = getCurrentEnhancementParams()
            val res = withContext(Dispatchers.Default) {
                advancedEditingUseCase.executeEnhancement(
                    source = current,
                    params = params
                )
            }
            _uiState.value = _uiState.value.copy(
                workingBitmap = res.bitmap,
                previewBitmap = res.bitmap,
                lastEnhancementReport = res.verification,
                histogramData = res.histogram,
                isLoading = false,
                statusMessage = "Enhancements applied (${res.verification.summary})"
            )
            resetEnhancements()
        }
    }

    private fun updatePreview() {
        val current = _uiState.value.workingBitmap ?: return
        viewModelScope.launch {
            val params = getCurrentEnhancementParams()
            val res = withContext(Dispatchers.Default) {
                advancedEditingUseCase.executeEnhancement(
                    source = current,
                    params = params
                )
            }
            _uiState.value = _uiState.value.copy(
                previewBitmap = res.bitmap,
                histogramData = res.histogram,
                lastEnhancementReport = res.verification
            )
        }
    }

    fun rotate(degrees: Int) {
        val current = _uiState.value.workingBitmap ?: return
        viewModelScope.launch {
            val rotated = withContext(Dispatchers.Default) {
                BitmapUtils.rotateBitmap(current, degrees)
            }
            _uiState.value = _uiState.value.copy(
                workingBitmap = rotated,
                previewBitmap = rotated,
                targetWidthPx = rotated.width,
                targetHeightPx = rotated.height
            )
            calculateEstimatedSize()
        }
    }

    fun rotate90Cw() {
        val current = _uiState.value.workingBitmap ?: return
        recordHistoryState("Rotate 90° CW")
        viewModelScope.launch {
            val rotated = withContext(Dispatchers.Default) {
                RotateStraightenEngine.rotate90Cw(current)
            }
            _uiState.value = _uiState.value.copy(
                workingBitmap = rotated,
                previewBitmap = rotated,
                targetWidthPx = rotated.width,
                targetHeightPx = rotated.height,
                lastOrientationMessage = "Rotated 90° Clockwise"
            )
            calculateEstimatedSize()
        }
    }

    fun rotate90Ccw() {
        val current = _uiState.value.workingBitmap ?: return
        recordHistoryState("Rotate 90° CCW")
        viewModelScope.launch {
            val rotated = withContext(Dispatchers.Default) {
                RotateStraightenEngine.rotate90Ccw(current)
            }
            _uiState.value = _uiState.value.copy(
                workingBitmap = rotated,
                previewBitmap = rotated,
                targetWidthPx = rotated.width,
                targetHeightPx = rotated.height,
                lastOrientationMessage = "Rotated 90° Counter-Clockwise"
            )
            calculateEstimatedSize()
        }
    }

    fun rotate180() {
        val current = _uiState.value.workingBitmap ?: return
        recordHistoryState("Rotate 180°")
        viewModelScope.launch {
            val rotated = withContext(Dispatchers.Default) {
                RotateStraightenEngine.rotate180(current)
            }
            _uiState.value = _uiState.value.copy(
                workingBitmap = rotated,
                previewBitmap = rotated,
                targetWidthPx = rotated.width,
                targetHeightPx = rotated.height,
                lastOrientationMessage = "Inverted 180°"
            )
            calculateEstimatedSize()
        }
    }

    fun flip(horizontal: Boolean, vertical: Boolean) {
        val current = _uiState.value.workingBitmap ?: return
        recordHistoryState("Flip")
        viewModelScope.launch {
            val flipped = withContext(Dispatchers.Default) {
                BitmapUtils.flipBitmap(current, horizontal, vertical)
            }
            val newH = if (horizontal) !_uiState.value.isFlippedHorizontally else _uiState.value.isFlippedHorizontally
            val newV = if (vertical) !_uiState.value.isFlippedVertically else _uiState.value.isFlippedVertically
            val newHCount = if (horizontal) _uiState.value.flipHorizontalCount + 1 else _uiState.value.flipHorizontalCount
            val newVCount = if (vertical) _uiState.value.flipVerticalCount + 1 else _uiState.value.flipVerticalCount
            _uiState.value = _uiState.value.copy(
                workingBitmap = flipped,
                previewBitmap = flipped,
                isFlippedHorizontally = newH,
                isFlippedVertically = newV,
                flipHorizontalCount = newHCount,
                flipVerticalCount = newVCount,
                lastOrientationMessage = when {
                    horizontal && vertical -> "Flipped Both Axes"
                    horizontal -> "Flipped Horizontally (Left ↔ Right)"
                    vertical -> "Flipped Vertically (Top ↕ Bottom)"
                    else -> "No change"
                }
            )
            calculateEstimatedSize()
        }
    }

    fun flipHorizontal() {
        flip(horizontal = true, vertical = false)
    }

    fun flipVertical() {
        flip(horizontal = false, vertical = true)
    }

    fun flipBoth() {
        flip(horizontal = true, vertical = true)
    }

    fun setFlipSymmetryMode(mode: RotateStraightenEngine.FlipSymmetryMode) {
        val original = _uiState.value.originalBitmap ?: _uiState.value.workingBitmap ?: return
        viewModelScope.launch {
            val mirrored = withContext(Dispatchers.Default) {
                RotateStraightenEngine.applySymmetryMirror(original, mode)
            }
            _uiState.value = _uiState.value.copy(
                workingBitmap = mirrored,
                previewBitmap = mirrored,
                flipSymmetryMode = mode,
                lastOrientationMessage = "Applied: ${mode.label}"
            )
            calculateEstimatedSize()
        }
    }

    fun toggleFlipAxisGuide(show: Boolean) {
        _uiState.value = _uiState.value.copy(showFlipAxisGuide = show)
    }

    fun resetFlipState() {
        val original = _uiState.value.originalBitmap ?: return
        _uiState.value = _uiState.value.copy(
            workingBitmap = original,
            previewBitmap = original,
            isFlippedHorizontally = false,
            isFlippedVertically = false,
            flipHorizontalCount = 0,
            flipVerticalCount = 0,
            flipSymmetryMode = RotateStraightenEngine.FlipSymmetryMode.NONE,
            lastOrientationMessage = "Reset to original orientation"
        )
        calculateEstimatedSize()
    }

    // ================= PERSPECTIVE CORRECTION (Request 25) =================

    fun updatePerspectiveCorner(corner: PerspectiveEngine.PerspectiveCorner, x: Float, y: Float) {
        val newQuad = _uiState.value.perspectiveQuad.withCorner(
            corner,
            PerspectiveEngine.PointFNormalized(x.coerceIn(0f, 1f), y.coerceIn(0f, 1f))
        )
        _uiState.value = _uiState.value.copy(
            perspectiveQuad = newQuad,
            activePerspectiveCorner = corner
        )
        if (_uiState.value.isPerspectivePreviewActive) {
            updatePerspectivePreview()
        }
    }

    fun setActivePerspectiveCorner(corner: PerspectiveEngine.PerspectiveCorner) {
        _uiState.value = _uiState.value.copy(activePerspectiveCorner = corner)
    }

    fun setPerspectiveQuad(quad: PerspectiveEngine.PerspectiveQuad) {
        _uiState.value = _uiState.value.copy(perspectiveQuad = quad)
        if (_uiState.value.isPerspectivePreviewActive) {
            updatePerspectivePreview()
        }
    }

    fun setPerspectiveOutputPreset(preset: PerspectiveEngine.PerspectiveOutputPreset) {
        _uiState.value = _uiState.value.copy(perspectiveOutputPreset = preset)
        if (_uiState.value.isPerspectivePreviewActive) {
            updatePerspectivePreview()
        }
    }

    fun setPerspectiveEnhanceMode(mode: PerspectiveEngine.DocumentEnhanceMode) {
        _uiState.value = _uiState.value.copy(perspectiveEnhanceMode = mode)
        if (_uiState.value.isPerspectivePreviewActive) {
            updatePerspectivePreview()
        }
    }

    fun togglePerspectiveGrid(show: Boolean) {
        _uiState.value = _uiState.value.copy(showPerspectiveGrid = show)
    }

    fun togglePerspectivePreview(active: Boolean) {
        _uiState.value = _uiState.value.copy(isPerspectivePreviewActive = active)
        if (active) {
            updatePerspectivePreview()
        } else {
            _uiState.value = _uiState.value.copy(previewBitmap = _uiState.value.workingBitmap)
        }
    }

    private fun updatePerspectivePreview() {
        val current = _uiState.value.workingBitmap ?: return
        viewModelScope.launch {
            val warped = withContext(Dispatchers.Default) {
                PerspectiveEngine.warpPerspective(
                    bitmap = current,
                    quad = _uiState.value.perspectiveQuad,
                    preset = _uiState.value.perspectiveOutputPreset,
                    enhanceMode = _uiState.value.perspectiveEnhanceMode
                )
            }
            _uiState.value = _uiState.value.copy(previewBitmap = warped)
        }
    }

    fun autoDetectDocumentCorners() {
        val current = _uiState.value.workingBitmap ?: return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isAutoDetectingCorners = true)
            val detectedQuad = withContext(Dispatchers.Default) {
                PerspectiveEngine.detectDocumentQuad(current)
            }
            _uiState.value = _uiState.value.copy(
                perspectiveQuad = detectedQuad,
                isAutoDetectingCorners = false,
                lastOrientationMessage = "Auto-detected document page corners"
            )
            if (_uiState.value.isPerspectivePreviewActive) {
                updatePerspectivePreview()
            }
        }
    }

    fun setPerspectiveKeystoneTilt(
        verticalTilt: Float = _uiState.value.perspectiveVerticalTilt,
        horizontalTilt: Float = _uiState.value.perspectiveHorizontalTilt
    ) {
        val v = verticalTilt.coerceIn(-45f, 45f)
        val h = horizontalTilt.coerceIn(-45f, 45f)
        val quad = PerspectiveEngine.quadFromKeystoneTilt(v, h)
        _uiState.value = _uiState.value.copy(
            perspectiveVerticalTilt = v,
            perspectiveHorizontalTilt = h,
            perspectiveQuad = quad
        )
        if (_uiState.value.isPerspectivePreviewActive) {
            updatePerspectivePreview()
        }
    }

    fun resetPerspectiveQuad() {
        _uiState.value = _uiState.value.copy(
            perspectiveQuad = PerspectiveEngine.PerspectiveQuad.default(),
            perspectiveVerticalTilt = 0f,
            perspectiveHorizontalTilt = 0f,
            perspectiveOutputPreset = PerspectiveEngine.PerspectiveOutputPreset.AUTO,
            perspectiveEnhanceMode = PerspectiveEngine.DocumentEnhanceMode.ORIGINAL,
            isPerspectivePreviewActive = false,
            previewBitmap = _uiState.value.workingBitmap,
            lastOrientationMessage = "Reset corners to default boundary"
        )
    }

    fun applyPerspectiveCorrection() {
        val current = _uiState.value.workingBitmap ?: return
        recordHistoryState("Perspective Rectify")
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            val res = withContext(Dispatchers.Default) {
                advancedEditingUseCase.executePerspectiveWarp(
                    source = current,
                    quad = _uiState.value.perspectiveQuad,
                    preset = _uiState.value.perspectiveOutputPreset,
                    enhanceMode = _uiState.value.perspectiveEnhanceMode
                )
            }
            val warped = res.bitmap
            _uiState.value = _uiState.value.copy(
                workingBitmap = warped,
                previewBitmap = warped,
                isLoading = false,
                isPerspectivePreviewActive = false,
                perspectiveVerticalTilt = 0f,
                perspectiveHorizontalTilt = 0f,
                lastPerspectiveReport = res.verification,
                targetWidthPx = warped.width,
                targetHeightPx = warped.height,
                lastOrientationMessage = res.verification.summary,
                statusMessage = res.verification.summary
            )
            calculateEstimatedSize()
        }
    }

    fun straighten(degrees: Float) {
        val current = _uiState.value.workingBitmap ?: return
        viewModelScope.launch {
            val straightened = withContext(Dispatchers.Default) {
                BitmapUtils.straightenBitmap(current, degrees)
            }
            _uiState.value = _uiState.value.copy(
                workingBitmap = straightened,
                previewBitmap = straightened
            )
        }
    }

    fun setCustomRotationAngle(angle: Float) {
        val clamped = ((angle % 360f) + 360f) % 360f
        val displayAngle = if (clamped > 180f) clamped - 360f else clamped
        _uiState.value = _uiState.value.copy(
            rotationAngle = displayAngle,
            customAngleInput = String.format("%.1f", displayAngle)
        )
    }

    fun setCustomAngleInput(input: String) {
        val parsed = input.toFloatOrNull()
        if (parsed != null) {
            _uiState.value = _uiState.value.copy(
                customAngleInput = input,
                rotationAngle = parsed
            )
        } else {
            _uiState.value = _uiState.value.copy(customAngleInput = input)
        }
    }

    fun nudgeRotationAngle(delta: Float) {
        val current = _uiState.value.rotationAngle
        val newAngle = ((current + delta) * 10f).roundToInt() / 10f
        setCustomRotationAngle(newAngle)
    }

    fun setStraightenAngle(angle: Float) {
        val clamped = angle.coerceIn(-45f, 45f)
        _uiState.value = _uiState.value.copy(
            straightenAngle = ((clamped * 10f).roundToInt() / 10f)
        )
    }

    fun nudgeStraightenAngle(delta: Float) {
        val current = _uiState.value.straightenAngle
        val newAngle = ((current + delta) * 10f).roundToInt() / 10f
        setStraightenAngle(newAngle)
    }

    fun resetRotationAndStraighten() {
        _uiState.value = _uiState.value.copy(
            rotationAngle = 0f,
            customAngleInput = "0.0",
            straightenAngle = 0f,
            detectedHorizon = null,
            lastOrientationMessage = "Reset to 0.0°"
        )
    }

    fun setShowGrid(show: Boolean) {
        _uiState.value = _uiState.value.copy(showGrid = show)
    }

    fun setGridType(type: RotateStraightenEngine.GridType) {
        _uiState.value = _uiState.value.copy(gridType = type)
    }

    fun setGridOpacity(opacity: Float) {
        _uiState.value = _uiState.value.copy(gridOpacity = opacity.coerceIn(0.1f, 1.0f))
    }

    fun setShowHorizonGuide(show: Boolean) {
        _uiState.value = _uiState.value.copy(showHorizonGuide = show)
    }

    fun setHorizonGuideYOffset(offset: Float) {
        _uiState.value = _uiState.value.copy(horizonGuideYOffset = offset.coerceIn(0.1f, 0.9f))
    }

    fun setStraightenCropMode(mode: RotateStraightenEngine.StraightenCropMode) {
        _uiState.value = _uiState.value.copy(straightenCropMode = mode)
    }

    fun setStraightenBgColor(color: Int) {
        _uiState.value = _uiState.value.copy(straightenBgColor = color)
    }

    fun detectHorizon() {
        val current = _uiState.value.workingBitmap ?: return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isDetectingHorizon = true)
            val result = withContext(Dispatchers.Default) {
                RotateStraightenEngine.detectHorizonAngle(current)
            }
            _uiState.value = _uiState.value.copy(
                isDetectingHorizon = false,
                detectedHorizon = result,
                lastOrientationMessage = result.description
            )
        }
    }

    fun levelHorizonToZero() {
        val detected = _uiState.value.detectedHorizon
        if (detected != null) {
            setStraightenAngle(detected.angleDegrees)
        } else {
            setStraightenAngle(0f)
        }
    }

    fun autoOrientImage() {
        val current = _uiState.value.workingBitmap ?: return
        val exifOrientation = _uiState.value.exifData?.orientation
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isDetectingHorizon = true)
            val result = withContext(Dispatchers.Default) {
                RotateStraightenEngine.autoOrient(current, exifOrientation)
            }
            _uiState.value = _uiState.value.copy(
                isDetectingHorizon = false,
                workingBitmap = result.bitmap,
                previewBitmap = result.bitmap,
                targetWidthPx = result.bitmap.width,
                targetHeightPx = result.bitmap.height,
                rotationAngle = 0f,
                straightenAngle = 0f,
                lastOrientationMessage = "${result.source}: ${result.description}"
            )
            calculateEstimatedSize()
        }
    }

    fun applyRotationAndStraighten() {
        val current = _uiState.value.workingBitmap ?: return
        val totalAngle = _uiState.value.rotationAngle + _uiState.value.straightenAngle
        if (abs(totalAngle) < 0.01f) return
        recordHistoryState(String.format("Straighten %.1f°", totalAngle))

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            val res = withContext(Dispatchers.Default) {
                advancedEditingUseCase.executeStraighten(
                    source = current,
                    angleDegrees = totalAngle,
                    cropMode = _uiState.value.straightenCropMode,
                    backgroundColor = _uiState.value.straightenBgColor
                )
            }
            val transformed = res.bitmap
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                workingBitmap = transformed,
                previewBitmap = transformed,
                targetWidthPx = transformed.width,
                targetHeightPx = transformed.height,
                rotationAngle = 0f,
                straightenAngle = 0f,
                customAngleInput = "0.0",
                lastStraightenReport = res.verification,
                lastOrientationMessage = res.verification.summary,
                statusMessage = res.verification.summary
            )
            calculateEstimatedSize()
        }
    }

    // Passport & ID Photo Creator
    fun selectPassportPreset(preset: PassportPreset) {
        val unit = _uiState.value.passportUnit
        val wPhysical: Float
        val hPhysical: Float
        when (unit) {
            PassportDimensionUnit.MILLIMETERS -> {
                wPhysical = preset.widthMm
                hPhysical = preset.heightMm
            }
            PassportDimensionUnit.CENTIMETERS -> {
                wPhysical = preset.widthMm / 10f
                hPhysical = preset.heightMm / 10f
            }
            PassportDimensionUnit.INCHES -> {
                wPhysical = preset.widthMm / 25.4f
                hPhysical = preset.heightMm / 25.4f
            }
            PassportDimensionUnit.PIXELS -> {
                val d = preset.defaultDpi
                wPhysical = (preset.widthMm / 25.4f * d)
                hPhysical = (preset.heightMm / 25.4f * d)
            }
        }

        val pxW = (preset.widthMm / 25.4f * preset.defaultDpi).roundToInt()
        val pxH = (preset.heightMm / 25.4f * preset.defaultDpi).roundToInt()

        _uiState.value = _uiState.value.copy(
            selectedPassportPreset = preset,
            passportWidthPhysical = wPhysical,
            passportHeightPhysical = hPhysical,
            passportDpi = preset.defaultDpi,
            customPassportDpiInput = preset.defaultDpi.toString(),
            targetWidthPx = pxW,
            targetHeightPx = pxH,
            dpi = preset.defaultDpi,
            passportTargetSizeKb = preset.maxFileSizeKb,
            targetSizeKb = preset.maxFileSizeKb
        )
    }

    fun setIdCategory(category: IdDocumentCategory) {
        _uiState.value = _uiState.value.copy(selectedIdCategory = category)
    }

    fun setPassportFineRotation(degrees: Float) {
        _uiState.value = _uiState.value.copy(passportFineRotation = degrees)
    }

    fun resetPassportFineRotation() {
        _uiState.value = _uiState.value.copy(passportFineRotation = 0f)
    }

    fun toggleGridGuide() {
        _uiState.value = _uiState.value.copy(showGridGuide = !_uiState.value.showGridGuide)
    }

    fun setPassportTargetSizeKb(kb: Int?) {
        _uiState.value = _uiState.value.copy(
            passportTargetSizeKb = kb,
            targetSizeKb = kb
        )
    }

    fun setPassportExactPixels(w: Int, h: Int, lockAspectRatio: Boolean = _uiState.value.passportLockAspectRatio) {
        val safeW = w.coerceAtLeast(10)
        val safeH = h.coerceAtLeast(10)
        val dpi = _uiState.value.passportDpi
        val unit = _uiState.value.passportUnit
        val wMm = (safeW.toFloat() / dpi) * 25.4f
        val hMm = (safeH.toFloat() / dpi) * 25.4f
        val wPhysical = when (unit) {
            PassportDimensionUnit.MILLIMETERS -> wMm
            PassportDimensionUnit.CENTIMETERS -> wMm / 10f
            PassportDimensionUnit.INCHES -> wMm / 25.4f
            PassportDimensionUnit.PIXELS -> safeW.toFloat()
        }
        val hPhysical = when (unit) {
            PassportDimensionUnit.MILLIMETERS -> hMm
            PassportDimensionUnit.CENTIMETERS -> hMm / 10f
            PassportDimensionUnit.INCHES -> hMm / 25.4f
            PassportDimensionUnit.PIXELS -> safeH.toFloat()
        }
        _uiState.value = _uiState.value.copy(
            targetWidthPx = safeW,
            targetHeightPx = safeH,
            passportWidthPhysical = wPhysical,
            passportHeightPhysical = hPhysical,
            passportLockAspectRatio = lockAspectRatio
        )
    }

    fun setPassportLockAspectRatio(lock: Boolean) {
        _uiState.value = _uiState.value.copy(passportLockAspectRatio = lock)
    }

    fun cropToPassportAspectRatio() {
        val current = _uiState.value.workingBitmap ?: return
        val targetW = _uiState.value.targetWidthPx.toFloat()
        val targetH = _uiState.value.targetHeightPx.toFloat()
        if (targetW <= 0 || targetH <= 0) return

        val targetRatio = targetW / targetH
        val currentRatio = current.width.toFloat() / current.height.toFloat()

        val cropped = if (currentRatio > targetRatio) {
            val newWidth = (current.height * targetRatio).roundToInt().coerceIn(1, current.width)
            val xOffset = (current.width - newWidth) / 2
            Bitmap.createBitmap(current, xOffset, 0, newWidth, current.height)
        } else {
            val newHeight = (current.width / targetRatio).roundToInt().coerceIn(1, current.height)
            val yOffset = ((current.height - newHeight) * 0.35f).roundToInt().coerceIn(0, current.height - newHeight)
            Bitmap.createBitmap(current, 0, yOffset, current.width, newHeight)
        }

        _uiState.value = _uiState.value.copy(
            workingBitmap = cropped,
            previewBitmap = cropped,
            statusMessage = "Cropped to match document ratio (${targetW.toInt()}:${targetH.toInt()})"
        )
    }

    fun setPassportDimensions(w: Float, h: Float, unit: PassportDimensionUnit = _uiState.value.passportUnit, dpi: Int = _uiState.value.passportDpi) {
        val safeW = if (w > 0) w else 1f
        val safeH = if (h > 0) h else 1f
        val wMm: Float = when (unit) {
            PassportDimensionUnit.MILLIMETERS -> safeW
            PassportDimensionUnit.CENTIMETERS -> safeW * 10f
            PassportDimensionUnit.INCHES -> safeW * 25.4f
            PassportDimensionUnit.PIXELS -> safeW / dpi.toFloat() * 25.4f
        }
        val hMm: Float = when (unit) {
            PassportDimensionUnit.MILLIMETERS -> safeH
            PassportDimensionUnit.CENTIMETERS -> safeH * 10f
            PassportDimensionUnit.INCHES -> safeH * 25.4f
            PassportDimensionUnit.PIXELS -> safeH / dpi.toFloat() * 25.4f
        }
        val targetW = (wMm / 25.4f * dpi).roundToInt()
        val targetH = (hMm / 25.4f * dpi).roundToInt()

        _uiState.value = _uiState.value.copy(
            passportWidthPhysical = safeW,
            passportHeightPhysical = safeH,
            passportUnit = unit,
            passportDpi = dpi,
            targetWidthPx = targetW,
            targetHeightPx = targetH,
            dpi = dpi
        )
    }

    fun setPassportUnit(newUnit: PassportDimensionUnit) {
        val oldUnit = _uiState.value.passportUnit
        if (oldUnit == newUnit) return
        val currentW = _uiState.value.passportWidthPhysical
        val currentH = _uiState.value.passportHeightPhysical
        val currentDpi = _uiState.value.passportDpi

        // Convert current to mm
        val wMm: Float = when (oldUnit) {
            PassportDimensionUnit.MILLIMETERS -> currentW
            PassportDimensionUnit.CENTIMETERS -> currentW * 10f
            PassportDimensionUnit.INCHES -> currentW * 25.4f
            PassportDimensionUnit.PIXELS -> currentW / currentDpi.toFloat() * 25.4f
        }
        val hMm: Float = when (oldUnit) {
            PassportDimensionUnit.MILLIMETERS -> currentH
            PassportDimensionUnit.CENTIMETERS -> currentH * 10f
            PassportDimensionUnit.INCHES -> currentH * 25.4f
            PassportDimensionUnit.PIXELS -> currentH / currentDpi.toFloat() * 25.4f
        }

        // Convert mm to new unit
        val newW: Float = when (newUnit) {
            PassportDimensionUnit.MILLIMETERS -> wMm
            PassportDimensionUnit.CENTIMETERS -> wMm / 10f
            PassportDimensionUnit.INCHES -> wMm / 25.4f
            PassportDimensionUnit.PIXELS -> (wMm / 25.4f * currentDpi)
        }
        val newH: Float = when (newUnit) {
            PassportDimensionUnit.MILLIMETERS -> hMm
            PassportDimensionUnit.CENTIMETERS -> hMm / 10f
            PassportDimensionUnit.INCHES -> hMm / 25.4f
            PassportDimensionUnit.PIXELS -> (hMm / 25.4f * currentDpi)
        }

        _uiState.value = _uiState.value.copy(
            passportUnit = newUnit,
            passportWidthPhysical = newW,
            passportHeightPhysical = newH
        )
    }

    fun setPassportDpi(dpi: Int) {
        val safeDpi = dpi.coerceIn(10, 2400)
        _uiState.value = _uiState.value.copy(
            passportDpi = safeDpi,
            customPassportDpiInput = safeDpi.toString(),
            dpi = safeDpi
        )
        setPassportDimensions(_uiState.value.passportWidthPhysical, _uiState.value.passportHeightPhysical, _uiState.value.passportUnit, safeDpi)
    }

    fun setCustomPassportDpiText(text: String) {
        val filtered = text.filter { it.isDigit() }
        val parsed = filtered.toIntOrNull()
        _uiState.value = _uiState.value.copy(
            customPassportDpiInput = filtered,
            passportDpi = if (parsed != null && parsed in 10..2400) parsed else _uiState.value.passportDpi
        )
        if (parsed != null && parsed in 10..2400) {
            setPassportDimensions(_uiState.value.passportWidthPhysical, _uiState.value.passportHeightPhysical, _uiState.value.passportUnit, parsed)
        }
    }

    fun setPassportBgColor(color: BackgroundProcessor.PassportBgColor) {
        _uiState.value = _uiState.value.copy(passportBgColor = color)
    }

    fun setPassportTolerance(tolerance: Float) {
        _uiState.value = _uiState.value.copy(passportTolerance = tolerance.coerceIn(5f, 100f))
    }

    fun toggleBiometricGuides() {
        _uiState.value = _uiState.value.copy(showBiometricGuides = !_uiState.value.showBiometricGuides)
    }

    fun toggleFaceOvalGuide() {
        _uiState.value = _uiState.value.copy(showFaceOvalGuide = !_uiState.value.showFaceOvalGuide)
    }

    fun toggleCrownChinGuide() {
        _uiState.value = _uiState.value.copy(showHeadCrownChinGuides = !_uiState.value.showHeadCrownChinGuides)
    }

    fun toggleEyeLineGuide() {
        _uiState.value = _uiState.value.copy(showEyeLineGuide = !_uiState.value.showEyeLineGuide)
    }

    fun toggleCenterAxisGuide() {
        _uiState.value = _uiState.value.copy(showCenterAxisGuide = !_uiState.value.showCenterAxisGuide)
    }

    fun setPassportCopies(copies: Int) {
        _uiState.value = _uiState.value.copy(passportCopies = copies.coerceIn(1, 36))
    }

    fun setPassportSheetPaper(paper: SheetPaperPreset) {
        _uiState.value = _uiState.value.copy(passportSheetPaper = paper)
    }

    fun setPassportCutLineStyle(style: CutLineStyle) {
        _uiState.value = _uiState.value.copy(passportCutLineStyle = style)
    }

    fun setPassportExportTarget(target: PassportExportTarget) {
        _uiState.value = _uiState.value.copy(passportExportTarget = target)
    }

    fun setPassportExportFormat(format: ExportFormat) {
        _uiState.value = _uiState.value.copy(passportExportFormat = format)
    }

    fun saveCustomPassportPreset(
        country: String,
        docType: String,
        widthMm: Float,
        heightMm: Float,
        dpi: Int = 300,
        backgroundColor: String = "White",
        maxFileSizeKb: Int? = null,
        category: IdDocumentCategory = IdDocumentCategory.CUSTOM
    ) {
        val pxW = (widthMm / 25.4f * dpi).roundToInt()
        val pxH = (heightMm / 25.4f * dpi).roundToInt()
        val customPreset = PassportPreset(
            country = country.ifBlank { "Custom Preset" },
            documentType = docType.ifBlank { "${widthMm}x${heightMm} mm" },
            widthMm = widthMm,
            heightMm = heightMm,
            widthPxAt300Dpi = pxW,
            heightPxAt300Dpi = pxH,
            defaultDpi = dpi,
            backgroundColor = backgroundColor,
            maxFileSizeKb = maxFileSizeKb,
            isCustom = true,
            category = category,
            note = "Custom user preset. Verify requirements with relevant authority."
        )
        val updated = _uiState.value.customPassportPresets + customPreset
        _uiState.value = _uiState.value.copy(
            customPassportPresets = updated,
            selectedPassportPreset = customPreset,
            passportTargetSizeKb = maxFileSizeKb,
            targetSizeKb = maxFileSizeKb
        )
    }

    fun deleteCustomPassportPreset(preset: PassportPreset) {
        val updated = _uiState.value.customPassportPresets.filter { it != preset }
        _uiState.value = _uiState.value.copy(
            customPassportPresets = updated,
            selectedPassportPreset = if (_uiState.value.selectedPassportPreset == preset) PassportPreset.PRESETS.first() else _uiState.value.selectedPassportPreset
        )
    }

    fun setPassportAutoBiometricCrop(enabled: Boolean) {
        _uiState.value = _uiState.value.copy(passportAutoBiometricCrop = enabled)
    }

    fun setPassportAddThinBorder(enabled: Boolean) {
        _uiState.value = _uiState.value.copy(passportAddThinBorder = enabled)
    }

    fun setPassportSlateConfig(
        enabled: Boolean = _uiState.value.passportSlateConfig.enabled,
        applicantName: String = _uiState.value.passportSlateConfig.applicantName,
        photoDateText: String = _uiState.value.passportSlateConfig.photoDateText
    ) {
        _uiState.value = _uiState.value.copy(
            passportSlateConfig = _uiState.value.passportSlateConfig.copy(
                enabled = enabled,
                applicantName = applicantName,
                photoDateText = photoDateText
            )
        )
    }

    fun setPassportDocumentMode(mode: PassportIdDocumentEngine.DocumentProcessMode) {
        _uiState.value = _uiState.value.copy(passportDocumentMode = mode)
    }

    fun runBiometricComplianceAnalysis() {
        val current = _uiState.value.workingBitmap ?: return
        viewModelScope.launch {
            val report = withContext(Dispatchers.Default) {
                passportIdPhotoUseCase.analyzeBiometricCompliance(current, _uiState.value.passportDpi)
            }
            _uiState.value = _uiState.value.copy(
                biometricComplianceReport = report,
                statusMessage = if (report.overallCompliant) {
                    "✓ Biometric check passed (Head: ${report.headHeightPercent.roundToInt()}%, Eye Line: ${report.eyeLevelFromBottomPercent.roundToInt()}%)"
                } else {
                    report.recommendations.firstOrNull() ?: "Biometric check complete"
                }
            )
        }
    }

    fun applyAutoBiometricFraming() {
        val current = _uiState.value.workingBitmap ?: return
        val targetW = _uiState.value.targetWidthPx.toFloat().coerceAtLeast(10f)
        val targetH = _uiState.value.targetHeightPx.toFloat().coerceAtLeast(10f)
        recordHistoryState("Biometric Auto-Frame")
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            val (framed, report) = withContext(Dispatchers.Default) {
                val cropped = PassportIdDocumentEngine.autoCropBiometricFrame(current, targetW / targetH)
                val rep = PassportIdDocumentEngine.analyzeBiometricCompliance(cropped, _uiState.value.passportDpi)
                cropped to rep
            }
            _uiState.value = _uiState.value.copy(
                workingBitmap = framed,
                previewBitmap = framed,
                biometricComplianceReport = report,
                isLoading = false,
                statusMessage = "Biometric auto-framing applied (Head ratio: ${report.headHeightPercent.roundToInt()}%)"
            )
            calculateEstimatedSize()
        }
    }

    fun applyPassportProcessing() {
        val current = _uiState.value.workingBitmap ?: return
        recordHistoryState("Passport & ID Format")
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, statusMessage = "Applying document standards...")
            val s = _uiState.value
            val processResult = withContext(Dispatchers.Default) {
                passportIdPhotoUseCase.processPassportOrIdPhoto(
                    source = current,
                    config = PassportIdDocumentEngine.PassportIdProcessConfig(
                        preset = s.selectedPassportPreset ?: PassportPreset.PRESETS.first(),
                        widthPhysical = s.passportWidthPhysical,
                        heightPhysical = s.passportHeightPhysical,
                        unit = s.passportUnit,
                        dpi = s.passportDpi,
                        exactWidthPxOverride = s.targetWidthPx,
                        exactHeightPxOverride = s.targetHeightPx,
                        backgroundColor = s.passportBgColor,
                        backgroundTolerance = s.passportTolerance,
                        fineRotationDegrees = s.passportFineRotation,
                        autoBiometricCrop = s.passportAutoBiometricCrop,
                        addThinBorder = s.passportAddThinBorder,
                        slateConfig = s.passportSlateConfig,
                        documentMode = s.passportDocumentMode,
                        targetMaxFileSizeKb = s.passportTargetSizeKb ?: s.targetSizeKb,
                        exportFormat = s.passportExportFormat
                    )
                )
            }
            val result = processResult.bitmap
            _uiState.value = _uiState.value.copy(
                workingBitmap = result,
                previewBitmap = result,
                targetWidthPx = result.width,
                targetHeightPx = result.height,
                passportFineRotation = 0f,
                biometricComplianceReport = processResult.biometricReport,
                lastDpiReport = processResult.dpiReport,
                isLoading = false,
                statusMessage = "Document photo formatted (${result.width}×${result.height} px @ ${processResult.dpi} DPI)"
            )
            calculateEstimatedSize()
        }
    }

    fun exportPassportModule(onComplete: (ExportEngine.ExportResult) -> Unit) {
        val current = _uiState.value.workingBitmap ?: return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isExporting = true, statusMessage = "Generating export...")
            val s = _uiState.value
            val (result, bioReport, dpiReport) = withContext(Dispatchers.Default) {
                val processedRes = passportIdPhotoUseCase.processPassportOrIdPhoto(
                    source = current,
                    config = PassportIdDocumentEngine.PassportIdProcessConfig(
                        preset = s.selectedPassportPreset ?: PassportPreset.PRESETS.first(),
                        widthPhysical = s.passportWidthPhysical,
                        heightPhysical = s.passportHeightPhysical,
                        unit = s.passportUnit,
                        dpi = s.passportDpi,
                        exactWidthPxOverride = s.targetWidthPx,
                        exactHeightPxOverride = s.targetHeightPx,
                        backgroundColor = s.passportBgColor,
                        backgroundTolerance = s.passportTolerance,
                        fineRotationDegrees = s.passportFineRotation,
                        autoBiometricCrop = s.passportAutoBiometricCrop,
                        addThinBorder = s.passportAddThinBorder,
                        slateConfig = s.passportSlateConfig,
                        documentMode = s.passportDocumentMode,
                        targetMaxFileSizeKb = s.passportTargetSizeKb ?: s.targetSizeKb,
                        exportFormat = s.passportExportFormat
                    )
                )
                val processedSingle = processedRes.bitmap

                val finalExportBitmap = if (s.passportExportTarget == PassportExportTarget.PRINT_SHEET) {
                    val paper = s.passportSheetPaper
                    PhotoSheetGenerator.createPhotoSheet(
                        photo = processedSingle,
                        paperPreset = paper,
                        columns = paper.defaultCols,
                        rows = paper.defaultRows,
                        dpi = s.passportDpi,
                        cutLineStyle = s.passportCutLineStyle,
                        maxCopies = s.passportCopies,
                        backgroundColor = Color.WHITE
                    ).bitmap
                } else {
                    processedSingle
                }

                val exp = ExportEngine.exportImage(
                    context = getApplication(),
                    bitmap = finalExportBitmap,
                    format = s.passportExportFormat,
                    quality = 95,
                    targetSizeKb = s.passportTargetSizeKb ?: s.targetSizeKb,
                    fileSizeMode = FileSizeMode.MAXIMUM_CEILING,
                    fileNamePrefix = if (s.passportExportTarget == PassportExportTarget.PRINT_SHEET) "DocumentSheet" else "DocumentPhoto",
                    dpi = s.passportDpi,
                    stripExif = false
                )
                Triple(exp, processedRes.biometricReport, processedRes.dpiReport)
            }
            _uiState.value = _uiState.value.copy(
                isExporting = false,
                lastExportResult = result,
                lastVerificationReport = result.verificationReport,
                biometricComplianceReport = bioReport,
                lastDpiReport = dpiReport,
                statusMessage = if (result.success) "Photo exported successfully!" else "Export failed: ${result.errorMessage}"
            )
            onComplete(result)
        }
    }

    // Signature Preparation
    fun setSignatureConfig(
        inkColor: SignatureProcessor.OutputInkColor = _uiState.value.signatureInkColor,
        makeTransparent: Boolean = _uiState.value.signatureTransparentBg,
        threshold: Int = _uiState.value.signatureThreshold,
        autoTrim: Boolean = _uiState.value.signatureAutoTrim,
        strokeWeightDelta: Int = _uiState.value.signatureStrokeWeightDelta
    ) {
        _uiState.value = _uiState.value.copy(
            signatureInkColor = inkColor,
            signatureTransparentBg = makeTransparent,
            signatureThreshold = threshold,
            signatureAutoTrim = autoTrim,
            signatureStrokeWeightDelta = strokeWeightDelta
        )
    }

    fun processSignaturePhoto() {
        val current = _uiState.value.workingBitmap ?: return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, statusMessage = "Preparing signature ink...")
            val result = withContext(Dispatchers.Default) {
                SignatureProcessor.prepareSignature(
                    source = current,
                    threshold = _uiState.value.signatureThreshold,
                    makeTransparent = _uiState.value.signatureTransparentBg,
                    inkColor = _uiState.value.signatureInkColor,
                    autoTrim = _uiState.value.signatureAutoTrim,
                    strokeWeightDelta = _uiState.value.signatureStrokeWeightDelta
                )
            }
            _uiState.value = _uiState.value.copy(
                workingBitmap = result,
                previewBitmap = result,
                targetWidthPx = result.width,
                targetHeightPx = result.height,
                exportFormat = if (_uiState.value.signatureTransparentBg) ExportFormat.PNG else ExportFormat.JPEG,
                isLoading = false,
                statusMessage = "Signature optimized (${result.width}x${result.height} px)"
            )
            calculateEstimatedSize()
        }
    }

    // Photo Sheet Generator
    fun setSheetExactPhotoSizeMode(
        enabled: Boolean,
        photoWidthMm: Float = _uiState.value.sheetConfig.targetPhotoWidthMm,
        photoHeightMm: Float = _uiState.value.sheetConfig.targetPhotoHeightMm,
        autoFillMax: Boolean = _uiState.value.sheetConfig.autoFillMaxCopies
    ) {
        val updated = _uiState.value.sheetConfig.copy(
            useExactPhotoPhysicalSize = enabled,
            targetPhotoWidthMm = photoWidthMm.coerceIn(10f, 200f),
            targetPhotoHeightMm = photoHeightMm.coerceIn(10f, 280f),
            autoFillMaxCopies = autoFillMax
        )
        updateSheetConfig(updated)
    }

    fun setSheetShowCalibrationFooter(show: Boolean) {
        val updated = _uiState.value.sheetConfig.copy(showCalibrationFooter = show)
        updateSheetConfig(updated)
    }

    fun setSheetCopies(copies: Int, customInput: String? = null) {
        val updated = _uiState.value.sheetConfig.copy(
            copies = copies.coerceAtLeast(1),
            customCopiesInput = customInput ?: copies.toString()
        )
        updateSheetConfig(updated)
    }

    fun setSheetPaper(paper: SheetPaperPreset) {
        val updated = _uiState.value.sheetConfig.copy(paperPreset = paper)
        _uiState.value = _uiState.value.copy(sheetPaper = paper)
        updateSheetConfig(updated)
    }

    fun setSheetCustomDimensions(widthInches: Float, heightInches: Float) {
        val updated = _uiState.value.sheetConfig.copy(
            customWidthInches = widthInches.coerceAtLeast(1f),
            customHeightInches = heightInches.coerceAtLeast(1f)
        )
        updateSheetConfig(updated)
    }

    fun setSheetOrientation(orientation: SheetOrientation) {
        val updated = _uiState.value.sheetConfig.copy(orientation = orientation)
        updateSheetConfig(updated)
    }

    fun setSheetMarginMm(marginMm: Float) {
        val updated = _uiState.value.sheetConfig.copy(marginMm = marginMm.coerceIn(0f, 40f))
        updateSheetConfig(updated)
    }

    fun setSheetSpacingMm(spacingMm: Float) {
        val updated = _uiState.value.sheetConfig.copy(spacingMm = spacingMm.coerceIn(0f, 30f))
        updateSheetConfig(updated)
    }

    fun setSheetAlignment(alignment: SheetAlignment) {
        val updated = _uiState.value.sheetConfig.copy(alignment = alignment)
        updateSheetConfig(updated)
    }

    fun setSheetRotation(rotation: PhotoSheetRotation) {
        val updated = _uiState.value.sheetConfig.copy(rotation = rotation)
        updateSheetConfig(updated)
    }

    fun setSheetCutLineStyle(cutLineStyle: CutLineStyle) {
        val updated = _uiState.value.sheetConfig.copy(cutLineStyle = cutLineStyle)
        _uiState.value = _uiState.value.copy(sheetCutLines = cutLineStyle != CutLineStyle.NONE)
        updateSheetConfig(updated)
    }

    fun setSheetDpi(dpi: Int) {
        val updated = _uiState.value.sheetConfig.copy(dpi = dpi)
        updateSheetConfig(updated)
    }

    fun setSheetExportFormat(format: ExportFormat) {
        _uiState.value = _uiState.value.copy(sheetExportFormat = format)
    }

    fun setPhotoSheetConfig(
        paper: SheetPaperPreset = _uiState.value.sheetPaper,
        cols: Int = _uiState.value.sheetCols,
        rows: Int = _uiState.value.sheetRows,
        cutLines: Boolean = _uiState.value.sheetCutLines
    ) {
        val cutStyle = if (cutLines) CutLineStyle.SOLID else CutLineStyle.NONE
        val updated = _uiState.value.sheetConfig.copy(
            paperPreset = paper,
            columns = cols,
            rows = rows,
            cutLineStyle = cutStyle
        )
        _uiState.value = _uiState.value.copy(
            sheetPaper = paper,
            sheetCols = cols,
            sheetRows = rows,
            sheetCutLines = cutLines
        )
        updateSheetConfig(updated)
    }

    fun updateSheetConfig(config: PhotoSheetGenerator.SheetConfig) {
        _uiState.value = _uiState.value.copy(sheetConfig = config)
        generatePhotoSheet()
    }

    fun generatePhotoSheet() {
        val current = _uiState.value.workingBitmap ?: return
        viewModelScope.launch {
            val sheet = withContext(Dispatchers.Default) {
                PhotoSheetGenerator.createPhotoSheet(
                    photo = current,
                    config = _uiState.value.sheetConfig
                )
            }
            _uiState.value = _uiState.value.copy(
                sheetResult = sheet,
                previewBitmap = if (_uiState.value.activeTab == StudioTab.SHEET) sheet.bitmap else _uiState.value.previewBitmap,
                targetWidthPx = sheet.bitmap.width,
                targetHeightPx = sheet.bitmap.height,
                sheetCols = sheet.cols,
                sheetRows = sheet.rows,
                statusMessage = "Photo Sheet: ${sheet.totalCopies} copies on ${sheet.paperPreset.name}"
            )
        }
    }

    fun exportPhotoSheet(onDone: (ExportEngine.ExportResult) -> Unit = {}) {
        val current = _uiState.value.workingBitmap ?: return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isExporting = true, statusMessage = "Exporting photo sheet...")
            val sheetBmp = _uiState.value.sheetResult?.bitmap ?: run {
                val res = withContext(Dispatchers.Default) {
                    PhotoSheetGenerator.createPhotoSheet(current, _uiState.value.sheetConfig)
                }
                _uiState.value = _uiState.value.copy(sheetResult = res)
                res.bitmap
            }

            val format = _uiState.value.sheetExportFormat
            val result = withContext(Dispatchers.IO) {
                ExportEngine.exportImage(
                    context = getApplication(),
                    bitmap = sheetBmp,
                    format = format,
                    quality = 95,
                    fileNamePrefix = "PhotoSheet_${_uiState.value.sheetConfig.copies}copies",
                    dpi = _uiState.value.sheetConfig.dpi
                )
            }

            if (result.success && result.outputUri != null) {
                val thumb = BitmapUtils.generateThumbnailBase64(sheetBmp)
                val historyItem = HistoryEntity(
                    title = "PhotoSheet_${_uiState.value.sheetConfig.copies}copies",
                    outputUriString = result.outputUri.toString(),
                    operationType = "Photo Sheet",
                    width = result.width,
                    height = result.height,
                    fileSizeBytes = result.fileSizeBytes,
                    format = result.format.extension.uppercase(),
                    previewThumbnailBase64 = thumb
                )
                repository.insertHistory(historyItem)
            }

            _uiState.value = _uiState.value.copy(
                isExporting = false,
                lastExportResult = result,
                lastVerificationReport = result.verificationReport,
                statusMessage = if (result.success) "Saved ${result.format.displayName}!" else result.errorMessage
            )
            onDone(result)
        }
    }

    // Preset selection & Configurable Social Media Presets (Request 35)
    fun getEffectiveSocialPresets(): List<SocialMediaPreset> {
        val state = _uiState.value
        val defaultsWithOverrides = SocialMediaPreset.DEFAULT_PRESETS.map { defaultPreset ->
            state.modifiedSocialPresets[defaultPreset.id] ?: defaultPreset
        }
        return defaultsWithOverrides + state.customSocialPresets
    }

    fun applySocialPreset(preset: SocialMediaPreset) {
        setTargetDimensions(preset.width, preset.height, false)
        val format = when (preset.recommendedFormat.uppercase()) {
            "PNG" -> ExportFormat.PNG
            "WEBP", "WEBP_LOSSY" -> ExportFormat.WEBP_LOSSY
            "WEBP_LOSSLESS" -> ExportFormat.WEBP_LOSSLESS
            "PDF" -> ExportFormat.PDF
            else -> ExportFormat.JPEG
        }
        val cropMode = try {
            ResizeMode.valueOf(preset.cropMode.uppercase())
        } catch (e: Exception) {
            ResizeMode.FIT
        }

        val updatedBatchConfig = _uiState.value.batchConfig.copy(
            exactWidth = preset.width,
            exactHeight = preset.height,
            cropMode = cropMode,
            format = format,
            dpi = preset.dpi,
            targetMaxKb = preset.maxFileSizeKb
        )

        _uiState.value = _uiState.value.copy(
            targetWidthPx = preset.width,
            targetHeightPx = preset.height,
            dpi = preset.dpi,
            customDpiInput = preset.dpi.toString(),
            exportFormat = format,
            targetSizeKb = preset.maxFileSizeKb,
            resizeMode = cropMode,
            batchConfig = updatedBatchConfig,
            statusMessage = "Applied preset: ${preset.platform} ${preset.name} (${preset.width}×${preset.height} px, ${preset.aspectRatioLabel})"
        )
        calculateEstimatedSize()
        updatePreview()
    }

    fun createCustomSocialPreset(
        platform: String,
        name: String,
        category: SocialMediaCategory,
        width: Int,
        height: Int,
        aspectRatioLabel: String? = null,
        recommendedFormat: String = "JPG",
        maxFileSizeKb: Int? = null,
        dpi: Int = 72,
        cropMode: String = "FIT",
        note: String = "Custom user dimension"
    ) {
        val calculatedRatio = aspectRatioLabel?.ifBlank { null } ?: formatCalculatedAspectRatio(width, height)
        val newPreset = SocialMediaPreset(
            id = "custom_social_${System.currentTimeMillis()}_${(100..999).random()}",
            platform = platform.ifBlank { "Custom" },
            name = name.ifBlank { "${width}×${height}" },
            category = category,
            width = width.coerceAtLeast(1),
            height = height.coerceAtLeast(1),
            aspectRatioLabel = calculatedRatio,
            recommendedFormat = recommendedFormat.uppercase(),
            maxFileSizeKb = maxFileSizeKb?.coerceAtLeast(1),
            dpi = dpi.coerceIn(1, 2400),
            cropMode = cropMode.uppercase(),
            note = note.ifBlank { "Custom user dimension" },
            isCustom = true,
            isModified = false
        )
        val updatedCustomList = _uiState.value.customSocialPresets + newPreset
        _uiState.value = _uiState.value.copy(
            customSocialPresets = updatedCustomList,
            statusMessage = "Custom preset '${newPreset.platform} - ${newPreset.name}' saved!"
        )
        persistSocialPresets()
    }

    fun updateSocialPreset(preset: SocialMediaPreset) {
        if (preset.isCustom) {
            val updated = _uiState.value.customSocialPresets.map {
                if (it.id == preset.id) preset else it
            }
            _uiState.value = _uiState.value.copy(
                customSocialPresets = updated,
                statusMessage = "Preset '${preset.name}' updated!"
            )
        } else {
            val updatedMap = _uiState.value.modifiedSocialPresets.toMutableMap()
            updatedMap[preset.id] = preset.copy(isModified = true)
            _uiState.value = _uiState.value.copy(
                modifiedSocialPresets = updatedMap,
                statusMessage = "Specification for '${preset.platform} - ${preset.name}' updated!"
            )
        }
        persistSocialPresets()
    }

    fun duplicateSocialPreset(preset: SocialMediaPreset, newName: String? = null) {
        val copyPreset = preset.copy(
            id = "custom_social_${System.currentTimeMillis()}_${(100..999).random()}",
            name = newName ?: "${preset.name} (Copy)",
            isCustom = true,
            isModified = false
        )
        val updatedCustomList = _uiState.value.customSocialPresets + copyPreset
        _uiState.value = _uiState.value.copy(
            customSocialPresets = updatedCustomList,
            statusMessage = "Preset '${copyPreset.name}' duplicated!"
        )
        persistSocialPresets()
    }

    fun deleteSocialPreset(preset: SocialMediaPreset) {
        if (preset.isCustom) {
            val updated = _uiState.value.customSocialPresets.filter { it.id != preset.id }
            _uiState.value = _uiState.value.copy(
                customSocialPresets = updated,
                statusMessage = "Preset '${preset.name}' deleted"
            )
        } else {
            resetSocialPresetToDefault(preset.id)
        }
        persistSocialPresets()
    }

    fun resetSocialPresetToDefault(presetId: String) {
        val updatedMap = _uiState.value.modifiedSocialPresets.toMutableMap()
        updatedMap.remove(presetId)
        _uiState.value = _uiState.value.copy(
            modifiedSocialPresets = updatedMap,
            statusMessage = "Reset to official platform spec"
        )
        persistSocialPresets()
    }

    fun resetAllSocialPresetsToDefault() {
        _uiState.value = _uiState.value.copy(
            customSocialPresets = emptyList(),
            modifiedSocialPresets = emptyMap(),
            statusMessage = "All social media presets reset to factory specifications"
        )
        val prefs = getApplication<Application>().getSharedPreferences("social_presets_prefs", android.content.Context.MODE_PRIVATE)
        prefs.edit().clear().apply()
    }

    private fun formatCalculatedAspectRatio(width: Int, height: Int): String {
        if (width <= 0 || height <= 0) return "1:1"
        fun gcd(a: Int, b: Int): Int = if (b == 0) a else gcd(b, a % b)
        val div = gcd(width, height)
        val rw = width / div
        val rh = height / div
        return if (rw < 50 && rh < 50) "$rw:$rh" else {
            val ratio = width.toFloat() / height.toFloat()
            String.format(java.util.Locale.US, "%.2f:1", ratio)
        }
    }

    private fun persistSocialPresets() {
        try {
            val prefs = getApplication<Application>().getSharedPreferences("social_presets_prefs", android.content.Context.MODE_PRIVATE)
            val customArray = org.json.JSONArray()
            for (p in _uiState.value.customSocialPresets) {
                val obj = org.json.JSONObject().apply {
                    put("id", p.id)
                    put("platform", p.platform)
                    put("name", p.name)
                    put("category", p.category.name)
                    put("width", p.width)
                    put("height", p.height)
                    put("aspectRatioLabel", p.aspectRatioLabel)
                    put("recommendedFormat", p.recommendedFormat)
                    if (p.maxFileSizeKb != null) put("maxFileSizeKb", p.maxFileSizeKb)
                    put("dpi", p.dpi)
                    put("cropMode", p.cropMode)
                    put("note", p.note)
                    put("isCustom", true)
                }
                customArray.put(obj)
            }

            val modifiedArray = org.json.JSONArray()
            for ((key, p) in _uiState.value.modifiedSocialPresets) {
                val obj = org.json.JSONObject().apply {
                    put("id", key)
                    put("platform", p.platform)
                    put("name", p.name)
                    put("category", p.category.name)
                    put("width", p.width)
                    put("height", p.height)
                    put("aspectRatioLabel", p.aspectRatioLabel)
                    put("recommendedFormat", p.recommendedFormat)
                    if (p.maxFileSizeKb != null) put("maxFileSizeKb", p.maxFileSizeKb)
                    put("dpi", p.dpi)
                    put("cropMode", p.cropMode)
                    put("note", p.note)
                    put("isModified", true)
                }
                modifiedArray.put(obj)
            }

            prefs.edit()
                .putString("custom_social_presets", customArray.toString())
                .putString("modified_social_presets", modifiedArray.toString())
                .apply()
        } catch (e: Exception) {
            // Ignore persistence error
        }
    }

    private fun loadSocialPresetsFromStorage() {
        try {
            val prefs = getApplication<Application>().getSharedPreferences("social_presets_prefs", android.content.Context.MODE_PRIVATE)
            val customJson = prefs.getString("custom_social_presets", null)
            val modifiedJson = prefs.getString("modified_social_presets", null)

            val customList = mutableListOf<SocialMediaPreset>()
            if (!customJson.isNullOrBlank()) {
                val array = org.json.JSONArray(customJson)
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    val cat = try {
                        SocialMediaCategory.valueOf(obj.optString("category", "CUSTOM"))
                    } catch (e: Exception) {
                        SocialMediaCategory.CUSTOM
                    }
                    customList.add(
                        SocialMediaPreset(
                            id = obj.optString("id", java.util.UUID.randomUUID().toString()),
                            platform = obj.optString("platform", "Custom"),
                            name = obj.optString("name", "Custom Spec"),
                            category = cat,
                            width = obj.optInt("width", 1080),
                            height = obj.optInt("height", 1080),
                            aspectRatioLabel = obj.optString("aspectRatioLabel", "1:1"),
                            recommendedFormat = obj.optString("recommendedFormat", "JPG"),
                            maxFileSizeKb = if (obj.has("maxFileSizeKb")) obj.getInt("maxFileSizeKb") else null,
                            dpi = obj.optInt("dpi", 72),
                            cropMode = obj.optString("cropMode", "FIT"),
                            note = obj.optString("note", "Custom user dimension"),
                            isCustom = true,
                            isModified = false
                        )
                    )
                }
            }

            val modifiedMap = mutableMapOf<String, SocialMediaPreset>()
            if (!modifiedJson.isNullOrBlank()) {
                val array = org.json.JSONArray(modifiedJson)
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    val id = obj.optString("id")
                    if (id.isNotBlank()) {
                        val cat = try {
                            SocialMediaCategory.valueOf(obj.optString("category", "POSTS"))
                        } catch (e: Exception) {
                            SocialMediaCategory.POSTS
                        }
                        modifiedMap[id] = SocialMediaPreset(
                            id = id,
                            platform = obj.optString("platform", "Platform"),
                            name = obj.optString("name", "Preset"),
                            category = cat,
                            width = obj.optInt("width", 1080),
                            height = obj.optInt("height", 1080),
                            aspectRatioLabel = obj.optString("aspectRatioLabel", "1:1"),
                            recommendedFormat = obj.optString("recommendedFormat", "JPG"),
                            maxFileSizeKb = if (obj.has("maxFileSizeKb")) obj.getInt("maxFileSizeKb") else null,
                            dpi = obj.optInt("dpi", 72),
                            cropMode = obj.optString("cropMode", "FIT"),
                            note = obj.optString("note", "Updated platform guideline"),
                            isCustom = false,
                            isModified = true
                        )
                    }
                }
            }

            _uiState.value = _uiState.value.copy(
                customSocialPresets = customList,
                modifiedSocialPresets = modifiedMap
            )
        } catch (e: Exception) {
            // Ignore load error
        }
    }

    fun applyCustomPreset(preset: PresetEntity) {
        setTargetDimensions(preset.targetWidth, preset.targetHeight, false)
        val format = when (preset.format.uppercase()) {
            "JPG", "JPEG" -> ExportFormat.JPEG
            "PNG" -> ExportFormat.PNG
            "WEBP", "WEBP_LOSSY" -> ExportFormat.WEBP_LOSSY
            "WEBP_LOSSLESS" -> ExportFormat.WEBP_LOSSLESS
            "PDF" -> ExportFormat.PDF
            else -> ExportFormat.values().firstOrNull {
                it.extension.equals(preset.format, true) || it.name.equals(preset.format, true)
            } ?: ExportFormat.JPEG
        }
        val cropMode = try {
            ResizeMode.valueOf(preset.cropMode.uppercase())
        } catch (e: Exception) {
            ResizeMode.FIT
        }

        val updatedBatchConfig = _uiState.value.batchConfig.copy(
            exactWidth = preset.targetWidth,
            exactHeight = preset.targetHeight,
            cropMode = cropMode,
            format = format,
            dpi = preset.dpi,
            quality = preset.quality,
            targetMaxKb = preset.maxFileSizeKb
        )

        _uiState.value = _uiState.value.copy(
            targetWidthPx = preset.targetWidth,
            targetHeightPx = preset.targetHeight,
            dpi = preset.dpi,
            customDpiInput = preset.dpi.toString(),
            exportFormat = format,
            targetSizeKb = preset.maxFileSizeKb,
            quality = preset.quality,
            batchConfig = updatedBatchConfig,
            statusMessage = "Preset '${preset.name}' applied (${preset.targetWidth}×${preset.targetHeight} px, ${preset.format}, ${preset.cropMode})"
        )
        calculateEstimatedSize()
        updatePreview()
    }

    fun createCustomPreset(
        name: String,
        targetWidth: Int,
        targetHeight: Int,
        dpi: Int = 300,
        format: String = "JPG",
        maxFileSizeKb: Int? = null,
        quality: Int = 90,
        cropMode: String = "FIT"
    ) {
        viewModelScope.launch {
            val preset = PresetEntity(
                name = name.ifBlank { "Preset ${targetWidth}x${targetHeight}" },
                targetWidth = targetWidth.coerceAtLeast(1),
                targetHeight = targetHeight.coerceAtLeast(1),
                dpi = dpi.coerceIn(1, 2400),
                format = format.uppercase(),
                maxFileSizeKb = maxFileSizeKb?.coerceAtLeast(1),
                quality = quality.coerceIn(1, 100),
                cropMode = cropMode.uppercase()
            )
            repository.insertPreset(preset)
            _uiState.value = _uiState.value.copy(statusMessage = "Preset '$name' created!")
        }
    }

    fun updateCustomPreset(preset: PresetEntity) {
        viewModelScope.launch {
            repository.updatePreset(preset)
            _uiState.value = _uiState.value.copy(statusMessage = "Preset '${preset.name}' updated!")
        }
    }

    fun duplicateCustomPreset(preset: PresetEntity, newName: String? = null) {
        viewModelScope.launch {
            val duplicate = preset.copy(
                id = 0,
                name = newName ?: "${preset.name} (Copy)"
            )
            repository.insertPreset(duplicate)
            _uiState.value = _uiState.value.copy(statusMessage = "Preset '${duplicate.name}' duplicated!")
        }
    }

    fun saveCurrentAsCustomPreset(name: String, cropMode: String = "FIT") {
        val state = _uiState.value
        createCustomPreset(
            name = name,
            targetWidth = state.targetWidthPx,
            targetHeight = state.targetHeightPx,
            dpi = state.dpi,
            format = state.exportFormat.extension.uppercase(),
            maxFileSizeKb = state.targetSizeKb,
            quality = state.quality,
            cropMode = cropMode
        )
    }

    fun deleteCustomPreset(preset: PresetEntity) {
        viewModelScope.launch {
            repository.deletePreset(preset)
            _uiState.value = _uiState.value.copy(statusMessage = "Preset '${preset.name}' deleted")
        }
    }

    // EXIF & Metadata Privacy (Request 28)
    fun setMetadataPolicy(policy: MetadataPolicy) {
        val currentCfg = _uiState.value.metadataPrivacyConfig
        val updatedCfg = currentCfg.copy(policy = policy)
        _uiState.value = _uiState.value.copy(
            metadataPrivacyConfig = updatedCfg,
            stripExifOnExport = policy == MetadataPolicy.STRIP_ALL
        )
    }

    fun setRemoveGps(remove: Boolean) {
        val updatedCfg = _uiState.value.metadataPrivacyConfig.copy(
            policy = MetadataPolicy.CUSTOM_SELECTIVE,
            removeGps = remove
        )
        _uiState.value = _uiState.value.copy(
            metadataPrivacyConfig = updatedCfg,
            stripExifOnExport = false
        )
    }

    fun setRemoveCameraInfo(remove: Boolean) {
        val updatedCfg = _uiState.value.metadataPrivacyConfig.copy(
            policy = MetadataPolicy.CUSTOM_SELECTIVE,
            removeCameraInfo = remove
        )
        _uiState.value = _uiState.value.copy(
            metadataPrivacyConfig = updatedCfg,
            stripExifOnExport = false
        )
    }

    fun setRemoveDeviceInfo(remove: Boolean) {
        val updatedCfg = _uiState.value.metadataPrivacyConfig.copy(
            policy = MetadataPolicy.CUSTOM_SELECTIVE,
            removeDeviceInfo = remove
        )
        _uiState.value = _uiState.value.copy(
            metadataPrivacyConfig = updatedCfg,
            stripExifOnExport = false
        )
    }

    fun setRemoveDateMetadata(remove: Boolean) {
        val updatedCfg = _uiState.value.metadataPrivacyConfig.copy(
            policy = MetadataPolicy.CUSTOM_SELECTIVE,
            removeDateMetadata = remove
        )
        _uiState.value = _uiState.value.copy(
            metadataPrivacyConfig = updatedCfg,
            stripExifOnExport = false
        )
    }

    fun setCustomAuthorInfo(artist: String, copyright: String, comment: String = "") {
        val updatedCfg = _uiState.value.metadataPrivacyConfig.copy(
            customArtist = artist,
            customCopyright = copyright,
            customComment = comment
        )
        _uiState.value = _uiState.value.copy(
            metadataPrivacyConfig = updatedCfg,
            customArtist = artist,
            customCopyright = copyright,
            customComment = comment
        )
    }

    fun stripAllMetadataNow() {
        val updatedCfg = _uiState.value.metadataPrivacyConfig.copy(
            policy = MetadataPolicy.STRIP_ALL,
            removeGps = true,
            removeCameraInfo = true,
            removeDeviceInfo = true,
            removeDateMetadata = true
        )
        _uiState.value = _uiState.value.copy(
            metadataPrivacyConfig = updatedCfg,
            stripExifOnExport = true
        )
    }

    fun keepAllMetadataNow() {
        val updatedCfg = _uiState.value.metadataPrivacyConfig.copy(
            policy = MetadataPolicy.KEEP_ALL,
            removeGps = false,
            removeCameraInfo = false,
            removeDeviceInfo = false,
            removeDateMetadata = false
        )
        _uiState.value = _uiState.value.copy(
            metadataPrivacyConfig = updatedCfg,
            stripExifOnExport = false
        )
    }

    fun setExifConfig(strip: Boolean, artist: String, copyright: String) {
        val policy = if (strip) MetadataPolicy.STRIP_ALL else MetadataPolicy.CUSTOM_SELECTIVE
        val updatedCfg = _uiState.value.metadataPrivacyConfig.copy(
            policy = policy,
            customArtist = artist,
            customCopyright = copyright
        )
        _uiState.value = _uiState.value.copy(
            metadataPrivacyConfig = updatedCfg,
            stripExifOnExport = strip,
            customArtist = artist,
            customCopyright = copyright
        )
    }

    // Export Single Image
    fun exportCurrent(onDone: (ExportEngine.ExportResult) -> Unit = {}) {
        if (_uiState.value.activeTab == StudioTab.SHEET) {
            exportPhotoSheet(onDone)
            return
        }
        val current = _uiState.value.workingBitmap ?: return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isExporting = true, statusMessage = "Exporting...")
            val result = withContext(Dispatchers.IO) {
                // Guarantee exported pixels match targetWidthPx and targetHeightPx exactly
                val targetW = _uiState.value.targetWidthPx
                val targetH = _uiState.value.targetHeightPx
                val bitmapToExport = if (current.width != targetW || current.height != targetH) {
                    BitmapUtils.resizeBitmap(current, targetW, targetH)
                } else current

                ExportEngine.exportImage(
                    context = getApplication(),
                    bitmap = bitmapToExport,
                    format = _uiState.value.exportFormat,
                    quality = _uiState.value.quality,
                    targetSizeKb = _uiState.value.targetSizeKb,
                    fileSizeMode = _uiState.value.fileSizeMode,
                    allowDimensionAdjustment = _uiState.value.allowDimensionDownscaling,
                    dpi = _uiState.value.dpi,
                    privacyConfig = _uiState.value.metadataPrivacyConfig,
                    sourceUri = _uiState.value.currentImageUri,
                    stripExif = _uiState.value.metadataPrivacyConfig.policy == MetadataPolicy.STRIP_ALL,
                    artist = _uiState.value.metadataPrivacyConfig.customArtist.ifBlank { null },
                    copyright = _uiState.value.metadataPrivacyConfig.customCopyright.ifBlank { null },
                    jpegBackgroundColor = _uiState.value.jpegBackgroundColor
                )
            }

            if (result.success && result.outputUri != null) {
                // Save to Room History
                recordOperationHistory(
                    title = _uiState.value.originalFileName.ifBlank { "Export_${System.currentTimeMillis()}" },
                    operationType = _uiState.value.activeTab.label,
                    operationDetails = "${result.width}×${result.height} px • ${result.format.extension.uppercase()} ${_uiState.value.quality}% • ${_uiState.value.dpi} DPI",
                    result = result,
                    sourceOriginalUri = _uiState.value.currentImageUri,
                    originalBmp = _uiState.value.originalBitmap,
                    outputBmp = current,
                    instructions = getCurrentEditingInstructions("Export")
                )
            }

            _uiState.value = _uiState.value.copy(
                isExporting = false,
                lastExportResult = result,
                lastVerificationReport = result.verificationReport,
                statusMessage = if (result.success) "Saved to Gallery & Studio storage!" else result.errorMessage
            )
            onDone(result)
        }
    }

    // Batch Processing (Phase 9: Multi-Select, Resize, Compress, Convert, Metadata, Progress, Cancel, Retry)
    fun updateBatchConfig(updater: (BatchConfig) -> BatchConfig) {
        _uiState.value = _uiState.value.copy(batchConfig = updater(_uiState.value.batchConfig))
    }

    fun setBatchConfig(config: BatchConfig) {
        _uiState.value = _uiState.value.copy(batchConfig = config)
    }

    fun setBatchSkipDuplicates(skip: Boolean) {
        _uiState.value = _uiState.value.copy(batchSkipDuplicates = skip)
    }

    fun setBatchSortOption(sortOption: BatchSortOption) {
        val sorted = batchProcessingUseCase.sortItems(_uiState.value.batchItems, sortOption)
        _uiState.value = _uiState.value.copy(
            batchSortOption = sortOption,
            batchItems = sorted,
            batchSummaryReport = batchProcessingUseCase.computeSummary(sorted)
        )
    }

    fun setBatchFilterOption(filterOption: BatchFilterOption) {
        _uiState.value = _uiState.value.copy(batchFilterOption = filterOption)
    }

    fun addBatchUris(uris: List<Uri>, skipDuplicates: Boolean = _uiState.value.batchSkipDuplicates) {
        if (uris.isEmpty()) return
        viewModelScope.launch {
            val existing = _uiState.value.batchItems
            val addedItems = withContext(Dispatchers.IO) {
                batchProcessingUseCase.inspectAndCreateItems(
                    context = getApplication(),
                    uris = uris,
                    existingItems = existing,
                    skipDuplicates = skipDuplicates
                )
            }
            val combined = batchProcessingUseCase.sortItems(
                existing + addedItems,
                _uiState.value.batchSortOption
            )
            val skippedCount = uris.size - addedItems.size
            val msg = if (skippedCount > 0) {
                "Added ${addedItems.size} photo(s) ($skippedCount duplicate(s) skipped)"
            } else {
                "Added ${addedItems.size} photo(s) to batch queue"
            }
            _uiState.value = _uiState.value.copy(
                batchItems = combined,
                batchSummaryReport = batchProcessingUseCase.computeSummary(combined),
                statusMessage = msg
            )
        }
    }

    fun toggleBatchItemSelection(id: String) {
        val updated = batchProcessingUseCase.toggleSelection(_uiState.value.batchItems, id)
        _uiState.value = _uiState.value.copy(batchItems = updated)
    }

    fun selectAllBatchItems() {
        val updated = batchProcessingUseCase.selectAll(_uiState.value.batchItems)
        _uiState.value = _uiState.value.copy(
            batchItems = updated,
            statusMessage = "Selected all ${updated.size} items"
        )
    }

    fun deselectAllBatchItems() {
        val updated = batchProcessingUseCase.deselectAll(_uiState.value.batchItems)
        _uiState.value = _uiState.value.copy(
            batchItems = updated,
            statusMessage = "Cleared selection"
        )
    }

    fun invertBatchSelection() {
        val updated = batchProcessingUseCase.invertSelection(_uiState.value.batchItems)
        val selCount = updated.count { it.selected }
        _uiState.value = _uiState.value.copy(
            batchItems = updated,
            statusMessage = "Inverted selection ($selCount selected)"
        )
    }

    fun removeSelectedBatchItems() {
        val before = _uiState.value.batchItems.size
        val updated = batchProcessingUseCase.removeSelected(_uiState.value.batchItems)
        val removed = before - updated.size
        _uiState.value = _uiState.value.copy(
            batchItems = updated,
            batchSummaryReport = batchProcessingUseCase.computeSummary(updated),
            statusMessage = "Removed $removed selected item(s)"
        )
    }

    fun removeCompletedBatchItems() {
        val updated = _uiState.value.batchItems.filter { it.status != BatchStatus.COMPLETED }
        _uiState.value = _uiState.value.copy(
            batchItems = updated,
            batchSummaryReport = batchProcessingUseCase.computeSummary(updated),
            statusMessage = "Cleared completed items from queue"
        )
    }

    fun removeBatchItem(id: String) {
        val updated = _uiState.value.batchItems.filter { it.id != id }
        _uiState.value = _uiState.value.copy(
            batchItems = updated,
            batchSummaryReport = batchProcessingUseCase.computeSummary(updated)
        )
    }

    fun clearBatch() {
        cancelBatchProcessing()
        _uiState.value = _uiState.value.copy(
            batchItems = emptyList(),
            batchCurrentProcessingIndex = -1,
            batchCurrentFileName = "",
            batchCurrentStage = "",
            batchOverallProgress = 0f,
            batchElapsedTimeMs = 0L,
            batchSummaryReport = BatchSummaryReport()
        )
    }

    fun cancelBatchProcessing() {
        batchCancelFlag.set(true)
        batchProcessingJob?.cancel()
        batchProcessingJob = null
        val updated = _uiState.value.batchItems.map { item ->
            if (item.status == BatchStatus.PROCESSING || item.status == BatchStatus.PENDING) {
                item.copy(
                    status = BatchStatus.CANCELLED,
                    progress = 0f,
                    stageLabel = "Cancelled",
                    errorMessage = "Processing cancelled"
                )
            } else {
                item
            }
        }
        _uiState.value = _uiState.value.copy(
            batchItems = updated,
            isBatchProcessing = false,
            batchCurrentProcessingIndex = -1,
            batchCurrentFileName = "",
            batchCurrentStage = "Cancelled",
            batchSummaryReport = batchProcessingUseCase.computeSummary(updated),
            statusMessage = "Batch processing cancelled"
        )
    }

    fun cancelSingleBatchItem(id: String) {
        val updated = _uiState.value.batchItems.map { item ->
            if (item.id == id && (item.status == BatchStatus.PENDING || item.status == BatchStatus.PROCESSING)) {
                item.copy(
                    status = BatchStatus.CANCELLED,
                    progress = 0f,
                    stageLabel = "Cancelled",
                    errorMessage = "Cancelled by user"
                )
            } else {
                item
            }
        }
        _uiState.value = _uiState.value.copy(
            batchItems = updated,
            batchSummaryReport = batchProcessingUseCase.computeSummary(updated)
        )
    }

    fun retryFailedBatchItems() {
        val resetList = batchProcessingUseCase.prepareRetryFailedAndCancelled(_uiState.value.batchItems)
        _uiState.value = _uiState.value.copy(
            batchItems = resetList,
            batchSummaryReport = batchProcessingUseCase.computeSummary(resetList)
        )
        runBatchProcessing()
    }

    fun retrySingleBatchItem(id: String) {
        val resetList = batchProcessingUseCase.prepareRetrySingleItem(_uiState.value.batchItems, id)
        _uiState.value = _uiState.value.copy(
            batchItems = resetList,
            batchSummaryReport = batchProcessingUseCase.computeSummary(resetList)
        )
        runBatchProcessing(targetItemId = id)
    }

    fun resetCompletedBatchItems() {
        if (_uiState.value.isBatchProcessing) return
        val resetList = batchProcessingUseCase.prepareReprocessAll(_uiState.value.batchItems)
        _uiState.value = _uiState.value.copy(
            batchItems = resetList,
            batchOverallProgress = 0f,
            batchSummaryReport = batchProcessingUseCase.computeSummary(resetList),
            statusMessage = "All items reset to Pending — ready to reprocess"
        )
    }

    fun runBatchProcessing(
        config: BatchConfig = _uiState.value.batchConfig,
        targetItemId: String? = null
    ) {
        val items = _uiState.value.batchItems
        if (items.isEmpty()) return

        batchProcessingJob?.cancel()
        val cancelToken = AtomicBoolean(false)
        batchCancelFlag = cancelToken
        val startMs = System.currentTimeMillis()

        batchProcessingJob = viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isBatchProcessing = true,
                batchConfig = config,
                batchOverallProgress = 0f,
                batchCurrentStage = "Initializing batch pipeline...",
                batchElapsedTimeMs = 0L
            )

            val outputConfig = _uiState.value.outputFileConfig
            val finalItems = batchProcessingUseCase.executeBatch(
                context = getApplication(),
                items = items,
                config = config,
                outputFileConfig = outputConfig,
                cancelFlag = cancelToken,
                targetItemId = targetItemId,
                onProgress = { updatedList, currentIndex, currentFileName, currentStage, overallProgress ->
                    if (!cancelToken.get()) {
                        val elapsed = (System.currentTimeMillis() - startMs).coerceAtLeast(0L)
                        _uiState.value = _uiState.value.copy(
                            batchItems = updatedList,
                            batchCurrentProcessingIndex = currentIndex,
                            batchCurrentFileName = currentFileName,
                            batchCurrentStage = currentStage,
                            batchOverallProgress = overallProgress,
                            batchElapsedTimeMs = elapsed,
                            batchSummaryReport = batchProcessingUseCase.computeSummary(updatedList)
                        )
                    }
                }
            )

            // Record newly completed items into Room history
            withContext(Dispatchers.IO) {
                finalItems.forEach { completedItem ->
                    if (completedItem.status == BatchStatus.COMPLETED && completedItem.outputUri != null) {
                        val wasAlreadyCompleted = items.any { it.id == completedItem.id && it.status == BatchStatus.COMPLETED }
                        if (!wasAlreadyCompleted) {
                            try {
                                val fmtStr = (completedItem.outputFormat ?: config.format).extension.uppercase()
                                val historyEntity = HistoryEntity(
                                    title = completedItem.outputFileName ?: completedItem.originalName,
                                    originalUriString = completedItem.uri.toString(),
                                    originalFileName = completedItem.originalName,
                                    originalWidth = completedItem.originalWidth,
                                    originalHeight = completedItem.originalHeight,
                                    originalFileSizeBytes = completedItem.originalSize,
                                    outputUriString = completedItem.outputUri.toString(),
                                    outputFileName = completedItem.outputFileName ?: completedItem.originalName,
                                    operationType = "Batch Process",
                                    operationDetails = "${completedItem.outputWidth ?: 0}×${completedItem.outputHeight ?: 0} px • $fmtStr • ${completedItem.metadataSummary ?: "${config.dpi} DPI"}",
                                    width = completedItem.outputWidth ?: completedItem.originalWidth,
                                    height = completedItem.outputHeight ?: completedItem.originalHeight,
                                    fileSizeBytes = completedItem.outputSize,
                                    format = fmtStr
                                )
                                repository.insertHistory(historyEntity)
                            } catch (_: Exception) {
                                // Ignore history error
                            }
                        }
                    }
                }
            }

            val summary = batchProcessingUseCase.computeSummary(finalItems)
            val totalElapsed = (System.currentTimeMillis() - startMs).coerceAtLeast(1L)
            val statusMsg = buildString {
                append("Batch complete: ${summary.completedItems} succeeded")
                if (summary.failedItems > 0) append(", ${summary.failedItems} failed")
                if (summary.cancelledItems > 0) append(", ${summary.cancelledItems} cancelled")
            }

            _uiState.value = _uiState.value.copy(
                batchItems = finalItems,
                isBatchProcessing = false,
                batchCurrentProcessingIndex = -1,
                batchCurrentFileName = "",
                batchCurrentStage = if (cancelToken.get()) "Cancelled" else "Completed",
                batchOverallProgress = 1f,
                batchElapsedTimeMs = totalElapsed,
                batchSummaryReport = summary.copy(totalProcessingTimeMs = totalElapsed),
                statusMessage = statusMsg
            )
        }
    }

    // History Actions & Metadata Storage (Request 37)
    fun recordOperationHistory(
        title: String,
        operationType: String,
        operationDetails: String,
        result: ExportEngine.ExportResult,
        sourceOriginalUri: Uri? = _uiState.value.currentImageUri,
        originalBmp: Bitmap? = _uiState.value.originalBitmap,
        outputBmp: Bitmap? = null,
        instructions: EditingInstructions? = null
    ) {
        if (!result.success || (result.outputUri == null && result.outputFile == null)) return
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val origThumb = originalBmp?.let { BitmapUtils.generateThumbnailBase64(it, 96) }
                val outThumb = (outputBmp ?: originalBmp)?.let { BitmapUtils.generateThumbnailBase64(it, 96) }
                val origFileName = _uiState.value.originalFileName.ifBlank {
                    sourceOriginalUri?.lastPathSegment?.substringAfterLast('/') ?: "source_image"
                }
                val outFileName = result.outputFile?.name ?: result.outputUri?.lastPathSegment?.substringAfterLast('/') ?: title
                val origW = _uiState.value.originalWidth.takeIf { it > 0 } ?: (originalBmp?.width ?: result.width)
                val origH = _uiState.value.originalHeight.takeIf { it > 0 } ?: (originalBmp?.height ?: result.height)
                val origBytes = _uiState.value.originalFileSizeBytes.takeIf { it > 0 } ?: (origW * origH * 3L / 2L)

                val settingsJson = instructions?.let { NonDestructivePipeline.serializeInstructions(it) }

                val item = HistoryEntity(
                    title = title,
                    originalUriString = sourceOriginalUri?.toString(),
                    originalFileName = origFileName,
                    originalWidth = origW,
                    originalHeight = origH,
                    originalFileSizeBytes = origBytes,
                    originalThumbnailBase64 = origThumb,
                    outputUriString = result.outputUri?.toString() ?: result.outputFile?.absolutePath ?: "",
                    outputFileName = outFileName,
                    operationType = operationType,
                    operationDetails = operationDetails,
                    operationSettingsJson = settingsJson,
                    width = result.width,
                    height = result.height,
                    fileSizeBytes = result.fileSizeBytes,
                    format = result.format.extension.uppercase(),
                    timestamp = System.currentTimeMillis(),
                    isFavorite = false,
                    previewThumbnailBase64 = outThumb
                )
                repository.insertHistory(item)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun reuseHistorySettings(item: HistoryEntity, onApplied: () -> Unit = {}) {
        viewModelScope.launch {
            val targetW = if (item.width > 0) item.width else 1080
            val targetH = if (item.height > 0) item.height else 1080
            val format = try {
                ExportFormat.values().firstOrNull {
                    it.extension.equals(item.format, true) || it.name.equals(item.format, true)
                } ?: ExportFormat.JPEG
            } catch (e: Exception) {
                ExportFormat.JPEG
            }

            val instructions = item.operationSettingsJson?.let {
                NonDestructivePipeline.deserializeInstructions(it)
            }

            if (instructions != null) {
                applyInstructionsToState(instructions)
                _uiState.value = _uiState.value.copy(
                    targetWidthPx = targetW,
                    targetHeightPx = targetH,
                    exportFormat = format,
                    saveAsFormat = format,
                    statusMessage = "Loaded settings: ${item.operationType} (${targetW}×${targetH} px, ${format.extension.uppercase()})"
                )
                renderPipeline(instructions)
            } else {
                _uiState.value = _uiState.value.copy(
                    targetWidthPx = targetW,
                    targetHeightPx = targetH,
                    calcDimensionWidth = targetW,
                    calcDimensionHeight = targetH,
                    calcDimensionWidthInput = targetW.toString(),
                    calcDimensionHeightInput = targetH.toString(),
                    exportFormat = format,
                    saveAsFormat = format,
                    statusMessage = "Applied settings from history: ${targetW}×${targetH} px • ${format.extension.uppercase()}"
                )
                calculateEstimatedSize()
                updatePreview()
            }
            onApplied()
        }
    }

    fun openHistoryImageInEditor(item: HistoryEntity, onDone: () -> Unit = {}) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, statusMessage = "Loading image into editor...")
            val uriStr = item.outputUriString.ifBlank { item.originalUriString }
            if (!uriStr.isNullOrBlank()) {
                val uri = if (uriStr.startsWith("content://") || uriStr.startsWith("file://")) {
                    Uri.parse(uriStr)
                } else {
                    Uri.fromFile(java.io.File(uriStr))
                }
                try {
                    loadSourceImage(uri)
                    _uiState.value = _uiState.value.copy(
                        statusMessage = "Opened '${item.outputFileName.ifBlank { item.title }}' in Editor"
                    )
                    onDone()
                    return@launch
                } catch (e: Exception) {
                    // Fallback to thumbnail or failure
                }
            }
            val thumb = item.previewThumbnailBase64?.let { BitmapUtils.decodeThumbnailFromBase64(it) }
            if (thumb != null) {
                setWorkingBitmapDirectly(thumb, item.title)
                _uiState.value = _uiState.value.copy(
                    statusMessage = "Loaded preview for '${item.title}' in Editor"
                )
                onDone()
            } else {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    statusMessage = "Image file no longer accessible at saved path"
                )
            }
        }
    }

    fun toggleFavorite(history: HistoryEntity) {
        viewModelScope.launch {
            fileManagementUseCase.toggleHistoryFavorite(repository, history)
        }
    }

    fun renameHistoryItem(
        history: HistoryEntity,
        newName: String,
        onDone: (FileManagementUseCase.HistoryRenameOutcome) -> Unit = {}
    ) {
        viewModelScope.launch {
            val outcome = withContext(Dispatchers.IO) {
                fileManagementUseCase.renameHistoryEntry(
                    context = getApplication(),
                    repository = repository,
                    item = history,
                    newRawName = newName
                )
            }
            _uiState.value = _uiState.value.copy(statusMessage = outcome.message)
            onDone(outcome)
        }
    }

    fun deleteHistory(
        history: HistoryEntity,
        deletePhysicalFile: Boolean = false,
        onDone: (FileManagementUseCase.HistoryDeleteOutcome) -> Unit = {}
    ) {
        viewModelScope.launch {
            val outcome = withContext(Dispatchers.IO) {
                fileManagementUseCase.deleteHistoryEntry(
                    context = getApplication(),
                    repository = repository,
                    item = history,
                    deletePhysicalFile = deletePhysicalFile
                )
            }
            _uiState.value = _uiState.value.copy(statusMessage = outcome.message)
            onDone(outcome)
        }
    }

    fun clearAllHistory() {
        viewModelScope.launch {
            repository.clearAllHistory()
            _uiState.value = _uiState.value.copy(statusMessage = "Cleared all operation history")
        }
    }

    // ==========================================
    // FAVORITES MANAGEMENT (Request 38 & Phase 11)
    // ==========================================

    fun toggleFavoriteItem(item: FavoriteItemEntity) {
        viewModelScope.launch {
            repository.deleteFavorite(item)
        }
    }

    fun renameFavoriteItem(item: FavoriteItemEntity, newTitle: String) {
        viewModelScope.launch {
            val updated = fileManagementUseCase.renameFavoriteItem(repository, item, newTitle)
            _uiState.value = _uiState.value.copy(
                statusMessage = "Renamed favorite to '${updated.title}'"
            )
        }
    }

    fun renameCustomPreset(preset: PresetEntity, newName: String) {
        viewModelScope.launch {
            val updated = fileManagementUseCase.renameCustomPreset(repository, preset, newName)
            _uiState.value = _uiState.value.copy(
                statusMessage = "Renamed preset to '${updated.name}'"
            )
        }
    }

    fun renameCurrentWorkingFile(
        newName: String,
        onDone: (OutputFileManager.FileRenameResult?) -> Unit = {}
    ) {
        val sanitization = OutputFileManager.sanitizeFilename(
            newName.substringBeforeLast('.'),
            fallback = "image_export"
        )
        val cleanBase = sanitization.sanitizedName
        val ext = _uiState.value.exportFormat.extension
        val fullDisplayName = "$cleanBase.$ext"

        viewModelScope.launch {
            var renameResult: OutputFileManager.FileRenameResult? = null
            val lastExportFile = _uiState.value.lastExportResult?.outputFile
            if (lastExportFile != null && lastExportFile.exists()) {
                renameResult = withContext(Dispatchers.IO) {
                    fileManagementUseCase.renameFile(
                        context = getApplication(),
                        targetPathOrUri = lastExportFile.absolutePath,
                        newRawName = cleanBase,
                        fallbackExtension = ext
                    )
                }
            }

            val updatedLastExport = if (renameResult?.success == true && renameResult.newFile != null) {
                _uiState.value.lastExportResult?.copy(
                    outputFile = renameResult.newFile,
                    outputUri = renameResult.newUri
                )
            } else {
                _uiState.value.lastExportResult
            }

            _uiState.value = _uiState.value.copy(
                originalFileName = fullDisplayName,
                saveAsFileName = cleanBase,
                filenameSanitizationResult = sanitization,
                outputFileConfig = _uiState.value.outputFileConfig.copy(
                    namingMode = NamingMode.EXACT_CUSTOM,
                    customExactName = cleanBase
                ),
                lastExportResult = updatedLastExport,
                statusMessage = "Renamed active file to '$fullDisplayName'"
            )
            onDone(renameResult)
        }
    }

    fun deleteCurrentOrLastExportedFile(
        deleteExportedFile: Boolean = true,
        clearWorkspace: Boolean = false,
        onDone: (String) -> Unit = {}
    ) {
        viewModelScope.launch {
            var deletedFileMsg: String? = null
            val lastFile = _uiState.value.lastExportResult?.outputFile
            val lastUri = _uiState.value.lastExportResult?.outputUri
            if (deleteExportedFile) {
                val target = lastFile?.absolutePath ?: lastUri?.toString() ?: ""
                if (target.isNotBlank()) {
                    val delRes = withContext(Dispatchers.IO) {
                        fileManagementUseCase.deleteFile(getApplication(), target)
                    }
                    if (delRes.success) {
                        deletedFileMsg = "Deleted exported file (${delRes.deletedTarget.substringAfterLast('/')})"
                    }
                }
            }
            if (clearWorkspace) {
                _uiState.value = _uiState.value.copy(
                    workingBitmap = _uiState.value.originalBitmap,
                    previewBitmap = _uiState.value.originalBitmap,
                    lastExportResult = null,
                    lastVerificationReport = null,
                    currentInstructions = EditingInstructions(),
                    globalUndoStack = emptyList(),
                    globalRedoStack = emptyList(),
                    statusMessage = deletedFileMsg ?: "Workspace edits cleared"
                )
            } else if (deletedFileMsg != null) {
                _uiState.value = _uiState.value.copy(
                    lastExportResult = null,
                    statusMessage = deletedFileMsg
                )
            }
            val finalMsg = deletedFileMsg ?: if (clearWorkspace) "Workspace reset" else "Nothing to delete"
            onDone(finalMsg)
        }
    }

    fun deleteFavorite(item: FavoriteItemEntity) {
        viewModelScope.launch {
            repository.deleteFavorite(item)
        }
    }

    fun clearFavoritesByType(type: FavoriteType) {
        viewModelScope.launch {
            repository.clearFavoritesByType(type)
        }
    }

    fun clearAllFavorites() {
        viewModelScope.launch {
            repository.clearAllFavorites()
        }
    }

    fun addFavoriteImage(title: String, uri: Uri?, bitmap: Bitmap?) {
        if (uri == null && bitmap == null) return
        viewModelScope.launch {
            val uriStr = uri?.toString() ?: ""
            val dataKey = "IMG_${uriStr.hashCode()}_${System.currentTimeMillis()}"
            val thumbBase64 = bitmap?.let { BitmapUtils.generateThumbnailBase64(it, 120) }
            val w = bitmap?.width ?: 0
            val h = bitmap?.height ?: 0
            val displayTitle = if (title.isNotBlank()) title else "Favorite Image (${w}×${h})"
            val fav = FavoriteItemEntity(
                type = FavoriteType.IMAGE,
                title = displayTitle,
                subtitle = "${w}×${h} px",
                dataKey = dataKey,
                width = w,
                height = h,
                imageUriString = uriStr,
                thumbnailBase64 = thumbBase64
            )
            repository.insertFavorite(fav)
            _uiState.value = _uiState.value.copy(statusMessage = "Added '$displayTitle' to Favorites ⭐")
        }
    }

    fun toggleFavoriteTool(tab: StudioTab) {
        viewModelScope.launch {
            val key = tab.name
            val existing = repository.getFavoriteByTypeAndKey(FavoriteType.RECENT_TOOL, key)
            if (existing != null) {
                repository.deleteFavorite(existing)
                _uiState.value = _uiState.value.copy(statusMessage = "Removed '${tab.label}' from favorite tools")
            } else {
                val fav = FavoriteItemEntity(
                    type = FavoriteType.RECENT_TOOL,
                    title = tab.label,
                    subtitle = "Tool Shortcut",
                    dataKey = key
                )
                repository.insertFavorite(fav)
                _uiState.value = _uiState.value.copy(statusMessage = "Pinned '${tab.label}' to favorite tools ⭐")
            }
        }
    }

    fun toggleFavoriteDimension(title: String, width: Int, height: Int, unit: String = "PX", dpi: Int = 300) {
        viewModelScope.launch {
            val key = "DIM_${width}_${height}_${unit}"
            val existing = repository.getFavoriteByTypeAndKey(FavoriteType.CUSTOM_DIMENSION, key)
            if (existing != null) {
                repository.deleteFavorite(existing)
                _uiState.value = _uiState.value.copy(statusMessage = "Removed dimension from Favorites")
            } else {
                val displayTitle = if (title.isNotBlank()) title else "$width × $height $unit"
                val fav = FavoriteItemEntity(
                    type = FavoriteType.CUSTOM_DIMENSION,
                    title = displayTitle,
                    subtitle = "$width × $height $unit • ${dpi} DPI",
                    dataKey = key,
                    width = width,
                    height = height,
                    unit = unit,
                    dpi = dpi
                )
                repository.insertFavorite(fav)
                _uiState.value = _uiState.value.copy(statusMessage = "Saved '$displayTitle' as Favorite Dimension ⭐")
            }
        }
    }

    fun toggleFavoriteCompressionTarget(title: String, targetKb: Int, quality: Int = 90, format: String = "JPEG") {
        viewModelScope.launch {
            val key = "COMP_${targetKb}KB_${format}"
            val existing = repository.getFavoriteByTypeAndKey(FavoriteType.COMPRESSION_TARGET, key)
            if (existing != null) {
                repository.deleteFavorite(existing)
                _uiState.value = _uiState.value.copy(statusMessage = "Removed compression target from Favorites")
            } else {
                val displayTitle = if (title.isNotBlank()) title else "Target ≤ $targetKb KB"
                val fav = FavoriteItemEntity(
                    type = FavoriteType.COMPRESSION_TARGET,
                    title = displayTitle,
                    subtitle = "Target: $targetKb KB • $quality% Quality • $format",
                    dataKey = key,
                    targetFileSizeKb = targetKb,
                    quality = quality,
                    format = format
                )
                repository.insertFavorite(fav)
                _uiState.value = _uiState.value.copy(statusMessage = "Saved '$displayTitle' as Favorite Target ⭐")
            }
        }
    }

    fun toggleFavoritePreset(presetName: String, category: String, width: Int, height: Int, dpi: Int = 300, format: String = "JPEG") {
        viewModelScope.launch {
            val key = "PRESET_${presetName.replace(" ", "_").uppercase()}"
            val existing = repository.getFavoriteByTypeAndKey(FavoriteType.PRESET, key)
            if (existing != null) {
                repository.deleteFavorite(existing)
                _uiState.value = _uiState.value.copy(statusMessage = "Removed '$presetName' from Favorites")
            } else {
                val fav = FavoriteItemEntity(
                    type = FavoriteType.PRESET,
                    title = presetName,
                    subtitle = "$category • ${width}×${height} • $format",
                    dataKey = key,
                    width = width,
                    height = height,
                    dpi = dpi,
                    format = format
                )
                repository.insertFavorite(fav)
                _uiState.value = _uiState.value.copy(statusMessage = "Saved '$presetName' to Favorites ⭐")
            }
        }
    }

    fun applyFavoriteItem(item: FavoriteItemEntity, onNavigateToEditorTab: ((StudioTab) -> Unit)? = null) {
        viewModelScope.launch {
            fileManagementUseCase.recordFavoriteUsage(repository, item)
        }
        when (item.type) {
            FavoriteType.IMAGE -> {
                if (!item.imageUriString.isNullOrBlank()) {
                    loadSourceImage(Uri.parse(item.imageUriString))
                    onNavigateToEditorTab?.invoke(StudioTab.CROP)
                } else if (!item.thumbnailBase64.isNullOrBlank()) {
                    val thumb = BitmapUtils.decodeThumbnailFromBase64(item.thumbnailBase64)
                    if (thumb != null) {
                        setWorkingBitmapDirectly(thumb, item.title)
                        onNavigateToEditorTab?.invoke(StudioTab.CROP)
                    }
                }
            }
            FavoriteType.PRESET -> {
                if (item.width != null && item.height != null) {
                    setTargetDimensions(item.width, item.height)
                    item.dpi?.let { d -> _uiState.value = _uiState.value.copy(dpi = d) }
                    item.format?.let { fmtStr ->
                        ExportFormat.values().find { it.name.equals(fmtStr, ignoreCase = true) || it.extension.equals(fmtStr, ignoreCase = true) }?.let { fmt ->
                            setFormat(fmt)
                        }
                    }
                    _uiState.value = _uiState.value.copy(statusMessage = "Applied preset '${item.title}' (${item.width}×${item.height})")
                    onNavigateToEditorTab?.invoke(StudioTab.EXACT_SIZE)
                }
            }
            FavoriteType.RECENT_TOOL -> {
                val tab = try {
                    StudioTab.valueOf(item.dataKey)
                } catch (e: Exception) {
                    StudioTab.CROP
                }
                selectTab(tab)
                onNavigateToEditorTab?.invoke(tab)
            }
            FavoriteType.CUSTOM_DIMENSION -> {
                if (item.width != null && item.height != null) {
                    setExactWidthInput(item.width.toString())
                    setExactHeightInput(item.height.toString())
                    setTargetDimensions(item.width, item.height)
                    item.dpi?.let { d -> _uiState.value = _uiState.value.copy(dpi = d) }
                    item.unit?.let { u ->
                        ExactSizeUnit.values().find { it.name.equals(u, ignoreCase = true) }?.let { unit ->
                            setExactSizeUnit(unit)
                        }
                    }
                    _uiState.value = _uiState.value.copy(statusMessage = "Applied custom dimension '${item.title}'")
                    onNavigateToEditorTab?.invoke(StudioTab.EXACT_SIZE)
                }
            }
            FavoriteType.COMPRESSION_TARGET -> {
                if (item.targetFileSizeKb != null) {
                    setCustomCompressionMode(CustomCompressionMode.MAX_CEILING)
                    setTargetFileSizeKb(item.targetFileSizeKb)
                    item.quality?.let { setQuality(it) }
                    item.format?.let { fmtStr ->
                        ExportFormat.values().find { it.name.equals(fmtStr, ignoreCase = true) || it.extension.equals(fmtStr, ignoreCase = true) }?.let { fmt ->
                            setFormat(fmt)
                        }
                    }
                    _uiState.value = _uiState.value.copy(statusMessage = "Applied compression target: ≤ ${item.targetFileSizeKb} KB")
                    onNavigateToEditorTab?.invoke(StudioTab.COMPRESS)
                }
            }
        }
    }


    // ==========================================
    // BACKGROUND TOOLS (Request 27)
    // ==========================================

    private var bgProcessingJob: Job? = null

    fun setBackgroundMode(mode: BackgroundProcessor.BgMode) {
        _uiState.value = _uiState.value.copy(
            bgConfig = _uiState.value.bgConfig.copy(mode = mode)
        )
        // If switching to transparent, suggest PNG or WebP format for export
        if (mode == BackgroundProcessor.BgMode.TRANSPARENT && _uiState.value.exportFormat == ExportFormat.JPEG) {
            _uiState.value = _uiState.value.copy(exportFormat = ExportFormat.PNG)
        }
        triggerBackgroundProcessing()
    }

    fun setBackgroundSolidColor(colorInt: Int, hexString: String) {
        _uiState.value = _uiState.value.copy(
            bgConfig = _uiState.value.bgConfig.copy(
                mode = BackgroundProcessor.BgMode.SOLID_COLOR,
                solidColor = colorInt,
                customColorHex = hexString
            )
        )
        triggerBackgroundProcessing()
    }

    fun setBackgroundGradient(preset: BackgroundProcessor.GradientPreset) {
        _uiState.value = _uiState.value.copy(
            bgConfig = _uiState.value.bgConfig.copy(
                mode = BackgroundProcessor.BgMode.GRADIENT,
                gradientPreset = preset
            )
        )
        triggerBackgroundProcessing()
    }

    fun setBackgroundBlurRadius(radius: Int) {
        _uiState.value = _uiState.value.copy(
            bgConfig = _uiState.value.bgConfig.copy(blurRadius = radius)
        )
        triggerBackgroundProcessing()
    }

    fun setCustomBgImage(bitmap: Bitmap) {
        _uiState.value = _uiState.value.copy(
            customBgImage = bitmap,
            bgConfig = _uiState.value.bgConfig.copy(mode = BackgroundProcessor.BgMode.CUSTOM_IMAGE)
        )
        triggerBackgroundProcessing()
    }

    fun setBackgroundTolerance(tolerance: Float) {
        _uiState.value = _uiState.value.copy(
            bgConfig = _uiState.value.bgConfig.copy(tolerance = tolerance)
        )
        triggerBackgroundProcessing()
    }

    fun setBackgroundFeatherRadius(feather: Float) {
        _uiState.value = _uiState.value.copy(
            bgConfig = _uiState.value.bgConfig.copy(featherRadius = feather)
        )
        triggerBackgroundProcessing()
    }

    fun setBackgroundEdgeSmoothness(smoothness: Float) {
        _uiState.value = _uiState.value.copy(
            bgConfig = _uiState.value.bgConfig.copy(edgeSmoothness = smoothness)
        )
        triggerBackgroundProcessing()
    }

    fun toggleBackgroundNoiseCleanup() {
        _uiState.value = _uiState.value.copy(
            bgConfig = _uiState.value.bgConfig.copy(cleanupNoise = !_uiState.value.bgConfig.cleanupNoise)
        )
        triggerBackgroundProcessing()
    }

    fun toggleBackgroundHaloDecontaminate() {
        _uiState.value = _uiState.value.copy(
            bgConfig = _uiState.value.bgConfig.copy(decontaminateHalo = !_uiState.value.bgConfig.decontaminateHalo)
        )
        triggerBackgroundProcessing()
    }

    fun setBackgroundRemovalMethod(method: BackgroundProcessor.RemovalMethod) {
        _uiState.value = _uiState.value.copy(
            bgConfig = _uiState.value.bgConfig.copy(removalMethod = method)
        )
        triggerBackgroundProcessing()
    }

    fun resetBackgroundConfig() {
        _uiState.value = _uiState.value.copy(
            bgConfig = BackgroundProcessor.BackgroundConfig(),
            customBgImage = null,
            previewBitmap = _uiState.value.workingBitmap
        )
        triggerBackgroundProcessing()
    }

    private fun triggerBackgroundProcessing() {
        bgProcessingJob?.cancel()
        val current = _uiState.value.workingBitmap ?: return
        val config = _uiState.value.bgConfig
        val customBg = _uiState.value.customBgImage

        bgProcessingJob = viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isProcessingBg = true)
            val res = withContext(Dispatchers.Default) {
                advancedEditingUseCase.executeBackgroundProcessing(
                    source = current,
                    config = config,
                    customBg = customBg
                )
            }
            _uiState.value = _uiState.value.copy(
                previewBitmap = res.bitmap,
                lastBackgroundReport = res.verification,
                isProcessingBg = false
            )
            calculateEstimatedSize()
        }
    }

    fun applyBackgroundPermanently() {
        val current = _uiState.value.workingBitmap ?: return
        val processed = _uiState.value.previewBitmap ?: return
        val config = _uiState.value.bgConfig
        recordHistoryState("Background")
        val verification = BackgroundProcessor.verifyBackgroundOutput(
            before = current,
            after = processed,
            mode = config.mode,
            tolerance = config.tolerance
        )
        _uiState.value = _uiState.value.copy(
            workingBitmap = processed,
            previewBitmap = processed,
            lastBackgroundReport = verification,
            statusMessage = verification.summary
        )
        calculateEstimatedSize()
    }

    // ==========================================
    // PHASE 10 — CREATIVE & DIAGNOSTIC TOOLS
    // ==========================================

    fun setWatermarkText(text: String) {
        _uiState.value = _uiState.value.copy(watermarkText = text)
    }

    fun setWatermarkOpacity(opacity: Int) {
        _uiState.value = _uiState.value.copy(watermarkOpacity = opacity.coerceIn(5, 100))
    }

    fun setWatermarkTiled(tiled: Boolean) {
        _uiState.value = _uiState.value.copy(watermarkTiled = tiled)
    }

    fun applyWatermarkOverlay() {
        val current = _uiState.value.workingBitmap ?: return
        val text = _uiState.value.watermarkText.ifBlank { "© Photo Studio" }
        recordHistoryState("Watermark")
        viewModelScope.launch {
            val watermarked = withContext(Dispatchers.Default) {
                advancedEditingUseCase.executeWatermark(
                    source = current,
                    config = AdvancedCreativeEngine.WatermarkConfig(
                        text = text,
                        opacity = (_uiState.value.watermarkOpacity / 100f).coerceIn(0.05f, 1f),
                        position = if (_uiState.value.watermarkTiled) {
                            AdvancedCreativeEngine.WatermarkPosition.TILED
                        } else {
                            AdvancedCreativeEngine.WatermarkPosition.BOTTOM_RIGHT
                        }
                    )
                )
            }
            _uiState.value = _uiState.value.copy(
                workingBitmap = watermarked,
                previewBitmap = watermarked,
                statusMessage = "Applied watermark '$text'"
            )
            calculateEstimatedSize()
        }
    }

    fun setFramePaddingPercent(padding: Float) {
        _uiState.value = _uiState.value.copy(framePaddingPercent = padding.coerceIn(0f, 25f))
    }

    fun setFrameDropShadow(enabled: Boolean) {
        _uiState.value = _uiState.value.copy(frameDropShadow = enabled)
    }

    fun applyDecorativeFrame() {
        val current = _uiState.value.workingBitmap ?: return
        recordHistoryState("Decorative Frame")
        viewModelScope.launch {
            val framed = withContext(Dispatchers.Default) {
                advancedEditingUseCase.executeFrameStyling(
                    source = current,
                    config = AdvancedCreativeEngine.FrameConfig(
                        borderWidthPx = _uiState.value.frameBorderWidthPx.roundToInt().coerceAtLeast(12),
                        borderColor = Color.WHITE,
                        cornerRadiusPx = 20,
                        addDropShadow = _uiState.value.frameDropShadow
                    )
                )
            }
            _uiState.value = _uiState.value.copy(
                workingBitmap = framed,
                previewBitmap = framed,
                targetWidthPx = framed.width,
                targetHeightPx = framed.height,
                statusMessage = "Applied decorative frame (${framed.width}×${framed.height} px)"
            )
            calculateEstimatedSize()
        }
    }

    fun applyPrivacyPixelateCenter() {
        val current = _uiState.value.workingBitmap ?: return
        recordHistoryState("Privacy Redact")
        viewModelScope.launch {
            val redacted = withContext(Dispatchers.Default) {
                advancedEditingUseCase.executeRegionRedaction(
                    source = current,
                    regions = listOf(
                        AdvancedCreativeEngine.PrivacyRegion(
                            normalizedRect = RectF(0.25f, 0.30f, 0.75f, 0.70f),
                            mode = AdvancedCreativeEngine.RedactionMode.PIXELATE
                        )
                    )
                )
            }
            _uiState.value = _uiState.value.copy(
                workingBitmap = redacted,
                previewBitmap = redacted,
                statusMessage = "Applied privacy pixelation mask"
            )
            calculateEstimatedSize()
        }
    }

    fun runImageDiagnosticsAndPalette() {
        val current = _uiState.value.workingBitmap ?: return
        viewModelScope.launch {
            val palette = withContext(Dispatchers.Default) {
                advancedEditingUseCase.extractColorPalette(current, 6)
            }
            val quality = withContext(Dispatchers.Default) {
                advancedEditingUseCase.analyzeQuality(current)
            }
            val dHash = withContext(Dispatchers.Default) {
                val hash = advancedEditingUseCase.computePerceptualHash(current)
                java.lang.Long.toUnsignedString(hash, 16).uppercase().padStart(16, '0')
            }
            _uiState.value = _uiState.value.copy(
                colorPaletteSwatches = palette,
                qualityReport = quality,
                perceptualHashHex = dHash,
                statusMessage = "Analyzed: ${quality.sharpnessLabel} (Score ${quality.sharpnessScore.roundToInt()}) • dHash $dHash"
            )
        }
    }

    private fun calculateEstimatedSize() {
        sizeCalculationJob?.cancel()
        val current = _uiState.value.workingBitmap ?: return
        val targetW = _uiState.value.targetWidthPx
        val targetH = _uiState.value.targetHeightPx
        val format = _uiState.value.exportFormat
        val quality = _uiState.value.quality
        val targetKb = _uiState.value.targetSizeKb
        val mode = _uiState.value.fileSizeMode
        val allowDownscale = _uiState.value.allowDimensionDownscaling

        sizeCalculationJob = viewModelScope.launch(Dispatchers.Default) {
            val optResult = ExportEngine.measureActualCompressedBytes(
                source = current,
                targetW = targetW,
                targetH = targetH,
                format = format,
                quality = quality,
                targetSizeKb = targetKb,
                fileSizeMode = mode,
                allowDimensionAdjustment = allowDownscale,
                jpegBackgroundColor = _uiState.value.jpegBackgroundColor
            )

            val compReport = CompressionEngine.verifyCompressedBytes(
                bytes = optResult.encodedBytes,
                format = format,
                quality = optResult.quality,
                requestedTargetBytes = targetKb?.let { it * 1024L },
                requestedMaxBytes = if (mode == FileSizeMode.MAXIMUM_CEILING && targetKb != null) targetKb * 1024L else null,
                sourceWidth = current.width,
                sourceHeight = current.height,
                iterationsCount = optResult.iterationsCount,
                dimensionsAdjusted = optResult.dimensionsAdjusted
            )

            _uiState.value = _uiState.value.copy(
                estimatedBytes = optResult.actualBytes,
                fileSizeOptimizationResult = optResult,
                lastCompressionReport = compReport
            )
        }
    }

    fun applyQualityCompression(quality: Int) {
        val current = _uiState.value.workingBitmap ?: return
        val res = compressImageUseCase.compressByQuality(
            source = current,
            format = _uiState.value.exportFormat,
            quality = quality,
            backgroundColor = _uiState.value.jpegBackgroundColor
        )
        _uiState.value = _uiState.value.copy(
            quality = quality,
            lastCompressionReport = res.verificationReport,
            statusMessage = "Compressed to Quality $quality% (${res.actualBytes / 1024} KB)"
        )
    }

    fun applyTargetFileSize(targetKb: Int, exactByteAlignment: Boolean = false) {
        val current = _uiState.value.workingBitmap ?: return
        val res = compressImageUseCase.compressToTargetSize(
            source = current,
            format = _uiState.value.exportFormat,
            targetKb = targetKb,
            allowDimensionAdjustment = _uiState.value.allowDimensionDownscaling,
            exactByteAlignment = exactByteAlignment,
            backgroundColor = _uiState.value.jpegBackgroundColor
        )
        _uiState.value = _uiState.value.copy(
            targetSizeKb = targetKb,
            fileSizeMode = if (exactByteAlignment) FileSizeMode.EXACT_BYTE_ALIGNMENT else FileSizeMode.TARGET_CLOSEST,
            lastCompressionReport = res.verificationReport,
            statusMessage = "Optimized for target $targetKb KB (Actual: ${res.actualBytes / 1024} KB)"
        )
    }

    fun applyMaxFileSizeCeiling(maxKb: Int) {
        val current = _uiState.value.workingBitmap ?: return
        val res = compressImageUseCase.compressToMaximumCeiling(
            source = current,
            format = _uiState.value.exportFormat,
            maxKb = maxKb,
            allowDimensionAdjustment = _uiState.value.allowDimensionDownscaling,
            backgroundColor = _uiState.value.jpegBackgroundColor
        )
        _uiState.value = _uiState.value.copy(
            targetSizeKb = maxKb,
            fileSizeMode = FileSizeMode.MAXIMUM_CEILING,
            lastCompressionReport = res.verificationReport,
            statusMessage = "Enforced maximum ceiling ≤ $maxKb KB (Actual: ${res.actualBytes / 1024} KB)"
        )
    }

    // Settings Management (Request 48)
    fun updateAppSettings(updater: (AppSettings) -> AppSettings) {
        val newSettings = updater(_uiState.value.appSettings)
        _uiState.value = _uiState.value.copy(
            appSettings = newSettings,
            exportFormat = newSettings.defaultExportFormat,
            quality = newSettings.defaultQuality,
            showGrid = newSettings.showGridByDefault,
            showHorizonGuide = newSettings.showGuidesByDefault,
            keepAspectRatio = newSettings.defaultAspectRatioLock,
            exactAspectLocked = newSettings.defaultAspectRatioLock,
            metadataPrivacyConfig = _uiState.value.metadataPrivacyConfig.copy(policy = newSettings.defaultMetadataPolicy)
        )
    }

    fun setThemeMode(mode: AppThemeMode) {
        updateAppSettings { it.copy(themeMode = mode) }
    }

    // ==========================================
    // PHASE 12 — SECURITY + PRIVACY AUDIT & CONTROLS
    // ==========================================

    fun runSecurityPrivacyAudit() {
        viewModelScope.launch {
            val settings = _uiState.value.appSettings
            val overwriteMode = _uiState.value.outputFileConfig.overwriteAction
            val report = withContext(Dispatchers.IO) {
                securityPrivacyUseCase.runCompleteAudit(
                    context = getApplication(),
                    defaultMetadataPolicy = settings.defaultMetadataPolicy,
                    defaultOverwriteAction = overwriteMode
                )
            }
            _uiState.value = _uiState.value.copy(
                securityAuditReport = report,
                statusMessage = "Security & Privacy Audit: ${report.overallScore}% (${report.overallStatus.name})"
            )
        }
    }

    fun setScrubMetadataBeforeShare(enabled: Boolean) {
        _uiState.value = _uiState.value.copy(
            scrubMetadataBeforeShare = enabled,
            statusMessage = if (enabled) {
                "Enabled automatic EXIF/GPS scrubbing before sharing"
            } else {
                "Share files with current export metadata"
            }
        )
    }

    fun purgeTemporaryCache(zeroFill: Boolean = true) {
        viewModelScope.launch {
            val purgeRes = withContext(Dispatchers.IO) {
                securityPrivacyUseCase.purgeTemporaryCache(
                    context = getApplication(),
                    zeroFill = zeroFill,
                    olderThanMs = 0L
                )
            }
            val updatedReport = withContext(Dispatchers.IO) {
                securityPrivacyUseCase.runCompleteAudit(
                    context = getApplication(),
                    defaultMetadataPolicy = _uiState.value.appSettings.defaultMetadataPolicy,
                    defaultOverwriteAction = _uiState.value.outputFileConfig.overwriteAction
                )
            }
            val kbFreed = purgeRes.freedBytes / 1024
            _uiState.value = _uiState.value.copy(
                securityAuditReport = updatedReport,
                statusMessage = "Securely wiped ${purgeRes.deletedFileCount} temporary file(s) ($kbFreed KB freed)"
            )
        }
    }

    fun performFullPrivacyReset() {
        viewModelScope.launch {
            val purgeRes = withContext(Dispatchers.IO) {
                securityPrivacyUseCase.performFullPrivacyReset(
                    context = getApplication(),
                    repository = repository,
                    clearHistory = true,
                    clearFavorites = true,
                    purgeTempCache = true
                )
            }
            val updatedReport = withContext(Dispatchers.IO) {
                securityPrivacyUseCase.runCompleteAudit(
                    context = getApplication(),
                    defaultMetadataPolicy = _uiState.value.appSettings.defaultMetadataPolicy,
                    defaultOverwriteAction = _uiState.value.outputFileConfig.overwriteAction
                )
            }
            _uiState.value = _uiState.value.copy(
                securityAuditReport = updatedReport,
                statusMessage = "Privacy reset complete: Cleared history, favorites & wiped ${purgeRes.deletedFileCount} temp file(s)"
            )
        }
    }
}

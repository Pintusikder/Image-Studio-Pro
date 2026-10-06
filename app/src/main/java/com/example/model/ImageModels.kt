package com.example.model

import android.net.Uri

enum class ExportFormat(val extension: String, val mimeType: String, val displayName: String) {
    JPEG("jpg", "image/jpeg", "JPEG (.jpg)"),
    PNG("png", "image/png", "PNG (.png)"),
    WEBP_LOSSY("webp", "image/webp", "WEBP Lossy (.webp)"),
    WEBP_LOSSLESS("webp", "image/webp", "WEBP Lossless (.webp)"),
    PDF("pdf", "application/pdf", "PDF Document (.pdf)")
}

enum class DetectedImageFormat(
    val displayName: String,
    val shortName: String,
    val extension: String,
    val mimeType: String,
    val supportsAlpha: Boolean
) {
    JPEG("JPEG Image", "JPG", "jpg", "image/jpeg", false),
    PNG("PNG Image", "PNG", "png", "image/png", true),
    WEBP("WebP Image", "WEBP", "webp", "image/webp", true),
    HEIC("HEIF / HEIC Image", "HEIC", "heic", "image/heic", false),
    BMP("BMP Bitmap", "BMP", "bmp", "image/bmp", false),
    GIF("GIF Image", "GIF", "gif", "image/gif", true),
    UNKNOWN("Unknown Format", "IMG", "jpg", "image/jpeg", false)
}

enum class FormatConversionPair(
    val fromFormat: DetectedImageFormat,
    val toFormat: ExportFormat,
    val label: String,
    val description: String,
    val requiresBackgroundForAlpha: Boolean = false
) {
    JPG_TO_PNG(DetectedImageFormat.JPEG, ExportFormat.PNG, "JPG → PNG", "Convert JPG to lossless PNG"),
    JPG_TO_WEBP(DetectedImageFormat.JPEG, ExportFormat.WEBP_LOSSY, "JPG → WebP", "Convert JPG to compact WebP lossy"),
    JPG_TO_PDF(DetectedImageFormat.JPEG, ExportFormat.PDF, "JPG → PDF", "Convert JPG to PDF document"),
    PNG_TO_JPG(DetectedImageFormat.PNG, ExportFormat.JPEG, "PNG → JPG", "Convert PNG to JPG (with background matte)", true),
    PNG_TO_WEBP(DetectedImageFormat.PNG, ExportFormat.WEBP_LOSSY, "PNG → WebP", "Convert PNG to lightweight WebP"),
    PNG_TO_PDF(DetectedImageFormat.PNG, ExportFormat.PDF, "PNG → PDF", "Convert PNG to PDF document"),
    WEBP_TO_JPG(DetectedImageFormat.WEBP, ExportFormat.JPEG, "WebP → JPG", "Convert WebP to universal JPG", true),
    WEBP_TO_PNG(DetectedImageFormat.WEBP, ExportFormat.PNG, "WebP → PNG", "Convert WebP to lossless PNG"),
    WEBP_TO_PDF(DetectedImageFormat.WEBP, ExportFormat.PDF, "WebP → PDF", "Convert WebP to PDF document"),
    HEIC_TO_JPG(DetectedImageFormat.HEIC, ExportFormat.JPEG, "HEIC → JPG", "Convert Apple HEIC/HEIF to universal JPG"),
    HEIC_TO_PNG(DetectedImageFormat.HEIC, ExportFormat.PNG, "HEIC → PNG", "Convert Apple HEIC/HEIF to lossless PNG"),
    HEIC_TO_WEBP(DetectedImageFormat.HEIC, ExportFormat.WEBP_LOSSY, "HEIC → WebP", "Convert Apple HEIC/HEIF to compact WebP"),
    HEIC_TO_PDF(DetectedImageFormat.HEIC, ExportFormat.PDF, "HEIC → PDF", "Convert Apple HEIC/HEIF to PDF document"),
    BMP_TO_JPG(DetectedImageFormat.BMP, ExportFormat.JPEG, "BMP → JPG", "Convert uncompressed BMP to compact JPG"),
    BMP_TO_PNG(DetectedImageFormat.BMP, ExportFormat.PNG, "BMP → PNG", "Convert uncompressed BMP to lossless PNG"),
    BMP_TO_WEBP(DetectedImageFormat.BMP, ExportFormat.WEBP_LOSSY, "BMP → WebP", "Convert uncompressed BMP to modern WebP"),
    BMP_TO_PDF(DetectedImageFormat.BMP, ExportFormat.PDF, "BMP → PDF", "Convert uncompressed BMP to PDF document"),
    GIF_TO_JPG(DetectedImageFormat.GIF, ExportFormat.JPEG, "GIF → JPG", "Convert GIF to universal JPG (with background matte)", true),
    GIF_TO_PNG(DetectedImageFormat.GIF, ExportFormat.PNG, "GIF → PNG", "Convert GIF to lossless PNG"),
    GIF_TO_WEBP(DetectedImageFormat.GIF, ExportFormat.WEBP_LOSSY, "GIF → WebP", "Convert GIF to compact WebP"),
    GIF_TO_PDF(DetectedImageFormat.GIF, ExportFormat.PDF, "GIF → PDF", "Convert GIF to PDF document")
}

enum class PrintUnit(val symbol: String, val label: String) {
    INCHES("in", "Inches"),
    CENTIMETERS("cm", "Centimeters"),
    MILLIMETERS("mm", "Millimeters")
}

enum class StandardPrintPreset(
    val displayName: String,
    val widthInches: Float,
    val heightInches: Float,
    val description: String,
    val defaultUnit: PrintUnit
) {
    WALLET_2X3("2.5 × 3.5 inch", 2.5f, 3.5f, "Wallet Size (63.5 × 88.9 mm)", PrintUnit.INCHES),
    PHOTO_4X6("4 × 6 inch", 4.0f, 6.0f, "Standard Photo (101.6 × 152.4 mm)", PrintUnit.INCHES),
    PHOTO_5X7("5 × 7 inch", 5.0f, 7.0f, "Medium Photo (127.0 × 177.8 mm)", PrintUnit.INCHES),
    PHOTO_8X10("8 × 10 inch", 8.0f, 10.0f, "Portrait Frame (203.2 × 254.0 mm)", PrintUnit.INCHES),
    ISO_A5("A5", 5.83f, 8.27f, "Compact Booklet (148 × 210 mm)", PrintUnit.MILLIMETERS),
    ISO_A4("A4", 8.27f, 11.69f, "Standard Document (210 × 297 mm)", PrintUnit.MILLIMETERS),
    ISO_A3("A3", 11.69f, 16.54f, "Large Poster / Ledger (297 × 420 mm)", PrintUnit.MILLIMETERS),
    US_LETTER("Letter", 8.5f, 11.0f, "Standard US Letter (215.9 × 279.4 mm)", PrintUnit.INCHES),
    US_LEGAL("Legal", 8.5f, 14.0f, "US Legal Document (215.9 × 355.6 mm)", PrintUnit.INCHES),
    CUSTOM("Custom", 0.0f, 0.0f, "Custom Dimensions & Density", PrintUnit.INCHES);

    fun getWidthInUnit(unit: PrintUnit): Float = when (unit) {
        PrintUnit.INCHES -> widthInches
        PrintUnit.CENTIMETERS -> widthInches * 2.54f
        PrintUnit.MILLIMETERS -> widthInches * 25.4f
    }

    fun getHeightInUnit(unit: PrintUnit): Float = when (unit) {
        PrintUnit.INCHES -> heightInches
        PrintUnit.CENTIMETERS -> heightInches * 2.54f
        PrintUnit.MILLIMETERS -> heightInches * 25.4f
    }
}

enum class FilterPreset(val displayName: String) {
    ORIGINAL("Original"),
    DOCUMENT_BW("Doc B&W (High Contrast)"),
    DOCUMENT_MAGIC("Doc Magic Clean"),
    GRAYSCALE("Grayscale"),
    SEPIA("Sepia"),
    VIVID("Vivid Boost"),
    WARM("Warm Tone"),
    COOL("Cool Tone"),
    HIGH_CONTRAST("Deep Contrast")
}

enum class ResizeMode(val label: String, val description: String) {
    FIT("Fit", "Entire image fits inside target dimensions (no cropping)"),
    FILL("Fill", "Target dimensions are completely filled with center cropping"),
    STRETCH("Stretch", "Image is forced into target dimensions (may distort)"),
    SMART_CROP("Smart Crop", "Automatically detects the most salient subject/region to crop")
}

enum class CompressionPreset(
    val displayName: String,
    val defaultQuality: Int,
    val description: String
) {
    MAXIMUM_QUALITY("Maximum Quality", 95, "Near-lossless visual fidelity with minimal compression (~5% compression)"),
    HIGH_QUALITY("High Quality", 85, "Superb quality with balanced file size reduction (~15% compression)"),
    BALANCED("Balanced", 70, "Optimal ratio of visual clarity and lightweight file size (~30% compression)"),
    MEDIUM("Medium", 50, "Noticeable compression suitable for fast web delivery (~50% compression)"),
    SMALL_FILE("Small File", 30, "Aggressive compression for maximum space savings (~70% compression)"),
    CUSTOM("Custom", 80, "User-defined quality percentage, compression level, or target size")
}

enum class CustomCompressionMode(val label: String, val description: String) {
    QUALITY_PERCENT("Quality %", "Set compression output quality percentage (1-100%)"),
    COMPRESSION_PERCENT("Compression %", "Set reduction compression percentage (0-99%)"),
    TARGET_SIZE("Target File Size", "Iteratively optimize to closest target file size (KB)"),
    MAX_CEILING("Maximum File Size", "Strictly enforce maximum ceiling file size (KB)")
}

data class CropAspectRatio(
    val title: String,
    val ratioX: Float?,
    val ratioY: Float?
) {
    companion object {
        val FREE = CropAspectRatio("Free", null, null)
        val SQUARE = CropAspectRatio("1:1", 1f, 1f)
        val RATIO_4_5 = CropAspectRatio("4:5 (IG Portrait)", 4f, 5f)
        val RATIO_16_9 = CropAspectRatio("16:9 (Landscape)", 16f, 9f)
        val RATIO_9_16 = CropAspectRatio("9:16 (Story/Reel)", 9f, 16f)
        val RATIO_3_2 = CropAspectRatio("3:2 (Classic 35mm)", 3f, 2f)
        val RATIO_2_3 = CropAspectRatio("2:3 (Portrait)", 2f, 3f)
        val RATIO_4_3 = CropAspectRatio("4:3 (Standard)", 4f, 3f)
        val RATIO_3_4 = CropAspectRatio("3:4", 3f, 4f)
        val RATIO_2_1 = CropAspectRatio("2:1 (Header)", 2f, 1f)

        val DEFAULT_LIST = listOf(
            FREE, SQUARE, RATIO_4_5, RATIO_16_9, RATIO_9_16,
            RATIO_3_2, RATIO_2_3, RATIO_4_3, RATIO_3_4, RATIO_2_1
        )
    }
}

enum class PassportDimensionUnit(val symbol: String, val label: String) {
    MILLIMETERS("mm", "Millimeters (mm)"),
    CENTIMETERS("cm", "Centimeters (cm)"),
    INCHES("in", "Inches (in)"),
    PIXELS("px", "Pixels (px)")
}

enum class CutLineStyle(val label: String) {
    SOLID("Solid Border"),
    DASHED("Dashed Line"),
    CORNER_TICKS("Corner Crop Marks"),
    CROSSHAIRS("Crosshairs"),
    NONE("No Lines")
}

enum class SheetAlignment(val label: String) {
    CENTER("Center"),
    TOP_LEFT("Top-Left"),
    JUSTIFIED("Evenly Spaced")
}

enum class SheetOrientation(val label: String) {
    PORTRAIT("Portrait"),
    LANDSCAPE("Landscape")
}

enum class PhotoSheetRotation(val degrees: Float, val label: String) {
    ROT_0(0f, "0° (Upright)"),
    ROT_90(90f, "90° (Clockwise)"),
    ROT_180(180f, "180° (Inverted)"),
    ROT_270(270f, "270° (Counter-CW)")
}

enum class PassportExportTarget(val label: String) {
    SINGLE_PHOTO("Single Photo"),
    PRINT_SHEET("Multi-Copy Print Sheet")
}

enum class IdDocumentCategory(val label: String) {
    ALL("All Presets"),
    PASSPORT_PHOTO("Passport Photo"),
    ID_PHOTO("ID Photo"),
    VISA_PHOTO("Visa Photo"),
    APPLICATION_PHOTO("Application Photo"),
    JOB_APPLICATION("Job Application"),
    EXAM_PHOTO("Exam Photo"),
    DOCUMENT_PHOTO("Document Photo"),
    CUSTOM("Custom Dimensions")
}

data class PassportPreset(
    val country: String,
    val documentType: String,
    val widthMm: Float,
    val heightMm: Float,
    val widthPxAt300Dpi: Int,
    val heightPxAt300Dpi: Int,
    val defaultDpi: Int = 300,
    val maxFileSizeKb: Int? = null,
    val minFileSizeKb: Int? = null,
    val backgroundColor: String = "White",
    val isCustom: Boolean = false,
    val category: IdDocumentCategory = IdDocumentCategory.ID_PHOTO,
    val note: String = "Standard dimension template. Verify requirements with relevant issuing authority."
) {
    companion object {
        val PRESETS = listOf(
            // --- PASSPORT PHOTOS ---
            PassportPreset("United States", "US Biometric Passport (2×2 in)", 50.8f, 50.8f, 600, 600, 300, 240, 54, backgroundColor = "White", category = IdDocumentCategory.PASSPORT_PHOTO),
            PassportPreset("United Kingdom", "UK Biometric Passport (35×45 mm)", 35f, 45f, 413, 531, 300, 500, 50, backgroundColor = "Light Grey", category = IdDocumentCategory.PASSPORT_PHOTO),
            PassportPreset("European Union", "EU / Schengen Passport (35×45 mm)", 35f, 45f, 413, 531, 300, 500, backgroundColor = "Light Grey", category = IdDocumentCategory.PASSPORT_PHOTO),
            PassportPreset("India", "Indian Passport Seva (2×2 in / 51×51 mm)", 50.8f, 50.8f, 600, 600, 300, 250, 20, backgroundColor = "White", category = IdDocumentCategory.PASSPORT_PHOTO),
            PassportPreset("Canada", "Canadian Passport (50×70 mm)", 50f, 70f, 591, 827, 300, 400, backgroundColor = "White", category = IdDocumentCategory.PASSPORT_PHOTO),
            PassportPreset("China", "PRC Biometric Passport (33×48 mm)", 33f, 48f, 390, 567, 300, 120, 40, backgroundColor = "White", category = IdDocumentCategory.PASSPORT_PHOTO),
            PassportPreset("Australia", "Australian Passport (35×45 mm)", 35f, 45f, 413, 531, 300, 300, backgroundColor = "White", category = IdDocumentCategory.PASSPORT_PHOTO),
            PassportPreset("Japan", "Japanese Passport (35×45 mm)", 35f, 45f, 413, 531, 300, 300, backgroundColor = "White", category = IdDocumentCategory.PASSPORT_PHOTO),

            // --- ID PHOTOS ---
            PassportPreset("United States", "Real ID / State ID (2×2 in)", 50.8f, 50.8f, 600, 600, 300, 240, backgroundColor = "White", category = IdDocumentCategory.ID_PHOTO),
            PassportPreset("European Union", "National ID (35×45 mm)", 35f, 45f, 413, 531, 300, backgroundColor = "Light Grey", category = IdDocumentCategory.ID_PHOTO),
            PassportPreset("United Kingdom", "Driving Licence / ID (35×45 mm)", 35f, 45f, 413, 531, 300, backgroundColor = "Light Grey", category = IdDocumentCategory.ID_PHOTO),
            PassportPreset("India", "Aadhaar Card Photo (35×45 mm)", 35f, 45f, 413, 531, 300, 50, 20, backgroundColor = "White", category = IdDocumentCategory.ID_PHOTO),
            PassportPreset("India", "PAN Card (25×35 mm)", 25f, 35f, 295, 413, 300, 50, 10, backgroundColor = "White", category = IdDocumentCategory.ID_PHOTO),
            PassportPreset("Canada", "PR / Citizenship Card (50×70 mm)", 50f, 70f, 591, 827, 300, backgroundColor = "White", category = IdDocumentCategory.ID_PHOTO),
            PassportPreset("Australia", "Driver Licence / Photo ID (35×45 mm)", 35f, 45f, 413, 531, 300, backgroundColor = "White", category = IdDocumentCategory.ID_PHOTO),
            PassportPreset("Malaysia", "MyKad National ID (35×50 mm)", 35f, 50f, 413, 591, 300, backgroundColor = "Blue", category = IdDocumentCategory.ID_PHOTO),
            PassportPreset("Singapore", "NRIC / Identity Card (35×45 mm)", 35f, 45f, 413, 531, 300, backgroundColor = "White", category = IdDocumentCategory.ID_PHOTO),

            // --- VISA PHOTOS ---
            PassportPreset("United States", "US Visa DS-160 (2×2 in / 600×600 px)", 50.8f, 50.8f, 600, 600, 300, 240, 10, backgroundColor = "White", category = IdDocumentCategory.VISA_PHOTO),
            PassportPreset("Schengen", "Schengen Visa (35×45 mm)", 35f, 45f, 413, 531, 300, backgroundColor = "Light Grey / Off-White", category = IdDocumentCategory.VISA_PHOTO),
            PassportPreset("United Kingdom", "UK Visa & Immigration (35×45 mm)", 35f, 45f, 413, 531, 300, backgroundColor = "Light Grey", category = IdDocumentCategory.VISA_PHOTO),
            PassportPreset("China", "China Visa (33×48 mm)", 33f, 48f, 390, 567, 300, 120, 40, backgroundColor = "White / Light Blue", category = IdDocumentCategory.VISA_PHOTO),
            PassportPreset("Canada", "Canada Visitor Visa (35×45 mm)", 35f, 45f, 413, 531, 300, backgroundColor = "White", category = IdDocumentCategory.VISA_PHOTO),
            PassportPreset("Japan", "Japan Visa (35×45 mm)", 35f, 45f, 413, 531, 300, backgroundColor = "White", category = IdDocumentCategory.VISA_PHOTO),
            PassportPreset("Australia", "Australia e-Visitor Visa (35×45 mm)", 35f, 45f, 413, 531, 300, 500, backgroundColor = "White", category = IdDocumentCategory.VISA_PHOTO),
            PassportPreset("UAE / Dubai", "UAE Entry Visa (40×60 mm)", 40f, 60f, 472, 709, 300, backgroundColor = "White", category = IdDocumentCategory.VISA_PHOTO),
            PassportPreset("India", "India e-Visa (2×2 in / 51×51 mm)", 50.8f, 50.8f, 600, 600, 300, 1000, 10, backgroundColor = "White", category = IdDocumentCategory.VISA_PHOTO),

            // --- APPLICATION PHOTOS ---
            PassportPreset("University Admission", "College Application (35×45 mm)", 35f, 45f, 413, 531, 300, 100, 20, backgroundColor = "White", category = IdDocumentCategory.APPLICATION_PHOTO),
            PassportPreset("Govt Services", "General Portal Upload (35×45 mm)", 35f, 45f, 413, 531, 300, 50, 20, backgroundColor = "White", category = IdDocumentCategory.APPLICATION_PHOTO),
            PassportPreset("Passport Renewal", "Online Application (2×2 in)", 50.8f, 50.8f, 600, 600, 300, 200, 20, backgroundColor = "White", category = IdDocumentCategory.APPLICATION_PHOTO),
            PassportPreset("Transit Authority", "Travel / Metro Pass (30×40 mm)", 30f, 40f, 354, 472, 300, 50, backgroundColor = "White", category = IdDocumentCategory.APPLICATION_PHOTO),
            PassportPreset("Club / Association", "Membership Card Photo (25×30 mm)", 25f, 30f, 295, 354, 300, 50, backgroundColor = "White", category = IdDocumentCategory.APPLICATION_PHOTO),

            // --- JOB APPLICATION PHOTOS ---
            PassportPreset("Resume / CV", "Professional Headshot (35×45 mm)", 35f, 45f, 413, 531, 300, backgroundColor = "White / Light Grey", category = IdDocumentCategory.JOB_APPLICATION),
            PassportPreset("LinkedIn / Profile", "Professional Square (2×2 in / 600×600 px)", 50.8f, 50.8f, 600, 600, 300, backgroundColor = "White / Blue", category = IdDocumentCategory.JOB_APPLICATION),
            PassportPreset("Corporate ID", "Employee Security Badge (35×45 mm)", 35f, 45f, 413, 531, 300, backgroundColor = "White", category = IdDocumentCategory.JOB_APPLICATION),
            PassportPreset("Recruitment Portal", "Square Upload (400×400 px)", 33.9f, 33.9f, 400, 400, 300, 100, backgroundColor = "White", category = IdDocumentCategory.JOB_APPLICATION),

            // --- EXAM PHOTOS ---
            PassportPreset("UPSC", "Civil Services Exam (35×45 mm)", 35f, 45f, 350, 450, 300, 300, 20, backgroundColor = "White", category = IdDocumentCategory.EXAM_PHOTO),
            PassportPreset("SSC", "Staff Selection Commission (200×230 px)", 35f, 45f, 200, 230, 200, 50, 20, backgroundColor = "White", category = IdDocumentCategory.EXAM_PHOTO),
            PassportPreset("IBPS / Bank PO", "Banking Exam Photo (200×230 px)", 35f, 45f, 200, 230, 200, 50, 20, backgroundColor = "White", category = IdDocumentCategory.EXAM_PHOTO),
            PassportPreset("NEET / JEE", "National Testing Agency (35×45 mm)", 35f, 45f, 413, 531, 300, 200, 10, backgroundColor = "White (80% Face)", category = IdDocumentCategory.EXAM_PHOTO),
            PassportPreset("GATE", "Graduate Aptitude Test (240×320 px)", 35f, 45f, 240, 320, 200, 200, 20, backgroundColor = "White", category = IdDocumentCategory.EXAM_PHOTO),
            PassportPreset("GRE / TOEFL", "ETS Testing Registration (2×2 in)", 50.8f, 50.8f, 600, 600, 300, 200, 10, backgroundColor = "White", category = IdDocumentCategory.EXAM_PHOTO),

            // --- DOCUMENT PHOTOS ---
            PassportPreset("Official Attachment", "Document Photo Inset (2×2 in)", 50.8f, 50.8f, 600, 600, 300, backgroundColor = "White", category = IdDocumentCategory.DOCUMENT_PHOTO),
            PassportPreset("Certificate", "Diploma / Degree Inset (30×40 mm)", 30f, 40f, 354, 472, 300, backgroundColor = "White", category = IdDocumentCategory.DOCUMENT_PHOTO),
            PassportPreset("Card Inset", "Card Photo Scanned (25×35 mm)", 25f, 35f, 295, 413, 300, backgroundColor = "White", category = IdDocumentCategory.DOCUMENT_PHOTO),
            PassportPreset("Green Card", "US Form I-551 (2×2 in / 600×600 px)", 50.8f, 50.8f, 600, 600, 300, 240, backgroundColor = "White", category = IdDocumentCategory.DOCUMENT_PHOTO),
            PassportPreset("Driving Permit", "International Driving Permit (35×45 mm)", 35f, 45f, 413, 531, 300, backgroundColor = "White", category = IdDocumentCategory.DOCUMENT_PHOTO)
        )
    }
}

enum class SocialMediaCategory(val label: String, val description: String) {
    ALL("All Dimensions", "All platform guidelines"),
    PROFILE_PHOTOS("Profile Photos", "Avatars & profile pictures"),
    POSTS("Posts", "Feeds, timelines & pins"),
    STORIES("Stories", "Full vertical format 9:16"),
    COVERS("Covers", "Page, event & channel covers"),
    HEADERS("Headers", "Banners & profile headers"),
    THUMBNAILS("Thumbnails", "Video & audio thumbnails"),
    CUSTOM("Custom", "User defined dimensions")
}

data class SocialMediaPreset(
    val id: String = java.util.UUID.randomUUID().toString(),
    val platform: String,
    val name: String,
    val category: SocialMediaCategory = SocialMediaCategory.POSTS,
    val width: Int,
    val height: Int,
    val aspectRatioLabel: String,
    val recommendedFormat: String = "JPG",
    val maxFileSizeKb: Int? = null,
    val dpi: Int = 72,
    val cropMode: String = "FIT",
    val note: String = "Platform guideline. Fully customizable.",
    val isCustom: Boolean = false,
    val isModified: Boolean = false
) {
    companion object {
        val DEFAULT_PRESETS = listOf(
            // --- PROFILE PHOTOS ---
            SocialMediaPreset(
                id = "ig_profile",
                platform = "Instagram",
                name = "Profile Photo",
                category = SocialMediaCategory.PROFILE_PHOTOS,
                width = 320,
                height = 320,
                aspectRatioLabel = "1:1",
                recommendedFormat = "JPG",
                maxFileSizeKb = 2048,
                note = "Circular crop on profile; keep subject centered"
            ),
            SocialMediaPreset(
                id = "x_profile",
                platform = "Twitter / X",
                name = "Profile Photo",
                category = SocialMediaCategory.PROFILE_PHOTOS,
                width = 400,
                height = 400,
                aspectRatioLabel = "1:1",
                recommendedFormat = "JPG",
                maxFileSizeKb = 2048,
                note = "Displays circular across timeline and author tags"
            ),
            SocialMediaPreset(
                id = "fb_profile",
                platform = "Facebook",
                name = "Profile Picture",
                category = SocialMediaCategory.PROFILE_PHOTOS,
                width = 720,
                height = 720,
                aspectRatioLabel = "1:1",
                recommendedFormat = "JPG",
                maxFileSizeKb = 4096,
                note = "High resolution avatar for crisp desktop & mobile view"
            ),
            SocialMediaPreset(
                id = "li_profile",
                platform = "LinkedIn",
                name = "Profile Photo (Headshot)",
                category = SocialMediaCategory.PROFILE_PHOTOS,
                width = 400,
                height = 400,
                aspectRatioLabel = "1:1",
                recommendedFormat = "JPG",
                maxFileSizeKb = 8192,
                note = "Professional headshot with centered framing"
            ),
            SocialMediaPreset(
                id = "yt_profile",
                platform = "YouTube",
                name = "Channel Profile Photo",
                category = SocialMediaCategory.PROFILE_PHOTOS,
                width = 800,
                height = 800,
                aspectRatioLabel = "1:1",
                recommendedFormat = "JPG",
                maxFileSizeKb = 4096,
                note = "Renders on channel homepage and comments"
            ),
            SocialMediaPreset(
                id = "tiktok_profile",
                platform = "TikTok",
                name = "Profile Avatar",
                category = SocialMediaCategory.PROFILE_PHOTOS,
                width = 200,
                height = 200,
                aspectRatioLabel = "1:1",
                recommendedFormat = "JPG",
                maxFileSizeKb = 2048,
                note = "Recommended 200x200 px for crisp mobile display"
            ),
            SocialMediaPreset(
                id = "wa_profile",
                platform = "WhatsApp",
                name = "Profile Picture",
                category = SocialMediaCategory.PROFILE_PHOTOS,
                width = 500,
                height = 500,
                aspectRatioLabel = "1:1",
                recommendedFormat = "JPG",
                maxFileSizeKb = 2048,
                note = "Displays in chat header and contact card"
            ),
            SocialMediaPreset(
                id = "discord_profile",
                platform = "Discord",
                name = "User Avatar",
                category = SocialMediaCategory.PROFILE_PHOTOS,
                width = 512,
                height = 512,
                aspectRatioLabel = "1:1",
                recommendedFormat = "PNG",
                maxFileSizeKb = 10240,
                note = "Square avatar; supports alpha transparency"
            ),
            SocialMediaPreset(
                id = "pinterest_profile",
                platform = "Pinterest",
                name = "Profile Image",
                category = SocialMediaCategory.PROFILE_PHOTOS,
                width = 165,
                height = 165,
                aspectRatioLabel = "1:1",
                recommendedFormat = "JPG",
                maxFileSizeKb = 10240,
                note = "Circular avatar on board profile"
            ),
            SocialMediaPreset(
                id = "twitch_profile",
                platform = "Twitch",
                name = "Channel Avatar",
                category = SocialMediaCategory.PROFILE_PHOTOS,
                width = 256,
                height = 256,
                aspectRatioLabel = "1:1",
                recommendedFormat = "PNG",
                maxFileSizeKb = 10240,
                note = "Streamer avatar badge"
            ),

            // --- POSTS ---
            SocialMediaPreset(
                id = "ig_post_sq",
                platform = "Instagram",
                name = "Square Post",
                category = SocialMediaCategory.POSTS,
                width = 1080,
                height = 1080,
                aspectRatioLabel = "1:1",
                recommendedFormat = "JPG",
                note = "Standard 1:1 Instagram feed post"
            ),
            SocialMediaPreset(
                id = "ig_post_portrait",
                platform = "Instagram",
                name = "Portrait Post",
                category = SocialMediaCategory.POSTS,
                width = 1080,
                height = 1350,
                aspectRatioLabel = "4:5",
                recommendedFormat = "JPG",
                note = "Maximizes vertical screen height on mobile feeds"
            ),
            SocialMediaPreset(
                id = "ig_post_landscape",
                platform = "Instagram",
                name = "Landscape Post",
                category = SocialMediaCategory.POSTS,
                width = 1080,
                height = 566,
                aspectRatioLabel = "1.91:1",
                recommendedFormat = "JPG",
                note = "Widescreen horizontal feed image"
            ),
            SocialMediaPreset(
                id = "fb_post_landscape",
                platform = "Facebook",
                name = "Shared Image (Landscape)",
                category = SocialMediaCategory.POSTS,
                width = 1200,
                height = 630,
                aspectRatioLabel = "1.91:1",
                recommendedFormat = "JPG",
                note = "Optimal feed timeline preview and link card"
            ),
            SocialMediaPreset(
                id = "fb_post_sq",
                platform = "Facebook",
                name = "Square Post",
                category = SocialMediaCategory.POSTS,
                width = 1080,
                height = 1080,
                aspectRatioLabel = "1:1",
                recommendedFormat = "JPG",
                note = "1:1 grid and carousel image format"
            ),
            SocialMediaPreset(
                id = "x_post_feed",
                platform = "Twitter / X",
                name = "In-Stream Post",
                category = SocialMediaCategory.POSTS,
                width = 1200,
                height = 675,
                aspectRatioLabel = "16:9",
                recommendedFormat = "JPG",
                maxFileSizeKb = 5120,
                note = "Standard single image timeline post"
            ),
            SocialMediaPreset(
                id = "x_post_hires",
                platform = "Twitter / X",
                name = "High-Res Post",
                category = SocialMediaCategory.POSTS,
                width = 1600,
                height = 900,
                aspectRatioLabel = "16:9",
                recommendedFormat = "JPG",
                maxFileSizeKb = 5120,
                note = "Ultra sharp photo presentation"
            ),
            SocialMediaPreset(
                id = "li_post_landscape",
                platform = "LinkedIn",
                name = "Feed Post (Landscape)",
                category = SocialMediaCategory.POSTS,
                width = 1200,
                height = 627,
                aspectRatioLabel = "1.91:1",
                recommendedFormat = "JPG",
                maxFileSizeKb = 5120,
                note = "Professional feed update and article preview"
            ),
            SocialMediaPreset(
                id = "li_post_sq",
                platform = "LinkedIn",
                name = "Feed Post (Square)",
                category = SocialMediaCategory.POSTS,
                width = 1080,
                height = 1080,
                aspectRatioLabel = "1:1",
                recommendedFormat = "JPG",
                note = "Square image upload for high mobile engagement"
            ),
            SocialMediaPreset(
                id = "pinterest_pin_std",
                platform = "Pinterest",
                name = "Standard Pin",
                category = SocialMediaCategory.POSTS,
                width = 1000,
                height = 1500,
                aspectRatioLabel = "2:3",
                recommendedFormat = "JPG",
                maxFileSizeKb = 20480,
                note = "Recommended 2:3 vertical pin format"
            ),
            SocialMediaPreset(
                id = "pinterest_pin_sq",
                platform = "Pinterest",
                name = "Square Pin",
                category = SocialMediaCategory.POSTS,
                width = 1000,
                height = 1000,
                aspectRatioLabel = "1:1",
                recommendedFormat = "JPG",
                note = "1:1 ratio square pin"
            ),
            SocialMediaPreset(
                id = "threads_post",
                platform = "Threads",
                name = "Feed Post",
                category = SocialMediaCategory.POSTS,
                width = 1080,
                height = 1080,
                aspectRatioLabel = "1:1",
                recommendedFormat = "JPG",
                note = "Square post image for Threads feed"
            ),
            SocialMediaPreset(
                id = "reddit_post",
                platform = "Reddit",
                name = "Post Image",
                category = SocialMediaCategory.POSTS,
                width = 1200,
                height = 630,
                aspectRatioLabel = "1.91:1",
                recommendedFormat = "JPG",
                note = "Standard horizontal card image preview"
            ),

            // --- STORIES & REELS ---
            SocialMediaPreset(
                id = "ig_story",
                platform = "Instagram",
                name = "Story / Reel",
                category = SocialMediaCategory.STORIES,
                width = 1080,
                height = 1920,
                aspectRatioLabel = "9:16",
                recommendedFormat = "JPG",
                note = "Full screen 9:16 vertical story and reel"
            ),
            SocialMediaPreset(
                id = "fb_story",
                platform = "Facebook",
                name = "Story",
                category = SocialMediaCategory.STORIES,
                width = 1080,
                height = 1920,
                aspectRatioLabel = "9:16",
                recommendedFormat = "JPG",
                note = "Full screen mobile story format"
            ),
            SocialMediaPreset(
                id = "tiktok_video",
                platform = "TikTok",
                name = "Full Vertical Video / Story",
                category = SocialMediaCategory.STORIES,
                width = 1080,
                height = 1920,
                aspectRatioLabel = "9:16",
                recommendedFormat = "JPG",
                note = "Standard TikTok mobile dimension"
            ),
            SocialMediaPreset(
                id = "snapchat_story",
                platform = "Snapchat",
                name = "Snap / Story",
                category = SocialMediaCategory.STORIES,
                width = 1080,
                height = 1920,
                aspectRatioLabel = "9:16",
                recommendedFormat = "JPG",
                note = "1080x1920 px vertical snap format"
            ),
            SocialMediaPreset(
                id = "yt_shorts",
                platform = "YouTube",
                name = "Shorts Video Frame",
                category = SocialMediaCategory.STORIES,
                width = 1080,
                height = 1920,
                aspectRatioLabel = "9:16",
                recommendedFormat = "JPG",
                note = "Vertical Shorts format"
            ),
            SocialMediaPreset(
                id = "wa_status",
                platform = "WhatsApp",
                name = "Status Story",
                category = SocialMediaCategory.STORIES,
                width = 1080,
                height = 1920,
                aspectRatioLabel = "9:16",
                recommendedFormat = "JPG",
                note = "Vertical status media for WhatsApp"
            ),

            // --- COVERS ---
            SocialMediaPreset(
                id = "fb_cover_page",
                platform = "Facebook",
                name = "Page / Profile Cover",
                category = SocialMediaCategory.COVERS,
                width = 820,
                height = 312,
                aspectRatioLabel = "2.6:1",
                recommendedFormat = "JPG",
                note = "Displays 820x312 on desktop, 640x360 on mobile"
            ),
            SocialMediaPreset(
                id = "fb_cover_group",
                platform = "Facebook",
                name = "Group Cover Photo",
                category = SocialMediaCategory.COVERS,
                width = 1640,
                height = 856,
                aspectRatioLabel = "1.91:1",
                recommendedFormat = "JPG",
                note = "Recommended group header dimensions"
            ),
            SocialMediaPreset(
                id = "fb_cover_event",
                platform = "Facebook",
                name = "Event Cover",
                category = SocialMediaCategory.COVERS,
                width = 1920,
                height = 1005,
                aspectRatioLabel = "1.91:1",
                recommendedFormat = "JPG",
                note = "Event banner cover photo"
            ),
            SocialMediaPreset(
                id = "li_cover_company",
                platform = "LinkedIn",
                name = "Company Page Cover",
                category = SocialMediaCategory.COVERS,
                width = 1128,
                height = 191,
                aspectRatioLabel = "5.9:1",
                recommendedFormat = "PNG",
                note = "Wide banner for company organization pages"
            ),
            SocialMediaPreset(
                id = "soundcloud_cover",
                platform = "SoundCloud",
                name = "Profile Header Cover",
                category = SocialMediaCategory.COVERS,
                width = 2480,
                height = 520,
                aspectRatioLabel = "4.77:1",
                recommendedFormat = "JPG",
                note = "Artist header banner cover"
            ),
            SocialMediaPreset(
                id = "twitch_cover",
                platform = "Twitch",
                name = "Channel Cover Banner",
                category = SocialMediaCategory.COVERS,
                width = 1200,
                height = 480,
                aspectRatioLabel = "2.5:1",
                recommendedFormat = "JPG",
                note = "Displays at top of channel page"
            ),

            // --- HEADERS ---
            SocialMediaPreset(
                id = "x_header",
                platform = "Twitter / X",
                name = "Profile Header Banner",
                category = SocialMediaCategory.HEADERS,
                width = 1500,
                height = 500,
                aspectRatioLabel = "3:1",
                recommendedFormat = "JPG",
                maxFileSizeKb = 2048,
                note = "3:1 header; account for profile avatar bottom left overlap"
            ),
            SocialMediaPreset(
                id = "yt_banner",
                platform = "YouTube",
                name = "Channel Banner Header",
                category = SocialMediaCategory.HEADERS,
                width = 2560,
                height = 1440,
                aspectRatioLabel = "16:9",
                recommendedFormat = "JPG",
                maxFileSizeKb = 6144,
                note = "Keep logos and text in center 1546x423 safe zone"
            ),
            SocialMediaPreset(
                id = "li_header_personal",
                platform = "LinkedIn",
                name = "Personal Profile Banner",
                category = SocialMediaCategory.HEADERS,
                width = 1584,
                height = 396,
                aspectRatioLabel = "4:1",
                recommendedFormat = "JPG",
                maxFileSizeKb = 8192,
                note = "Background banner behind personal headshot"
            ),
            SocialMediaPreset(
                id = "discord_header_banner",
                platform = "Discord",
                name = "Server Banner",
                category = SocialMediaCategory.HEADERS,
                width = 960,
                height = 540,
                aspectRatioLabel = "16:9",
                recommendedFormat = "JPG",
                maxFileSizeKb = 10240,
                note = "Displayed at top of server channels"
            ),
            SocialMediaPreset(
                id = "github_header",
                platform = "GitHub",
                name = "Social Preview Header",
                category = SocialMediaCategory.HEADERS,
                width = 1280,
                height = 640,
                aspectRatioLabel = "2:1",
                recommendedFormat = "PNG",
                note = "Repository social preview card header"
            ),
            SocialMediaPreset(
                id = "etsy_header",
                platform = "Etsy",
                name = "Shop Big Banner",
                category = SocialMediaCategory.HEADERS,
                width = 2400,
                height = 800,
                aspectRatioLabel = "3:1",
                recommendedFormat = "JPG",
                note = "Large desktop banner for online storefront"
            ),

            // --- THUMBNAILS ---
            SocialMediaPreset(
                id = "yt_thumbnail",
                platform = "YouTube",
                name = "Video Thumbnail",
                category = SocialMediaCategory.THUMBNAILS,
                width = 1280,
                height = 720,
                aspectRatioLabel = "16:9",
                recommendedFormat = "JPG",
                maxFileSizeKb = 2048,
                note = "16:9 HD video thumbnail (Strictly ≤ 2048 KB)"
            ),
            SocialMediaPreset(
                id = "tiktok_thumbnail",
                platform = "TikTok",
                name = "Video Cover Thumbnail",
                category = SocialMediaCategory.THUMBNAILS,
                width = 1080,
                height = 1920,
                aspectRatioLabel = "9:16",
                recommendedFormat = "JPG",
                note = "Vertical frame thumbnail for video feed grid"
            ),
            SocialMediaPreset(
                id = "vimeo_thumbnail",
                platform = "Vimeo",
                name = "Video Thumbnail",
                category = SocialMediaCategory.THUMBNAILS,
                width = 1920,
                height = 1080,
                aspectRatioLabel = "16:9",
                recommendedFormat = "JPG",
                note = "Full HD 1080p video poster frame"
            ),
            SocialMediaPreset(
                id = "podcast_cover",
                platform = "Apple / Spotify Podcasts",
                name = "Podcast Show Cover Art",
                category = SocialMediaCategory.THUMBNAILS,
                width = 3000,
                height = 3000,
                aspectRatioLabel = "1:1",
                recommendedFormat = "JPG",
                maxFileSizeKb = 2048,
                note = "High res podcast square (1400x1400 to 3000x3000, < 2 MB)"
            ),
            SocialMediaPreset(
                id = "blog_thumbnail",
                platform = "Web / Blog",
                name = "Featured Article Thumbnail",
                category = SocialMediaCategory.THUMBNAILS,
                width = 1200,
                height = 630,
                aspectRatioLabel = "1.91:1",
                recommendedFormat = "JPG",
                note = "Standard Open Graph / social card thumbnail"
            ),
            SocialMediaPreset(
                id = "dribbble_shot",
                platform = "Dribbble",
                name = "Design Shot Thumbnail",
                category = SocialMediaCategory.THUMBNAILS,
                width = 1600,
                height = 1200,
                aspectRatioLabel = "4:3",
                recommendedFormat = "PNG",
                maxFileSizeKb = 10240,
                note = "4:3 design portfolio showcase"
            )
        )

        val PRESETS: List<SocialMediaPreset> get() = DEFAULT_PRESETS
    }
}

enum class MetadataPolicy(val label: String, val description: String) {
    STRIP_ALL("Remove All", "Strips all optional EXIF, location, camera, and date fields"),
    CUSTOM_SELECTIVE("Selective Scrub", "Granular control over which sensitive categories to remove"),
    KEEP_ALL("Keep All", "Preserves original EXIF, camera, GPS, and date tags in supported formats")
}

data class MetadataPrivacyConfig(
    val policy: MetadataPolicy = MetadataPolicy.STRIP_ALL,
    val removeGps: Boolean = true,
    val removeCameraInfo: Boolean = true,
    val removeDeviceInfo: Boolean = true,
    val removeDateMetadata: Boolean = true,
    val customArtist: String = "",
    val customCopyright: String = "",
    val customComment: String = ""
) {
    val isAnyPrivacyActive: Boolean get() = policy == MetadataPolicy.STRIP_ALL || (policy == MetadataPolicy.CUSTOM_SELECTIVE && (removeGps || removeCameraInfo || removeDeviceInfo || removeDateMetadata))
    val scrubbedCategoriesCount: Int get() {
        if (policy == MetadataPolicy.STRIP_ALL) return 4
        if (policy == MetadataPolicy.KEEP_ALL) return 0
        var count = 0
        if (removeGps) count++
        if (removeCameraInfo) count++
        if (removeDeviceInfo) count++
        if (removeDateMetadata) count++
        return count
    }
}

data class ImageExifData(
    val make: String? = null,
    val model: String? = null,
    val lensModel: String? = null,
    val software: String? = null,
    val serialNumber: String? = null,
    val aperture: String? = null,
    val shutterSpeed: String? = null,
    val iso: String? = null,
    val focalLength: String? = null,
    val focalLength35mm: String? = null,
    val dateTime: String? = null,
    val dateTimeOriginal: String? = null,
    val dateTimeDigitized: String? = null,
    val subSecTime: String? = null,
    val flash: String? = null,
    val whiteBalance: String? = null,
    val exposureProgram: String? = null,
    val meteringMode: String? = null,
    val exposureBias: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val altitude: Double? = null,
    val gpsDateStamp: String? = null,
    val gpsTimeStamp: String? = null,
    val gpsProcessingMethod: String? = null,
    val deviceSettingDescription: String? = null,
    val sensingMethod: String? = null,
    val orientation: Int = 0,
    val artist: String? = null,
    val copyright: String? = null,
    val userComment: String? = null,
    val imageDescription: String? = null,
    val hasGps: Boolean = false
) {
    val hasCameraInfo: Boolean get() = !make.isNullOrBlank() || !model.isNullOrBlank() || !lensModel.isNullOrBlank() || !software.isNullOrBlank()
    val hasDeviceInfo: Boolean get() = !deviceSettingDescription.isNullOrBlank() || !sensingMethod.isNullOrBlank() || !serialNumber.isNullOrBlank()
    val hasDateInfo: Boolean get() = !dateTime.isNullOrBlank() || !dateTimeOriginal.isNullOrBlank() || !dateTimeDigitized.isNullOrBlank() || !gpsDateStamp.isNullOrBlank()
    val hasOpticsInfo: Boolean get() = !aperture.isNullOrBlank() || !shutterSpeed.isNullOrBlank() || !iso.isNullOrBlank() || !focalLength.isNullOrBlank()
    val totalIdentifiedTagsCount: Int get() {
        var c = 0
        if (hasGps) c++
        if (hasCameraInfo) c++
        if (hasDeviceInfo) c++
        if (hasDateInfo) c++
        if (hasOpticsInfo) c++
        if (!artist.isNullOrBlank() || !copyright.isNullOrBlank()) c++
        return c
    }
}

data class ImageMetadata(
    val uri: Uri,
    val fileName: String,
    val width: Int,
    val height: Int,
    val fileSizeInBytes: Long,
    val mimeType: String,
    val dpi: Int = 300,
    val exifData: ImageExifData? = null
) {
    val megapixels: Float get() = (width * height) / 1_000_000f
    val formattedSize: String get() {
        val kb = fileSizeInBytes / 1024.0
        return if (kb > 1024) {
            String.format("%.2f MB", kb / 1024.0)
        } else {
            String.format("%.1f KB", kb)
        }
    }
    val printWidthInches: Float get() = width.toFloat() / dpi
    val printHeightInches: Float get() = height.toFloat() / dpi
    val printWidthCm: Float get() = printWidthInches * 2.54f
    val printHeightCm: Float get() = printHeightInches * 2.54f
}

data class BatchItem(
    val id: String = java.util.UUID.randomUUID().toString(),
    val uri: Uri,
    val originalName: String,
    val originalSize: Long,
    val originalWidth: Int,
    val originalHeight: Int,
    val status: BatchStatus = BatchStatus.PENDING,
    val progress: Float = 0f,
    val outputUri: Uri? = null,
    val outputSize: Long = 0,
    val outputWidth: Int? = null,
    val outputHeight: Int? = null,
    val errorMessage: String? = null,
    // Phase 9 Multi-Select, Format, Metadata & Stage Telemetry
    val selected: Boolean = true,
    val detectedFormat: DetectedImageFormat = DetectedImageFormat.UNKNOWN,
    val hasExif: Boolean = false,
    val hasGps: Boolean = false,
    val stageLabel: String = "",
    val outputFormat: ExportFormat? = null,
    val outputFileName: String? = null,
    val processingTimeMs: Long = 0L,
    val metadataSummary: String? = null,
    val retryCount: Int = 0
) {
    val savedBytes: Long
        get() = if (status == BatchStatus.COMPLETED && originalSize > 0 && outputSize > 0) {
            (originalSize - outputSize).coerceAtLeast(0L)
        } else 0L

    val savedPercent: Float
        get() = if (status == BatchStatus.COMPLETED && originalSize > 0 && outputSize > 0) {
            (((originalSize - outputSize).toFloat() / originalSize.toFloat()) * 100f)
        } else 0f
}

enum class BatchStatus {
    PENDING, PROCESSING, COMPLETED, FAILED, CANCELLED
}

enum class BatchResizeOption(val label: String, val shortLabel: String) {
    ORIGINAL("Original", "Original"),
    PERCENTAGE("Scale %", "Scale %"),
    SAME_WIDTH("Same Width", "Same W"),
    SAME_HEIGHT("Same Height", "Same H"),
    LONGEST_EDGE("Longest Edge", "Max Edge"),
    EXACT_DIMENSIONS("Exact W × H", "Exact W×H"),
    PHYSICAL_PRINT("Print Size", "Print")
}

enum class BatchCompressionMode(val label: String, val description: String) {
    QUALITY("Quality %", "Compress by fixed quality percentage (1–100%)"),
    PRESET("Preset Tier", "Standard compression presets (Max, High, Balanced, Medium, Small)"),
    EXACT_TARGET_KB("Target KB", "Iteratively optimize each image to match target KB"),
    MAX_CEILING_KB("Max Ceiling KB", "Strictly cap each image under maximum KB ceiling")
}

enum class BatchSortOption(val label: String) {
    QUEUE_ORDER("Queue Order"),
    NAME_ASC("Name (A–Z)"),
    SIZE_DESC("Size (Largest)"),
    SIZE_ASC("Size (Smallest)"),
    RESOLUTION_DESC("Resolution (Highest)"),
    STATUS("Status")
}

enum class BatchFilterOption(val label: String) {
    ALL("All"),
    SELECTED("Selected"),
    PENDING("Pending"),
    COMPLETED("Done"),
    FAILED("Failed / Cancelled")
}

data class BatchConfig(
    val resizeOption: BatchResizeOption = BatchResizeOption.ORIGINAL,
    val scalePercent: Int = 100,
    val sameWidth: Int = 1080,
    val sameHeight: Int = 1080,
    val longestEdge: Int = 1920,
    val exactWidth: Int = 1920,
    val exactHeight: Int = 1080,
    val physicalWidth: Float = 4.0f,
    val physicalHeight: Float = 6.0f,
    val physicalUnit: PrintUnit = PrintUnit.INCHES,
    val cropMode: ResizeMode = ResizeMode.FIT,
    val fitBackgroundColor: Int = android.graphics.Color.WHITE,
    val doNotUpscale: Boolean = false,
    val compressionMode: BatchCompressionMode = BatchCompressionMode.QUALITY,
    val compressionPreset: CompressionPreset = CompressionPreset.HIGH_QUALITY,
    val quality: Int = 85,
    val targetMaxKb: Int? = null,
    val allowDimensionDownscalingForKb: Boolean = true,
    val keepOriginalFormat: Boolean = false,
    val format: ExportFormat = ExportFormat.JPEG,
    val jpegBackgroundColor: Int = android.graphics.Color.WHITE,
    val rotationDegrees: Int = 0,
    val flipHorizontal: Boolean = false,
    val flipVertical: Boolean = false,
    val metadataPolicy: MetadataPolicy = MetadataPolicy.KEEP_ALL,
    val removeGps: Boolean = true,
    val removeCameraInfo: Boolean = true,
    val removeDeviceInfo: Boolean = true,
    val removeDateMetadata: Boolean = false,
    val customArtist: String = "",
    val customCopyright: String = "",
    val customComment: String = "",
    val dpi: Int = 300,
    val injectBinaryDpi: Boolean = true,
    val processSelectedOnly: Boolean = false
) {
    fun toMetadataPrivacyConfig(): MetadataPrivacyConfig = MetadataPrivacyConfig(
        policy = metadataPolicy,
        removeGps = if (metadataPolicy == MetadataPolicy.STRIP_ALL) true else if (metadataPolicy == MetadataPolicy.KEEP_ALL) false else removeGps,
        removeCameraInfo = if (metadataPolicy == MetadataPolicy.STRIP_ALL) true else if (metadataPolicy == MetadataPolicy.KEEP_ALL) false else removeCameraInfo,
        removeDeviceInfo = if (metadataPolicy == MetadataPolicy.STRIP_ALL) true else if (metadataPolicy == MetadataPolicy.KEEP_ALL) false else removeDeviceInfo,
        removeDateMetadata = if (metadataPolicy == MetadataPolicy.STRIP_ALL) true else if (metadataPolicy == MetadataPolicy.KEEP_ALL) false else removeDateMetadata,
        customArtist = customArtist,
        customCopyright = customCopyright,
        customComment = customComment
    )
}

data class BatchSummaryReport(
    val totalItems: Int = 0,
    val completedItems: Int = 0,
    val failedItems: Int = 0,
    val cancelledItems: Int = 0,
    val pendingItems: Int = 0,
    val totalOriginalBytes: Long = 0L,
    val totalOutputBytes: Long = 0L,
    val totalSavedBytes: Long = 0L,
    val averageCompressionPercent: Float = 0f,
    val totalProcessingTimeMs: Long = 0L,
    val averageTimePerItemMs: Long = 0L
)

data class SheetPaperPreset(
    val name: String,
    val widthInches: Float,
    val heightInches: Float,
    val defaultCols: Int = 2,
    val defaultRows: Int = 3,
    val isCustom: Boolean = false
) {
    val widthMm: Float get() = widthInches * 25.4f
    val heightMm: Float get() = heightInches * 25.4f

    companion object {
        val A4 = SheetPaperPreset("A4 (210×297 mm)", 8.27f, 11.69f, 2, 4)
        val A5 = SheetPaperPreset("A5 (148×210 mm)", 5.83f, 8.27f, 2, 3)
        val LETTER = SheetPaperPreset("Letter (8.5×11 in)", 8.5f, 11.0f, 2, 4)
        val LEGAL = SheetPaperPreset("Legal (8.5×14 in)", 8.5f, 14.0f, 2, 5)
        val PHOTO_4X6 = SheetPaperPreset("4×6 in (102×152 mm)", 4.0f, 6.0f, 2, 3)
        val PHOTO_5X7 = SheetPaperPreset("5×7 in (127×178 mm)", 5.0f, 7.0f, 2, 4)
        val CUSTOM = SheetPaperPreset("Custom Paper", 8.27f, 11.69f, 2, 4, isCustom = true)

        val ALL = listOf(A4, A5, LETTER, LEGAL, PHOTO_4X6, PHOTO_5X7, CUSTOM)
    }
}

enum class ExactSizeUnit(val symbol: String, val label: String) {
    PX("px", "Pixels"),
    IN("in", "Inches"),
    CM("cm", "Centimeters"),
    MM("mm", "Millimeters")
}

data class ExactSizePreset(
    val name: String,
    val width: Float,
    val height: Float,
    val unit: ExactSizeUnit,
    val description: String
) {
    fun toPixels(dpi: Int = 300): Pair<Int, Int> {
        return when (unit) {
            ExactSizeUnit.PX -> Pair(width.toInt(), height.toInt())
            ExactSizeUnit.IN -> Pair((width * dpi).toInt(), (height * dpi).toInt())
            ExactSizeUnit.CM -> Pair(((width / 2.54f) * dpi).toInt(), ((height / 2.54f) * dpi).toInt())
            ExactSizeUnit.MM -> Pair(((width / 25.4f) * dpi).toInt(), ((height / 25.4f) * dpi).toInt())
        }
    }

    companion object {
        val INSTAGRAM_SQUARE = ExactSizePreset("Instagram Square", 1080f, 1080f, ExactSizeUnit.PX, "1080 × 1080 px (1:1)")
        val FULL_HD = ExactSizePreset("Full HD 1080p", 1920f, 1080f, ExactSizeUnit.PX, "1920 × 1080 px (16:9)")
        val UHD_4K = ExactSizePreset("4K Ultra HD", 3840f, 2160f, ExactSizeUnit.PX, "3840 × 2160 px (16:9)")
        val SOCIAL_STORY = ExactSizePreset("Social Story", 1080f, 1920f, ExactSizeUnit.PX, "1080 × 1920 px (9:16)")
        val TWITTER_HEADER = ExactSizePreset("Twitter/X Header", 1500f, 500f, ExactSizeUnit.PX, "1500 × 500 px (3:1)")
        val PHOTO_4X6 = ExactSizePreset("Photo 4×6 in", 4.0f, 6.0f, ExactSizeUnit.IN, "4 × 6 inches (1200×1800 @ 300 DPI)")
        val PHOTO_5X7 = ExactSizePreset("Photo 5×7 in", 5.0f, 7.0f, ExactSizeUnit.IN, "5 × 7 inches (1500×2100 @ 300 DPI)")
        val PHOTO_8X10 = ExactSizePreset("Photo 8×10 in", 8.0f, 10.0f, ExactSizeUnit.IN, "8 × 10 inches (2400×3000 @ 300 DPI)")
        val PASSPORT_US = ExactSizePreset("US Passport 2×2 in", 2.0f, 2.0f, ExactSizeUnit.IN, "2 × 2 inches (600×600 @ 300 DPI)")
        val A4_PAGE = ExactSizePreset("A4 Sheet", 210f, 297f, ExactSizeUnit.MM, "210 × 297 mm (2480×3508 @ 300 DPI)")
        val BUSINESS_CARD = ExactSizePreset("Business Card", 3.5f, 2.0f, ExactSizeUnit.IN, "3.5 × 2 inches (1050×600 @ 300 DPI)")

        val PRESETS = listOf(
            INSTAGRAM_SQUARE, FULL_HD, UHD_4K, SOCIAL_STORY, TWITTER_HEADER,
            PHOTO_4X6, PHOTO_5X7, PHOTO_8X10, PASSPORT_US, A4_PAGE, BUSINESS_CARD
        )
    }
}

data class EditorHistorySnapshot(
    val description: String,
    val instructions: EditingInstructions = EditingInstructions(),
    val timestamp: Long = System.currentTimeMillis()
)

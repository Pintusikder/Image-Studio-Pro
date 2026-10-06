package com.example.domain.usecase

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import com.example.processing.ImageInfoAnalyzer
import com.example.processing.ImageInfoDetails

/**
 * Clean Architecture Use Case: Extracts deep image technical statistics, dimensions,
 * Megapixels, Aspect Ratio, Color Space, Dominant Swatches, and EXIF attributes.
 */
class AnalyzeImageUseCase {

    operator fun invoke(context: Context, uri: Uri?, bitmap: Bitmap?): ImageInfoDetails {
        return ImageInfoAnalyzer.analyze(context = context, uri = uri, bitmap = bitmap)
    }
}

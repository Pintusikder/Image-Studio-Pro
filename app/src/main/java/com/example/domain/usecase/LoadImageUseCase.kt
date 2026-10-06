package com.example.domain.usecase

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import com.example.processing.BitmapUtils

/**
 * Clean Architecture Use Case: Decodes full or preview Bitmaps safely from Content URIs,
 * preventing OutOfMemory errors through bounds-only pre-decoding and sub-sampling.
 */
class LoadImageUseCase {

    operator fun invoke(context: Context, uri: Uri, maxDimension: Int = 4096): Bitmap? {
        return BitmapUtils.decodeSampledBitmapFromUri(context, uri, maxDimension, maxDimension)
    }

    fun getHeader(context: Context, uri: Uri): BitmapUtils.ImageHeader? {
        return BitmapUtils.getImageHeader(context, uri)
    }
}

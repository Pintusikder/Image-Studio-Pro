package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DataObject
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.HighQuality
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Straighten
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.ExportFormat
import com.example.processing.FileSizeMode
import com.example.processing.FileSizeOptimizationResult
import com.example.ui.theme.LocalStudioPalette
import kotlin.math.abs

@Composable
fun ImageStatsBar(
    width: Int,
    height: Int,
    fileSizeBytes: Long,
    dpi: Int,
    format: ExportFormat,
    targetSizeKb: Int? = null,
    fileSizeMode: FileSizeMode = FileSizeMode.MAXIMUM_CEILING,
    fileSizeResult: FileSizeOptimizationResult? = null,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    val palette = LocalStudioPalette.current

    val formattedSize = if (fileSizeBytes > 0) {
        val kb = fileSizeBytes / 1024.0
        if (kb > 1024) String.format("%.2f MB", kb / 1024.0) else String.format("%.1f KB", kb)
    } else "Live Estimating"

    val printInW = if (dpi > 0) width.toFloat() / dpi else 0f
    val printInH = if (dpi > 0) height.toFloat() / dpi else 0f
    val printCmW = printInW * 2.54f
    val printCmH = printInH * 2.54f
    val megapixels = (width * height) / 1_000_000f

    // Size Pill title and value based on exact file size rule
    val sizePillLabel: String
    val sizePillValue: String
    val sizePillTint: Color

    if (targetSizeKb != null) {
        val isExact = fileSizeResult?.isExactByteMatch == true
        val isCompliant = fileSizeResult?.isCompliantWithCeiling ?: (fileSizeBytes <= targetSizeKb * 1024L)

        when (fileSizeMode) {
            FileSizeMode.MAXIMUM_CEILING -> {
                sizePillLabel = "C: File Size (≤ $targetSizeKb KB)"
                sizePillValue = if (isCompliant) "$formattedSize (Compliant)" else "$formattedSize (Exceeds Limit)"
                sizePillTint = if (isCompliant) palette.success else palette.error
            }
            FileSizeMode.TARGET_CLOSEST -> {
                sizePillLabel = "C: Closest Size (~$targetSizeKb KB)"
                val diffBytes = fileSizeResult?.differenceBytes ?: (fileSizeBytes - targetSizeKb * 1024L)
                val diffStr = if (diffBytes < 0) "-${abs(diffBytes)} B" else "+${diffBytes} B"
                sizePillValue = "$formattedSize ($diffStr)"
                sizePillTint = palette.warning
            }
            FileSizeMode.EXACT_BYTE_ALIGNMENT -> {
                sizePillLabel = "C: Exact Target"
                sizePillValue = if (isExact) "✓ Exact $targetSizeKb KB ($formattedSize)" else "Closest: $formattedSize"
                sizePillTint = if (isExact) palette.success else palette.warning
            }
        }
    } else {
        sizePillLabel = "C: File Size"
        sizePillValue = formattedSize
        sizePillTint = palette.warning
    }

    Row(
        modifier = modifier
            .horizontalScroll(scrollState)
            .padding(horizontal = 12.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        StatPill(
            icon = Icons.Default.Straighten,
            label = "A: Pixel Dimensions",
            value = "${width} × ${height} px",
            tint = palette.accentPrimary
        )
        StatPill(
            icon = Icons.Default.Print,
            label = "B: Print @ ${dpi} DPI",
            value = String.format("%.2f × %.2f in (%.1f × %.1f cm)", printInW, printInH, printCmW, printCmH),
            tint = palette.success
        )
        StatPill(
            icon = Icons.Default.Description,
            label = sizePillLabel,
            value = sizePillValue,
            tint = sizePillTint
        )
        StatPill(
            icon = Icons.Default.HighQuality,
            label = "Resolution",
            value = String.format("%.2f MP", megapixels),
            tint = palette.accentSecondary
        )
        StatPill(
            icon = Icons.Default.DataObject,
            label = "Format",
            value = format.extension.uppercase(),
            tint = palette.textPrimary
        )
    }
}

@Composable
private fun StatPill(
    icon: ImageVector,
    label: String,
    value: String,
    tint: Color
) {
    val palette = LocalStudioPalette.current
    Box(
        modifier = Modifier
            .heightIn(min = 38.dp)
            .background(palette.cardSurface, RoundedCornerShape(10.dp))
            .border(1.dp, palette.cardBorder, RoundedCornerShape(10.dp))
            .padding(horizontal = 10.dp, vertical = 6.dp)
            .semantics(mergeDescendants = true) {
                contentDescription = "$label: $value"
            }
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(14.dp)
            )
            Column {
                Text(
                    text = label.uppercase(),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = palette.textSecondary
                    )
                )
                Text(
                    text = value,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = palette.textPrimary
                    )
                )
            }
        }
    }
}

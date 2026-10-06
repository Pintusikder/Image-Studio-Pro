package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import com.example.model.OutputVerificationReport
import com.example.model.VerificationMetric
import com.example.model.VerificationStatus
import com.example.processing.ExportEngine

@Composable
fun ExportVerificationDialog(
    report: OutputVerificationReport,
    exportResult: ExportEngine.ExportResult,
    onDismiss: () -> Unit,
    onShare: (() -> Unit)? = null
) {
    val context = LocalContext.current

    AlertDialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
        modifier = Modifier
            .padding(16.dp)
            .fillMaxWidth()
            .testTag("verification_dialog"),
        shape = RoundedCornerShape(20.dp),
        containerColor = Color(0xFF0F172A),
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(Color(0xFF38BDF8).copy(alpha = 0.15f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Verified,
                            contentDescription = null,
                            tint = Color(0xFF38BDF8),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "EXACT OUTPUT VERIFICATION",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                fontSize = 16.sp
                            )
                        )
                        Text(
                            text = "Physical File & Metadata Audit",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = Color(0xFF94A3B8),
                                fontSize = 12.sp
                            )
                        )
                    }
                }
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.testTag("close_verification_dialog_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = Color(0xFF94A3B8)
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 480.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Top Overall Status Banner
                val bannerBg = when (report.overallStatus) {
                    VerificationStatus.PASS -> Color(0xFF052E16)
                    VerificationStatus.WITHIN_LIMIT -> Color(0xFF083344)
                    VerificationStatus.EXCEEDED, VerificationStatus.FAIL -> Color(0xFF450A0A)
                    VerificationStatus.UNVERIFIED, VerificationStatus.NOT_APPLICABLE -> Color(0xFF2E2005)
                }
                val bannerBorder = when (report.overallStatus) {
                    VerificationStatus.PASS -> Color(0xFF10B981)
                    VerificationStatus.WITHIN_LIMIT -> Color(0xFF06B6D4)
                    VerificationStatus.EXCEEDED, VerificationStatus.FAIL -> Color(0xFFEF4444)
                    VerificationStatus.UNVERIFIED, VerificationStatus.NOT_APPLICABLE -> Color(0xFFF59E0B)
                }
                val bannerText = when (report.overallStatus) {
                    VerificationStatus.PASS -> "✓ All measurable outputs rigorously verified"
                    VerificationStatus.WITHIN_LIMIT -> "✓ Output within requested target limits"
                    VerificationStatus.EXCEEDED -> "⚠️ File size constraint exceeded"
                    VerificationStatus.FAIL -> "❌ Output verification failed"
                    VerificationStatus.UNVERIFIED -> "⚠️ One or more parameters unverified (no false pass reported)"
                    VerificationStatus.NOT_APPLICABLE -> "Verification completed"
                }

                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    color = bannerBg,
                    border = BorderStroke(1.dp, bannerBorder.copy(alpha = 0.6f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = bannerText,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = Color.White,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.sp
                            ),
                            modifier = Modifier.weight(1f)
                        )
                        StatusBadge(status = report.overallStatus)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Verification Metric Cards
                VerificationMetricCard(
                    metric = report.dimensionsMetric,
                    tag = "verification_item_dimensions"
                )
                Spacer(modifier = Modifier.height(10.dp))

                VerificationMetricCard(
                    metric = report.fileSizeMetric,
                    tag = "verification_item_file_size"
                )
                Spacer(modifier = Modifier.height(10.dp))

                VerificationMetricCard(
                    metric = report.dpiMetric,
                    tag = "verification_item_dpi"
                )
                Spacer(modifier = Modifier.height(10.dp))

                VerificationMetricCard(
                    metric = report.formatMetric,
                    tag = "verification_item_format"
                )

                if (report.metadataMetric != null) {
                    Spacer(modifier = Modifier.height(10.dp))
                    VerificationMetricCard(
                        metric = report.metadataMetric,
                        tag = "verification_item_metadata"
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // File path disclosure
                if (exportResult.outputFile != null) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFF1E293B),
                        border = BorderStroke(1.dp, Color(0xFF334155))
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = "VERIFIED OUTPUT FILE",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = Color(0xFF64748B),
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp
                                )
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = exportResult.outputFile.name,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = Color(0xFFE2E8F0),
                                    fontWeight = FontWeight.Medium,
                                    fontFamily = FontFamily.Monospace
                                )
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        val clip = ClipData.newPlainText("Export Verification Report", report.toFormattedSummary())
                        clipboard.setPrimaryClip(clip)
                        Toast.makeText(context, "Verification report copied to clipboard", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("copy_verification_report_button"),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, Color(0xFF334155)),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF94A3B8))
                ) {
                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Copy Report", fontSize = 12.sp)
                }

                if (onShare != null && exportResult.outputFile != null) {
                    Button(
                        onClick = onShare,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("share_verified_file_button"),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF38BDF8))
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = null,
                            tint = Color(0xFF0F172A),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Share", color = Color(0xFF0F172A), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                } else {
                    Button(
                        onClick = onDismiss,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("done_verification_dialog_button"),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF38BDF8))
                    ) {
                        Text("Done", color = Color(0xFF0F172A), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }
        }
    )
}

@Composable
fun VerificationMetricCard(
    metric: VerificationMetric,
    tag: String
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag(tag),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
        border = BorderStroke(1.dp, Color(0xFF334155))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = metric.title,
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = Color(0xFF38BDF8),
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.8.sp
                    )
                )
                StatusBadge(status = metric.status)
            }

            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider(color = Color(0xFF334155).copy(alpha = 0.5f), thickness = 0.5.dp)
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Requested:",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = Color(0xFF64748B),
                            fontSize = 11.sp
                        )
                    )
                    Text(
                        text = metric.requested,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = Color(0xFFE2E8F0),
                            fontWeight = FontWeight.SemiBold,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 13.sp
                        )
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Actual:",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = Color(0xFF64748B),
                            fontSize = 11.sp
                        )
                    )
                    Text(
                        text = metric.actual,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = when (metric.status) {
                                VerificationStatus.PASS -> Color(0xFF10B981)
                                VerificationStatus.WITHIN_LIMIT -> Color(0xFF06B6D4)
                                VerificationStatus.EXCEEDED, VerificationStatus.FAIL -> Color(0xFFEF4444)
                                VerificationStatus.UNVERIFIED -> Color(0xFFF59E0B)
                                VerificationStatus.NOT_APPLICABLE -> Color(0xFFE2E8F0)
                            },
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 13.sp
                        )
                    )
                }
            }

            if (!metric.details.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = metric.details,
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = Color(0xFF94A3B8),
                        fontSize = 11.sp
                    )
                )
            }
        }
    }
}

@Composable
fun StatusBadge(status: VerificationStatus) {
    val (bg, fg, icon) = when (status) {
        VerificationStatus.PASS -> Triple(Color(0xFF064E3B), Color(0xFF34D399), Icons.Default.CheckCircle)
        VerificationStatus.WITHIN_LIMIT -> Triple(Color(0xFF083344), Color(0xFF38BDF8), Icons.Default.CheckCircle)
        VerificationStatus.EXCEEDED -> Triple(Color(0xFF7F1D1D), Color(0xFFF87171), Icons.Default.Warning)
        VerificationStatus.FAIL -> Triple(Color(0xFF7F1D1D), Color(0xFFF87171), Icons.Default.Error)
        VerificationStatus.UNVERIFIED -> Triple(Color(0xFF451A03), Color(0xFFFBBF24), Icons.Default.HelpOutline)
        VerificationStatus.NOT_APPLICABLE -> Triple(Color(0xFF1E293B), Color(0xFF94A3B8), Icons.Default.HelpOutline)
    }

    Surface(
        shape = RoundedCornerShape(6.dp),
        color = bg,
        border = BorderStroke(1.dp, fg.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = fg,
                modifier = Modifier.size(12.dp)
            )
            Text(
                text = status.displayName,
                style = MaterialTheme.typography.labelSmall.copy(
                    color = fg,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 11.sp
                )
            )
        }
    }
}

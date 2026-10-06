package com.example.model

enum class VerificationStatus(val displayName: String) {
    PASS("PASS"),
    WITHIN_LIMIT("WITHIN LIMIT"),
    EXCEEDED("EXCEEDED"),
    FAIL("FAIL"),
    UNVERIFIED("UNVERIFIED"),
    NOT_APPLICABLE("N/A")
}

data class VerificationMetric(
    val title: String,
    val requested: String,
    val actual: String,
    val status: VerificationStatus,
    val details: String? = null,
    val isMeasurable: Boolean = true
)

data class OutputVerificationReport(
    val dimensionsMetric: VerificationMetric,
    val fileSizeMetric: VerificationMetric,
    val dpiMetric: VerificationMetric,
    val formatMetric: VerificationMetric,
    val metadataMetric: VerificationMetric? = null,
    val allMeasurablePassed: Boolean,
    val overallStatus: VerificationStatus,
    val verifiedAtTimestamp: Long = System.currentTimeMillis()
) {
    val metrics: List<VerificationMetric> get() = listOfNotNull(
        dimensionsMetric,
        fileSizeMetric,
        dpiMetric,
        formatMetric,
        metadataMetric
    )

    fun toFormattedSummary(): String {
        val sb = StringBuilder()
        sb.appendLine("=== EXACT OUTPUT VERIFICATION ===")
        metrics.forEach { m ->
            sb.appendLine("${m.title}:")
            sb.appendLine("  Requested: ${m.requested}")
            sb.appendLine("  Actual:    ${m.actual}")
            sb.appendLine("  Status:    ${m.status.displayName}")
            if (!m.details.isNullOrBlank()) {
                sb.appendLine("  Note:      ${m.details}")
            }
        }
        sb.appendLine("Overall Status: ${overallStatus.displayName}")
        return sb.toString().trimEnd()
    }
}

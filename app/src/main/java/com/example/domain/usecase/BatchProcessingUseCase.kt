package com.example.domain.usecase

import android.content.Context
import android.net.Uri
import com.example.model.BatchConfig
import com.example.model.BatchFilterOption
import com.example.model.BatchItem
import com.example.model.BatchSortOption
import com.example.model.BatchStatus
import com.example.model.BatchSummaryReport
import com.example.model.OutputFileConfiguration
import com.example.processing.BatchProcessingEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.coroutineContext

/**
 * PHASE 9 — CLEAN ARCHITECTURE USE CASE FOR BATCH PROCESSING
 *
 * Orchestrates:
 * - Multi-select URI inspection & deduplication
 * - Batch Resize, Batch Compression, Batch Format Conversion, Batch Metadata & DPI
 * - Real-time per-item & overall progress updates
 * - Cooperative cancellation
 * - Retry failed/cancelled items (all or individual) & Reprocess completed items
 */
class BatchProcessingUseCase {

    fun inspectAndCreateItems(
        context: Context,
        uris: List<Uri>,
        existingItems: List<BatchItem> = emptyList(),
        skipDuplicates: Boolean = true
    ): List<BatchItem> {
        return BatchProcessingEngine.inspectAndCreateBatchItems(
            context = context,
            uris = uris,
            existingItems = existingItems,
            skipDuplicates = skipDuplicates
        )
    }

    fun computeTargetDimensions(
        srcWidth: Int,
        srcHeight: Int,
        config: BatchConfig
    ): Pair<Int, Int> {
        return BatchProcessingEngine.computeBatchTargetDimensions(srcWidth, srcHeight, config)
    }

    fun selectAll(items: List<BatchItem>): List<BatchItem> =
        items.map { it.copy(selected = true) }

    fun deselectAll(items: List<BatchItem>): List<BatchItem> =
        items.map { it.copy(selected = false) }

    fun invertSelection(items: List<BatchItem>): List<BatchItem> =
        items.map { it.copy(selected = !it.selected) }

    fun toggleSelection(items: List<BatchItem>, itemId: String): List<BatchItem> =
        items.map { if (it.id == itemId) it.copy(selected = !it.selected) else it }

    fun removeSelected(items: List<BatchItem>): List<BatchItem> =
        items.filter { !it.selected }

    fun prepareRetryFailedAndCancelled(items: List<BatchItem>): List<BatchItem> =
        items.map { item ->
            if (item.status == BatchStatus.FAILED || item.status == BatchStatus.CANCELLED) {
                item.copy(
                    status = BatchStatus.PENDING,
                    progress = 0f,
                    stageLabel = "Queued for retry",
                    errorMessage = null,
                    retryCount = item.retryCount + 1
                )
            } else {
                item
            }
        }

    fun prepareRetrySingleItem(items: List<BatchItem>, itemId: String): List<BatchItem> =
        items.map { item ->
            if (item.id == itemId) {
                item.copy(
                    status = BatchStatus.PENDING,
                    progress = 0f,
                    stageLabel = "Queued for retry",
                    errorMessage = null,
                    retryCount = item.retryCount + 1
                )
            } else {
                item
            }
        }

    fun prepareReprocessAll(items: List<BatchItem>): List<BatchItem> =
        items.map { item ->
            item.copy(
                status = BatchStatus.PENDING,
                progress = 0f,
                stageLabel = "Queued",
                errorMessage = null
            )
        }

    fun sortItems(items: List<BatchItem>, sortOption: BatchSortOption): List<BatchItem> =
        BatchProcessingEngine.sortBatchItems(items, sortOption)

    fun filterItems(items: List<BatchItem>, filterOption: BatchFilterOption): List<BatchItem> =
        BatchProcessingEngine.filterBatchItems(items, filterOption)

    fun computeSummary(items: List<BatchItem>): BatchSummaryReport =
        BatchProcessingEngine.computeSummaryReport(items)

    /**
     * Executes a batch processing run over [items] according to [config], emitting live updates
     * via [onProgress] and honoring [cancelFlag] + coroutine cancellation.
     */
    suspend fun executeBatch(
        context: Context,
        items: List<BatchItem>,
        config: BatchConfig,
        outputFileConfig: OutputFileConfiguration = OutputFileConfiguration(),
        cancelFlag: AtomicBoolean = AtomicBoolean(false),
        targetItemId: String? = null,
        onProgress: (
            updatedItems: List<BatchItem>,
            currentIndex: Int,
            currentFileName: String,
            currentStage: String,
            overallProgress: Float
        ) -> Unit = { _, _, _, _, _ -> }
    ): List<BatchItem> = withContext(Dispatchers.IO) {
        val workingList = items.toMutableList()
        val totalEligible = workingList.count { item ->
            when {
                targetItemId != null -> item.id == targetItemId
                config.processSelectedOnly -> item.selected && item.status != BatchStatus.COMPLETED
                else -> item.status != BatchStatus.COMPLETED
            }
        }.coerceAtLeast(1)

        var processedEligibleCount = 0

        for (i in workingList.indices) {
            if (cancelFlag.get()) break
            try {
                coroutineContext.ensureActive()
            } catch (e: Exception) {
                cancelFlag.set(true)
                break
            }

            val item = workingList[i]
            val shouldProcess = when {
                targetItemId != null -> item.id == targetItemId
                item.status == BatchStatus.COMPLETED -> false
                item.status == BatchStatus.CANCELLED || item.status == BatchStatus.FAILED -> false
                config.processSelectedOnly -> item.selected
                else -> true
            }

            if (!shouldProcess) continue

            workingList[i] = item.copy(
                status = BatchStatus.PROCESSING,
                progress = 0.05f,
                stageLabel = "Starting..."
            )
            val baseProgress = processedEligibleCount.toFloat() / totalEligible.toFloat()
            onProgress(workingList.toList(), i, item.originalName, "Starting...", baseProgress)

            val processedItem = BatchProcessingEngine.processSingleBatchItem(
                context = context,
                item = workingList[i],
                indexInBatch = i,
                config = config,
                outputFileConfig = outputFileConfig,
                cancelFlag = cancelFlag,
                onStageUpdate = { stage, itemProgress ->
                    workingList[i] = workingList[i].copy(
                        progress = itemProgress,
                        stageLabel = stage
                    )
                    val overall = ((processedEligibleCount.toFloat() + itemProgress) / totalEligible.toFloat()).coerceIn(0f, 1f)
                    onProgress(workingList.toList(), i, item.originalName, stage, overall)
                }
            )

            workingList[i] = processedItem
            processedEligibleCount++
            val overallAfter = (processedEligibleCount.toFloat() / totalEligible.toFloat()).coerceIn(0f, 1f)
            onProgress(workingList.toList(), i, item.originalName, processedItem.stageLabel, overallAfter)
        }

        if (cancelFlag.get()) {
            for (i in workingList.indices) {
                val cur = workingList[i]
                if (cur.status == BatchStatus.PROCESSING ||
                    (targetItemId == null && cur.status == BatchStatus.PENDING && (!config.processSelectedOnly || cur.selected))
                ) {
                    workingList[i] = cur.copy(
                        status = BatchStatus.CANCELLED,
                        progress = 0f,
                        stageLabel = "Cancelled",
                        errorMessage = "Processing cancelled"
                    )
                }
            }
        }

        workingList.toList()
    }
}

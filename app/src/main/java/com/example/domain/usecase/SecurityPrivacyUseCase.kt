package com.example.domain.usecase

import android.content.Context
import android.net.Uri
import com.example.data.local.UtilityRepository
import com.example.model.ExportFormat
import com.example.model.MetadataPolicy
import com.example.model.OverwriteConflictAction
import com.example.processing.SecurityPrivacyEngine
import java.io.File

/**
 * Clean Architecture Use Case for Phase 12 — Security + Privacy:
 * Coordinates full 8-domain security & privacy audits, URI & path validation,
 * zero-fill temporary cache purging, privacy-scrubbed sharing, and full local privacy resets.
 */
class SecurityPrivacyUseCase {

    fun runCompleteAudit(
        context: Context,
        defaultMetadataPolicy: MetadataPolicy = MetadataPolicy.KEEP_ALL,
        defaultOverwriteAction: OverwriteConflictAction = OverwriteConflictAction.ASK_BEFORE_OVERWRITE
    ): SecurityPrivacyEngine.SecurityPrivacyAuditReport {
        return SecurityPrivacyEngine.runCompleteSecurityPrivacyAudit(
            context = context,
            defaultMetadataPolicy = defaultMetadataPolicy,
            defaultOverwriteAction = defaultOverwriteAction
        )
    }

    fun auditTempCache(
        context: Context,
        staleThresholdHours: Int = 6
    ): SecurityPrivacyEngine.TempCacheAuditSummary {
        return SecurityPrivacyEngine.auditTemporaryFiles(context, staleThresholdHours)
    }

    fun purgeTemporaryCache(
        context: Context,
        zeroFill: Boolean = true,
        olderThanMs: Long = 0L
    ): SecurityPrivacyEngine.TempPurgeResult {
        return SecurityPrivacyEngine.purgeAllTemporaryFiles(
            context = context,
            zeroFill = zeroFill,
            olderThanMs = olderThanMs
        )
    }

    fun validateSafeFilePath(
        context: Context,
        pathOrUri: String,
        allowExternalStorage: Boolean = true
    ): SecurityPrivacyEngine.PathValidationResult {
        return SecurityPrivacyEngine.validateSafeFileAccess(
            context = context,
            rawPathOrFile = pathOrUri,
            allowExternalStorage = allowExternalStorage
        )
    }

    fun validateUri(
        context: Context,
        uri: Uri,
        verifyImageStream: Boolean = true
    ): SecurityPrivacyEngine.UriValidationResult {
        return SecurityPrivacyEngine.validateAndInspectUri(
            context = context,
            uri = uri,
            verifyImageStream = verifyImageStream
        )
    }

    fun createPrivacyScrubbedShareCopy(
        context: Context,
        sourceFile: File,
        format: ExportFormat = ExportFormat.JPEG
    ): File? {
        return SecurityPrivacyEngine.createPrivacyScrubbedShareFile(
            context = context,
            sourceFile = sourceFile,
            format = format
        )
    }

    suspend fun performFullPrivacyReset(
        context: Context,
        repository: UtilityRepository,
        clearHistory: Boolean = true,
        clearFavorites: Boolean = true,
        purgeTempCache: Boolean = true
    ): SecurityPrivacyEngine.TempPurgeResult {
        if (clearHistory) {
            repository.clearAllHistory()
        }
        if (clearFavorites) {
            repository.clearAllFavorites()
        }
        return if (purgeTempCache) {
            SecurityPrivacyEngine.purgeAllTemporaryFiles(context, zeroFill = true, olderThanMs = 0L)
        } else {
            SecurityPrivacyEngine.TempPurgeResult(0, 0L, 0, 0)
        }
    }
}

package com.neatfiles.app.domain.usecase

import com.neatfiles.app.core.model.CleanupOverview
import com.neatfiles.app.core.model.CleanupReason
import com.neatfiles.app.core.model.DuplicateGroup
import com.neatfiles.app.core.model.FileCategory
import com.neatfiles.app.core.model.NeatFile
import com.neatfiles.app.core.model.OrganizePlan
import com.neatfiles.app.core.model.SmartRenameItem
import com.neatfiles.app.core.model.StorageOverview
import com.neatfiles.app.core.util.HashEngine
import com.neatfiles.app.core.util.SmartRenameEngine
import com.neatfiles.app.domain.repository.FileRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ScanDownloadsUseCase(
    private val repository: FileRepository,
    private val detectDuplicatesUseCase: DetectDuplicatesUseCase,
    private val detectCleanupCandidatesUseCase: DetectCleanupCandidatesUseCase
) {
    suspend operator fun invoke(): StorageOverview = withContext(Dispatchers.IO) {
        repository.scanDownloads()
    }
}

class DetectDuplicatesUseCase {
    operator fun invoke(files: List<NeatFile>): List<DuplicateGroup> {
        // Group by size first for fast bucketing
        val sameSizeBuckets = files.filter { it.sizeBytes > 0 }
            .groupBy { it.sizeBytes }
            .filter { it.value.size > 1 }
        val duplicateGroups = mutableListOf<DuplicateGroup>()

        for ((size, bucket) in sameSizeBuckets) {
            val hashGroups = bucket.groupBy { file ->
                if (file.file.exists()) {
                    HashEngine.computeQuickHash(file.file)
                } else {
                    val base = file.name.substringBeforeLast(".")
                        .replace("""\s*\(\d+\)|\s*-\s*Copy|\s*\(copy\)|_duplicate""".toRegex(RegexOption.IGNORE_CASE), "")
                        .trim()
                    "${file.sizeBytes}_${base.lowercase()}.${file.extension.lowercase()}"
                }
            }.filter { it.value.size > 1 }

            for ((hash, group) in hashGroups) {
                // Sort by last modified: oldest is considered original, newer are redundant copies
                val sorted = group.sortedBy { it.lastModifiedMillis }
                val original = sorted.first()
                val redundant = sorted.drop(1)

                duplicateGroups.add(
                    DuplicateGroup(
                        id = "dup_${hash}_${size}",
                        fileHash = hash,
                        fileSize = size,
                        originalFile = original.copy(isOriginal = true, isDuplicate = false),
                        duplicateFiles = redundant.map {
                            it.copy(
                                isDuplicate = true,
                                isOriginal = false,
                                isSafeToRemove = true,
                                cleanupReason = CleanupReason.DUPLICATE_COPY
                            )
                        }
                    )
                )
            }
        }
        return duplicateGroups
    }
}

class DetectCleanupCandidatesUseCase {
    operator fun invoke(files: List<NeatFile>, oldDaysThreshold: Int = 30): CleanupOverview {
        val now = System.currentTimeMillis()
        val thresholdMillis = oldDaysThreshold * 24L * 60L * 60L * 1000L

        val duplicates = mutableListOf<NeatFile>()
        val oldFiles = mutableListOf<NeatFile>()
        val obsoleteInstallers = mutableListOf<NeatFile>()
        val tempFiles = mutableListOf<NeatFile>()

        for (file in files) {
            // 1. Temporary/Incomplete downloads
            if (file.extraDetails.isTempOrIncomplete ||
                file.extension.lowercase() in listOf("crdownload", "tmp", "part", "download")
            ) {
                tempFiles.add(file.copy(isSafeToRemove = true, cleanupReason = CleanupReason.INCOMPLETE_OR_TEMP))
                continue
            }

            // 2. Duplicate copies
            if (file.isDuplicate && !file.isOriginal) {
                duplicates.add(file.copy(isSafeToRemove = true, cleanupReason = CleanupReason.DUPLICATE_COPY))
                continue
            }

            // 3. Obsolete APK Installers
            if (file.category == FileCategory.INSTALLERS) {
                val age = now - file.lastModifiedMillis
                if (age > 7L * 24L * 60L * 60L * 1000L) { // Older than 7 days
                    obsoleteInstallers.add(
                        file.copy(isSafeToRemove = true, cleanupReason = CleanupReason.OBSOLETE_INSTALLER)
                    )
                    continue
                }
            }

            // 4. Old downloads untouched > threshold
            val age = now - file.lastModifiedMillis
            if (age > thresholdMillis) {
                oldFiles.add(
                    file.copy(isSafeToRemove = true, cleanupReason = CleanupReason.OLD_DOWNLOAD)
                )
            }
        }

        val allSafe = (duplicates + oldFiles + obsoleteInstallers + tempFiles).distinctBy { it.id }
        val totalBytes = allSafe.sumOf { it.sizeBytes }

        return CleanupOverview(
            totalSafeToRemoveCount = allSafe.size,
            totalReclaimableBytes = totalBytes,
            duplicates = duplicates,
            oldFiles = oldFiles,
            obsoleteInstallers = obsoleteInstallers,
            tempFiles = tempFiles
        )
    }
}

class SmartRenameUseCase {
    operator fun invoke(files: List<NeatFile>): List<SmartRenameItem> {
        val results = mutableListOf<SmartRenameItem>()
        for (file in files) {
            val eval = SmartRenameEngine.evaluate(file)
            if (eval.needsRenaming) {
                results.add(
                    SmartRenameItem(
                        file = file,
                        originalName = eval.originalName,
                        suggestedName = eval.suggestedName,
                        reason = eval.reason,
                        isSelected = true
                    )
                )
            }
        }
        return results
    }
}

class AutoOrganizeUseCase {
    operator fun invoke(
        files: List<NeatFile>,
        folderMappings: Map<FileCategory, String> = emptyMap()
    ): List<OrganizePlan> {
        // Only organize files sitting at the root of Downloads, skip already organized subfolders
        val looseFiles = files.filter { it.isRootLevel }

        return looseFiles.groupBy { it.category }
            .filter { it.value.isNotEmpty() }
            .map { (category, categoryFiles) ->
                val target = folderMappings[category] ?: category.folderName
                OrganizePlan(
                    targetFolder = target,
                    category = category,
                    filesToMove = categoryFiles,
                    totalBytes = categoryFiles.sumOf { it.sizeBytes }
                )
            }
    }
}

class PerformCleanupUseCase(private val repository: FileRepository) {
    suspend operator fun invoke(filesToDelete: List<NeatFile>): Result<Int> {
        return repository.deleteFiles(filesToDelete)
    }
}

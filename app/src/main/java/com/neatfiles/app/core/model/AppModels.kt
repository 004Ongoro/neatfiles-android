package com.neatfiles.app.core.model

/**
 * Group of duplicate files with one original and one or more redundant copies.
 */
data class DuplicateGroup(
    val id: String,
    val fileHash: String,
    val fileSize: Long,
    val originalFile: NeatFile,
    val duplicateFiles: List<NeatFile>
) {
    val wastedBytes: Long get() = fileSize * duplicateFiles.size
    val totalFiles: Int get() = 1 + duplicateFiles.size
}

/**
 * Cleanup group container for structured display in Review Cleanup screen.
 */
data class CleanupOverview(
    val totalSafeToRemoveCount: Int,
    val totalReclaimableBytes: Long,
    val duplicates: List<NeatFile>,
    val oldFiles: List<NeatFile>,
    val obsoleteInstallers: List<NeatFile>,
    val tempFiles: List<NeatFile>
) {
    val allCandidates: List<NeatFile>
        get() = (duplicates + oldFiles + obsoleteInstallers + tempFiles).distinctBy { it.id }
}

/**
 * Represents a smart rename suggestion comparing original name with neat name.
 */
data class SmartRenameItem(
    val file: NeatFile,
    val originalName: String,
    val suggestedName: String,
    val reason: String,
    val isSelected: Boolean = true
)

/**
 * Category breakdown item for dashboard and storage analysis.
 */
data class CategoryStat(
    val category: FileCategory,
    val count: Int,
    val totalBytes: Long,
    val percentageOfTotal: Float
)

/**
 * Complete storage metrics for Downloads and device storage.
 */
data class StorageOverview(
    val totalDownloadsFiles: Int,
    val totalDownloadsBytes: Long,
    val totalDeviceStorageBytes: Long,
    val freeDeviceStorageBytes: Long,
    val usedDeviceStorageBytes: Long,
    val categoryStats: List<CategoryStat>,
    val cleanupOverview: CleanupOverview
)

/**
 * Folder organization plan for moving loose files into neat directories.
 */
data class OrganizePlan(
    val targetFolder: String,
    val category: FileCategory,
    val filesToMove: List<NeatFile>,
    val totalBytes: Long
)

package com.neatfiles.app.domain.repository

import com.neatfiles.app.core.model.FileCategory
import com.neatfiles.app.core.model.NeatFile
import com.neatfiles.app.core.model.OrganizePlan
import com.neatfiles.app.core.model.StorageOverview
import kotlinx.coroutines.flow.Flow

interface FileRepository {
    fun getDownloadsFilesFlow(): Flow<List<NeatFile>>
    suspend fun scanDownloads(): StorageOverview
    suspend fun deleteFiles(files: List<NeatFile>): Result<Int>
    suspend fun renameFile(file: NeatFile, newName: String): Result<NeatFile>
    suspend fun organizeFiles(plans: List<OrganizePlan>): Result<Int>
    suspend fun getStorageOverview(): StorageOverview
    fun hasStoragePermission(): Boolean
}

interface PreferencesRepository {
    val isOnboardingCompleted: Flow<Boolean>
    val oldFileThresholdDays: Flow<Int>
    val isScheduledCleanupEnabled: Flow<Boolean>
    val cleanupIntervalHours: Flow<Int>
    val isAutoOrganizeEnabled: Flow<Boolean>
    val categoryFolderMappings: Flow<Map<FileCategory, String>>

    suspend fun setOnboardingCompleted(completed: Boolean)
    suspend fun setOldFileThresholdDays(days: Int)
    suspend fun setScheduledCleanupEnabled(enabled: Boolean)
    suspend fun setCleanupIntervalHours(hours: Int)
    suspend fun setAutoOrganizeEnabled(enabled: Boolean)
    suspend fun updateCategoryFolder(category: FileCategory, folderName: String)
}

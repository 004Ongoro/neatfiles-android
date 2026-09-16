package com.neatfiles.app.data.repository

import android.content.Context
import android.media.MediaScannerConnection
import android.os.Build
import android.os.Environment
import android.os.StatFs
import androidx.core.content.ContextCompat
import com.neatfiles.app.core.model.CategoryStat
import com.neatfiles.app.core.model.CleanupOverview
import com.neatfiles.app.core.model.FileCategory
import com.neatfiles.app.core.model.NeatFile
import com.neatfiles.app.core.model.OrganizePlan
import com.neatfiles.app.core.model.StorageOverview
import com.neatfiles.app.core.util.ContentInspector
import com.neatfiles.app.domain.repository.FileRepository
import com.neatfiles.app.domain.usecase.DetectCleanupCandidatesUseCase
import com.neatfiles.app.domain.usecase.DetectDuplicatesUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.nio.file.Files
import java.nio.file.StandardCopyOption

class FileRepositoryImpl(
    private val context: Context,
    private val detectDuplicatesUseCase: DetectDuplicatesUseCase = DetectDuplicatesUseCase(),
    private val detectCleanupCandidatesUseCase: DetectCleanupCandidatesUseCase = DetectCleanupCandidatesUseCase()
) : FileRepository {

    private val _filesFlow = MutableStateFlow<List<NeatFile>>(emptyList())

    override fun getDownloadsFilesFlow(): Flow<List<NeatFile>> = _filesFlow.asStateFlow()

    override fun hasStoragePermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Environment.isExternalStorageManager()
        } else {
            val read = ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.READ_EXTERNAL_STORAGE
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
            val write = ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
            read && write
        }
    }

    override suspend fun scanDownloads(): StorageOverview = withContext(Dispatchers.IO) {
        val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)

        if (!hasStoragePermission()) {
            _filesFlow.value = emptyList()
            return@withContext emptyStorageOverview()
        }

        if (!downloadsDir.exists()) {
            downloadsDir.mkdirs()
        }

        // Real file scan: root files and first-level subdirectories
        val scannedFiles = scanRealDirectory(downloadsDir)

        if (scannedFiles.isEmpty()) {
            _filesFlow.value = emptyList()
            return@withContext emptyStorageOverview()
        }

        // Run intelligence passes
        val duplicates = detectDuplicatesUseCase(scannedFiles)
        val duplicateMap = duplicates.flatMap { group ->
            group.duplicateFiles.map { it.id to group.id }
        }.toMap()

        val markedDuplicates = scannedFiles.map { file ->
            val groupId = duplicateMap[file.id]
            if (groupId != null) {
                file.copy(isDuplicate = true, isOriginal = false, duplicateGroupId = groupId)
            } else {
                file
            }
        }

        val cleanupOverview = detectCleanupCandidatesUseCase(markedDuplicates)
        val safeIds = cleanupOverview.allCandidates.associateBy { it.id }

        val fullyEnrichedFiles = markedDuplicates.map { file ->
            safeIds[file.id] ?: file
        }

        _filesFlow.value = fullyEnrichedFiles

        buildStorageOverview(fullyEnrichedFiles, cleanupOverview)
    }

    private fun scanRealDirectory(root: File): List<NeatFile> {
        val result = mutableListOf<NeatFile>()
        val directFiles = root.listFiles() ?: return emptyList()

        for (item in directFiles) {
            if (item.name.startsWith(".")) continue

            if (item.isFile) {
                addFileRecord(item, isRoot = true, result = result)
            } else if (item.isDirectory) {
                // Scan direct subfolders (e.g. Downloads/Documents, Downloads/Images)
                val subFiles = item.listFiles() ?: continue
                for (subItem in subFiles) {
                    if (subItem.isFile && !subItem.name.startsWith(".")) {
                        addFileRecord(subItem, isRoot = false, result = result)
                    }
                }
            }
        }
        return result
    }

    private fun addFileRecord(file: File, isRoot: Boolean, result: MutableList<NeatFile>) {
        val ext = file.extension
        val category = FileCategory.fromExtension(ext)
        val extra = ContentInspector.inspect(context, file, category)

        result.add(
            NeatFile(
                id = file.absolutePath.hashCode().toString(),
                name = file.name,
                path = file.absolutePath,
                sizeBytes = file.length(),
                lastModifiedMillis = file.lastModified(),
                category = category,
                mimeType = ext,
                extension = ext,
                isRootLevel = isRoot,
                extraDetails = extra
            )
        )
    }

    private fun emptyStorageOverview(): StorageOverview {
        val statFs = StatFs(Environment.getDataDirectory().path)
        val totalDevice = statFs.blockCountLong * statFs.blockSizeLong
        val freeDevice = statFs.availableBlocksLong * statFs.blockSizeLong
        val usedDevice = totalDevice - freeDevice

        return StorageOverview(
            totalDownloadsFiles = 0,
            totalDownloadsBytes = 0L,
            totalDeviceStorageBytes = if (totalDevice > 0) totalDevice else 128L * 1024 * 1024 * 1024,
            freeDeviceStorageBytes = if (freeDevice > 0) freeDevice else 44L * 1024 * 1024 * 1024,
            usedDeviceStorageBytes = if (usedDevice > 0) usedDevice else 84L * 1024 * 1024 * 1024,
            categoryStats = emptyList(),
            cleanupOverview = CleanupOverview(0, 0L, emptyList(), emptyList(), emptyList(), emptyList())
        )
    }

    private fun buildStorageOverview(
        files: List<NeatFile>,
        cleanupOverview: CleanupOverview
    ): StorageOverview {
        val totalBytes = files.sumOf { it.sizeBytes }

        val categoryStats = FileCategory.entries.mapNotNull { cat ->
            val catFiles = files.filter { it.category == cat }
            if (catFiles.isEmpty()) null
            else {
                val catBytes = catFiles.sumOf { it.sizeBytes }
                CategoryStat(
                    category = cat,
                    count = catFiles.size,
                    totalBytes = catBytes,
                    percentageOfTotal = if (totalBytes > 0) catBytes.toFloat() / totalBytes else 0f
                )
            }
        }

        val statFs = StatFs(Environment.getDataDirectory().path)
        val blockSize = statFs.blockSizeLong
        val totalBlocks = statFs.blockCountLong
        val availableBlocks = statFs.availableBlocksLong

        val totalDevice = totalBlocks * blockSize
        val freeDevice = availableBlocks * blockSize
        val usedDevice = totalDevice - freeDevice

        return StorageOverview(
            totalDownloadsFiles = files.size,
            totalDownloadsBytes = totalBytes,
            totalDeviceStorageBytes = if (totalDevice > 0) totalDevice else 128L * 1024 * 1024 * 1024,
            freeDeviceStorageBytes = if (freeDevice > 0) freeDevice else 44L * 1024 * 1024 * 1024,
            usedDeviceStorageBytes = if (usedDevice > 0) usedDevice else 84L * 1024 * 1024 * 1024,
            categoryStats = categoryStats,
            cleanupOverview = cleanupOverview
        )
    }

    override suspend fun deleteFiles(files: List<NeatFile>): Result<Int> = withContext(Dispatchers.IO) {
        try {
            var deletedCount = 0
            val deletedPaths = mutableListOf<String>()

            for (neatFile in files) {
                val realFile = File(neatFile.path)
                if (realFile.exists() && realFile.delete()) {
                    deletedCount++
                    deletedPaths.add(neatFile.path)
                }
            }

            if (deletedPaths.isNotEmpty()) {
                MediaScannerConnection.scanFile(context, deletedPaths.toTypedArray(), null, null)
            }

            // Rescan real storage to update all flows
            scanDownloads()
            Result.success(deletedCount)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun renameFile(file: NeatFile, newName: String): Result<NeatFile> = withContext(Dispatchers.IO) {
        try {
            val source = File(file.path)
            if (!source.exists()) {
                return@withContext Result.failure(IllegalStateException("File not found: ${file.path}"))
            }

            val parent = source.parentFile ?: return@withContext Result.failure(IllegalStateException("Parent dir null"))
            var target = File(parent, newName)

            val isCaseOnlyChange = source.name.equals(newName, ignoreCase = true) && source.name != newName

            // Resolve collision if target exists for a different file
            if (!isCaseOnlyChange && target.exists() && !target.canonicalPath.equals(source.canonicalPath, ignoreCase = true)) {
                val base = newName.substringBeforeLast(".")
                val ext = if (newName.contains(".")) ".${newName.substringAfterLast(".")}" else ""
                var counter = 1
                while (target.exists()) {
                    target = File(parent, "${base}_$counter$ext")
                    counter++
                }
            }

            var renamed = false
            if (isCaseOnlyChange) {
                val tempTarget = File(parent, "tmp_neat_${System.currentTimeMillis()}_${newName}")
                if (source.renameTo(tempTarget)) {
                    renamed = tempTarget.renameTo(target)
                }
            } else {
                renamed = source.renameTo(target)
            }

            if (!renamed) {
                // Fallback stream copy + delete for scoped storage limitations
                renamed = try {
                    source.inputStream().use { input ->
                        target.outputStream().use { output ->
                            input.copyTo(output)
                        }
                    }
                    source.delete()
                } catch (_: Exception) {
                    false
                }
            }

            if (renamed) {
                MediaScannerConnection.scanFile(
                    context,
                    arrayOf(source.absolutePath, target.absolutePath),
                    null,
                    null
                )
                val updated = file.copy(name = target.name, path = target.absolutePath)
                scanDownloads()
                Result.success(updated)
            } else {
                Result.failure(IllegalStateException("Failed to rename file on storage"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun organizeFiles(plans: List<OrganizePlan>): Result<Int> = withContext(Dispatchers.IO) {
        try {
            var movedCount = 0
            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            val scannedPaths = mutableListOf<String>()

            for (plan in plans) {
                val targetDir = File(downloadsDir, plan.targetFolder)
                if (!targetDir.exists()) {
                    targetDir.mkdirs()
                }

                for (neatFile in plan.filesToMove) {
                    val source = File(neatFile.path)
                    if (!source.exists()) continue

                    var dest = File(targetDir, neatFile.name)
                    // If file already exists in target folder, resolve collision
                    if (dest.exists() && dest.absolutePath != source.absolutePath) {
                        val base = neatFile.name.substringBeforeLast(".")
                        val ext = if (neatFile.name.contains(".")) ".${neatFile.name.substringAfterLast(".")}" else ""
                        var counter = 1
                        while (dest.exists()) {
                            dest = File(targetDir, "${base}_$counter$ext")
                            counter++
                        }
                    }

                    val moved = try {
                        Files.move(source.toPath(), dest.toPath(), StandardCopyOption.REPLACE_EXISTING)
                        true
                    } catch (_: Exception) {
                        if (source.renameTo(dest)) {
                            true
                        } else {
                            try {
                                source.inputStream().use { input ->
                                    dest.outputStream().use { output ->
                                        input.copyTo(output)
                                    }
                                }
                                if (dest.exists() && dest.length() == source.length()) {
                                    source.delete()
                                    true
                                } else {
                                    false
                                }
                            } catch (_: Exception) {
                                false
                            }
                        }
                    }

                    if (moved) {
                        scannedPaths.add(source.absolutePath)
                        scannedPaths.add(dest.absolutePath)
                        movedCount++
                    }
                }
            }

            if (scannedPaths.isNotEmpty()) {
                MediaScannerConnection.scanFile(context, scannedPaths.toTypedArray(), null, null)
            }

            scanDownloads()
            Result.success(movedCount)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getStorageOverview(): StorageOverview {
        return scanDownloads()
    }
}

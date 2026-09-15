package com.neatfiles.app.core.model

import java.io.File

/**
 * Core model representing a file in NeatFiles with intelligent classification metadata.
 */
data class NeatFile(
    val id: String,
    val name: String,
    val path: String,
    val sizeBytes: Long,
    val lastModifiedMillis: Long,
    val category: FileCategory,
    val mimeType: String,
    val extension: String,
    val isDuplicate: Boolean = false,
    val duplicateGroupId: String? = null,
    val isOriginal: Boolean = true,
    val isSafeToRemove: Boolean = false,
    val cleanupReason: CleanupReason? = null,
    val smartSuggestedName: String? = null,
    val targetOrganizeFolder: String? = null,
    val isRootLevel: Boolean = true,
    val extraDetails: FileExtraDetails = FileExtraDetails()
) {
    val file: File get() = File(path)
}

data class FileExtraDetails(
    val pdfPageCount: Int? = null,
    val pdfTitle: String? = null,
    val imageWidth: Int? = null,
    val imageHeight: Int? = null,
    val apkPackageName: String? = null,
    val apkVersionName: String? = null,
    val isTempOrIncomplete: Boolean = false
)

enum class FileCategory(
    val displayName: String,
    val folderName: String,
    val colorHex: Long
) {
    DOCUMENTS("Documents", "Documents", 0xFF1976D2),
    IMAGES("Images", "Images", 0xFFE91E63),
    INSTALLERS("Installers", "Installers", 0xFFFF9800),
    ARCHIVES("Archives", "Archives", 0xFF9C27B0),
    MEDIA("Media", "Media", 0xFF009688),
    CODE("Code", "Code", 0xFF607D8B),
    OTHERS("Others", "Misc", 0xFF795548);

    companion object {
        fun fromExtension(ext: String): FileCategory {
            return when (ext.lowercase()) {
                "pdf", "doc", "docx", "xls", "xlsx", "ppt", "pptx", "txt", "rtf", "odt", "csv" -> DOCUMENTS
                "jpg", "jpeg", "png", "gif", "webp", "bmp", "svg", "heic" -> IMAGES
                "apk", "xapk", "apkm", "aab" -> INSTALLERS
                "zip", "rar", "7z", "tar", "gz", "bz2" -> ARCHIVES
                "mp4", "mkv", "mov", "avi", "mp3", "m4a", "wav", "flac", "aac" -> MEDIA
                "kt", "java", "py", "js", "html", "css", "json", "xml", "c", "cpp", "rs", "go" -> CODE
                else -> OTHERS
            }
        }
    }
}

enum class CleanupReason(val title: String, val description: String) {
    DUPLICATE_COPY(
        "Duplicate File",
        "An identical copy of this file already exists in Downloads."
    ),
    OLD_DOWNLOAD(
        "Old Unused File",
        "Downloaded more than 30 days ago and not accessed since."
    ),
    OBSOLETE_INSTALLER(
        "Obsolete Installer",
        "APK files are usually safe to remove once the app is installed."
    ),
    INCOMPLETE_OR_TEMP(
        "Temp or Incomplete Download",
        "Unfinished or temporary cache file (.tmp, .crdownload)."
    )
}

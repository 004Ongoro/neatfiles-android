package com.neatfiles.app.core.util

import android.content.Context
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.os.Build
import com.neatfiles.app.core.model.FileCategory
import com.neatfiles.app.core.model.FileExtraDetails
import java.io.File
import java.io.FileInputStream
import java.nio.charset.StandardCharsets

object ContentInspector {

    fun inspect(context: Context, file: File, category: FileCategory): FileExtraDetails {
        if (!file.exists() || !file.canRead()) {
            return FileExtraDetails()
        }

        return when (category) {
            FileCategory.DOCUMENTS -> {
                if (file.extension.equals("pdf", ignoreCase = true)) {
                    inspectPdf(file)
                } else {
                    FileExtraDetails()
                }
            }
            FileCategory.IMAGES -> inspectImage(file)
            FileCategory.INSTALLERS -> inspectApk(context, file)
            FileCategory.OTHERS -> {
                val isTemp = isTempOrIncomplete(file)
                FileExtraDetails(isTempOrIncomplete = isTemp)
            }
            else -> FileExtraDetails()
        }
    }

    private fun inspectPdf(file: File): FileExtraDetails {
        var pageCount: Int? = null
        var title: String? = null

        try {
            // Read first 32KB and last 32KB to extract basic PDF info without heavy dependencies
            val length = file.length()
            val headerBytes = ByteArray(32768.coerceAtMost(length.toInt()))
            FileInputStream(file).use { fis ->
                fis.read(headerBytes)
            }
            val headerString = String(headerBytes, StandardCharsets.ISO_8859_1)

            // Look for /Title (Some Title)
            val titleRegex = """/Title\s*\(([^)]+)\)""".toRegex()
            titleRegex.find(headerString)?.let { match ->
                title = match.groupValues.getOrNull(1)?.trim()
            }

            // Approximate or parse page count via /Count
            val countRegex = """/Count\s+(\d+)""".toRegex()
            countRegex.findAll(headerString).lastOrNull()?.let { match ->
                pageCount = match.groupValues.getOrNull(1)?.toIntOrNull()
            }
        } catch (_: Exception) { }

        return FileExtraDetails(
            pdfPageCount = pageCount,
            pdfTitle = title
        )
    }

    private fun inspectImage(file: File): FileExtraDetails {
        return try {
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            BitmapFactory.decodeFile(file.absolutePath, options)
            if (options.outWidth > 0 && options.outHeight > 0) {
                FileExtraDetails(
                    imageWidth = options.outWidth,
                    imageHeight = options.outHeight
                )
            } else {
                FileExtraDetails()
            }
        } catch (_: Exception) {
            FileExtraDetails()
        }
    }

    private fun inspectApk(context: Context, file: File): FileExtraDetails {
        return try {
            val pm = context.packageManager
            val packageInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                pm.getPackageArchiveInfo(file.absolutePath, PackageManager.PackageInfoFlags.of(0))
            } else {
                @Suppress("DEPRECATION")
                pm.getPackageArchiveInfo(file.absolutePath, 0)
            }

            if (packageInfo != null) {
                FileExtraDetails(
                    apkPackageName = packageInfo.packageName,
                    apkVersionName = packageInfo.versionName ?: "v${packageInfo.longVersionCode}"
                )
            } else {
                FileExtraDetails()
            }
        } catch (_: Exception) {
            FileExtraDetails()
        }
    }

    fun isTempOrIncomplete(file: File): Boolean {
        val ext = file.extension.lowercase()
        val name = file.name.lowercase()
        return ext in listOf("crdownload", "tmp", "part", "download", "pending") ||
                name.endsWith(".crdownload") ||
                name.startsWith(".pending-") ||
                name.startsWith("tmp_")
    }
}

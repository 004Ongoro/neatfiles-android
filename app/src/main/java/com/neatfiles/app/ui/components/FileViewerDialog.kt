package com.neatfiles.app.ui.components

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.Image
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
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.FileProvider
import com.neatfiles.app.core.model.FileCategory
import com.neatfiles.app.core.model.NeatFile
import com.neatfiles.app.core.util.Formatters
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

@Composable
fun FileViewerDialog(
    file: NeatFile,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var previewBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var textPreviewContent by remember { mutableStateOf<String?>(null) }
    var isLoadingContent by remember { mutableStateOf(true) }

    val categoryColor = Color(file.category.colorHex)

    // Load in-app preview for images and text in background thread
    LaunchedEffect(file.path) {
        isLoadingContent = true
        withContext(Dispatchers.IO) {
            val realFile = File(file.path)
            if (realFile.exists()) {
                if (file.category == FileCategory.IMAGES) {
                    try {
                        val options = BitmapFactory.Options().apply {
                            inJustDecodeBounds = true
                        }
                        BitmapFactory.decodeFile(file.path, options)

                        // Sample down to fit dialog safely without memory pressure
                        var sampleSize = 1
                        val reqWidth = 800
                        val reqHeight = 800
                        while ((options.outWidth / sampleSize) > reqWidth || (options.outHeight / sampleSize) > reqHeight) {
                            sampleSize *= 2
                        }

                        val decodeOpts = BitmapFactory.Options().apply {
                            inSampleSize = sampleSize
                        }
                        previewBitmap = BitmapFactory.decodeFile(file.path, decodeOpts)
                    } catch (_: Exception) {
                        previewBitmap = null
                    }
                } else if (file.category == FileCategory.CODE || file.extension.lowercase() in listOf("txt", "log", "json", "csv", "xml", "md", "html")) {
                    try {
                        // Read first ~15 KB of text
                        val text = realFile.bufferedReader().use { reader ->
                            val buffer = CharArray(15000)
                            val read = reader.read(buffer, 0, buffer.size)
                            if (read > 0) String(buffer, 0, read) else ""
                        }
                        textPreviewContent = if (realFile.length() > 15000) "$text\n\n[... Preview truncated for large file ...]" else text
                    } catch (_: Exception) {
                        textPreviewContent = "Unable to read text preview."
                    }
                }
            }
        }
        isLoadingContent = false
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .clip(RoundedCornerShape(24.dp)),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(categoryColor.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = getCategoryIcon(file.category),
                                contentDescription = null,
                                tint = categoryColor,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = file.name,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1
                            )
                            Text(
                                text = "${file.category.displayName} • ${Formatters.formatFileSize(file.sizeBytes)}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // In-App Content Preview Area
                when {
                    file.category == FileCategory.IMAGES && previewBitmap != null -> {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 280.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color.Black.copy(alpha = 0.05f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Image(
                                bitmap = previewBitmap!!.asImageBitmap(),
                                contentDescription = file.name,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(16.dp))
                            )
                        }
                    }

                    textPreviewContent != null -> {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 260.dp),
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            )
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp)
                                    .verticalScroll(rememberScrollState())
                            ) {
                                Text(
                                    text = textPreviewContent!!,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 12.sp,
                                    lineHeight = 16.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }

                    else -> {
                        // Metadata card for PDFs, APKs, Media, Archives
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                            )
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                file.extraDetails.pdfTitle?.let { title ->
                                    DetailLine(label = "Embedded Title", value = title)
                                }
                                file.extraDetails.pdfPageCount?.let { count ->
                                    DetailLine(label = "Page Count", value = "$count pages")
                                }
                                file.extraDetails.apkPackageName?.let { pkg ->
                                    DetailLine(label = "Package", value = pkg)
                                }
                                file.extraDetails.apkVersionName?.let { ver ->
                                    DetailLine(label = "Version", value = ver)
                                }
                                DetailLine(label = "File Path", value = file.path)
                                DetailLine(
                                    label = "Last Modified",
                                    value = Formatters.formatRelativeAge(file.lastModifiedMillis)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Actions Bottom Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Close")
                    }

                    Button(
                        onClick = {
                            openFileInSystem(context, file)
                        },
                        modifier = Modifier.weight(1.5f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Open File", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun DetailLine(label: String, value: String) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

private fun openFileInSystem(context: Context, file: NeatFile) {
    try {
        val targetFile = File(file.path)
        if (!targetFile.exists()) {
            Toast.makeText(context, "File does not exist on storage.", Toast.LENGTH_SHORT).show()
            return
        }

        val uri: Uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            targetFile
        )

        val mime = getMimeType(file.extension)

        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, mime)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        context.startActivity(Intent.createChooser(intent, "Open ${file.name}"))
    } catch (_: ActivityNotFoundException) {
        Toast.makeText(context, "No app found to open this file type (${file.extension}).", Toast.LENGTH_LONG).show()
    } catch (e: Exception) {
        Toast.makeText(context, "Could not open file: ${e.message}", Toast.LENGTH_SHORT).show()
    }
}

private fun getMimeType(extension: String): String {
    return when (extension.lowercase()) {
        "pdf" -> "application/pdf"
        "jpg", "jpeg" -> "image/jpeg"
        "png" -> "image/png"
        "webp" -> "image/webp"
        "gif" -> "image/gif"
        "apk" -> "application/vnd.android.package-archive"
        "mp4" -> "video/mp4"
        "mkv" -> "video/x-matroska"
        "mp3" -> "audio/mpeg"
        "m4a" -> "audio/mp4"
        "txt", "log" -> "text/plain"
        "csv" -> "text/csv"
        "json" -> "application/json"
        "xml" -> "text/xml"
        "html" -> "text/html"
        "zip" -> "application/zip"
        else -> "*/*"
    }
}

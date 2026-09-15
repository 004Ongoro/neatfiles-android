package com.neatfiles.app.core.util

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.ln
import kotlin.math.pow

object Formatters {

    fun formatFileSize(bytes: Long): String {
        if (bytes <= 0) return "0 B"
        val units = arrayOf("B", "KB", "MB", "GB", "TB")
        val digitGroups = (ln(bytes.toDouble()) / ln(1024.0)).toInt().coerceIn(0, units.size - 1)
        val value = bytes / 1024.0.pow(digitGroups.toDouble())
        return String.format(Locale.getDefault(), "%.1f %s", value, units[digitGroups])
    }

    fun formatDate(epochMillis: Long): String {
        val sdf = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
        return sdf.format(Date(epochMillis))
    }

    fun formatRelativeAge(epochMillis: Long): String {
        val now = System.currentTimeMillis()
        val diffMillis = (now - epochMillis).coerceAtLeast(0)
        val days = diffMillis / (1000 * 60 * 60 * 24)

        return when {
            days == 0L -> "Today"
            days == 1L -> "Yesterday"
            days < 30 -> "$days days ago"
            days < 365 -> "${days / 30} months ago"
            else -> "${days / 365} years ago"
        }
    }
}

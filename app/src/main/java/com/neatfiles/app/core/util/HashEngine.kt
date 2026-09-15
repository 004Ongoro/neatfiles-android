package com.neatfiles.app.core.util

import java.io.File
import java.io.FileInputStream
import java.security.MessageDigest

object HashEngine {

    /**
     * Computes a quick hash over the first 8 KB of a file for fast candidate grouping.
     */
    fun computeQuickHash(file: File): String {
        if (!file.exists() || !file.canRead() || file.length() == 0L) return ""
        return try {
            val md = MessageDigest.getInstance("SHA-256")
            FileInputStream(file).use { fis ->
                val buffer = ByteArray(8192)
                val read = fis.read(buffer)
                if (read > 0) {
                    md.update(buffer, 0, read)
                }
            }
            // Append file length to quick hash to prevent header collisions
            bytesToHex(md.digest()) + "_" + file.length()
        } catch (e: Exception) {
            "${file.length()}_${file.name}"
        }
    }

    /**
     * Computes a full SHA-256 hash over the entire file contents.
     */
    fun computeFullHash(file: File): String {
        if (!file.exists() || !file.canRead() || file.length() == 0L) return ""
        return try {
            val md = MessageDigest.getInstance("SHA-256")
            FileInputStream(file).use { fis ->
                val buffer = ByteArray(32768)
                var bytesRead: Int
                while (fis.read(buffer).also { bytesRead = it } != -1) {
                    md.update(buffer, 0, bytesRead)
                }
            }
            bytesToHex(md.digest())
        } catch (e: Exception) {
            ""
        }
    }

    private fun bytesToHex(bytes: ByteArray): String {
        val hexChars = "0123456789abcdef"
        val result = StringBuilder(bytes.size * 2)
        for (b in bytes) {
            val i = b.toInt()
            result.append(hexChars[(i shr 4) and 0x0f])
            result.append(hexChars[i and 0x0f])
        }
        return result.toString()
    }
}

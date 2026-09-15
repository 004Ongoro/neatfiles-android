package com.neatfiles.app.core.util

import com.neatfiles.app.core.model.FileCategory
import com.neatfiles.app.core.model.NeatFile
import java.net.URLDecoder
import java.nio.charset.StandardCharsets

object SmartRenameEngine {

    data class RenameResult(
        val originalName: String,
        val suggestedName: String,
        val reason: String,
        val needsRenaming: Boolean
    )

    fun evaluate(file: NeatFile): RenameResult {
        val original = file.name
        val ext = file.extension
        val baseName = if (ext.isNotEmpty()) original.substringBeforeLast(".$ext") else original

        // 1. Check if PDF has a meaningful embedded title
        if (file.category == FileCategory.DOCUMENTS && ext.equals("pdf", ignoreCase = true)) {
            val pdfTitle = file.extraDetails.pdfTitle
            if (!pdfTitle.isNullOrBlank() &&
                pdfTitle.length > 3 &&
                !pdfTitle.contains("Untitled", ignoreCase = true) &&
                !pdfTitle.equals(baseName, ignoreCase = true)
            ) {
                val sanitized = sanitizeFilename(pdfTitle)
                if (sanitized.isNotBlank()) {
                    return RenameResult(
                        originalName = original,
                        suggestedName = "$sanitized.$ext",
                        reason = "Extracted document title from PDF",
                        needsRenaming = true
                    )
                }
            }
        }

        var cleaned = baseName

        // 2. Decode URL encoded artifacts like %20, %2B
        if (cleaned.contains("%") || cleaned.contains("+")) {
            try {
                cleaned = URLDecoder.decode(cleaned, StandardCharsets.UTF_8.name())
            } catch (_: Exception) { }
        }

        // 3. Remove repeated browser duplicate suffixes like (1), (2), (copy), _1
        val duplicateSuffixRegex = """[\s_]*\([0-9]+\)[\s_]*|[\s_]*\(copy\)|[\s_]+-\s+Copy""".toRegex(RegexOption.IGNORE_CASE)
        val strippedDuplicates = cleaned.replace(duplicateSuffixRegex, "").trim()
        if (strippedDuplicates.isNotEmpty()) {
            cleaned = strippedDuplicates
        }

        // 4. WhatsApp image pattern: IMG-20240315-WA0004 -> Photo_2024-03-15_WA04
        val waRegex = """IMG-(\d{4})(\d{2})(\d{2})-WA(\d+)""".toRegex(RegexOption.IGNORE_CASE)
        waRegex.find(cleaned)?.let { match ->
            val y = match.groupValues[1]
            val m = match.groupValues[2]
            val d = match.groupValues[3]
            val num = match.groupValues[4]
            return RenameResult(
                originalName = original,
                suggestedName = "Photo_${y}-${m}-${d}_WA${num.takeLast(4)}.$ext",
                reason = "Formatted WhatsApp photo date & index",
                needsRenaming = true
            )
        }

        // 5. Screenshot format: Screenshot_20240915-182030_Chrome -> Screenshot_Chrome_2024-09-15
        val screenshotRegex = """Screenshot_(\d{4})(\d{2})(\d{2})[-_](\d{6})_?([a-zA-Z0-9]+)?""".toRegex(RegexOption.IGNORE_CASE)
        screenshotRegex.find(cleaned)?.let { match ->
            val y = match.groupValues[1]
            val m = match.groupValues[2]
            val d = match.groupValues[3]
            val app = match.groupValues.getOrNull(5)
            val appLabel = if (!app.isNullOrBlank()) "${app}_" else ""
            return RenameResult(
                originalName = original,
                suggestedName = "Screenshot_${appLabel}${y}-${m}-${d}.$ext",
                reason = "Standardized screenshot name",
                needsRenaming = true
            )
        }

        // 6. Clean up messy underscores, dashes, trailing numbers, random hash prefixes
        val hashPrefixRegex = """^[a-f0-9]{16,32}[_-]?""".toRegex(RegexOption.IGNORE_CASE)
        if (hashPrefixRegex.containsMatchIn(cleaned)) {
            val unhashed = cleaned.replace(hashPrefixRegex, "").trim()
            if (unhashed.length >= 3) {
                cleaned = unhashed
            }
        }

        // Replace multiple consecutive underscores/hyphens with a single space or neat underscore
        cleaned = cleaned.replace("""[_\-\s]+""".toRegex(), " ").trim()

        // Capitalize words nicely if all lowercase
        if (cleaned.isNotEmpty() && cleaned == cleaned.lowercase()) {
            cleaned = cleaned.split(" ").joinToString(" ") { word ->
                word.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
            }
        }

        val finalName = if (ext.isNotEmpty()) "$cleaned.$ext" else cleaned

        val needsRenaming = !finalName.equals(original, ignoreCase = false) && cleaned.isNotBlank()

        return RenameResult(
            originalName = original,
            suggestedName = if (needsRenaming) finalName else original,
            reason = if (needsRenaming) "Cleaned duplicate suffixes and symbols" else "Name is already clean",
            needsRenaming = needsRenaming
        )
    }

    private fun sanitizeFilename(name: String): String {
        return name.replace("""[\\/:*?"<>|\r\n\t]""".toRegex(), " ")
            .replace("""\s+""".toRegex(), " ")
            .trim()
    }
}

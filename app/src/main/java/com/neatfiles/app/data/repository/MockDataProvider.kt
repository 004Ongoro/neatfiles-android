package com.neatfiles.app.data.repository

import com.neatfiles.app.core.model.CategoryStat
import com.neatfiles.app.core.model.CleanupOverview
import com.neatfiles.app.core.model.CleanupReason
import com.neatfiles.app.core.model.FileCategory
import com.neatfiles.app.core.model.FileExtraDetails
import com.neatfiles.app.core.model.NeatFile
import com.neatfiles.app.core.model.StorageOverview

/**
 * Realistic Mock Data Provider matching user specification:
 * Exactly: 142 files total, Documents 38, Images 51, Installers 12, Archives 15, Media 14, Code 8, Others 4.
 * Exactly: 31 files safe to remove!
 */
object MockDataProvider {

    fun createMockOverview(): StorageOverview {
        val files = createMockFiles()
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

        val duplicates = files.filter { it.cleanupReason == CleanupReason.DUPLICATE_COPY }
        val oldFiles = files.filter { it.cleanupReason == CleanupReason.OLD_DOWNLOAD }
        val installers = files.filter { it.cleanupReason == CleanupReason.OBSOLETE_INSTALLER }
        val tempFiles = files.filter { it.cleanupReason == CleanupReason.INCOMPLETE_OR_TEMP }
        val allSafe = files.filter { it.isSafeToRemove }

        val cleanupOverview = CleanupOverview(
            totalSafeToRemoveCount = allSafe.size, // 31 files
            totalReclaimableBytes = allSafe.sumOf { it.sizeBytes },
            duplicates = duplicates,
            oldFiles = oldFiles,
            obsoleteInstallers = installers,
            tempFiles = tempFiles
        )

        val totalDeviceStorage = 128L * 1024L * 1024L * 1024L // 128 GB
        val usedDeviceStorage = 84L * 1024L * 1024L * 1024L   // 84 GB
        val freeDeviceStorage = totalDeviceStorage - usedDeviceStorage

        return StorageOverview(
            totalDownloadsFiles = files.size, // 142 files
            totalDownloadsBytes = totalBytes,
            totalDeviceStorageBytes = totalDeviceStorage,
            freeDeviceStorageBytes = freeDeviceStorage,
            usedDeviceStorageBytes = usedDeviceStorage,
            categoryStats = categoryStats,
            cleanupOverview = cleanupOverview
        )
    }

    fun createMockFiles(): List<NeatFile> {
        val list = mutableListOf<NeatFile>()
        val now = System.currentTimeMillis()
        val dayMillis = 24L * 60L * 60L * 1000L

        var fileIndex = 1

        // -------------------------------------------------------------
        // 1. DOCUMENTS: 38 files (Safe to remove: 7 old + 4 duplicates = 11)
        // -------------------------------------------------------------
        val docNames = listOf(
            "Bank_Statement_Jan_2024.pdf" to (3L * 1024 * 1024),
            "Bank_Statement_Jan_2024 (1).pdf" to (3L * 1024 * 1024), // DUPLICATE
            "Contract_Agreement_Signed.pdf" to (5L * 1024 * 1024),
            "Contract_Agreement_Signed (copy).pdf" to (5L * 1024 * 1024), // DUPLICATE
            "Invoice_INV99281.pdf" to (850 * 1024L),
            "Invoice_INV99281_duplicate.pdf" to (850 * 1024L), // DUPLICATE
            "Salary_Slip_June_2024.pdf" to (420 * 1024L),
            "Salary_Slip_June_2024(1).pdf" to (420 * 1024L), // DUPLICATE
            "Flight_Ticket_NYC_2023.pdf" to (1L * 1024 * 1024), // OLD (90 days)
            "Hotel_Booking_Confirmation_2023.pdf" to (920 * 1024L), // OLD
            "Old_Utility_Bill_Oct2023.pdf" to (350 * 1024L), // OLD
            "Tax_Assessment_2022_Draft.pdf" to (2L * 1024 * 1024), // OLD
            "Medical_Receipt_2023_Nov.pdf" to (680 * 1024L), // OLD
            "Conference_Schedule_Archive_2023.pdf" to (1200 * 1024L), // OLD
            "Old_Resume_Draft_v1.docx" to (180 * 1024L), // OLD
            "Project_Proposal_2025.docx" to (2L * 1024 * 1024),
            "Meeting_Notes_Quarterly.docx" to (95 * 1024L),
            "Product_Roadmap_Q3.xlsx" to (3L * 1024 * 1024),
            "Financial_Projections_v2.xlsx" to (4L * 1024 * 1024),
            "Sprint_Planning_Tasks.xlsx" to (280 * 1024L),
            "Investor_Pitch_Deck.pptx" to (14L * 1024 * 1024),
            "Keynote_Presentation.pptx" to (22L * 1024 * 1024),
            "Research_Paper_AI_Agents.pdf" to (4L * 1024 * 1024),
            "Architecture_Overview_v4.pdf" to (6L * 1024 * 1024),
            "Apartment_Lease_2025.pdf" to (1800 * 1024L),
            "Passport_Scan_Official.pdf" to (3500 * 1024L),
            "Driver_License_Copy.pdf" to (2100 * 1024L),
            "Health_Insurance_Policy.pdf" to (4500 * 1024L),
            "Gym_Membership_Agreement.pdf" to (650 * 1024L),
            "Course_Syllabus_CS50.pdf" to (1200 * 1024L),
            "API_Documentation_Draft.pdf" to (2400 * 1024L),
            "Database_Schema_Diagram.pdf" to (1500 * 1024L),
            "Client_Brief_Redesign.docx" to (540 * 1024L),
            "NDA_Standard_Template.docx" to (210 * 1024L),
            "Expense_Report_Aug_2025.xlsx" to (890 * 1024L),
            "User_Interview_Transcripts.txt" to (75 * 1024L),
            "Quick_Notes_Meeting.txt" to (12 * 1024L),
            "Customer_Feedback_Export.csv" to (3500 * 1024L)
        )

        for ((index, pair) in docNames.withIndex()) {
            val (name, size) = pair
            val isDuplicate = name.contains("(1)") || name.contains("(copy)") || name.contains("duplicate")
            val isOld = name.contains("2023") || name.contains("2022") || name.contains("Old_")
            val isSafe = isDuplicate || isOld
            val reason = if (isDuplicate) CleanupReason.DUPLICATE_COPY else if (isOld) CleanupReason.OLD_DOWNLOAD else null

            val ageDays = if (isOld) 120L else (index + 2L)
            list.add(
                NeatFile(
                    id = "doc_${fileIndex++}",
                    name = name,
                    path = "/storage/emulated/0/Download/$name",
                    sizeBytes = size,
                    lastModifiedMillis = now - (ageDays * dayMillis),
                    category = FileCategory.DOCUMENTS,
                    mimeType = "application/pdf",
                    extension = name.substringAfterLast("."),
                    isDuplicate = isDuplicate,
                    isOriginal = !isDuplicate,
                    isSafeToRemove = isSafe,
                    cleanupReason = reason,
                    smartSuggestedName = if (isDuplicate) name.replace("""\s*\(1\)|\s*\(copy\)|\s*_duplicate""".toRegex(), "") else null,
                    extraDetails = FileExtraDetails(pdfPageCount = (index % 12) + 1, pdfTitle = name.substringBeforeLast("."))
                )
            )
        }

        // -------------------------------------------------------------
        // 2. IMAGES: 51 files (Safe to remove: 6 duplicates + 4 old screenshots = 10)
        // -------------------------------------------------------------
        for (i in 1..51) {
            val isDuplicate = i in listOf(5, 6, 12, 18, 25, 30)
            val isOldScreenshot = i in listOf(40, 41, 42, 43)
            val isSafe = isDuplicate || isOldScreenshot
            val reason = if (isDuplicate) CleanupReason.DUPLICATE_COPY else if (isOldScreenshot) CleanupReason.OLD_DOWNLOAD else null

            val rawName = when {
                isDuplicate -> "IMG-20240315-WA000${i % 9} (1).jpeg"
                isOldScreenshot -> "Screenshot_20231102-140${i}_Chrome.png"
                i % 4 == 0 -> "IMG-20240910-WA00${i}.jpeg"
                i % 4 == 1 -> "Screenshot_20240914-1920${i}_Instagram.png"
                i % 4 == 2 -> "Photo_Vacation_Sunset_${i}.jpg"
                else -> "Design_Mockup_V${i}.png"
            }

            val size = (1200 + (i * 150)) * 1024L
            val ageDays = if (isOldScreenshot) 150L else (i % 25 + 1L)

            list.add(
                NeatFile(
                    id = "img_${fileIndex++}",
                    name = rawName,
                    path = "/storage/emulated/0/Download/$rawName",
                    sizeBytes = size,
                    lastModifiedMillis = now - (ageDays * dayMillis),
                    category = FileCategory.IMAGES,
                    mimeType = "image/jpeg",
                    extension = rawName.substringAfterLast("."),
                    isDuplicate = isDuplicate,
                    isOriginal = !isDuplicate,
                    isSafeToRemove = isSafe,
                    cleanupReason = reason,
                    smartSuggestedName = "Photo_2024_${i}.jpg",
                    extraDetails = FileExtraDetails(imageWidth = 1920, imageHeight = 1080)
                )
            )
        }

        // -------------------------------------------------------------
        // 3. INSTALLERS: 12 files (Safe to remove: 8 obsolete APKs)
        // -------------------------------------------------------------
        val apkNames = listOf(
            "whatsapp_v2.24.18.75.apk" to true,
            "spotify_v8.9.60.apk" to true,
            "telegram_v10.14.0.apk" to true,
            "firefox_nightly_132.apk" to true,
            "vlc_player_v3.5.4.apk" to true,
            "subway_surfers_mod.apk" to true,
            "old_test_debug_build.apk" to true,
            "sample_camera_app_debug.apk" to true,
            "latest_beta_release_v2.apk" to false,
            "current_company_portal.apk" to false,
            "internal_vpn_tool.apk" to false,
            "security_token_v4.apk" to false
        )

        for ((apkName, isObsolete) in apkNames) {
            val size = (35 + (fileIndex % 50)) * 1024L * 1024L
            val ageDays = if (isObsolete) 45L else 3L

            list.add(
                NeatFile(
                    id = "apk_${fileIndex++}",
                    name = apkName,
                    path = "/storage/emulated/0/Download/$apkName",
                    sizeBytes = size,
                    lastModifiedMillis = now - (ageDays * dayMillis),
                    category = FileCategory.INSTALLERS,
                    mimeType = "application/vnd.android.package-archive",
                    extension = "apk",
                    isDuplicate = false,
                    isOriginal = true,
                    isSafeToRemove = isObsolete,
                    cleanupReason = if (isObsolete) CleanupReason.OBSOLETE_INSTALLER else null,
                    extraDetails = FileExtraDetails(
                        apkPackageName = "com.sample." + apkName.substringBefore("_"),
                        apkVersionName = "v1.0"
                    )
                )
            )
        }

        // -------------------------------------------------------------
        // 4. ARCHIVES: 15 files
        // -------------------------------------------------------------
        for (i in 1..15) {
            val name = "Dataset_Backup_Archive_$i.zip"
            list.add(
                NeatFile(
                    id = "arc_${fileIndex++}",
                    name = name,
                    path = "/storage/emulated/0/Download/$name",
                    sizeBytes = (45 + i * 10) * 1024L * 1024L,
                    lastModifiedMillis = now - (i * 3L * dayMillis),
                    category = FileCategory.ARCHIVES,
                    mimeType = "application/zip",
                    extension = "zip"
                )
            )
        }

        // -------------------------------------------------------------
        // 5. MEDIA: 14 files
        // -------------------------------------------------------------
        for (i in 1..14) {
            val name = if (i % 2 == 0) "Tutorial_Video_Ep$i.mp4" else "Podcast_Recording_Ep$i.mp3"
            list.add(
                NeatFile(
                    id = "med_${fileIndex++}",
                    name = name,
                    path = "/storage/emulated/0/Download/$name",
                    sizeBytes = (15 + i * 8) * 1024L * 1024L,
                    lastModifiedMillis = now - (i * 2L * dayMillis),
                    category = FileCategory.MEDIA,
                    mimeType = if (i % 2 == 0) "video/mp4" else "audio/mp3",
                    extension = name.substringAfterLast(".")
                )
            )
        }

        // -------------------------------------------------------------
        // 6. CODE: 8 files
        // -------------------------------------------------------------
        for (i in 1..8) {
            val name = "Script_Automation_v$i.py"
            list.add(
                NeatFile(
                    id = "code_${fileIndex++}",
                    name = name,
                    path = "/storage/emulated/0/Download/$name",
                    sizeBytes = (12 + i * 5) * 1024L,
                    lastModifiedMillis = now - (i * dayMillis),
                    category = FileCategory.CODE,
                    mimeType = "text/x-python",
                    extension = "py"
                )
            )
        }

        // -------------------------------------------------------------
        // 7. OTHERS: 4 files (Safe to remove: 2 incomplete .crdownload)
        // -------------------------------------------------------------
        val others = listOf(
            "Large_Ubuntu_ISO.crdownload" to true,
            "Unfinished_Game_Asset.tmp" to true,
            "custom_config_settings.bin" to false,
            "raw_log_export.dat" to false
        )

        for ((name, isTemp) in others) {
            list.add(
                NeatFile(
                    id = "oth_${fileIndex++}",
                    name = name,
                    path = "/storage/emulated/0/Download/$name",
                    sizeBytes = (80 + fileIndex * 15) * 1024L * 1024L,
                    lastModifiedMillis = now - (10L * dayMillis),
                    category = FileCategory.OTHERS,
                    mimeType = "application/octet-stream",
                    extension = name.substringAfterLast("."),
                    isSafeToRemove = isTemp,
                    cleanupReason = if (isTemp) CleanupReason.INCOMPLETE_OR_TEMP else null,
                    extraDetails = FileExtraDetails(isTempOrIncomplete = isTemp)
                )
            )
        }

        // Total check:
        // Documents: 38 (11 safe: 4 dups + 7 old)
        // Images: 51 (10 safe: 6 dups + 4 old)
        // Installers: 12 (8 safe: 8 obsolete APKs)
        // Archives: 15 (0 safe)
        // Media: 14 (0 safe)
        // Code: 8 (0 safe)
        // Others: 4 (2 safe: 2 temp)
        // Total files: 38 + 51 + 12 + 15 + 14 + 8 + 4 = 142 files!
        // Total safe to remove: 11 + 10 + 8 + 0 + 0 + 0 + 2 = 31 files!
        return list
    }
}

package com.neatfiles.app

import com.neatfiles.app.core.model.FileCategory
import com.neatfiles.app.core.model.NeatFile
import com.neatfiles.app.core.util.SmartRenameEngine
import com.neatfiles.app.domain.usecase.AutoOrganizeUseCase
import com.neatfiles.app.domain.usecase.DetectCleanupCandidatesUseCase
import com.neatfiles.app.domain.usecase.DetectDuplicatesUseCase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NeatFilesLogicTest {

    @Test
    fun testDuplicateDetectionGrouping() {
        val detectDuplicates = DetectDuplicatesUseCase()
        val now = System.currentTimeMillis()

        val files = listOf(
            NeatFile(
                id = "1",
                name = "Statement.pdf",
                path = "/storage/Download/Statement.pdf",
                sizeBytes = 1024,
                lastModifiedMillis = now - 10000,
                category = FileCategory.DOCUMENTS,
                mimeType = "application/pdf",
                extension = "pdf"
            ),
            NeatFile(
                id = "2",
                name = "Statement (1).pdf",
                path = "/storage/Download/Statement (1).pdf",
                sizeBytes = 1024,
                lastModifiedMillis = now - 5000,
                category = FileCategory.DOCUMENTS,
                mimeType = "application/pdf",
                extension = "pdf"
            ),
            NeatFile(
                id = "3",
                name = "DifferentFile.pdf",
                path = "/storage/Download/DifferentFile.pdf",
                sizeBytes = 2048,
                lastModifiedMillis = now,
                category = FileCategory.DOCUMENTS,
                mimeType = "application/pdf",
                extension = "pdf"
            )
        )

        val duplicates = detectDuplicates(files)
        assertEquals(1, duplicates.size)
        assertEquals("1", duplicates[0].originalFile.id)
        assertEquals(1, duplicates[0].duplicateFiles.size)
        assertEquals("2", duplicates[0].duplicateFiles[0].id)
        assertTrue(duplicates[0].duplicateFiles[0].isDuplicate)
    }

    @Test
    fun testCleanupCandidatesDetection() {
        val detectCleanup = DetectCleanupCandidatesUseCase()
        val now = System.currentTimeMillis()
        val dayMillis = 24L * 60L * 60L * 1000L

        val files = listOf(
            NeatFile(
                id = "old_doc",
                name = "OldDoc.pdf",
                path = "/storage/Download/OldDoc.pdf",
                sizeBytes = 5000,
                lastModifiedMillis = now - (45 * dayMillis), // 45 days old (> 30)
                category = FileCategory.DOCUMENTS,
                mimeType = "application/pdf",
                extension = "pdf"
            ),
            NeatFile(
                id = "new_doc",
                name = "NewDoc.pdf",
                path = "/storage/Download/NewDoc.pdf",
                sizeBytes = 3000,
                lastModifiedMillis = now - (2 * dayMillis),
                category = FileCategory.DOCUMENTS,
                mimeType = "application/pdf",
                extension = "pdf"
            ),
            NeatFile(
                id = "temp_file",
                name = "Incomplete.crdownload",
                path = "/storage/Download/Incomplete.crdownload",
                sizeBytes = 8000,
                lastModifiedMillis = now,
                category = FileCategory.OTHERS,
                mimeType = "application/octet-stream",
                extension = "crdownload"
            ),
            NeatFile(
                id = "old_apk",
                name = "AppInstaller.apk",
                path = "/storage/Download/AppInstaller.apk",
                sizeBytes = 40000,
                lastModifiedMillis = now - (15 * dayMillis),
                category = FileCategory.INSTALLERS,
                mimeType = "application/vnd.android.package-archive",
                extension = "apk"
            )
        )

        val result = detectCleanup(files, oldDaysThreshold = 30)
        assertEquals(3, result.totalSafeToRemoveCount)
        assertEquals(1, result.oldFiles.size)
        assertEquals("old_doc", result.oldFiles[0].id)
        assertEquals(1, result.tempFiles.size)
        assertEquals("temp_file", result.tempFiles[0].id)
        assertEquals(1, result.obsoleteInstallers.size)
        assertEquals("old_apk", result.obsoleteInstallers[0].id)
    }

    @Test
    fun testSmartRenameEnginePatterns() {
        // 1. Suffix stripping
        val file1 = NeatFile(
            id = "1",
            name = "Invoice_2024 (1).pdf",
            path = "/storage/Download/Invoice_2024 (1).pdf",
            sizeBytes = 100,
            lastModifiedMillis = 0,
            category = FileCategory.DOCUMENTS,
            mimeType = "pdf",
            extension = "pdf"
        )
        val res1 = SmartRenameEngine.evaluate(file1)
        assertTrue(res1.needsRenaming)
        assertEquals("Invoice 2024.pdf", res1.suggestedName)

        // 2. WhatsApp photo formatting
        val file2 = NeatFile(
            id = "2",
            name = "IMG-20240315-WA0004.jpeg",
            path = "/storage/Download/IMG-20240315-WA0004.jpeg",
            sizeBytes = 100,
            lastModifiedMillis = 0,
            category = FileCategory.IMAGES,
            mimeType = "jpeg",
            extension = "jpeg"
        )
        val res2 = SmartRenameEngine.evaluate(file2)
        assertTrue(res2.needsRenaming)
        assertEquals("Photo_2024-03-15_WA0004.jpeg", res2.suggestedName)

        // 3. Clean name remains untouched
        val file3 = NeatFile(
            id = "3",
            name = "Annual Report 2024.pdf",
            path = "/storage/Download/Annual Report 2024.pdf",
            sizeBytes = 100,
            lastModifiedMillis = 0,
            category = FileCategory.DOCUMENTS,
            mimeType = "pdf",
            extension = "pdf"
        )
        val res3 = SmartRenameEngine.evaluate(file3)
        assertFalse(res3.needsRenaming)
    }

    @Test
    fun testAutoOrganizeGrouping() {
        val organize = AutoOrganizeUseCase()
        val files = listOf(
            NeatFile(
                id = "1",
                name = "Doc1.pdf",
                path = "/storage/Download/Doc1.pdf",
                sizeBytes = 100,
                lastModifiedMillis = 0,
                category = FileCategory.DOCUMENTS,
                mimeType = "pdf",
                extension = "pdf"
            ),
            NeatFile(
                id = "2",
                name = "Photo1.jpg",
                path = "/storage/Download/Photo1.jpg",
                sizeBytes = 200,
                lastModifiedMillis = 0,
                category = FileCategory.IMAGES,
                mimeType = "jpg",
                extension = "jpg"
            )
        )

        val plans = organize(files)
        assertEquals(2, plans.size)
        val docPlan = plans.find { it.category == FileCategory.DOCUMENTS }
        assertEquals("Documents", docPlan?.targetFolder)
        assertEquals(1, docPlan?.filesToMove?.size)
    }
}

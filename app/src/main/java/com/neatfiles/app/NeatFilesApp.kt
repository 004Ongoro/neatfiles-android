package com.neatfiles.app

import android.app.Application
import com.neatfiles.app.data.repository.FileRepositoryImpl
import com.neatfiles.app.data.repository.PreferencesRepositoryImpl
import com.neatfiles.app.data.worker.ScheduledCleanupWorker
import com.neatfiles.app.domain.repository.FileRepository
import com.neatfiles.app.domain.repository.PreferencesRepository
import com.neatfiles.app.domain.usecase.AutoOrganizeUseCase
import com.neatfiles.app.domain.usecase.DetectCleanupCandidatesUseCase
import com.neatfiles.app.domain.usecase.DetectDuplicatesUseCase
import com.neatfiles.app.domain.usecase.PerformCleanupUseCase
import com.neatfiles.app.domain.usecase.ScanDownloadsUseCase
import com.neatfiles.app.domain.usecase.SmartRenameUseCase

class NeatFilesApp : Application() {

    lateinit var fileRepository: FileRepository private set
    lateinit var preferencesRepository: PreferencesRepository private set

    // UseCases
    lateinit var scanDownloadsUseCase: ScanDownloadsUseCase private set
    lateinit var detectDuplicatesUseCase: DetectDuplicatesUseCase private set
    lateinit var detectCleanupCandidatesUseCase: DetectCleanupCandidatesUseCase private set
    lateinit var smartRenameUseCase: SmartRenameUseCase private set
    lateinit var autoOrganizeUseCase: AutoOrganizeUseCase private set
    lateinit var performCleanupUseCase: PerformCleanupUseCase private set

    override fun onCreate() {
        super.onCreate()
        instance = this

        // Initialize dependencies
        detectDuplicatesUseCase = DetectDuplicatesUseCase()
        detectCleanupCandidatesUseCase = DetectCleanupCandidatesUseCase()
        smartRenameUseCase = SmartRenameUseCase()
        autoOrganizeUseCase = AutoOrganizeUseCase()

        fileRepository = FileRepositoryImpl(
            context = this,
            detectDuplicatesUseCase = detectDuplicatesUseCase,
            detectCleanupCandidatesUseCase = detectCleanupCandidatesUseCase
        )

        preferencesRepository = PreferencesRepositoryImpl(this)

        scanDownloadsUseCase = ScanDownloadsUseCase(
            repository = fileRepository,
            detectDuplicatesUseCase = detectDuplicatesUseCase,
            detectCleanupCandidatesUseCase = detectCleanupCandidatesUseCase
        )

        performCleanupUseCase = PerformCleanupUseCase(fileRepository)

        // Schedule periodic cleanup worker
        ScheduledCleanupWorker.schedule(this, 24)
    }

    companion object {
        lateinit var instance: NeatFilesApp private set
    }
}

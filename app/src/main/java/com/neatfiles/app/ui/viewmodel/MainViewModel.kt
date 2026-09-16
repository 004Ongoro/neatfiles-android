package com.neatfiles.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.neatfiles.app.NeatFilesApp
import com.neatfiles.app.core.model.CleanupReason
import com.neatfiles.app.core.model.FileCategory
import com.neatfiles.app.core.model.NeatFile
import com.neatfiles.app.core.model.OrganizePlan
import com.neatfiles.app.core.model.SmartRenameItem
import com.neatfiles.app.core.model.StorageOverview
import com.neatfiles.app.domain.repository.FileRepository
import com.neatfiles.app.domain.repository.PreferencesRepository
import com.neatfiles.app.domain.usecase.AutoOrganizeUseCase
import com.neatfiles.app.domain.usecase.PerformCleanupUseCase
import com.neatfiles.app.domain.usecase.ScanDownloadsUseCase
import com.neatfiles.app.domain.usecase.SmartRenameUseCase
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class MainUiState(
    val isLoading: Boolean = true,
    val isOnboardingCompleted: Boolean = false,
    val storageOverview: StorageOverview? = null,
    val allFiles: List<NeatFile> = emptyList(),
    val selectedCategory: FileCategory? = null,
    val selectedCleanupIds: Set<String> = emptySet(),
    val smartRenameList: List<SmartRenameItem> = emptyList(),
    val organizePlans: List<OrganizePlan> = emptyList(),
    val hasStoragePermission: Boolean = false,
    val oldFileThresholdDays: Int = 30,
    val isScheduledCleanupEnabled: Boolean = true,
    val userMessage: String? = null
)

sealed interface UiEvent {
    data class ShowMessage(val message: String) : UiEvent
    data class CleanupCompleted(val deletedCount: Int, val reclaimedBytes: Long) : UiEvent
    data class OrganizeCompleted(val movedCount: Int) : UiEvent
}

class MainViewModel(
    private val scanDownloadsUseCase: ScanDownloadsUseCase,
    private val performCleanupUseCase: PerformCleanupUseCase,
    private val smartRenameUseCase: SmartRenameUseCase,
    private val autoOrganizeUseCase: AutoOrganizeUseCase,
    private val fileRepository: FileRepository,
    private val preferencesRepository: PreferencesRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<UiEvent>()
    val events: SharedFlow<UiEvent> = _events.asSharedFlow()

    init {
        checkPermission()
        observePreferences()
        observeFilesFlow()
        scanDownloads()
    }

    fun checkPermission() {
        val hasPermission = fileRepository.hasStoragePermission()
        _uiState.update { it.copy(hasStoragePermission = hasPermission) }
        if (!hasPermission) {
            _uiState.update {
                it.copy(
                    allFiles = emptyList(),
                    storageOverview = null,
                    organizePlans = emptyList(),
                    smartRenameList = emptyList(),
                    selectedCleanupIds = emptySet()
                )
            }
        }
    }

    private fun observePreferences() {
        viewModelScope.launch {
            preferencesRepository.isOnboardingCompleted.collect { completed ->
                _uiState.update { it.copy(isOnboardingCompleted = completed) }
            }
        }
        viewModelScope.launch {
            preferencesRepository.oldFileThresholdDays.collect { days ->
                _uiState.update { it.copy(oldFileThresholdDays = days) }
            }
        }
        viewModelScope.launch {
            preferencesRepository.isScheduledCleanupEnabled.collect { enabled ->
                _uiState.update { it.copy(isScheduledCleanupEnabled = enabled) }
            }
        }
    }

    private fun observeFilesFlow() {
        viewModelScope.launch {
            fileRepository.getDownloadsFilesFlow().collect { files ->
                val renameCandidates = smartRenameUseCase(files)
                val organize = autoOrganizeUseCase(files)
                _uiState.update {
                    it.copy(
                        allFiles = files,
                        smartRenameList = renameCandidates,
                        organizePlans = organize
                    )
                }
            }
        }
    }

    fun scanDownloads() {
        viewModelScope.launch {
            if (!fileRepository.hasStoragePermission()) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        hasStoragePermission = false,
                        storageOverview = null,
                        allFiles = emptyList(),
                        organizePlans = emptyList(),
                        smartRenameList = emptyList(),
                        selectedCleanupIds = emptySet()
                    )
                }
                return@launch
            }

            _uiState.update { it.copy(isLoading = true) }
            try {
                val overview = scanDownloadsUseCase()
                val defaultSafeIds = overview.cleanupOverview.allCandidates.map { it.id }.toSet()

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        storageOverview = overview,
                        selectedCleanupIds = defaultSafeIds
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, userMessage = "Error scanning: ${e.message}") }
            }
        }
    }

    fun selectCategory(category: FileCategory?) {
        _uiState.update { it.copy(selectedCategory = category) }
    }

    fun toggleCleanupSelection(fileId: String) {
        _uiState.update { state ->
            val updated = state.selectedCleanupIds.toMutableSet()
            if (fileId in updated) {
                updated.remove(fileId)
            } else {
                updated.add(fileId)
            }
            state.copy(selectedCleanupIds = updated)
        }
    }

    fun selectAllCleanup(reasonFilter: CleanupReason? = null) {
        _uiState.update { state ->
            val candidates = state.storageOverview?.cleanupOverview?.allCandidates ?: emptyList()
            val filtered = if (reasonFilter != null) {
                candidates.filter { it.cleanupReason == reasonFilter }
            } else {
                candidates
            }
            val updated = state.selectedCleanupIds.toMutableSet()
            updated.addAll(filtered.map { it.id })
            state.copy(selectedCleanupIds = updated)
        }
    }

    fun deselectAllCleanup(reasonFilter: CleanupReason? = null) {
        _uiState.update { state ->
            val candidates = state.storageOverview?.cleanupOverview?.allCandidates ?: emptyList()
            val toRemove = if (reasonFilter != null) {
                candidates.filter { it.cleanupReason == reasonFilter }.map { it.id }.toSet()
            } else {
                state.selectedCleanupIds
            }
            state.copy(selectedCleanupIds = state.selectedCleanupIds - toRemove)
        }
    }

    fun executeCleanup() {
        val selectedIds = _uiState.value.selectedCleanupIds
        if (selectedIds.isEmpty()) return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val filesToDelete = _uiState.value.allFiles.filter { it.id in selectedIds }
            val reclaimedBytes = filesToDelete.sumOf { it.sizeBytes }

            val result = performCleanupUseCase(filesToDelete)
            result.onSuccess { count ->
                _uiState.update { it.copy(isLoading = false, selectedCleanupIds = emptySet()) }
                _events.emit(UiEvent.CleanupCompleted(count, reclaimedBytes))
                scanDownloads()
            }.onFailure { error ->
                _uiState.update { it.copy(isLoading = false, userMessage = "Cleanup failed: ${error.message}") }
            }
        }
    }

    fun applySmartRename(item: SmartRenameItem) {
        viewModelScope.launch {
            fileRepository.renameFile(item.file, item.suggestedName).onSuccess {
                _events.emit(UiEvent.ShowMessage("Renamed to ${item.suggestedName}"))
                scanDownloads()
            }.onFailure { error ->
                _uiState.update { it.copy(userMessage = "Rename failed: ${error.message}") }
            }
        }
    }

    fun executeAutoOrganize(onComplete: ((Result<Int>) -> Unit)? = null) {
        val plans = _uiState.value.organizePlans
        if (plans.isEmpty()) {
            onComplete?.invoke(Result.success(0))
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val result = fileRepository.organizeFiles(plans)
            result.onSuccess { moved ->
                _uiState.update { it.copy(isLoading = false) }
                _events.emit(UiEvent.OrganizeCompleted(moved))
                scanDownloads()
                onComplete?.invoke(Result.success(moved))
            }.onFailure { error ->
                _uiState.update { it.copy(isLoading = false, userMessage = "Organize failed: ${error.message}") }
                onComplete?.invoke(Result.failure(error))
            }
        }
    }

    fun setOldFileThreshold(days: Int) {
        viewModelScope.launch {
            preferencesRepository.setOldFileThresholdDays(days)
            scanDownloads()
        }
    }

    fun toggleScheduledCleanup(enabled: Boolean) {
        viewModelScope.launch {
            preferencesRepository.setScheduledCleanupEnabled(enabled)
            if (enabled) {
                com.neatfiles.app.data.worker.ScheduledCleanupWorker.schedule(NeatFilesApp.instance)
            } else {
                com.neatfiles.app.data.worker.ScheduledCleanupWorker.cancel(NeatFilesApp.instance)
            }
        }
    }

    fun completeOnboarding() {
        viewModelScope.launch {
            preferencesRepository.setOnboardingCompleted(true)
            scanDownloads()
        }
    }

    fun resetOnboarding() {
        viewModelScope.launch {
            preferencesRepository.setOnboardingCompleted(false)
        }
    }

    fun clearUserMessage() {
        _uiState.update { it.copy(userMessage = null) }
    }

    companion object {
        fun Factory(app: NeatFilesApp): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return MainViewModel(
                    scanDownloadsUseCase = app.scanDownloadsUseCase,
                    performCleanupUseCase = app.performCleanupUseCase,
                    smartRenameUseCase = app.smartRenameUseCase,
                    autoOrganizeUseCase = app.autoOrganizeUseCase,
                    fileRepository = app.fileRepository,
                    preferencesRepository = app.preferencesRepository
                ) as T
            }
        }
    }
}

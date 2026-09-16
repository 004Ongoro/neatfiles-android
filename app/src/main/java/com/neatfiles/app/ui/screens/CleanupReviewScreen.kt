package com.neatfiles.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.neatfiles.app.core.model.CleanupReason
import com.neatfiles.app.core.model.NeatFile
import com.neatfiles.app.core.util.Formatters
import com.neatfiles.app.ui.components.DeleteConfirmationDialog
import com.neatfiles.app.ui.components.FileViewerDialog
import com.neatfiles.app.ui.components.NeatFileCard
import com.neatfiles.app.ui.components.SmartRenameDialog
import com.neatfiles.app.ui.viewmodel.MainUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CleanupReviewScreen(
    state: MainUiState,
    onBackClick: () -> Unit,
    onToggleSelection: (String) -> Unit,
    onSelectAll: (CleanupReason?) -> Unit,
    onDeselectAll: (CleanupReason?) -> Unit,
    onExecuteCleanup: () -> Unit,
    onDeleteSingleFile: (NeatFile) -> Unit = {},
    onRenameSingleFile: (NeatFile, String) -> Unit = { _, _ -> },
    modifier: Modifier = Modifier
) {
    val overview = state.storageOverview?.cleanupOverview
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    var showConfirmDialog by remember { mutableStateOf(false) }
    var activeViewerFile by remember { mutableStateOf<NeatFile?>(null) }
    var fileToRename by remember { mutableStateOf<NeatFile?>(null) }
    var fileToDelete by remember { mutableStateOf<NeatFile?>(null) }

    val tabs = listOf(
        "All (${overview?.totalSafeToRemoveCount ?: 0})",
        "Duplicates (${overview?.duplicates?.size ?: 0})",
        "Old Files (${overview?.oldFiles?.size ?: 0})",
        "APKs (${overview?.obsoleteInstallers?.size ?: 0})",
        "Temp (${overview?.tempFiles?.size ?: 0})"
    )

    val currentCandidates: List<NeatFile> = when (selectedTabIndex) {
        1 -> overview?.duplicates ?: emptyList()
        2 -> overview?.oldFiles ?: emptyList()
        3 -> overview?.obsoleteInstallers ?: emptyList()
        4 -> overview?.tempFiles ?: emptyList()
        else -> overview?.allCandidates ?: emptyList()
    }

    val selectedInCurrentTab = currentCandidates.count { it.id in state.selectedCleanupIds }
    val allSelectedInTab = currentCandidates.isNotEmpty() && selectedInCurrentTab == currentCandidates.size

    val selectedFiles = (overview?.allCandidates ?: emptyList()).filter { it.id in state.selectedCleanupIds }
    val selectedBytes = selectedFiles.sumOf { it.sizeBytes }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = "Review Cleanup",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        bottomBar = {
            if (selectedFiles.isNotEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(16.dp)
                ) {
                    Button(
                        onClick = { showConfirmDialog = true },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error,
                            contentColor = MaterialTheme.colorScheme.onError
                        )
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Clean Selected (${selectedFiles.size} files • ${Formatters.formatFileSize(selectedBytes)})",
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        },
        modifier = modifier
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Category Tabs
            ScrollableTabRow(
                selectedTabIndex = selectedTabIndex,
                edgePadding = 16.dp,
                containerColor = MaterialTheme.colorScheme.background
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTabIndex == index,
                        onClick = { selectedTabIndex = index },
                        text = {
                            Text(
                                text = title,
                                fontWeight = if (selectedTabIndex == index) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    )
                }
            }

            // Quick Selection Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "$selectedInCurrentTab of ${currentCandidates.size} selected",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                TextButton(
                    onClick = {
                        val reasonFilter = when (selectedTabIndex) {
                            1 -> CleanupReason.DUPLICATE_COPY
                            2 -> CleanupReason.OLD_DOWNLOAD
                            3 -> CleanupReason.OBSOLETE_INSTALLER
                            4 -> CleanupReason.INCOMPLETE_OR_TEMP
                            else -> null
                        }
                        if (allSelectedInTab) {
                            onDeselectAll(reasonFilter)
                        } else {
                            onSelectAll(reasonFilter)
                        }
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.SelectAll,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = if (allSelectedInTab) "Deselect All" else "Select All")
                }
            }

            // File List
            if (currentCandidates.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = Color(0xFF2E7D32),
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "No Safe Cleanup Items",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "No clutter found for this category.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(currentCandidates, key = { it.id }) { file ->
                        NeatFileCard(
                            file = file,
                            isSelected = file.id in state.selectedCleanupIds,
                            onSelectionChange = { onToggleSelection(file.id) },
                            onItemClick = {
                                activeViewerFile = file
                            },
                            onRenameClick = {
                                fileToRename = file
                            },
                            onDeleteClick = {
                                fileToDelete = file
                            }
                        )
                    }
                }
            }
        }
    }

    if (showConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showConfirmDialog = false },
            title = { Text("Confirm Cleanup") },
            text = {
                Text(
                    "Are you sure you want to delete ${selectedFiles.size} files (${Formatters.formatFileSize(selectedBytes)}) from your Downloads folder? Original copies of duplicate files will not be deleted."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showConfirmDialog = false
                        onExecuteCleanup()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showConfirmDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // In-App File Viewer Dialog
    activeViewerFile?.let { file ->
        FileViewerDialog(
            file = file,
            onDismiss = { activeViewerFile = null },
            onRenameClick = {
                activeViewerFile = null
                fileToRename = file
            },
            onDeleteClick = {
                activeViewerFile = null
                fileToDelete = file
            }
        )
    }

    // Smart Rename Dialog
    fileToRename?.let { file ->
        SmartRenameDialog(
            file = file,
            onDismiss = { fileToRename = null },
            onConfirmRename = { newName ->
                onRenameSingleFile(file, newName)
            }
        )
    }

    // Delete Confirmation Dialog
    fileToDelete?.let { file ->
        DeleteConfirmationDialog(
            file = file,
            onDismiss = { fileToDelete = null },
            onConfirmDelete = {
                onDeleteSingleFile(file)
            }
        )
    }
}

package com.neatfiles.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.neatfiles.app.core.model.FileCategory
import com.neatfiles.app.core.model.NeatFile
import com.neatfiles.app.core.util.Formatters
import com.neatfiles.app.ui.components.AnimatedFeedbackState
import com.neatfiles.app.ui.components.DeleteConfirmationDialog
import com.neatfiles.app.ui.components.FeedbackType
import com.neatfiles.app.ui.components.FileViewerDialog
import com.neatfiles.app.ui.components.NeatFileCard
import com.neatfiles.app.ui.components.SmartRenameDialog
import com.neatfiles.app.ui.viewmodel.MainUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryDetailScreen(
    category: FileCategory,
    state: MainUiState,
    onBackClick: () -> Unit,
    onDeleteFile: (NeatFile) -> Unit = {},
    onRenameFile: (NeatFile, String) -> Unit = { _, _ -> },
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }
    var activeViewerFile by remember { mutableStateOf<NeatFile?>(null) }
    var fileToRename by remember { mutableStateOf<NeatFile?>(null) }
    var fileToDelete by remember { mutableStateOf<NeatFile?>(null) }

    val categoryFiles = state.allFiles.filter { it.category == category }
    val filteredFiles = if (searchQuery.isBlank()) {
        categoryFiles
    } else {
        categoryFiles.filter { it.name.contains(searchQuery, ignoreCase = true) }
    }
    val totalBytes = categoryFiles.sumOf { it.sizeBytes }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = category.displayName,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${categoryFiles.size} files • ${Formatters.formatFileSize(totalBytes)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
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
        modifier = modifier
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Search Input (shown when category has items)
            if (categoryFiles.isNotEmpty()) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    placeholder = { Text("Search in ${category.displayName}...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    singleLine = true,
                    shape = MaterialTheme.shapes.medium
                )
            }

            // File Content List or Empty State
            if (filteredFiles.isEmpty()) {
                AnimatedFeedbackState(
                    feedback = FeedbackType.Empty(
                        title = if (searchQuery.isNotBlank()) "No Matching Files" else "No ${category.displayName} Found",
                        description = if (searchQuery.isNotBlank()) "No files match '$searchQuery' in this category." else "There are currently no files categorized as ${category.displayName} in your Downloads folder.",
                        actionLabel = "Back to Dashboard"
                    ),
                    onPrimaryAction = onBackClick,
                    modifier = Modifier.padding(top = 40.dp)
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(filteredFiles, key = { it.id }) { file ->
                        NeatFileCard(
                            file = file,
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
                onRenameFile(file, newName)
            }
        )
    }

    // Delete Confirmation Dialog
    fileToDelete?.let { file ->
        DeleteConfirmationDialog(
            file = file,
            onDismiss = { fileToDelete = null },
            onConfirmDelete = {
                onDeleteFile(file)
            }
        )
    }
}

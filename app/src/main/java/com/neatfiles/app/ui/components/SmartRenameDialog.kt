package com.neatfiles.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DriveFileRenameOutline
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.neatfiles.app.core.model.NeatFile
import com.neatfiles.app.core.util.Formatters
import com.neatfiles.app.core.util.SmartRenameEngine

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SmartRenameDialog(
    file: NeatFile,
    onDismiss: () -> Unit,
    onConfirmRename: (newName: String) -> Unit
) {
    val renameEval = remember(file) { SmartRenameEngine.evaluate(file) }
    val originalBaseName = remember(file) {
        if (file.extension.isNotEmpty()) file.name.substringBeforeLast(".${file.extension}") else file.name
    }
    val suggestedBaseName = remember(renameEval) {
        val suggested = renameEval.suggestedName
        if (file.extension.isNotEmpty() && suggested.endsWith(".${file.extension}", ignoreCase = true)) {
            suggested.substringBeforeLast(".${file.extension}")
        } else {
            suggested
        }
    }

    var editedBaseName by remember(file) {
        mutableStateOf(if (renameEval.needsRenaming) suggestedBaseName else originalBaseName)
    }

    val invalidCharsRegex = remember { """[\\/:*?"<>|\r\n\t]""".toRegex() }
    val hasIllegalChars = editedBaseName.contains(invalidCharsRegex)
    val isBlankName = editedBaseName.trim().isEmpty()
    val fullTargetName = if (file.extension.isNotEmpty()) {
        "${editedBaseName.trim()}.${file.extension}"
    } else {
        editedBaseName.trim()
    }
    val isUnchanged = fullTargetName == file.name
    val canRename = !isBlankName && !hasIllegalChars && !isUnchanged

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .clip(RoundedCornerShape(24.dp)),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(22.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.size(38.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.DriveFileRenameOutline,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Smart Rename",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = Formatters.formatFileSize(file.sizeBytes),
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

                // Suggestion Reason Banner if available
                if (renameEval.needsRenaming) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.6f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.tertiary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = renameEval.reason,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onTertiaryContainer,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(14.dp))
                }

                // Original File Name
                Text(
                    text = "Current Name",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = file.name,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(top = 2.dp, bottom = 12.dp)
                )

                // Editable Name Input
                OutlinedTextField(
                    value = editedBaseName,
                    onValueChange = { editedBaseName = it },
                    label = { Text("New File Name") },
                    trailingIcon = {
                        if (file.extension.isNotEmpty()) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier.padding(end = 8.dp)
                            ) {
                                Text(
                                    text = ".${file.extension}",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    },
                    isError = isBlankName || hasIllegalChars,
                    supportingText = {
                        when {
                            isBlankName -> Text("Name cannot be empty", color = MaterialTheme.colorScheme.error)
                            hasIllegalChars -> Text("Contains invalid characters: / \\ : * ? \" < > |", color = MaterialTheme.colorScheme.error)
                            isUnchanged -> Text("Name is identical to original", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            else -> Text("Ready to rename", color = MaterialTheme.colorScheme.primary)
                        }
                    },
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Quick Preset Modifiers
                Text(
                    text = "Quick Presets",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.SemiBold
                )

                FlowRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (renameEval.needsRenaming && editedBaseName != suggestedBaseName) {
                        AssistChip(
                            onClick = { editedBaseName = suggestedBaseName },
                            label = { Text("Smart Suggestion") },
                            leadingIcon = {
                                Icon(Icons.Default.AutoAwesome, null, modifier = Modifier.size(14.dp))
                            },
                            colors = AssistChipDefaults.assistChipColors(
                                containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f)
                            )
                        )
                    }

                    AssistChip(
                        onClick = {
                            editedBaseName = editedBaseName
                                .replace("""[_\-]+""".toRegex(), " ")
                                .replace("""\s+""".toRegex(), " ")
                                .trim()
                        },
                        label = { Text("Spaces instead of _") }
                    )

                    AssistChip(
                        onClick = {
                            editedBaseName = editedBaseName
                                .split(" ")
                                .joinToString(" ") { word ->
                                    word.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
                                }
                        },
                        label = { Text("Title Case") }
                    )

                    if (editedBaseName != originalBaseName) {
                        AssistChip(
                            onClick = { editedBaseName = originalBaseName },
                            label = { Text("Reset Original") }
                        )
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
                        Text("Cancel")
                    }

                    Button(
                        onClick = {
                            if (canRename) {
                                onConfirmRename(fullTargetName)
                                onDismiss()
                            }
                        },
                        enabled = canRename,
                        modifier = Modifier.weight(1.5f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Rename File", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

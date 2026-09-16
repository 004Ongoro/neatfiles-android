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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DriveFileMove
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DriveFileRenameOutline
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.neatfiles.app.core.model.FileCategory
import com.neatfiles.app.core.util.Formatters
import com.neatfiles.app.ui.components.CategoryCard
import com.neatfiles.app.ui.components.CleanupActionCard
import com.neatfiles.app.ui.components.StorageBreakdownBar
import com.neatfiles.app.ui.viewmodel.MainUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    state: MainUiState,
    onReviewCleanupClick: () -> Unit,
    onCategoryClick: (FileCategory) -> Unit,
    onAutoOrganizeClick: () -> Unit,
    onSmartRenameClick: () -> Unit,
    onStorageAnalysisClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onRefreshClick: () -> Unit,
    onRequestStoragePermission: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Neat",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Files",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onRefreshClick) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                    }
                    IconButton(onClick = onSettingsClick) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        modifier = modifier
    ) { innerPadding ->
        if (state.isLoading && state.storageOverview == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            val overview = state.storageOverview
            val totalCount = overview?.totalDownloadsFiles ?: 0
            val totalBytes = overview?.totalDownloadsBytes ?: 0L
            val cleanup = overview?.cleanupOverview
            val safeCount = cleanup?.totalSafeToRemoveCount ?: 0
            val safeBytes = cleanup?.totalReclaimableBytes ?: 0L

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Storage Permission Alert Banner
                if (!state.hasStoragePermission) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.8f)
                            )
                        ) {
                            Column(modifier = Modifier.padding(18.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Warning,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = "Storage Access Needed",
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.onErrorContainer
                                    )
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "NeatFiles needs All Files Access to inspect, rename, and organize downloads on your device.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Button(
                                    onClick = onRequestStoragePermission,
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.error
                                    )
                                ) {
                                    Text("Grant Storage Access")
                                }
                            }
                        }
                    }
                }

                // Headline Downloads Summary Card
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp)
                        ) {
                            Text(
                                text = "Downloads Folder",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.SemiBold
                            )

                            Spacer(modifier = Modifier.height(4.dp))

                            Row(verticalAlignment = Alignment.Bottom) {
                                Text(
                                    text = "$totalCount files",
                                    style = MaterialTheme.typography.headlineMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "•  ${Formatters.formatFileSize(totalBytes)}",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(bottom = 2.dp)
                                )
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            // Segmented Storage Visualizer
                            if (overview != null && overview.categoryStats.isNotEmpty()) {
                                StorageBreakdownBar(
                                    categoryStats = overview.categoryStats,
                                    totalBytes = totalBytes
                                )
                            } else {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(12.dp)
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                                )
                            }
                        }
                    }
                }

                // Empty State if no files exist
                if (state.hasStoragePermission && totalCount == 0) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                            )
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = Color(0xFF2E7D32),
                                    modifier = Modifier.size(40.dp)
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    text = "Downloads Folder is Empty & Clean",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "No files found in Downloads. As you download documents, images, and files, NeatFiles will automatically analyze, detect duplicates, and organize them here.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                            }
                        }
                    }
                }

                // Safe to Remove Banner
                if (safeCount > 0) {
                    item {
                        CleanupActionCard(
                            safeToRemoveCount = safeCount,
                            reclaimableBytes = safeBytes,
                            onReviewClick = onReviewCleanupClick
                        )
                    }
                }

                // Quick Action Tiles
                item {
                    Text(
                        text = "Intelligent Actions",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        QuickActionPill(
                            title = "Auto-Organize",
                            subtitle = "Move to folders",
                            icon = Icons.AutoMirrored.Filled.DriveFileMove,
                            onClick = onAutoOrganizeClick,
                            modifier = Modifier.weight(1f)
                        )
                        QuickActionPill(
                            title = "Smart Rename",
                            subtitle = "Clean names",
                            icon = Icons.Default.DriveFileRenameOutline,
                            onClick = onSmartRenameClick,
                            modifier = Modifier.weight(1f)
                        )
                        QuickActionPill(
                            title = "Deep Analysis",
                            subtitle = "Storage usage",
                            icon = Icons.Default.PieChart,
                            onClick = onStorageAnalysisClick,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // File Categories Grid
                item {
                    Text(
                        text = "File Categories",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    val categoryStats = overview?.categoryStats ?: emptyList()
                    val statMap = categoryStats.associateBy { it.category }

                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            val docStat = statMap[FileCategory.DOCUMENTS]
                            CategoryCard(
                                category = FileCategory.DOCUMENTS,
                                count = docStat?.count ?: 0,
                                totalBytes = docStat?.totalBytes ?: 0L,
                                onClick = { onCategoryClick(FileCategory.DOCUMENTS) },
                                modifier = Modifier.weight(1f)
                            )
                            val imgStat = statMap[FileCategory.IMAGES]
                            CategoryCard(
                                category = FileCategory.IMAGES,
                                count = imgStat?.count ?: 0,
                                totalBytes = imgStat?.totalBytes ?: 0L,
                                onClick = { onCategoryClick(FileCategory.IMAGES) },
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            val insStat = statMap[FileCategory.INSTALLERS]
                            CategoryCard(
                                category = FileCategory.INSTALLERS,
                                count = insStat?.count ?: 0,
                                totalBytes = insStat?.totalBytes ?: 0L,
                                onClick = { onCategoryClick(FileCategory.INSTALLERS) },
                                modifier = Modifier.weight(1f)
                            )
                            val arcStat = statMap[FileCategory.ARCHIVES]
                            CategoryCard(
                                category = FileCategory.ARCHIVES,
                                count = arcStat?.count ?: 0,
                                totalBytes = arcStat?.totalBytes ?: 0L,
                                onClick = { onCategoryClick(FileCategory.ARCHIVES) },
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            val medStat = statMap[FileCategory.MEDIA]
                            CategoryCard(
                                category = FileCategory.MEDIA,
                                count = medStat?.count ?: 0,
                                totalBytes = medStat?.totalBytes ?: 0L,
                                onClick = { onCategoryClick(FileCategory.MEDIA) },
                                modifier = Modifier.weight(1f)
                            )
                            val codeStat = statMap[FileCategory.CODE]
                            CategoryCard(
                                category = FileCategory.CODE,
                                count = codeStat?.count ?: 0,
                                totalBytes = codeStat?.totalBytes ?: 0L,
                                onClick = { onCategoryClick(FileCategory.CODE) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun QuickActionPill(
    title: String,
    subtitle: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(18.dp)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

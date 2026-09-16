package com.neatfiles.app.ui

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.neatfiles.app.NeatFilesApp
import com.neatfiles.app.core.model.FileCategory
import com.neatfiles.app.core.util.Formatters
import com.neatfiles.app.ui.screens.CategoryDetailScreen
import com.neatfiles.app.ui.screens.CleanupReviewScreen
import com.neatfiles.app.ui.screens.DashboardScreen
import com.neatfiles.app.ui.screens.LicensesScreen
import com.neatfiles.app.ui.screens.OnboardingScreen
import com.neatfiles.app.ui.screens.SettingsScreen
import com.neatfiles.app.ui.screens.SmartOrganizerScreen
import com.neatfiles.app.ui.screens.SmartRenameScreen
import com.neatfiles.app.ui.screens.StorageAnalysisScreen
import com.neatfiles.app.ui.theme.NeatFilesTheme
import com.neatfiles.app.ui.viewmodel.MainViewModel
import com.neatfiles.app.ui.viewmodel.UiEvent
import kotlinx.coroutines.flow.collectLatest

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels {
        MainViewModel.Factory(application as NeatFilesApp)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            NeatFilesTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    NeatFilesAppRoot(
                        viewModel = viewModel,
                        onRequestStoragePermission = { requestStoragePermission() }
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.checkPermission()
        viewModel.scanDownloads()
    }

    private fun requestStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
                    data = Uri.parse("package:$packageName")
                }
                startActivity(intent)
            } catch (_: Exception) {
                val intent = Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
                startActivity(intent)
            }
        } else {
            requestPermissions(
                arrayOf(
                    android.Manifest.permission.READ_EXTERNAL_STORAGE,
                    android.Manifest.permission.WRITE_EXTERNAL_STORAGE
                ),
                101
            )
        }
    }
}

@Composable
fun NeatFilesAppRoot(
    viewModel: MainViewModel,
    onRequestStoragePermission: () -> Unit
) {
    val navController = rememberNavController()
    val state by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.events.collectLatest { event ->
            when (event) {
                is UiEvent.ShowMessage -> {
                    snackbarHostState.showSnackbar(event.message)
                }
                is UiEvent.CleanupCompleted -> {
                    snackbarHostState.showSnackbar(
                        "Cleaned ${event.deletedCount} files! Reclaimed ${Formatters.formatFileSize(event.reclaimedBytes)}."
                    )
                }
                is UiEvent.OrganizeCompleted -> {
                    snackbarHostState.showSnackbar(
                        "Organized ${event.movedCount} files into clean category folders!"
                    )
                }
            }
        }
    }

    LaunchedEffect(state.userMessage) {
        state.userMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearUserMessage()
        }
    }

    val startDest = if (!state.isOnboardingCompleted) "onboarding" else "dashboard"

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = startDest
        ) {
            composable("onboarding") {
                OnboardingScreen(
                    state = state,
                    onCompleteOnboarding = {
                        viewModel.completeOnboarding()
                        navController.navigate("dashboard") {
                            popUpTo("onboarding") { inclusive = true }
                        }
                    },
                    onThresholdChange = { days -> viewModel.setOldFileThreshold(days) },
                    onToggleScheduledCleanup = { enabled -> viewModel.toggleScheduledCleanup(enabled) },
                    onCheckPermissions = {
                        viewModel.checkPermission()
                        viewModel.scanDownloads()
                    }
                )
            }

            composable("dashboard") {
                DashboardScreen(
                    state = state,
                    onReviewCleanupClick = { navController.navigate("cleanup_review") },
                    onCategoryClick = { category ->
                        navController.navigate("category_detail/${category.name}")
                    },
                    onAutoOrganizeClick = { navController.navigate("smart_organizer") },
                    onSmartRenameClick = { navController.navigate("smart_rename") },
                    onStorageAnalysisClick = { navController.navigate("storage_analysis") },
                    onSettingsClick = { navController.navigate("settings") },
                    onRefreshClick = {
                        viewModel.checkPermission()
                        viewModel.scanDownloads()
                    },
                    onRequestStoragePermission = onRequestStoragePermission
                )
            }

            composable("cleanup_review") {
                CleanupReviewScreen(
                    state = state,
                    onBackClick = { navController.popBackStack() },
                    onToggleSelection = { fileId -> viewModel.toggleCleanupSelection(fileId) },
                    onSelectAll = { reason -> viewModel.selectAllCleanup(reason) },
                    onDeselectAll = { reason -> viewModel.deselectAllCleanup(reason) },
                    onExecuteCleanup = {
                        viewModel.executeCleanup()
                        navController.popBackStack()
                    }
                )
            }

            composable(
                route = "category_detail/{categoryName}",
                arguments = listOf(navArgument("categoryName") { type = NavType.StringType })
            ) { backStackEntry ->
                val categoryName = backStackEntry.arguments?.getString("categoryName") ?: ""
                val category = try {
                    FileCategory.valueOf(categoryName)
                } catch (_: Exception) {
                    FileCategory.DOCUMENTS
                }
                CategoryDetailScreen(
                    category = category,
                    state = state,
                    onBackClick = { navController.popBackStack() }
                )
            }

            composable("smart_organizer") {
                SmartOrganizerScreen(
                    state = state,
                    onBackClick = { navController.popBackStack() },
                    onOrganizeClick = { onComplete ->
                        viewModel.executeAutoOrganize(onComplete)
                    }
                )
            }

            composable("smart_rename") {
                SmartRenameScreen(
                    state = state,
                    onBackClick = { navController.popBackStack() },
                    onApplyRename = { item -> viewModel.applySmartRename(item) }
                )
            }

            composable("storage_analysis") {
                StorageAnalysisScreen(
                    state = state,
                    onBackClick = { navController.popBackStack() }
                )
            }

            composable("settings") {
                SettingsScreen(
                    state = state,
                    onBackClick = { navController.popBackStack() },
                    onOldThresholdChange = { days -> viewModel.setOldFileThreshold(days) },
                    onToggleScheduledCleanup = { enabled -> viewModel.toggleScheduledCleanup(enabled) },
                    onReRunOnboarding = {
                        viewModel.resetOnboarding()
                        navController.navigate("onboarding")
                    },
                    onLicensesClick = { navController.navigate("licenses") }
                )
            }

            composable("licenses") {
                LicensesScreen(
                    onBackClick = { navController.popBackStack() }
                )
            }
        }
    }
}

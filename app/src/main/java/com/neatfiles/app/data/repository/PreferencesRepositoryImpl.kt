package com.neatfiles.app.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.neatfiles.app.core.model.FileCategory
import com.neatfiles.app.domain.repository.PreferencesRepository
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOf

class PreferencesRepositoryImpl(context: Context) : PreferencesRepository {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("neatfiles_prefs", Context.MODE_PRIVATE)

    override val isOnboardingCompleted: Flow<Boolean> = callbackFlow {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == KEY_ONBOARDING_COMPLETED) {
                trySend(prefs.getBoolean(KEY_ONBOARDING_COMPLETED, false))
            }
        }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        trySend(prefs.getBoolean(KEY_ONBOARDING_COMPLETED, false))
        awaitClose { prefs.unregisterOnSharedPreferenceChangeListener(listener) }
    }

    override val oldFileThresholdDays: Flow<Int> = callbackFlow {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == KEY_OLD_THRESHOLD) {
                trySend(prefs.getInt(KEY_OLD_THRESHOLD, 30))
            }
        }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        trySend(prefs.getInt(KEY_OLD_THRESHOLD, 30))
        awaitClose { prefs.unregisterOnSharedPreferenceChangeListener(listener) }
    }

    override val isScheduledCleanupEnabled: Flow<Boolean> = callbackFlow {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == KEY_SCHEDULED_ENABLED) {
                trySend(prefs.getBoolean(KEY_SCHEDULED_ENABLED, true))
            }
        }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        trySend(prefs.getBoolean(KEY_SCHEDULED_ENABLED, true))
        awaitClose { prefs.unregisterOnSharedPreferenceChangeListener(listener) }
    }

    override val cleanupIntervalHours: Flow<Int> = flowOf(24)

    override val isAutoOrganizeEnabled: Flow<Boolean> = flowOf(true)

    override val categoryFolderMappings: Flow<Map<FileCategory, String>> = flowOf(
        FileCategory.entries.associateWith { it.folderName }
    )

    override suspend fun setOnboardingCompleted(completed: Boolean) {
        prefs.edit().putBoolean(KEY_ONBOARDING_COMPLETED, completed).apply()
    }

    override suspend fun setOldFileThresholdDays(days: Int) {
        prefs.edit().putInt(KEY_OLD_THRESHOLD, days).apply()
    }

    override suspend fun setScheduledCleanupEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_SCHEDULED_ENABLED, enabled).apply()
    }

    override suspend fun setCleanupIntervalHours(hours: Int) {
        prefs.edit().putInt(KEY_CLEANUP_INTERVAL, hours).apply()
    }

    override suspend fun setAutoOrganizeEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_AUTO_ORGANIZE, enabled).apply()
    }

    override suspend fun updateCategoryFolder(category: FileCategory, folderName: String) {
        prefs.edit().putString("folder_${category.name}", folderName).apply()
    }

    companion object {
        private const val KEY_ONBOARDING_COMPLETED = "key_onboarding_completed"
        private const val KEY_OLD_THRESHOLD = "key_old_threshold"
        private const val KEY_SCHEDULED_ENABLED = "key_scheduled_enabled"
        private const val KEY_CLEANUP_INTERVAL = "key_cleanup_interval"
        private const val KEY_AUTO_ORGANIZE = "key_auto_organize"
    }
}

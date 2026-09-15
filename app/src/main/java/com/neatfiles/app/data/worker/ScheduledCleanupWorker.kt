package com.neatfiles.app.data.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.neatfiles.app.R
import com.neatfiles.app.core.util.Formatters
import com.neatfiles.app.data.repository.FileRepositoryImpl
import com.neatfiles.app.ui.MainActivity
import java.util.concurrent.TimeUnit

class ScheduledCleanupWorker(
    private val context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            val repository = FileRepositoryImpl(context)
            val overview = repository.scanDownloads()
            val cleanup = overview.cleanupOverview

            if (cleanup.totalSafeToRemoveCount > 0) {
                showCleanupNotification(
                    count = cleanup.totalSafeToRemoveCount,
                    reclaimableBytes = cleanup.totalReclaimableBytes
                )
            }
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }

    private fun showCleanupNotification(count: Int, reclaimableBytes: Long) {
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val channelId = "neatfiles_cleanup_channel"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "NeatFiles Cleanup Alerts",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Periodic alerts when safe-to-remove downloads accumulate"
            }
            notificationManager.createNotificationChannel(channel)
        }

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("OPEN_SCREEN", "cleanup_review")
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            1001,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val sizeStr = Formatters.formatFileSize(reclaimableBytes)
        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("NeatFiles: Downloads Tidy Up")
            .setContentText("$count files may be safe to remove ($sizeStr)")
            .setStyle(
                NotificationCompat.BigTextStyle().bigText(
                    "You have $count clutter files (duplicates, old downloads, obsolete APKs) using $sizeStr in Downloads. Tap to review cleanup."
                )
            )
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(2002, notification)
    }

    companion object {
        private const val WORK_NAME = "NeatFilesScheduledCleanup"

        fun schedule(context: Context, intervalHours: Long = 24) {
            val request = PeriodicWorkRequestBuilder<ScheduledCleanupWorker>(
                intervalHours, TimeUnit.HOURS
            ).build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.UPDATE,
                request
            )
        }

        fun cancel(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
        }
    }
}

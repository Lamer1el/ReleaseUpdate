package com.example.sync

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.data.preferences.UserPreferencesRepository
import com.example.notification.NotificationHelper
import com.example.updater.UpdateCheckResult
import com.example.updater.UpdateManager
import kotlinx.coroutines.flow.first
import java.util.concurrent.TimeUnit

class UpdateCheckWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val prefsRepo = UserPreferencesRepository(applicationContext)
        val settings = prefsRepo.settingsFlow.first()

        if (!settings.autoCheckUpdates) {
            return Result.success()
        }

        val updateManager = UpdateManager(applicationContext)
        val notificationHelper = NotificationHelper(applicationContext)

        when (val result = updateManager.checkForUpdates()) {
            is UpdateCheckResult.UpdateAvailable -> {
                if (settings.notifyUpdates) {
                    notificationHelper.showUpdateAvailableNotification(
                        result.latestRelease.tagName,
                        result.latestRelease.body ?: "Доступна новая версия на GitHub"
                    )
                }
            }
            is UpdateCheckResult.UpdateAvailableNoApk -> {
                if (settings.notifyUpdates) {
                    notificationHelper.showUpdateAvailableNotification(
                        result.latestRelease.tagName,
                        result.latestRelease.body ?: "Доступна новая версия на GitHub"
                    )
                }
            }
            else -> {}
        }

        return Result.success()
    }

    companion object {
        private const val WORK_NAME = "skitefi_periodic_update_check"

        fun schedule(context: Context, intervalHours: Int = 24) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val request = PeriodicWorkRequestBuilder<UpdateCheckWorker>(
                intervalHours.toLong().coerceAtLeast(12),
                TimeUnit.HOURS
            ).setConstraints(constraints).build()

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

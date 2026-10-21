package com.example

import android.app.Application
import com.example.data.local.AppDatabase
import com.example.data.preferences.UserPreferencesRepository
import com.example.notification.NotificationHelper
import com.example.sync.UpdateCheckWorker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class SkitefiApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        // Initialize Notification Channels
        NotificationHelper(this)

        // Initialize Room DB early
        AppDatabase.getInstance(this)

        // Schedule periodic update checks if enabled
        val scope = CoroutineScope(Dispatchers.IO)
        scope.launch {
            val prefs = UserPreferencesRepository(this@SkitefiApplication)
            val settings = prefs.settingsFlow.first()
            if (settings.autoCheckUpdates) {
                UpdateCheckWorker.schedule(this@SkitefiApplication, settings.checkUpdatesIntervalHours)
            }
        }
    }
}

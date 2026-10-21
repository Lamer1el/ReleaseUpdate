package com.example.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "skitefi_settings")

data class UserSettings(
    val themeMode: String = "DARK", // DARK, LIGHT, SYSTEM
    val accentColor: String = "CYBER_NEON", // CYBER_NEON, MATRIX_GREEN, SOLAR_AMBER, SYNTHWAVE, CRIMSON
    val cardCornerRadiusDp: Int = 14, // 8, 14, 20
    val uiDensity: String = "COMFORTABLE", // COMFORTABLE, COMPACT
    val customFontUri: String? = null,
    val customFontName: String? = null,
    val onboardingCompleted: Boolean = false,

    val storageWriteEnabled: Boolean = true,
    val autoCheckUpdates: Boolean = true,
    val checkUpdatesIntervalHours: Int = 24,
    val notifyUpdates: Boolean = true,
    val notifyReminders: Boolean = true,
    val notifySyncErrors: Boolean = true,
    val userNickname: String = "User_${(1000..9999).random()}"
)

class UserPreferencesRepository(private val context: Context) {

    private object Keys {
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val ACCENT_COLOR = stringPreferencesKey("accent_color")
        val CARD_CORNER_RADIUS = intPreferencesKey("card_corner_radius")
        val UI_DENSITY = stringPreferencesKey("ui_density")
        val CUSTOM_FONT_URI = stringPreferencesKey("custom_font_uri")
        val CUSTOM_FONT_NAME = stringPreferencesKey("custom_font_name")
        val ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
        val STORAGE_WRITE_ENABLED = booleanPreferencesKey("storage_write_enabled")
        val AUTO_CHECK_UPDATES = booleanPreferencesKey("auto_check_updates")
        val CHECK_INTERVAL_HOURS = intPreferencesKey("check_interval_hours")
        val NOTIFY_UPDATES = booleanPreferencesKey("notify_updates")
        val NOTIFY_REMINDERS = booleanPreferencesKey("notify_reminders")
        val NOTIFY_SYNC_ERRORS = booleanPreferencesKey("notify_sync_errors")
        val USER_NICKNAME = stringPreferencesKey("user_nickname")
    }

    val settingsFlow: Flow<UserSettings> = context.dataStore.data.map { prefs ->
        UserSettings(
            themeMode = prefs[Keys.THEME_MODE] ?: "SYSTEM",
            accentColor = prefs[Keys.ACCENT_COLOR] ?: "DYNAMIC",
            cardCornerRadiusDp = prefs[Keys.CARD_CORNER_RADIUS] ?: 16,
            uiDensity = prefs[Keys.UI_DENSITY] ?: "COMFORTABLE",
            customFontUri = prefs[Keys.CUSTOM_FONT_URI],
            customFontName = prefs[Keys.CUSTOM_FONT_NAME],
            onboardingCompleted = prefs[Keys.ONBOARDING_COMPLETED] ?: false,
            storageWriteEnabled = prefs[Keys.STORAGE_WRITE_ENABLED] ?: true,
            autoCheckUpdates = prefs[Keys.AUTO_CHECK_UPDATES] ?: true,
            checkUpdatesIntervalHours = prefs[Keys.CHECK_INTERVAL_HOURS] ?: 24,
            notifyUpdates = prefs[Keys.NOTIFY_UPDATES] ?: true,
            notifyReminders = prefs[Keys.NOTIFY_REMINDERS] ?: true,
            notifySyncErrors = prefs[Keys.NOTIFY_SYNC_ERRORS] ?: true,
            userNickname = prefs[Keys.USER_NICKNAME] ?: "User_${(1000..9999).random()}"
        )
    }

    suspend fun setThemeMode(mode: String) {
        context.dataStore.edit { it[Keys.THEME_MODE] = mode }
    }

    suspend fun setAccentColor(accent: String) {
        context.dataStore.edit { it[Keys.ACCENT_COLOR] = accent }
    }

    suspend fun setCardCornerRadius(radiusDp: Int) {
        context.dataStore.edit { it[Keys.CARD_CORNER_RADIUS] = radiusDp }
    }

    suspend fun setUiDensity(density: String) {
        context.dataStore.edit { it[Keys.UI_DENSITY] = density }
    }

    suspend fun setCustomFont(uri: String?, name: String?) {
        context.dataStore.edit {
            if (uri != null) it[Keys.CUSTOM_FONT_URI] = uri else it.remove(Keys.CUSTOM_FONT_URI)
            if (name != null) it[Keys.CUSTOM_FONT_NAME] = name else it.remove(Keys.CUSTOM_FONT_NAME)
        }
    }

    suspend fun setOnboardingCompleted(completed: Boolean) {
        context.dataStore.edit { it[Keys.ONBOARDING_COMPLETED] = completed }
    }

    suspend fun setStorageWriteEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.STORAGE_WRITE_ENABLED] = enabled }
    }

    suspend fun setAutoCheckUpdates(enabled: Boolean) {
        context.dataStore.edit { it[Keys.AUTO_CHECK_UPDATES] = enabled }
    }

    suspend fun setCheckIntervalHours(hours: Int) {
        context.dataStore.edit { it[Keys.CHECK_INTERVAL_HOURS] = hours }
    }

    suspend fun setNotifyUpdates(enabled: Boolean) {
        context.dataStore.edit { it[Keys.NOTIFY_UPDATES] = enabled }
    }

    suspend fun setNotifyReminders(enabled: Boolean) {
        context.dataStore.edit { it[Keys.NOTIFY_REMINDERS] = enabled }
    }

    suspend fun setNotifySyncErrors(enabled: Boolean) {
        context.dataStore.edit { it[Keys.NOTIFY_SYNC_ERRORS] = enabled }
    }

    suspend fun setUserNickname(nickname: String) {
        context.dataStore.edit { it[Keys.USER_NICKNAME] = nickname }
    }
}

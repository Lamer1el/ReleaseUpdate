package com.example.benchmark

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Build
import android.view.WindowManager
import java.io.File

data class DeviceSpecs(
    val model: String,
    val manufacturer: String,
    val brand: String,
    val board: String,
    val hardware: String,
    val androidVersion: String,
    val apiLevel: Int,
    val cpuCores: Int,
    val totalRamMB: Long,
    val availableRamMB: Long,
    val displayWidth: Int,
    val displayHeight: Int,
    val refreshRateHz: Float,
    val supportedRefreshRates: List<Float>,
    val batteryPercent: Int,
    val batteryTempCelsius: Float,
    val batteryStatus: String,
    val isCharging: Boolean
)

object DeviceInfoProvider {

    fun getDeviceSpecs(context: Context): DeviceSpecs {
        val cores = Runtime.getRuntime().availableProcessors()
        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as? android.app.ActivityManager
        val mi = android.app.ActivityManager.MemoryInfo()
        am?.getMemoryInfo(mi)
        val totalRamMB = mi.totalMem / (1024 * 1024)
        val availRamMB = mi.availMem / (1024 * 1024)

        val wm = context.getSystemService(Context.WINDOW_SERVICE) as? WindowManager
        var width = 1080
        var height = 2400
        var currentHz = 60.0f
        val supportedRates = mutableListOf<Float>()

        if (wm != null) {
            val display = wm.defaultDisplay
            val metrics = android.util.DisplayMetrics()
            display.getRealMetrics(metrics)
            width = metrics.widthPixels
            height = metrics.heightPixels
            currentHz = display.refreshRate
            val modes = display.supportedModes
            modes.forEach { mode ->
                if (!supportedRates.contains(mode.refreshRate)) {
                    supportedRates.add(mode.refreshRate)
                }
            }
        }
        if (supportedRates.isEmpty()) supportedRates.add(currentHz)

        // Battery
        val batteryFilter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        val batteryStatusIntent = context.registerReceiver(null, batteryFilter)
        val level = batteryStatusIntent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = batteryStatusIntent?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
        val batteryPercent = if (level >= 0 && scale > 0) (level * 100 / scale) else -1
        val tempRaw = batteryStatusIntent?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0) ?: 0
        val tempCelsius = tempRaw / 10.0f
        val statusInt = batteryStatusIntent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        val isCharging = statusInt == BatteryManager.BATTERY_STATUS_CHARGING || statusInt == BatteryManager.BATTERY_STATUS_FULL
        val statusString = when (statusInt) {
            BatteryManager.BATTERY_STATUS_CHARGING -> "Заряжается"
            BatteryManager.BATTERY_STATUS_DISCHARGING -> "Разряжается"
            BatteryManager.BATTERY_STATUS_FULL -> "Заряжен полностью"
            BatteryManager.BATTERY_STATUS_NOT_CHARGING -> "Не заряжается"
            else -> "Неизвестно"
        }

        return DeviceSpecs(
            model = Build.MODEL,
            manufacturer = Build.MANUFACTURER,
            brand = Build.BRAND,
            board = Build.BOARD,
            hardware = Build.HARDWARE,
            androidVersion = Build.VERSION.RELEASE,
            apiLevel = Build.VERSION.SDK_INT,
            cpuCores = cores,
            totalRamMB = totalRamMB,
            availableRamMB = availRamMB,
            displayWidth = width,
            displayHeight = height,
            refreshRateHz = currentHz,
            supportedRefreshRates = supportedRates.sorted(),
            batteryPercent = batteryPercent,
            batteryTempCelsius = tempCelsius,
            batteryStatus = statusString,
            isCharging = isCharging
        )
    }
}

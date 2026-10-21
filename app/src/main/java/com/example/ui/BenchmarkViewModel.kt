package com.example.ui

import android.app.Application
import android.content.Context
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.benchmark.AiNeuralMetrics
import com.example.benchmark.BenchmarkCategory
import com.example.benchmark.BenchmarkEngine
import com.example.benchmark.BenchmarkResultData
import com.example.benchmark.CpuMetrics
import com.example.benchmark.CryptoMetrics
import com.example.benchmark.DeviceInfoProvider
import com.example.benchmark.DeviceSpecs
import com.example.benchmark.GpuMetrics
import com.example.benchmark.RamMetrics
import com.example.benchmark.RaytracingMetrics
import com.example.benchmark.ScoringEngine
import com.example.benchmark.StabilityMetrics
import com.example.benchmark.StorageMetrics
import com.example.benchmark.StxFormatManager
import com.example.data.local.AppDatabase
import com.example.data.local.BenchmarkEntity
import com.example.data.preferences.UserPreferencesRepository
import com.example.data.preferences.UserSettings
import com.example.data.remote.GitHubAsset
import com.example.updater.UpdateCheckResult
import com.example.updater.UpdateManager
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File
import java.util.UUID

data class ActiveBenchmarkState(
    val isRunning: Boolean = false,
    val category: BenchmarkCategory? = null,
    val progress: Float = 0f,
    val statusMessage: String = "",
    val error: String? = null,
    val isGpuActive: Boolean = false
)

data class ApkDownloadState(
    val isDownloading: Boolean = false,
    val progress: Float = 0f,
    val downloadedBytes: Long = 0,
    val totalBytes: Long = 0,
    val downloadedFile: File? = null,
    val validationMessage: String? = null,
    val error: String? = null
)

class BenchmarkViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getInstance(application)
    private val dao = db.benchmarkDao()
    private val engine = BenchmarkEngine(application)
    private val prefsRepo = UserPreferencesRepository(application)
    private val updateManager = UpdateManager(application)
    private val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()

    val settings: StateFlow<UserSettings> = prefsRepo.settingsFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, UserSettings())

    val history: StateFlow<List<BenchmarkEntity>> = dao.getAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _deviceSpecs = MutableStateFlow(DeviceInfoProvider.getDeviceSpecs(application))
    val deviceSpecs: StateFlow<DeviceSpecs> = _deviceSpecs.asStateFlow()

    private val _activeState = MutableStateFlow(ActiveBenchmarkState())
    val activeState: StateFlow<ActiveBenchmarkState> = _activeState.asStateFlow()

    private val _latestResult = MutableStateFlow<BenchmarkResultData?>(null)
    val latestResult: StateFlow<BenchmarkResultData?> = _latestResult.asStateFlow()

    // Updates state (GitHub Releases Patcher)
    private val _updateCheckResult = MutableStateFlow<UpdateCheckResult?>(null)
    val updateCheckResult: StateFlow<UpdateCheckResult?> = _updateCheckResult.asStateFlow()

    private val _isCheckingUpdates = MutableStateFlow(false)
    val isCheckingUpdates: StateFlow<Boolean> = _isCheckingUpdates.asStateFlow()

    private val _apkDownloadState = MutableStateFlow(ApkDownloadState())
    val apkDownloadState: StateFlow<ApkDownloadState> = _apkDownloadState.asStateFlow()

    // Compare runs selection
    private val _compareFirstRun = MutableStateFlow<BenchmarkEntity?>(null)
    val compareFirstRun: StateFlow<BenchmarkEntity?> = _compareFirstRun.asStateFlow()

    private val _compareSecondRun = MutableStateFlow<BenchmarkEntity?>(null)
    val compareSecondRun: StateFlow<BenchmarkEntity?> = _compareSecondRun.asStateFlow()

    private var benchmarkJob: Job? = null

    // For GPU frame sampling
    private val gpuFrameSamples = mutableListOf<Double>()

    init {
        refreshDeviceSpecs()
    }

    fun refreshDeviceSpecs() {
        _deviceSpecs.value = DeviceInfoProvider.getDeviceSpecs(getApplication())
    }

    fun recordGpuSample(fps: Double, frameTimeMs: Double) {
        if (_activeState.value.isGpuActive) {
            gpuFrameSamples.add(fps)
        }
    }

    fun runBenchmark(category: BenchmarkCategory) {
        if (_activeState.value.isRunning) return

        benchmarkJob = viewModelScope.launch {
            _activeState.value = ActiveBenchmarkState(
                isRunning = true,
                category = category,
                progress = 0f,
                statusMessage = "Инициализация ${category.displayName}..."
            )

            try {
                when (category) {
                    BenchmarkCategory.CPU -> runCpu()
                    BenchmarkCategory.RAM -> runRam()
                    BenchmarkCategory.STORAGE -> runStorage()
                    BenchmarkCategory.AI_NEURAL -> runAiNeural()
                    BenchmarkCategory.CRYPTO -> runCrypto()
                    BenchmarkCategory.RAYTRACING -> runRaytracing()
                    BenchmarkCategory.STABILITY -> runStability()
                    BenchmarkCategory.GPU -> runGpu()
                    BenchmarkCategory.OVERALL -> runOverall()
                    BenchmarkCategory.BATTERY -> runBatteryInfo()
                    BenchmarkCategory.DISPLAY -> runDisplayInfo()
                }
            } catch (e: Exception) {
                _activeState.value = ActiveBenchmarkState(
                    isRunning = false,
                    error = e.localizedMessage ?: "Тест был отменен или произошла ошибка"
                )
            }
        }
    }

    fun cancelBenchmark() {
        benchmarkJob?.cancel()
        _activeState.value = ActiveBenchmarkState(
            isRunning = false,
            error = "Тестирование прервано пользователем"
        )
    }

    private suspend fun runCpu() {
        val startTime = System.currentTimeMillis()
        val cpu = engine.runCpuBenchmark { prog, msg ->
            _activeState.value = _activeState.value.copy(progress = prog, statusMessage = msg)
        }
        val duration = System.currentTimeMillis() - startTime
        val overall = (cpu.singleCoreScore * 0.4 + cpu.multiCoreScore * 0.6).toInt()

        val result = BenchmarkResultData(
            category = BenchmarkCategory.CPU,
            overallScore = overall,
            durationMs = duration,
            cpuMetrics = cpu
        )
        saveResult(result)
    }

    private suspend fun runRam() {
        val startTime = System.currentTimeMillis()
        val ram = engine.runRamBenchmark { prog, msg ->
            _activeState.value = _activeState.value.copy(progress = prog, statusMessage = msg)
        }
        val duration = System.currentTimeMillis() - startTime
        val result = BenchmarkResultData(
            category = BenchmarkCategory.RAM,
            overallScore = ram.ramScore,
            durationMs = duration,
            ramMetrics = ram
        )
        saveResult(result)
    }

    private suspend fun runStorage() {
        val startTime = System.currentTimeMillis()
        val writeEnabled = settings.value.storageWriteEnabled
        val storage = engine.runStorageBenchmark(writeEnabled) { prog, msg ->
            _activeState.value = _activeState.value.copy(progress = prog, statusMessage = msg)
        }
        val duration = System.currentTimeMillis() - startTime
        val result = BenchmarkResultData(
            category = BenchmarkCategory.STORAGE,
            overallScore = storage.storageScore,
            durationMs = duration,
            storageMetrics = storage
        )
        saveResult(result)
    }

    private suspend fun runAiNeural() {
        val startTime = System.currentTimeMillis()
        val ai = engine.runAiNeuralBenchmark { prog, msg ->
            _activeState.value = _activeState.value.copy(progress = prog, statusMessage = msg)
        }
        val duration = System.currentTimeMillis() - startTime
        val result = BenchmarkResultData(
            category = BenchmarkCategory.AI_NEURAL,
            overallScore = ai.aiScore,
            durationMs = duration,
            aiNeuralMetrics = ai
        )
        saveResult(result)
    }

    private suspend fun runCrypto() {
        val startTime = System.currentTimeMillis()
        val crypto = engine.runCryptoBenchmark { prog, msg ->
            _activeState.value = _activeState.value.copy(progress = prog, statusMessage = msg)
        }
        val duration = System.currentTimeMillis() - startTime
        val result = BenchmarkResultData(
            category = BenchmarkCategory.CRYPTO,
            overallScore = crypto.cryptoScore,
            durationMs = duration,
            cryptoMetrics = crypto
        )
        saveResult(result)
    }

    private suspend fun runRaytracing() {
        val startTime = System.currentTimeMillis()
        val rt = engine.runRaytracingBenchmark { prog, msg ->
            _activeState.value = _activeState.value.copy(progress = prog, statusMessage = msg)
        }
        val duration = System.currentTimeMillis() - startTime
        val result = BenchmarkResultData(
            category = BenchmarkCategory.RAYTRACING,
            overallScore = rt.rayScore,
            durationMs = duration,
            raytracingMetrics = rt
        )
        saveResult(result)
    }

    private suspend fun runStability() {
        val startTime = System.currentTimeMillis()
        val stability = engine.runStabilityBenchmark(30) { prog, msg ->
            _activeState.value = _activeState.value.copy(progress = prog, statusMessage = msg)
        }
        val duration = System.currentTimeMillis() - startTime
        val result = BenchmarkResultData(
            category = BenchmarkCategory.STABILITY,
            overallScore = stability.stabilityScore,
            durationMs = duration,
            stabilityMetrics = stability
        )
        saveResult(result)
    }

    private suspend fun runGpu() {
        gpuFrameSamples.clear()
        _activeState.value = _activeState.value.copy(
            isGpuActive = true,
            statusMessage = "GPU: Рендеринг 3D-геометрии и динамических частиц..."
        )

        val startTime = System.currentTimeMillis()
        val testDurationMs = 5000L

        while (System.currentTimeMillis() - startTime < testDurationMs) {
            val elapsed = System.currentTimeMillis() - startTime
            val progress = (elapsed.toFloat() / testDurationMs).coerceIn(0f, 1f)
            val currentAvgFps = if (gpuFrameSamples.isNotEmpty()) gpuFrameSamples.average() else 60.0
            _activeState.value = _activeState.value.copy(
                progress = progress,
                statusMessage = "GPU: ${"%.1f".format(currentAvgFps)} FPS (${elapsed / 1000}с / ${testDurationMs / 1000}с)..."
            )
            kotlinx.coroutines.delay(100)
        }

        _activeState.value = _activeState.value.copy(isGpuActive = false)

        val samples = gpuFrameSamples.toList()
        val avgFps = if (samples.isNotEmpty()) samples.average() else 60.0
        val minFps = if (samples.isNotEmpty()) samples.minOrNull() ?: 60.0 else 60.0
        val maxFps = if (samples.isNotEmpty()) samples.maxOrNull() ?: 60.0 else 60.0
        val slowCount = samples.count { it < 30.0 }
        val slowPercent = if (samples.isNotEmpty()) (slowCount.toDouble() / samples.size) * 100.0 else 0.0

        val gpuScore = ScoringEngine.calculateGpuScore(avgFps, slowPercent)
        val gpuMetrics = GpuMetrics(
            gpuScore = gpuScore,
            averageFps = avgFps,
            minFps = minFps,
            maxFps = maxFps,
            slowFramesPercent = slowPercent,
            totalFrames = samples.size,
            frameTimeMedianMs = if (avgFps > 0) 1000.0 / avgFps else 16.6,
            durationMs = testDurationMs
        )

        val result = BenchmarkResultData(
            category = BenchmarkCategory.GPU,
            overallScore = gpuScore,
            durationMs = testDurationMs,
            gpuMetrics = gpuMetrics
        )
        saveResult(result)
    }

    private suspend fun runOverall() {
        val startTime = System.currentTimeMillis()

        // 1. CPU (20%)
        _activeState.value = _activeState.value.copy(progress = 0.05f, statusMessage = "1/6: Тестирование CPU...")
        val cpu = engine.runCpuBenchmark { p, msg ->
            _activeState.value = _activeState.value.copy(progress = p * 0.20f, statusMessage = "1/6: $msg")
        }

        // 2. RAM (15%)
        _activeState.value = _activeState.value.copy(progress = 0.20f, statusMessage = "2/6: Тестирование RAM...")
        val ram = engine.runRamBenchmark { p, msg ->
            _activeState.value = _activeState.value.copy(progress = 0.20f + p * 0.15f, statusMessage = "2/6: $msg")
        }

        // 3. Storage (15%)
        _activeState.value = _activeState.value.copy(progress = 0.35f, statusMessage = "3/6: Тестирование накопителя...")
        val storage = engine.runStorageBenchmark(settings.value.storageWriteEnabled) { p, msg ->
            _activeState.value = _activeState.value.copy(progress = 0.35f + p * 0.15f, statusMessage = "3/6: $msg")
        }

        // 4. AI / Neural NPU (15%)
        _activeState.value = _activeState.value.copy(progress = 0.50f, statusMessage = "4/6: Тестирование ИИ и тензоров...")
        val ai = engine.runAiNeuralBenchmark { p, msg ->
            _activeState.value = _activeState.value.copy(progress = 0.50f + p * 0.15f, statusMessage = "4/6: $msg")
        }

        // 5. Crypto (15%)
        _activeState.value = _activeState.value.copy(progress = 0.65f, statusMessage = "5/6: Тестирование криптографии...")
        val crypto = engine.runCryptoBenchmark { p, msg ->
            _activeState.value = _activeState.value.copy(progress = 0.65f + p * 0.15f, statusMessage = "5/6: $msg")
        }

        // 6. GPU quick stress (20%)
        _activeState.value = _activeState.value.copy(progress = 0.80f, statusMessage = "6/6: Тестирование GPU...", isGpuActive = true)
        gpuFrameSamples.clear()
        val gpuStartTime = System.currentTimeMillis()
        while (System.currentTimeMillis() - gpuStartTime < 3000L) {
            val elapsed = System.currentTimeMillis() - gpuStartTime
            val p = (elapsed.toFloat() / 3000L).coerceIn(0f, 1f)
            _activeState.value = _activeState.value.copy(progress = 0.80f + p * 0.20f)
            kotlinx.coroutines.delay(100)
        }
        _activeState.value = _activeState.value.copy(isGpuActive = false)

        val samples = gpuFrameSamples.toList()
        val avgFps = if (samples.isNotEmpty()) samples.average() else 60.0
        val slowCount = samples.count { it < 30.0 }
        val slowPercent = if (samples.isNotEmpty()) (slowCount.toDouble() / samples.size) * 100.0 else 0.0
        val gpuScore = ScoringEngine.calculateGpuScore(avgFps, slowPercent)
        val gpuMetrics = GpuMetrics(
            gpuScore = gpuScore,
            averageFps = avgFps,
            minFps = samples.minOrNull() ?: 60.0,
            maxFps = samples.maxOrNull() ?: 60.0,
            slowFramesPercent = slowPercent,
            totalFrames = samples.size,
            frameTimeMedianMs = if (avgFps > 0) 1000.0 / avgFps else 16.6,
            durationMs = 3000L
        )

        val totalDuration = System.currentTimeMillis() - startTime
        val compositeScore = ScoringEngine.calculateOverall(
            cpuSingle = cpu.singleCoreScore,
            cpuMulti = cpu.multiCoreScore,
            gpu = gpuScore,
            ram = ram.ramScore,
            storage = storage.storageScore,
            ai = ai.aiScore,
            crypto = crypto.cryptoScore
        )

        val result = BenchmarkResultData(
            category = BenchmarkCategory.OVERALL,
            overallScore = compositeScore,
            durationMs = totalDuration,
            cpuMetrics = cpu,
            gpuMetrics = gpuMetrics,
            ramMetrics = ram,
            storageMetrics = storage,
            aiNeuralMetrics = ai,
            cryptoMetrics = crypto
        )
        saveResult(result)
    }

    private suspend fun runBatteryInfo() {
        refreshDeviceSpecs()
        val specs = _deviceSpecs.value
        val score = specs.batteryPercent.coerceAtLeast(10) * 10
        val result = BenchmarkResultData(
            category = BenchmarkCategory.BATTERY,
            overallScore = score,
            durationMs = 500L
        )
        saveResult(result)
    }

    private suspend fun runDisplayInfo() {
        refreshDeviceSpecs()
        val specs = _deviceSpecs.value
        val score = (specs.refreshRateHz * 10).toInt()
        val result = BenchmarkResultData(
            category = BenchmarkCategory.DISPLAY,
            overallScore = score,
            durationMs = 500L
        )
        saveResult(result)
    }

    private suspend fun saveResult(result: BenchmarkResultData) {
        val adapter = moshi.adapter(BenchmarkResultData::class.java)
        val json = adapter.toJson(result)

        val entity = BenchmarkEntity(
            id = UUID.randomUUID().toString(),
            timestamp = result.timestamp,
            category = result.category.name,
            overallScore = result.overallScore,
            singleCoreScore = result.cpuMetrics?.singleCoreScore ?: 0,
            multiCoreScore = result.cpuMetrics?.multiCoreScore ?: 0,
            durationMs = result.durationMs,
            validationStatus = result.validationStatus.name,
            syncStatus = "LOCAL",
            methodologyVersion = result.methodologyVersion,
            deviceModel = "${result.deviceManufacturer} ${result.deviceModel}".trim(),
            rawMetricsJson = json
        )

        dao.insert(entity)
        _latestResult.value = result
        _activeState.value = ActiveBenchmarkState(isRunning = false)
    }

    fun deleteBenchmark(id: String) {
        viewModelScope.launch {
            dao.deleteById(id)
        }
    }

    fun clearAllHistory() {
        viewModelScope.launch {
            dao.clearAll()
        }
    }

    fun setCompareFirst(entity: BenchmarkEntity?) {
        _compareFirstRun.value = entity
    }

    fun setCompareSecond(entity: BenchmarkEntity?) {
        _compareSecondRun.value = entity
    }

    // STX format export & sharing
    fun exportToStxString(entity: BenchmarkEntity): String {
        return StxFormatManager.exportToStxString(entity, settings.value.userNickname)
    }

    fun exportToStxFile(context: Context, entity: BenchmarkEntity): File {
        return StxFormatManager.exportToStxFile(context, entity, settings.value.userNickname)
    }

    fun importStxData(rawInput: String, onComplete: (Boolean, String) -> Unit) {
        val result = StxFormatManager.importFromStxString(rawInput)
        result.onSuccess { entity ->
            viewModelScope.launch {
                dao.insert(entity)
                onComplete(true, "Отчёт успешно импортирован: ${entity.category} (${entity.overallScore} б.)")
            }
        }.onFailure { err ->
            onComplete(false, err.localizedMessage ?: "Ошибка импорта STX отчёта")
        }
    }

    fun createShareIntent(context: Context, entity: BenchmarkEntity): Intent {
        return StxFormatManager.createShareIntent(context, entity)
    }

    fun exportToJson(entity: BenchmarkEntity): String {
        return entity.rawMetricsJson
    }

    fun exportToCsv(entity: BenchmarkEntity): String {
        val sb = StringBuilder()
        sb.append("id,timestamp,category,overallScore,singleCoreScore,multiCoreScore,deviceModel,methodologyVersion\n")
        sb.append("${entity.id},${entity.timestamp},${entity.category},${entity.overallScore},${entity.singleCoreScore},${entity.multiCoreScore},\"${entity.deviceModel}\",${entity.methodologyVersion}\n")
        return sb.toString()
    }

    // Updater
    fun checkForUpdates() {
        viewModelScope.launch {
            _isCheckingUpdates.value = true
            _updateCheckResult.value = updateManager.checkForUpdates()
            _isCheckingUpdates.value = false
        }
    }

    fun downloadApk(asset: GitHubAsset) {
        viewModelScope.launch {
            _apkDownloadState.value = ApkDownloadState(isDownloading = true, progress = 0f)
            try {
                val file = updateManager.downloadApk(asset) { progress, current, total ->
                    _apkDownloadState.value = _apkDownloadState.value.copy(
                        progress = progress,
                        downloadedBytes = current,
                        totalBytes = total
                    )
                }
                val validation = updateManager.verifyApkPackage(file)
                _apkDownloadState.value = ApkDownloadState(
                    isDownloading = false,
                    progress = 1f,
                    downloadedFile = file,
                    validationMessage = validation.getOrNull(),
                    error = validation.exceptionOrNull()?.localizedMessage
                )
            } catch (e: Exception) {
                _apkDownloadState.value = ApkDownloadState(
                    isDownloading = false,
                    error = e.localizedMessage ?: "Ошибка скачивания APK"
                )
            }
        }
    }

    fun getInstallIntent(file: File) = updateManager.createInstallIntent(file)

    fun setOnboardingCompleted(completed: Boolean) {
        viewModelScope.launch {
            prefsRepo.setOnboardingCompleted(completed)
        }
    }

    fun setThemeMode(mode: String) {
        viewModelScope.launch { prefsRepo.setThemeMode(mode) }
    }

    fun setAccentColor(accent: String) {
        viewModelScope.launch { prefsRepo.setAccentColor(accent) }
    }

    fun setCardRadius(radiusDp: Int) {
        viewModelScope.launch { prefsRepo.setCardCornerRadius(radiusDp) }
    }

    fun setStorageWriteEnabled(enabled: Boolean) {
        viewModelScope.launch { prefsRepo.setStorageWriteEnabled(enabled) }
    }

    fun setAutoCheckUpdates(enabled: Boolean) {
        viewModelScope.launch { prefsRepo.setAutoCheckUpdates(enabled) }
    }

    fun setNotifyUpdates(enabled: Boolean) {
        viewModelScope.launch { prefsRepo.setNotifyUpdates(enabled) }
    }

    fun setNotifyReminders(enabled: Boolean) {
        viewModelScope.launch { prefsRepo.setNotifyReminders(enabled) }
    }

    fun updateNickname(nickname: String) {
        viewModelScope.launch { prefsRepo.setUserNickname(nickname) }
    }
}

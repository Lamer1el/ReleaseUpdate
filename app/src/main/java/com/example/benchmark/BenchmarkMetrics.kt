package com.example.benchmark

data class CpuMetrics(
    val singleCoreScore: Int,
    val multiCoreScore: Int,
    val singleCoreOpsPerSec: Double,
    val multiCoreOpsPerSec: Double,
    val integerOpsPerSec: Double,
    val floatingPointOpsPerSec: Double,
    val threadsUsed: Int,
    val durationMs: Long
)

data class GpuMetrics(
    val gpuScore: Int,
    val averageFps: Double,
    val minFps: Double,
    val maxFps: Double,
    val slowFramesPercent: Double,
    val totalFrames: Int,
    val frameTimeMedianMs: Double,
    val durationMs: Long
)

data class RamMetrics(
    val ramScore: Int,
    val readThroughputMBs: Double,
    val writeThroughputMBs: Double,
    val copyThroughputMBs: Double,
    val latencyNs: Double,
    val testBlockSizeKB: Int
)

data class StorageMetrics(
    val storageScore: Int,
    val seqReadMBs: Double,
    val seqWriteMBs: Double,
    val randReadIops: Double,
    val randWriteIops: Double,
    val writeTestsEnabled: Boolean
)

data class StabilityMetrics(
    val stabilityScore: Int,
    val stabilityPercent: Double,
    val peakScore: Int,
    val lowestScore: Int,
    val degradationPercent: Double,
    val durationSeconds: Int,
    val epochScores: List<Int>
)

data class AiNeuralMetrics(
    val aiScore: Int,
    val gflops: Double,
    val inferencesPerSec: Double,
    val matrixSize: Int,
    val latencyMs: Double,
    val simulatedLayers: Int
)

data class CryptoMetrics(
    val cryptoScore: Int,
    val sha256ThroughputMBs: Double,
    val aesThroughputMBs: Double,
    val hashesPerSec: Double,
    val totalBlocksProcessed: Long
)

data class RaytracingMetrics(
    val rayScore: Int,
    val raysPerSec: Double,
    val bouncesPerSec: Double,
    val spheresTested: Int,
    val durationMs: Long
)

data class BenchmarkResultData(
    val category: BenchmarkCategory,
    val overallScore: Int,
    val timestamp: Long = System.currentTimeMillis(),
    val durationMs: Long,
    val cpuMetrics: CpuMetrics? = null,
    val gpuMetrics: GpuMetrics? = null,
    val ramMetrics: RamMetrics? = null,
    val storageMetrics: StorageMetrics? = null,
    val stabilityMetrics: StabilityMetrics? = null,
    val aiNeuralMetrics: AiNeuralMetrics? = null,
    val cryptoMetrics: CryptoMetrics? = null,
    val raytracingMetrics: RaytracingMetrics? = null,
    val validationStatus: ValidationStatus = ValidationStatus.VALID,
    val methodologyVersion: String = ScoringEngine.METHODOLOGY_VERSION,
    val deviceModel: String = android.os.Build.MODEL,
    val deviceManufacturer: String = android.os.Build.MANUFACTURER,
    val androidVersion: String = android.os.Build.VERSION.RELEASE
)

object ScoringEngine {
    const val METHODOLOGY_VERSION = "Skitefi-v2.0"

    // Reference baseline values calibrated for 1000 baseline score
    private const val BASE_SINGLE_CORE_OPS = 2_500_000.0
    private const val BASE_MULTI_CORE_OPS = 12_000_000.0
    private const val BASE_GPU_FPS = 60.0
    private const val BASE_RAM_THROUGHPUT_MBS = 5_000.0
    private const val BASE_STORAGE_SEQ_MBS = 400.0
    private const val BASE_AI_GFLOPS = 4.0
    private const val BASE_CRYPTO_SHA256_MBS = 180.0
    private const val BASE_RAYTRACING_RAYS_PER_SEC = 250_000.0

    fun calculateCpuScore(singleOps: Double, multiOps: Double): Pair<Int, Int> {
        val single = ((singleOps / BASE_SINGLE_CORE_OPS) * 1000.0).toInt().coerceAtLeast(10)
        val multi = ((multiOps / BASE_MULTI_CORE_OPS) * 1000.0).toInt().coerceAtLeast(10)
        return Pair(single, multi)
    }

    fun calculateGpuScore(avgFps: Double, slowFramePercent: Double): Int {
        val fpsScore = (avgFps / BASE_GPU_FPS) * 1000.0
        val stabilityPenalty = (100.0 - slowFramePercent.coerceIn(0.0, 100.0)) / 100.0
        return (fpsScore * stabilityPenalty).toInt().coerceAtLeast(10)
    }

    fun calculateRamScore(readMBs: Double, writeMBs: Double, copyMBs: Double): Int {
        val avgThroughput = (readMBs + writeMBs + copyMBs) / 3.0
        return ((avgThroughput / BASE_RAM_THROUGHPUT_MBS) * 1000.0).toInt().coerceAtLeast(10)
    }

    fun calculateStorageScore(seqRead: Double, seqWrite: Double, randReadIops: Double, randWriteIops: Double, writeEnabled: Boolean): Int {
        val readScore = (seqRead / BASE_STORAGE_SEQ_MBS) * 500.0 + (randReadIops / 10000.0) * 500.0
        if (!writeEnabled) {
            return (readScore * 2.0).toInt().coerceAtLeast(10)
        }
        val writeScore = (seqWrite / BASE_STORAGE_SEQ_MBS) * 500.0 + (randWriteIops / 10000.0) * 500.0
        return ((readScore + writeScore) / 2.0).toInt().coerceAtLeast(10)
    }

    fun calculateAiScore(gflops: Double, latencyMs: Double): Int {
        val raw = (gflops / BASE_AI_GFLOPS) * 1000.0
        val latencyMultiplier = if (latencyMs > 0) (25.0 / latencyMs).coerceIn(0.4, 2.5) else 1.0
        return (raw * latencyMultiplier).toInt().coerceAtLeast(10)
    }

    fun calculateCryptoScore(sha256MBs: Double, aesMBs: Double): Int {
        val avgThroughput = (sha256MBs + aesMBs) / 2.0
        return ((avgThroughput / BASE_CRYPTO_SHA256_MBS) * 1000.0).toInt().coerceAtLeast(10)
    }

    fun calculateRaytracingScore(raysPerSec: Double): Int {
        return ((raysPerSec / BASE_RAYTRACING_RAYS_PER_SEC) * 1000.0).toInt().coerceAtLeast(10)
    }

    fun calculateOverall(
        cpuSingle: Int,
        cpuMulti: Int,
        gpu: Int,
        ram: Int,
        storage: Int,
        ai: Int = 1000,
        crypto: Int = 1000
    ): Int {
        // Balanced composite: 25% CPU, 25% GPU, 15% RAM, 15% Storage, 10% AI, 10% Crypto
        val cpuWeighted = (cpuSingle * 0.4 + cpuMulti * 0.6) * 0.25
        val gpuWeighted = gpu * 0.25
        val ramWeighted = ram * 0.15
        val storageWeighted = storage * 0.15
        val aiWeighted = ai * 0.10
        val cryptoWeighted = crypto * 0.10
        return (cpuWeighted + gpuWeighted + ramWeighted + storageWeighted + aiWeighted + cryptoWeighted).toInt().coerceAtLeast(10)
    }
}


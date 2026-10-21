package com.example.benchmark

import android.app.ActivityManager
import android.content.Context
import android.os.Build
import android.os.SystemClock
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import kotlinx.coroutines.yield
import java.io.File
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

class BenchmarkEngine(private val context: Context) {

    suspend fun runCpuBenchmark(onProgress: (Float, String) -> Unit): CpuMetrics = withContext(Dispatchers.Default) {
        onProgress(0.05f, "Прогрев ядер CPU...")
        warmupCpu()

        // 1. Single core integer & float
        onProgress(0.15f, "Однопоточный тест: целочисленные вычисления...")
        val singleIntOps = measureSingleCoreInteger()
        yield()

        onProgress(0.35f, "Однопоточный тест: вычисления с плавающей точкой...")
        val singleFloatOps = measureSingleCoreFloat()
        yield()

        val singleTotalOps = (singleIntOps + singleFloatOps) / 2.0

        // 2. Multi core test
        val cores = Runtime.getRuntime().availableProcessors().coerceAtLeast(1)
        onProgress(0.55f, "Многопоточный тест ($cores потоков)...")
        val multiOps = measureMultiCore(cores) { progress ->
            onProgress(0.55f + progress * 0.40f, "Многопоточный тест ($cores потоков)...")
        }

        val (singleScore, multiScore) = ScoringEngine.calculateCpuScore(singleTotalOps, multiOps)
        onProgress(1.0f, "Тестирование CPU завершено")

        CpuMetrics(
            singleCoreScore = singleScore,
            multiCoreScore = multiScore,
            singleCoreOpsPerSec = singleTotalOps,
            multiCoreOpsPerSec = multiOps,
            integerOpsPerSec = singleIntOps,
            floatingPointOpsPerSec = singleFloatOps,
            threadsUsed = cores,
            durationMs = 4500L
        )
    }

    private suspend fun warmupCpu() {
        val start = System.currentTimeMillis()
        var dummy = 0.0
        while (System.currentTimeMillis() - start < 500) {
            dummy += sin(dummy + 0.1)
        }
    }

    private suspend fun measureSingleCoreInteger(): Double {
        val durationMs = 1200L
        val startTime = SystemClock.elapsedRealtime()
        var iterations = 0L
        var accumulator = 123456789L

        while (SystemClock.elapsedRealtime() - startTime < durationMs) {
            for (i in 0 until 5000) {
                accumulator = (accumulator xor (accumulator shl 13))
                accumulator = (accumulator xor (accumulator ushr 17))
                accumulator = (accumulator xor (accumulator shl 5))
            }
            iterations += 5000
            if (iterations % 250000 == 0L) yield()
        }
        val actualSec = (SystemClock.elapsedRealtime() - startTime) / 1000.0
        return if (actualSec > 0) iterations / actualSec else 0.0
    }

    private suspend fun measureSingleCoreFloat(): Double {
        val durationMs = 1200L
        val startTime = SystemClock.elapsedRealtime()
        var iterations = 0L
        var x = 1.05

        while (SystemClock.elapsedRealtime() - startTime < durationMs) {
            for (i in 0 until 5000) {
                x = sqrt(x * 1.00001 + 0.00002) * 0.99999 + sin(x) * 0.00001
            }
            iterations += 5000
            if (iterations % 250000 == 0L) yield()
        }
        val actualSec = (SystemClock.elapsedRealtime() - startTime) / 1000.0
        return if (actualSec > 0) iterations / actualSec else 0.0
    }

    private suspend fun measureMultiCore(cores: Int, onSubProgress: suspend (Float) -> Unit): Double = coroutineScope {
        val testDurationMs = 1800L
        val tasks = (0 until cores).map { coreIndex ->
            async(Dispatchers.Default) {
                var localIterations = 0L
                var acc = 987654321L + coreIndex
                val start = SystemClock.elapsedRealtime()
                while (SystemClock.elapsedRealtime() - start < testDurationMs) {
                    for (i in 0 until 10000) {
                        acc = (acc xor (acc shl 7)) * 31L + (acc ushr 11)
                    }
                    localIterations += 10000
                    if (localIterations % 200000 == 0L) yield()
                }
                localIterations
            }
        }
        val results = tasks.awaitAll()
        val totalOps = results.sum()
        totalOps / (testDurationMs / 1000.0)
    }

    suspend fun runRamBenchmark(onProgress: (Float, String) -> Unit): RamMetrics = withContext(Dispatchers.Default) {
        onProgress(0.1f, "Выделение прямого буфера памяти...")
        val bufferSizeBytes = 16 * 1024 * 1024 // 16 MB safe allocation
        val directBuffer = ByteBuffer.allocateDirect(bufferSizeBytes)

        try {
            // Sequential Write
            onProgress(0.3f, "Измерение скорости записи в RAM...")
            val writeStart = System.nanoTime()
            val fillByte = 0xAA.toByte()
            val chunk = ByteArray(64 * 1024) { fillByte }
            var written = 0
            while (written < bufferSizeBytes) {
                val toWrite = minOf(chunk.size, bufferSizeBytes - written)
                directBuffer.put(chunk, 0, toWrite)
                written += toWrite
            }
            val writeElapsedNs = System.nanoTime() - writeStart
            val writeThroughputMBs = (bufferSizeBytes / (1024.0 * 1024.0)) / (writeElapsedNs / 1_000_000_000.0)
            yield()

            // Sequential Read
            onProgress(0.6f, "Измерение скорости чтения из RAM...")
            directBuffer.flip()
            val readStart = System.nanoTime()
            val readChunk = ByteArray(64 * 1024)
            var readTotal = 0
            while (directBuffer.hasRemaining()) {
                val toRead = minOf(readChunk.size, directBuffer.remaining())
                directBuffer.get(readChunk, 0, toRead)
                readTotal += toRead
            }
            val readElapsedNs = System.nanoTime() - readStart
            val readThroughputMBs = (bufferSizeBytes / (1024.0 * 1024.0)) / (readElapsedNs / 1_000_000_000.0)
            yield()

            // Memory Copy Throughput
            onProgress(0.85f, "Измерение скорости копирования блоков памяти...")
            val copyStart = System.nanoTime()
            val copyBuffer = ByteBuffer.allocateDirect(bufferSizeBytes)
            directBuffer.flip()
            copyBuffer.put(directBuffer)
            val copyElapsedNs = System.nanoTime() - copyStart
            val copyThroughputMBs = (bufferSizeBytes / (1024.0 * 1024.0)) / (copyElapsedNs / 1_000_000_000.0)

            // Latency estimate (random read)
            val latencyStart = System.nanoTime()
            val rnd = Random(42)
            var dummySum = 0
            val samples = 10000
            for (i in 0 until samples) {
                val pos = rnd.nextInt(bufferSizeBytes)
                dummySum += directBuffer.get(pos).toInt()
            }
            val latencyNs = (System.nanoTime() - latencyStart).toDouble() / samples

            val ramScore = ScoringEngine.calculateRamScore(readThroughputMBs, writeThroughputMBs, copyThroughputMBs)
            onProgress(1.0f, "Тестирование RAM завершено")

            RamMetrics(
                ramScore = ramScore,
                readThroughputMBs = readThroughputMBs,
                writeThroughputMBs = writeThroughputMBs,
                copyThroughputMBs = copyThroughputMBs,
                latencyNs = latencyNs,
                testBlockSizeKB = 16 * 1024
            )
        } finally {
            // Buffer will be garbage collected
        }
    }

    suspend fun runStorageBenchmark(writeEnabled: Boolean, onProgress: (Float, String) -> Unit): StorageMetrics = withContext(Dispatchers.IO) {
        val testFile = File(context.cacheDir, "skitefi_bench_temp.dat")
        val fileSizeBytes = 16L * 1024 * 1024 // 16 MB
        val bufferSize = 64 * 1024 // 64 KB block

        try {
            var seqWriteMBs = 0.0
            var randWriteIops = 0.0

            if (writeEnabled) {
                onProgress(0.15f, "Последовательная запись (16 MB)...")
                val writeStart = System.nanoTime()
                val writeBuffer = ByteArray(bufferSize) { (it % 255).toByte() }
                testFile.outputStream().buffered(bufferSize).use { output ->
                    var written = 0L
                    while (written < fileSizeBytes) {
                        output.write(writeBuffer)
                        written += bufferSize
                    }
                    output.flush()
                }
                val writeNs = System.nanoTime() - writeStart
                seqWriteMBs = (fileSizeBytes / (1024.0 * 1024.0)) / (writeNs / 1_000_000_000.0)
                yield()

                // Random Write 4KB
                onProgress(0.35f, "Случайная запись (4KB блоки)...")
                val randWriteOps = 500
                val block4k = ByteArray(4096) { 0x55 }
                val rnd = Random(123)
                val randWriteStart = System.nanoTime()
                RandomAccessFile(testFile, "rw").use { raf ->
                    for (i in 0 until randWriteOps) {
                        val maxPos = (fileSizeBytes - 4096).coerceAtLeast(0)
                        val offset = rnd.nextLong(maxPos)
                        raf.seek(offset)
                        raf.write(block4k)
                    }
                }
                val randWriteNs = System.nanoTime() - randWriteStart
                randWriteIops = randWriteOps / (randWriteNs / 1_000_000_000.0)
                yield()
            } else {
                // If writing was disabled, create a minimal file once for reading
                if (!testFile.exists() || testFile.length() < fileSizeBytes) {
                    testFile.writeBytes(ByteArray(bufferSize))
                }
            }

            // Sequential Read
            onProgress(0.65f, "Последовательное чтение (16 MB)...")
            val readBuffer = ByteArray(bufferSize)
            val readStart = System.nanoTime()
            var readTotal = 0L
            testFile.inputStream().buffered(bufferSize).use { input ->
                while (true) {
                    val count = input.read(readBuffer)
                    if (count <= 0) break
                    readTotal += count
                }
            }
            val readNs = System.nanoTime() - readStart
            val seqReadMBs = if (readTotal > 0) (readTotal / (1024.0 * 1024.0)) / (readNs / 1_000_000_000.0) else 0.0
            yield()

            // Random Read 4KB
            onProgress(0.85f, "Случайное чтение (4KB блоки)...")
            val randReadOps = 1000
            val rndRead = Random(456)
            val blockRead = ByteArray(4096)
            val randReadStart = System.nanoTime()
            RandomAccessFile(testFile, "r").use { raf ->
                val maxPos = (testFile.length() - 4096).coerceAtLeast(0)
                for (i in 0 until randReadOps) {
                    val offset = rndRead.nextLong(maxPos)
                    raf.seek(offset)
                    raf.read(blockRead)
                }
            }
            val randReadNs = System.nanoTime() - randReadStart
            val randReadIops = randReadOps / (randReadNs / 1_000_000_000.0)

            val score = ScoringEngine.calculateStorageScore(seqReadMBs, seqWriteMBs, randReadIops, randWriteIops, writeEnabled)
            onProgress(1.0f, "Тестирование накопителя завершено")

            StorageMetrics(
                storageScore = score,
                seqReadMBs = seqReadMBs,
                seqWriteMBs = seqWriteMBs,
                randReadIops = randReadIops,
                randWriteIops = randWriteIops,
                writeTestsEnabled = writeEnabled
            )
        } finally {
            if (testFile.exists()) {
                testFile.delete()
            }
        }
    }

    suspend fun runStabilityBenchmark(durationSec: Int = 30, onProgress: (Float, String) -> Unit): StabilityMetrics = withContext(Dispatchers.Default) {
        val epochs = 10
        val epochDurationMs = (durationSec * 1000L) / epochs
        val epochScores = mutableListOf<Int>()

        for (epoch in 0 until epochs) {
            val progress = epoch.toFloat() / epochs
            onProgress(progress, "Стабильность: эпоха ${epoch + 1}/$epochs...")

            val epochStart = SystemClock.elapsedRealtime()
            var ops = 0L
            var acc = 54321L + epoch
            while (SystemClock.elapsedRealtime() - epochStart < epochDurationMs) {
                for (i in 0 until 5000) {
                    acc = (acc xor (acc shl 9)) * 37L + (acc ushr 5)
                }
                ops += 5000
                if (ops % 100000 == 0L) yield()
            }
            val epochScore = (ops / 10000).toInt()
            epochScores.add(epochScore)
        }

        val peak = epochScores.maxOrNull() ?: 1
        val lowest = epochScores.minOrNull() ?: 1
        val degradationPercent = if (peak > 0) ((peak - lowest).toDouble() / peak) * 100.0 else 0.0
        val stabilityPercent = (100.0 - degradationPercent).coerceIn(0.0, 100.0)
        val stabilityScore = (peak * (stabilityPercent / 100.0)).toInt()

        onProgress(1.0f, "Стресс-тест стабильности завершён")
        StabilityMetrics(
            stabilityScore = stabilityScore,
            stabilityPercent = stabilityPercent,
            peakScore = peak,
            lowestScore = lowest,
            degradationPercent = degradationPercent,
            durationSeconds = durationSec,
            epochScores = epochScores
        )
    }

    suspend fun runAiNeuralBenchmark(onProgress: (Float, String) -> Unit): AiNeuralMetrics = withContext(Dispatchers.Default) {
        onProgress(0.05f, "Инициализация тензорных матриц NPU...")
        val matrixDim = 64
        val totalElements = matrixDim * matrixDim
        val matA = FloatArray(totalElements) { (it % 100) / 50.0f - 1.0f }
        val matB = FloatArray(totalElements) { ((it + 3) % 100) / 50.0f - 1.0f }
        val matC = FloatArray(totalElements)
        val bias = FloatArray(matrixDim) { 0.05f }

        val testDurationMs = 2800L
        val startTime = SystemClock.elapsedRealtime()
        var completedPasses = 0L
        var totalFlops = 0.0
        val flopsPerPass = 2.0 * matrixDim * matrixDim * matrixDim // GEMM operations count

        onProgress(0.25f, "Тест инференса: умножение матриц GEMM (64x64) + ReLU...")
        while (SystemClock.elapsedRealtime() - startTime < testDurationMs) {
            // GEMM: C = A * B
            for (i in 0 until matrixDim) {
                val rowOffset = i * matrixDim
                for (k in 0 until matrixDim) {
                    val aVal = matA[rowOffset + k]
                    val bRowOffset = k * matrixDim
                    for (j in 0 until matrixDim) {
                        matC[rowOffset + j] += aVal * matB[bRowOffset + j]
                    }
                }
                // Bias + ReLU activation
                for (j in 0 until matrixDim) {
                    val activated = matC[rowOffset + j] + bias[j]
                    matC[rowOffset + j] = if (activated > 0f) activated else 0f
                }
            }
            completedPasses++
            totalFlops += flopsPerPass
            if (completedPasses % 50 == 0L) {
                val p = (SystemClock.elapsedRealtime() - startTime).toFloat() / testDurationMs
                onProgress(0.25f + p * 0.70f, "Инференс тензоров ($completedPasses слоёв обработано)...")
                yield()
            }
        }

        val elapsedSec = (SystemClock.elapsedRealtime() - startTime) / 1000.0
        val gflops = if (elapsedSec > 0) (totalFlops / 1_000_000_000.0) / elapsedSec else 0.0
        val inferencesPerSec = if (elapsedSec > 0) completedPasses / elapsedSec else 0.0
        val latencyMs = if (completedPasses > 0) (elapsedSec * 1000.0) / completedPasses else 0.0
        val score = ScoringEngine.calculateAiScore(gflops, latencyMs)

        onProgress(1.0f, "Тестирование ИИ и нейросетей завершено")

        AiNeuralMetrics(
            aiScore = score,
            gflops = gflops,
            inferencesPerSec = inferencesPerSec,
            matrixSize = matrixDim,
            latencyMs = latencyMs,
            simulatedLayers = completedPasses.toInt()
        )
    }

    suspend fun runCryptoBenchmark(onProgress: (Float, String) -> Unit): CryptoMetrics = withContext(Dispatchers.Default) {
        onProgress(0.1f, "Инициализация криптографических примитивов...")

        // 1. SHA-256 Hashing
        onProgress(0.2f, "Тест хеширования SHA-256 (блочные данные)...")
        val md = java.security.MessageDigest.getInstance("SHA-256")
        val blockSize = 64 * 1024 // 64 KB block
        val testBlock = ByteArray(blockSize) { (it % 256).toByte() }
        val shaDurationMs = 1500L
        val shaStart = SystemClock.elapsedRealtime()
        var shaBlocks = 0L

        while (SystemClock.elapsedRealtime() - shaStart < shaDurationMs) {
            md.update(testBlock)
            val hash = md.digest()
            testBlock[0] = hash[0]
            shaBlocks++
            if (shaBlocks % 50 == 0L) yield()
        }
        val shaElapsedSec = (SystemClock.elapsedRealtime() - shaStart) / 1000.0
        val shaTotalBytes = shaBlocks * blockSize
        val sha256ThroughputMBs = (shaTotalBytes / (1024.0 * 1024.0)) / shaElapsedSec
        val hashesPerSec = shaBlocks / shaElapsedSec

        // 2. AES-128 Encryption
        onProgress(0.6f, "Тест блочного шифрования AES-128...")
        val keyBytes = ByteArray(16) { 0x5A }
        val secretKey = javax.crypto.spec.SecretKeySpec(keyBytes, "AES")
        val cipher = javax.crypto.Cipher.getInstance("AES/ECB/PKCS5Padding")
        cipher.init(javax.crypto.Cipher.ENCRYPT_MODE, secretKey)

        val aesDurationMs = 1500L
        val aesStart = SystemClock.elapsedRealtime()
        var aesBlocks = 0L
        val plainBlock = ByteArray(32 * 1024) { (it % 128).toByte() }

        while (SystemClock.elapsedRealtime() - aesStart < aesDurationMs) {
            val enc = cipher.doFinal(plainBlock)
            plainBlock[0] = enc[0]
            aesBlocks++
            if (aesBlocks % 50 == 0L) yield()
        }
        val aesElapsedSec = (SystemClock.elapsedRealtime() - aesStart) / 1000.0
        val aesTotalBytes = aesBlocks * plainBlock.size
        val aesThroughputMBs = (aesTotalBytes / (1024.0 * 1024.0)) / aesElapsedSec

        val cryptoScore = ScoringEngine.calculateCryptoScore(sha256ThroughputMBs, aesThroughputMBs)
        onProgress(1.0f, "Тестирование криптографии завершено")

        CryptoMetrics(
            cryptoScore = cryptoScore,
            sha256ThroughputMBs = sha256ThroughputMBs,
            aesThroughputMBs = aesThroughputMBs,
            hashesPerSec = hashesPerSec,
            totalBlocksProcessed = shaBlocks + aesBlocks
        )
    }

    suspend fun runRaytracingBenchmark(onProgress: (Float, String) -> Unit): RaytracingMetrics = withContext(Dispatchers.Default) {
        onProgress(0.1f, "Построение 3D сцены для трассировки лучей...")
        // Define simple geometric spheres: (x, y, z, radius)
        val spheres = arrayOf(
            floatArrayOf(0f, 0f, 5f, 1.2f),
            floatArrayOf(-2f, 1f, 6f, 0.9f),
            floatArrayOf(2f, -0.5f, 5.5f, 1.0f),
            floatArrayOf(0f, -101f, 5f, 100f) // Ground plane sphere
        )

        val testDurationMs = 2600L
        val startTime = SystemClock.elapsedRealtime()
        var totalRays = 0L
        var totalBounces = 0L

        onProgress(0.25f, "Расчёт векторных пересечений лучей и отражений...")
        while (SystemClock.elapsedRealtime() - startTime < testDurationMs) {
            for (step in 0 until 500) {
                // Ray origin and direction
                val ox = (Random.nextFloat() - 0.5f) * 2f
                val oy = (Random.nextFloat() - 0.5f) * 2f
                val oz = 0f

                var dx = (Random.nextFloat() - 0.5f)
                var dy = (Random.nextFloat() - 0.5f)
                var dz = 1.0f
                val len = sqrt(dx * dx + dy * dy + dz * dz)
                dx /= len
                dy /= len
                dz /= len

                totalRays++

                // Intersect with spheres
                for (s in spheres) {
                    val cx = s[0]; val cy = s[1]; val cz = s[2]; val r = s[3]
                    val ocx = ox - cx; val ocy = oy - cy; val ocz = oz - cz
                    val b = 2.0f * (ocx * dx + ocy * dy + ocz * dz)
                    val c = ocx * ocx + ocy * ocy + ocz * ocz - r * r
                    val disc = b * b - 4f * c
                    if (disc > 0f) {
                        totalBounces++
                        break
                    }
                }
            }
            if (totalRays % 25000 == 0L) {
                val p = (SystemClock.elapsedRealtime() - startTime).toFloat() / testDurationMs
                onProgress(0.25f + p * 0.70f, "Трассировка ($totalRays лучей)...")
                yield()
            }
        }

        val elapsedSec = (SystemClock.elapsedRealtime() - startTime) / 1000.0
        val raysPerSec = if (elapsedSec > 0) totalRays / elapsedSec else 0.0
        val bouncesPerSec = if (elapsedSec > 0) totalBounces / elapsedSec else 0.0
        val rayScore = ScoringEngine.calculateRaytracingScore(raysPerSec)

        onProgress(1.0f, "Тестирование Ray Tracing завершено")

        RaytracingMetrics(
            rayScore = rayScore,
            raysPerSec = raysPerSec,
            bouncesPerSec = bouncesPerSec,
            spheresTested = spheres.size,
            durationMs = testDurationMs
        )
    }

    fun getMemoryInfo(): Pair<Long, Long> {
        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
        val mi = ActivityManager.MemoryInfo()
        am?.getMemoryInfo(mi)
        return Pair(mi.availMem, mi.totalMem)
    }
}


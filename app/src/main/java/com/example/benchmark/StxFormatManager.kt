package com.example.benchmark

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import com.example.data.local.BenchmarkEntity
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import java.io.File
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import android.util.Base64

data class StxReportPayload(
    val format: String = "SKITEFI_STX",
    val version: String = "1.0",
    val id: String,
    val category: String,
    val overallScore: Int,
    val singleCoreScore: Int,
    val multiCoreScore: Int,
    val durationMs: Long,
    val timestamp: Long,
    val deviceModel: String,
    val deviceManufacturer: String,
    val androidVersion: String,
    val author: String,
    val methodologyVersion: String,
    val validationStatus: String,
    val rawMetricsJson: String,
    val checksum: String
)

object StxFormatManager {

    private const val PREFIX = "STX1_"
    private val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
    private val adapter = moshi.adapter(StxReportPayload::class.java)

    fun calculateChecksum(id: String, score: Int, timestamp: Long, model: String): String {
        val input = "$id:$score:$timestamp:$model:skitefi_secret_salt_2026"
        val md = MessageDigest.getInstance("SHA-256")
        val bytes = md.digest(input.toByteArray(StandardCharsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }.take(16)
    }

    fun exportToStxString(entity: BenchmarkEntity, authorName: String = "User"): String {
        val checksum = calculateChecksum(entity.id, entity.overallScore, entity.timestamp, entity.deviceModel)
        val payload = StxReportPayload(
            id = entity.id,
            category = entity.category,
            overallScore = entity.overallScore,
            singleCoreScore = entity.singleCoreScore,
            multiCoreScore = entity.multiCoreScore,
            durationMs = entity.durationMs,
            timestamp = entity.timestamp,
            deviceModel = entity.deviceModel,
            deviceManufacturer = android.os.Build.MANUFACTURER,
            androidVersion = android.os.Build.VERSION.RELEASE,
            author = authorName,
            methodologyVersion = entity.methodologyVersion,
            validationStatus = entity.validationStatus,
            rawMetricsJson = entity.rawMetricsJson,
            checksum = checksum
        )
        val json = adapter.toJson(payload)
        val b64 = Base64.encodeToString(json.toByteArray(StandardCharsets.UTF_8), Base64.NO_WRAP)
        return "$PREFIX$b64"
    }

    fun exportToStxFile(context: Context, entity: BenchmarkEntity, authorName: String = "User"): File {
        val stxString = exportToStxString(entity, authorName)
        val fileName = "skitefi_${entity.category.lowercase()}_${entity.overallScore}_${System.currentTimeMillis()}.stx"
        val exportDir = File(context.cacheDir, "exports").apply { mkdirs() }
        val file = File(exportDir, fileName)
        file.writeText(stxString, StandardCharsets.UTF_8)
        return file
    }

    fun importFromStxString(rawInput: String): Result<BenchmarkEntity> {
        return try {
            val trimmed = rawInput.trim()
            val jsonString = if (trimmed.startsWith(PREFIX)) {
                val b64 = trimmed.substring(PREFIX.length)
                String(Base64.decode(b64, Base64.DEFAULT), StandardCharsets.UTF_8)
            } else if (trimmed.startsWith("{") && trimmed.endsWith("}")) {
                trimmed
            } else {
                return Result.failure(IllegalArgumentException("Неверный формат STX файла (ожидается префикс STX1_)"))
            }

            val payload = adapter.fromJson(jsonString)
                ?: return Result.failure(IllegalArgumentException("Не удалось распарсить STX отчёт"))

            val expectedChecksum = calculateChecksum(payload.id, payload.overallScore, payload.timestamp, payload.deviceModel)
            val isValid = payload.checksum.equals(expectedChecksum, ignoreCase = true)
            val validation = if (isValid) ValidationStatus.VALID.name else ValidationStatus.ANOMALOUS.name

            val entity = BenchmarkEntity(
                id = payload.id,
                timestamp = payload.timestamp,
                category = payload.category,
                overallScore = payload.overallScore,
                singleCoreScore = payload.singleCoreScore,
                multiCoreScore = payload.multiCoreScore,
                durationMs = payload.durationMs,
                validationStatus = validation,
                syncStatus = SyncStatus.STX_IMPORTED.name,
                methodologyVersion = payload.methodologyVersion,
                deviceModel = "${payload.deviceManufacturer} ${payload.deviceModel} (Автор: ${payload.author})",
                rawMetricsJson = payload.rawMetricsJson,
                cloudDocId = null
            )
            Result.success(entity)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun generateShareReportText(entity: BenchmarkEntity): String {
        val dateStr = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault()).format(Date(entity.timestamp))
        val sb = StringBuilder()
        sb.append("⚡ [SKITEFI BENCHMARK REPORT] ⚡\n")
        sb.append("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n")
        sb.append("📱 Устройство: ${entity.deviceModel}\n")
        sb.append("🏆 Категория: ${entity.category}\n")
        sb.append("🔥 Результат: ${entity.overallScore} баллов\n")
        sb.append("⏱ Длительность: ${entity.durationMs / 1000.0} с\n")
        sb.append("📅 Дата теста: $dateStr\n")
        sb.append("🛡 Статус: ${entity.validationStatus}\n")
        if (entity.singleCoreScore > 0 || entity.multiCoreScore > 0) {
            sb.append("⚙️ Single-core: ${entity.singleCoreScore} | Multi-core: ${entity.multiCoreScore}\n")
        }
        sb.append("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n")
        sb.append("Отчёт экспортирован в формате Skitefi (.stx)\n")
        sb.append("Тестируйте и сравнивайте свои устройства!")
        return sb.toString()
    }

    fun createShareIntent(context: Context, entity: BenchmarkEntity): Intent {
        val reportText = generateShareReportText(entity)
        val file = exportToStxFile(context, entity)
        val fileUri: Uri = try {
            FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        } catch (e: Exception) {
            Uri.fromFile(file)
        }

        return Intent(Intent.ACTION_SEND).apply {
            type = "*/*"
            putExtra(Intent.EXTRA_SUBJECT, "Skitefi Benchmark: ${entity.category} (${entity.overallScore} б.)")
            putExtra(Intent.EXTRA_TEXT, reportText)
            putExtra(Intent.EXTRA_STREAM, fileUri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }
}

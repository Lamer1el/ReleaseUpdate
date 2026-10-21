package com.example.updater

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.core.content.FileProvider
import com.example.data.remote.GitHubAsset
import com.example.data.remote.GitHubRelease
import com.example.data.remote.GitHubReleaseApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.security.MessageDigest

sealed class UpdateCheckResult {
    data object NoReleasesFound : UpdateCheckResult()
    data class UpToDate(val currentVersion: String, val latestRelease: GitHubRelease) : UpdateCheckResult()
    data class UpdateAvailable(
        val currentVersion: String,
        val latestRelease: GitHubRelease,
        val apkAsset: GitHubAsset
    ) : UpdateCheckResult()
    data class UpdateAvailableNoApk(
        val currentVersion: String,
        val latestRelease: GitHubRelease
    ) : UpdateCheckResult()
    data class Error(val message: String) : UpdateCheckResult()
}

data class VersionTriple(val major: Int, val minor: Int, val patch: Int) : Comparable<VersionTriple> {
    override fun compareTo(other: VersionTriple): Int {
        if (major != other.major) return major.compareTo(other.major)
        if (minor != other.minor) return minor.compareTo(other.minor)
        return patch.compareTo(other.patch)
    }

    companion object {
        fun parse(versionStr: String): VersionTriple? {
            val cleaned = versionStr.trim().removePrefix("v").removePrefix("V")
            val parts = cleaned.split(".").mapNotNull { it.takeWhile { char -> char.isDigit() }.toIntOrNull() }
            return when {
                parts.size >= 3 -> VersionTriple(parts[0], parts[1], parts[2])
                parts.size == 2 -> VersionTriple(parts[0], parts[1], 0)
                parts.size == 1 -> VersionTriple(parts[0], 0, 0)
                else -> null
            }
        }
    }
}

class UpdateManager(private val context: Context, private val api: GitHubReleaseApi = GitHubReleaseApi.create()) {

    suspend fun checkForUpdates(): UpdateCheckResult = withContext(Dispatchers.IO) {
        try {
            val response = api.getAllReleases()
            if (!response.isSuccessful) {
                return@withContext UpdateCheckResult.Error("Ошибка GitHub API: HTTP ${response.code()}")
            }
            val releases = response.body().orEmpty().filter { !it.draft && !it.prerelease }
            if (releases.isEmpty()) {
                return@withContext UpdateCheckResult.NoReleasesFound
            }

            val latest = releases.first()
            val currentPkgInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.packageManager.getPackageInfo(context.packageName, PackageManager.PackageInfoFlags.of(0))
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.getPackageInfo(context.packageName, 0)
            }
            val currentVersionName = currentPkgInfo.versionName ?: "1.0"

            val currentVer = VersionTriple.parse(currentVersionName) ?: VersionTriple(1, 0, 0)
            val releaseVer = VersionTriple.parse(latest.tagName)

            if (releaseVer == null) {
                return@withContext UpdateCheckResult.Error("Тег релиза '${latest.tagName}' не соответствует семантическому версионированию")
            }

            val apkAsset = latest.assets.firstOrNull { it.name.endsWith(".apk", ignoreCase = true) }

            if (releaseVer > currentVer) {
                if (apkAsset != null) {
                    UpdateCheckResult.UpdateAvailable(currentVersionName, latest, apkAsset)
                } else {
                    UpdateCheckResult.UpdateAvailableNoApk(currentVersionName, latest)
                }
            } else {
                UpdateCheckResult.UpToDate(currentVersionName, latest)
            }
        } catch (e: Exception) {
            UpdateCheckResult.Error(e.localizedMessage ?: "Сетевая ошибка при проверке обновлений")
        }
    }

    suspend fun downloadApk(
        asset: GitHubAsset,
        onProgress: (Float, Long, Long) -> Unit
    ): File = withContext(Dispatchers.IO) {
        val updatesDir = File(context.cacheDir, "apk_updates")
        if (!updatesDir.exists()) updatesDir.mkdirs()

        val destinationFile = File(updatesDir, asset.name)
        if (destinationFile.exists()) destinationFile.delete()

        val response = api.downloadFile(asset.browserDownloadUrl)
        if (!response.isSuccessful || response.body() == null) {
            throw IllegalStateException("Не удалось загрузить файл: HTTP ${response.code()}")
        }

        val body = response.body()!!
        val contentLength = if (asset.size > 0) asset.size else body.contentLength()

        var inputStream: InputStream? = null
        var outputStream: FileOutputStream? = null

        try {
            inputStream = body.byteStream()
            outputStream = FileOutputStream(destinationFile)
            val buffer = ByteArray(8192)
            var bytesRead: Int
            var totalBytesRead = 0L

            while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                outputStream.write(buffer, 0, bytesRead)
                totalBytesRead += bytesRead
                val progress = if (contentLength > 0) (totalBytesRead.toFloat() / contentLength).coerceIn(0f, 1f) else 0f
                onProgress(progress, totalBytesRead, contentLength)
            }
            outputStream.flush()
            destinationFile
        } finally {
            inputStream?.close()
            outputStream?.close()
        }
    }

    fun verifyApkPackage(apkFile: File): Result<String> {
        val pm = context.packageManager
        val archiveInfo = pm.getPackageArchiveInfo(apkFile.absolutePath, 0)
            ?: return Result.failure(IllegalStateException("Файл повреждён или не является корректным Android APK"))

        val expectedPkg = context.packageName
        if (archiveInfo.packageName != expectedPkg) {
            return Result.failure(
                IllegalStateException("Несовпадение Package Name: в APK '${archiveInfo.packageName}', ожидалось '$expectedPkg'")
            )
        }

        val currentVersionCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            context.packageManager.getPackageInfo(expectedPkg, 0).longVersionCode
        } else {
            @Suppress("DEPRECATION")
            context.packageManager.getPackageInfo(expectedPkg, 0).versionCode.toLong()
        }

        val apkVersionCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            archiveInfo.longVersionCode
        } else {
            @Suppress("DEPRECATION")
            archiveInfo.versionCode.toLong()
        }

        if (apkVersionCode < currentVersionCode) {
            return Result.failure(
                IllegalStateException("Попытка даунгрейда: версия APK ($apkVersionCode) ниже установленной ($currentVersionCode)")
            )
        }

        return Result.success("APK валиден: ${archiveInfo.packageName} v${archiveInfo.versionName} ($apkVersionCode)")
    }

    fun calculateSha256(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { input ->
            val buffer = ByteArray(8192)
            var bytesRead: Int
            while (input.read(buffer).also { bytesRead = it } != -1) {
                digest.update(buffer, 0, bytesRead)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    fun createInstallIntent(apkFile: File): Intent {
        val contentUri: Uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            apkFile
        )
        return Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(contentUri, "application/vnd.android.package-archive")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
        }
    }
}

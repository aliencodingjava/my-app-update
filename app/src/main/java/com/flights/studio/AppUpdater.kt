package com.flights.studio


import android.content.ClipData
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.net.toUri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedInputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale

data class DownloadProgress(
    val percent: Int,
    val downloadedMb: Double,
    val speedMbPerSec: Double,
    val timeLeftText: String
)

object AppUpdater {

    suspend fun getCurrentVersionCode(context: Context): Long = withContext(Dispatchers.Default) {
        val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            packageInfo.longVersionCode
        } else {
            @Suppress("DEPRECATION")
            packageInfo.versionCode.toLong()
        }
    }

    suspend fun downloadApk(
        context: Context,
        apkUrl: String,
        onProgress: suspend (DownloadProgress) -> Unit
    ): Uri = withContext(Dispatchers.IO) {
        val connection = (URL(apkUrl).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 15_000
            readTimeout = 30_000
            connect()
        }

        if (connection.responseCode != HttpURLConnection.HTTP_OK) {
            connection.disconnect()
            throw IOException("Server returned HTTP ${connection.responseCode}")
        }

        val fileLength = connection.contentLength
        if (fileLength <= 0) {
            connection.disconnect()
            throw IOException("Invalid file length")
        }

        val uri = createDownloadUri(context) ?: run {
            connection.disconnect()
            throw IOException("Failed to create download URI")
        }

        val startTime = System.currentTimeMillis()
        var total: Long = 0

        BufferedInputStream(connection.inputStream).use { input ->
            context.contentResolver.openOutputStream(uri)?.buffered().use { output ->
                if (output == null) throw IOException("Failed to open output stream")

                val data = ByteArray(8 * 1024)
                while (true) {
                    val count = input.read(data)
                    if (count == -1) break

                    output.write(data, 0, count)
                    total += count

                    val elapsedSec = ((System.currentTimeMillis() - startTime) / 1000.0).coerceAtLeast(0.1)
                    val speedMbPerSec = total / 1024.0 / 1024.0 / elapsedSec
                    val downloadedMb = total / 1024.0 / 1024.0
                    val percent = ((total * 100) / fileLength).toInt().coerceIn(0, 100)
                    val timeLeft = calculateTimeLeft(total, fileLength, startTime)

                    onProgress(
                        DownloadProgress(
                            percent = percent,
                            downloadedMb = downloadedMb,
                            speedMbPerSec = speedMbPerSec,
                            timeLeftText = timeLeft
                        )
                    )
                }
            }
        }

        connection.disconnect()
        uri
    }

    private fun createDownloadUri(context: Context): Uri? {
        val values = ContentValues().apply {
            put(MediaStore.Downloads.DISPLAY_NAME, "update_${System.currentTimeMillis()}.apk")
            put(MediaStore.Downloads.MIME_TYPE, "application/vnd.android.package-archive")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
            }
        }

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            context.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
        } else {
            @Suppress("DEPRECATION")
            context.contentResolver.insert(MediaStore.Files.getContentUri("external"), values)
        }
    }

    fun installApk(context: Context, fileUri: Uri) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.BAKLAVA &&
            !context.packageManager.canRequestPackageInstalls()
        ) {
            val settingsIntent = Intent(android.provider.Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES)
                .setData("package:${context.packageName}".toUri())
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(settingsIntent)
            return
        }

        val installIntent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(fileUri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            clipData = ClipData.newUri(context.contentResolver, "APK", fileUri)
        }

        context.packageManager
            .queryIntentActivities(installIntent, PackageManager.MATCH_DEFAULT_ONLY)
            .forEach {
                context.grantUriPermission(
                    it.activityInfo.packageName,
                    fileUri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            }

        context.startActivity(installIntent)
    }

    private fun calculateTimeLeft(totalBytesRead: Long, fileLength: Int, startTime: Long): String {
        val elapsedTime = System.currentTimeMillis() - startTime
        val speed = totalBytesRead / (elapsedTime / 1000.0).coerceAtLeast(0.1)
        val remaining = fileLength - totalBytesRead
        val secondsLeft = remaining / speed

        val minutes = (secondsLeft / 60).toInt()
        val seconds = (secondsLeft % 60).toInt()
        return String.format(Locale.getDefault(), "%d:%02d", minutes, seconds)
    }
}
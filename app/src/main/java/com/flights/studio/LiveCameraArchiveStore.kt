package com.flights.studio

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File

data class LiveCameraArchiveImage(
    val cameraKey: String,
    val title: String,
    val timestampMillis: Long,
    val file: File
) {
    val expiresAtMillis: Long get() = timestampMillis + LiveCameraArchiveStore.RETENTION_MS
}

object LiveCameraArchiveStore {
    const val RETENTION_MS = 48L * 60L * 60L * 1000L

    private const val DIR_NAME = "live_camera_archive"
    private val client = OkHttpClient()

    suspend fun saveSnapshots(context: Context, cards: List<CameraCard>) = withContext(Dispatchers.IO) {
        cleanupExpired(context)
        cards.forEach { card ->
            runCatching { saveSnapshot(context, card) }
        }
    }

    suspend fun saveSnapshot(context: Context, card: CameraCard) = withContext(Dispatchers.IO) {
        cleanupExpired(context)
        val request = Request.Builder()
            .url(card.url)
            .get()
            .build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) return@withContext
            val bytes = response.body.bytes()
            if (bytes.isEmpty()) return@withContext

            val timestamp = System.currentTimeMillis()
            val cameraKey = cameraKey(card.title)
            val cameraDir = cameraDir(context, cameraKey)
            cameraDir.mkdirs()
            File(cameraDir, "$timestamp.jpg").writeBytes(bytes)
        }
    }

    fun listImages(context: Context, cameraKey: String? = null): List<LiveCameraArchiveImage> {
        cleanupExpired(context)
        val keys = cameraKey?.let(::listOf) ?: CAMERA_KEYS
        return keys.flatMap { key ->
            cameraDir(context, key)
                .listFiles { file -> file.isFile && file.extension.equals("jpg", ignoreCase = true) }
                .orEmpty()
                .mapNotNull { file ->
                    val timestamp = file.name.substringBefore(".").toLongOrNull() ?: return@mapNotNull null
                    LiveCameraArchiveImage(
                        cameraKey = key,
                        title = cameraTitle(key),
                        timestampMillis = timestamp,
                        file = file
                    )
                }
            }
            .sortedByDescending { it.timestampMillis }
    }

    fun cleanupExpired(context: Context) {
        val root = archiveDir(context)
        if (!root.exists()) return
        val cutoff = System.currentTimeMillis() - RETENTION_MS
        root.walkTopDown()
            .filter { it.isFile && it.extension.equals("jpg", ignoreCase = true) }
            .forEach { file ->
                val timestamp = file.name.substringBefore(".").substringBefore("_").toLongOrNull()
                if (timestamp != null && timestamp < cutoff) {
                    runCatching { file.delete() }
                }
            }
    }

    fun cameraKey(title: String): String {
        val normalized = title.lowercase()
        return when {
            "north" in normalized -> "north"
            "south" in normalized -> "south"
            "curb" in normalized -> "curb"
            else -> normalized.replace(Regex("[^a-z0-9]+"), "-").trim('-').ifBlank { "camera" }
        }
    }

    fun cameraTitle(key: String): String {
        return when (key) {
            "curb" -> "Curb"
            "north" -> "North"
            "south" -> "South"
            else -> key.replace("-", " ")
                .replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
        }
    }

    private fun archiveDir(context: Context): File =
        File(context.filesDir, DIR_NAME)

    private fun cameraDir(context: Context, cameraKey: String): File =
        File(archiveDir(context), cameraKey)

    private val CAMERA_KEYS = listOf("curb", "north", "south")
}

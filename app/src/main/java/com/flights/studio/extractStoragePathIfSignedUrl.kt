package com.flights.studio

import android.content.Context
import java.net.URLDecoder
import androidx.core.net.toUri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.net.URL

/* ============================================================
   In-memory cache for Supabase signed URLs
   (NO extra files, NO extra packages)
   ============================================================ */

object SignedUrlCache {

    private data class Entry(
        val url: String,
        val expiresAtMs: Long
    )

    private val map = mutableMapOf<String, Entry>()

    fun getValid(path: String): String? {
        val now = System.currentTimeMillis()
        val entry = map[path] ?: return null
        return if (entry.expiresAtMs > now) entry.url else null
    }

    fun put(path: String, url: String, ttlSeconds: Int) {
        val expiresAt = System.currentTimeMillis() + ttlSeconds * 1000L
        map[path] = Entry(url, expiresAt)
    }

    fun invalidate(path: String) {
        map.remove(path)
    }
}

/* ============================================================
   Signed URL → storage path extractor + cache seeding
   ============================================================ */

fun extractStoragePathIfSignedUrl(
    url: String,
    bucket: String,
    cacheTtlSeconds: Int = 60 * 60 // 1 hour
): String? {
    return runCatching {
        val u = url.toUri()
        val path = u.path ?: return null

        // /storage/v1/object/sign/<bucket>/<encodedPath>
        val marker = "/storage/v1/object/sign/$bucket/"
        val idx = path.indexOf(marker)
        if (idx == -1) return null

        val encodedObjectPath = path.substring(idx + marker.length)

        val objectPath = encodedObjectPath
            .split("/")
            .joinToString("/") { URLDecoder.decode(it, "UTF-8") }

        // ✅ seed cache so Compose/Glide can reuse instantly
        SignedUrlCache.put(
            path = objectPath,
            url = url,
            ttlSeconds = cacheTtlSeconds
        )

        objectPath
    }.getOrNull()
}

/* ============================================================
   Local disk cache for avatar (offline + instant load)
   Keyed by raw storage path (so it survives app restarts)
   ============================================================ */
object AvatarDiskCache {

    private fun fileNameFor(rawPhotoPath: String): String {
        // rawPhotoPath example: "profiles/<uid>/avatar.jpg"
        // Hash keeps it filename-safe.
        val key = rawPhotoPath.hashCode()
        return "avatar_cache_$key.jpg"
    }

    fun localFile(context: Context, rawPhotoPath: String): File {
        return File(context.filesDir, fileNameFor(rawPhotoPath))
    }

    // ✅ THIS is what your code is calling
    fun delete(context: Context, rawPhotoPath: String) {
        runCatching {
            val f = localFile(context, rawPhotoPath)
            if (f.exists()) f.delete()
        }
    }

    suspend fun cacheFromSignedUrl(
        context: Context,
        rawPhotoPath: String,
        signedUrl: String
    ): File? = withContext(Dispatchers.IO) {
        try {
            if (rawPhotoPath.isBlank() || signedUrl.isBlank()) return@withContext null

            val bytes = URL(signedUrl).openStream().use { it.readBytes() }
            if (bytes.isEmpty()) return@withContext null

            val f = localFile(context, rawPhotoPath)
            f.writeBytes(bytes)
            f
        } catch (_: Exception) {
            null
        }
    }

    fun invalidate(context: Context, rawPhotoPath: String) {
        runCatching { localFile(context, rawPhotoPath).delete() }
    }
}


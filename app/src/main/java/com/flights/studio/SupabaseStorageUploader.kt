package com.flights.studio

import android.content.Context
import android.net.Uri
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.net.URLEncoder

object SupabaseStorageUploader {
    private const val TAG = "SupabaseStorageUploader"

    private val supabaseUrl: String get() = BuildConfig.SUPABASE_URL.trimEnd('/')
    private val supabaseAnonKey: String get() = BuildConfig.SUPABASE_ANON_KEY
    private val client by lazy { OkHttpClient() }

    // ----------------------------
    // Upload
    // ----------------------------
    /**
     * Uploads the image bytes to Supabase Storage and returns the STORAGE PATH:
     *   "profiles/<userId>/avatar.<ext>"
     *
     * Recommended:
     * - Store this PATH in DB (photo_uri)
     * - Generate signed url for UI display
     */
    suspend fun uploadProfilePhotoAndReturnPath(
        context: Context,
        userId: String,
        authToken: String,
        photoUri: Uri,
        bucket: String = "profile-photos",
    ): String? = withContext(Dispatchers.IO) {
        try {
            val bytes = context.contentResolver
                .openInputStream(photoUri)
                ?.use { it.readBytes() }
                ?: return@withContext null

            val mime = context.contentResolver.getType(photoUri)
                ?: when {
                    photoUri.toString().endsWith(".png", true) -> "image/png"
                    photoUri.toString().endsWith(".webp", true) -> "image/webp"
                    photoUri.toString().endsWith(".heic", true) || photoUri.toString().endsWith(".heif", true) -> "image/heic"
                    else -> "image/jpeg"
                }

            val ext = when (mime.lowercase()) {
                "image/png" -> "png"
                "image/webp" -> "webp"
                "image/heic", "image/heif" -> "heic"
                else -> "jpg"
            }

            val objectPath = "profiles/$userId/avatar.$ext"

            val encodedPath = objectPath
                .split("/")
                .joinToString("/") { URLEncoder.encode(it, "UTF-8") }

            val uploadUrl = "$supabaseUrl/storage/v1/object/$bucket/$encodedPath"

            val req = Request.Builder()
                .url(uploadUrl)
                .addHeader("apikey", supabaseAnonKey)
                .addHeader("Authorization", "Bearer $authToken")
                .addHeader("Content-Type", mime)
                .addHeader("x-upsert", "true")
                .put(bytes.toRequestBody(mime.toMediaType()))
                .build()

            client.newCall(req).execute().use { resp ->
                val bodyText = runCatching { resp.body.string() }.getOrNull().orEmpty()
                if (!resp.isSuccessful) {
                    Log.e(TAG, "❌ Upload failed: ${resp.code} $bodyText")
                    return@withContext null
                }
            }

            Log.d(TAG, "✅ Uploaded OK: $objectPath ($mime, ${bytes.size} bytes)")
            objectPath
        } catch (e: Exception) {
            Log.e(TAG, "❌ uploadProfilePhotoAndReturnPath failed: ${e.message}", e)
            null
        }
    }

    // ----------------------------
    // Signed URL
    // ----------------------------
    /**
     * Creates a signed URL for a PRIVATE bucket object path like:
     *   "profiles/<id>/avatar.jpg"
     */
    suspend fun createSignedUrl(
        objectPath: String,
        authToken: String,
        bucket: String = "profile-photos",
        expiresInSeconds: Int = 60 * 60
    ): String? = withContext(Dispatchers.IO) {
        try {
            val encodedPath = objectPath
                .split("/")
                .joinToString("/") { URLEncoder.encode(it, "UTF-8") }

            val url = "$supabaseUrl/storage/v1/object/sign/$bucket/$encodedPath"

            val body = JSONObject()
                .put("expiresIn", expiresInSeconds)
                .toString()
                .toRequestBody("application/json".toMediaType())

            val req = Request.Builder()
                .url(url)
                .addHeader("apikey", supabaseAnonKey)
                .addHeader("Authorization", "Bearer $authToken")
                .addHeader("Content-Type", "application/json")
                .post(body)
                .build()

            client.newCall(req).execute().use { resp ->
                val raw = runCatching { resp.body.string() }.getOrNull().orEmpty()
                if (!resp.isSuccessful) {
                    Log.e(TAG, "❌ createSignedUrl failed: ${resp.code} $raw")
                    return@withContext null
                }

                val signedPath = JSONObject(raw).optString("signedURL") // often starts with "/storage/."
                if (signedPath.isBlank()) return@withContext null

                val full = when {
                    signedPath.startsWith("http", true) -> signedPath

                    // normal case: "/storage/v1/object/sign/..."
                    signedPath.startsWith("/storage/", true) -> "$supabaseUrl$signedPath"

                    // your case: "/object/sign/..." (missing /storage/v1)
                    signedPath.startsWith("/object/", true) -> "$supabaseUrl/storage/v1$signedPath"

                    else -> "$supabaseUrl/$signedPath"
                }
                Log.d(TAG, "✅ Signed URL OK: $full")
                full
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ createSignedUrl exception: ${e.message}", e)
            null
        }
    }

    // ----------------------------
    // DB update
    // ----------------------------
    /**
     * Updates public.user_profiles.photo_uri with the STORAGE PATH (not signed url).
     */
    suspend fun updateProfilePhotoUrl(
        userId: String,
        authToken: String,
        photoPath: String
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val url = "$supabaseUrl/rest/v1/user_profiles?id=eq.$userId"

            val bodyJson = JSONObject()
                .put("photo_uri", photoPath) // ✅ store PATH
                .toString()

            val req = Request.Builder()
                .url(url)
                .addHeader("apikey", supabaseAnonKey)
                .addHeader("Authorization", "Bearer $authToken")
                .addHeader("Content-Type", "application/json")
                .addHeader("Prefer", "return=minimal")
                .patch(bodyJson.toRequestBody("application/json".toMediaType()))
                .build()

            client.newCall(req).execute().use { resp ->
                val raw = runCatching { resp.body.string() }.getOrNull().orEmpty()
                Log.d(TAG, "Signed raw response: $raw")

                if (!resp.isSuccessful) {
                    return@withContext false
                }
            }

            Log.d(TAG, "✅ DB photo_uri updated to: $photoPath")
            true
        } catch (e: Exception) {
            Log.e(TAG, "❌ updateProfilePhotoUrl exception: ${e.message}", e)
            false
        }
    }
}

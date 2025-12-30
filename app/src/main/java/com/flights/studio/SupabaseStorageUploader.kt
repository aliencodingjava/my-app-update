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
import java.net.URLEncoder
import org.json.JSONObject

object SupabaseStorageUploader {
    private const val TAG = "SupabaseStorageUploader"

    private val supabaseUrl: String get() = BuildConfig.SUPABASE_URL.trimEnd('/')
    private val supabaseAnonKey: String get() = BuildConfig.SUPABASE_ANON_KEY
    private val client by lazy { OkHttpClient() }

    /**
     * Uploads the image bytes to Supabase Storage and returns a PUBLIC URL.
     * Bucket must be public for this URL to work.
     */
    suspend fun uploadProfilePhotoAndGetPublicUrl(
        context: Context,
        userId: String,
        authToken: String,
        photoUri: Uri,
        bucket: String = "profile-photos",
    ): String? = withContext(Dispatchers.IO) {
        try {
            val bytes = context.contentResolver.openInputStream(photoUri)?.use { it.readBytes() }
                ?: return@withContext null

            // Stable path: overwrites each time user changes photo
            val objectPath = "profiles/$userId/avatar.jpg"

            val encodedPath =
                objectPath.split("/").joinToString("/") { URLEncoder.encode(it, "UTF-8") }
            val uploadUrl = "$supabaseUrl/storage/v1/object/$bucket/$encodedPath"

            val req = Request.Builder()
                .url(uploadUrl)
                .addHeader("apikey", supabaseAnonKey)
                .addHeader("Authorization", "Bearer $authToken")
                .addHeader("Content-Type", "image/jpeg")
                .addHeader("x-upsert", "true")
                .put(bytes.toRequestBody("image/jpeg".toMediaType()))
                .build()

            client.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) {
                    val err = runCatching { resp.body.string() }.getOrNull()
                    Log.e(TAG, "❌ Upload failed: ${resp.code} $err")
                    return@withContext null
                }
            }

            // Public URL format (bucket public)
            "$supabaseUrl/storage/v1/object/public/$bucket/$objectPath"
        } catch (e: Exception) {
            Log.e(TAG, "❌ uploadProfilePhoto failed: ${e.message}", e)
            null
        }
    }

    /**
     * ✅ THIS is the missing part:
     * Updates profiles.photo_uri in the database, so next login loads the NEW photo.
     */
    suspend fun updateProfilePhotoUrl(
        userId: String,
        authToken: String,
        photoUrl: String
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val url = "$supabaseUrl/rest/v1/profiles?id=eq.$userId"

            val bodyJson = JSONObject()
                .put("photo_uri", photoUrl)
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
                if (!resp.isSuccessful) {
                    val err = runCatching { resp.body.string() }.getOrNull()
                    Log.e(TAG, "❌ DB update failed: ${resp.code} $err")
                    return@withContext false
                }
            }

            true
        } catch (e: Exception) {
            Log.e(TAG, "❌ updateProfilePhotoUrl failed: ${e.message}", e)
            false
        }
    }
}

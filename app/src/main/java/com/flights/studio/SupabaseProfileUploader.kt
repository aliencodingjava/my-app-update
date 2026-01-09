package com.flights.studio

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

object SupabaseProfileUploader {

    private const val TAG = "SupabaseProfileUploader"

    // Read from BuildConfig (generated from local.properties via build.gradle.kts)
    private val supabaseUrl: String
        get() = BuildConfig.SUPABASE_URL.trimEnd('/')

    private val supabaseAnonKey: String
        get() = BuildConfig.SUPABASE_ANON_KEY

    private val client by lazy { OkHttpClient() }
    private val userProfilesUrl: String
        get() = "$supabaseUrl/rest/v1/user_profiles"

    suspend fun uploadProfile(
        userId: String,
        authToken: String,
        name: String?,
        phone: String?,
        email: String?,
        bio: String?,
        birthday: String?,
        photoUri: String?,
        languageCode: String?,
        appVersion: String?
    ): Boolean = withContext(Dispatchers.IO) {
        if (supabaseUrl.isBlank() || supabaseAnonKey.isBlank()) {
            Log.e(TAG, "Missing SUPABASE_URL or SUPABASE_ANON_KEY in BuildConfig")
            return@withContext false
        }

        try {
            val json = JSONObject().apply {
                put("id", userId)

                if (!name.isNullOrBlank()) put("full_name", name)
                if (!phone.isNullOrBlank()) put("phone", phone)
                if (!email.isNullOrBlank()) put("email", email)

                if (!bio.isNullOrBlank()) put("bio", bio)
                if (!birthday.isNullOrBlank()) put("birthday", birthday)

                // ✅ store PATH returned by uploadProfilePhotoAndReturnPath (profiles/<id>/avatar.<ext>)
                if (!photoUri.isNullOrBlank()) {
                    put("photo_uri", photoUri)
                }



                if (!languageCode.isNullOrBlank()) put("language_code", languageCode)
                if (!appVersion.isNullOrBlank()) put("app_version", appVersion)

                put("last_login", java.time.Instant.now().toString())
            }



            val request = Request.Builder()
                .url(userProfilesUrl)
                .addHeader("apikey", supabaseAnonKey)              // anon key
                .addHeader("Authorization", "Bearer $authToken")    // user session JWT
                .addHeader("Content-Type", "application/json")
                .addHeader("Prefer", "resolution=merge-duplicates") // upsert behavior
                .post(json.toString().toRequestBody("application/json".toMediaType()))
                .build()

            client.newCall(request).execute().use { resp ->
                if (resp.isSuccessful) {
                    Log.d(TAG, "✅ Profile upserted for user: $userId")
                    true
                } else {
                    val errorBody = try { resp.body.string() } catch (_: Exception) { "No response body" }
                    Log.e(TAG, "❌ Upload failed: Code=${resp.code}, Body=$errorBody")
                    false
                }
            }

        } catch (e: Exception) {
            Log.e(TAG, "❌ Upload failed for user: $userId, Error: ${e.message}", e)
            false
        }
    }
}

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
    private const val SUPABASE_URL = "https://gdvhiudodnqdqhkyghsk.supabase.co"
    private const val SUPABASE_API_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6ImdkdmhpdWRvZG5xZHFoa3lnaHNrIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NDkwMTI2MTksImV4cCI6MjA2NDU4ODYxOX0.p9P-Hv4r-C9eZU3Dz-kX9U2iv8PUAKaUU5qZGPMJ844"
    private val client = OkHttpClient()

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
        try {
            val json = JSONObject().apply {
                put("id", userId)
                put("full_name", name)
                put("phone", phone)
                put("email", email)
                put("bio", bio)
                put("birthday", birthday)
                put("photo_uri", photoUri)
                put("language_code", languageCode)
                put("app_version", appVersion)
                put("last_login", java.time.Instant.now().toString())
            }

            val body = json.toString().toRequestBody("application/json".toMediaType())

            val request = Request.Builder()
                .url("$SUPABASE_URL/rest/v1/user_profiles")
                .addHeader("apikey", SUPABASE_API_KEY)
                .addHeader("Authorization", "Bearer $authToken")
                .addHeader("Content-Type", "application/json")
                .addHeader("Prefer", "resolution=merge-duplicates")
                .post(body)
                .build()

            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                Log.d(TAG, "✅ Profile upstarted successfully for user: $userId")
                return@withContext true
            } else {
                val errorBody = response.body?.string() ?: "No response body"
                Log.e(TAG, "❌ Upload failed: Code=${response.code}, Body=$errorBody\nRequest: $json")
                return@withContext false
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Upload failed for user: $userId, Error: ${e.message}", e)
            return@withContext false
        }
    }
}
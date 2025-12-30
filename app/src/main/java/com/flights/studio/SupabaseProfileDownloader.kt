package com.flights.studio

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject

object SupabaseProfileDownloader {
    private const val TAG = "SupabaseProfileDownloader"

    private val supabaseUrl: String get() = BuildConfig.SUPABASE_URL.trimEnd('/')
    private val supabaseAnonKey: String get() = BuildConfig.SUPABASE_ANON_KEY
    private val client by lazy { OkHttpClient() }

    data class ProfileRemote(
        val fullName: String?,
        val phone: String?,
        val email: String?,
        val bio: String?,
        val birthday: String?,
        val photoUri: String?
    )

    private fun JSONObject.optStringOrNull(key: String): String? {
        if (isNull(key)) return null
        val v = optString(key, "")
        return v.takeIf { it.isNotBlank() }
    }

    suspend fun fetchProfile(
        userId: String,
        authToken: String
    ): ProfileRemote? = withContext(Dispatchers.IO) {
        try {
            val url =
                "$supabaseUrl/rest/v1/user_profiles" +
                        "?id=eq.$userId" +
                        "&select=full_name,phone,email,bio,birthday,photo_uri"

            val req = Request.Builder()
                .url(url)
                .addHeader("apikey", supabaseAnonKey)
                .addHeader("Authorization", "Bearer $authToken")
                .addHeader("Accept", "application/json")
                .get()
                .build()

            client.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) {
                    val err = runCatching { resp.body.string() }.getOrNull()
                    Log.e(TAG, "❌ fetchProfile failed: ${resp.code} $err")
                    return@withContext null
                }

                val body = resp.body.string()
                val arr = JSONArray(body)
                if (arr.length() == 0) return@withContext null

                val o = arr.getJSONObject(0)

                ProfileRemote(
                    fullName = o.optStringOrNull("full_name"),
                    phone = o.optStringOrNull("phone"),
                    email = o.optStringOrNull("email"),
                    bio = o.optStringOrNull("bio"),
                    birthday = o.optStringOrNull("birthday"),
                    photoUri = o.optStringOrNull("photo_uri")
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ fetchProfile exception: ${e.message}", e)
            null
        }
    }
}

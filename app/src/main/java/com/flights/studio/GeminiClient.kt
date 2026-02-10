package com.flights.studio

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException

object GeminiClient {

    private const val FUNCTION_PATH = "/functions/v1/gemini"
    private val client = OkHttpClient()

    suspend fun generate(prompt: String): String = withContext(Dispatchers.IO) {
        val url = BuildConfig.SUPABASE_URL.trimEnd('/') + FUNCTION_PATH

        val body = JSONObject()
            .put("prompt", prompt)
            .toString()
            .toRequestBody("application/json".toMediaType())

        val request = Request.Builder()
            .url(url)
            .post(body)
            .addHeader("Content-Type", "application/json")
            .addHeader("apikey", BuildConfig.SUPABASE_ANON_KEY)
            .addHeader("Authorization", "Bearer ${BuildConfig.SUPABASE_ANON_KEY}")
            .build()

        client.newCall(request).execute().use { resp ->
            val raw = resp.body.string()

            if (!resp.isSuccessful) {
                Log.e("Gemini", "HTTP ${resp.code} raw=$raw")
                throw IOException("Gemini proxy error ${resp.code}: $raw")
            }

            val obj = JSONObject(raw)
            obj.getJSONArray("candidates")
                .getJSONObject(0)
                .getJSONObject("content")
                .getJSONArray("parts")
                .getJSONObject(0)
                .getString("text")
        }
    }
}

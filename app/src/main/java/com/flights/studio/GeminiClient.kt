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
import java.util.concurrent.TimeUnit

object GeminiClient {

    private const val FUNCTION_PATH = "/functions/v1/gemini"
    private val client = OkHttpClient()

    suspend fun generate(
        prompt: String,
        callTimeoutMillis: Long? = null
    ): String = withContext(Dispatchers.IO) {
        generateViaProxy(prompt, callTimeoutMillis)
    }

    private fun generateViaProxy(
        prompt: String,
        callTimeoutMillis: Long?
    ): String {
        val supabaseUrl = BuildConfig.SUPABASE_URL.trimEnd('/')
        val supabaseAnonKey = BuildConfig.SUPABASE_ANON_KEY.trim()

        if (supabaseUrl.isBlank() || supabaseAnonKey.isBlank()) {
            throw IOException("Supabase AI proxy is not configured")
        }

        val url = supabaseUrl + FUNCTION_PATH

        val body = JSONObject()
            .put("prompt", prompt)
            .toString()
            .toRequestBody("application/json".toMediaType())

        val request = Request.Builder()
            .url(url)
            .post(body)
            .addHeader("Content-Type", "application/json")
            .addHeader("apikey", supabaseAnonKey)
            .addHeader("Authorization", "Bearer $supabaseAnonKey")
            .build()

        clientFor(callTimeoutMillis).newCall(request).execute().use { resp ->
            val raw = resp.body.string()

            if (!resp.isSuccessful) {
                Log.e("Gemini", "Supabase AI proxy HTTP ${resp.code} raw=$raw")
                throw IOException("Gemini proxy error ${resp.code}: $raw")
            }

            return parseGeminiText(raw)
        }
    }

    private fun clientFor(callTimeoutMillis: Long?): OkHttpClient =
        callTimeoutMillis
            ?.let { client.newBuilder().callTimeout(it, TimeUnit.MILLISECONDS).build() }
            ?: client

    private fun parseGeminiText(raw: String): String {
        val obj = JSONObject(raw)
        obj.optString("text").takeIf { it.isNotBlank() }?.let { return it }
        obj.optString("output").takeIf { it.isNotBlank() }?.let { return it }
        obj.optString("result").takeIf { it.isNotBlank() }?.let { return it }

        return obj.getJSONArray("candidates")
            .getJSONObject(0)
            .getJSONObject("content")
            .getJSONArray("parts")
            .getJSONObject(0)
            .getString("text")
    }
}

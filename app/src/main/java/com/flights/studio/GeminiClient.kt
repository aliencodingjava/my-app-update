package com.flights.studio

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

object GeminiClient {

    private const val FUNCTION_PATH = "/functions/v1/gemini"
    private const val DIRECT_MODEL = "gemini-flash-lite-latest"
    private const val DIRECT_URL =
        "https://generativelanguage.googleapis.com/v1beta/models/$DIRECT_MODEL:generateContent"
    private val client = OkHttpClient()

    suspend fun generate(
        prompt: String,
        callTimeoutMillis: Long? = null
    ): String = withContext(Dispatchers.IO) {
        val directKey = BuildConfig.GEMINI_API_KEY.trim()
        var directFailure: Throwable? = null
        if (directKey.isNotBlank()) {
            val directResult = runCatching {
                generateDirect(prompt, directKey, callTimeoutMillis)
            }

            if (directResult.isSuccess) {
                return@withContext directResult.getOrThrow()
            }

            Log.w(
                "Gemini",
                "Direct Gemini failed; trying Supabase proxy: ${directResult.exceptionOrNull()?.message}"
            )
            directFailure = directResult.exceptionOrNull()
        }

        val proxyResult = runCatching {
            generateViaProxy(prompt, callTimeoutMillis)
        }

        if (proxyResult.isSuccess) {
            return@withContext proxyResult.getOrThrow()
        }

        throw directFailure ?: proxyResult.exceptionOrNull() ?: IOException("Gemini unavailable")
    }

    private fun generateViaProxy(
        prompt: String,
        callTimeoutMillis: Long?
    ): String {
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

        clientFor(callTimeoutMillis).newCall(request).execute().use { resp ->
            val raw = resp.body.string()

            if (!resp.isSuccessful) {
                Log.e("Gemini", "HTTP ${resp.code} raw=$raw")
                throw IOException("Gemini proxy error ${resp.code}: $raw")
            }

            return parseGeminiText(raw)
        }
    }

    private fun generateDirect(
        prompt: String,
        apiKey: String,
        callTimeoutMillis: Long?
    ): String {
        val body = JSONObject()
            .put(
                "contents",
                JSONArray().put(
                    JSONObject().put(
                        "parts",
                        JSONArray().put(JSONObject().put("text", prompt))
                    )
                )
            )
            .put(
                "generationConfig",
                JSONObject()
                    .put("temperature", 0.35)
                    .put("maxOutputTokens", 900)
            )
            .toString()
            .toRequestBody("application/json".toMediaType())

        val request = Request.Builder()
            .url(DIRECT_URL)
            .post(body)
            .addHeader("Content-Type", "application/json")
            .addHeader("x-goog-api-key", apiKey)
            .build()

        clientFor(callTimeoutMillis).newCall(request).execute().use { resp ->
            val raw = resp.body.string()

            if (!resp.isSuccessful) {
                Log.e("Gemini", "Direct HTTP ${resp.code} raw=$raw")
                throw IOException("Gemini direct error ${resp.code}: $raw")
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
        return obj.getJSONArray("candidates")
            .getJSONObject(0)
            .getJSONObject("content")
            .getJSONArray("parts")
            .getJSONObject(0)
            .getString("text")
    }
}

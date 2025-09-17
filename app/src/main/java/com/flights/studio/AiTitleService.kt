package com.flights.studio

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

object AiTitleService {
    private val json = "application/json; charset=utf-8".toMediaType()
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    /**
     * Calls OpenAI to turn a note into a short, one-line title.
     * Returns "" on any failure (so you can fall back to your local generator).
     */
    suspend fun suggestTitle(note: String): String = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.OPENAI_API_KEY
        if (apiKey.isBlank() || note.isBlank()) return@withContext ""

        // Use a small, chat-capable model you have access to (example shown).
        val body = JSONObject().apply {
            put("model", "gpt-4o-mini") // set to a model on your account
            put("temperature", 0.2)
            put("max_tokens", 40)
            put("messages", JSONArray().apply {
                put(JSONObject().put("role","system").put("content",
                    "You turn a single user note into a concise, human-friendly, " +
                            "one-line title (<= 8 words). No emojis. No quotes. No punctuation at the end."
                ))
                put(JSONObject().put("role","user").put("content", note))
            })
        }.toString().toRequestBody(json)

        val req = Request.Builder()
            .url("https://api.openai.com/v1/chat/completions")
            .addHeader("Authorization", "Bearer $apiKey")
            .addHeader("Content-Type", "application/json")
            .post(body)
            .build()

        try {
            client.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) return@withContext ""
                val txt = resp.body.string()
                val root = JSONObject(txt)
                val first = root.optJSONArray("choices")
                    ?.optJSONObject(0)
                    ?.optJSONObject("message")
                    ?.optString("content")
                    ?.trim()
                    .orEmpty()
                // Keep it ultra-compact; strip newlines just in case
                first.replace("\n", " ").take(60)
            }
        } catch (_: Throwable) {
            ""
        }
    }
}

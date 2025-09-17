package com.flights.studio

import android.content.Context
import android.util.Log
import org.json.JSONObject

class NotesCacheManager {
    companion object {
        var cachedNotes: MutableList<String> = mutableListOf()
        var cachedTitles: MutableMap<String, String> = mutableMapOf()

        var emojiLibrary: JSONObject? = null
        var keywordsLibrary: Map<String, List<String>> = emptyMap()

        fun preloadResources(context: Context) {
            if (emojiLibrary == null) {
                emojiLibrary = loadEmojiLibrary(context)
                Log.d("DEBUG_TAG", "✅ Emoji Library preloaded")
            }
            if (keywordsLibrary.isEmpty()) {
                keywordsLibrary = loadKeywords(context)
                Log.d("DEBUG_TAG", "✅ Keywords Library preloaded")
            }
        }

        private fun loadEmojiLibrary(context: Context): JSONObject {
            return try {
                val jsonString = context.resources.openRawResource(R.raw.emojis)
                    .bufferedReader().use { it.readText() }
                JSONObject(jsonString)
            } catch (e: Exception) {
                e.printStackTrace()
                JSONObject()
            }
        }

        private fun loadKeywords(context: Context): Map<String, List<String>> {
            return try {
                val jsonString = context.resources.openRawResource(R.raw.keywords)
                    .bufferedReader().use { it.readText() }
                val jsonObject = JSONObject(jsonString)
                val flatMap = mutableMapOf<String, MutableList<String>>()

                fun flatten(category: String, value: Any?) {
                    when (value) {
                        is JSONObject -> {
                            for (subKey in value.keys()) {
                                flatten(category, value.get(subKey))
                            }
                        }
                        is org.json.JSONArray -> {
                            for (i in 0 until value.length()) {
                                flatten(category, value.get(i))
                            }
                        }
                        is String -> {
                            flatMap.getOrPut(category) { mutableListOf() }.add(value)
                        }
                    }
                }

                for (key in jsonObject.keys()) {
                    flatten(key, jsonObject.get(key))
                }

                flatMap
            } catch (e: Exception) {
                e.printStackTrace()
                emptyMap()
            }
        }
    }
}

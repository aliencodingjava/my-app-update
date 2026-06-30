package com.flights.studio

import org.json.JSONObject
import java.net.URL

private const val GIST_URL =
    "https://gist.githubusercontent.com/aliencodingjava/358e7c8b5658f60cf098c1e7cb4a3cf4/raw/gistfile2.txt"

object AppUpdateRepository {

    fun fetchRemoteUpdate(): RemoteUpdateInfo {
        val jsonText = URL(GIST_URL).readText()
        val json = JSONObject(jsonText)

        val updatesJson = json.optJSONArray("updates")
        val updates = buildList {
            if (updatesJson != null) {
                for (i in 0 until updatesJson.length()) {
                    val o = updatesJson.getJSONObject(i)

                    val title = o.optString("title")
                    val body = o.optString("body")

                    add(parseUpdateBlock(title, body))
                }
            }
        }

        return RemoteUpdateInfo(
            versionCode = json.getInt("versionCode"),
            versionName = json.optString("versionName"),
            apkUrl = json.getString("apkUrl"),
            updates = updates
        )
    }

    private fun parseUpdateBlock(
        title: String,
        body: String
    ): UpdateBlock {
        val lines = body
            .lines()
            .map { it.trim() }
            .filter { it.isNotEmpty() }

        val bullets = lines
            .filter { it.startsWith("•") }
            .map { it.removePrefix("•").trim() }

        val summary = lines
            .filterNot { it.startsWith("•") }
            .joinToString("\n\n")
            .trim()

        return UpdateBlock(
            title = title,
            summary = summary,
            bullets = bullets
        )
    }
}

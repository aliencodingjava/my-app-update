package com.flights.studio

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import java.util.Calendar
import java.util.TimeZone

object BriefingWeatherRepository {
    private const val AIRPORT_HOME_URL = "https://www.jacksonholeairport.com/"

    suspend fun refresh(context: Context): BriefingWeatherSnapshot = withContext(Dispatchers.IO) {
        runCatching {
            val html = fetchAirportHomePage()
            val parsed = parseAirportWeather(html)
            val snapshot = if (parsed.temp.isNotBlank() || parsed.summary.isNotBlank()) {
                parsed
            } else {
                parsed.copy(condition = parsed.condition.ifBlank { conditionForJacksonTime() })
            }
            val existing = currentSnapshot(context)
            if (existing != null && existing.sameDisplayedWeather(snapshot)) {
                existing
            } else {
                snapshot.also {
                    SettingsStore.setBriefingWeatherSnapshot(context, GsonProvider.gson.toJson(snapshot))
                }
            }
        }.getOrElse {
            currentSnapshot(context)?.takeIf { snapshot -> snapshot.temp.isNotBlank() || snapshot.summary.isNotBlank() } ?: BriefingWeatherSnapshot(
                temp = "",
                condition = conditionForJacksonTime(),
                summary = "",
                source = "airport_web",
                updatedAt = System.currentTimeMillis()
            ).also { snapshot ->
                SettingsStore.setBriefingWeatherSnapshot(context, GsonProvider.gson.toJson(snapshot))
            }
        }
    }

    private fun currentSnapshot(context: Context): BriefingWeatherSnapshot? {
        return runCatching {
            GsonProvider.gson.fromJson(SettingsStore.briefingWeatherSnapshot(context), BriefingWeatherSnapshot::class.java)
        }.getOrNull()
    }

    private fun BriefingWeatherSnapshot.sameDisplayedWeather(other: BriefingWeatherSnapshot): Boolean {
        return temp == other.temp &&
            condition == other.condition &&
            summary == other.summary &&
            source == other.source
    }

    private fun fetchAirportHomePage(): String {
        val connection = (URL(AIRPORT_HOME_URL).openConnection() as HttpURLConnection).apply {
            connectTimeout = 7_000
            readTimeout = 7_000
            requestMethod = "GET"
            setRequestProperty("Accept", "text/html,application/xhtml+xml")
            setRequestProperty("User-Agent", "JHAirTracker/1.0")
        }
        return try {
            connection.inputStream.bufferedReader().use { it.readText() }
        } finally {
            connection.disconnect()
        }
    }

    private fun parseAirportWeather(html: String): BriefingWeatherSnapshot {
        val widget = Regex(
            """<div\s+class=["'][^"']*\bweather-widget\b[^"']*["'][^>]*id=["']weather-widget["'][^>]*>([\s\S]*?)<footer\b""",
            RegexOption.IGNORE_CASE
        ).find(html)?.groupValues?.getOrNull(1)
            ?: Regex(
                """<div\s+class=["'][^"']*\bweather-widget\b[^"']*["'][^>]*>([\s\S]*?)<ul\s+class=["']forecast["']""",
                RegexOption.IGNORE_CASE
            ).find(html)?.groupValues?.getOrNull(1)
            ?: ""

        val currentF = classText(widget, "cur-fahren").removeSuffix("/").trim()
        val currentC = classText(widget, "cur-celcius")
        val high = labeledValue(widget, "High")
        val low = labeledValue(widget, "Low")
        val cloud = detailValue(widget, "Cloud Coverage")
        val visibility = detailValue(widget, "Visibility")
        val wind = detailValue(widget, "Wind")
        val iconCondition = currentIconCondition(widget)
        val temp = listOf(currentF, currentC).filter { it.isNotBlank() }.joinToString(" / ")
        val summary = buildList {
            if (high.isNotBlank()) add("High $high")
            if (low.isNotBlank()) add("Low $low")
            if (iconCondition == "rain") add("Rain")
            if (cloud.isNotBlank()) add("Cloud $cloud")
            if (visibility.isNotBlank()) add("Vis $visibility")
            if (wind.isNotBlank()) add("Wind $wind")
        }.joinToString(" • ")
        val cloudPercent = Regex("""(\d+)\s*%""").find(cloud)?.groupValues?.getOrNull(1)?.toIntOrNull()
        val visibilityMiles = Regex("""(\d+(?:\.\d+)?)""").find(visibility)?.groupValues?.getOrNull(1)?.toDoubleOrNull()
        return BriefingWeatherSnapshot(
            temp = temp,
            condition = airportCondition(iconCondition, cloudPercent, visibilityMiles),
            summary = summary,
            source = "airport_web",
            updatedAt = System.currentTimeMillis()
        )
    }

    private fun airportCondition(iconCondition: String, cloudPercent: Int?, visibilityMiles: Double?): String {
        if (iconCondition.isNotBlank()) return iconCondition
        if (visibilityMiles != null && visibilityMiles < 3.0) return "fog"
        if (cloudPercent != null) {
            if (cloudPercent >= 70) return "cloudy"
            if (cloudPercent >= 30) return "partly"
        }
        return conditionForJacksonTime()
    }

    private fun currentIconCondition(widget: String): String {
        val current = Regex(
            """<div\s+class=["'][^"']*\bcurrent-conditions\b[^"']*["'][^>]*>([\s\S]*?)<ul\s+class=["']forecast["']""",
            RegexOption.IGNORE_CASE
        ).find(widget)?.groupValues?.getOrNull(1).orEmpty().ifBlank { widget }
        val icon = Regex(
            """<span\s+class=["'][^"']*\bicon-wrap\b[^"']*["'][^>]*>([\s\S]*?)</span>""",
            RegexOption.IGNORE_CASE
        ).find(current)?.groupValues?.getOrNull(1).orEmpty()
        val normalized = icon.replace(Regex("""\s+"""), " ")
        return when {
            // The airport rain glyph has descending streak paths and the current 22x21 rain viewBox.
            normalized.contains("viewBox=\"0 0 22 21\"", ignoreCase = true) ||
                normalized.contains("l1.63-5.77", ignoreCase = true) ||
                normalized.contains("2.44-8.77", ignoreCase = true) -> "rain"
            normalized.contains("viewBox=\"0 0 24 15\"", ignoreCase = true) -> "cloudy"
            normalized.contains("viewBox=\"0 0 22 22\"", ignoreCase = true) -> conditionForJacksonTime()
            else -> ""
        }
    }

    private fun conditionForJacksonTime(): String {
        val hour = Calendar.getInstance(TimeZone.getTimeZone("America/Denver")).get(Calendar.HOUR_OF_DAY)
        return if (hour in 6..19) "sunny" else "night"
    }

    private fun classText(html: String, className: String): String {
        return Regex(
            """<[^>]+class=["'][^"']*\b$className\b[^"']*["'][^>]*>([\s\S]*?)</[^>]+>""",
            RegexOption.IGNORE_CASE
        ).find(html)?.groupValues?.getOrNull(1).orEmpty().cleanHtml()
    }

    private fun labeledValue(html: String, label: String): String {
        return Regex("""\b$label\s+([^<]+)""", RegexOption.IGNORE_CASE)
            .find(html.cleanBreaks())
            ?.groupValues
            ?.getOrNull(1)
            .orEmpty()
            .cleanHtml()
    }

    private fun detailValue(html: String, label: String): String {
        return Regex("""\b$label\s*:\s*([^<]+)""", RegexOption.IGNORE_CASE)
            .find(html.cleanBreaks())
            ?.groupValues
            ?.getOrNull(1)
            .orEmpty()
            .cleanHtml()
    }

    private fun String.cleanBreaks(): String = replace("<br/>", "\n")
        .replace("<br />", "\n")
        .replace("<br>", "\n")

    private fun String.cleanHtml(): String =
        replace(Regex("""<script[\s\S]*?</script>""", RegexOption.IGNORE_CASE), "")
            .replace(Regex("""<style[\s\S]*?</style>""", RegexOption.IGNORE_CASE), "")
            .replace(Regex("""<[^>]+>"""), " ")
            .replace("&deg;", "°")
            .replace("&nbsp;", " ")
            .replace("&amp;", "&")
            .replace(Regex("""\s+"""), " ")
            .trim()
}

private object GsonProvider {
    val gson = com.google.gson.Gson()
}

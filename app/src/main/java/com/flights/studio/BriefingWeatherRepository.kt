package com.flights.studio

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.util.Calendar
import java.util.TimeZone
import kotlin.math.roundToInt

object BriefingWeatherRepository {
    private const val AIRPORT_HOME_URL = "https://www.jacksonholeairport.com/"

    /*
     * The airport's visible weather values are now populated dynamically in the
     * browser, so a plain HttpURLConnection request may receive the widget shell
     * without the actual temperature. Open-Meteo is used only when the airport
     * HTML does not contain a readable temperature.
     */
    private const val OPEN_METEO_URL =
        "https://api.open-meteo.com/v1/forecast" +
                "?latitude=43.6073" +
                "&longitude=-110.7377" +
                "&current=temperature_2m,weather_code,is_day,cloud_cover,visibility,wind_speed_10m,wind_direction_10m" +
                "&daily=temperature_2m_max,temperature_2m_min" +
                "&temperature_unit=fahrenheit" +
                "&wind_speed_unit=mph" +
                "&timezone=America%2FDenver" +
                "&forecast_days=1"

    suspend fun refresh(context: Context): BriefingWeatherSnapshot =
        withContext(Dispatchers.IO) {
            val existing = currentSnapshot(context)?.takeIf { it.hasRealTemperature() }

            val airportSnapshot = runCatching {
                parseAirportWeather(fetchText(AIRPORT_HOME_URL, "text/html,application/xhtml+xml"))
            }.getOrNull()

            val freshSnapshot = when {
                airportSnapshot?.hasRealTemperature() == true -> airportSnapshot

                else -> runCatching {
                    parseOpenMeteoWeather(
                        fetchText(OPEN_METEO_URL, "application/json")
                    )
                }.getOrNull()?.takeIf { it.hasRealTemperature() }
            }

            when {
                freshSnapshot != null &&
                        existing != null &&
                        existing.sameDisplayedWeather(freshSnapshot) -> existing

                freshSnapshot != null -> freshSnapshot.also {
                    saveSnapshot(context, it)
                }

                existing != null -> existing

                else -> BriefingWeatherSnapshot(
                    temp = "",
                    condition = conditionForJacksonTime(),
                    summary = "Weather temporarily unavailable",
                    source = "weather_unavailable",
                    updatedAt = System.currentTimeMillis()
                ).also {
                    saveSnapshot(context, it)
                }
            }
        }

    private fun saveSnapshot(
        context: Context,
        snapshot: BriefingWeatherSnapshot
    ) {
        SettingsStore.setBriefingWeatherSnapshot(
            context,
            GsonProvider.gson.toJson(snapshot)
        )
    }

    private fun currentSnapshot(context: Context): BriefingWeatherSnapshot? {
        val json = SettingsStore.briefingWeatherSnapshot(context)
        if (json.isBlank()) return null

        return runCatching {
            GsonProvider.gson.fromJson(
                json,
                BriefingWeatherSnapshot::class.java
            )
        }.getOrNull()
    }

    private fun BriefingWeatherSnapshot.hasRealTemperature(): Boolean {
        return Regex("""-?\d+""").containsMatchIn(temp)
    }

    private fun BriefingWeatherSnapshot.sameDisplayedWeather(
        other: BriefingWeatherSnapshot
    ): Boolean {
        return temp == other.temp &&
                condition == other.condition &&
                summary == other.summary &&
                source == other.source
    }

    private fun fetchText(
        address: String,
        accept: String
    ): String {
        val connection = (URL(address).openConnection() as HttpURLConnection).apply {
            connectTimeout = 10_000
            readTimeout = 10_000
            requestMethod = "GET"
            instanceFollowRedirects = true
            useCaches = false
            setRequestProperty("Accept", accept)
            setRequestProperty("Accept-Language", "en-US,en;q=0.9")
            setRequestProperty("Cache-Control", "no-cache")
            setRequestProperty(
                "User-Agent",
                "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 " +
                        "Chrome/126.0 Mobile Safari/537.36 JHAirTracker/1.0"
            )
        }

        return try {
            val responseCode = connection.responseCode
            if (responseCode !in 200..299) {
                throw IOException("Weather request failed: HTTP $responseCode")
            }

            connection.inputStream
                .bufferedReader(Charsets.UTF_8)
                .use { it.readText() }
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
                """<div\s+class=["'][^"']*\bweather-widget\b[^"']*["'][^>]*>([\s\S]*?)<ul\s+class=["'][^"']*\bforecast\b[^"']*["']""",
                RegexOption.IGNORE_CASE
            ).find(html)?.groupValues?.getOrNull(1)
            ?: ""

        // Fall back to the whole page when the outer widget markup changes.
        val weatherSource = widget.ifBlank { html }

        val currentF = firstNonBlank(
            classText(weatherSource, "cur-fahren"),
            classText(weatherSource, "fahrenheit"),
            classText(weatherSource, "temp-f")
        ).removeSuffix("/").trim()

        val currentC = firstNonBlank(
            classText(weatherSource, "cur-celcius"),
            classText(weatherSource, "cur-celsius"),
            classText(weatherSource, "celsius"),
            classText(weatherSource, "temp-c")
        )

        val textPair = findTemperaturePair(weatherSource.cleanHtml())

        val fahrenheit = firstNonBlank(
            normalizeTemperature(currentF, "F"),
            textPair?.first.orEmpty()
        )

        val celsius = firstNonBlank(
            normalizeTemperature(currentC, "C"),
            textPair?.second.orEmpty()
        )

        val high = labeledValue(weatherSource, "High")
        val low = labeledValue(weatherSource, "Low")
        val cloud = detailValue(weatherSource, "Cloud Coverage")
        val visibility = detailValue(weatherSource, "Visibility")
        val wind = detailValue(weatherSource, "Wind")
        val iconCondition = currentIconCondition(weatherSource)

        val temp = listOf(fahrenheit, celsius)
            .filter { it.isNotBlank() }
            .distinct()
            .joinToString(" / ")

        val summary = buildList {
            if (high.isNotBlank()) add("High $high")
            if (low.isNotBlank()) add("Low $low")
            if (iconCondition == "rain") add("Rain")
            if (cloud.isNotBlank()) add("Cloud $cloud")
            if (visibility.isNotBlank()) add("Vis $visibility")
            if (wind.isNotBlank()) add("Wind $wind")
        }.joinToString(" • ")

        val cloudPercent = Regex("""(\d+)\s*%""")
            .find(cloud)
            ?.groupValues
            ?.getOrNull(1)
            ?.toIntOrNull()

        val visibilityMiles = Regex("""(\d+(?:\.\d+)?)""")
            .find(visibility)
            ?.groupValues
            ?.getOrNull(1)
            ?.toDoubleOrNull()

        return BriefingWeatherSnapshot(
            temp = temp,
            condition = airportCondition(
                iconCondition,
                cloudPercent,
                visibilityMiles
            ),
            summary = summary,
            source = "airport_web",
            updatedAt = System.currentTimeMillis()
        )
    }

    private fun parseOpenMeteoWeather(json: String): BriefingWeatherSnapshot {
        val root = JSONObject(json)
        val current = root.getJSONObject("current")
        val daily = root.optJSONObject("daily")

        val tempF = current.optDouble("temperature_2m", Double.NaN)
        if (!tempF.isFinite()) {
            throw IOException("Open-Meteo temperature is missing")
        }

        val tempC = (tempF - 32.0) * 5.0 / 9.0
        val weatherCode = current.optInt("weather_code", -1)
        val isDay = current.optInt("is_day", -1)
        val cloud = current.optDouble("cloud_cover", Double.NaN)
        val visibilityMeters = current.optDouble("visibility", Double.NaN)
        val windMph = current.optDouble("wind_speed_10m", Double.NaN)
        val windDirection = current.optDouble("wind_direction_10m", Double.NaN)

        val highF = daily
            ?.optJSONArray("temperature_2m_max")
            ?.optDouble(0, Double.NaN)
            ?: Double.NaN

        val lowF = daily
            ?.optJSONArray("temperature_2m_min")
            ?.optDouble(0, Double.NaN)
            ?: Double.NaN

        val visibilityMiles = if (visibilityMeters.isFinite()) {
            visibilityMeters / 1609.344
        } else {
            Double.NaN
        }

        val summary = buildList {
            if (highF.isFinite()) {
                add("High ${highF.roundToInt()}°F")
            }

            if (lowF.isFinite()) {
                add("Low ${lowF.roundToInt()}°F")
            }

            if (cloud.isFinite()) {
                add("Cloud ${cloud.roundToInt()}%")
            }

            if (visibilityMiles.isFinite()) {
                add("Vis ${formatOneDecimal(visibilityMiles)} mi")
            }

            if (windMph.isFinite()) {
                val direction = if (windDirection.isFinite()) {
                    " ${cardinalDirection(windDirection)}"
                } else {
                    ""
                }
                add("Wind ${windMph.roundToInt()} mph$direction")
            }
        }.joinToString(" • ")

        return BriefingWeatherSnapshot(
            temp = "${tempF.roundToInt()}°F / ${tempC.roundToInt()}°C",
            condition = conditionFromWeatherCode(
                code = weatherCode,
                isDay = isDay
            ),
            summary = summary,
            source = "open_meteo",
            updatedAt = System.currentTimeMillis()
        )
    }

    private fun conditionFromWeatherCode(
        code: Int,
        isDay: Int
    ): String {
        return when (code) {
            0 -> if (isDay == 0) "night" else "sunny"
            1, 2 -> if (isDay == 0) "partly_night" else "partly"
            3 -> "cloudy"
            45, 48 -> "fog"
            51, 53, 55, 56, 57,
            61, 63, 65, 66, 67,
            80, 81, 82 -> "rain"
            71, 73, 75, 77, 85, 86 -> "snow"
            95, 96, 99 -> "thunder"
            else -> if (isDay == 0) "night" else conditionForJacksonTime()
        }
    }

    private fun airportCondition(
        iconCondition: String,
        cloudPercent: Int?,
        visibilityMiles: Double?
    ): String {
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
            """<div\s+class=["'][^"']*\bcurrent-conditions\b[^"']*["'][^>]*>([\s\S]*?)<ul\s+class=["'][^"']*\bforecast\b[^"']*["']""",
            RegexOption.IGNORE_CASE
        ).find(widget)
            ?.groupValues
            ?.getOrNull(1)
            .orEmpty()
            .ifBlank { widget }

        val icon = Regex(
            """<span\s+class=["'][^"']*\bicon-wrap\b[^"']*["'][^>]*>([\s\S]*?)</span>""",
            RegexOption.IGNORE_CASE
        ).find(current)
            ?.groupValues
            ?.getOrNull(1)
            .orEmpty()

        val normalized = icon.replace(Regex("""\s+"""), " ")

        return when {
            normalized.contains("thunder", ignoreCase = true) ||
                    normalized.contains("lightning", ignoreCase = true) -> "thunder"

            normalized.contains("snow", ignoreCase = true) -> "snow"

            normalized.contains("viewBox=\"0 0 22 21\"", ignoreCase = true) ||
                    normalized.contains("l1.63-5.77", ignoreCase = true) ||
                    normalized.contains("2.44-8.77", ignoreCase = true) -> "rain"

            normalized.contains("viewBox=\"0 0 24 15\"", ignoreCase = true) -> "cloudy"

            normalized.contains("viewBox=\"0 0 22 22\"", ignoreCase = true) ->
                conditionForJacksonTime()

            else -> ""
        }
    }

    private fun conditionForJacksonTime(): String {
        val hour = Calendar.getInstance(
            TimeZone.getTimeZone("America/Denver")
        ).get(Calendar.HOUR_OF_DAY)

        return if (hour in 6..19) "sunny" else "night"
    }

    private fun classText(
        html: String,
        className: String
    ): String {
        val safeClassName = Regex.escape(className)

        return Regex(
            """<([a-zA-Z0-9]+)[^>]*class=["'][^"']*\b$safeClassName\b[^"']*["'][^>]*>([\s\S]*?)</\1>""",
            RegexOption.IGNORE_CASE
        ).find(html)
            ?.groupValues
            ?.getOrNull(2)
            .orEmpty()
            .cleanHtml()
    }

    private fun findTemperaturePair(
        text: String
    ): Pair<String, String>? {
        val match = Regex(
            """(-?\d{1,3}(?:\.\d+)?)\s*°?\s*F\s*[/|⁄]\s*(-?\d{1,3}(?:\.\d+)?)\s*°?\s*C""",
            RegexOption.IGNORE_CASE
        ).find(text) ?: return null

        val fahrenheit = match.groupValues.getOrNull(1).orEmpty()
        val celsius = match.groupValues.getOrNull(2).orEmpty()

        if (fahrenheit.isBlank() || celsius.isBlank()) return null
        return "$fahrenheit°F" to "$celsius°C"
    }

    private fun normalizeTemperature(
        value: String,
        unit: String
    ): String {
        if (value.isBlank()) return ""

        val number = Regex("""-?\d{1,3}(?:\.\d+)?""")
            .find(value)
            ?.value
            .orEmpty()

        if (number.isBlank()) return ""
        return "$number°${unit.uppercase()}"
    }

    private fun labeledValue(
        html: String,
        label: String
    ): String {
        return Regex(
            """\b${Regex.escape(label)}\s+([^<]+)""",
            RegexOption.IGNORE_CASE
        ).find(html.cleanBreaks())
            ?.groupValues
            ?.getOrNull(1)
            .orEmpty()
            .cleanHtml()
    }

    private fun detailValue(
        html: String,
        label: String
    ): String {
        return Regex(
            """\b${Regex.escape(label)}\s*:\s*([^<]+)""",
            RegexOption.IGNORE_CASE
        ).find(html.cleanBreaks())
            ?.groupValues
            ?.getOrNull(1)
            .orEmpty()
            .cleanHtml()
    }

    private fun cardinalDirection(degrees: Double): String {
        val directions = arrayOf(
            "N", "NE", "E", "SE",
            "S", "SW", "W", "NW"
        )

        val normalized = ((degrees % 360.0) + 360.0) % 360.0
        val index = ((normalized + 22.5) / 45.0).toInt() % directions.size
        return directions[index]
    }

    private fun formatOneDecimal(value: Double): String {
        return String.format(
            java.util.Locale.US,
            "%.1f",
            value
        )
    }

    private fun firstNonBlank(vararg values: String?): String {
        return values
            .firstOrNull { !it.isNullOrBlank() }
            .orEmpty()
            .trim()
    }

    private fun String.cleanBreaks(): String =
        replace(
            Regex("""<br\s*/?>""", RegexOption.IGNORE_CASE),
            "\n"
        )

    private fun String.cleanHtml(): String =
        replace(
            Regex(
                """<script[\s\S]*?</script>""",
                RegexOption.IGNORE_CASE
            ),
            ""
        )
            .replace(
                Regex(
                    """<style[\s\S]*?</style>""",
                    RegexOption.IGNORE_CASE
                ),
                ""
            )
            .replace(Regex("""<[^>]+>"""), " ")
            .replace("&deg;", "°", ignoreCase = true)
            .replace("&#176;", "°", ignoreCase = true)
            .replace("&#xB0;", "°", ignoreCase = true)
            .replace("&nbsp;", " ", ignoreCase = true)
            .replace("&#160;", " ", ignoreCase = true)
            .replace("&amp;", "&", ignoreCase = true)
            .replace(Regex("""\s+"""), " ")
            .trim()
}

private object GsonProvider {
    val gson = com.google.gson.Gson()
}
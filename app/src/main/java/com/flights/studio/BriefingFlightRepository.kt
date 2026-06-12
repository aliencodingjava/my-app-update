package com.flights.studio

import android.content.Context
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import kotlin.math.max

object BriefingFlightRepository {
    private const val FLIGHT_TABLE_URL = "https://www.jacksonholeairport.com/flight.php"
    private const val JAC_FLIGHTS_URL = "https://www.jacksonholeairport.com/flights/"

    suspend fun refresh(context: Context): BriefingFlightSnapshot? = withContext(Dispatchers.IO) {
        runCatching {
            val legacyRows = runCatching { parseRows(fetchLegacyFlightTable()) }.getOrDefault(emptyList())
            val rows = if (legacyRows.isNotEmpty()) legacyRows else parseRowsFromFlightsPage(fetchFlightsPage())
            val snapshot = if (rows.isNotEmpty()) {
                buildSnapshot(rows)
            } else {
                val existing = runCatching {
                    Gson().fromJson(SettingsStore.flightBriefSnapshot(context), BriefingFlightSnapshot::class.java)
                }.getOrNull()
                if (existing != null && existing.isReadableTableSnapshot()) {
                    return@withContext existing
                }
                unavailableSnapshot()
            }
            snapshot.also {
                SettingsStore.setFlightBriefSnapshot(context, Gson().toJson(snapshot))
            }
        }.getOrNull()
    }

    private fun fetchLegacyFlightTable(): String {
        val connection = (URL(FLIGHT_TABLE_URL).openConnection() as HttpURLConnection).apply {
            connectTimeout = 7_000
            readTimeout = 7_000
            requestMethod = "POST"
            setRequestProperty("Accept", "text/html")
            setRequestProperty("User-Agent", "JHAirTracker/1.0")
            doOutput = true
        }
        return try {
            connection.outputStream.use { it.write(ByteArray(0)) }
            connection.inputStream.bufferedReader().use { it.readText() }
        } finally {
            connection.disconnect()
        }
    }

    private fun fetchFlightsPage(): String {
        val connection = (URL(JAC_FLIGHTS_URL).openConnection() as HttpURLConnection).apply {
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

    private fun parseRows(html: String): List<FlightBriefRow> {
        return parseTable(html, "arrival") + parseTable(html, "departure")
    }

    private fun parseRowsFromFlightsPage(html: String): List<FlightBriefRow> {
        val container = Regex(
            """<div\s+id=["']flight-container["'][^>]*>([\s\S]*?)</div>""",
            RegexOption.IGNORE_CASE
        ).find(html)?.groupValues?.getOrNull(1).orEmpty()
        val rows = parseGenericFlightRows(container)
        if (rows.isNotEmpty()) return rows

        val hasOfficialFlightView = html.contains("tracker.flightview.com", ignoreCase = true) ||
            html.contains("FlightView", ignoreCase = true)
        val hasFlightAware = html.contains("flightaware.com/live/airport/KJAC", ignoreCase = true)
        return if (hasOfficialFlightView || hasFlightAware) emptyList() else parseGenericFlightRows(html)
    }

    private fun parseGenericFlightRows(html: String): List<FlightBriefRow> {
        if (html.isBlank()) return emptyList()
        val rows = mutableListOf<FlightBriefRow>()
        Regex("""<tr[^>]*>([\s\S]*?)</tr>""", RegexOption.IGNORE_CASE)
            .findAll(html)
            .forEach { match ->
                val cells = Regex("""<t[dh][^>]*>([\s\S]*?)</t[dh]>""", RegexOption.IGNORE_CASE)
                    .findAll(match.groupValues[1])
                    .map { it.groupValues[1].stripHtml().decodeEntities().squashSpaces() }
                    .filter { it.isNotBlank() }
                    .toList()
                if (cells.size < 4) return@forEach
                val joined = cells.joinToString(" ")
                if (!joined.contains(Regex("""\b[A-Z]{2}\s*\d{1,4}\b|\b\d{1,4}\b"""))) return@forEach
                val status = cells.lastOrNull { statusTone(it) != "ontime" } ?: cells.last()
                val kind = if (joined.contains("depart", ignoreCase = true)) "departure" else "arrival"
                val flight = cells.firstOrNull { it.contains(Regex("""\b[A-Z]{2}\s*\d{1,4}\b|\b\d{1,4}\b""")) }.orEmpty()
                val sched = cells.firstOrNull { clockToMinutes(it) != null }.orEmpty()
                rows += FlightBriefRow(
                    kind = kind,
                    day = "Today",
                    airline = "",
                    flight = flight,
                    route = cells.drop(1).take(2).joinToString(" to ").ifBlank { "JAC" },
                    sched = sched,
                    actual = "",
                    status = status,
                    tone = statusTone(status),
                    delay = 0
                )
            }
        return rows
    }

    private fun parseTable(html: String, kind: String): List<FlightBriefRow> {
        val tableBody = Regex(
            """<div\s+class=["']flight-table-wrap\s+-$kind["'][\s\S]*?<tbody>([\s\S]*?)</tbody>""",
            RegexOption.IGNORE_CASE
        ).find(html)?.groupValues?.getOrNull(1).orEmpty()
        if (tableBody.isBlank()) return emptyList()

        val rows = mutableListOf<FlightBriefRow>()
        var day = ""
        Regex("""<tr[^>]*>([\s\S]*?)</tr>""", RegexOption.IGNORE_CASE)
            .findAll(tableBody)
            .forEach { match ->
                val rowHtml = match.groupValues[1]
                val dayLabel = cellText(rowHtml, "day")
                if (dayLabel.isNotBlank()) {
                    day = cleanDay(dayLabel)
                    return@forEach
                }

                val airline = cellText(rowHtml, "airline")
                val flight = cellText(rowHtml, "flight")
                if (airline.isBlank() || flight.isBlank()) return@forEach

                val place = cellText(rowHtml, if (kind == "departure") "to" else "from")
                val sched = cellText(rowHtml, "sched")
                val actual = cellText(rowHtml, "actual")
                val status = cellText(rowHtml, "status").ifBlank { "Scheduled" }
                val tone = statusTone(status)
                val delay = if (tone == "cancelled" || tone == "diverted") 0 else delayMins(sched, actual)
                rows += FlightBriefRow(
                    kind = kind,
                    day = day,
                    airline = airline,
                    flight = flight,
                    route = if (kind == "departure") {
                        "JAC to ${place.ifBlank { "destination" }}"
                    } else {
                        "${place.ifBlank { "origin" }} to JAC"
                    },
                    sched = sched,
                    actual = actual,
                    status = status,
                    tone = tone,
                    delay = delay
                )
            }
        return rows
    }

    private fun buildSnapshot(rows: List<FlightBriefRow>): BriefingFlightSnapshot {
        val briefingRows = rows.todayOnly()
        val issues = briefingRows
            .filter { it.tone == "cancelled" || it.tone == "diverted" || it.tone == "delayed" || it.delay > 0 }
            .sortedWith(
                compareByDescending<FlightBriefRow> { issueRank(it) }
                    .thenByDescending { it.delay }
            )
        return BriefingFlightSnapshot(
            summary = summaryLine(briefingRows),
            issueCount = issues.size,
            issues = issues.take(4).map { row ->
                BriefingFlightIssueCard(
                    label = when {
                        row.tone == "cancelled" -> "Cancelled"
                        row.tone == "diverted" -> "Diverted"
                        row.delay > 0 -> "+${row.delay} min"
                        else -> row.status.ifBlank { "Delayed" }
                    },
                    flight = "${row.airline} ${row.flight}".trim(),
                    route = row.route,
                    time = if (row.actual.isNotBlank() && row.actual != row.sched) {
                        "${row.sched} -> ${row.actual}"
                    } else {
                        row.sched.ifBlank { "time pending" }
                    },
                    tone = row.tone
                )
            },
            arrivalCount = briefingRows.count { it.kind != "departure" },
            departureCount = briefingRows.count { it.kind == "departure" },
            delayedCount = briefingRows.count { it.tone == "delayed" || it.delay > 0 },
            cancelledCount = briefingRows.count { it.tone == "cancelled" },
            divertedCount = briefingRows.count { it.tone == "diverted" },
            source = "native_table",
            updatedAt = System.currentTimeMillis()
        )
    }

    private fun List<FlightBriefRow>.todayOnly(): List<FlightBriefRow> {
        val today = firstOrNull()?.day?.ifBlank { "Today" } ?: "Today"
        return filter { it.day.ifBlank { "Today" } == today }.ifEmpty { this }
    }

    private fun summaryLine(rows: List<FlightBriefRow>): String {
        if (rows.isEmpty()) return "Flight table is still loading."
        val delayedRows = rows.filter { it.tone == "delayed" || it.delay > 0 }
        val cancelled = rows.count { it.tone == "cancelled" }
        val diverted = rows.count { it.tone == "diverted" }
        val dayText = rows
            .fold(mutableListOf<DayFlightCount>()) { days, row ->
                val key = row.day.ifBlank { "Today" }
                val day = days.firstOrNull { it.key == key } ?: DayFlightCount(key).also(days::add)
                if (row.kind == "departure") day.departures += 1 else day.arrivals += 1
                days
            }
            .take(2)
            .joinToString(". ") { day ->
                "${day.key}: ${day.arrivals} arrival${if (day.arrivals == 1) "" else "s"}, " +
                    "${day.departures} departure${if (day.departures == 1) "" else "s"}"
            }
        val longestDelay = delayedRows.maxOfOrNull { it.delay } ?: 0
        val delayTail = if (longestDelay > 0) " Longest visible delay is $longestDelay min." else ""
        val issueText = if (delayedRows.isEmpty() && cancelled == 0 && diverted == 0) {
            "No delays, cancellations, or diversions visible right now."
        } else {
            "${delayedRows.size} delayed, $cancelled cancelled, $diverted diverted.$delayTail"
        }
        return "$dayText. $issueText"
    }

    private fun unavailableSnapshot(): BriefingFlightSnapshot {
        return BriefingFlightSnapshot(
            summary = "Live flight rows are available inside Flights. Open Flights for cancellations, diversions, and delays.",
            issueCount = 0,
            issues = emptyList(),
            source = "native_unavailable",
            updatedAt = System.currentTimeMillis()
        )
    }

    private fun BriefingFlightSnapshot.isReadableTableSnapshot(): Boolean {
        if (summary.isBlank()) return false
        if (source == "native_unavailable") return false
        if (summary.contains("unavailable", ignoreCase = true) || summary.contains("not readable", ignoreCase = true)) return false
        return arrivalCount > 0 || departureCount > 0 ||
            summary.contains("arrival", ignoreCase = true) ||
            summary.contains("departure", ignoreCase = true) ||
            issues.isNotEmpty()
    }

    private fun cellText(rowHtml: String, className: String): String {
        val cell = Regex(
            """<td[^>]*class=["'][^"']*\b$className\b[^"']*["'][^>]*>([\s\S]*?)</td>""",
            RegexOption.IGNORE_CASE
        ).find(rowHtml)?.groupValues?.getOrNull(1).orEmpty()
        return cell.stripHtml().decodeEntities().squashSpaces()
    }

    private fun statusTone(status: String): String {
        val value = status.lowercase()
        return when {
            "cancel" in value -> "cancelled"
            "divert" in value -> "diverted"
            "delay" in value -> "delayed"
            "arriv" in value -> "arrived"
            else -> "ontime"
        }
    }

    private fun delayMins(sched: String, actual: String): Int {
        val scheduled = clockToMinutes(sched) ?: return 0
        val actualMinutes = clockToMinutes(actual) ?: return 0
        var diff = actualMinutes - scheduled
        if (diff < 0) {
            diff = if (diff < -720) diff + 1440 else return 0
        }
        return max(0, diff)
    }

    private fun clockToMinutes(value: String): Int? {
        val match = Regex("""(\d{1,2}):(\d{2})\s*([ap]m)?""", RegexOption.IGNORE_CASE)
            .find(value.trim())
            ?: return null
        var hour = match.groupValues[1].toIntOrNull() ?: return null
        val minute = match.groupValues[2].toIntOrNull() ?: return null
        val meridiem = match.groupValues.getOrNull(3).orEmpty().lowercase()
        if (meridiem == "pm" && hour < 12) hour += 12
        if (meridiem == "am" && hour == 12) hour = 0
        return hour * 60 + minute
    }

    private fun issueRank(row: FlightBriefRow): Int {
        return when (row.tone) {
            "cancelled" -> 3
            "diverted" -> 2
            else -> 1
        }
    }

    private fun cleanDay(value: String): String {
        return value
            .replace(Regex("""\s+\d+\s+flights?\s+total.*$""", RegexOption.IGNORE_CASE), "")
            .replace(Regex("""\s+last\s+updated.*$""", RegexOption.IGNORE_CASE), "")
            .replace(Regex("""\s+last\s+update\s*:.*$""", RegexOption.IGNORE_CASE), "")
            .squashSpaces()
    }

    private fun String.stripHtml(): String =
        replace(Regex("""<script[\s\S]*?</script>""", RegexOption.IGNORE_CASE), "")
            .replace(Regex("""<style[\s\S]*?</style>""", RegexOption.IGNORE_CASE), "")
            .replace(Regex("""<[^>]+>"""), " ")

    private fun String.decodeEntities(): String =
        replace("&nbsp;", " ")
            .replace("&amp;", "&")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&quot;", "\"")
            .replace("&#39;", "'")

    private fun String.squashSpaces(): String = replace(Regex("""\s+"""), " ").trim()

    private data class FlightBriefRow(
        val kind: String,
        val day: String,
        val airline: String,
        val flight: String,
        val route: String,
        val sched: String,
        val actual: String,
        val status: String,
        val tone: String,
        val delay: Int
    )

    private data class DayFlightCount(
        val key: String,
        var arrivals: Int = 0,
        var departures: Int = 0
    )
}

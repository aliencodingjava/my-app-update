package com.flights.studio

import android.util.Log
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.time.Duration.Companion.milliseconds

object BriefingAiService {
    suspend fun generateBrief(
        noteCount: Int,
        recentNotes: List<BriefingNoteContext>,
        appContext: BriefingAppContext
    ): String {
        return try {
            withTimeoutOrNull(4_500L.milliseconds) {
                GeminiClient.generate(
                    prompt = buildPrompt(noteCount, recentNotes, appContext),
                    callTimeoutMillis = 4_500L
                )
            }
                ?.trim()
                ?.replace("\n", " ")
                ?.squashSpaces()
                ?.removeSurrounding("\"")
                ?.withoutGreeting()
                ?.take(140)
                .orEmpty()
        } catch (t: Throwable) {
            Log.e("BriefingAiService", "Gemini briefing failed: ${t.message}", t)
            ""
        }
    }

    private fun buildPrompt(
        noteCount: Int,
        recentNotes: List<BriefingNoteContext>,
        appContext: BriefingAppContext
    ): String = buildString {
        appendLine("You write a short in-app airport briefing for JH AirTracker's whole app hub.")
        appendLine("Use only the provided app state.")
        appendLine("Do not invent live flight status, weather, delays, camera conditions, or user location.")
        appendLine("Flight data source is https://www.jacksonholeairport.com/flights/ and its official FlightView/FlightAware links.")
        appendLine("Weather data source is https://www.jacksonholeairport.com/#weather-widget.")
        appendLine("If flight or weather values are unavailable, say they are unavailable or recommend opening Flights; never fill in missing numbers.")
        appendLine("Do not only summarize notes unless notes/reminders are clearly the most useful thing.")
        appendLine("Choose the most useful next action from notes, reminders, flights, live cameras, FBO, contacts, settings, cache, or privacy state.")
        appendLine("Do not include a greeting; the UI shows the greeting separately.")
        appendLine("Do not repeat the whole action list.")
        appendLine("Output exactly one concise sentence, max 26 words.")
        appendLine("No markdown. No bullets. No emoji. No quotation marks.")
        appendLine()
        appendLine("APP STATE:")
        appendLine("greeting=${appContext.greeting}")
        appendLine("day_part=${appContext.dayPart}")
        appendLine("saved_notes=$noteCount")
        appendLine("notes_with_reminders=${appContext.reminderCount}")
        appendLine("active_reminder_badges=${appContext.badgeCount}")
        appendLine("notes_with_images=${appContext.imageNoteCount}")
        appendLine("contacts_visible=${appContext.contactsCount}")
        appendLine("contacts_sort=${appContext.contactsSort}")
        appendLine("web_table_theme=${appContext.webTheme}")
        appendLine("web_text_zoom=${appContext.webTextZoom}")
        appendLine("web_table_grouped=${appContext.groupFlights}")
        appendLine("web_high_contrast=${appContext.highContrastWeb}")
        appendLine("web_cache_pages=${appContext.cachePages}")
        appendLine("web_block_trackers=${appContext.blockTrackers}")
        appendLine("web_reduce_motion=${appContext.reduceWebMotion}")
        appendLine("flight_table_summary=${appContext.flightSummary.ifBlank { "(none)" }}")
        appendLine("flight_issue_count=${appContext.flightIssueCount}")
        appendLine("flight_issue_cards=${appContext.flightIssueCards.joinToString("; ") { it.label }}")
        appendLine("airport_weather=${appContext.weatherSummary.ifBlank { "(none)" }}")
        appendLine("available_actions=Flights, Live cameras, FBO services, News, Notes, Quick note, Welcome, About airport, Airport help")
        appendLine()
        appendLine("RECENT NOTES, newest first:")
        if (recentNotes.isEmpty()) {
            appendLine("(none)")
        } else {
            recentNotes.take(3).forEachIndexed { index, note ->
                appendLine("${index + 1}. title=${note.title.ifBlank { "(blank)" }.take(80)}")
                appendLine("   text=${note.text.squashSpaces().take(220)}")
            }
        }
    }

    private fun String.squashSpaces(): String = replace(Regex("\\s+"), " ").trim()

    private fun String.withoutGreeting(): String {
        val clean = squashSpaces()
        if (clean.isBlank()) return clean
        return clean
            .replace(Regex("^good\\s+(morning|afternoon|evening|night)[.!?,]?\\s*", RegexOption.IGNORE_CASE), "")
            .trim()
    }
}

data class BriefingNoteContext(
    val title: String,
    val text: String
)

data class BriefingAppContext(
    val greeting: String,
    val dayPart: String,
    val reminderCount: Int,
    val badgeCount: Int,
    val imageNoteCount: Int,
    val contactsCount: Int,
    val contactsSort: String,
    val webTheme: String,
    val webTextZoom: Int,
    val groupFlights: Boolean,
    val highContrastWeb: Boolean,
    val cachePages: Boolean,
    val blockTrackers: Boolean,
    val reduceWebMotion: Boolean,
    val flightSummary: String,
    val flightIssueCount: Int,
    val flightIssueCards: List<BriefingFlightIssueCard>,
    val weatherSummary: String
) {
    val cacheKey: String
        get() = listOf(
            reminderCount,
            greeting,
            dayPart,
            badgeCount,
            imageNoteCount,
            contactsCount,
            contactsSort,
            webTheme,
            webTextZoom,
            groupFlights,
            highContrastWeb,
            cachePages,
            blockTrackers,
            reduceWebMotion,
            flightSummary,
            flightIssueCount,
            flightIssueCards.joinToString("~") { it.label },
            weatherSummary
        ).joinToString("|")
}

data class BriefingFlightSnapshot(
    val summary: String = "",
    val issueCount: Int = 0,
    val issues: List<BriefingFlightIssueCard> = emptyList(),
    val arrivalCount: Int = 0,
    val departureCount: Int = 0,
    val delayedCount: Int = 0,
    val cancelledCount: Int = 0,
    val divertedCount: Int = 0,
    val source: String = "",
    val updatedAt: Long = 0L
)

data class BriefingFlightIssueCard(
    val label: String = "",
    val flight: String = "",
    val route: String = "",
    val time: String = "",
    val tone: String = ""
)

data class BriefingWeatherSnapshot(
    val temp: String = "",
    val condition: String = "",
    val summary: String = "",
    val source: String = "",
    val updatedAt: Long = 0L
)

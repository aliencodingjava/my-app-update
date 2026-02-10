package com.flights.studio


import android.util.Log

object AiTitleService {

    /**
     * Called by NotesAdapter:
     * withContext(Dispatchers.IO) { AiTitleService.suggestTitle(note) }
     */
    suspend fun suggestTitle(note: String): String {
        val prompt = buildPrompt(note)

        return try {
            GeminiClient.generate(prompt)
                .trim()
                .replace("\n", " ")
                .squashSpaces()
                .take(80) // safety
        } catch (t: Throwable) {
            Log.e("AiTitleService", "Gemini failed: ${t.message}", t)
            ""
        }
    }

    private fun buildPrompt(note: String): String {
        // Keep it short, stable, and safe:
        // Gemini should return ONLY the title text.
        val cleaned = note.trim().take(1500)

        return """
            You generate titles for notes.

            Output rules:
            - Return ONLY the title text (no quotes, no bullets, no extra lines)
            - 3 to 7 words
            - No emojis
            - Title Case

            Note:
            $cleaned
        """.trimIndent()
    }

    private fun String.squashSpaces(): String =
        replace(Regex("\\s+"), " ").trim()
}

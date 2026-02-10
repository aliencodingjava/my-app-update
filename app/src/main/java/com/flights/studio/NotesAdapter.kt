package com.flights.studio

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.core.content.edit
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.kyant.backdrop.Backdrop
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.util.Locale

class NotesAdapter(
    private var notes: List<String>,
    private val appContext: Context,
    private val onLongClick: (String) -> Unit,
    private val onClick: (String, Int) -> Unit,
    private val onEditIconClick: (String, Int) -> Unit,
    private val onReminderClick: (String, Int) -> Unit,
) : RecyclerView.Adapter<NotesAdapter.NoteViewHolder>() {

    // ---------------- UI flags ----------------
    private var reminderFlags: Map<String, Boolean> = emptyMap()
    private var reminderBadgeStates: Map<String, Boolean> = emptyMap()
    private var titleTopCompactDp: Int = NotesPagePrefs.DEFAULT_TITLE_TOP_COMPACT
    private var titleTopNormalDp: Int = NotesPagePrefs.DEFAULT_TITLE_TOP_NORMAL

    private var compactRows: Boolean = false
    private var showImagesBadge: Boolean = true
    private var showReminderBadge: Boolean = true
    private var showReminderBell: Boolean = true
    private var backdrop: Backdrop? = null
    fun setBackdrop(backdrop: Backdrop) {
        this.backdrop = backdrop
        notifyItemRangeChanged(0, itemCount)
    }




    fun applyPageSettings(
        compact: Boolean,
        showImagesBadge: Boolean,
        showReminderBadge: Boolean,
        showReminderBell: Boolean,
        titleTopCompactDp: Int,
        titleTopNormalDp: Int,
    ) {
        val changed =
            compactRows != compact ||
                    this.showImagesBadge != showImagesBadge ||
                    this.showReminderBadge != showReminderBadge ||
                    this.showReminderBell != showReminderBell ||
                    this.titleTopCompactDp != titleTopCompactDp ||
                    this.titleTopNormalDp != titleTopNormalDp

        compactRows = compact
        this.showImagesBadge = showImagesBadge
        this.showReminderBadge = showReminderBadge
        this.showReminderBell = showReminderBell
        this.titleTopCompactDp = titleTopCompactDp
        this.titleTopNormalDp = titleTopNormalDp

        if (changed) notifyItemRangeChanged(0, itemCount)
    }


    // -------- Compose bridge (so LazyColumn can reuse adapter logic) --------


    fun bellOn(note: String): Boolean =
        showReminderBell && (reminderFlags[note] == true)

    fun badgeOn(note: String): Boolean =
        showReminderBadge && (reminderBadgeStates[note] == true)

    fun imagesCount(note: String): Int =
        if (showImagesBadge) NoteMediaStore.getUris(appContext, note).size else 0

    fun titleNow(note: String): String? {
        val typedTitle = getUserTitle(note)
        val cachedTitle = NotesCacheManager.cachedTitles[note]
        return when {
            !typedTitle.isNullOrBlank() -> typedTitle
            !cachedTitle.isNullOrBlank() -> cachedTitle
            else -> null
        }
    }

    fun requestTitleIfNeeded(note: String, onReady: () -> Unit) {
        val typedTitle = getUserTitle(note)
        val cachedTitle = NotesCacheManager.cachedTitles[note]
        val titleNow = when {
            !typedTitle.isNullOrBlank() -> typedTitle
            !cachedTitle.isNullOrBlank() -> cachedTitle
            else -> null
        }

        if (titleNow == null && pendingTitles.add(note)) {
            suggestTitle(appContext, note) { computed ->
                NotesCacheManager.cachedTitles[note] = computed
                pendingTitles.remove(note)
                onReady()
            }
        }
    }

    // forward the SAME click logic your RV used
    fun fireClick(note: String) = onClick(note, positionOf(note))
    fun fireLongClick(note: String) = onLongClick(note)
    fun fireEdit(note: String) = onEditIconClick(note, positionOf(note))
    fun fireReminder(note: String) = onReminderClick(note, positionOf(note))



    // ---------------- selection ----------------
    private val selectedKeys = mutableSetOf<String>()
    private var keyOf: ((String) -> String)? = null

    fun provideKeyResolver(resolver: (String) -> String) { keyOf = resolver }
    // NotesAdapter
//    fun stableKey(note: String): String = (keyOf?.invoke(note) ?: note)


    fun toggleSelectionByKey(key: String) {
        val pos = notes.indexOfFirst { (keyOf?.invoke(it) ?: it) == key }
        if (pos == -1) return

        if (selectedKeys.contains(key)) selectedKeys.remove(key) else selectedKeys.add(key)

        notifyItemChanged(pos)

        // radios appear/disappear on all rows when entering/exiting selection mode
        if (selectedKeys.size == 1 || selectedKeys.isEmpty()) {
            notifyItemRangeChanged(0, notes.size)
        }
    }

    fun clearSelection() {
        if (selectedKeys.isEmpty()) return
        selectedKeys.clear()
        notifyItemRangeChanged(0, notes.size)
    }

    // ---------------- user titles ----------------
    private val titlePrefs = appContext.getSharedPreferences("note_titles", Context.MODE_PRIVATE)

    private val userTypedTitles = mutableMapOf<String, String>().apply {
        putAll(readAllTitles())
    }

    private fun readAllTitles(): Map<String, String> {
        val json = titlePrefs.getString("map", "{}")
        val type = object : TypeToken<Map<String, String>>() {}.type
        return Gson().fromJson<Map<String, String>>(json, type) ?: emptyMap()
    }

    private fun writeAllTitles() {
        titlePrefs.edit { putString("map", Gson().toJson(userTypedTitles)) }
    }

    fun getUserTitle(noteText: String): String? = userTypedTitles[noteText]

    fun setUserTitle(noteText: String, title: String) {
        userTypedTitles[noteText] = title
        writeAllTitles()

        // teach + clear auto cache
        learn(appContext, noteText, title)
        invalidateCache(appContext, noteText)

        val idx = notes.indexOf(noteText)
        if (idx != -1) notifyItemChanged(idx)
    }

    fun removeUserTitle(noteText: String) {
        if (userTypedTitles.remove(noteText) != null) {
            writeAllTitles()
            invalidateCache(appContext, noteText)
            val idx = notes.indexOf(noteText)
            if (idx != -1) notifyItemChanged(idx)
        }
    }

    fun migrateUserTitle(oldNoteText: String, newNoteText: String) {
        userTypedTitles.remove(oldNoteText)?.let { t ->
            userTypedTitles[newNoteText] = t
            writeAllTitles()

            learn(appContext, newNoteText, t)
            invalidateCache(appContext, oldNoteText)

            val idx = notes.indexOf(newNoteText)
            if (idx != -1) notifyItemChanged(idx)
        }
    }

    // ---------------- RecyclerView (Compose rows) ----------------

    class NoteViewHolder(val composeView: ComposeView) : RecyclerView.ViewHolder(composeView)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NoteViewHolder {
        val cv = ComposeView(parent.context).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
        }
        return NoteViewHolder(cv)
    }

    // prevent re-requesting title 20 times while scrolling
    private val pendingTitles = mutableSetOf<String>()

    override fun onBindViewHolder(holder: NoteViewHolder, position: Int) {
        val note = notes[position]
        val key = keyOf?.invoke(note) ?: note

        val selectionMode = selectedKeys.isNotEmpty()
        val selected = selectionMode && selectedKeys.contains(key)

        val bellOn = showReminderBell && (reminderFlags[note] == true)
        val badgeOn = showReminderBadge && (reminderBadgeStates[note] == true)

        val imagesCount = if (showImagesBadge) {
            NoteMediaStore.getUris(holder.composeView.context, note).size
        } else 0

        val typedTitle = userTypedTitles[note]
        val cachedTitle = NotesCacheManager.cachedTitles[note]
        val titleNow: String? = when {
            !typedTitle.isNullOrBlank() -> typedTitle
            !cachedTitle.isNullOrBlank() -> cachedTitle
            else -> null
        }

        if (titleNow == null && pendingTitles.add(note)) {
            suggestTitle(holder.composeView.context, note) { computed ->
                NotesCacheManager.cachedTitles[note] = computed
                pendingTitles.remove(note)

                // ✅ NEVER notify during RV layout; post to next frame
                holder.composeView.post {
                    val p = notes.indexOf(note)
                    if (p != -1) notifyItemChanged(p)
                }
            }
        }

        val b = backdrop

        holder.composeView.setContent {
            FlightsTheme {
                if (b != null) {
                    NoteItem(
                        title = titleNow,
                        note = note,
                        compact = compactRows,
                        dense = compactRows,
                        selectionMode = selectionMode,
                        selected = selected,
                        showReminderBell = bellOn,
                        showReminderBadge = badgeOn,
                        imagesCount = imagesCount,
                        onClick = { onClick(note, holder.bindingAdapterPosition) },
                        onLongClick = { onLongClick(note) },
                        onEdit = { onEditIconClick(note, holder.bindingAdapterPosition) },
                        onReminderClick = { onReminderClick(note, holder.bindingAdapterPosition) },

                        // ✅ NEW
                        backdrop = b,
                        titleTopCompactDp = titleTopCompactDp,
                        titleTopNormalDp = titleTopNormalDp,
                    )

                }
            }
        }

    }

    override fun getItemCount(): Int = notes.size

    // ---------------- list updates ----------------

    fun updateList(newNotes: List<String>) {
        val diffCallback = NotesDiffCallback(notes, newNotes)
        val diffResult = DiffUtil.calculateDiff(diffCallback)
        notes = newNotes
        diffResult.dispatchUpdatesTo(this)
    }

    fun submit(newNotes: List<String>) = updateList(newNotes)

    class NotesDiffCallback(
        private val oldList: List<String>,
        private val newList: List<String>,
    ) : DiffUtil.Callback() {
        override fun getOldListSize(): Int = oldList.size
        override fun getNewListSize(): Int = newList.size
        override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean =
            oldList[oldItemPosition] == newList[newItemPosition]
        override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean =
            oldList[oldItemPosition] == newList[newItemPosition]
    }

    fun positionOf(content: String): Int = notes.indexOf(content)

    fun notifyByContent(content: String) {
        val idx = positionOf(content)
        if (idx != -1) notifyItemChanged(idx)
    }

    // ---------------- reminder prefs ----------------

    fun preloadBadgeStates(context: Context) {
        val prefs = context.getSharedPreferences("reminder_badges", Context.MODE_PRIVATE)
        reminderBadgeStates = notes.associateWith { note ->
            prefs.getBoolean(note.hashCode().toString(), false)
        }
    }

    fun preloadReminderFlags(context: Context) {
        val prefs = context.getSharedPreferences("reminder_flags", Context.MODE_PRIVATE)
        reminderFlags = notes.associateWith { n ->
            prefs.getBoolean(n.hashCode().toString(), false)
        }
    }

    // =====================================================================
    // =============== YOUR TITLE ENGINE (kept intact) ======================
    // =====================================================================

    companion object {

        @JvmStatic
        fun suggestTitle(
            context: Context,
            note: String,
            onTitleReady: (String) -> Unit,
        ) {
            val scope = (context as? AppCompatActivity)?.lifecycleScope
                ?: CoroutineScope(Dispatchers.Main)

            scope.launch {
                val cache = getCache(context)
                val cacheKey = note.hashCode().toString()

                recallLearned(context, note)?.let {
                    onTitleReady(store(cache, cacheKey, it))
                    return@launch
                }

                cache.getString(cacheKey, null)?.let {
                    onTitleReady(it)
                    return@launch
                }

                val aiTitle = try {
                    withContext(Dispatchers.IO) { AiTitleService.suggestTitle(note) }
                } catch (_: Throwable) { "" }

                if (aiTitle.isNotBlank()) {
                    onTitleReady(store(cache, cacheKey, aiTitle.trim()))
                    return@launch
                }

                val offline = generateOfflineTitle(context, note)
                onTitleReady(store(cache, cacheKey, offline))
            }
        }

        // ---------------- offline engine (same as yours) ----------------

        private fun generateOfflineTitle(ctx: Context, raw: String): String {
            val note = normalize(raw)
            quickTaskTitle(ctx, note)?.let { return it }
            domainTitle(ctx, note)?.let { return it }
            categoryTitle(ctx, note)?.let { return it }
            return extractBestSentence(raw)
        }

        private val rxCall   = Regex("""\b(call|ring|dial|phone)\b""", RegexOption.IGNORE_CASE)
        private val rxMsg    = Regex("""\b(text|sms|message|dm|reply)\b""", RegexOption.IGNORE_CASE)
        private val rxMail   = Regex("""\b(email|mail|inbox|send\s+mail)\b""", RegexOption.IGNORE_CASE)
        private val rxBuy    = Regex("""\b(buy|order|purchase|pay|invoice|bill|checkout)\b""", RegexOption.IGNORE_CASE)
        private val rxMeet   = Regex("""\b(book|reserve|schedule|meet|meeting|appointment|appoint)\b""", RegexOption.IGNORE_CASE)

        private val rxTime   = Regex("""\b(\d{1,2}(:\d{2})?\s?(am|pm)?)\b""", RegexOption.IGNORE_CASE)
        private val rxDay    = Regex("""\b(today|tomorrow|mon|tue|wed|thu|fri|sat|sun)\b""", RegexOption.IGNORE_CASE)

        private fun quickTaskTitle(ctx: Context, note: String): String? {
            val lib = loadEmojiLibrary(ctx)

            fun em(section: String, key: String, alt: String): String =
                findJsonEmoji(lib, section, key) ?: alt

            var prefix = ""
            when {
                rxCall.containsMatchIn(note) -> prefix = em("objects", "bell", "📞")
                rxMail.containsMatchIn(note) -> prefix = em("technology", "fax", "✉️")
                rxMsg.containsMatchIn(note)  -> prefix = em("technology", "telephone", "💬")
                rxBuy.containsMatchIn(note)  -> prefix = em("objects", "key", "🛒")
                rxMeet.containsMatchIn(note) || rxDay.containsMatchIn(note) || rxTime.containsMatchIn(note) ->
                    prefix = em("objects", "bell", "📅")
            }

            if (prefix.isNotEmpty()) {
                val phrase = chooseTopPhrase(note)
                val whenTxt = listOfNotNull(rxDay.find(note)?.value, rxTime.find(note)?.value)
                    .joinToString(" ").trim()
                val tail = listOf(phrase, whenTxt).filter { it.isNotBlank() }.joinToString(" ")
                if (tail.isNotBlank()) return "$prefix $tail".squashSpaces().toTitleCase()
            }
            return null
        }

        private fun chooseTopPhrase(note: String): String {
            val firstSentence = note.split(Regex("[.!?\n]")).firstOrNull()?.trim().orEmpty()
            if (firstSentence.isNotBlank()) return firstSentence.take(60)
            val words = tokens(note).take(5).joinToString(" ")
            return words
        }

        private fun domainTitle(ctx: Context, note: String): String? {
            val lib = loadEmojiLibrary(ctx)
            val domains = extractDomains(note)
            if (domains.isEmpty()) return null

            val d0 = domains.first()
            val key = domainToSocialKey(d0)
            val emoji = findJsonEmoji(lib, "social_networks", key)
                ?: findJsonEmoji(lib, "technology", "computer")
                ?: "🌐"

            return "$emoji $d0".toTitleCase()
        }

        private fun extractDomains(text: String): List<String> {
            val rx = """(?i)\b(?:https?://)?(?:www\.)?([\w-]+\.\w{2,})(?:/\S*)?""".toRegex()
            return rx.findAll(text).map { it.groupValues[1].lowercase(Locale.getDefault()) }.toList()
        }

        private fun domainToSocialKey(domain: String): String {
            val d = domain.lowercase(Locale.getDefault())
            return when {
                d.endsWith("youtube.com") || d.endsWith("youtu.be") -> "youtube"
                d.endsWith("twitter.com") || d.endsWith("x.com")    -> "twitter"
                d.endsWith("facebook.com")                          -> "facebook"
                d.endsWith("instagram.com")                         -> "instagram"
                d.endsWith("linkedin.com")                          -> "linkedin"
                d.endsWith("tiktok.com")                            -> "tiktok"
                d.endsWith("pinterest.com")                         -> "pinterest"
                d.endsWith("reddit.com")                            -> "reddit"
                d.endsWith("snapchat.com")                          -> "snapchat"
                d.endsWith("discord.com") || d.endsWith("discord.gg") -> "discord"
                d.endsWith("zoom.us")                               -> "zoom"
                d.endsWith("slack.com")                             -> "slack"
                else -> d.removePrefix("www.")
            }
        }

        private fun categoryTitle(ctx: Context, note: String): String? {
            val keywords = loadKeywords(ctx)
            val lib = loadEmojiLibrary(ctx)

            val toks = tokens(note).map(::stem)
            if (toks.isEmpty()) return null

            val scores = mutableMapOf<String, Int>()
            for ((cat, words) in keywords) {
                for (w in words) {
                    val s = stem(w.lowercase(Locale.getDefault()))
                    if (s.length < 3) continue
                    if (toks.any { it == s }) scores[cat] = (scores[cat] ?: 0) + 1
                }
            }

            if (scores.isEmpty()) return null

            val topCats = scores.entries.sortedByDescending { it.value }.take(3).map { it.key }
            val parts = topCats.mapNotNull { cat ->
                val label = pickLabel(cat)
                val emoji = pickEmoji(lib, cat, note)
                when {
                    label != null && emoji != null -> "$emoji $label"
                    label != null -> label
                    emoji != null -> emoji
                    else -> null
                }
            }.distinct()

            return parts.joinToString(" | ").takeIf { it.isNotBlank() }?.toTitleCase()
        }

        private fun pickLabel(category: String): String? =
            when (category.lowercase(Locale.getDefault())) {
                "coding", "languages", "tools" -> "Coding"
                "work" -> "Work"
                "personal", "relationships" -> "Personal"
                "sentiment" -> null
                "animals" -> "Animals"
                "home", "decor" -> "Home"
                "universe" -> "Space"
                "war" -> "War"
                "guns" -> "Guns"
                "technology", "ai", "future", "gadgets" -> "Tech"
                "gaming" -> "Gaming"
                "reminder", "calendar" -> "Reminder"
                "aviation" -> "Aviation"
                else -> category.replaceFirstChar { it.titlecase(Locale.getDefault()) }
            }

        private val DEFAULT_EMOJI = mapOf(
            "coding" to "💻",
            "languages" to "💻",
            "tools" to "🛠️",
            "work" to "📁",
            "personal" to "📝",
            "relationships" to "💞",
            "animals" to "🐾",
            "home" to "🏠",
            "decor" to "🖼️",
            "universe" to "🌌",
            "war" to "⚔️",
            "guns" to "🔫",
            "technology" to "🤖",
            "ai" to "🤖",
            "future" to "🔮",
            "gadgets" to "📱",
            "gaming" to "🎮",
            "reminder" to "⏰",
            "calendar" to "📅",
            "aviation" to "✈️"
        )

        private fun pickEmoji(lib: JSONObject, category: String, note: String): String? {
            val doms = extractDomains(note)
            if (doms.isNotEmpty()) {
                val k = domainToSocialKey(doms.first())
                findJsonEmoji(lib, "social_networks", k)?.let { return it }
            }
            return DEFAULT_EMOJI[category.lowercase(Locale.getDefault())]
        }

        private fun findJsonEmoji(lib: JSONObject, section: String, key: String): String? {
            val sec = lib.optJSONObject(section) ?: return null
            val em = sec.optString(key.lowercase(Locale.getDefault()), "")
            return em.takeIf { it.isNotBlank() }
        }

        // ---------------- learning ----------------
        private const val LEARN_SP = "note_title_learning"
        private fun learnSp(ctx: Context) = ctx.getSharedPreferences(LEARN_SP, Context.MODE_PRIVATE)

        private fun signatureOf(note: String): String {
            val toks = tokens(normalize(note))
                .filter { it.length > 2 && it !in STOP }
                .map(::stem)
            val freq = toks.groupingBy { it }.eachCount()
                .entries.sortedByDescending { it.value }.take(12).map { it.key }
            return freq.joinToString("|")
        }

        fun learn(ctx: Context, note: String, finalTitle: String) {
            val sig = signatureOf(note)
            learnSp(ctx).edit { putString(sig, finalTitle) }
        }

        private fun recallLearned(ctx: Context, note: String): String? {
            val sp = learnSp(ctx)
            val sig = signatureOf(note)
            sp.getString(sig, null)?.let { return it }
            val parts = sig.split("|")
            if (parts.size > 4) {
                val soft = parts.dropLast(1).joinToString("|")
                sp.all.entries.firstOrNull { it.key.toString().startsWith(soft) }?.value?.toString()?.let { return it }
            }
            return null
        }

        // ---------------- cache ----------------
        private const val CACHE_SP = "note_title_cache"
        private fun getCache(ctx: Context) = ctx.getSharedPreferences(CACHE_SP, Context.MODE_PRIVATE)

        fun invalidateCache(ctx: Context, note: String) {
            getCache(ctx).edit { remove(note.hashCode().toString()) }
        }

        private fun store(sp: SharedPreferences, k: String, v: String): String {
            sp.edit { putString(k, v) }
            return v
        }

        // ---------------- utilities ----------------
        private val STOP = setOf(
            "a","an","the","is","are","was","were","to","in","on","at","of","for","with","by",
            "and","or","but","from","as","that","this","it","be","been","next","event","meeting",
            "service","reminder","note","about","around","into","over","under","up","down","out",
            "off","so","just","very","really","like","get","got","also","etc"
        )

        private fun normalize(s: String) = s.trim().replace(Regex("\\s+"), " ")

        private fun tokens(s: String): List<String> =
            Regex("[A-Za-z0-9_]+").findAll(s).map { it.value.lowercase(Locale.getDefault()) }.toList()

        private fun stem(w: String): String {
            var x = w
            x = x.removeSuffix("ing").removeSuffix("ers").removeSuffix("er")
            x = x.removeSuffix("ments").removeSuffix("ment")
            x = x.removeSuffix("ation").removeSuffix("ations")
            x = x.removeSuffix("ed").removeSuffix("es").removeSuffix("s")
            return x
        }

        private fun String.squashSpaces() = replace(Regex("\\s+"), " ").trim()
        private fun String.toTitleCase(): String =
            split(Regex("\\s+")).joinToString(" ") { it.replaceFirstChar { ch -> ch.titlecase(Locale.getDefault()) } }

        private fun loadEmojiLibrary(context: Context): JSONObject = try {
            val jsonString = context.resources.openRawResource(R.raw.emojis)
                .bufferedReader().use { it.readText() }
            val parsed = JSONObject(jsonString)

            if (!parsed.has("social_networks") && parsed.has("social_network")) {
                parsed.put("social_networks", parsed.getJSONObject("social_network"))
                Log.d("NotesAdapter", "Aliased 'social_network' → 'social_networks'")
            }
            parsed
        } catch (e: Exception) {
            e.printStackTrace()
            JSONObject()
        }

        private fun loadKeywords(context: Context): Map<String, List<String>> = try {
            val jsonString = context.resources.openRawResource(R.raw.keywords)
                .bufferedReader().use { it.readText() }
            val jsonObject = JSONObject(jsonString)
            val flat = mutableMapOf<String, MutableList<String>>()

            fun flatten(category: String, value: Any?) {
                when (value) {
                    is JSONObject -> for (k in value.keys()) flatten(category, value.get(k))
                    is JSONArray -> for (i in 0 until value.length()) flatten(category, value.get(i))
                    is String -> flat.getOrPut(category) { mutableListOf() }.add(value)
                }
            }
            for (key in jsonObject.keys()) flatten(key, jsonObject.get(key))
            flat
        } catch (e: Exception) {
            e.printStackTrace()
            emptyMap()
        }

        private fun extractBestSentence(text: String): String {
            val sentences = text.split(Regex("[.!?]")).map { it.trim() }
            val best = sentences.maxByOrNull { it.length }?.takeIf { it.length > 6 }
            return best ?: text.trim().split(Regex("\\s+")).take(10).joinToString(" ")
                .ifBlank { "Untitled note" }
        }
    }
}

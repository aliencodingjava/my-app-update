package com.flights.studio

import android.content.Context
import android.graphics.Typeface
import android.os.Build
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.BulletSpan
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import android.util.Log
import android.util.Patterns
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.OptIn
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatImageView
import androidx.core.app.ActivityOptionsCompat
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import androidx.core.text.HtmlCompat
import androidx.core.view.doOnLayout
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.badge.BadgeDrawable
import com.google.android.material.badge.BadgeUtils
import com.google.android.material.badge.ExperimentalBadgeUtils
import com.google.android.material.card.MaterialCardView
import com.google.android.material.color.MaterialColors
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.util.Locale

class NotesAdapter(
    private var notes: List<String>,
    private val appContext: Context,
    private val onLongClick: (String) -> Unit,
    private val onClick: (String, Int) -> Unit,
    private val onEditIconClick: (String, Int) -> Unit,
    private val onReminderClick: (String, Int) -> Unit,
//    private val getStableId: (String) -> Long,   // ⬅️ new
) : RecyclerView.Adapter<NotesAdapter.NoteViewHolder>() {

//    init { setHasStableIds(true) }

//    override fun getItemId(position: Int): Long = getStableId(notes[position])

    private val tagRx     = Regex("""#\w+""")
    private val mentionRx = Regex("""@\w+""")
    private val timeRx    = Regex("""\b(\d{1,2}(:\d{2})?\s?(am|pm)?)\b""", RegexOption.IGNORE_CASE)
    private val dayRx     = Regex("""\b(today|tomorrow|mon|tue|wed|thu|fri|sat|sun)\b""", RegexOption.IGNORE_CASE)
    private var reminderFlags: Map<String, Boolean> = emptyMap()


    companion object {
        /**
         * AI-first title generation using AiTitleService.
         * Falls back to a smarter offline generator (domains+tasks+categories+learning).
         */
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

                // 0) learned memory first (user-typed titles → reused)
                recallLearned(context, note)?.let {
                    onTitleReady(store(cache, cacheKey, it))
                    return@launch
                }

                // 1) cache
                cache.getString(cacheKey, null)?.let {
                    onTitleReady(it)
                    return@launch
                }

                // 2) Try your online AI (if it returns blank, we continue offline)
                val aiTitle = try {
                    withContext(Dispatchers.IO) { AiTitleService.suggestTitle(note) }
                } catch (_: Throwable) { "" }

                if (aiTitle.isNotBlank()) {
                    onTitleReady(store(cache, cacheKey, aiTitle.trim()))
                    return@launch
                }

                // 3) Offline generator (smart)
                val offline = generateOfflineTitle(context, note)
                onTitleReady(store(cache, cacheKey, offline))
            }
        }

        const val PAYLOAD_IMAGES = "payload_images_changed"

        // ===================== Offline Engine =====================

        private fun generateOfflineTitle(ctx: Context, raw: String): String {
            val note = normalize(raw)

            // A) direct task/time heuristics (highest priority)
            quickTaskTitle(ctx, note)?.let { return it }

            // B) social / domain cues
            domainTitle(ctx, note)?.let { return it }

            // C) keyword categories (your keywords.json) + emoji library
            categoryTitle(ctx, note)?.let { return it }

            // D) fallback: best sentence
            return extractBestSentence(raw)
        }

        // ---------- A) Task/time heuristics ----------
        private val rxCall   = Regex("""\b(call|ring|dial|phone)\b""", RegexOption.IGNORE_CASE)
        private val rxMsg    = Regex("""\b(text|sms|message|dm|reply)\b""", RegexOption.IGNORE_CASE)
        private val rxMail   = Regex("""\b(email|mail|inbox|send\s+mail)\b""", RegexOption.IGNORE_CASE)
        private val rxBuy    = Regex("""\b(buy|order|purchase|pay|invoice|bill|checkout)\b""", RegexOption.IGNORE_CASE)
        private val rxMeet   = Regex("""\b(book|reserve|schedule|meet|meeting|appointment|appoint)\b""", RegexOption.IGNORE_CASE)

        private val rxTime   = Regex("""\b(\d{1,2}(:\d{2})?\s?(am|pm)?)\b""", RegexOption.IGNORE_CASE)
        private val rxDay    = Regex("""\b(today|tomorrow|mon|tue|wed|thu|fri|sat|sun)\b""", RegexOption.IGNORE_CASE)

        private fun quickTaskTitle(ctx: Context, note: String): String? {
            val lib = loadEmojiLibrary(ctx)

            fun em(section: String, key: String, alt: String): String {
                return findJsonEmoji(lib, section, key) ?: alt
            }

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
                val whenTxt = listOfNotNull(
                    rxDay.find(note)?.value,
                    rxTime.find(note)?.value
                ).joinToString(" ").trim()
                val tail = listOf(phrase, whenTxt).filter { it.isNotBlank() }.joinToString(" ")
                if (tail.isNotBlank()) return "$prefix $tail".squashSpaces().toTitleCase()
            }
            return null
        }

        private fun chooseTopPhrase(note: String): String {
            // first sentence, or first 5 words
            val firstSentence = note.split(Regex("[.!?\n]")).firstOrNull()?.trim().orEmpty()
            if (firstSentence.isNotBlank()) return firstSentence.take(60)
            val words = tokens(note).take(5).joinToString(" ")
            return words
        }

        // ---------- B) Domain / social ----------
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
                d.endsWith("discord.com") || d.endsWith("discord.gg")-> "discord"
                d.endsWith("zoom.us")                               -> "zoom"
                d.endsWith("slack.com")                             -> "slack"
                else -> d.removePrefix("www.")
            }
        }

        // ---------- C) Category scoring via keywords.json ----------
        private fun categoryTitle(ctx: Context, note: String): String? {
            val keywords = loadKeywords(ctx) // Map<Category, List<String>>
            val lib = loadEmojiLibrary(ctx)

            val toks = tokens(note).map(::stem)
            if (toks.isEmpty()) return null

            val scores = mutableMapOf<String, Int>()
            val hits   = mutableMapOf<String, MutableSet<String>>() // category -> matched words

            for ((cat, words) in keywords) {
                for (w in words) {
                    val s = stem(w.lowercase(Locale.getDefault()))
                    if (s.length < 3) continue
                    if (toks.any { it == s }) {
                        scores[cat] = (scores[cat] ?: 0) + 1
                        hits.getOrPut(cat) { mutableSetOf() }.add(w)
                    }
                }
            }

            if (scores.isEmpty()) return null

            val topCats = scores.entries.sortedByDescending { it.value }.take(3).map { it.key }

            val parts = topCats.mapNotNull { cat ->
                val label = pickLabel(cat)
                val emoji = pickEmoji(lib, cat, note)        // ✅ matches (lib, category, note)
                when {
                    label != null && emoji != null -> "$emoji $label"
                    label != null -> label
                    emoji != null -> emoji
                    else -> null
                }
            }.distinct()


            val title = parts.joinToString(" | ").takeIf { it.isNotBlank() }
            return title?.toTitleCase()
        }

        private fun pickLabel(category: String): String? {
            return when (category.lowercase(Locale.getDefault())) {
                "coding", "languages", "tools"   -> "Coding"
                "work"                            -> "Work"
                "personal", "relationships"       -> "Personal"
                "sentiment"                       -> null
                "animals"                         -> "Animals"
                "home", "decor"                   -> "Home"
                "universe"                        -> "Space"
                "war"                              -> "War"
                "guns"                             -> "Guns"
                "technology", "ai", "future", "gadgets" -> "Tech"
                "gaming"                           -> "Gaming"
                "reminder", "calendar"             -> "Reminder"
                "aviation"                         -> "Aviation"
                else                               -> category.replaceFirstChar { it.titlecase() }
            }
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

        // signature
        private fun pickEmoji(lib: JSONObject, category: String, note: String): String? {
            // 1) If note has a domain that maps to a social icon
            val doms = extractDomains(note)
            if (doms.isNotEmpty()) {
                val k = domainToSocialKey(doms.first())
                findJsonEmoji(lib, "social_networks", k)?.let { return it }
            }

            // 2) Try category-specific emoji in your JSON
            when (category.lowercase(Locale.getDefault())) {
                "animals" -> {
                    Regex("""\b(cat|dog|lion|tiger|elephant|panda|koala|rabbit|monkey|horse|cow)\b""",
                        RegexOption.IGNORE_CASE).find(note)?.groupValues?.firstOrNull()
                        ?.lowercase(Locale.getDefault())
                        ?.let { a -> findJsonEmoji(lib, "animals", a)?.let { return it } }
                }
                "technology", "ai", "gadgets" -> {
                    findJsonEmoji(lib, "technology", "computer")?.let { return it }
                }
                "reminder", "calendar" -> {
                    findJsonEmoji(lib, "objects", "bell")?.let { return it }
                }
                "aviation" -> return "✈️"
            }

            // 3) Defaults
            return DEFAULT_EMOJI[category.lowercase(Locale.getDefault())]
        }


        private fun findJsonEmoji(lib: JSONObject, section: String, key: String): String? {
            val sec = lib.optJSONObject(section) ?: return null
            val em = sec.optString(key.lowercase(Locale.getDefault()), "")
            return em.takeIf { it.isNotBlank() }
        }

        // ===================== Learning (remembers manual titles) =====================

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

        // Let callers reset cache if content changes
        fun invalidateCache(ctx: Context, note: String) {
            getCache(ctx).edit { remove(note.hashCode().toString()) }
        }

        // ===================== Cache =====================

        private const val CACHE_SP = "note_title_cache"
        private fun getCache(ctx: Context) = ctx.getSharedPreferences(CACHE_SP, Context.MODE_PRIVATE)
        private fun store(sp: android.content.SharedPreferences, k: String, v: String): String {
            sp.edit { putString(k, v) }; return v
        }

        // ===================== Utilities =====================

        private val STOP = setOf(
            "a","an","the","is","are","was","were","to","in","on","at","of","for","with","by",
            "and","or","but","from","as","that","this","it","be","been","next","event","meeting",
            "service","reminder","note","about","around","into","over","under","up","down","out",
            "off","so","just","very","really","like","get","got","also","etc"
        )

        private fun normalize(s: String) = s.trim().replace(Regex("\\s+"), " ")

        private fun tokens(s: String): List<String> =
            Regex("[A-Za-z0-9_]+").findAll(s).map { it.value.lowercase(Locale.getDefault()) }.toList()

        // bare-minimum stemmer (not perfect; good enough offline)
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

        // ---------------- You already have these loaders ----------------

        private fun loadEmojiLibrary(context: Context): JSONObject = try {
            val jsonString = context.resources.openRawResource(R.raw.emojis)
                .bufferedReader().use { it.readText() }
            if (jsonString.isBlank()) throw RuntimeException("emojis.json is empty")

            val parsed = JSONObject(jsonString)
            if (parsed.length() == 0) throw RuntimeException("emojis.json parsed empty")

            if (!parsed.has("social_networks") && parsed.has("social_network")) {
                parsed.put("social_networks", parsed.getJSONObject("social_network"))
                Log.d("NotesAdapter", "Aliased 'social_network' → 'social_networks'")
            }
            parsed
        } catch (e: Exception) {
            e.printStackTrace(); JSONObject()
        }

        private fun loadKeywords(context: Context): Map<String, List<String>> = try {
            val jsonString = context.resources.openRawResource(R.raw.keywords)
                .bufferedReader().use { it.readText() }
            val jsonObject = JSONObject(jsonString)
            val flat = mutableMapOf<String, MutableList<String>>()

            fun flatten(category: String, value: Any?) {
                when (value) {
                    is JSONObject -> for (k in value.keys()) flatten(category, value.get(k))
                    is org.json.JSONArray -> for (i in 0 until value.length()) flatten(category, value.get(i))
                    is String -> flat.getOrPut(category) { mutableListOf() }.add(value)
                }
            }
            for (key in jsonObject.keys()) flatten(key, jsonObject.get(key))
            flat
        } catch (e: Exception) {
            e.printStackTrace(); emptyMap()
        }

        private fun extractBestSentence(text: String): String {
            val sentences = text.split(Regex("[.!?]")).map { it.trim() }
            val best = sentences.maxByOrNull { it.length }?.takeIf { it.length > 6 }
            return best ?: text.trim().split(Regex("\\s+")).take(10).joinToString(" ")
                .ifBlank { "Untitled note" }
        }
    }

    private var reminderBadgeStates: Map<String, Boolean> = emptyMap()

    private val selectedKeys = mutableSetOf<String>()
    private var keyOf: ((String) -> String)? = null


    private val titlePrefs = appContext.getSharedPreferences("note_titles", Context.MODE_PRIVATE)

    private val userTypedTitles = mutableMapOf<String, String>().apply {
        putAll(readAllTitles())
    }
    private fun readAllTitles(): Map<String, String> {
        val json = titlePrefs.getString("map", "{}")
        val type = object : com.google.gson.reflect.TypeToken<Map<String, String>>() {}.type
        return com.google.gson.Gson().fromJson<Map<String, String>>(json, type) ?: emptyMap()
    }
    private fun writeAllTitles() {
        titlePrefs.edit { putString("map", com.google.gson.Gson().toJson(userTypedTitles)) }
    }

    inner class NoteViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val noteTextView: TextView = view.findViewById(R.id.tv_note)
        val editIcon: ImageView = view.findViewById(R.id.expandCollapseIcon)
        val reminderIcon: AppCompatImageView = view.findViewById(R.id.reminderIcon)
        val contentArea: View = view.findViewById(R.id.contentArea)
        val card: MaterialCardView = view as MaterialCardView
        val radio: com.google.android.material.radiobutton.MaterialRadioButton = view.findViewById(R.id.checkedIcon)

        // 👇 NEW
        val imagesIcon: AppCompatImageView = view.findViewById(R.id.note_images)
        var imagesBadge: BadgeDrawable? = null
    }


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NoteViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_note, parent, false)
        return NoteViewHolder(view)
    }
    override fun onBindViewHolder(
        holder: NoteViewHolder,
        position: Int,
        payloads: MutableList<Any>
    ) {
        if (payloads.isNotEmpty() && payloads.contains(PAYLOAD_IMAGES)) {
            // only refresh the images badge/count for this row
            bindImagesBadge(holder, notes[position])
            return
        }
        // fall back to full bind if no relevant payload
        super.onBindViewHolder(holder, position, payloads)
    }

    @OptIn(ExperimentalBadgeUtils::class)
    override fun onBindViewHolder(holder: NoteViewHolder, position: Int) {
        val note = notes[position]
        val key = keyOf?.invoke(note) ?: note

        holder.noteTextView.text = buildNotePreviewStyled(note, holder.itemView.context)

        // Make the outer card inert; use contentArea for clicks
        holder.itemView.isClickable = false
        holder.itemView.isFocusable = false
        holder.itemView.setOnClickListener(null)
        holder.itemView.setOnLongClickListener(null)

        val titleTextView: TextView = holder.itemView.findViewById(R.id.tv_title)
        val context = holder.itemView.context

        // Title (user-typed → cached → async compute)
        val typed = userTypedTitles[note]
        if (!typed.isNullOrBlank()) {
            titleTextView.text = typed
        } else if (NotesCacheManager.cachedTitles.containsKey(note)) {
            titleTextView.text = parseHtmlNeutral(NotesCacheManager.cachedTitles[note])

        } else {
            titleTextView.text = context.getString(R.string.loading)
            titleTextView.tag = note
            suggestTitle(context, note) { computed ->
                NotesCacheManager.cachedTitles[note] = computed
                if (
                    holder.bindingAdapterPosition != RecyclerView.NO_POSITION &&
                    titleTextView.tag == note
                ) {
                    titleTextView.post {
                        titleTextView.text = parseHtmlNeutral(computed)
                    }

                }
            }
        }

        val cardView = holder.itemView as MaterialCardView
        val radio = holder.radio
        radio.isClickable = false
        radio.isEnabled = false

        if (selectedKeys.isNotEmpty()) {              // ⬅️ use selectedKeys now
            val isSelected = selectedKeys.contains(key)  // ⬅️ check with key
            radio.visibility = View.VISIBLE
            radio.isChecked = isSelected

            cardView.setCardBackgroundColor(
                ContextCompat.getColor(
                    holder.itemView.context,
                    if (isSelected) com.wdullaer.materialdatetimepicker.R.color.mdtp_accent_color_dark
                    else R.color.note_selected
                )
            )
            cardView.radius = 20f
            cardView.cardElevation = 1f
        } else {
            radio.visibility = View.GONE
            radio.isChecked = false
            cardView.setCardBackgroundColor(
                ContextCompat.getColor(holder.itemView.context, R.color.box_alert_update)
            )
            cardView.radius = 20f
            cardView.cardElevation = 1f
        }

        // Row interaction (content only)
        holder.contentArea.setOnClickListener { onClick(note, holder.bindingAdapterPosition) }
        holder.contentArea.setOnLongClickListener { onLongClick(note); true }

        // Icons
        holder.editIcon.setOnClickListener { onEditIconClick(note, holder.bindingAdapterPosition) }
        holder.reminderIcon.setOnClickListener { onReminderClick(note, holder.bindingAdapterPosition) }

        // Reminder badge dot animation
        val badgeDot = holder.itemView.findViewById<View>(R.id.reminderBadge)
        val badgeVisible = reminderBadgeStates[note] == true
        if (badgeVisible) {
            badgeDot.visibility = View.VISIBLE
            if (badgeDot.animation == null) {
                badgeDot.startAnimation(
                    AnimationUtils.loadAnimation(holder.itemView.context, R.anim.pulse_timer)
                )
            }
        } else {
            badgeDot.visibility = View.GONE
            badgeDot.clearAnimation()
        }

        // 📸 Images badge (count) + click → ViewNoteActivity

        bindImagesBadge(holder, note)


        // Show/hide reminder icon based on persisted tiny flag
        val bellOn = reminderFlags[note] == true
        holder.reminderIcon.visibility = if (bellOn) View.VISIBLE else View.GONE
        holder.reminderIcon.setOnClickListener { onReminderClick(note, holder.bindingAdapterPosition) }

    }

    private fun buildNotePreviewStyled(
        raw: String,
        ctx: Context,
        maxBullets: Int = 4,
        maxChars: Int = 400
    ): CharSequence {
        val text = raw.trim()
        if (text.isEmpty()) return ""

        val lines = text.lines()
            .asSequence()
            .map(String::trim)
            .filter(String::isNotEmpty)
            .toList()

        if (lines.isEmpty()) return ""

        val styleConfig = StyleConfig(ctx)

        return when {
            lines.size >= 2 -> buildBulletedPreview(lines, styleConfig, maxBullets)
            else -> buildSingleLinePreview(lines.first(), styleConfig, maxChars)
        }
    }

    private data class StyleConfig(
        val gap: Int,
        val radius: Int,
        val bulletColor: Int,
        val tagColor: Int,
        val timeColor: Int
    ) {
        constructor(ctx: Context) : this(
            gap = (10 * ctx.resources.displayMetrics.density).toInt(),
            radius = (6 * ctx.resources.displayMetrics.density).toInt(),
            bulletColor = MaterialColors.getColor(
                ctx, com.google.android.material.R.attr.colorSecondary, 0
            ),
            tagColor = MaterialColors.getColor(
                ctx, com.google.android.material.R.attr.colorOnSurface, 0
            ),
            timeColor = MaterialColors.getColor(
                ctx, com.google.android.material.R.attr.colorOnSurface, 0
            )
        )
    }


    private fun parseHtmlNeutral(raw: String?): CharSequence {
        if (raw.isNullOrEmpty()) return ""
        val sp = HtmlCompat.fromHtml(raw, HtmlCompat.FROM_HTML_MODE_LEGACY) as? Spannable ?: return raw
        sp.getSpans(0, sp.length, ForegroundColorSpan::class.java).forEach { sp.removeSpan(it) }
        return sp
    }


    private fun buildBulletedPreview(
        lines: List<String>,
        config: StyleConfig,
        maxBullets: Int
    ): CharSequence {
        val sb = SpannableStringBuilder()
        val visibleLines = lines.take(maxBullets)

        visibleLines.forEachIndexed { index, line ->
            val paraStart = sb.length
            sb.append(line)

            // Apply bullet span
            val bulletSpan = createBulletSpan(config.gap, config.bulletColor, config.radius)
            sb.setSpan(bulletSpan, paraStart, sb.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)

            // Apply inline styling
            applyInlineStyles(sb, paraStart, line, config)

            // Add newline except for last item
            if (index < visibleLines.lastIndex) {
                sb.append('\n')
            }
        }

        // Add ellipsis if there are more lines
        if (lines.size > maxBullets) {
            sb.append("\n…")
        }

        return sb
    }

    private fun buildSingleLinePreview(
        line: String,
        config: StyleConfig,
        maxChars: Int
    ): CharSequence {
        val sb = SpannableStringBuilder()
        val truncated = if (line.length <= maxChars) {
            line
        } else {
            line.take(maxChars).trimEnd() + "…"
        }

        val start = sb.length
        sb.append(truncated)
        applyInlineStyles(sb, start, truncated, config)

        return sb
    }

    private fun createBulletSpan(gap: Int, color: Int, radius: Int): BulletSpan {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S_V2) { // API 28
            BulletSpan(gap, color, radius)
        } else {
            BulletSpan(gap, color)
        }
    }

    private fun applyInlineStyles(
        sb: SpannableStringBuilder,
        baseOffset: Int,
        text: CharSequence,
        config: StyleConfig
    ) {
        // Apply tag styling
        tagRx.findAll(text).forEach { match ->
            applySpan(sb, baseOffset + match.range.first, baseOffset + match.range.last + 1) {
                ForegroundColorSpan(config.tagColor)
            }
        }

        // Apply mention styling (bold + colored)
        mentionRx.findAll(text).forEach { match ->
            val start = baseOffset + match.range.first
            val end = baseOffset + match.range.last + 1
            applySpan(sb, start, end) { ForegroundColorSpan(config.tagColor) }
            applySpan(sb, start, end) { StyleSpan(Typeface.BOLD) }
        }

        // Apply time styling (bold + colored)
        listOf(timeRx, dayRx).forEach { regex ->
            regex.findAll(text).forEach { match ->
                val start = baseOffset + match.range.first
                val end = baseOffset + match.range.last + 1
                applySpan(sb, start, end) { ForegroundColorSpan(config.timeColor) }
                applySpan(sb, start, end) { StyleSpan(Typeface.BOLD) }
            }
        }

        // Apply URL styling (bold + colored)
        val urlMatcher = Patterns.WEB_URL.matcher(text)
        while (urlMatcher.find()) {
            val start = baseOffset + urlMatcher.start()
            val end = baseOffset + urlMatcher.end()
            applySpan(sb, start, end) { ForegroundColorSpan(config.timeColor) }
            applySpan(sb, start, end) { StyleSpan(Typeface.BOLD) }
        }
    }

    private inline fun applySpan(
        sb: SpannableStringBuilder,
        start: Int,
        end: Int,
        spanFactory: () -> Any
    ) {
        sb.setSpan(spanFactory(), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
    }
    // Titles the user typed by hand
    fun getUserTitle(noteText: String): String? = userTypedTitles[noteText]

    fun setUserTitle(noteText: String, title: String) {
        // persist user-typed title
        userTypedTitles[noteText] = title
        writeAllTitles()

        // 🧠 teach the offline title engine & clear any auto-title cache for this note
        learn(appContext, noteText, title)
        invalidateCache(appContext, noteText)

        // refresh row
        val idx = notes.indexOf(noteText)
        if (idx != -1) notifyItemChanged(idx)
    }

    fun removeUserTitle(noteText: String) {
        if (userTypedTitles.remove(noteText) != null) {
            writeAllTitles()

            // drop any cached auto-title so it’ll be recomputed cleanly
            invalidateCache(appContext, noteText)

            val idx = notes.indexOf(noteText)
            if (idx != -1) notifyItemChanged(idx)
        }
    }
    fun migrateUserTitle(oldNoteText: String, newNoteText: String) {
        userTypedTitles.remove(oldNoteText)?.let { t ->
            // carry the user’s title forward to the new text
            userTypedTitles[newNoteText] = t
            writeAllTitles()

            // 🧠 reinforce learning on the new text and clear stale cache on the old
            learn(appContext, newNoteText, t)
            invalidateCache(appContext, oldNoteText)

            val idx = notes.indexOf(newNoteText)
            if (idx != -1) notifyItemChanged(idx)
        }
    }
    override fun getItemCount(): Int = notes.size

    fun updateList(newNotes: List<String>) {
        val diffCallback = NotesDiffCallback(notes, newNotes)
        val diffResult = DiffUtil.calculateDiff(diffCallback)
        notes = newNotes
        diffResult.dispatchUpdatesTo(this)
    }

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



    fun preloadBadgeStates(context: Context) {
        val prefs = context.getSharedPreferences("reminder_badges", Context.MODE_PRIVATE)
        reminderBadgeStates = notes.associateWith { note ->
            prefs.getBoolean(note.hashCode().toString(), false)
        }
    }




    @OptIn(ExperimentalBadgeUtils::class)
    private fun bindImagesBadge(holder: NoteViewHolder, note: String) {
        val ctx = holder.itemView.context
        val icon = holder.imagesIcon

        val uris = NoteMediaStore.getUris(ctx, note)
        val count = uris.size

        // 👇 choose icon by count
        val iconRes = if (count > 1) {
            R.drawable.my_multi_icon   // your “multiple images” icon
        } else {
            R.drawable.ic_oui_image          // your “single image” icon
        }
        icon.setImageResource(iconRes)

        fun attachOrUpdateBadge() {
            holder.imagesBadge?.let { BadgeUtils.detachBadgeDrawable(it, icon) }
            val badge = holder.imagesBadge ?: BadgeDrawable.create(ctx).also { holder.imagesBadge = it }

            badge.isVisible = true
            badge.number = count
            badge.badgeGravity = BadgeDrawable.TOP_END

            val d = ctx.resources.displayMetrics.density
            badge.horizontalOffset = (15 * d).toInt()
            badge.verticalOffset   = (15 * d).toInt()
            badge.backgroundColor  = ContextCompat.getColor(ctx, R.color.material_yellow)
            badge.badgeTextColor   = ContextCompat.getColor(ctx, android.R.color.black)
            badge.maxCharacterCount = 3

            BadgeUtils.attachBadgeDrawable(badge, icon, null)
        }

        if (count > 0) {
            icon.visibility = View.VISIBLE
            icon.contentDescription = ctx.getString(R.string.images_attached)
            if (icon.isLaidOut) attachOrUpdateBadge() else icon.doOnLayout { attachOrUpdateBadge() }
            icon.setOnClickListener {
                (ctx as? AllNotesActivity)?.let { act ->
                    val intent = ViewNoteActivity.newIntent(
                        act, note, holder.bindingAdapterPosition, getUserTitle(note)
                    )
                    val opts = ActivityOptionsCompat.makeCustomAnimation(
                        act, R.anim.m3_motion_fade_enter, R.anim.m3_motion_fade_exit
                    )
                    act.startActivity(intent, opts.toBundle())
                }
            }
        } else {
            icon.visibility = View.GONE   // use GONE if you don’t want to keep space; INVISIBLE if you do
            icon.contentDescription = null
            icon.setOnClickListener(null)
            holder.imagesBadge?.let { BadgeUtils.detachBadgeDrawable(it, icon) }
            holder.imagesBadge = null
        }
    }
    fun preloadReminderFlags(context: Context) {
        val prefs = context.getSharedPreferences("reminder_flags", Context.MODE_PRIVATE)
        reminderFlags = notes.associateWith { n -> prefs.getBoolean(n.hashCode().toString(), false) }
    }

    // Inside NotesAdapter
    fun submit(newNotes: List<String>) {
        val diff = DiffUtil.calculateDiff(NotesDiffCallback(this.notes, newNotes))
        this.notes = newNotes
        diff.dispatchUpdatesTo(this)
    }

    fun positionOf(content: String): Int = notes.indexOf(content)

    fun notifyByContent(content: String, payload: Any? = null) {
        val idx = positionOf(content)
        if (idx != -1) {
            if (payload == null) notifyItemChanged(idx) else notifyItemChanged(idx, payload)
        }
    }


    fun provideKeyResolver(resolver: (String) -> String) { keyOf = resolver }
    fun toggleSelectionByKey(key: String) {
        val pos = notes.indexOfFirst { (keyOf?.invoke(it) ?: it) == key }
        if (pos == -1) return

        if (selectedKeys.contains(key)) selectedKeys.remove(key) else selectedKeys.add(key)

        // Rebind just the tapped row
        notifyItemChanged(pos)

        // When entering or exiting multi-select, the radio visibility changes on every row
        if (selectedKeys.size == 1 || selectedKeys.isEmpty()) {
            notifyItemRangeChanged(0, notes.size)
        }
    }

    fun clearSelection() {
        if (selectedKeys.isEmpty()) return
        selectedKeys.clear()
        // We just left multi-select → radios disappear on every row
        notifyItemRangeChanged(0, notes.size)
    }



}

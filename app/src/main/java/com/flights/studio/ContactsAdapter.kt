package com.flights.studio

import android.content.Context
import android.content.Intent
import android.content.ContextWrapper
import android.content.SharedPreferences
import android.content.res.ColorStateList
import android.content.res.Configuration
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.util.TypedValue
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.HorizontalScrollView
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.animation.doOnEnd
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import androidx.core.graphics.ColorUtils
import androidx.core.graphics.createBitmap
import androidx.core.graphics.drawable.toDrawable
import androidx.core.net.toUri
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListUpdateCallback
import androidx.recyclerview.widget.RecyclerView
import androidx.fragment.app.FragmentActivity
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.DownsampleStrategy
import com.bumptech.glide.request.RequestOptions
import com.flights.studio.databinding.ItemContactBinding
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


@Suppress("DEPRECATION")
class ContactsAdapter(

    private val contacts: MutableList<AllContact>,
    private val context: Context,
    private val onDeleteConfirmed: (AllContact, Int) -> Unit,
    private val onItemClicked: (AllContact) -> Unit,
    private val onSelectionChanged: (Int) -> Unit = {},
    private val onContactOpened: (AllContact) -> Unit = {},
    private val onRecentContactMenuRequested: (AllContact, () -> Unit) -> Unit = { _, _ -> },


) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    data class ColorPalette(
        val mainColor: Int,       // for combined_card
        val overlayColor: Int,    // for expandableOverlay
        val buttonColor: Int,      // for the 3 action buttons
    )




    private var expandedPosition = -1
    private var filteredContacts: MutableList<AllContact> = contacts.toMutableList()
    private var searchQuery: String = ""
    private var suppressRecentTextRow = false
    private var alphabeticalMode = false
    private var recentTextContacts: List<AllContact> = emptyList()
    private val displayRows = mutableListOf<DisplayRow>()
    private val selectedContactIds = linkedSetOf<String>()
    private val birthdayPrefs: SharedPreferences =
        context.getSharedPreferences("birthday_reminders", Context.MODE_PRIVATE)
    private val palettePrefs: SharedPreferences =
        context.getSharedPreferences("contact_palettes", Context.MODE_PRIVATE)
    private val paletteCache = mutableMapOf<String, ColorPalette>()
    private val hiddenRecentPrefs: SharedPreferences =
        context.getSharedPreferences("recent_text_contacts", Context.MODE_PRIVATE)
    private val flagDrawableCache = mutableMapOf<String, Drawable>()
    private val flagSizePx = (context.resources.displayMetrics.density * 24).toInt().coerceAtLeast(1)
    private val avatarLoadSizePx = (context.resources.displayMetrics.density * 72).toInt().coerceAtLeast(96)
    private val loadContactPhotosInList = true
    private val diffScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var updateJob: Job? = null
    private var updateGeneration = 0


    init {
        setHasStableIds(true)
        rebuildDisplayRows()
    }

    private val showRecentTextRow: Boolean
        get() = !suppressRecentTextRow && !alphabeticalMode && searchQuery.isBlank() && recentTextContacts.isNotEmpty()

    override fun getItemViewType(position: Int): Int =
        when (displayRows.getOrNull(position)) {
            DisplayRow.RecentText -> VIEW_TYPE_RECENT_TEXT
            is DisplayRow.Section -> VIEW_TYPE_SECTION
            is DisplayRow.Contact -> VIEW_TYPE_CONTACT
            null -> VIEW_TYPE_CONTACT
        }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        if (viewType == VIEW_TYPE_RECENT_TEXT) {
            return RecentTextViewHolder(parent)
        }
        if (viewType == VIEW_TYPE_SECTION) {
            return SectionViewHolder(parent)
        }
        val binding = ItemContactBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ContactViewHolder(binding).apply {
            // lock radius here so every holder is consistent
            val r = 0f

            fun com.google.android.material.card.MaterialCardView.lockRadius(px: Float) {
                shapeAppearanceModel = shapeAppearanceModel
                    .toBuilder()
                    .setAllCornerSizes(px)
                    .build()
                radius = px
                clipToOutline = true
                invalidateOutline()
            }

            binding.combinedCard.lockRadius(r)
            binding.deleteBackground.lockRadius(r)
        }
    }

    override fun onBindViewHolder(
        holder: RecyclerView.ViewHolder,
        position: Int,
        payloads: MutableList<Any>
    ) {
        if (holder !is ContactViewHolder) {
            onBindViewHolder(holder, position)
            return
        }
        if (payloads.isNotEmpty() && payloads.any { it == "toggle_expand" }) {
            holder.setExpandableState(contactIndexForAdapterPosition(position), animate = true)
        } else if (payloads.isNotEmpty() && payloads.any { it == "selection" }) {
            holder.bindSelectionState()
        } else {
            super.onBindViewHolder(holder, position, payloads)
        }
    }
    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder is RecentTextViewHolder) {
            holder.bind(recentTextContacts)
            return
        }
        if (holder is SectionViewHolder) {
            holder.bind((displayRows[position] as DisplayRow.Section).letter)
            return
        }
        val contactPosition = contactIndexForAdapterPosition(position)
        if (contactPosition !in filteredContacts.indices) return
        val contact = filteredContacts[contactPosition]
        (holder as ContactViewHolder).bind(contact, contactPosition)
    }

    override fun getItemId(position: Int): Long =
        when (val row = displayRows.getOrNull(position)) {
            DisplayRow.RecentText -> Long.MIN_VALUE
            is DisplayRow.Section -> Long.MIN_VALUE + row.letter.hashCode()
            is DisplayRow.Contact -> filteredContacts.getOrNull(row.contactIndex)?.id?.hashCode()?.toLong() ?: RecyclerView.NO_ID
            null -> RecyclerView.NO_ID
        }



    fun getFilteredContacts(): List<AllContact> = filteredContacts

    override fun getItemCount() = displayRows.size

    fun preloadAvatarsAround(startPosition: Int, count: Int = 24) {
        if (!loadContactPhotosInList || filteredContacts.isEmpty()) return

        val start = startPosition.coerceAtLeast(0)
        val end = (start + count).coerceAtMost(filteredContacts.size)
        for (index in start until end) {
            val rawPhoto = filteredContacts[index].photoUri?.trim().orEmpty()
            if (rawPhoto.isBlank()) continue

            Glide.with(context)
                .load(resolvePhotoModel(rawPhoto))
                .apply(
                    RequestOptions()
                        .override(avatarLoadSizePx, avatarLoadSizePx)
                        .downsample(DownsampleStrategy.CENTER_INSIDE)
                        .circleCrop()
                        .dontAnimate()
                )
                .preload(avatarLoadSizePx, avatarLoadSizePx)
        }
    }

    private fun resolvePhotoModel(rawPhoto: String): Any {
        return when {
            rawPhoto.startsWith("content://", true) -> rawPhoto
            rawPhoto.startsWith("file://", true) -> rawPhoto
            rawPhoto.startsWith("http://", true) || rawPhoto.startsWith("https://", true) -> rawPhoto
            rawPhoto.startsWith("/") -> java.io.File(rawPhoto)
            else -> rawPhoto
        }
    }

    fun contactIndexForAdapterPosition(adapterPosition: Int): Int =
        (displayRows.getOrNull(adapterPosition) as? DisplayRow.Contact)?.contactIndex ?: -1

    fun adapterPositionForContactIndex(contactIndex: Int): Int =
        displayRows.indexOfFirst { it is DisplayRow.Contact && it.contactIndex == contactIndex }

    fun isAlphabeticalMode(): Boolean = alphabeticalMode

    fun setAlphabeticalMode(enabled: Boolean) {
        if (alphabeticalMode == enabled) return
        alphabeticalMode = enabled
        expandedPosition = -1
        rebuildDisplayRows()
        notifyDataSetChanged()
    }

    fun getAvailableSectionLetters(): List<String> =
        filteredContacts
            .map { sectionLetterFor(it.name) }
            .distinct()

    fun adapterPositionForSection(letter: String): Int =
        displayRows.indexOfFirst { it is DisplayRow.Section && it.letter == letter }

    fun setSearchQuery(query: String) {
        if (searchQuery == query) return
        searchQuery = query
        rebuildDisplayRows()
        notifyDataSetChanged()
    }

    fun setRecentTextSuppressed(suppressed: Boolean) {
        if (suppressRecentTextRow == suppressed) return
        suppressRecentTextRow = suppressed
        rebuildDisplayRows()
        notifyDataSetChanged()
    }

    fun updateContact(position: Int, updatedContact: AllContact) {
        val originalIndex = contacts.indexOfFirst { it.id == filteredContacts[position].id }
        if (originalIndex != -1) {
            contacts[originalIndex] = updatedContact
            filteredContacts[position] = updatedContact
            rebuildDisplayRows()
            val adapterPosition = adapterPositionForContactIndex(position)
            if (adapterPosition != RecyclerView.NO_POSITION) {
                notifyItemChanged(adapterPosition)
            } else {
                notifyDataSetChanged()
            }
        }
    }


    fun updateData(newList: List<AllContact>) {
        if (filteredContacts != newList) {
            updateJob?.cancel()
            filteredContacts.clear()
            filteredContacts.addAll(newList)
            refreshRecentTextContacts(notify = false)
            rebuildDisplayRows()
            notifyDataSetChanged()
            pruneSelectionToVisibleContacts()
        }
    }

    fun resetData() {
        updateData(contacts)
    }

    fun setFilteredContacts(newList: List<AllContact>) {
        filteredContacts.clear()
        filteredContacts.addAll(newList)
        refreshRecentTextContacts(notify = false)
        rebuildDisplayRows()
        notifyDataSetChanged()
    }

    val isSelectionMode: Boolean
        get() = selectedContactIds.isNotEmpty()

    fun clearSelection() {
        if (selectedContactIds.isEmpty()) return
        selectedContactIds.clear()
        notifyItemRangeChanged(0, itemCount, "selection")
        onSelectionChanged(0)
    }

    fun getSelectedContacts(): List<AllContact> =
        filteredContacts.filter { selectedContactIds.contains(it.id) }

    fun selectAllVisibleContacts() {
        if (filteredContacts.isEmpty()) return
        expandedPosition = -1
        selectedContactIds.clear()
        selectedContactIds.addAll(filteredContacts.map { it.id })
        notifyItemRangeChanged(0, itemCount, "selection")
        onSelectionChanged(selectedContactIds.size)
    }

    fun startSelectionAtAdapterPosition(adapterPosition: Int) {
        val contactPosition = contactIndexForAdapterPosition(adapterPosition)
        if (adapterPosition == RecyclerView.NO_POSITION || contactPosition !in filteredContacts.indices) return
        startSelection(filteredContacts[contactPosition])
    }

    fun toggleSelectionAtAdapterPosition(adapterPosition: Int) {
        val contactPosition = contactIndexForAdapterPosition(adapterPosition)
        if (adapterPosition == RecyclerView.NO_POSITION || contactPosition !in filteredContacts.indices) return
        toggleSelection(filteredContacts[contactPosition])
    }

    private fun toggleSelection(contact: AllContact) {
        val wasSelectionMode = isSelectionMode
        if (!selectedContactIds.add(contact.id)) {
            selectedContactIds.remove(contact.id)
        }
        val position = filteredContacts.indexOfFirst { it.id == contact.id }
        if (wasSelectionMode && selectedContactIds.isEmpty()) {
            notifyItemRangeChanged(0, itemCount, "selection")
        } else if (position != RecyclerView.NO_POSITION) {
            notifyItemChanged(adapterPositionForContactIndex(position), "selection")
        }
        onSelectionChanged(selectedContactIds.size)
    }

    private fun startSelection(contact: AllContact) {
        if (!isSelectionMode) {
            expandedPosition = -1
        }
        if (selectedContactIds.contains(contact.id)) return
        selectedContactIds.add(contact.id)
        val position = filteredContacts.indexOfFirst { it.id == contact.id }
        if (position != RecyclerView.NO_POSITION) {
            notifyItemChanged(adapterPositionForContactIndex(position), "selection")
        }
        onSelectionChanged(selectedContactIds.size)
    }

    private fun pruneSelectionToVisibleContacts() {
        if (selectedContactIds.isEmpty()) return
        val visibleIds = filteredContacts.mapTo(hashSetOf()) { it.id }
        if (selectedContactIds.retainAll(visibleIds)) {
            onSelectionChanged(selectedContactIds.size)
        }
    }

    private fun refreshRecentTextContacts(notify: Boolean = true) {
        val previousVisible = showRecentTextRow
        recentTextContacts = emptyList()
        if (!notify) return

        val nowVisible = showRecentTextRow
        when {
            previousVisible && !nowVisible -> notifyItemRemoved(0)
            !previousVisible && nowVisible -> notifyItemInserted(0)
        }
    }

    private fun isRecentContactHidden(contactId: String): Boolean =
        hiddenRecentPrefs.getStringSet(KEY_HIDDEN_RECENT_CONTACTS, emptySet()).orEmpty().contains(contactId)

    fun hideRecentContact(contact: AllContact) {
        val hidden = hiddenRecentPrefs
            .getStringSet(KEY_HIDDEN_RECENT_CONTACTS, emptySet())
            .orEmpty()
            .toMutableSet()
        hidden.add(contact.id)
        hiddenRecentPrefs.edit { putStringSet(KEY_HIDDEN_RECENT_CONTACTS, hidden) }
        Toast.makeText(context, "Removed from Recent", Toast.LENGTH_SHORT).show()
        refreshRecentTextContacts()
    }

    private fun rebuildDisplayRows() {
        displayRows.clear()
        if (showRecentTextRow) {
            displayRows.add(DisplayRow.RecentText)
        }
        var previousLetter: String? = null
        filteredContacts.forEachIndexed { index, contact ->
            if (alphabeticalMode && searchQuery.isBlank()) {
                val letter = sectionLetterFor(contact.name)
                if (letter != previousLetter) {
                    displayRows.add(DisplayRow.Section(letter))
                    previousLetter = letter
                }
            }
            displayRows.add(DisplayRow.Contact(index))
        }
    }

    private fun sectionLetterFor(name: String): String {
        val first = name.trim().firstOrNull { it.isLetterOrDigit() } ?: return "#"
        return if (first.isLetter()) first.uppercaseChar().toString() else "#"
    }

    private fun dp(value: Int): Int =
        (value * context.resources.displayMetrics.density).toInt()

    class ContactDiffCallback(
        private val oldList: List<AllContact>,
        private val newList: List<AllContact>,
    ) : DiffUtil.Callback() {

        override fun getOldListSize(): Int = oldList.size
        override fun getNewListSize(): Int = newList.size

        override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            return oldList[oldItemPosition].id == newList[newItemPosition].id
        }

        override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            return oldList[oldItemPosition] == newList[newItemPosition]
        }
    }

    inner class RecentTextViewHolder(parent: ViewGroup) : RecyclerView.ViewHolder(
        LinearLayout(parent.context).apply {
            layoutParams = RecyclerView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            orientation = LinearLayout.VERTICAL
            setPadding(dp(6), dp(18), dp(6), dp(18))
            setBackgroundColor(Color.TRANSPARENT)
        }
    ) {
        private val title = TextView(parent.context).apply {
            text = "Recent"
            setTextColor(if (isDarkTheme(context)) Color.rgb(225, 225, 230) else Color.rgb(80, 80, 86))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            includeFontPadding = false
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = dp(14).toFloat()
                setColor(if (isDarkTheme(context)) Color.argb(54, 255, 255, 255) else Color.argb(235, 255, 255, 255))
                setStroke(dp(1), if (isDarkTheme(context)) Color.argb(28, 255, 255, 255) else Color.argb(18, 0, 0, 0))
            }
            setPadding(dp(12), dp(5), dp(12), dp(5))
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                leftMargin = dp(8)
                bottomMargin = dp(12)
            }
        }

        private val scroller = HorizontalScrollView(parent.context).apply {
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            isHorizontalScrollBarEnabled = false
            overScrollMode = View.OVER_SCROLL_NEVER
            setBackgroundColor(Color.TRANSPARENT)
        }

        private val row = LinearLayout(parent.context).apply {
            orientation = LinearLayout.HORIZONTAL
            clipToPadding = false
            clipChildren = false
            setBackgroundColor(Color.TRANSPARENT)
        }

        init {
            val container = itemView as LinearLayout
            container.addView(title)
            container.addView(scroller)
            scroller.addView(
                row,
                FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
            )
        }

        fun bind(contacts: List<AllContact>) {
            row.removeAllViews()
            contacts.forEach { contact ->
                row.addView(createRecentContactView(contact))
            }
        }

        private fun createRecentContactView(contact: AllContact): View {
            val avatarSize = dp(74)
            val borderColor = if (isDarkTheme(context)) Color.rgb(88, 92, 100) else Color.WHITE
            val openChat = {
                // Removed
            }
            val openRecentMenu = {
                onRecentContactMenuRequested(contact) {
                    hideRecentContact(contact)
                }
            }
            val pressScaleTouch = View.OnTouchListener { view, event ->
                when (event.actionMasked) {
                    MotionEvent.ACTION_DOWN -> {
                        view.animate()
                            .scaleX(0.94f)
                            .scaleY(0.94f)
                            .setDuration(90L)
                            .start()
                    }
                    MotionEvent.ACTION_UP,
                    MotionEvent.ACTION_CANCEL -> {
                        view.animate()
                            .scaleX(1f)
                            .scaleY(1f)
                            .setDuration(160L)
                            .start()
                    }
                }
                false
            }
            val item = LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                gravity = android.view.Gravity.CENTER
                clipChildren = false
                clipToPadding = false
                setPadding(dp(6), dp(2), dp(6), dp(4))
                layoutParams = LinearLayout.LayoutParams(dp(112), ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                    marginEnd = dp(12)
                }
                setOnTouchListener(pressScaleTouch)
                setOnClickListener { openChat() }
                setOnLongClickListener {
                    openRecentMenu()
                    true
                }
            }
            val avatarFrame = FrameLayout(context).apply {
                layoutParams = LinearLayout.LayoutParams(avatarSize, avatarSize)
                clipChildren = false
                clipToPadding = false
                setBackgroundColor(Color.TRANSPARENT)
                setOnClickListener { openChat() }
                setOnLongClickListener {
                    openRecentMenu()
                    true
                }
            }
            val rawPhoto = contact.photoUri?.trim().orEmpty()
            if (rawPhoto.isNotBlank()) {
                val image = ImageView(context).apply {
                    layoutParams = FrameLayout.LayoutParams(avatarSize, avatarSize)
                    scaleType = ImageView.ScaleType.CENTER_CROP
                    background = GradientDrawable().apply {
                        shape = GradientDrawable.OVAL
                        setColor(contact.color)
                    }
                    clipToOutline = true
                }
                Glide.with(context)
                    .load(resolvePhotoModel(rawPhoto))
                    .apply(
                        RequestOptions()
                            .override(avatarLoadSizePx, avatarLoadSizePx)
                            .downsample(DownsampleStrategy.CENTER_INSIDE)
                            .circleCrop()
                            .dontAnimate()
                    )
                    .into(image)
                avatarFrame.addView(image)
            } else {
                avatarFrame.addView(TextView(context).apply {
                    layoutParams = FrameLayout.LayoutParams(avatarSize, avatarSize)
                    gravity = android.view.Gravity.CENTER
                    text = getInitials(contact.name).uppercase()
                    setTextColor(Color.WHITE)
                    setTextSize(TypedValue.COMPLEX_UNIT_SP, 19f)
                    setTypeface(typeface, android.graphics.Typeface.BOLD)
                    background = GradientDrawable().apply {
                        shape = GradientDrawable.OVAL
                        setColor(contact.color)
                    }
                })
            }
            avatarFrame.addView(View(context).apply {
                layoutParams = FrameLayout.LayoutParams(avatarSize, avatarSize)
                background = GradientDrawable().apply {
                    shape = GradientDrawable.OVAL
                    setColor(Color.TRANSPARENT)
                    setStroke(dp(2), borderColor)
                }
            })
            val label = TextView(context).apply {
                layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
                gravity = android.view.Gravity.CENTER
                maxLines = 1
                ellipsize = android.text.TextUtils.TruncateAt.END
                text = contact.name.ifBlank { "Contact" }
                setTextColor(if (isDarkTheme(context)) Color.WHITE else Color.rgb(28, 28, 30))
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 12.5f)
                setTypeface(typeface, android.graphics.Typeface.BOLD)
                setPadding(0, dp(5), 0, 0)
                setOnClickListener { openChat() }
                setOnLongClickListener {
                    openRecentMenu()
                    true
                }
            }
            item.addView(avatarFrame)
            item.addView(label)
            return item
        }

    }

    inner class SectionViewHolder(parent: ViewGroup) : RecyclerView.ViewHolder(
        TextView(parent.context).apply {
            layoutParams = RecyclerView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                dp(54)
            )
            gravity = android.view.Gravity.CENTER_VERTICAL
            setPadding(dp(84), dp(8), dp(16), 0)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 23f)
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            setTextColor(if (isDarkTheme(context)) Color.rgb(134, 134, 140) else Color.rgb(116, 116, 122))
            includeFontPadding = false
        }
    ) {
        fun bind(letter: String) {
            (itemView as TextView).text = letter
        }
    }

    private fun getInitials(name: String): String {
        val nameParts = name.split(" ")
        return nameParts.getOrNull(0)?.take(1).orEmpty() +
                nameParts.getOrNull(1)?.take(1).orEmpty()
    }
    fun isExpanded(position: Int): Boolean {
        return position == expandedPosition
    }
    fun getTextcolorForBackground(context: Context, backgroundColor: Int): Int {
        if (backgroundColor == Color.TRANSPARENT) {
            return if (isDarkTheme(context)) Color.WHITE else Color.BLACK
        }

        val luminance = ColorUtils.calculateLuminance(backgroundColor)
        val color = if (luminance < 0.5) Color.WHITE else Color.BLACK

        return color
    }


    fun isDarkTheme(context: Context): Boolean {
        val theme = context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
        return theme == Configuration.UI_MODE_NIGHT_YES
    }


    inner class ContactViewHolder(private val binding: ItemContactBinding) :
        RecyclerView.ViewHolder(binding.root) {

        private var currentContact: AllContact? = null

        fun bind(contact: AllContact, contactPosition: Int) {
            currentContact = contact
            with(binding) {
                textName.text = contact.name.ifBlank { "(No name)" }

                // Hide delete background by default
                deleteBackground.visibility = View.GONE
                deleteBackground.alpha = 0f

                if (loadContactPhotosInList && !contact.photoUri.isNullOrBlank()) {
                    iconImage.visibility = View.VISIBLE
                    iconInitials.visibility = View.GONE

                    val rawPhoto = contact.photoUri?.trim().orEmpty()

                    val model = when {
                        rawPhoto.startsWith("content://", true) -> rawPhoto
                        rawPhoto.startsWith("file://", true) -> rawPhoto
                        rawPhoto.startsWith("http://", true) || rawPhoto.startsWith("https://", true) -> rawPhoto
                        rawPhoto.startsWith("/") -> java.io.File(rawPhoto)   // ✅ local file path
                        else -> rawPhoto
                    }

                    Glide.with(context)
                        .load(model)
                        .placeholder(de.dlyt.yanndroid.samsung.R.drawable.ic_samsung_image)
                        .error(R.drawable.avatar_11)
                        .apply(
                            RequestOptions()
                                .override(avatarLoadSizePx, avatarLoadSizePx)
                                .downsample(DownsampleStrategy.CENTER_INSIDE)
                                .circleCrop()
                                .dontAnimate()
                        )
                        .into(iconImage)

                } else {
                    Glide.with(context).clear(iconImage)
                    iconImage.visibility = View.GONE
                    iconInitials.visibility = View.VISIBLE
                    iconInitials.text = getInitials(contact.name).uppercase()

                    val background = GradientDrawable().apply {
                        shape = GradientDrawable.OVAL
                        setColor(contact.color)
                    }
                    iconInitials.background = background
                }





                // Retrieve and set the current color from preferences
                val palette = getPaletteForContact(contact.id)
                val headerColor = resolvePaletteColor(palette.mainColor, defaultContactSurfaceColor())

                combinedCard.setCardBackgroundColor(headerColor)
                collapsingTextContainer.setBackgroundColor(headerColor)

                val topTextColor = getTextcolorForBackground(context, headerColor)
                textName.setTextColor(topTextColor)
                bindSelectionState()

                if (isExpanded(contactPosition)) {
                    bindExpandableContent(contact, palette)
                }
                setExpandableState(contactPosition, animate = false) // snap to state in bind; no animation
                val openThreadClickListener = View.OnClickListener {
                    val adapterPosition = adapterPosition
                    if (adapterPosition == RecyclerView.NO_POSITION) return@OnClickListener
                    if (isSelectionMode) {
                        currentContact?.let { toggleSelection(it) }
                    } else {
                        currentContact?.let { openChat(it) }
                    }
                }
                val openProfileClickListener = View.OnClickListener {
                    val adapterPosition = adapterPosition
                    if (adapterPosition == RecyclerView.NO_POSITION) return@OnClickListener
                    if (isSelectionMode) {
                        currentContact?.let { toggleSelection(it) }
                    } else {
                        currentContact?.let { openContactProfile(it) }
                    }
                }
                itemView.setOnClickListener(openThreadClickListener)
                combinedCard.setOnClickListener(openThreadClickListener)
                collapsingTextContainer.setOnClickListener(openThreadClickListener)
                iconInitials.setOnClickListener(openProfileClickListener)
                iconImage.setOnClickListener(openProfileClickListener)
                textName.setOnClickListener(openThreadClickListener)
                expandCollapseIcon.setOnClickListener(openProfileClickListener)
                val longPressListener = View.OnLongClickListener {
                    currentContact?.let { contact -> startSelection(contact) }
                    true
                }
                itemView.setOnLongClickListener(longPressListener)
                combinedCard.setOnLongClickListener(longPressListener)
                collapsingTextContainer.setOnLongClickListener(longPressListener)
                iconInitials.setOnLongClickListener(longPressListener)
                iconImage.setOnLongClickListener(longPressListener)
                textName.setOnLongClickListener(longPressListener)
            }
        }

        fun bindSelectionState() {
            val contact = currentContact ?: return
            val selected = selectedContactIds.contains(contact.id)
            with(binding) {
                contactSelectionCheck?.visibility = if (isSelectionMode) View.VISIBLE else View.GONE
                contactSelectionCheck?.text = if (selected) "✓" else ""
                contactSelectionCheck?.animate()?.cancel()
                if (!isSelectionMode) {
                    contactSelectionCheck?.alpha = 0f
                    contactSelectionCheck?.scaleX = 0.82f
                    contactSelectionCheck?.scaleY = 0.82f
                    combinedCard.animate().cancel()
                    combinedCard.alpha = 1f
                    expandCollapseIcon.visibility = View.GONE
                    return
                }
                val checkBackground = GradientDrawable().apply {
                    shape = GradientDrawable.OVAL
                    setColor(if (selected) Color.rgb(82, 196, 66) else Color.TRANSPARENT)
                    setStroke(
                        (1.5f * context.resources.displayMetrics.density).toInt().coerceAtLeast(1),
                        if (selected) Color.WHITE else getTextcolorForBackground(
                            context,
                            resolvePaletteColor(getPaletteForContact(contact.id).mainColor, defaultContactSurfaceColor())
                        )
                    )
                }
                contactSelectionCheck?.background = checkBackground
                contactSelectionCheck?.animate()
                    ?.alpha(if (selected) 1f else 0.34f)
                    ?.scaleX(if (selected) 1f else 0.82f)
                    ?.scaleY(if (selected) 1f else 0.82f)
                    ?.setDuration(180L)
                    ?.start()
                combinedCard.animate()
                    .alpha(if (isSelectionMode && !selected) 0.76f else 1f)
                    .setDuration(160L)
                    .start()
                expandCollapseIcon.visibility = View.GONE
            }
        }

        private fun bindExpandableContent(contact: AllContact, palette: ColorPalette) {
            with(binding) {
                val contentColor = resolvePaletteColor(palette.overlayColor, defaultContactSurfaceColor())
                val phoneText = contact.phone.ifEmpty { "No Phone Available" }
                textPhone.textDirection = View.TEXT_DIRECTION_LTR
                textPhone.text = phoneText
                textPhone.setCompoundDrawablesRelativeWithIntrinsicBounds(null, null, null, null)
                textPhone.compoundDrawablePadding = 0

                flagEnd.visibility = View.GONE
                flagEnd.setImageDrawable(null)
                if (!contact.flag.isNullOrEmpty()) {
                    flagEnd.visibility = View.VISIBLE
                    getCachedEmojiDrawable(contact.flag, flagSizePx)?.let { flagEnd.setImageDrawable(it) }
                }

                textEmail.text = contact.email ?: "No Email Available"
                textAddress.text = contact.address ?: "No Address Available"
                textBirthday.text = contact.birthday ?: "No Birthday Available"
                birthdayIcon.visibility = if (
                    !contact.birthday.isNullOrBlank() &&
                    birthdayPrefs.getBoolean("${contact.id}_birthday_set", false)
                ) {
                    View.VISIBLE
                } else {
                    View.GONE
                }

                expandableContent.setBackgroundColor(
                    if (palette.overlayColor == Color.TRANSPARENT) contentColor else getSofterColor(contentColor)
                )

                updateButtonColors(fabDelete, palette.buttonColor)
                updateButtonColors(fabUpdate, palette.buttonColor)
                updateButtonColors(fabCall, palette.buttonColor)
                updateButtonColors(fabChat, palette.buttonColor)

                val middleTextColor = getTextcolorForBackground(context, contentColor)
                textPhone.setTextColor(middleTextColor)
                textEmail.setTextColor(middleTextColor)
                textAddress.setTextColor(middleTextColor)
                textBirthday.setTextColor(middleTextColor)

                setupColorPickerListeners(contact)
                fabUpdate.setOnClickListener { onItemClicked(contact) }
                fabCall.setOnClickListener { handleCallClick(contact) }
                fabChat.setOnClickListener { openChat(contact) }
                fabDelete.setOnClickListener {
                    val pos = adapterPosition
                    if (pos != RecyclerView.NO_POSITION) {
                        onDeleteConfirmed(contact, pos)
                    }
                }
            }
        }




        private fun getSofterColor(color: Int): Int {
            val factor = 0.85f
            val alpha = (Color.alpha(color) * factor).toInt()
            val red = (Color.red(color) * factor).toInt()
            val green = (Color.green(color) * factor).toInt()
            val blue = (Color.blue(color) * factor).toInt()
            return Color.argb(alpha, red, green, blue)
        }

        private fun updateButtonColors(button: MaterialButton, backgroundColor: Int) {
            val isDark = ColorUtils.calculateLuminance(backgroundColor) < 0.5
            val textColor = if (isDark) Color.WHITE else Color.BLACK

            button.setBackgroundColor(backgroundColor) // optional fallback
            button.backgroundTintList = ColorStateList.valueOf(backgroundColor) // ✅ required for Material3
            button.setTextColor(textColor)
            button.iconTint = ColorStateList.valueOf(textColor)
            button.strokeWidth = 0
            button.strokeColor = null
        }
        private fun triggerPaintDripAnimation(binding: ItemContactBinding) {
            val container = binding.bubbleContainer

            // List of nice bold paint colors
            val paintColors = listOf(
                0xFFE53935.toInt(), // Red
                0xFF1E88E5.toInt(), // Blue
                0xFF43A047.toInt(), // Green
                0xFFFDD835.toInt(), // Yellow
                0xFF8E24AA.toInt(), // Purple
                0xFFFB8C00.toInt(), // Orange
                0xFF000000.toInt(), // Black
                0xFF90CAF9.toInt()  // Light blue (your original)
            )

            container.post {
                val widthPx = container.width
                val heightPx = container.height

                repeat(10) {
                    val dripWidth = (4..8).random()
                    val dripHeight = (20..heightPx).random()
                    val xPosition = (0..(widthPx - dripWidth)).random()
                    val paintColor = paintColors.random()

                    val drip = View(container.context).apply {
                        layoutParams = FrameLayout.LayoutParams(dripWidth, dripHeight)
                        background = GradientDrawable().apply {
                            shape = GradientDrawable.RECTANGLE
                            cornerRadius = 4f
                            setColor(paintColor)
                        }
                        alpha = 0.95f
                        translationX = xPosition.toFloat()
                        translationY = binding.holoColorSelector.top.toFloat() + 4f
                        translationZ = 20f
                        scaleY = 0f
                    }

                    container.addView(drip)

                    drip.animate()
                        .scaleY(1f)
                        .translationY(drip.translationY + dripHeight)
                        .alpha(0f)
                        .setDuration((800..1200).random().toLong())
                        .withEndAction { container.removeView(drip) }
                        .start()
                }
            }
        }


        private fun setupColorPickerListeners(contact: AllContact) {
            binding.holoColorSelector.setOnClickListener {
                triggerPaintDripAnimation(binding)
                showHoloColorPicker { palette ->
                    updateContactPalette(contact, palette)
                }
            }
        }

        private fun updateContactPalette(contact: AllContact, palette: ColorPalette) {
            if (palette.mainColor == Color.TRANSPARENT &&
                palette.overlayColor == Color.TRANSPARENT &&
                palette.buttonColor == Color.TRANSPARENT
            ) {
                // Reset case
                context.getSharedPreferences("contact_palettes", Context.MODE_PRIVATE).edit {
                    remove("${contact.id}_main")
                    remove("${contact.id}_overlay")
                    remove("${contact.id}_button")
                }
                paletteCache.remove(contact.id)

                val fallbackBackground = defaultContactSurfaceColor()
                val fallbackTextColor = getTextcolorForBackground(context, fallbackBackground)

                binding.combinedCard.setCardBackgroundColor(fallbackBackground)
                binding.collapsingTextContainer.setBackgroundColor(fallbackBackground)
                binding.expandableContent.setBackgroundColor(fallbackBackground)

                // ✅ Force correct tint for buttons
                val buttonList = listOf(binding.fabChat, binding.fabCall, binding.fabUpdate, binding.fabDelete)
                buttonList.forEach { button ->
                    button.setBackgroundColor(fallbackBackground)
                    button.backgroundTintList = ColorStateList.valueOf(fallbackBackground)
                    button.setTextColor(fallbackTextColor)
                    button.iconTint = ColorStateList.valueOf(fallbackTextColor)
                }

                binding.textName.setTextColor(fallbackTextColor)
                binding.textPhone.setTextColor(fallbackTextColor)
                binding.textEmail.setTextColor(fallbackTextColor)
                binding.textAddress.setTextColor(fallbackTextColor)
                binding.textBirthday.setTextColor(fallbackTextColor)

            } else {
                // 🔁 Normal palette save
                savePaletteForContact(contact.id, palette)

                binding.apply {
                    val headerColor = resolvePaletteColor(palette.mainColor, defaultContactSurfaceColor())
                    val contentColor = resolvePaletteColor(palette.overlayColor, defaultContactSurfaceColor())

                    combinedCard.setCardBackgroundColor(headerColor)
                    collapsingTextContainer.setBackgroundColor(headerColor)
                    expandableContent.setBackgroundColor(contentColor)
                    updateButtonColors(fabDelete, palette.buttonColor)
                    updateButtonColors(fabUpdate, palette.buttonColor)
                    updateButtonColors(fabCall, palette.buttonColor)
                    updateButtonColors(fabChat, palette.buttonColor)

                    val topTextColor = getTextcolorForBackground(context, headerColor)
                    val middleTextColor = getTextcolorForBackground(context, contentColor)

                    textName.setTextColor(topTextColor)
                    textPhone.setTextColor(middleTextColor)
                    textEmail.setTextColor(middleTextColor)
                    textAddress.setTextColor(middleTextColor)
                    textBirthday.setTextColor(middleTextColor)
                }
            }

            notifyItemChanged(adapterPosition)
        }


        fun setExpandableState(position: Int, animate: Boolean) {
            with(binding) {
                val shouldExpand = expandedPosition == position
                val isVisible = expandableContent.isVisible
                if (shouldExpand) {
                    currentContact?.let { bindExpandableContent(it, getPaletteForContact(it.id)) }
                }

                // cancel running
                expandableContent.animate().cancel()
                expandCollapseIcon.animate().cancel()

                expandCollapseIcon.visibility = View.GONE

                if (!animate) {
                    expandableContent.visibility = if (shouldExpand) View.VISIBLE else View.GONE
                    expandableContent.layoutParams.height = ViewGroup.LayoutParams.WRAP_CONTENT
                    expandableContent.alpha = 1f
                    expandableContent.scaleY = 1f
                    return
                }

                if (shouldExpand && !isVisible) {
                    // measure target height
                    expandableContent.visibility = View.VISIBLE
                    expandableContent.alpha = 0f
                    expandableContent.scaleY = 0.98f

                    expandableContent.measure(
                        View.MeasureSpec.makeMeasureSpec(itemView.width, View.MeasureSpec.EXACTLY),
                        View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
                    )
                    val targetH = expandableContent.measuredHeight

                    // start collapsed
                    expandableContent.layoutParams = expandableContent.layoutParams.apply { height = 0 }

                    val anim = android.animation.ValueAnimator.ofInt(0, targetH).apply {
                        duration = 320L
                        interpolator = androidx.interpolator.view.animation.FastOutSlowInInterpolator()
                        addUpdateListener {
                            val h = it.animatedValue as Int
                            expandableContent.layoutParams = expandableContent.layoutParams.apply { height = h }
                        }
                        doOnEnd {
                            // let it wrap after expand completes
                            expandableContent.layoutParams = expandableContent.layoutParams.apply {
                                height = ViewGroup.LayoutParams.WRAP_CONTENT
                            }
                        }
                    }

                    expandableContent.animate()
                        .alpha(1f)
                        .scaleY(1f)
                        .setDuration(220L)
                        .setInterpolator(androidx.interpolator.view.animation.FastOutSlowInInterpolator())
                        .start()

                    anim.start()

                } else if (!shouldExpand && isVisible) {
                    // collapse from current height to 0
                    val startH = expandableContent.height

                    val anim = android.animation.ValueAnimator.ofInt(startH, 0).apply {
                        duration = 240L
                        interpolator = androidx.interpolator.view.animation.FastOutLinearInInterpolator()
                        addUpdateListener {
                            val h = it.animatedValue as Int
                            expandableContent.layoutParams = expandableContent.layoutParams.apply { height = h }
                        }
                        doOnEnd {
                            expandableContent.visibility = View.GONE
                            expandableContent.alpha = 1f
                            expandableContent.scaleY = 1f
                            expandableContent.layoutParams = expandableContent.layoutParams.apply {
                                height = ViewGroup.LayoutParams.WRAP_CONTENT
                            }
                        }
                    }

                    expandableContent.animate()
                        .alpha(0f)
                        .scaleY(0.98f)
                        .setDuration(180L)
                        .setInterpolator(androidx.interpolator.view.animation.FastOutLinearInInterpolator())
                        .start()

                    anim.start()
                }
            }
        }





        private fun handleExpandCollapseClick(position: Int) {
            val previous = expandedPosition
            expandedPosition = if (expandedPosition == position) -1 else position

            if (previous != RecyclerView.NO_POSITION && previous in 0 until itemCount) {
                notifyItemChanged(previous, "toggle_expand")
            }
            if (position in 0 until filteredContacts.size) {
                notifyItemChanged(position, "toggle_expand")
            }
        }


        private fun handleCallClick(contact: AllContact) {
            if (contact.phone.isNotEmpty()) {
                val dialIntent = Intent(Intent.ACTION_DIAL, "tel:${contact.phone}".toUri())
                context.startActivity(dialIntent)
            } else {
                Toast.makeText(context, "Invalid phone number", Toast.LENGTH_SHORT).show()
            }
        }

        private fun openChat(contact: AllContact) {
            // Removed
        }

        private fun openContactProfile(contact: AllContact) {
            onContactOpened(contact)
            onItemClicked(contact)
        }

        private fun savePaletteForContact(contactId: String, palette: ColorPalette) {
            palettePrefs.edit {
                putInt("${contactId}_main", palette.mainColor)
                    .putInt("${contactId}_overlay", palette.overlayColor)
                    .putInt("${contactId}_button", palette.buttonColor)
            }
            paletteCache[contactId] = palette
        }

        private fun getPaletteForContact(contactId: String): ColorPalette {
            paletteCache[contactId]?.let { return it }

            val isDark = isDarkTheme(context)
            val fallbackButton = if (isDark) Color.BLACK else Color.WHITE

            val main = palettePrefs.getInt("${contactId}_main", Color.TRANSPARENT)
            val overlay = palettePrefs.getInt("${contactId}_overlay", Color.TRANSPARENT)
            val button = palettePrefs.getInt("${contactId}_button", fallbackButton)

            return ColorPalette(main, overlay, button).also { paletteCache[contactId] = it }
        }

        private fun defaultContactSurfaceColor(): Int =
            if (isDarkTheme(context)) Color.rgb(21, 22, 23)
            else ContextCompat.getColor(context, R.color.box_alert_update)

        private fun resolvePaletteColor(color: Int, fallback: Int): Int =
            if (color == Color.TRANSPARENT) fallback else color





        private fun showHoloColorPicker(onPaletteSelected: (ColorPalette) -> Unit) {
            val activity = context.findFragmentActivity()
            activity?.supportFragmentManager?.let { fm ->
                val palettePicker = PalettePickerBottomSheet(onPaletteSelected)
                palettePicker.show(fm, "PalettePicker")
            }
        }


    }

    fun getEmojiDrawable(context: Context, emoji: String, sizePx: Int): Drawable? = try {
        val tv = TextView(context).apply {
            text = emoji
            includeFontPadding = false
            // draw the glyph slightly smaller than the box so it doesn’t clip
            setTextSize(TypedValue.COMPLEX_UNIT_PX, sizePx * 0.88f)
            setTextColor(Color.BLACK)
            measure(
                View.MeasureSpec.makeMeasureSpec(sizePx, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(sizePx, View.MeasureSpec.EXACTLY)
            )
            layout(0, 0, sizePx, sizePx)
        }

        val bmp = createBitmap(sizePx, sizePx)
        val canvas = Canvas(bmp)
        tv.draw(canvas)
        bmp.toDrawable(context.resources)
    } catch (_: Exception) { null }

    private fun getCachedEmojiDrawable(emoji: String, sizePx: Int): Drawable? {
        val cacheKey = "$emoji:$sizePx"
        flagDrawableCache[cacheKey]?.let { return it }
        val drawable = getEmojiDrawable(context, emoji, sizePx) ?: return null
        flagDrawableCache[cacheKey] = drawable
        return drawable
    }

    companion object {
        const val VIEW_TYPE_RECENT_TEXT = 0
        const val VIEW_TYPE_CONTACT = 1
        const val VIEW_TYPE_SECTION = 2
        private const val KEY_HIDDEN_RECENT_CONTACTS = "hidden_recent_contacts"
    }

    private sealed class DisplayRow {
        object RecentText : DisplayRow()
        data class Section(val letter: String) : DisplayRow()
        data class Contact(val contactIndex: Int) : DisplayRow()
    }

}

private tailrec fun Context.findFragmentActivity(): FragmentActivity? = when (this) {
    is FragmentActivity -> this
    is ContextWrapper -> baseContext.findFragmentActivity()
    else -> null
}

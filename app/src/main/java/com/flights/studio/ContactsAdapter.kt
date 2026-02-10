package com.flights.studio

import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.content.res.Configuration
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.animation.doOnEnd
import androidx.core.content.edit
import androidx.core.graphics.ColorUtils
import androidx.core.graphics.createBitmap
import androidx.core.graphics.drawable.toDrawable
import androidx.core.net.toUri
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.DownsampleStrategy
import com.bumptech.glide.request.RequestOptions
import com.flights.studio.databinding.ItemContactBinding
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale


@Suppress("DEPRECATION")
class ContactsAdapter(

    private val contacts: MutableList<AllContact>,
    private val context: Context,
    private val onDeleteConfirmed: (AllContact, Int) -> Unit,
    private val onItemClicked: (AllContact) -> Unit,
    private val navContactCount: TextView,


) : RecyclerView.Adapter<ContactsAdapter.ContactViewHolder>() {

    data class ColorPalette(
        val mainColor: Int,       // for combined_card
        val overlayColor: Int,    // for expandableOverlay
        val buttonColor: Int,      // for the 3 action buttons
    )




    private var expandedPosition = -1
    private var filteredContacts: MutableList<AllContact> = contacts.toMutableList()



    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ContactViewHolder {
        val binding = ItemContactBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ContactViewHolder(binding).apply {
            // lock radius here so every holder is consistent
            val r = parent.resources.getDimension(R.dimen.contact_card_radius)

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
        holder: ContactViewHolder,
        position: Int,
        payloads: MutableList<Any>
    ) {
        if (payloads.isNotEmpty() && payloads.any { it == "toggle_expand" }) {
            holder.setExpandableState(position, animate = true) // only animate here
        } else {
            super.onBindViewHolder(holder, position, payloads)
        }
    }
    override fun onBindViewHolder(holder: ContactViewHolder, position: Int) {
        val contact = filteredContacts[position]
        holder.bind(contact)
    }


    init { setHasStableIds(true) }

    override fun getItemId(position: Int): Long =
        filteredContacts[position].id.hashCode().toLong()



    fun getFilteredContacts(): List<AllContact> = filteredContacts

    override fun getItemCount() = filteredContacts.size

    fun updateContact(position: Int, updatedContact: AllContact) {
        val originalIndex = contacts.indexOfFirst { it.id == filteredContacts[position].id }
        if (originalIndex != -1) {
            contacts[originalIndex] = updatedContact
            filteredContacts[position] = updatedContact
            notifyItemChanged(position)
        }
    }


    fun updateContactCount() {
        // Use filteredContacts.size so it reflects the currently displayed items
        navContactCount.text = String.format(Locale.getDefault(), "%,d", filteredContacts.size)

    }



    fun updateData(newList: List<AllContact>) {
        // Check if data has changed to avoid unnecessary UI updates
        if (filteredContacts != newList) {
            CoroutineScope(Dispatchers.IO).launch {
                // Perform DiffUtil calculations on background thread to not block UI
                val diffCallback = ContactDiffCallback(filteredContacts, newList)
                val diffResult = DiffUtil.calculateDiff(diffCallback, false)

                // Apply updates on the main thread after DiffUtil calculation
                withContext(Dispatchers.Main) {
                    // Update the filteredContacts list and apply diff to the RecyclerView adapter
                    filteredContacts.clear()
                    filteredContacts.addAll(newList)
                    diffResult.dispatchUpdatesTo(this@ContactsAdapter)
                    // Update the count after the data is updated
                    updateContactCount()
                }
            }
        }
    }

    fun resetData() {
        updateData(contacts)
    }

    fun setFilteredContacts(newList: List<AllContact>) {
        filteredContacts.clear()
        filteredContacts.addAll(newList)
        updateData(newList)
    }

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

        android.util.Log.d("TextColorDebug", "bg=${String.format("#%06X", 0xFFFFFF and backgroundColor)}, lum=$luminance, color=${if (color == Color.WHITE) "WHITE" else "BLACK"}")
        return color
    }


    fun isDarkTheme(context: Context): Boolean {
        val theme = context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
        return theme == Configuration.UI_MODE_NIGHT_YES
    }


    inner class ContactViewHolder(private val binding: ItemContactBinding) :
        RecyclerView.ViewHolder(binding.root) {


        fun bind(contact: AllContact) {
            with(binding) {
                // --- Phone text + flag on the RIGHT (ImageView: flagEnd) ---
                val phoneText = contact.phone.ifEmpty { "No Phone Available" }
                textPhone.textDirection = View.TEXT_DIRECTION_LTR
                textPhone.text = phoneText
                textName.text = contact.name.ifBlank { "(No name)" }

                textPhone.setCompoundDrawablesRelativeWithIntrinsicBounds(null, null, null, null)
                textPhone.compoundDrawablePadding = 0

// reset
                flagEnd.visibility = View.GONE
                flagEnd.setImageDrawable(null)

                if (!contact.flag.isNullOrEmpty()) {
                    flagEnd.visibility = View.VISIBLE
                    // render the emoji exactly at the ImageView's size (respect XML)
                    flagEnd.post {
                        val target = (minOf(flagEnd.width, flagEnd.height)
                            .takeIf { it > 0 } ?: textPhone.lineHeight).coerceAtLeast(1)

                        getEmojiDrawable(context, contact.flag, target)?.let { flagEnd.setImageDrawable(it) }
                    }
                }

                textEmail.text = contact.email ?: "No Email Available"
                textAddress.text = contact.address ?: "No Address Available"
                textBirthday.text = contact.birthday ?: "No Birthday Available"
                if (!contact.birthday.isNullOrBlank()) {
                    val prefs = context.getSharedPreferences("birthday_reminders", Context.MODE_PRIVATE)
                    val isReminderSet = prefs.getBoolean("${contact.id}_birthday_set", false)
                    birthdayIcon.visibility = if (isReminderSet) View.VISIBLE else View.GONE
                } else {
                    birthdayIcon.visibility = View.GONE
                }


                // Hide delete background by default
                deleteBackground.visibility = View.GONE
                deleteBackground.alpha = 0f

                if (!contact.photoUri.isNullOrBlank()) {
                    iconImage.visibility = View.VISIBLE
                    iconInitials.visibility = View.GONE

                    val rawPhoto = contact.photoUri?.trim().orEmpty()

                    val model: Any = when {
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
                                .override(600, 600)
                                .downsample(DownsampleStrategy.CENTER_INSIDE)
                                .circleCrop()
                        )
                        .into(iconImage)

                } else {
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

                collapsingTextContainer.setBackgroundColor(palette.mainColor)

                expandableContent.setBackgroundColor(
                    if (isExpanded(absoluteAdapterPosition)) getSofterColor(palette.overlayColor)

                    else palette.overlayColor
                )

                updateButtonColors(fabDelete, palette.buttonColor)
                updateButtonColors(fabUpdate, palette.buttonColor)
                updateButtonColors(fabCall, palette.buttonColor)

                val topTextColor = getTextcolorForBackground(context, palette.mainColor)
                val middleTextColor = getTextcolorForBackground(context, palette.overlayColor)

                textName.setTextColor(topTextColor)
                textPhone.setTextColor(middleTextColor)

                textEmail.setTextColor(middleTextColor)
                textAddress.setTextColor(middleTextColor)
                textBirthday.setTextColor(middleTextColor)



                // Setup the holoColors picker
                holoColorSelector.setOnClickListener {
                    showHoloColorPicker { selectedPalette -> updateContactPalette(contact, selectedPalette) }

                }


                setupColorPickerListeners(contact)




                fabUpdate.setOnClickListener { onItemClicked(contact) }
                fabCall.setOnClickListener { handleCallClick(contact) }
                fabDelete.setOnClickListener {
                    val pos = absoluteAdapterPosition
                    if (pos != RecyclerView.NO_POSITION) {
                        onDeleteConfirmed(contact, pos)
                    }
                }
                setExpandableState(absoluteAdapterPosition, animate = false) // snap to state in bind; no animation
                collapsingTextContainer.setOnClickListener {
                    handleExpandCollapseClick(absoluteAdapterPosition)
                }
                expandCollapseIcon.setOnClickListener {
                    handleExpandCollapseClick(absoluteAdapterPosition)
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

                val isDark = isDarkTheme(context)
                val fallbackBackground = if (isDark) Color.BLACK else Color.WHITE
                val fallbackTextColor = if (isDark) Color.WHITE else Color.BLACK

                binding.collapsingTextContainer.setBackgroundColor(fallbackBackground)
                binding.expandableContent.setBackgroundColor(fallbackBackground)

                // ✅ Force correct tint for buttons
                val buttonList = listOf(binding.fabCall, binding.fabUpdate, binding.fabDelete)
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
                    collapsingTextContainer.setBackgroundColor(palette.mainColor)
                    expandableContent.setBackgroundColor(palette.overlayColor)
                    updateButtonColors(fabDelete, palette.buttonColor)
                    updateButtonColors(fabUpdate, palette.buttonColor)
                    updateButtonColors(fabCall, palette.buttonColor)

                    val topTextColor = getTextcolorForBackground(context, palette.mainColor)
                    val middleTextColor = getTextcolorForBackground(context, palette.overlayColor)

                    textName.setTextColor(topTextColor)
                    textPhone.setTextColor(middleTextColor)
                    textEmail.setTextColor(middleTextColor)
                    textAddress.setTextColor(middleTextColor)
                    textBirthday.setTextColor(middleTextColor)
                }
            }

            notifyItemChanged(absoluteAdapterPosition)
        }


        fun setExpandableState(position: Int, animate: Boolean) {
            with(binding) {
                val shouldExpand = expandedPosition == position
                val isVisible = expandableContent.isVisible

                // cancel running
                expandableContent.animate().cancel()
                expandCollapseIcon.animate().cancel()

                // Arrow rotation
                val targetRot = if (shouldExpand) 0f else 180f
                if (animate && (shouldExpand != isVisible)) {
                    expandCollapseIcon.animate()
                        .rotation(targetRot)
                        .setDuration(220L)
                        .setInterpolator(androidx.interpolator.view.animation.FastOutSlowInInterpolator())
                        .start()
                } else {
                    expandCollapseIcon.rotation = targetRot
                }

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
            if (position in 0 until itemCount) {
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

        private fun savePaletteForContact(contactId: String, palette: ColorPalette) {
            val prefs = context.getSharedPreferences("contact_palettes", Context.MODE_PRIVATE)
            prefs.edit {
                putInt("${contactId}_main", palette.mainColor)
                    .putInt("${contactId}_overlay", palette.overlayColor)
                    .putInt("${contactId}_button", palette.buttonColor)
            }
        }

        private fun getPaletteForContact(contactId: String): ColorPalette {
            val prefs = context.getSharedPreferences("contact_palettes", Context.MODE_PRIVATE)

            val isDark = isDarkTheme(context)
            val fallbackButton = if (isDark) Color.BLACK else Color.WHITE

            val main = prefs.getInt("${contactId}_main", Color.TRANSPARENT)
            val overlay = prefs.getInt("${contactId}_overlay", Color.TRANSPARENT)
            val button = prefs.getInt("${contactId}_button", fallbackButton)

            return ColorPalette(main, overlay, button)
        }





        private fun showHoloColorPicker(onPaletteSelected: (ColorPalette) -> Unit) {
            val activity = context as? AppCompatActivity
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


}
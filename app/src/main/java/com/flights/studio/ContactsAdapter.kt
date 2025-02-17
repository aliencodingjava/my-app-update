package com.flights.studio

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.graphics.ColorUtils
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.flask.colorpicker.ColorPickerView
import com.flights.studio.databinding.ItemContactBinding
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.gson.Gson
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
    private val navContactCount: TextView  // New parameter for the TextView

) : RecyclerView.Adapter<ContactsAdapter.ContactViewHolder>() {

    private val holoColors: MutableMap<String, Int> = mutableMapOf(
        "None / Remove color" to Color.TRANSPARENT, // Option to remove color
        "Holo Blue (Transparent)" to Color.parseColor("#805399E5"),
        "Holo Blue (Solid)" to Color.parseColor("#FF3399E5"),
        "Holo Green (Transparent)" to Color.parseColor("#8033E599"),
        "Holo Green (Solid)" to Color.parseColor("#FF33E599"),
        "White (Transparent)" to Color.parseColor("#80FFFFFF"),
        "Black (Transparent)" to Color.parseColor("#80000000"),
        "Red (Transparent)" to Color.parseColor("#80FF0000"),
        "Red (Solid)" to Color.parseColor("#FFFF0000"),
        "Orange (Transparent)" to Color.parseColor("#80FFA500"),
        "Orange (Solid)" to Color.parseColor("#FFFFA500"),
        "Yellow (Transparent)" to Color.parseColor("#80FFFF00"),
        "Yellow (Solid)" to Color.parseColor("#FFFFFF00"),
        "Green (Transparent)" to Color.parseColor("#80008000"),
        "Green (Solid)" to Color.parseColor("#FF008000"),
        "Blue (Transparent)" to Color.parseColor("#800000FF"),
        "Blue (Solid)" to Color.parseColor("#FF0000FF"),
        "Indigo (Transparent)" to Color.parseColor("#804B0082"),
        "Indigo (Solid)" to Color.parseColor("#FF4B0082"),
        "Violet (Transparent)" to Color.parseColor("#80EE82EE"),
        "Violet (Solid)" to Color.parseColor("#FFEE82EE"),
        "Magenta (Transparent)" to Color.parseColor("#80FF00FF"),
        "Magenta (Solid)" to Color.parseColor("#FFFF00FF"),
        "Pink (Transparent)" to Color.parseColor("#80FFC0CB"),
        "Pink (Solid)" to Color.parseColor("#FFFFC0CB"),
        "Cyan (Transparent)" to Color.parseColor("#8000FFFF"),
        "Cyan (Solid)" to Color.parseColor("#FF00FFFF"),
        "Aqua (Transparent)" to Color.parseColor("#8000FFFF"),
        "Aqua (Solid)" to Color.parseColor("#FF00FFFF"),
        "Teal (Transparent)" to Color.parseColor("#80808080"),
        "Teal (Solid)" to Color.parseColor("#FF808080"),
        "Grey (Transparent)" to Color.parseColor("#80808080"),
        "Grey (Solid)" to Color.parseColor("#FF808080"),
        "Brown (Transparent)" to Color.parseColor("#80A52A2A"),
        "Brown (Solid)" to Color.parseColor("#FFA52A2A"),
        "Maroon (Transparent)" to Color.parseColor("#80800000"),
        "Maroon (Solid)" to Color.parseColor("#FF800000"),
        "Olive (Transparent)" to Color.parseColor("#80808000"),
        "Olive (Solid)" to Color.parseColor("#FF808000"),
        "Lime (Transparent)" to Color.parseColor("#8080FF00"),
        "Lime (Solid)" to Color.parseColor("#FF80FF00"),
        "Chocolate (Transparent)" to Color.parseColor("#80D2691E"),
        "Chocolate (Solid)" to Color.parseColor("#FFD2691E"),
        "Coral (Transparent)" to Color.parseColor("#80FF7F50"),
        "Coral (Solid)" to Color.parseColor("#FFFF7F50"),
        "Salmon (Transparent)" to Color.parseColor("#80FA8072"),
        "Salmon (Solid)" to Color.parseColor("#FFFA8072"),
        "Gold (Transparent)" to Color.parseColor("#80FFD700"),
        "Gold (Solid)" to Color.parseColor("#FFFFD700"),
        "Plum (Transparent)" to Color.parseColor("#80DDA0DD"),
        "Plum (Solid)" to Color.parseColor("#FFDDA0DD"),
        "Orchid (Transparent)" to Color.parseColor("#80DA70D6"),
        "Orchid (Solid)" to Color.parseColor("#FFDA70D6"),
        "Turquoise (Transparent)" to Color.parseColor("#8040E0D0"),
        "Turquoise (Solid)" to Color.parseColor("#FF40E0D0"),
        "Sky Blue (Transparent)" to Color.parseColor("#8087CEEB"),
        "Sky Blue (Solid)" to Color.parseColor("#FF87CEEB"),
        "Crimson (Transparent)" to Color.parseColor("#80DC143C"),
        "Crimson (Solid)" to Color.parseColor("#FFDC143C"),
        "Emerald (Transparent)" to Color.parseColor("#8050C878"),
        "Emerald (Solid)" to Color.parseColor("#FF50C878"),
        "Lavender (Transparent)" to Color.parseColor("#80E6E6FA"),
        "Lavender (Solid)" to Color.parseColor("#FFE6E6FA"),
        "Sunset Orange (Transparent)" to Color.parseColor("#80FD5E53"),
        "Sunset Orange (Solid)" to Color.parseColor("#FFFD5E53"),
        "Royal Purple (Transparent)" to Color.parseColor("#807845A8"),
        "Royal Purple (Solid)" to Color.parseColor("#FF7845A8"),
        "Charcoal (Transparent)" to Color.parseColor("#80808080"),
        "Charcoal (Solid)" to Color.parseColor("#FF808080"),
        "Midnight Blue (Transparent)" to Color.parseColor("#80191970"),
        "Midnight Blue (Solid)" to Color.parseColor("#FF191970"),
        "Beige (Transparent)" to Color.parseColor("#80F5F5DC"),
        "Beige (Solid)" to Color.parseColor("#FFF5F5DC"),
        "Sienna (Transparent)" to Color.parseColor("#80A0522D"),
        "Sienna (Solid)" to Color.parseColor("#FFA0522D"),
        "Pale Pink (Transparent)" to Color.parseColor("#80FFD1DC"),
        "Pale Pink (Solid)" to Color.parseColor("#FFFFD1DC"),
        "Forest Green (Transparent)" to Color.parseColor("#80228B22"),
        "Forest Green (Solid)" to Color.parseColor("#FF228B22"),
        "Ice Blue (Transparent)" to Color.parseColor("#8080CED1"),
        "Ice Blue (Solid)" to Color.parseColor("#FF80CED1"),
        "Mint Green (Transparent)" to Color.parseColor("#8098FB98"),
        "Mint Green (Solid)" to Color.parseColor("#FF98FB98"),
        "Mustard (Transparent)" to Color.parseColor("#80FFDB58"),
        "Mustard (Solid)" to Color.parseColor("#FFFFDB58"),
        "Holo Purple (Transparent)" to Color.parseColor("#807A00E6"),
        "Holo Purple (Solid)" to Color.parseColor("#FF7A00E6"),
        "Holo Cyan (Transparent)" to Color.parseColor("#8000E6E6"),
        "Holo Cyan (Solid)" to Color.parseColor("#FF00E6E6"),
        "Holo Amber (Transparent)" to Color.parseColor("#80FFBF00"),
        "Holo Amber (Solid)" to Color.parseColor("#FFFFBF00"),
        "Holo Lime (Transparent)" to Color.parseColor("#8080FF00"),
        "Holo Lime (Solid)" to Color.parseColor("#FF80FF00"),
        "Holo Rose (Transparent)" to Color.parseColor("#80FF007F"),
        "Holo Rose (Solid)" to Color.parseColor("#FFFF007F"),
        "Holo Bronze (Transparent)" to Color.parseColor("#806B4423"),
        "Holo Bronze (Solid)" to Color.parseColor("#FF6B4423"),
        "Holo Silver (Transparent)" to Color.parseColor("#80C0C0C0"),
        "Holo Silver (Solid)" to Color.parseColor("#FFC0C0C0"),
        "Holo Periwinkle (Transparent)" to Color.parseColor("#808C82E6"),
        "Holo Periwinkle (Solid)" to Color.parseColor("#FF8C82E6"),
        "Holo Peach (Transparent)" to Color.parseColor("#80FFDAB9"),
        "Holo Peach (Solid)" to Color.parseColor("#FFFFDAB9"),
        "Holo Mint (Transparent)" to Color.parseColor("#8078C2C0"),
        "Holo Mint (Solid)" to Color.parseColor("#FF78C2C0"),
        "Holo Sapphire (Transparent)" to Color.parseColor("#80395BC6"),
        "Holo Sapphire (Solid)" to Color.parseColor("#FF395BC6"),
        "Holo Ruby (Transparent)" to Color.parseColor("#80E0115F"),
        "Holo Ruby (Solid)" to Color.parseColor("#FFE0115F"),
        "Holo Graphite (Transparent)" to Color.parseColor("#80404040"),
        "Holo Graphite (Solid)" to Color.parseColor("#FF404040"),
        "Holo Jade (Transparent)" to Color.parseColor("#8040E68C"),
        "Holo Jade (Solid)" to Color.parseColor("#FF40E68C"),
        "Neon Yellow (Transparent)" to Color.parseColor("#80FFFF33"),
        "Neon Yellow (Solid)" to Color.parseColor("#FFFFFF33"),
        "Neon Green (Transparent)" to Color.parseColor("#8026FF33"),
        "Neon Green (Solid)" to Color.parseColor("#FF26FF33"),
        "Neon Pink (Transparent)" to Color.parseColor("#80FF33FF"),
        "Neon Pink (Solid)" to Color.parseColor("#FFFF33FF"),
        "Neon Orange (Transparent)" to Color.parseColor("#80FF7F24"),
        "Neon Orange (Solid)" to Color.parseColor("#FFFF7F24"),
        "Neon Blue (Transparent)" to Color.parseColor("#802482FF"),
        "Neon Blue (Solid)" to Color.parseColor("#FF2482FF"),
        "Soft Lavender (Transparent)" to Color.parseColor("#80E6E6FA"),
        "Soft Lavender (Solid)" to Color.parseColor("#FFE6E6FA"),
        "Soft Peach (Transparent)" to Color.parseColor("#80FFDAB9"),
        "Soft Peach (Solid)" to Color.parseColor("#FFFFDAB9"),
        "Soft Mint (Transparent)" to Color.parseColor("#8098FB98"),
        "Soft Mint (Solid)" to Color.parseColor("#FF98FB98"),
        "Deep Burgundy (Transparent)" to Color.parseColor("#80512020"),
        "Deep Burgundy (Solid)" to Color.parseColor("#FF512020"),
        "Deep Navy (Transparent)" to Color.parseColor("#8015154A"),
        "Deep Navy (Solid)" to Color.parseColor("#FF15154A"),
        "Warm Sand (Transparent)" to Color.parseColor("#80E4C590"),
        "Warm Sand (Solid)" to Color.parseColor("#FFE4C590"),
        "Rust Red (Transparent)" to Color.parseColor("#80B7410E"),
        "Rust Red (Solid)" to Color.parseColor("#FFB7410E"),
        "Ash Grey (Transparent)" to Color.parseColor("#80B2BEB5"),
        "Ash Grey (Solid)" to Color.parseColor("#FFB2BEB5"),
        "Royal Teal (Transparent)" to Color.parseColor("#80488FB1"),
        "Royal Teal (Solid)" to Color.parseColor("#FF488FB1"),
        "Soft Blush (Transparent)" to Color.parseColor("#80F4C2C2"),
        "Soft Blush (Solid)" to Color.parseColor("#FFF4C2C2")

    ).apply {
        repeat(20) { index ->
            this["Empty color $index"] = Color.TRANSPARENT // ✅ Unique names
        }
    }


    private var expandedPosition = -1
    private var filteredContacts: MutableList<AllContact> = contacts.toMutableList()



    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ContactViewHolder {
        val binding = ItemContactBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ContactViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ContactViewHolder, position: Int) {
        val contact = filteredContacts[position]
        holder.bind(contact)

    }


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

    fun updateContactPhoto(position: Int, uri: Uri) {
        if (position !in filteredContacts.indices) return

        val contactId = filteredContacts[position].id
        val originalIndex = contacts.indexOfFirst { it.id == contactId }

        if (originalIndex != -1) {
            val updatedContact = contacts[originalIndex].copy(photoUri = uri.toString())
            contacts[originalIndex] = updatedContact

            // 🔥 Update filteredContacts immediately
            filteredContacts[position] = updatedContact

            // ✅ Notify UI
            notifyItemChanged(position)


            // ✅ Save immediately
            saveContactsToSharedPreferences()
        }
    }

    fun updateContactCount() {
        // Use filteredContacts.size so it reflects the currently displayed items
        navContactCount.text = String.format(Locale.getDefault(), "%,d", filteredContacts.size)

    }



    /**
     * ✅ This function saves the updated contact list to SharedPreferences.
     */
    private fun saveContactsToSharedPreferences() {
        val sharedPreferences = context.getSharedPreferences("contacts_data", Context.MODE_PRIVATE)
        val json = Gson().toJson(contacts)
        sharedPreferences.edit().putString("contacts", json).apply()
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
        private val newList: List<AllContact>
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
            // If no color is selected, decide based on the theme
            return if (isDarkTheme(context)) Color.WHITE else Color.BLACK
        } else {
            // Calculate the luminance of the background color
            val luminance = (0.299 * Color.red(backgroundColor) + 0.587 * Color.green(backgroundColor) + 0.114 * Color.blue(backgroundColor)) / 255
            return if (luminance < 0.5) Color.WHITE else Color.BLACK // Lighter text for dark backgrounds and vice versa
        }
    }


    fun isDarkTheme(context: Context): Boolean {
        val theme = context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
        return theme == Configuration.UI_MODE_NIGHT_YES
    }


    inner class ContactViewHolder(private val binding: ItemContactBinding) :
        RecyclerView.ViewHolder(binding.root) {


        fun bind(contact: AllContact) {
            with(binding) {
                textName.text = contact.name.ifEmpty { "Unknown Name" }
                textPhone.text = contact.phone.ifEmpty { "No Phone Available" }
                textEmail.text = contact.email ?: "No Email Available"
                textAddress.text = contact.address ?: "No Address Available"

                // Hide delete background by default
                deleteBackground.visibility = View.GONE
                deleteBackground.alpha = 0f

                if (!contact.photoUri.isNullOrEmpty()) {
                    iconImage.visibility = View.VISIBLE
                    iconInitials.visibility = View.GONE
                    Glide.with(context)
                        .load(contact.photoUri)
                        .diskCacheStrategy(DiskCacheStrategy.NONE)
                        .skipMemoryCache(true)
                        .signature(com.bumptech.glide.signature.ObjectKey(System.currentTimeMillis()))
                        .circleCrop()
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
                val savedColor = getColorForContact(contact.id)
                collapsingTextContainer.setBackgroundColor(savedColor)


                val textColor = getTextcolorForBackground(context, savedColor)
                textName.setTextColor(textColor)
                textPhone.setTextColor(textColor)
                textEmail.setTextColor(textColor)
                textAddress.setTextColor(textColor)
                // Set text color for the buttons based on the saved color
                updateButtonColors(fabDelete, textColor, savedColor)
                updateButtonColors(fabUpdate, textColor, savedColor)
                updateButtonColors(fabCall, textColor, savedColor)

                // Calculate modified background color for expandable content
                val expandedColor = getSofterColor(savedColor, 0.85f) // Slightly faded
                val collapsedColor = savedColor // Keep original color

                // Apply colors based on expansion state
                expandableContent.setBackgroundColor(if (isExpanded(absoluteAdapterPosition)) expandedColor else collapsedColor)

                // Setup the holoColors picker
                holoColorSelector.setOnClickListener {
                    showHoloColorPicker { selectedColor ->
                        updateContactColor(contact, selectedColor)
                    }
                }
                // Setup the color selector click listener
                colorSelector.setOnClickListener {
                    showColorPickerDialog(contact) { selectedColor ->  // Pass the contact here
                        collapsingTextContainer.setBackgroundColor(selectedColor)
                        saveColorForContact(contact.id, selectedColor)

                        // Update text colors based on the new background color
                        val newTextColor = getTextcolorForBackground(context, selectedColor)
                        textName.setTextColor(newTextColor)
                        textPhone.setTextColor(newTextColor)
                        textEmail.setTextColor(newTextColor)
                        textAddress.setTextColor(newTextColor)
                    }
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
                setExpandableState(absoluteAdapterPosition)
                collapsingTextContainer.setOnClickListener {
                    handleExpandCollapseClick(absoluteAdapterPosition)
                }
                expandCollapseIcon.setOnClickListener {
                    handleExpandCollapseClick(absoluteAdapterPosition)
                }
            }
        }

        private fun getSofterColor(color: Int, factor: Float): Int {
            val alpha = (Color.alpha(color) * factor).toInt()
            val red = (Color.red(color) * factor).toInt()
            val green = (Color.green(color) * factor).toInt()
            val blue = (Color.blue(color) * factor).toInt()
            return Color.argb(alpha, red, green, blue)
        }


        private fun updateButtonColors(button: MaterialButton, textColor: Int, backgroundColor: Int) {
            button.setTextColor(textColor)
            button.setBackgroundColor(getSofterColor(backgroundColor, 0.9f)) // Slightly lighter background
        }


        private fun setupColorPickerListeners(contact: AllContact) {
            binding.holoColorSelector.setOnClickListener {
                showHoloColorPicker { selectedColor ->
                    updateContactColor(contact, selectedColor)
                }
            }
            binding.colorSelector.setOnClickListener {
                showColorPickerDialog(contact) { selectedColor ->  // Pass the contact here
                    updateContactColor(contact, selectedColor)
                }
            }
        }

//        fun showDeleteBackground() {
//            binding.deleteBackground.apply {
//                visibility = View.VISIBLE
//                alpha = 0f  // reset alpha
//                bringToFront() // ensure it appears above other views
//                animate().alpha(1f).setDuration(200).start()
//            }
//        }


        private fun setExpandableState(position: Int) {
            with(binding) {
                val isExpanded = expandedPosition == position

                // Ensure expandable content is visible when expanded
                expandableContent.visibility = if (isExpanded) View.VISIBLE else View.GONE
                expandCollapseIcon.rotation = if (isExpanded) 0f else 180f

                // Change icon color based on expand state
                val expandedColor = Color.YELLOW
                val collapsedColor = resolveThemeColor(expandCollapseIcon.context, com.google.android.material.R.attr.colorPrimary)
                expandCollapseIcon.setColorFilter(if (isExpanded) expandedColor else collapsedColor)

                // Retrieve the saved color or default theme color
                val savedColor = getColorForContact(filteredContacts[position].id)
                val defaultDarkColor = Color.parseColor("#1C1D23") // Dark theme default
                val defaultLightColor = Color.parseColor("#60FFFFFF") // Light theme default
                val themeDefaultColor = if (isDarkTheme(context)) defaultDarkColor else defaultLightColor
                val baseColor = if (savedColor != Color.TRANSPARENT) savedColor else themeDefaultColor

                if (isExpanded) {
                    expandableContent.background = createInfiniteSlidingGlow(expandableContent.context, baseColor)
                } else {
                    expandableContent.setBackgroundColor(ColorUtils.setAlphaComponent(baseColor, 77))
                }





                combinedCard.setCardBackgroundColor(
                    context.getColor(if (isExpanded) R.color.bottom_bar else R.color.box_alert_update)
                )

                val layoutParams = combinedCard.layoutParams as ViewGroup.MarginLayoutParams
                layoutParams.topMargin = if (isExpanded) 0 else 2
                layoutParams.bottomMargin = if (isExpanded) (3 * context.resources.displayMetrics.density).toInt() else 3
                combinedCard.layoutParams = layoutParams

                combinedCard.radius = if (isExpanded) (10 * context.resources.displayMetrics.density) else 35f
            }
        }

        private fun resolveThemeColor(context: Context, attr: Int): Int {
            val typedValue = TypedValue()
            val theme = context.theme
            theme.resolveAttribute(attr, typedValue, true)
            return typedValue.data
        }





        private fun handleExpandCollapseClick(position: Int) {
            val previousPosition = expandedPosition
            expandedPosition = if (expandedPosition == position) -1 else position
            notifyItemChanged(previousPosition)
            notifyItemChanged(position)
        }

        private fun handleCallClick(contact: AllContact) {
            if (contact.phone.isNotEmpty()) {
                val dialIntent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${contact.phone}"))
                context.startActivity(dialIntent)
            } else {
                Toast.makeText(context, "Invalid phone number", Toast.LENGTH_SHORT).show()
            }
        }

        fun showColorPickerDialog(contact: AllContact, onColorSelected: (Int) -> Unit) {
            val activity = context as? Activity
            val parentView = activity?.findViewById<ViewGroup>(android.R.id.content)
            val view = LayoutInflater.from(context).inflate(R.layout.custom_color_picker, parentView, false)

            val colorPickerView = view.findViewById<ColorPickerView>(R.id.color_picker_view)
            val colorPreview = view.findViewById<MaterialCardView>(R.id.color_preview)
            val toolbar = view.findViewById<Toolbar>(R.id.toolbar)
            val colorNameText = view.findViewById<TextView>(R.id.color_name_text)

            // Set the initial color of the color picker and the color preview
            val savedColor = getColorForContact(contact.id)
            colorPickerView.setColor(savedColor, true)
            colorPreview.setCardBackgroundColor(savedColor)
            colorNameText.text = getColorDescriptionWithTransparency(savedColor)
            colorNameText.setTextColor(getTextcolorForBackground(context, savedColor))


            val lightnessSlider = view.findViewById<com.flask.colorpicker.slider.LightnessSlider>(R.id.v_lightness_slider)
            val alphaSlider = view.findViewById<com.flask.colorpicker.slider.AlphaSlider>(R.id.v_alpha_slider)

            colorPickerView.setLightnessSlider(lightnessSlider)
            colorPickerView.setAlphaSlider(alphaSlider)

            colorPickerView.addOnColorChangedListener { color ->
                // Update the preview color dynamically
                colorPreview.setCardBackgroundColor(color)
                val alphaColor = Color.argb((Color.alpha(color) * 0.3).toInt(), Color.red(color), Color.green(color), Color.blue(color))
                toolbar.setBackgroundColor(alphaColor)

                val colorName = getColorDescriptionWithTransparency(color)
                colorNameText.text = colorName
                val textColor = getTextcolorForBackground(context, alphaColor)
                colorNameText.setTextColor(textColor)

                updateButtonColors(binding.fabDelete, textColor, color)
                updateButtonColors(binding.fabUpdate, textColor, color)
                updateButtonColors(binding.fabCall, textColor, color)
            }

            val bottomSheetDialog = BottomSheetDialog(context, R.style.CustomBottomSheetDialog)
            bottomSheetDialog.setContentView(view)
            val bottomSheet = bottomSheetDialog.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
            bottomSheet?.setBackgroundResource(R.drawable.bottom_sheet_rounded)

            val behavior = BottomSheetBehavior.from(bottomSheet as View)
            behavior.isDraggable = false
            behavior.state = BottomSheetBehavior.STATE_EXPANDED
            bottomSheetDialog.setCancelable(false)

            toolbar.setNavigationOnClickListener {
                bottomSheetDialog.dismiss()
            }

            view.findViewById<Button>(R.id.btn_save).setOnClickListener {
                onColorSelected(colorPickerView.selectedColor)
                bottomSheetDialog.dismiss()
            }
            view.findViewById<Button>(R.id.btn_cancel).setOnClickListener {
                bottomSheetDialog.dismiss()
            }

            bottomSheetDialog.show()
        }

        fun getColorDescriptionWithTransparency(color: Int): String {
            val alpha = Color.alpha(color)
            val red = Color.red(color)
            val green = Color.green(color)
            val blue = Color.blue(color)

            val hsl = FloatArray(3)
            ColorUtils.RGBToHSL(red, green, blue, hsl)

            val transparencyDescription = getTransparencyDescription(alpha)
            val lightnessDescription = getLightnessDescription(hsl[2]) // hsl[2] is the lightness
            val colorName = getBaseColorNameFromHSL(hsl)

            return "$colorName, $lightnessDescription, $transparencyDescription"
        }

        fun getTransparencyDescription(alpha: Int): String = when {
            alpha >= 255 -> "opaque"
            alpha >= 200 -> "mostly opaque"
            alpha >= 150 -> "semi-transparent"
            alpha >= 100 -> "translucent"
            else -> "transparent"
        }

        fun getLightnessDescription(lightness: Float): String = when {
            lightness < 0.2 -> "dark"
            lightness in 0.2..0.4 -> "dim"
            lightness in 0.4..0.6 -> "normal"
            lightness in 0.6..0.8 -> "light"
            else -> "bright"
        }

        fun getBaseColorNameFromHSL(hsl: FloatArray): String {
            val (hue, saturation, lightness) = hsl
            return when {
                lightness < 0.2 -> "Black"
                lightness > 0.8 -> "White"
                saturation < 0.1 && lightness > 0.9 -> "White" // Very low saturation whites
                hue in 0f..30f || hue in 330f..360f -> "Red"
                hue in 30f..90f -> "Orange"
                hue in 90f..150f -> "Yellow"
                hue in 150f..210f -> "Green"
                hue in 210f..270f -> "Blue"
                hue in 270f..330f -> "Purple"
                else -> "Unknown Color"
            }
        }

        private fun saveColorForContact(contactId: String, color: Int) {
            val editor = context.getSharedPreferences("contact_colors", Context.MODE_PRIVATE).edit()
            editor.putInt(contactId, color)
            editor.apply()
        }

        private fun getColorForContact(contactId: String): Int {
            return context.getSharedPreferences("contact_colors", Context.MODE_PRIVATE)
                .getInt(contactId, Color.TRANSPARENT)  // Default to no color if none saved
        }

        private fun updateContactColor(contact: AllContact, color: Int) {
            saveColorForContact(contact.id, color)

            val newTextColor = getTextcolorForBackground(context, color)
            binding.apply {
                collapsingTextContainer.setBackgroundColor(color)
                textName.setTextColor(newTextColor)
                textPhone.setTextColor(newTextColor)
                textEmail.setTextColor(newTextColor)
                textAddress.setTextColor(newTextColor)
            }
            updateColors(color, isExpanded(absoluteAdapterPosition))

            notifyItemChanged(absoluteAdapterPosition)
        }

        private fun updateColors(color: Int, isExpanded: Boolean) {
            val alphaColor = Color.argb(77, Color.red(color), Color.green(color), Color.blue(color)) // 30% opacity
            binding.collapsingTextContainer.setBackgroundColor(color)
            binding.expandableContent.setBackgroundColor(if (isExpanded) alphaColor else Color.TRANSPARENT)

            val textColor = getTextcolorForBackground(context, color)
            binding.textName.setTextColor(textColor)
            binding.textPhone.setTextColor(textColor)
            binding.textEmail.setTextColor(textColor)
            binding.textAddress.setTextColor(textColor)
        }


        private fun showHoloColorPicker(onColorSelected: (Int) -> Unit) {
            val activity = context as? AppCompatActivity
            activity?.supportFragmentManager?.let { fm ->
                val holoPicker = HoloColorPickerBottomSheet(holoColors, onColorSelected)
                holoPicker.show(fm, "HoloColorPicker")
            }
        }


    }

 }
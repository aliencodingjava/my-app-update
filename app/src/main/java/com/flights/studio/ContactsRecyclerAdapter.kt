package com.flights.studio

import android.content.Context
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.flights.studio.databinding.ItemContact2Binding

/**
 * RecyclerView adapter for contacts - optimized for smooth native scrolling.
 * Key advantages:
 * - Native view recycling (automatic, very fast)
 * - No recomposition - just show/hide views
 * - Efficient memory usage (only visible items + buffer)
 * - True 120 Hz smooth scrolling
 */
class ContactsRecyclerAdapter(
    private val context: Context,
    private val onEditContact: (AllContact) -> Unit,
    private val onDeleteContact: (AllContact) -> Unit,
    private val onCallContact: (AllContact) -> Unit,
    private val onPaletteClick: (AllContact) -> Unit
) : RecyclerView.Adapter<ContactsRecyclerAdapter.ContactViewHolder>() {

    private val contacts = mutableListOf<AllContact>()
    private val colorPalettes = mutableMapOf<String, ContactsAdapter.ColorPalette>()

    inner class ContactViewHolder(private val binding: ItemContact2Binding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(contact: AllContact, palette: ContactsAdapter.ColorPalette?) {
            val isDark = context.resources.configuration.uiMode and
                android.content.res.Configuration.UI_MODE_NIGHT_MASK ==
                android.content.res.Configuration.UI_MODE_NIGHT_YES

            val headerDefault = if (isDark) Color.parseColor("#1D1726") else Color.WHITE
            val contentDefault = if (isDark) Color.parseColor("#1D1726") else Color.WHITE
            val buttonDefault = if (isDark) Color.parseColor("#2B2D34") else Color.WHITE

            val headerColor = if (palette?.mainColor != Color.TRANSPARENT && palette?.mainColor != 0) {
                palette?.mainColor ?: headerDefault
            } else {
                headerDefault
            }
            val contentColor = if (palette?.overlayColor != Color.TRANSPARENT && palette?.overlayColor != 0) {
                palette?.overlayColor ?: contentDefault
            } else {
                contentDefault
            }
            val buttonColor = if (palette?.buttonColor != Color.TRANSPARENT && palette?.buttonColor != 0) {
                palette?.buttonColor ?: buttonDefault
            } else {
                buttonDefault
            }

            // Set header background
            binding.collapsingTextContainer.setBackgroundColor(headerColor)

            // Set up avatar
            loadAvatar(binding.iconImage, contact, Color.parseColor("#FF6347"))

            // Set contact name
            binding.textName.text = contact.name.ifBlank { "(No name)" }
            binding.textName.setTextColor(getReadableTextColor(headerColor))

            // Set expandable content background
            binding.expandableContent.setBackgroundColor(contentColor)

            // Set contact details
            binding.textPhone.text = contact.phone.takeIf { it.isNotBlank() } ?: "No phone"
            binding.textPhone.setTextColor(getReadableTextColor(contentColor))

            binding.textEmail.text = contact.email?.takeIf { it.isNotBlank() } ?: "No email"
            binding.textEmail.setTextColor(getReadableTextColor(contentColor))

            // Expand/collapse functionality
            var isExpanded = false
            binding.expandCollapseIcon.rotation = if (isExpanded) 180f else 0f

            binding.collapsingTextContainer.setOnClickListener {
                isExpanded = !isExpanded
                binding.expandableContent.visibility = if (isExpanded) View.VISIBLE else View.GONE
                binding.expandCollapseIcon.animate().rotation(if (isExpanded) 180f else 0f).setDuration(300).start()
            }

            // Set action buttons
            binding.smsButton.setOnClickListener { onCallContact(contact) }
            binding.buttonShare.setOnClickListener { onEditContact(contact) }
            binding.mapButton.setOnClickListener { onPaletteClick(contact) }
        }

        private fun loadAvatar(imageView: ImageView, contact: AllContact, fallbackColor: Int) {
            val photoUri = contact.photoUri?.trim().orEmpty()
            if (photoUri.isBlank()) {
                // Show initials
                imageView.setBackgroundColor(fallbackColor)
                imageView.setImageDrawable(null)
            } else {
                // Load image with Glide for efficient caching
                Glide.with(context)
                    .load(photoUri)
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .centerCrop()
                    .into(imageView)
            }
        }

        private fun getReadableTextColor(backgroundColor: Int): Int {
            val r = Color.red(backgroundColor)
            val g = Color.green(backgroundColor)
            val b = Color.blue(backgroundColor)
            val brightness = (r * 299 + g * 587 + b * 114) / 1000
            return if (brightness > 128) Color.BLACK else Color.WHITE
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ContactViewHolder {
        val binding = ItemContact2Binding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ContactViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ContactViewHolder, position: Int) {
        if (position < contacts.size) {
            val contact = contacts[position]
            val palette = colorPalettes[contact.id]
            holder.bind(contact, palette)
        }
    }

    override fun getItemCount(): Int = contacts.size

    fun updateContacts(newContacts: List<AllContact>) {
        // Key: only rebuild when absolutely necessary.
        if (contacts.isEmpty()) {
            contacts.addAll(newContacts)
            notifyDataSetChanged()
        } else if (newContacts.isEmpty()) {
            val oldSize = contacts.size
            contacts.clear()
            notifyItemRangeRemoved(0, oldSize)
        } else {
            // Preserve scroll position - update in place
            contacts.clear()
            contacts.addAll(newContacts)
            notifyDataSetChanged()
        }
    }

    fun updatePalettes(palettes: Map<String, ContactsAdapter.ColorPalette>) {
        this.colorPalettes.clear()
        this.colorPalettes.putAll(palettes)
        notifyDataSetChanged()
    }

    fun getContacts(): List<AllContact> = contacts.toList()
}

package com.flights.studio

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.flights.studio.databinding.ItemContact2Binding

class ContactUsAdapter(
    private val contacts: List<AllContactus>,
    private val context: Context,
    private val onMapClick: (latitude: Double, longitude: Double, label: String) -> Unit

) : RecyclerView.Adapter<ContactUsAdapter.ContactViewHolder>() {

    // A list to track expanded state for each item
    private val expandedStates = MutableList(contacts.size) { false }

    inner class ContactViewHolder(private val binding: ItemContact2Binding) :
        RecyclerView.ViewHolder(binding.root) {

        @Suppress("DEPRECATION")
        fun bind(contact: AllContactus, isExpanded: Boolean) {
            // Safely set text with fallback values
            binding.textName.text = contact.name.takeIf { it.isNotBlank() } ?: "Unknown Name"
            binding.textPhone.text =
                contact.phone.takeIf { it.isNotBlank() } ?: "No Phone Available"
            binding.textEmail.text =
                contact.email.takeIf { !it.isNullOrBlank() } ?: "No Email Available"
            binding.iconImage.setImageResource(contact.iconResId)

            // Set expandable content visibility
            binding.expandableContent.visibility = if (isExpanded) View.VISIBLE else View.GONE

            // Update expand/collapse icon rotation
            binding.expandCollapseIcon.rotation = if (isExpanded) 180f else 0f

            // Adjust margins and radius based on expanded state
            val layoutParams = binding.combinedCard.layoutParams as ViewGroup.MarginLayoutParams
            layoutParams.topMargin = if (isExpanded) {
                (5 * context.resources.displayMetrics.density).toInt() // 5dp gap on top
            } else {
                0 // No top gap when collapsed
            }
            layoutParams.bottomMargin = if (isExpanded) {
                (9 * context.resources.displayMetrics.density).toInt() // 10dp gap on bottom
            } else {
                (8 * context.resources.displayMetrics.density).toInt() // Default gap when collapsed
            }
            binding.combinedCard.layoutParams = layoutParams

            // Adjust card corner radius
            binding.combinedCard.radius = if (isExpanded) {
                (15 * context.resources.displayMetrics.density) // 20dp radius when expanded
            } else {
                20f // 20dp radius when collapsed
            }


            // Set up expand/collapse behavior
            binding.collapsingTextContainer.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    // Toggle the state for the clicked item
                    expandedStates[position] = !expandedStates[position]
                    notifyItemChanged(position)
                }
            }


            // Handle Email action
            binding.textEmail.setOnClickListener {
                if (!contact.email.isNullOrBlank()) {
                    val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:${contact.email}"))
                    context.startActivity(Intent.createChooser(intent, "Send Email"))
                } else {
                    Toast.makeText(context, "No email address available", Toast.LENGTH_SHORT).show()
                }
            }

            // Handle Phone TextView click action
            binding.textPhone.setOnClickListener {
                if (contact.phone.isNotBlank()) {
                    val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${contact.phone}"))
                    context.startActivity(intent)
                } else {
                    Toast.makeText(context, "No phone number available", Toast.LENGTH_SHORT).show()
                }
            }

            // Handle SMS Button click action
            binding.smsButton.setOnClickListener {
                if (contact.phone.isNotBlank()) {
                    val smsIntent = Intent(Intent.ACTION_VIEW).apply {
                        data = Uri.parse("smsto:${contact.phone}")
                        putExtra("sms_body", "Hello, this is a test message.")
                    }
                    context.startActivity(smsIntent)
                } else {
                    Toast.makeText(context, "No phone number available for SMS", Toast.LENGTH_SHORT)
                        .show()
                }
            }


            // Map button click logic
            binding.mapButton.setOnClickListener {
                if (contact.latitude != null && contact.longitude != null) {
                    onMapClick(contact.latitude, contact.longitude, contact.name)
                } else {
                    Toast.makeText(context, "Coordinates unavailable for ${contact.name}", Toast.LENGTH_SHORT).show()
                }
            }

            // Handle Share button click
            binding.buttonShare.setOnClickListener {
                val shareContent = """
            Name: ${contact.name}
            Phone: ${contact.phone}
            Email: ${contact.email}
            Address: ${contact.address}
        """.trimIndent()

                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, shareContent)
                }
                context.startActivity(Intent.createChooser(shareIntent, "Share Contact"))
            }
        }

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ContactViewHolder {
        val binding =
            ItemContact2Binding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ContactViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ContactViewHolder, position: Int) {
        holder.bind(contacts[position], expandedStates[position])
    }

    override fun getItemCount(): Int = contacts.size

    companion object {
        fun getContactUsDetails(context: Context): List<AllContactus> {
            val defaultColor = context.getColor(R.color.primary_text)
            return listOf(
                AllContactus(
                    name = "Airport Operations",
                    email = "operations@jhairport.org",
                    phone = "307-733-7682",
                    address = "Jackson Hole Airport, WY",
                    latitude = 43.603207,
                    longitude = -110.736018,
                    id = "1",
                    color = defaultColor,
                    photoUri = null,
                    iconResId = R.drawable.ic_settings_black_24dp
                ),
                AllContactus(
                    name = "Airport Services",
                    email = "info@jhairport.org",
                    phone = "307-733-7682",
                    address = "Jackson Hole Airport, WY",
                    latitude = 43.603207,
                    longitude = -110.736018,
                    id = "2",
                    color = defaultColor,
                    photoUri = null,
                    iconResId = R.drawable.baseline_info_24
                ),
                AllContactus(
                    name = "Human Resources",
                    email = "hr@jhairport.org",
                    phone = "307-733-7682",
                    address = "Jackson Hole Airport, WY",
                    latitude = 43.603207,
                    longitude = -110.736018,
                    id = "3",
                    color = defaultColor,
                    photoUri = null,
                    iconResId = R.drawable.baseline_email_24
                ),
                AllContactus(
                    name = "Lost and Found",
                    email = "info@jhairport.org",
                    phone = "307-733-7682",
                    address = "Jackson Hole Airport, WY",
                    latitude = 43.603207,
                    longitude = -110.736018,
                    id = "4",
                    color = defaultColor,
                    photoUri = null,
                    iconResId = R.drawable.baseline_no_luggage_24
                ),
                AllContactus(
                    name = "Communications",
                    email = "megan.jenkins@jhairport.org",
                    phone = "307-733-7682",
                    address = "Jackson Hole Airport, WY",
                    latitude = 43.603207,
                    longitude = -110.736018,
                    id = "5",
                    color = defaultColor,
                    photoUri = null,
                    iconResId = R.drawable.baseline_local_phone_24
                )
            )
        }
    }
}

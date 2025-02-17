package com.flights.studio

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.drawable.LayerDrawable
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.Button
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import com.google.android.material.snackbar.Snackbar


class ContactFragment : PreferenceFragmentCompat() {
    @Suppress("DEPRECATION")
    @SuppressLint("RestrictedApi")
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        // Load the preferences from an XML resource
        setPreferencesFromResource(R.xml.root_contactpreferences, rootKey)

        // Set click listener for Phone Number
        findPreference<Preference>("phone")?.setOnPreferenceClickListener {
            val phoneNumber = getString(R.string._307_733_7682)
            CallConfirmationBottomSheetFragment.newInstance(phoneNumber)
                .show(parentFragmentManager, "callConfirmationBottomSheet") // Use parentFragmentManager
            true
        }

        findPreference<Preference>("email")?.setOnPreferenceClickListener {
            val email = "mailto:info@jhairport.org"
            val emailIntent = Intent(Intent.ACTION_SENDTO, Uri.parse(email))
            startActivity(emailIntent)
            true
        }
        findPreference<Preference>("custom_note")?.setOnPreferenceClickListener {
            val intent = Intent(requireContext(), AllNotesActivity::class.java)
            requireActivity().startActivity(intent)
            requireActivity().overridePendingTransition(R.anim.m3_motion_fade_enter, R.anim.m3_motion_fade_exit)
            true
        }



        // Set click listener for Physical Address
        findPreference<Preference>("address")?.setOnPreferenceClickListener {
            val rootView = requireActivity().findViewById<View>(android.R.id.content)

            // Inflate the custom Snackbar layout with a valid parent (rootView)
            val customSnackbarView = LayoutInflater.from(context).inflate(
                R.layout.snackbar_with_button,
                rootView as ViewGroup,
                false
            )

            // Find TextView and Button in the custom layout
            val snackbarTextView: TextView = customSnackbarView.findViewById(R.id.snackbar_text)
            val snackbarButton: Button = customSnackbarView.findViewById(R.id.snackbar_button)

            // Set the message text from string resource
            snackbarTextView.text = getString(R.string.drive_safely_and_stay_focused)

            // Create the Snackbar
            val snackbar = Snackbar.make(rootView, "", Snackbar.LENGTH_LONG)

            // Set custom view to the Snackbar (without accessing SnackbarLayout directly)
            snackbar.view.setPadding(0, 0, 0, 0) // Optional, adjust padding if needed
            val snackbarLayout = snackbar.view as Snackbar.SnackbarLayout
            snackbarLayout.addView(customSnackbarView) // Add the custom view

            // Add slide-in animation when the Snackbar appears
            val slideInAnimation = AnimationUtils.loadAnimation(context, androidx.appcompat.R.anim.abc_slide_in_bottom)
            snackbar.view.startAnimation(slideInAnimation)

            // Set the button click listener
            snackbarButton.setOnClickListener {
                val gmmIntentUri = Uri.parse("geo:43.603207,-110.736018?q=43.603207,-110.736018(Jackson Hole Airport)")
                val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri).apply {
                    setPackage("com.google.android.apps.maps")
                }
                startActivity(mapIntent)
            }

            // Show the Snackbar
            snackbar.show()

            // Add a slide-out animation when the Snackbar is dismissed
            Handler(Looper.getMainLooper()).postDelayed({
                snackbar.view.startAnimation(AnimationUtils.loadAnimation(context, R.anim.slide_in_left))
            }, 9000)

            true
        }

        setClickListener("Terminal", getString(R.string.info_jhairport_org))
        setClickListener("operations", getString(R.string.airport_operations_email))
        setClickListener("lost_and_found", getString(R.string.lost_and_found_email))
        setClickListener("human_resources", getString(R.string.human_resources_email))
        setClickListener("customer_experience", getString(R.string.communications_customer_experience_email))

        // Apply background to each preference icon
        listOf("phone", "email", "address", "pobox", "operations" , "custom_note", "lost_and_found", "human_resources", "all_contacts", "customer_experience")
            .forEach { key ->
                setIconBackground(findPreference(key), R.drawable.icon_settings_background)
            }
    }

    private fun setClickListener(key: String, emailAddress: String) {
        findPreference<Preference>(key)?.setOnPreferenceClickListener {
            val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:$emailAddress"))
            startActivity(intent)
            true
        }
    }

    private fun setIconBackground(preference: Preference?, drawableRes: Int) {
        preference?.icon?.let { icon ->
            // Get the background drawable
            val backgroundDrawable = ContextCompat.getDrawable(requireContext(), drawableRes)

            // Apply the background as a layer behind the icon
            backgroundDrawable?.let {
                val layerDrawable = LayerDrawable(arrayOf(it, icon))
                preference.icon = layerDrawable

                // Apply the tint using the selector
                val colorStateList = ContextCompat.getColorStateList(requireContext(), R.color.selector_item_bottomsheet)
                icon.setTintList(colorStateList)
            }
        }
    }
    override fun onPreferenceTreeClick(preference: Preference): Boolean {
        when (preference.key) {
            "all_contacts" -> {
                openAllContactsPage()
                return true
            }
        }
        return super.onPreferenceTreeClick(preference)
    }

    @Suppress("DEPRECATION")
    private fun openAllContactsPage() {
        // Navigate to a new fragment or activity
        val intent = Intent(requireContext(), AllContactsActivity::class.java)
        requireActivity().startActivity(intent)
        requireActivity().overridePendingTransition(R.anim.m3_motion_fade_enter, R.anim.m3_motion_fade_exit)
    }



}

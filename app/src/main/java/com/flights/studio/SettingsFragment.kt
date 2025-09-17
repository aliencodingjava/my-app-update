package com.flights.studio

import android.app.AlertDialog
import android.content.Intent
import android.graphics.drawable.LayerDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.FrameLayout
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.core.text.HtmlCompat
import androidx.preference.CheckBoxPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceGroup
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import java.util.Locale

@Suppress("DEPRECATION")
class SettingsFragment : PreferenceFragmentCompat() {
    private val allPreferences = mutableListOf<Preference>()

    @RequiresApi(Build.VERSION_CODES.VANILLA_ICE_CREAM)
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.root_preferences, rootKey)
        setupChangelogReleaseDate()
        setupLanguagePreference()
        // Setting up preference click listeners
        setupPreferenceListeners()
        // Storing all preferences for filtering
        populatePreferenceList(preferenceScreen)
        // Initializing Sync preference behavior
        setupSyncPreference()
        // Initialize app version info
        setupAppVersionInfo()
        // Apply icon backgrounds for all preferences
        applyIconBackgrounds()

    }


    private fun setupPreferenceListeners() {
        findPreference<Preference>("license")?.setOnPreferenceClickListener {
            openLicensesScreen()
            true
        }

        findPreference<Preference>("privacy_policy")?.setOnPreferenceClickListener {
            openPrivacyPolicyScreen()
            true
        }

        findPreference<Preference>("send_feedback")?.setOnPreferenceClickListener {
            sendFeedbackEmail()
            true
        }

        findPreference<Preference>("email_signup")?.setOnPreferenceClickListener {
            showSignUpDialog()
            true
        }

        findPreference<Preference>("rate_us")?.setOnPreferenceClickListener {
            showRateUsDialog()
            true
        }

        findPreference<Preference>("share_app")?.setOnPreferenceClickListener {
            shareApp()
            true
        }

        findPreference<Preference>("changelog")?.setOnPreferenceClickListener {
            showChangelogDialog()
            true
        }
    }

    private fun setupSyncPreference() {
        val syncPreference = findPreference<CheckBoxPreference>("sync")
        val checkForUpdatesPreference = findPreference<Preference>("check_for_updates")

        checkForUpdatesPreference?.isEnabled = syncPreference?.isChecked != true

        syncPreference?.setOnPreferenceChangeListener { _, newValue ->
            val isSyncEnabled = newValue as Boolean
            checkForUpdatesPreference?.isEnabled = !isSyncEnabled
            if (isSyncEnabled) showInitialAlertDialog()
            true
        }

        checkForUpdatesPreference?.setOnPreferenceClickListener {
            showInitialAlertDialog()
            true
        }
    }

    private fun setupAppVersionInfo() {
        try {
            val packageInfo = requireContext().packageManager.getPackageInfo(requireContext().packageName, 0)
            val versionName = packageInfo.versionName
            val versionCode = packageInfo.longVersionCode
            val architecture = getDeviceArchitecture()
            updateAppVersionSummary(versionName, versionCode, architecture)
        } catch (e: Exception) {
            Log.e("SettingsFragment", "Error retrieving package info", e)
        }
    }

    private fun applyIconBackgrounds() {
        listOf(
            "sync", "attachment", "check_for_updates", "app_version", "changelog",
            "email_signup", "rate_us", "share_app", "send_feedback", "language", "license", "General_Settings", "privacy_policy"
        ).forEach { key ->
            setIconBackground(findPreference(key), R.drawable.icon_settings_background)
        }
    }


    private fun setupLanguagePreference() {
        // inside setupLanguagePreference()
        findPreference<Preference>("language")?.setOnPreferenceClickListener {
            LanguageBottomSheetFragment()
                .show(requireActivity().supportFragmentManager, "LanguageBottomSheet")
            true
        }

    }

    // in SettingsFragment
    override fun onGetLayoutInflater(savedInstanceState: Bundle?): LayoutInflater {
        val base = super.onGetLayoutInflater(savedInstanceState)
        val lang = com.flights.studio.ui.AppLanguageManager.currentLanguageTag(base.context)
        return base.cloneInContext(LocaleUtils.wrap(base.context, lang))
    }






    private fun sendFeedbackEmail() {
        val mailto = "mailto:megan.jenkins@jhairport.org?subject=" + Uri.encode("User Experience Feedback for JH AirTracker")
        val emailIntent = Intent(Intent.ACTION_VIEW, mailto.toUri())
        try {
            startActivity(emailIntent)
        } catch (_: Exception) {
            Toast.makeText(context, "No email application found.", Toast.LENGTH_LONG).show()
        }
    }

    private fun showSignUpDialog() {
        SignUpBottomSheetDialogFragment().show(parentFragmentManager, null)
    }

    private fun showRateUsDialog() {
        RateUsDialogFragment().show(parentFragmentManager, "RateUsDialog")
    }

    private fun shareApp() {
        val sendIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, "0.2.213: https://tinyurl.com/ykrsyp8w")
            type = "text/plain"
        }
        startActivity(Intent.createChooser(sendIntent, null))
    }

    private fun openLicensesScreen() {
        val intent = Intent(requireContext(), LicensesActivity::class.java)
        startActivity(intent)
    }

    private fun openPrivacyPolicyScreen() {
        val intent = Intent(requireContext(), PrivacyPolicyActivity::class.java)
        startActivity(intent)
    }

    private fun showChangelogDialog() {
        val changelogText = """
        &#8211; <b>Previous Update:</b><br/>
        General improvements and enhancements.<br/><br/>
        
        &#8211; <b>Introducing a New Color Palette System:</b><br/>
        Customize your experience with themed palettes for cards, overlays, and action buttons — over 50 vibrant combinations to choose from.<br/><br/>

        
        &#8211; <b>Quick timer setup:</b><br/>
        Type “calendar” or “clock” beneath any note to schedule a timer notification.<br/><br/>

        
        &#8211; <b>Redesigned Feedback:</b><br/>
        Improved visual design and user experience.<br/><br/>
        
        &#8211; <b>Instant Ban/Unban Alerts:</b><br/>
        Receive immediate updates when your feedback access is restricted or restored.<br/><br/>

        &#8211; <b>Keep It Respectful:</b><br/>
        Please ensure all feedback is constructive and courteous.<br/><br/>

        
        &#8211; <b>Added Notes:</b><br/>
        A new section to capture and organize ideas.<br/><br/>
        
        &#8211; <b>under the notes you want to create timer notification:</b><br/>
        just write calendar or clock under the notes you want to create timer notification.<br/><br/>
        
        &#8211; <b>Added Cards to home screen:</b><br/>
        Quickly access activities in one place.<br/><br/>
        
        &#8211; <b>Added Contact:</b><br/>
        Quickly access contact details in one place.<br/><br/>
        
        &#8211; <b>Enhanced UI &amp; Animations:</b><br/>
        Improved design with smoother transitions.<br/><br/>
    
        &#8211; <b>Optimized Performance:</b><br/>
        Faster loading times and reduced memory usage.<br/><br/>

        &#8211; <b>New Card Designs:</b><br/>
        Introduced dynamic expand/collapse functionality.<br/><br/>
        
        &#8211; <b>Improved Visual Hierarchy:</b><br/>
        Adjusted gaps for better readability and structure.<br/><br/>
        
        &#8211; <b>Advanced Page Settings:</b><br/>
        New options for customizing your experience.<br/><br/>
        
        &#8211; <b>Widget:</b><br/>
        A quick-access widget for essential features.<br/><br/>
        
        &#8211; <b>Redesigned the App:</b><br/>
        Improved layout and preferences for better customization.<br/><br/>
        
        &#8211; <b>Stability &amp; Performance:</b><br/>
        Enhanced stability and expandable content behavior.<br/><br/>
        
        &#8211; <b>Share Button:</b><br/>
        Added to contact cards for easier sharing.<br/><br/>
        
        &#8211; <b>Countdown Timer:</b><br/>
        Added functionality to refresh images online.<br/><br/>
        
        &#8211; <b>Countdown Timer:</b><br/>
        Added widget battery status (beta).<br/><br/>
        
        &#8211; <b>Thank You:</b><br/>
        Thank you for using our app!<br/>
    """.trimIndent()


        val versionName = requireContext().packageManager.getPackageInfo(requireContext().packageName, 0).versionName
        val version = "V.$versionName"

        val bottomSheetDialog = BottomSheetDialog(requireContext())
        val rootView = FrameLayout(requireContext())
        val bottomSheetView = layoutInflater.inflate(R.layout.dialog_changelog, rootView, false)

        bottomSheetView.findViewById<TextView>(R.id.dialogTitleChangelog).text = getString(R.string.changelog_title, version)
        bottomSheetView.findViewById<TextView>(R.id.dialogMessage_changelog).text = HtmlCompat.fromHtml(changelogText, HtmlCompat.FROM_HTML_MODE_LEGACY)
        bottomSheetView.findViewById<Button>(R.id.dialogButton).setOnClickListener {
            bottomSheetDialog.dismiss()
        }

        bottomSheetDialog.setContentView(bottomSheetView)
        bottomSheetDialog.setOnShowListener { dialog ->
            val bottomSheet = (dialog as BottomSheetDialog).findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
            bottomSheet?.let {
                val behavior = BottomSheetBehavior.from(it)
                behavior.peekHeight = (requireContext().resources.displayMetrics.heightPixels * 0.6).toInt()
                behavior.state = BottomSheetBehavior.STATE_COLLAPSED
            }
        }
        bottomSheetDialog.show()
    }


    private fun updateAppVersionSummary(versionName: String?, versionCode: Long?, architecture: String?) {
        val preference = findPreference<Preference>("app_version")
        preference?.summary = "$versionName ($versionCode) - $architecture"
    }

    private fun getDeviceArchitecture(): String {
        return Build.SUPPORTED_ABIS.firstOrNull() ?: "Unknown"
    }

    private fun showInitialAlertDialog() {
        val ctx = requireActivity() // <- use Activity context / inflater

        // Inflate with the Activity's inflater so Material theme is present
        val customLayout = ctx.layoutInflater.inflate(R.layout.custom_dialog_layout, null)

        customLayout.findViewById<TextView>(R.id.Checking)
            .setTextColor(ContextCompat.getColor(ctx, R.color.text_color_alert))
        customLayout.findViewById<TextView>(R.id.checking_for_updates)
            .setTextColor(ContextCompat.getColor(ctx, R.color.message_color_alert))

        val alertDialog = AlertDialog.Builder(ctx)
            .setView(customLayout)
            .setCancelable(false)
            .create()

        alertDialog.window?.setBackgroundDrawableResource(R.drawable.dialog_background)
        alertDialog.show()

        (activity as? SettingsActivity)?.checkForUpdates(alertDialog)
    }


    private fun setIconBackground(preference: Preference?, drawableRes: Int) {
        preference?.icon?.let { icon ->
            val backgroundDrawable = ContextCompat.getDrawable(requireContext(), drawableRes)
            backgroundDrawable?.let {
                val layerDrawable = LayerDrawable(arrayOf(it, icon))
                preference.icon = layerDrawable
                val colorStateList = ContextCompat.getColorStateList(requireContext(), R.color.selector_item_bottomsheet)
                icon.setTintList(colorStateList)
            }
        }
    }

    private fun populatePreferenceList(prefGroup: PreferenceGroup) {
        for (i in 0 until prefGroup.preferenceCount) {
            val pref = prefGroup.getPreference(i)
            allPreferences.add(pref)
            if (pref is PreferenceGroup) {
                populatePreferenceList(pref)
            }
        }
    }

    fun filterPreferences(query: String) {
        val lowerCaseQuery = query.lowercase(Locale.getDefault())
        val visibleCategories = mutableSetOf<PreferenceGroup>()

        allPreferences.forEach { preference ->
            val title = preference.title?.toString()?.lowercase(Locale.getDefault()) ?: ""
            val summary = preference.summary?.toString()?.lowercase(Locale.getDefault()) ?: ""
            val isVisible = title.contains(lowerCaseQuery) || summary.contains(lowerCaseQuery)
            preference.isVisible = isVisible

            if (isVisible && preference.parent is PreferenceGroup) {
                visibleCategories.add(preference.parent as PreferenceGroup)
            }
        }

        allPreferences.filterIsInstance<PreferenceGroup>().forEach { category ->
            category.isVisible = visibleCategories.contains(category)
        }
    }


    private fun setupChangelogReleaseDate() {
        val changelogPreference = findPreference<Preference>("changelog")
        val releaseDate = BuildConfig.RELEASE_DATE // Access the field from BuildConfig
        changelogPreference?.summary = "Release Date: $releaseDate"
    }



}

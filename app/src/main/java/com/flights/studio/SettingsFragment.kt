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
import androidx.lifecycle.lifecycleScope
import androidx.preference.CheckBoxPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetDialog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.URL
import java.util.Locale
private const val GIST_URL =
    "https://gist.github.com/aliencodingjava/8ef085a89b30d85e2e86fb6f148d80cb/raw/gistfile2.txt"

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

    private fun openCardDrawer(cardId: String) {
        val intent = Intent(requireContext(), webviewflightActivity::class.java)
        intent.putExtra("start_card", cardId)
        startActivity(intent)
    }

    private fun setupPreferenceListeners() {
        findPreference<Preference>("license")?.setOnPreferenceClickListener {
            openCardDrawer("licenses")
            true
        }

        findPreference<Preference>("privacy_policy")?.setOnPreferenceClickListener {
            openCardDrawer("privacy_policy")
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
            val versionCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                packageInfo.longVersionCode
            } else {
                TODO("VERSION.SDK_INT < P")
            }
            val architecture = getDeviceArchitecture()
            updateAppVersionSummary(versionName, versionCode, architecture)
        } catch (e: Exception) {
            Log.e("SettingsFragment", "Error retrieving package info", e)
        }
    }

    private fun applyIconBackgrounds() {
        listOf(
            "sync", "attachment", "check_for_updates", "app_version", "changelog",
            "email_signup", "rate_us", "share_app", "send_feedback",
            "language", "license", "General_Settings", "privacy_policy", "siri_camera_glow"
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
            putExtra(Intent.EXTRA_TEXT, "0.2.227: https://tinyurl.com/46w6827y")
            type = "text/plain"
        }
        startActivity(Intent.createChooser(sendIntent, null))
    }


    private fun showChangelogDialog() {
        val ctx = requireContext()

        val versionName = ctx.packageManager
            .getPackageInfo(ctx.packageName, 0)
            .versionName

        val bottomSheetDialog = BottomSheetDialog(ctx)

        val root = FrameLayout(ctx)
        val bottomSheetView = layoutInflater.inflate(R.layout.update_bottom_sheet, root, false)

        bottomSheetView.findViewById<TextView>(R.id.update_app_title)
            .text = getString(R.string.changelog_title, versionName)

        val recyclerView = bottomSheetView.findViewById<RecyclerView>(R.id.updateRecyclerView)
        recyclerView.layoutManager = LinearLayoutManager(ctx)

        // Buttons: OK only
        bottomSheetView.findViewById<Button>(R.id.button_download).apply {
            text = getString(R.string.ok)
            setOnClickListener { bottomSheetDialog.dismiss() }
        }
        bottomSheetView.findViewById<Button>(R.id.button_cancel).visibility = View.GONE

        bottomSheetDialog.setContentView(bottomSheetView)
        bottomSheetDialog.show()

        // ✅ Load changelog from Git
        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            val items: List<UpdateBlock> = try {
                val jsonText = URL(GIST_URL).readText()
                val json = JSONObject(jsonText)
                val arr = json.optJSONArray("updates")

                buildList {
                    if (arr != null) {
                        for (i in 0 until arr.length()) {
                            val o = arr.getJSONObject(i)
                            add(
                                UpdateBlock(
                                    title = o.optString("title"),
                                    body = o.optString("body")
                                )
                            )
                        }
                    }
                }
            } catch (_: Exception) {
                // ✅ fallback if offline
                listOf(
                    UpdateBlock("Offline", "No internet connection. Showing local changelog later.")
                )
            }

            withContext(Dispatchers.Main) {
                recyclerView.adapter = UpdateAdapter(items)
            }
        }
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

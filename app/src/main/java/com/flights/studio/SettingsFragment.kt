package com.flights.studio

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
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.lifecycle.lifecycleScope
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetDialog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Suppress("DEPRECATION")
class SettingsFragment : PreferenceFragmentCompat() {

    private val allPreferences = mutableListOf<Preference>()

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.root_preferences, rootKey)

        setupChangelogReleaseDate()
        setupLanguagePreference()
        setupPreferenceListeners()
        setupUpdatePreference()
        setupAppVersionInfo()
        applyIconBackgrounds()

        preferenceScreen?.let { populatePreferenceList(it) }
    }

    override fun onGetLayoutInflater(savedInstanceState: Bundle?): LayoutInflater {
        val base = super.onGetLayoutInflater(savedInstanceState)
        val lang = com.flights.studio.ui.AppLanguageManager.currentLanguageTag(base.context)
        return base.cloneInContext(LocaleUtils.wrap(base.context, lang))
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
            SignUpBottomSheetDialogFragment().show(parentFragmentManager, "SignUpBottomSheet")
            true
        }

        findPreference<Preference>("rate_us")?.setOnPreferenceClickListener {
            RateUsDialogFragment().show(parentFragmentManager, "RateUsDialog")
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

        findPreference<Preference>("app_icon")?.setOnPreferenceClickListener {
            startActivity(Intent(requireContext(), AppIconPickerActivity::class.java))
            true
        }
    }

    private fun setupUpdatePreference() {
        findPreference<Preference>("check_for_updates")?.setOnPreferenceClickListener {
            startActivity(Intent(requireContext(), SoftwareUpdateActivity::class.java))
            true
        }
    }

    private fun setupLanguagePreference() {
        findPreference<Preference>("language")?.setOnPreferenceClickListener {
            LanguageBottomSheetFragment()
                .show(requireActivity().supportFragmentManager, "LanguageBottomSheet")
            true
        }
    }

    private fun setupAppVersionInfo() {
        try {
            val packageInfo = requireContext().packageManager
                .getPackageInfo(requireContext().packageName, 0)
            val versionCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                packageInfo.longVersionCode
            } else {
                packageInfo.versionCode.toLong()
            }

            findPreference<Preference>("app_version")?.summary =
                "${packageInfo.versionName} ($versionCode) - ${getDeviceArchitecture()}"
        } catch (e: Exception) {
            Log.e("SettingsFragment", "Error retrieving package info", e)
        }
    }

    private fun setupChangelogReleaseDate() {
        findPreference<Preference>("changelog")?.summary =
            getString(R.string.settings_release_date_template, BuildConfig.RELEASE_DATE)
    }

    private fun applyIconBackgrounds() {
        listOf(
            "check_for_updates",
            "app_version",
            "changelog",
            "email_signup",
            "rate_us",
            "share_app",
            "send_feedback",
            "language",
            "app_icon",
            "license",
            "privacy_policy",
            "siri_camera_glow"
        ).forEach { key ->
            setIconBackground(findPreference(key), R.drawable.icon_settings_background)
        }
    }

    private fun openCardDrawer(cardId: String) {
        startActivity(
            Intent(requireContext(), WebviewflightActivity::class.java)
                .putExtra("start_card", cardId)
        )
    }

    private fun sendFeedbackEmail() {
        val mailto = "mailto:megan.jenkins@jhairport.org?subject=" +
            Uri.encode("User Experience Feedback for JH AirTracker")
        val emailIntent = Intent(Intent.ACTION_VIEW, mailto.toUri())
        try {
            startActivity(emailIntent)
        } catch (_: Exception) {
            Toast.makeText(context, "No email application found.", Toast.LENGTH_LONG).show()
        }
    }

    private fun shareApp() {
        val versionName = runCatching {
            requireContext().packageManager
                .getPackageInfo(requireContext().packageName, 0)
                .versionName
        }.getOrNull().orEmpty()

        val shareText = """
Download JH Flight Studio:
${MainActivity.APP_SHARE_URL}

Version: $versionName
""".trimIndent()

        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            putExtra(Intent.EXTRA_TEXT, shareText)
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

        bottomSheetView.findViewById<Button>(R.id.button_download).apply {
            text = getString(R.string.ok)
            setOnClickListener { bottomSheetDialog.dismiss() }
        }
        bottomSheetView.findViewById<Button>(R.id.button_cancel).visibility = View.GONE

        bottomSheetDialog.setContentView(bottomSheetView)
        bottomSheetDialog.show()

        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            val items = try {
                AppUpdateRepository.fetchRemoteUpdate().updates
            } catch (_: Exception) {
                listOf(
                    UpdateBlock(
                        title = getString(R.string.settings_changelog_offline_title),
                        summary = getString(R.string.settings_changelog_offline)
                    )
                )
            }

            withContext(Dispatchers.Main) {
                recyclerView.adapter = UpdateAdapter(items)
            }
        }
    }

    private fun getDeviceArchitecture(): String {
        return Build.SUPPORTED_ABIS.firstOrNull() ?: "Unknown"
    }

    private fun setIconBackground(preference: Preference?, drawableRes: Int) {
        val icon = preference?.icon ?: return
        val background = ContextCompat.getDrawable(requireContext(), drawableRes) ?: return
        preference.icon = LayerDrawable(arrayOf(background, icon))
        icon.setTintList(
            ContextCompat.getColorStateList(requireContext(), R.color.selector_item_bottomsheet)
        )
    }

    private fun populatePreferenceList(prefGroup: PreferenceGroup) {
        allPreferences.clear()
        collectPreferences(prefGroup)
    }

    private fun collectPreferences(prefGroup: PreferenceGroup) {
        for (i in 0 until prefGroup.preferenceCount) {
            val preference = prefGroup.getPreference(i)
            allPreferences.add(preference)
            if (preference is PreferenceGroup) collectPreferences(preference)
        }
    }
}

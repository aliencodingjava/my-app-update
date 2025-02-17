package com.flights.studio

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RadioGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.PreferenceManager
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import java.util.Locale

class LanguageBottomSheetFragment : BottomSheetDialogFragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.bottom_sheet_language, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val radioGroup = view.findViewById<RadioGroup>(R.id.languageRadioGroup)

        // Ensure English is the default language
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(requireContext())
        val savedLanguage = sharedPreferences.getString("language", "en") ?: "en"

        // Pre-select the saved language in the radio group
        when (savedLanguage) {
            "en" -> radioGroup.check(R.id.radioEnglish)
            "es" -> radioGroup.check(R.id.radioSpanish)
        }

        // Handle language selection
        radioGroup.setOnCheckedChangeListener { _, checkedId ->
            val selectedLanguage = when (checkedId) {
                R.id.radioEnglish -> "en"
                R.id.radioSpanish -> "es"
                else -> "en"
            }

            // Save the selected language and apply changes
            saveLanguage(selectedLanguage)
            applyLocale(selectedLanguage)
            dismiss()
        }
    }

    private fun saveLanguage(languageCode: String) {
        // Save the selected language in SharedPreferences
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(requireContext())
        sharedPreferences.edit().putString("language", languageCode).apply()
    }

    private fun applyLocale(languageCode: String) {
        val locale = Locale(languageCode)
        Locale.setDefault(locale)

        val activity = requireActivity() as AppCompatActivity

        // Update the activity configuration with the new locale
        val resources = activity.resources
        val config = resources.configuration
        config.setLocale(locale)
        config.setLayoutDirection(locale)

        activity.createConfigurationContext(config)

        // Recreate the activity to fully apply the new language
        activity.recreate()
    }
}

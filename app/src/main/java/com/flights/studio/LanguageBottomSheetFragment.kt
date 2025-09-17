
package com.flights.studio

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RadioGroup
import com.flights.studio.ui.AppLanguageManager
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.button.MaterialButton

class LanguageBottomSheetFragment : BottomSheetDialogFragment() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(
            STYLE_NORMAL,
            com.google.android.material.R.style.ThemeOverlay_Material3_BottomSheetDialog
        )
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.bottom_sheet_language, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val radioGroup = view.findViewById<RadioGroup>(R.id.languageRadioGroup)
        val confirmButton = view.findViewById<MaterialButton>(R.id.confirmButton)

        when (AppLanguageManager.currentLanguageTag(requireContext())) {
            "en" -> radioGroup.check(R.id.radioEnglish)
            "es" -> radioGroup.check(R.id.radioSpanish)
        }

        confirmButton.setOnClickListener {
            val selected = when (radioGroup.checkedRadioButtonId) {
                R.id.radioEnglish -> "en"
                R.id.radioSpanish -> "es"
                else -> AppLanguageManager.DEFAULT_LANGUAGE_TAG
            }
            val current = AppLanguageManager.currentLanguageTag(requireContext())
            if (selected == current) {
                dismiss(); return@setOnClickListener
            }

            // LanguageBottomSheetFragment (unchanged)
            AppLanguageManager.persistLanguage(requireContext(), selected)

            val fm = requireActivity().supportFragmentManager
            fm.beginTransaction()
                .replace(R.id.content_frame, SettingsFragment())
                .commitNowAllowingStateLoss()

            dismissAllowingStateLoss()
            requireActivity().recreate()


        }

    }
}

package com.flights.studio

import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import com.flights.studio.ui.AppLanguageManager

abstract class LocaleActivity : AppCompatActivity() {
    override fun attachBaseContext(newBase: Context) {
        val tag = AppLanguageManager.currentLanguageTag(newBase)
        super.attachBaseContext(LocaleUtils.wrap(newBase, tag))
    }
}
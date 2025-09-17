package com.flights.studio

import android.content.Context
import android.content.res.Configuration
import android.os.LocaleList
import java.util.Locale

object LocaleUtils {
    fun wrap(base: Context, languageTag: String): Context {
        val locale = Locale.forLanguageTag(languageTag)
        val cfg = Configuration(base.resources.configuration)
        val list = LocaleList(locale)
        LocaleList.setDefault(list)
        cfg.setLocales(list)
        return base.createConfigurationContext(cfg)
    }

}
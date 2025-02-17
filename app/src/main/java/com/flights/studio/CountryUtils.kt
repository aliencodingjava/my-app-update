package com.flights.studio

import android.util.Log
import com.google.i18n.phonenumbers.NumberParseException
import com.google.i18n.phonenumbers.PhoneNumberUtil

object CountryUtils {

    fun getCountryCodeAndFlag(phone: String): Pair<String?, String> {
        val phoneNumberUtil = PhoneNumberUtil.getInstance()
        return try {
            val phoneNumber = phoneNumberUtil.parse(phone, null) // Auto-detect country
            val regionCode = phoneNumberUtil.getRegionCodeForNumber(phoneNumber) // Get country code
            val flag = getFlagEmoji(regionCode) // Convert to flag emoji

            Log.d("CountryUtils", "Detected Region: $regionCode, Flag: $flag")

            Pair(regionCode, flag)
        } catch (e: NumberParseException) {
            e.printStackTrace()
            Pair(null, "") // Return empty flag if invalid
        }
    }

    fun getFlagEmoji(regionCode: String?): String {
        if (regionCode.isNullOrEmpty()) return "" // Return empty if null

        return regionCode.uppercase()
            .map { char -> 0x1F1E6 + (char.code - 'A'.code) }
            .map { codePoint -> String(Character.toChars(codePoint)) }
            .joinToString("")
    }

    fun getCountryName(regionCode: String): String {
        val countryMap = mapOf(
            "US" to "United States",
            "GB" to "United Kingdom",
            "DE" to "Germany",
            "IN" to "India",
            "JP" to "Japan",
            "FR" to "France",
            "IT" to "Italy",
            "ES" to "Spain",
            "BR" to "Brazil",
            "AU" to "Australia",
            "EG" to "Egypt",
            "AE" to "United Arab Emirates",
            "SA" to "Saudi Arabia",
            "KR" to "South Korea",
            "RU" to "Russia",
            "ZA" to "South Africa",
            "MX" to "Mexico",
            "PK" to "Pakistan",
            "UA" to "Ukraine",
            "CN" to "China",
            "PT" to "Portugal",
            "ID" to "Indonesia",
            "SE" to "Sweden",
            "NO" to "Norway",
            "PL" to "Poland",
            "NL" to "Netherlands",
            "TR" to "Turkey",
            "MD" to "Moldova" // ✅ Added Moldova 🇲🇩
        )
        return countryMap[regionCode] ?: "Unknown Country"
    }
}

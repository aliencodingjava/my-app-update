package com.flights.studio

import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import androidx.core.content.edit
import androidx.core.net.toUri
import androidx.lifecycle.MutableLiveData

class UserPreferencesManager(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    // ✅ Observable login state
    val isLoggedInLiveData = MutableLiveData(prefs.getBoolean(KEY_IS_LOGGED_IN, false))

    var isLoggedIn: Boolean
        get() = prefs.getBoolean(KEY_IS_LOGGED_IN, false)
        set(value) {
            prefs.edit { putBoolean(KEY_IS_LOGGED_IN, value) }
            isLoggedInLiveData.postValue(value)
        }

    var userName: String?
        get() = prefs.getString(KEY_USER_NAME, null)
        set(value) = prefs.edit { putString(KEY_USER_NAME, value) }

    var userPhone: String?
        get() = prefs.getString(KEY_USER_PHONE, null)
        set(value) = prefs.edit { putString(KEY_USER_PHONE, value) }

    var userEmail: String?
        get() = prefs.getString(KEY_USER_EMAIL, null)
        set(value) = prefs.edit { putString(KEY_USER_EMAIL, value) }

    var userBirthday: String?
        get() = prefs.getString(KEY_USER_BIRTHDAY, null)
        set(value) = prefs.edit { putString(KEY_USER_BIRTHDAY, value) }

    var userBio: String?
        get() = prefs.getString(KEY_USER_BIO, null)
        set(value) = prefs.edit { putString(KEY_USER_BIO, value) }

    var userInitials: String?
        get() = prefs.getString(KEY_USER_INITIALS, null)
        set(value) = prefs.edit { putString(KEY_USER_INITIALS, value) }

    /**
     * Stores either:
     * - content://... (local chosen photo)
     * - https://...   (remote Supabase photo url)
     *
     * Never store blank or the literal "null".
     */
    var userPhotoUriString: String?
        get() = prefs.getString(KEY_USER_PHOTO_URI, null)
        set(value) = prefs.edit { putString(KEY_USER_PHOTO_URI, value) }

    var loggedInUserId: String?
        get() = prefs.getString(KEY_LOGGED_IN_USER_ID, null)
        set(value) = prefs.edit { putString(KEY_LOGGED_IN_USER_ID, value) }

    var profileThemeMode: Int
        get() = prefs.getInt("profile_theme_mode", 0) // 0=Auto, 1=Glass, 2=Solid
        set(value) = prefs.edit { putInt("profile_theme_mode", value) }

    var pendingFullName: String?
        get() = prefs.getString("pending_full_name", null)
        set(v) = prefs.edit { putString("pending_full_name", v) }

    var pendingPhone: String?
        get() = prefs.getString("pending_phone", null)
        set(v) = prefs.edit { putString("pending_phone", v) }

    var pendingEmail: String?
        get() = prefs.getString("pending_email", null)
        set(v) = prefs.edit { putString("pending_email", v) }


    // ----------------------------
    // Photo helpers
    // ----------------------------

    fun setPhotoString(value: String?) {
        val clean = value
            ?.trim()
            ?.takeIf { it.isNotBlank() && !it.equals("null", ignoreCase = true) }

        userPhotoUriString = clean
        if (clean != null) userInitials = null
    }

    fun clearUserPhoto() {
        userPhotoUriString = null
    }
    fun getUserPhotoUri(): Uri? {
        val raw = userPhotoUriString?.trim().orEmpty()
        if (raw.isBlank()) return null

        if (raw.startsWith("/")) return Uri.fromFile(java.io.File(raw))

        // ✅ Only return real URIs that Android loaders can handle directly.
        if (raw.startsWith("content", true) || raw.startsWith("file", true) || raw.startsWith("http", true)) {
            return raw.toUri()
        }

        // ✅ storage paths are NOT Uris
        return null
    }






    // ----------------------------
    // Profile save / clear
    // ----------------------------

    fun saveUserProfile(
        name: String,
        phone: String,
        email: String?,
        birthday: String?,
        bio: String?,
        selectedPhotoUri: Uri?
    ) {
        isLoggedIn = true

        userName = name.trim().ifBlank { null }
        userPhone = phone.trim().ifBlank { null }
        userEmail = email?.trim()?.ifBlank { null }
        userBirthday = birthday?.trim()?.ifBlank { null }
        userBio = bio?.trim()?.ifBlank { null }

        if (selectedPhotoUri != null) {
            // ✅ new photo chosen (content://...)
            setPhotoString(selectedPhotoUri.toString())
        } else {
            // ✅ no new photo: keep existing photo if already saved,
            // otherwise generate initials from name
            val hasPhoto = !userPhotoUriString.isNullOrBlank() &&
                    !userPhotoUriString.equals("null", ignoreCase = true)

            if (!hasPhoto) {
                userInitials = computeInitialsFrom(name)
            }
        }
    }

    fun clear() {
        prefs.edit { clear() }
        isLoggedInLiveData.postValue(false)
    }

    fun getRawFullName(): String? = userName?.takeIf { it.isNotBlank() }

    private fun computeInitialsFrom(name: String): String {
        return name.trim()
            .split(Regex("\\s+"))
            .filter { it.isNotBlank() }
            .take(2)
            .mapNotNull { it.firstOrNull()?.uppercaseChar()?.toString() }
            .joinToString("")
            .ifEmpty { "?" }
    }

    companion object {
        private const val PREFS_NAME = "UserPrefs"

        const val KEY_IS_LOGGED_IN = "isLoggedIn"
        const val KEY_USER_NAME = "userName"
        const val KEY_USER_PHONE = "userPhone"
        const val KEY_USER_EMAIL = "userEmail"
        const val KEY_USER_BIRTHDAY = "userBirthday"
        const val KEY_USER_BIO = "userBio"
        const val KEY_USER_INITIALS = "userInitials"
        const val KEY_USER_PHOTO_URI = "userPhotoUri"

        private const val KEY_LOGGED_IN_USER_ID = "loggedInUserId"
    }
}

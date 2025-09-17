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

    var userBio: String?
        get() = prefs.getString(KEY_USER_BIO, null)
        set(value) = prefs.edit { putString(KEY_USER_BIO, value) }
    

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

    var userInitials: String?
        get() = prefs.getString(KEY_USER_INITIALS, null)
        set(value) = prefs.edit { putString(KEY_USER_INITIALS, value) }

    var userPhotoUriString: String?
        get() = prefs.getString(KEY_USER_PHOTO_URI, null)
        set(value) = prefs.edit { putString(KEY_USER_PHOTO_URI, value) }

    var loggedInUserId: String?
        get() = prefs.getString("loggedInUserId", null)
        set(value) = prefs.edit { putString("loggedInUserId", value) }



    fun getUserPhotoUri(): Uri? = userPhotoUriString?.toUri()

    fun saveUserProfile(
        name: String,
        phone: String,
        email: String?,
        birthday: String?,
        bio: String?,
        selectedPhotoUri: Uri?
    ) {
        isLoggedIn = true
        userName = name
        userPhone = phone
        userEmail = email
        userBirthday = birthday
        userBio = bio

        if (selectedPhotoUri != null) {
            userPhotoUriString = selectedPhotoUri.toString()
            userInitials = null
        } else {
            if (userPhotoUriString == null) {
                userInitials = name.split(" ")
                    .filter { it.isNotBlank() }
                    .take(2)
                    .mapNotNull { it.firstOrNull()?.uppercaseChar()?.toString() }
                    .joinToString("")
            }
        }
    }



    fun clear() {
        prefs.edit { clear() }
        isLoggedInLiveData.postValue(false)
    }

    fun clearUserPhoto() {
        userPhotoUriString = null
    }

    fun getRawFullName(): String? = userName?.takeIf { it.isNotBlank() }

    companion object {
        private const val PREFS_NAME = "UserPrefs"
        const val KEY_IS_LOGGED_IN = "isLoggedIn"
        const val KEY_USER_NAME = "userName"
        const val KEY_USER_PHONE = "userPhone"
        const val KEY_USER_EMAIL = "userEmail"
        const val KEY_USER_BIRTHDAY = "userBirthday"
        const val KEY_USER_INITIALS = "userInitials"
        const val KEY_USER_PHOTO_URI = "userPhotoUri"
        const val KEY_USER_BIO = "userBio"
    }
}

package com.flights.studio

import android.os.Looper
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class CardBottomSheetViewModel(
    private val prefs: UserPreferencesManager
) : ViewModel() {

    private val _isLoggedIn = MutableLiveData(prefs.isLoggedIn)
    val isLoggedIn: LiveData<Boolean> = _isLoggedIn

    private val _name = MutableLiveData(prefs.getRawFullName())
    val name: LiveData<String?> = _name

    private val _phone = MutableLiveData(prefs.userPhone)
    val phone: LiveData<String?> = _phone

    private val _photoUri = MutableLiveData(prefs.userPhotoUriString)
    val photoUri: LiveData<String?> = _photoUri

    private val _initials = MutableLiveData(prefs.userInitials)
    val initials: LiveData<String?> = _initials

    // Online status LiveData
    private val _isOnline = MutableLiveData(false)
    val isOnline: LiveData<Boolean> = _isOnline

    /**
     * Update online status. Uses postValue when called off the main thread.
     */
    fun setOnline(online: Boolean) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            _isOnline.value = online
        } else {
            _isOnline.postValue(online)
        }
    }

    fun refresh() {
        _isLoggedIn.value = prefs.isLoggedIn
        _name.value = prefs.getRawFullName()
        _phone.value = prefs.userPhone
        _photoUri.value = prefs.userPhotoUriString
        _initials.value = prefs.userInitials
    }

    fun logout() {
        prefs.clear()
        _isLoggedIn.value = false
        _name.value = null
        _phone.value = null
        _photoUri.value = null
        _initials.value = null
        _isOnline.value = false
    }
}

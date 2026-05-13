package com.flights.studio

import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.view.ViewGroup
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import android.widget.FrameLayout
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop

@Suppress("DEPRECATION", "OVERRIDE_DEPRECATION")
class AllContactsActivity : LocaleActivity() {

    private var allContactsFragment: AllContactsFragment? = null
    private var containerView: FrameLayout? = null
    private var fragmentInstalled = false
    private val contactsChromeCount = mutableIntStateOf(0)
    private val contactsSearchQuery = mutableStateOf("")
    private val contactsFloatingSearchVisible = mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.setSoftInputMode(
            WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING or
                WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN
        )

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (contactsSearchQuery.value.isNotEmpty()) {
                    updateContactsSearch("")
                    updateContactsFloatingSearchVisible(false)
                    hideKeyboard()
                } else {
                    finish()
                    overridePendingTransition(R.anim.enter_animation, R.anim.exit_animation)
                }
            }
        })

        window.decorView.alpha = 1.0f

        setContent {
            FlightsTheme(profileBackdropStyle = ProfileBackdropStyle.Auto) {
                ContactsScreenHost(
                    contactCount = contactsChromeCount.intValue,
                    searchQuery = contactsSearchQuery.value,
                    showFloatingSearch = contactsFloatingSearchVisible.value,
                    onContainerReady = { container ->
                        containerView = container
                        installContactsFragment()
                    }
                )
            }
        }
    }

    private fun goToHomeScreen() {
        startActivity(Intent(this, MainActivity::class.java))
        overridePendingTransition(R.anim.enter_animation, R.anim.exit_animation)

        finish()
    }

    private fun goToSettingsScreen() {
        startActivity(Intent(this, SettingsActivity::class.java))
        overridePendingTransition(R.anim.enter_animation, R.anim.exit_animation)
    }

    private fun goToNotesScreen() {
        startActivity(Intent(this, AllNotesActivity::class.java))
        overridePendingTransition(R.anim.enter_animation, R.anim.exit_animation)

        finish()
    }

    fun updateContactsChromeCount(visibleCount: Int) {
        contactsChromeCount.intValue = visibleCount
    }

    fun updateContactsSearch(query: String) {
        contactsSearchQuery.value = query
        if (query.isBlank()) contactsFloatingSearchVisible.value = false
        allContactsFragment?.filterContacts(query)
    }

    fun updateContactsFloatingSearch(query: String) {
        contactsSearchQuery.value = query
        allContactsFragment?.filterContacts(
            query = query,
            syncTopSearch = false,
            keepFloatingSearchActive = true
        )
    }

    fun updateContactsFloatingSearchVisible(visible: Boolean) {
        contactsFloatingSearchVisible.value = visible
    }

    private fun installContactsFragment() {
        if (fragmentInstalled) return
        fragmentInstalled = true

        allContactsFragment =
            supportFragmentManager.findFragmentById(R.id.container) as? AllContactsFragment

        if (allContactsFragment == null) {
            allContactsFragment = AllContactsFragment()
            supportFragmentManager.beginTransaction()
                .replace(R.id.container, allContactsFragment!!)
                .commitNow()
        }
    }

    private fun hideKeyboard() {
        val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(containerView?.windowToken, 0)
        containerView?.clearFocus()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                onBackPressed()
                finish()
                overridePendingTransition(R.anim.enter_animation, R.anim.exit_animation)

                true
            }

            else -> super.onOptionsItemSelected(item)
        }
    }

    @Composable
    private fun ContactsScreenHost(
        contactCount: Int,
        searchQuery: String,
        showFloatingSearch: Boolean,
        onContainerReady: (FrameLayout) -> Unit
    ) {
        val backdrop = rememberLayerBackdrop()

        Box(Modifier.fillMaxSize()) {
            val isDark = isSystemInDarkTheme()
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .layerBackdrop(backdrop)
            ) {
                ProfileBackdropImageLayer(
                    modifier = Modifier.fillMaxSize(),
                    lightRes = R.drawable.light_grid_pattern,
                    darkRes = R.drawable.dark_grid_pattern,
                    imageAlpha = if (isDark) 1f else 0.8f,
                    scrimDark = 0f,
                    scrimLight = 0f
                )
            }

            AndroidView(
                modifier = Modifier
                    .fillMaxSize(),
                factory = { context ->
                    FrameLayout(context).apply {
                        id = R.id.container
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                        setBackgroundColor(android.graphics.Color.TRANSPARENT)
                    }
                },
                update = { container ->
                    if (containerView !== container) {
                        onContainerReady(container)
                    }
                }
            )

            val density = LocalDensity.current
            val keyboardOpen = WindowInsets.ime.getBottom(density) > 0
            val searchBottomPadding = if (keyboardOpen) 12.dp else 80.dp
            var floatingSearchSawKeyboard by remember { mutableStateOf(false) }

            LaunchedEffect(showFloatingSearch, keyboardOpen) {
                when {
                    !showFloatingSearch -> floatingSearchSawKeyboard = false
                    keyboardOpen -> floatingSearchSawKeyboard = true
                    floatingSearchSawKeyboard -> {
                        updateContactsSearch("")
                        updateContactsFloatingSearchVisible(false)
                    }
                }
            }

            ContactsFloatingSearchBar(
                query = searchQuery,
                onQueryChange = ::updateContactsFloatingSearch,
                backdrop = backdrop,
                visible = showFloatingSearch,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .imePadding()
                    .navigationBarsPadding()
                    .padding(start = 12.dp, end = 16.dp, bottom = searchBottomPadding)
            )

            ContactsBottomChrome(
                contactCount = contactCount,
                backdrop = backdrop,
                onOpenHome = ::goToHomeScreen,
                onOpenSettings = ::goToSettingsScreen,
                onOpenNotes = ::goToNotesScreen,
                onAddContact = { allContactsFragment?.showAddContactBottomSheet() },
                onImportContacts = { allContactsFragment?.showImportConfirmationDialog() }
            )
        }
    }
}

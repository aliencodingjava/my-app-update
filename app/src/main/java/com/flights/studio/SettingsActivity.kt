package com.flights.studio

import android.animation.ObjectAnimator
import android.content.Intent
import android.graphics.Rect
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.animation.DecelerateInterpolator
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.ImageView
import androidx.activity.OnBackPressedCallback
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.content.edit
import androidx.core.graphics.toColorInt
import androidx.core.view.WindowCompat
import androidx.preference.PreferenceManager
import com.flights.studio.databinding.ActivityScrollingSettingsBinding
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.snackbar.Snackbar

@Suppress("DEPRECATION")
class SettingsActivity : LocaleActivity() {

    private lateinit var binding: ActivityScrollingSettingsBinding
    private lateinit var userPrefs: UserPreferencesManager
    private val settingsSearchQuery = mutableStateOf("")
    private val searchSheetVisible = mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)

        binding = ActivityScrollingSettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        userPrefs = UserPreferencesManager(this)
        rememberFirstInstallDate()
        scheduleUpdateCheckWorker()

        showModernSettingsScreen()
        setupBackNavigation()
    }

    override fun onPostResume() {
        super.onPostResume()

        if (com.flights.studio.ui.AppLanguageManager.consumeBlinkNext()) {
            val content = findViewById<View>(R.id.content_frame) ?: return
            content.setLayerType(View.LAYER_TYPE_HARDWARE, null)
            content.alpha = 0f
            content.animate()
                .alpha(1f)
                .setDuration(140)
                .withEndAction { content.setLayerType(View.LAYER_TYPE_NONE, null) }
                .start()
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                goToHomeScreen(finishCurrent = true)
                true
            }

            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun rememberFirstInstallDate() {
        val prefs = PreferenceManager.getDefaultSharedPreferences(this)
        if (prefs.getLong(PREF_INSTALLATION_DATE, 0L) == 0L) {
            prefs.edit {
                putLong(PREF_INSTALLATION_DATE, System.currentTimeMillis())
            }
        }
    }

    private fun setupBackNavigation() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                finish()
                overridePendingTransition(R.anim.enter_animation, R.anim.exit_animation)
            }
        })
    }

    private fun showModernSettingsScreen() {
        binding.contentFrame.removeAllViews()
        binding.contentFrame.addView(
            ComposeView(this).apply {
                setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
                setContent {
                    val appThemePreset = AppThemeStore.rememberPreset(this@SettingsActivity)
                    FlightsTheme(
                        profileBackdropStyle = ProfileBackdropStyle.Auto,
                        appThemePreset = appThemePreset
                    ) {
                        ModernSettingsScreen(
                            searchQuery = settingsSearchQuery.value,
                            onOpenHome = {
                                openMainPage(MainActivity.PAGE_HOME)
                            },
                            onOpenSoftwareUpdate = {
                                startActivityWithTransition(
                                    Intent(this@SettingsActivity, SoftwareUpdateActivity::class.java)
                                )
                            },
                            onOpenAppIcon = {
                                startActivityWithTransition(
                                    Intent(this@SettingsActivity, AppIconPickerActivity::class.java)
                                )
                            },
                            onOpenLiquidGlass = {
                                startActivityWithTransition(
                                    Intent(this@SettingsActivity, LiquidGlassSettingsActivity::class.java)
                                )
                            },
                            onOpenNotifications = {
                                SignUpBottomSheetDialogFragment()
                                    .show(supportFragmentManager, "SignUpBottomSheet")
                            },
                            onOpenRateUs = {
                                RateUsDialogFragment().show(supportFragmentManager, "RateUsDialog")
                            },
                            onOpenCardDrawer = { cardId ->
                                startActivityWithTransition(
                                    Intent(this@SettingsActivity, WebviewflightActivity::class.java)
                                        .putExtra("start_card", cardId)
                                )
                            },
                            onOpenNotes = {
                                openMainPage(MainActivity.PAGE_NOTES)
                            },
                            onOpenContacts = {
                                openMainPage(MainActivity.PAGE_CONTACTS)
                            },
                            onShareApp = ::shareApp,
                            onOpenSearch = ::openSearchView,
                            onOpenQrCode = {
                                startActivityWithTransition(
                                    Intent(this@SettingsActivity, QRCodeComposeActivity::class.java)
                                )
                            },
                            onOpenProfile = {
                                startActivityWithTransition(
                                    Intent(this@SettingsActivity, ProfileDetailsComposeActivity::class.java)
                                )
                            },
                            searchSheetVisible = searchSheetVisible.value
                        )
                    }
                }
            },
            ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        )
    }

    private fun openSearchView() {
        searchSheetVisible.value = true
        val parentView = findViewById<ViewGroup>(android.R.id.content)
        val rootLayout = LayoutInflater.from(this)
            .inflate(R.layout.dialog_search_view, parentView, false) as CoordinatorLayout
        val searchView = rootLayout.findViewById<androidx.appcompat.widget.SearchView>(
            R.id.material_search_view
        )

        configureSearchView(searchView)

        val searchDialog = BottomSheetDialog(this).apply {
            setContentView(rootLayout)
            window?.setBackgroundDrawableResource(android.R.color.transparent)
            window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
        }
        searchDialog.setOnDismissListener {
            searchSheetVisible.value = false
        }

        searchDialog.setOnShowListener {
            val bottomSheet =
                searchDialog.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
            bottomSheet?.let { configureSearchSheet(it, rootLayout) }
            showKeyboard(searchView)
        }

        searchView.setOnQueryTextListener(object :
            androidx.appcompat.widget.SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                query?.let { performSearch(it) }
                searchDialog.dismiss()
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                filterSettings(newText.orEmpty())
                return true
            }
        })

        searchDialog.show()

        ObjectAnimator.ofFloat(searchView, View.TRANSLATION_Y, 0f, 30f).apply {
            duration = 200
            interpolator = DecelerateInterpolator()
            start()
        }
    }

    private fun configureSearchView(searchView: androidx.appcompat.widget.SearchView) {
        searchView.isIconified = false
        searchView.requestFocus()

        searchView.findViewById<View>(androidx.appcompat.R.id.search_plate)
            ?.setBackgroundColor(android.graphics.Color.TRANSPARENT)

        searchView.findViewById<EditText>(androidx.appcompat.R.id.search_src_text)?.apply {
            setTextColor(android.graphics.Color.GRAY)
            setHintTextColor("#B3FFFFFF".toColorInt())
        }

        searchView.findViewById<ImageView>(androidx.appcompat.R.id.search_close_btn)?.apply {
            setImageResource(R.drawable.baseline_close_24)
            visibility = View.VISIBLE
            layoutParams = layoutParams.apply {
                width = 90
                height = 90
            }
            scaleType = ImageView.ScaleType.CENTER_INSIDE
            setBackgroundResource(R.drawable.custom_clear_button)
            setColorFilter(android.graphics.Color.BLACK, android.graphics.PorterDuff.Mode.SRC_IN)
            setOnClickListener {
                searchView.setQuery("", false)
                searchView.requestFocus()
                showKeyboard(searchView)
            }
        }
    }

    private fun configureSearchSheet(bottomSheet: View, rootLayout: CoordinatorLayout) {
        val behavior = BottomSheetBehavior.from(bottomSheet)
        behavior.isHideable = true
        behavior.skipCollapsed = true

        rootLayout.viewTreeObserver.addOnGlobalLayoutListener {
            val rect = Rect()
            rootLayout.getWindowVisibleDisplayFrame(rect)
            val screenHeight = rootLayout.rootView.height
            val keyboardHeight = screenHeight - rect.bottom

            behavior.peekHeight = if (keyboardHeight > KEYBOARD_VISIBLE_THRESHOLD_PX) {
                (screenHeight - keyboardHeight - 10).coerceAtMost(screenHeight / 3)
            } else {
                screenHeight / 4
            }
        }
    }

    private fun showKeyboard(view: View) {
        val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        imm.showSoftInput(view, InputMethodManager.SHOW_IMPLICIT)
    }

    private fun filterSettings(query: String) {
        settingsSearchQuery.value = query
    }

    private fun performSearch(query: String) {
        Snackbar.make(
            findViewById(android.R.id.content),
            getString(R.string.settings_searching_template, query),
            Snackbar.LENGTH_SHORT
        ).show()
    }

    private fun shareApp() {
        val versionName = runCatching {
            packageManager.getPackageInfo(packageName, 0).versionName
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

    private fun goToHomeScreen(finishCurrent: Boolean = false) {
        openMainPage(MainActivity.PAGE_HOME, finishCurrent)
    }

    private fun openMainPage(page: Int, finishCurrent: Boolean = true) {
        startActivityWithTransition(
            Intent(this, MainActivity::class.java)
                .putExtra(MainActivity.EXTRA_START_PAGE, page)
        )
        if (finishCurrent) finish()
    }

    private fun startActivityWithTransition(intent: Intent) {
        startActivity(intent)
        overridePendingTransition(R.anim.enter_animation, R.anim.exit_animation)
    }




    private fun scheduleUpdateCheckWorker() {
        AppUpdateCheckWorker.schedule(this)
    }

    private companion object {
        const val PREF_INSTALLATION_DATE = "installation_date"
        const val KEYBOARD_VISIBLE_THRESHOLD_PX = 300
    }

}

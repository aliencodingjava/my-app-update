package com.flights.studio

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.annotation.RequiresApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.fragment.app.FragmentActivity
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class MainActivity : FragmentActivity() {

    private lateinit var firebaseAnalytics: FirebaseAnalytics

    companion object {
        private const val TAG_MAIN = "MainActivity"
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Log.d(TAG_MAIN, "onCreate() START, savedInstanceState=$savedInstanceState")
        val openLogin = intent.getBooleanExtra(EXTRA_OPEN_LOGIN, false)

        if (openLogin) {
            startActivity(Intent(this, ProfileDetailsComposeActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            })
            finish()
            return
        }

        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, window.decorView).apply {
            hide(WindowInsetsCompat.Type.statusBars()) // ✅ hides time/battery/wifi
            systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }

        firebaseAnalytics = FirebaseAnalytics.getInstance(this)
        Log.d(TAG_MAIN, "FirebaseAnalytics initialized")

        val activitySelf = this

        setContent {
            Log.d(TAG_MAIN, "setContent: root composition ENTER")

            MaterialTheme {
                Log.d(TAG_MAIN, "MaterialTheme: composition ENTER")

                val context = LocalContext.current

                // ---- counts we will SHOW in the bottom sheet ----
                var notesCountForSheet by remember { mutableIntStateOf(0) }
                var contactsCountForSheet by remember { mutableIntStateOf(0) }

                // --- state: menu bottom sheet visibility ---
                var showMenuSheet by remember { mutableStateOf(false) }

                fun actuallyExitApp() {
                    Log.d(TAG_MAIN, "actuallyExitApp() -> finishAffinity + finishAndRemoveTask")
                    finishAffinity()
                    finishAndRemoveTask()
                }

                fun loadNotesCount(): Int {
                    Log.d(TAG_MAIN, "loadNotesCount() called")
                    val cachedSize = NotesCacheManager.cachedNotes.size
                    if (cachedSize > 0) return cachedSize

                    val prefs = context.getSharedPreferences("notes_prefs", MODE_PRIVATE)
                    val notesJson = prefs.getString("notes_list", null)
                    if (!notesJson.isNullOrEmpty()) {
                        val type = object : TypeToken<MutableList<String>>() {}.type
                        val savedNotes: MutableList<String> = try {
                            Gson().fromJson(notesJson, type)
                        } catch (e: Exception) {
                            Log.e(TAG_MAIN, "loadNotesCount(): JSON parse failed: ${e.message}", e)
                            mutableListOf()
                        }
                        return savedNotes.size
                    }
                    return 0
                }

                fun loadContactsCount(): Int {
                    Log.d(TAG_MAIN, "loadContactsCount() called")
                    val prefs = context.getSharedPreferences("contacts_data", MODE_PRIVATE)
                    val json = prefs.getString("contacts", null) ?: return 0

                    return try {
                        val arr = Gson().fromJson(json, Array<AllContact>::class.java)
                        arr?.size ?: 0
                    } catch (e: Exception) {
                        Log.e(TAG_MAIN, "loadContactsCount(): JSON parse failed: ${e.message}", e)
                        0
                    }
                }

                fun openMenuSheet() {
                    Log.d(TAG_MAIN, "openMenuSheet() called")

                    notesCountForSheet = loadNotesCount().also {
                        Log.d(TAG_MAIN, "openMenuSheet(): notesCountForSheet=$it")
                    }
                    contactsCountForSheet = loadContactsCount().also {
                        Log.d(TAG_MAIN, "openMenuSheet(): contactsCountForSheet=$it")
                    }

                    showMenuSheet = true
                }

                fun closeMenuSheet() {
                    Log.d(TAG_MAIN, "closeMenuSheet() called")
                    showMenuSheet = false
                }

                fun openFullScreenImages(currentCamUrl: String) {
                    Log.d(TAG_MAIN, "openFullScreenImages(currentCamUrl=$currentCamUrl)")

                    fun base(u: String) = u.substringBefore("?")

                    val ts = System.currentTimeMillis()

                    val curb =
                        "https://www.jacksonholeairport.com/wp-content/uploads/webcams/parking-curb.jpg?v=$ts"
                    val north =
                        "https://www.jacksonholeairport.com/wp-content/uploads/webcams/parking-north.jpg?v=$ts"
                    val south =
                        "https://www.jacksonholeairport.com/wp-content/uploads/webcams/parking-south.jpg?v=$ts"

                    val all = listOf(curb, north, south)
                    val currentBase = base(currentCamUrl)
                    val first = all.firstOrNull { base(it) == currentBase } ?: curb
                    val ordered = listOf(first) + all.filter { it != first }

                    val sheet = FullScreenImageBottomSheet.newInstance(
                        ordered[0], ordered[1], ordered[2]
                    )
                    sheet.show(activitySelf.supportFragmentManager, "FullScreenImageBottomSheet")
                }

                // ✅ Back = close menu if open, else exit immediately (no dialog)
                BackHandler {
                    Log.d(TAG_MAIN, "BackHandler: showMenuSheet=$showMenuSheet")
                    if (showMenuSheet) closeMenuSheet() else actuallyExitApp()
                }

                FlightsBackdropScaffold { globalBackdrop, buttonsBackdrop ->

                    HomeScreenRouteContent(
                        backdrop = globalBackdrop,
                        openFullScreenImages = { camUrl -> openFullScreenImages(camUrl) },
                        openMenuSheet = { openMenuSheet() },
                        triggerRefreshNow = { newUrl ->
                            Log.d(TAG_MAIN, "triggerRefreshNow(newUrl=$newUrl)")
                        },
                        exitApp = {
                            Log.d(TAG_MAIN, "HomeScreenRouteContent -> exitApp()")
                            actuallyExitApp()
                        },

                    )

                    FlightsMenuLiquidSheetModal(
                        backdrop = buttonsBackdrop,
                        visible = showMenuSheet,
                        onDismissRequest = { closeMenuSheet() },
                        notesCount = notesCountForSheet,
                        contactsCount = contactsCountForSheet
                    )
                }

                Log.d(TAG_MAIN, "MaterialTheme: composition EXIT")
            }

            Log.d(TAG_MAIN, "setContent: root composition EXIT")
        }

        Log.d(TAG_MAIN, "onCreate() END")
    }
}

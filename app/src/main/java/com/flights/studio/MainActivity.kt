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
        firebaseAnalytics = FirebaseAnalytics.getInstance(this)
        Log.d(TAG_MAIN, "FirebaseAnalytics initialized")

        // we need this to show fragment sheets later
        val activitySelf = this

        setContent {
            Log.d(TAG_MAIN, "setContent: root composition ENTER")

            MaterialTheme {
                Log.d(TAG_MAIN, "MaterialTheme: composition ENTER")

                val context = LocalContext.current
                Log.d(TAG_MAIN, "MaterialTheme: LocalContext.current = $context")

                // ---- counts we will SHOW in the bottom sheet ----
                var notesCountForSheet by remember { mutableIntStateOf(0) }
                var contactsCountForSheet by remember { mutableIntStateOf(0) }

                // --- state: exit confirmation glass dialog ---
                var showExitDialog by remember { mutableStateOf(false) }

                // --- state: menu bottom sheet visibility ---
                var showMenuSheet by remember { mutableStateOf(false) }

                fun requestExitApp() {
                    Log.d(TAG_MAIN, "requestExitApp() -> showExitDialog=true")
                    showExitDialog = true
                }

                fun actuallyExitApp() {
                    Log.d(TAG_MAIN, "actuallyExitApp() -> finishAffinity + finishAndRemoveTask")
                    finishAffinity()
                    finishAndRemoveTask()
                }

                // helper to load latest notes count (same logic you already had)
                fun loadNotesCount(): Int {
                    Log.d(TAG_MAIN, "loadNotesCount() called")
                    val cachedSize = NotesCacheManager.cachedNotes.size
                    if (cachedSize > 0) {
                        Log.d(TAG_MAIN, "loadNotesCount(): using cached=${cachedSize}")
                        return cachedSize
                    }

                    val prefs = context.getSharedPreferences("notes_prefs", MODE_PRIVATE)
                    val notesJson = prefs.getString("notes_list", null)
                    if (!notesJson.isNullOrEmpty()) {
                        val type = object : TypeToken<MutableList<String>>() {}.type
                        val savedNotes: MutableList<String> = try {
                            Gson().fromJson(notesJson, type)
                        } catch (e: Exception) {
                            Log.e(TAG_MAIN, "loadNotesCount(): failed to parse JSON: ${e.message}", e)
                            mutableListOf()
                        }
                        val size = savedNotes.size
                        Log.d(TAG_MAIN, "loadNotesCount(): loaded from prefs, size=$size")
                        return size
                    }
                    Log.d(TAG_MAIN, "loadNotesCount(): no data found, returning 0")
                    return 0
                }

                // helper to load latest contacts count (same logic you already had)
                fun loadContactsCount(): Int {
                    Log.d(TAG_MAIN, "loadContactsCount() called")
                    val prefs = context.getSharedPreferences("contacts_data", MODE_PRIVATE)
                    val json = prefs.getString("contacts", null) ?: run {
                        Log.d(TAG_MAIN, "loadContactsCount(): no contacts JSON, returning 0")
                        return 0
                    }
                    return try {
                        val arr = Gson().fromJson(json, Array<AllContact>::class.java)
                        val size = arr?.size ?: 0
                        Log.d(TAG_MAIN, "loadContactsCount(): parsed contacts, size=$size")
                        size
                    } catch (e: Exception) {
                        Log.e(TAG_MAIN, "loadContactsCount(): failed to parse JSON: ${e.message}", e)
                        0
                    }
                }

                // open bottom sheet menu
                fun openMenuSheet() {
                    Log.d(TAG_MAIN, "openMenuSheet() called")
                    // refresh counts *right before* opening the sheet
                    notesCountForSheet = loadNotesCount().also {
                        Log.d(TAG_MAIN, "openMenuSheet(): notesCountForSheet=$it")
                    }
                    contactsCountForSheet = loadContactsCount().also {
                        Log.d(TAG_MAIN, "openMenuSheet(): contactsCountForSheet=$it")
                    }
                    showMenuSheet = true
                    Log.d(TAG_MAIN, "openMenuSheet(): showMenuSheet=true")
                }

                fun closeMenuSheet() {
                    Log.d(TAG_MAIN, "closeMenuSheet() called, showMenuSheet=false")
                    showMenuSheet = false
                }

                fun openFullScreenImages(currentCamUrl: String) {
                    Log.d(TAG_MAIN, "openFullScreenImages(currentCamUrl=$currentCamUrl)")

                    // helper: base URL without query
                    fun base(u: String) = u.substringBefore("?")

                    val ts = System.currentTimeMillis()
                    Log.d(TAG_MAIN, "openFullScreenImages: ts=$ts")

                    // build the 3 canonical URLs with a fresh cache-buster
                    val curb  = "https://www.jacksonholeairport.com/wp-content/uploads/webcams/parking-curb.jpg?v=$ts"
                    val north = "https://www.jacksonholeairport.com/wp-content/uploads/webcams/parking-north.jpg?v=$ts"
                    val south = "https://www.jacksonholeairport.com/wp-content/uploads/webcams/parking-south.jpg?v=$ts"

                    val all = listOf(curb, north, south)
                    val currentBase = base(currentCamUrl)
                    val first = all.firstOrNull { base(it) == currentBase } ?: curb

                    // put the current one first, then the remaining two
                    val ordered = listOf(first) + all.filter { it != first }

                    Log.d(
                        TAG_MAIN,
                        "openFullScreenImages: ordered[0]=${ordered[0]}, [1]=${ordered[1]}, [2]=${ordered[2]}"
                    )

                    val sheet = FullScreenImageBottomSheet.newInstance(
                        ordered[0], ordered[1], ordered[2]
                    )
                    Log.d(TAG_MAIN, "openFullScreenImages: showing FullScreenImageBottomSheet")
                    sheet.show(activitySelf.supportFragmentManager, "FullScreenImageBottomSheet")
                }

                BackHandler {
                    Log.d(
                        TAG_MAIN,
                        "BackHandler: showMenuSheet=$showMenuSheet, showExitDialog=$showExitDialog"
                    )
                    if (showMenuSheet) {
                        closeMenuSheet()
                    } else {
                        requestExitApp()
                    }
                }

                // Provide the shared LayerBackdrop for all glass
                FlightsBackdropScaffold { globalBackdrop, buttonsBackdrop ->

                    Log.d(TAG_MAIN, "FlightsBackdropScaffold: content lambda invoked")

                    // ---------- 1. MAIN HOME CONTENT ----------
                    HomeScreenRouteContent(
                        backdrop = globalBackdrop, // ✅ HOME uses NORMAL background glass
                        openFullScreenImages = { camUrlFromScreen ->
                            Log.d(TAG_MAIN, "HomeScreenRouteContent -> openFullScreenImages($camUrlFromScreen)")
                            openFullScreenImages(camUrlFromScreen)
                        },
                        openMenuSheet = {
                            Log.d(TAG_MAIN, "HomeScreenRouteContent -> openMenuSheet()")
                            openMenuSheet()
                        },
                        triggerRefreshNow = { newUrl ->
                            Log.d(TAG_MAIN, "HomeScreenRouteContent -> triggerRefreshNow(newUrl=$newUrl)")
                        },
                        finishApp = {
                            Log.d(TAG_MAIN, "HomeScreenRouteContent -> finishApp()")
                            requestExitApp()
                        },

                        showExitDialog = showExitDialog,
                        onDismissExit = {
                            Log.d(TAG_MAIN, "ExitLiquidDialog: onDismissExit()")
                            showExitDialog = false
                        },
                        onConfirmExit = {
                            Log.d(TAG_MAIN, "ExitLiquidDialog: onConfirmExit()")
                            showExitDialog = false
                            actuallyExitApp()

                        }
                    )

                    // ---------- 3.in main activity MENU BOTTOM SHEET MODAL ----------
                    FlightsMenuLiquidSheetModal(
                        backdrop = buttonsBackdrop,   // ✅ ✅ ✅ BOTTOM SHEET NOW REFLECTS BUTTONS
                        visible = showMenuSheet,
                        onDismissRequest = {
                            Log.d(TAG_MAIN, "FlightsMenuLiquidSheetModal: onDismissRequest()")
                            closeMenuSheet()
                        },
                        notesCount = notesCountForSheet,
                        contactsCount = contactsCountForSheet
                    )
                }

                Log.d(TAG_MAIN, "MaterialTheme: composition EXIT (end of setContent block)")
            }

            Log.d(TAG_MAIN, "setContent: root composition EXIT")
        }

        Log.d(TAG_MAIN, "onCreate() END")
    }
}

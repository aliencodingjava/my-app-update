package com.flights.studio

import android.os.Build
import android.os.Bundle
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
import com.kyant.backdrop.backdrops.LayerBackdrop

class MainActivity : FragmentActivity() {

    private lateinit var firebaseAnalytics: FirebaseAnalytics

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        WindowCompat.setDecorFitsSystemWindows(window, false)
        firebaseAnalytics = FirebaseAnalytics.getInstance(this)

        // we need this to show fragment sheets later
        val activitySelf = this

        setContent {
            MaterialTheme {

                val context = LocalContext.current

                // ---- counts we will SHOW in the bottom sheet ----
                var notesCountForSheet by remember { mutableIntStateOf(0) }
                var contactsCountForSheet by remember { mutableIntStateOf(0) }

                // --- state: exit confirmation glass dialog ---
                var showExitDialog by remember { mutableStateOf(false) }

                // --- state: menu bottom sheet visibility ---
                var showMenuSheet by remember { mutableStateOf(false) }

                fun requestExitApp() {
                    showExitDialog = true
                }

                fun actuallyExitApp() {
                    finishAffinity()
                    finishAndRemoveTask()
                }

                // helper to load latest notes count (same logic you already had)
                fun loadNotesCount(): Int {
                    val cachedSize = NotesCacheManager.cachedNotes.size
                    if (cachedSize > 0) return cachedSize

                    val prefs = context.getSharedPreferences("notes_prefs", MODE_PRIVATE)
                    val notesJson = prefs.getString("notes_list", null)
                    if (!notesJson.isNullOrEmpty()) {
                        val type = object : TypeToken<MutableList<String>>() {}.type
                        val savedNotes: MutableList<String> = try {
                            Gson().fromJson(notesJson, type)
                        } catch (_: Exception) {
                            mutableListOf()
                        }
                        return savedNotes.size
                    }
                    return 0
                }

                // helper to load latest contacts count (same logic you already had)
                fun loadContactsCount(): Int {
                    val prefs = context.getSharedPreferences("contacts_data", MODE_PRIVATE)
                    val json = prefs.getString("contacts", null) ?: return 0
                    return try {
                        val arr = Gson().fromJson(json, Array<AllContact>::class.java)
                        arr?.size ?: 0
                    } catch (_: Exception) {
                        0
                    }
                }

                // open bottom sheet menu
                fun openMenuSheet() {
                    // refresh counts *right before* opening the sheet
                    notesCountForSheet = loadNotesCount()
                    contactsCountForSheet = loadContactsCount()
                    showMenuSheet = true
                }

                fun closeMenuSheet() {
                    showMenuSheet = false
                }


                fun openFullScreenImages(currentCamUrl: String) {
                    // helper: base URL without query
                    fun base(u: String) = u.substringBefore("?")

                    val ts = System.currentTimeMillis()

                    // build the 3 canonical URLs with a fresh cache-buster
                    val curb  = "https://www.jacksonholeairport.com/wp-content/uploads/webcams/parking-curb.jpg?v=$ts"
                    val north = "https://www.jacksonholeairport.com/wp-content/uploads/webcams/parking-north.jpg?v=$ts"
                    val south = "https://www.jacksonholeairport.com/wp-content/uploads/webcams/parking-south.jpg?v=$ts"

                    val all = listOf(curb, north, south)

                    // find which one matches the current camera (ignore any existing query on the incoming URL)
                    val currentBase = base(currentCamUrl)
                    val first = all.firstOrNull { base(it) == currentBase } ?: curb

                    // put the current one first, then the remaining two
                    val ordered = listOf(first) + all.filter { it != first }

                    val sheet = FullScreenImageBottomSheet.newInstance(
                        ordered[0], ordered[1], ordered[2]
                    )
                    sheet.show(activitySelf.supportFragmentManager, "FullScreenImageBottomSheet")
                }

                BackHandler {
                    if (showMenuSheet) {
                        closeMenuSheet()
                    } else {
                        requestExitApp()
                    }
                }

                // Provide the shared LayerBackdrop for all glass
                FlightsBackdropScaffold { backdrop: LayerBackdrop ->

                    // ---------- 1. MAIN HOME CONTENT ----------
                    HomeScreenRouteContent(
                        backdrop = backdrop,
                        openFullScreenImages = { camUrlFromScreen -> openFullScreenImages(camUrlFromScreen) },
                        openMenuSheet = { openMenuSheet() },
                        triggerRefreshNow = { /* no-op */ },
                        finishApp = { requestExitApp() },

                        // ✅ use the state you declared above
                        showExitDialog = showExitDialog,
                        onDismissExit = { showExitDialog = false },                // ✅ close on cancel/outside tap
                        onConfirmExit = {
                            showExitDialog = false                                 // ✅ close first (optional)
                            actuallyExitApp()
                        }
                    )



                    // ---------- 3. MENU BOTTOM SHEET MODAL ----------
                    FlightsMenuLiquidSheetModal(
                        backdrop = backdrop,
                        visible = showMenuSheet,
                        onDismissRequest = {
                            closeMenuSheet()
                        },
                        notesCount = notesCountForSheet,
                        contactsCount = contactsCountForSheet
                    )
                }
            }
        }
    }
}

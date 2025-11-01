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
import android.widget.ImageView
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.graphics.toColorInt
import com.flights.studio.databinding.ActivityAllContactsBinding
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.navigationrail.NavigationRailView

@Suppress("DEPRECATION", "OVERRIDE_DEPRECATION")
class AllContactsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAllContactsBinding
//    private var selectedContactPosition: Int = -1
    private var allContactsFragment: AllContactsFragment? = null // Reference to the fragment
    private var isSearchDialogVisible = false
    private var currentSearchView: androidx.appcompat.widget.SearchView? = null
    private var wasFilteringBeforeDismiss = false



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_all_contacts)

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                when {
                    // Case 1: Search dialog is open and query isn't empty → clear search
                    isSearchDialogVisible && currentSearchView?.query?.isNotEmpty() == true -> {
                        currentSearchView?.setQuery("", false)
                        allContactsFragment?.filterContacts("") // Reset list
                        wasFilteringBeforeDismiss = false
                    }

                    // Case 2: Search dialog was dismissed but results still filtered → show all
                    !isSearchDialogVisible && wasFilteringBeforeDismiss -> {
                        allContactsFragment?.filterContacts("") // Show all contacts
                        wasFilteringBeforeDismiss = false
                    }

                    // ✅ Case 3: Nothing left → finish the activity and apply exit animation
                    else -> {
                        finish()
                        overridePendingTransition(R.anim.enter_animation, R.anim.exit_animation)
                    }
                }
            }
        })


        window.decorView.alpha = 1.0f

        binding = ActivityAllContactsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.navAddContact?.setOnClickListener {
            allContactsFragment?.showAddContactBottomSheet()
        }

        // Load fragment correctly and store reference
        if (savedInstanceState == null) {
            allContactsFragment = AllContactsFragment()
            supportFragmentManager.beginTransaction()
                .replace(R.id.container, allContactsFragment!!)
                .commit()
        } else {
            allContactsFragment =
                supportFragmentManager.findFragmentById(R.id.container) as? AllContactsFragment

        }


        val navRail = findViewById<NavigationRailView>(R.id.navigation_rail)
        navRail.menu.findItem(R.id.nav_all_contacts).isChecked = true

        navRail.setOnItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.nav_home -> {
                    goToHomeScreen()
                    true
                }

                R.id.nav_contacts -> {
                    goToContactScreen()
                    true
                }

                R.id.nav_all_contacts -> {
                    // Stay in `AllContactsActivity`
                    true
                }

                R.id.nav_settings -> {
                    goToSettingsScreen()
                    true
                }

                R.id.openAddNoteScreen -> {
                    startActivity(Intent(this, AllNotesActivity::class.java))
                    overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
                    finish()
                    true
                }


                R.id.nav_import_contacts -> {
                    allContactsFragment?.showImportConfirmationDialog() // ✅ Fix: Call "Import Contacts"
                    true
                }

                R.id.action_search -> {
                    openSearchView() // ✅ Open Full-Screen Search

                    true
                }

                else -> false
            }
        }
    }

    // NEW: MainActivity::class.java (Compose home / dashboard)
    private fun goToHomeScreen() {
        startActivity(Intent(this, MainActivity::class.java))
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        finish()
    }

    private fun goToContactScreen() {
        startActivity(Intent(this, Contact::class.java))
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
    }

    private fun goToSettingsScreen() {
        startActivity(Intent(this, SettingsActivity::class.java))
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
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


    private fun openSearchView() {
        val parentView = findViewById<ViewGroup>(android.R.id.content)

        // ✅ Inflate `dialog_search_view.xml` properly
        val inflater = LayoutInflater.from(this)
        val rootLayout = inflater.inflate(
            R.layout.dialog_search_all_contacts,
            parentView,
            false
        ) as CoordinatorLayout

        val searchView = rootLayout.findViewById<androidx.appcompat.widget.SearchView>(R.id.material_search_view)
        currentSearchView = searchView
        isSearchDialogVisible = true


        // ✅ Ensure SearchView is ready
        searchView.isIconified = false // ✅ Expand it immediately
        searchView.requestFocus() // ✅ Show keyboard on open

        // ✅ Modify SearchView Appearance
        val searchPlate = searchView.findViewById<View>(androidx.appcompat.R.id.search_plate)
        searchPlate.setBackgroundColor(android.graphics.Color.TRANSPARENT) // ✅ Remove underline

        val searchText =
            searchView.findViewById<android.widget.EditText>(androidx.appcompat.R.id.search_src_text)
        searchText.setTextColor(android.graphics.Color.GRAY) // ✅ Adjust text color
        searchText.setHintTextColor("#B3FFFFFF".toColorInt()) // ✅ Hint color

        val searchDialog = BottomSheetDialog(this).apply {
            setContentView(rootLayout)
            window?.setBackgroundDrawableResource(android.R.color.transparent)
            window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
        }

        val bottomSheet =
            searchDialog.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)

        bottomSheet?.let {
            val behavior = BottomSheetBehavior.from(it)
            behavior.isHideable = true  // Allow dismissing by swiping down
            behavior.skipCollapsed = true  // Prevent it from jumping between states

            // Dynamically adjust BottomSheet height based on keyboard position
            rootLayout.viewTreeObserver.addOnGlobalLayoutListener {
                val rect = Rect()
                rootLayout.getWindowVisibleDisplayFrame(rect)
                val screenHeight = rootLayout.rootView.height
                val keyboardHeight = screenHeight - rect.bottom

                if (keyboardHeight > 300) { // If keyboard is open
                    val maxHeight =
                        screenHeight - keyboardHeight - 10 // Limit height to stay above the keyboard
                    behavior.peekHeight =
                        maxHeight.coerceAtMost(screenHeight / 12) // Set height to max 1/3 of screen
                } else {
                    behavior.peekHeight = screenHeight / 4 // Default height when keyboard is closed
                }
            }
        }

        searchDialog.show()

        searchDialog.setOnDismissListener {
            isSearchDialogVisible = false
            currentSearchView = null
        }

        val animator = ObjectAnimator.ofFloat(searchView, "translationY", 0f, 30f).apply {
            duration = 200
            interpolator = DecelerateInterpolator()
        }
        animator.start()

        searchView.setOnQueryTextListener(object :
            androidx.appcompat.widget.SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                query?.let { performSearch(it) }
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                newText?.let {
                    wasFilteringBeforeDismiss = it.isNotEmpty() // 🔥 Track filter state
                    allContactsFragment?.filterContacts(it)
                }
                return true
            }
        })


        val closeButton =
            searchView.findViewById<ImageView>(androidx.appcompat.R.id.search_close_btn)

        closeButton?.let {
            it.setImageResource(R.drawable.baseline_close_24) // ✅ Custom icon
            it.visibility = View.VISIBLE

            val layoutParams = it.layoutParams
            layoutParams.width = 90
            layoutParams.height = 90
            it.layoutParams = layoutParams
            it.scaleType = ImageView.ScaleType.CENTER_INSIDE
            it.setBackgroundResource(R.drawable.custom_clear_button)
            it.setColorFilter(android.graphics.Color.BLACK, android.graphics.PorterDuff.Mode.SRC_IN)

            // ✅ Fix: Ensure SearchView Stays Active & Keyboard Stays Open
            it.setOnClickListener {
                searchView.setQuery("", false) // ✅ Clears the text but keeps focus
                searchView.requestFocus() // ✅ Keeps SearchView active

                // ✅ Show keyboard immediately again
                val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
                imm.showSoftInput(searchView, InputMethodManager.SHOW_IMPLICIT)
            }
        }


    }


    private fun performSearch(query: String) {
        val fragment =
            supportFragmentManager.findFragmentById(R.id.container) as? AllContactsFragment
        fragment?.filterContacts(query)
    }



}

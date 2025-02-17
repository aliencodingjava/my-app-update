package com.flights.studio

import android.Manifest
import android.animation.ObjectAnimator
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Rect
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.animation.DecelerateInterpolator
import android.view.inputmethod.InputMethodManager
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.content.ContextCompat
import com.flights.studio.databinding.ActivityAllContactsBinding
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.navigationrail.NavigationRailView

@Suppress("DEPRECATION", "OVERRIDE_DEPRECATION")
class AllContactsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAllContactsBinding
    private var selectedContactPosition: Int = -1
    private var allContactsFragment: AllContactsFragment? = null // Reference to the fragment


    // Media picker for Android 14+ (Selected Photos Access)
    private val pickMediaLauncher =
        registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri: Uri? ->
            if (uri != null) {
                updateContactPhoto(uri) // Call with the selected URI
            } else {
                Toast.makeText(this, "No image selected", Toast.LENGTH_SHORT).show()
            }
        }


    // Permission launcher for Android 13 and below
    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                pickImageLauncher.launch("image/*")
            } else {
                Toast.makeText(this, "Storage permission denied", Toast.LENGTH_SHORT).show()
            }
        }

    // Image picker for Android 13 and below
    private val pickImageLauncher =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            if (uri != null) {
                updateContactPhoto(uri) // Call with the selected URI
            } else {
                Toast.makeText(this, "Image selection canceled", Toast.LENGTH_SHORT).show()
            }
        }



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_all_contacts)



        binding = ActivityAllContactsBinding.inflate(layoutInflater)
        setContentView(binding.root)

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


//            supportActionBar?.setDisplayShowTitleEnabled(false)
//            setSupportActionBar(findViewById(R.id.toolbar))
//        supportActionBar?.setDisplayHomeAsUpEnabled(true)
//        supportActionBar?.setHomeButtonEnabled(true)
//
//        val drawable = AppCompatResources.getDrawable(this, R.drawable.layered_arrow)
//        supportActionBar?.setHomeAsUpIndicator(drawable)


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

                R.id.nav_add_contact -> {
                    allContactsFragment?.showAddContactBottomSheet() // ✅ Fix: Call "Add Contact"
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

    private fun goToHomeScreen() {
        startActivity(Intent(this, SplashActivity::class.java))
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        finish()
    }

    private fun goToContactScreen() {
        startActivity(Intent(this, Contact::class.java))
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        finish()
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

    override fun onBackPressed() {
        super.onBackPressed()
        goToContactScreen()
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

        // ✅ Get the SearchView from XML
        val searchView =
            rootLayout.findViewById<androidx.appcompat.widget.SearchView>(R.id.material_search_view)

        // ✅ Ensure SearchView is ready
        searchView.isIconified = false // ✅ Expand it immediately
        searchView.requestFocus() // ✅ Show keyboard on open

        // ✅ Modify SearchView Appearance
        val searchPlate = searchView.findViewById<View>(androidx.appcompat.R.id.search_plate)
        searchPlate.setBackgroundColor(android.graphics.Color.TRANSPARENT) // ✅ Remove underline

        val searchText =
            searchView.findViewById<android.widget.EditText>(androidx.appcompat.R.id.search_src_text)
        searchText.setTextColor(android.graphics.Color.GRAY) // ✅ Adjust text color
        searchText.setHintTextColor(android.graphics.Color.parseColor("#B3FFFFFF")) // ✅ Hint color

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

        val animator = ObjectAnimator.ofFloat(searchView, "translationY", 0f, 30f).apply {
            duration = 200
            interpolator = DecelerateInterpolator()
        }
        animator.start()


        searchView.setOnQueryTextListener(object :
            androidx.appcompat.widget.SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                query?.let { performSearch(it) } // ✅ This is where it is used!
                searchDialog.dismiss()
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                newText?.let { allContactsFragment?.filterContacts(it) }
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


    /**
     * Launch the appropriate image picker based on Android version.
     */
    fun launchImagePicker(position: Int) {
        selectedContactPosition = position

        when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE -> {
                // Android 14+: Use PickVisualMedia for Selected Photos Access
                pickMediaLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
            }

            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> {
                // Android 13: Request READ_MEDIA_IMAGES permission
                if (ContextCompat.checkSelfPermission(
                        this,
                        Manifest.permission.READ_MEDIA_IMAGES
                    ) == PackageManager.PERMISSION_GRANTED
                ) {
                    pickImageLauncher.launch("image/*")
                } else {
                    requestPermissionLauncher.launch(Manifest.permission.READ_MEDIA_IMAGES)
                }
            }

            else -> {
                // Android 12 and below: Request READ_EXTERNAL_STORAGE permission
                if (ContextCompat.checkSelfPermission(
                        this,
                        Manifest.permission.READ_EXTERNAL_STORAGE
                    ) == PackageManager.PERMISSION_GRANTED
                ) {
                    pickImageLauncher.launch("image/*")
                } else {
                    requestPermissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
                }
            }
        }
    }


    /**
     * Update the contact's photo with the selected URI.
     */
    private fun updateContactPhoto(uri: Uri) {
        val fragment =
            supportFragmentManager.findFragmentById(R.id.container) as? AllContactsFragment
        fragment?.updateContactPhoto(selectedContactPosition, uri)
    }


}

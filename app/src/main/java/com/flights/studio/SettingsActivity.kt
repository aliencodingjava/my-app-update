package com.flights.studio

import android.Manifest
import android.animation.ObjectAnimator
import android.animation.PropertyValuesHolder
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.app.Dialog
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Rect
import android.graphics.drawable.GradientDrawable
import android.net.ConnectivityManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.animation.DecelerateInterpolator
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.annotation.RequiresApi
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import androidx.core.graphics.toColorInt
import androidx.lifecycle.lifecycleScope
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.bumptech.glide.Glide
import com.flights.studio.databinding.ActivityScrollingSettingsBinding
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.card.MaterialCardView
import com.google.android.material.navigationrail.NavigationRailView
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedInputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit


@Suppress("DEPRECATION", "OVERRIDE_DEPRECATION")
class SettingsActivity : LocaleActivity() {
    companion object {
        private const val REQUEST_CODE_PERMISSIONS = 101
        private const val PREF_INSTALLATION_DATE = "installation_date"
        private const val REQUEST_STORAGE_PERMISSION = 1001

    }

    private var currentApkUrl: String? = null
    private lateinit var networkConnectivityHelper: NetworkConnectivityHelper
    private lateinit var binding: ActivityScrollingSettingsBinding
    private lateinit var userPrefs: UserPreferencesManager



    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private val permissions = arrayOf(
        Manifest.permission.WRITE_EXTERNAL_STORAGE,
        Manifest.permission.POST_NOTIFICATIONS
    )

    @RequiresApi(Build.VERSION_CODES.VANILLA_ICE_CREAM)
    @Suppress("DEPRECATION")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityScrollingSettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        userPrefs = UserPreferencesManager(this)
        networkConnectivityHelper = NetworkConnectivityHelper(this)

        val apkUrl = intent.getStringExtra("apkUrl")
        Log.d("SettingsActivity", "Received APK URL: $apkUrl")

        if (apkUrl != null) {
            showUpdateBottomSheet(apkUrl)
        }
        // Schedule the worker
        scheduleUpdateCheckWorker()
        // App was updated, so save the current date as the update date
        val currentDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        saveUpdateDate(this, currentDate)

        // Check if the installation date is already saved in SharedPreferences
        val prefs = PreferenceManager.getDefaultSharedPreferences(this)
        val installationDate = prefs.getLong(PREF_INSTALLATION_DATE, 0L)


        // If installation date is not saved, save the current date as the installation date
        if (installationDate == 0L) {
            prefs.edit {
                val newCurrentDate =
                    System.currentTimeMillis() // Renamed variable to newCurrentDate
                putLong(PREF_INSTALLATION_DATE, newCurrentDate)
            }
        }

        checkAndRequestPermissions()

        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.content_frame, SettingsFragment())
                .commitNow()
        }

        // 5) Wire up the NavigationRail
        val navRail = findViewById<NavigationRailView>(R.id.navigation_rail)
        navRail.menu.findItem(R.id.nav_settings).isChecked = true
        navRail.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home     -> { goToHomeScreen();      true }
                R.id.nav_contacts -> { goToContactScreen();   true }
                R.id.nav_settings -> true  // stay here
                R.id.nav_search   -> { openSearchView();      true }
                R.id.nav_qr_code  -> { openQRCodeScanner();   true }
                R.id.nav_feedback -> { showFeedbackDialog();  true }
                R.id.nav_info     -> { showItemListDialog();  true }
                else              -> false
            }
        }
        refreshRailAvatar()


        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                finish()
                overridePendingTransition(R.anim.enter_animation, de.dlyt.yanndroid.samsung.R.anim.abc_tooltip_exit)
            }
        })


    }

    override fun onPostResume() {
        super.onPostResume()

        if (com.flights.studio.ui.AppLanguageManager.consumeBlinkNext()) {
            val content = findViewById<View>(R.id.content_frame) ?: return
            // hardware layer for smooth fade (optional)
            content.setLayerType(View.LAYER_TYPE_HARDWARE, null)
            content.alpha = 0f
            content.animate()
                .alpha(1f)
                .setDuration(140)
                .withEndAction { content.setLayerType(View.LAYER_TYPE_NONE, null) }
                .start()
        }
    }


    private fun refreshRailAvatar() {
        val navRail = findViewById<NavigationRailView>(R.id.navigation_rail)
        val header  = navRail.headerView!!
        val imgView = header.findViewById<ImageView>(R.id.iconImage)
        val initialsTv = header.findViewById<TextView>(R.id.iconInitials)

        userPrefs.getUserPhotoUri()?.let { uri ->
            initialsTv.visibility = View.GONE
            imgView.visibility     = View.VISIBLE

            Glide.with(this)
                .load(uri)
                .centerCrop()    // ← fill the card’s rounded rect
                .into(imgView)
        } ?: run {
            imgView.visibility     = View.GONE
            initialsTv.visibility = View.VISIBLE

            val initials = userPrefs.userName
                .orEmpty()
                .split(Regex("\\s+"))
                .filter { it.isNotBlank() }
                .map { it[0].uppercaseChar() }
                .take(2)
                .joinToString("")

            if (initials.isNotEmpty()) {
                initialsTv.text = initials
            } else {
                // fallback logo
                initialsTv.visibility = View.GONE
                imgView.visibility     = View.VISIBLE
                imgView.setImageResource(R.drawable.rt_text_logo_jac)
            }

            (initialsTv.background as? GradientDrawable)
                ?.setColor(ContextCompat.getColor(this, R.color.box_alert_update))
        }

        header.setOnClickListener {
            startActivity(Intent(this, ProfileDetailsActivity::class.java))
        }
    }
    override fun onResume() {
        super.onResume()
        refreshRailAvatar()
    }


    private fun openSearchView() {
        val parentView = findViewById<ViewGroup>(android.R.id.content)

        // ✅ Inflate `dialog_search_view.xml`
        val inflater = LayoutInflater.from(this)
        val rootLayout = inflater.inflate(R.layout.dialog_search_view, parentView, false) as CoordinatorLayout

        // ✅ Get the SearchView from XML
        val searchView = rootLayout.findViewById<androidx.appcompat.widget.SearchView>(R.id.material_search_view)

        // ✅ Make sure SearchView is ready
        searchView.isIconified = false // ✅ Expand it immediately
        searchView.requestFocus() // ✅ Show keyboard on open

        // ✅ Remove underline from SearchView
        val searchPlate = searchView.findViewById<View>(androidx.appcompat.R.id.search_plate)
        searchPlate.setBackgroundColor(android.graphics.Color.TRANSPARENT)


        val searchText = searchView.findViewById<android.widget.EditText>(androidx.appcompat.R.id.search_src_text)
        searchText.setTextColor(android.graphics.Color.GRAY) // ✅ Adjust text color
        searchText.setHintTextColor("#B3FFFFFF".toColorInt()) // ✅ Hint color

        val searchDialog = BottomSheetDialog(this).apply {
            setContentView(rootLayout)
            window?.setBackgroundDrawableResource(android.R.color.transparent)
            window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
        }

        val bottomSheet = searchDialog.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)

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
                    val maxHeight = screenHeight - keyboardHeight - 10 // Limit height to stay above the keyboard
                    behavior.peekHeight = maxHeight.coerceAtMost(screenHeight / 12) // Set height to max 1/3 of screen
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

        // ✅ Handle Search Queries
        searchView.setOnQueryTextListener(object : androidx.appcompat.widget.SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                query?.let { performSearch(it) } // ✅ Call your search function
                searchDialog.dismiss()
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                newText?.let { filterSettings(it) }
                return true
            }
        })

        val closeButton = searchView.findViewById<ImageView>(androidx.appcompat.R.id.search_close_btn)

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


    private fun goToHomeScreen() {
        val intent = Intent(this, SplashActivity::class.java)
        startActivity(intent)
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
    }

    private fun goToContactScreen() {
        val intent = Intent(this, Contact::class.java)
        startActivity(intent)
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
    }

    private fun openQRCodeScanner() {
        val intent = Intent(this, QRCodeActivity::class.java)
        startActivity(intent)
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
    }
    private fun showFeedbackDialog() {
        val feedbackBottomSheet = FeedbackBottomSheet()
        feedbackBottomSheet.show(supportFragmentManager, "FeedbackBottomSheet")
    }

    private fun filterSettings(query: String) {
        val fragment =
            supportFragmentManager.findFragmentById(R.id.content_frame) as? SettingsFragment
        fragment?.filterPreferences(query) ?: Log.e(
            "SettingsActivity",
            "SettingsFragment not found"
        )
    }

    private fun performSearch(query: String) {
        Snackbar.make(
            findViewById(android.R.id.content),
            "Searching for: $query",
            Snackbar.LENGTH_SHORT
        ).show()
    }


    private fun saveUpdateDate(context: Context, updateDate: String) {
        val sharedPreferences = context.getSharedPreferences("MyPrefs", MODE_PRIVATE)
        sharedPreferences.edit {
        putString("updateDate", updateDate)
        }
    }


    // If the selected item is the home button (back arrow)
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                val intent = Intent(this, SplashActivity::class.java)
                startActivity(intent)
                finish()
                overridePendingTransition(R.anim.enter_animation, R.anim.exit_animation)

                true
            }

            else -> super.onOptionsItemSelected(item)
        }
    }



    private fun showItemListDialog() {
        val emailItems = getEmailItems()
        ItemListDialogFragment.newInstance(emailItems).show(supportFragmentManager, "dialog")
    }

    // Function to filter out only email addresses
    private fun getEmailItems(): List<String> {
        return listOf(
            getString(R.string._307_733_7682), // Phone number (not an email)
            getString(R.string.info_jhairport_org), // Email
            getString(R.string.info_jhflight_org), // Email
            getString(R.string.fbo_general_manager), // Email
            getString(R.string.fbo_customer_service_manager), // Email
            getString(R.string.airport_operations), // Email
            getString(R.string.human_resources), // Email
            getString(R.string.lost_and_found), // Email
            getString(R.string.communications_customer_experience) // Email
        ).filter { it.contains('@') } // Filter only the email strings
    }


    @RequiresApi(Build.VERSION_CODES.VANILLA_ICE_CREAM)
    private fun checkAndRequestPermissions() {
        val permissionsToRequest = mutableListOf<String>()
        for (permission in permissions) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    permission
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                permissionsToRequest.add(permission)
            }
        }

        if (permissionsToRequest.isNotEmpty()) {
            ActivityCompat.requestPermissions(
                this,
                permissionsToRequest.toTypedArray(),
                REQUEST_CODE_PERMISSIONS
            )
        } else {
            // Permissions are already granted, you can proceed with your logic
        }
    }


    // Request permissions result handling
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray,
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_STORAGE_PERMISSION) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Retry the download after permission is granted
                currentApkUrl?.let {
                    showDownloadDialog(it) // Reattempt the download using the stored URL
                }
            } else {
                showErrorDialog("Storage permission is required to download the update.")
            }
        }
    }

    fun checkForUpdates(initialDialog: AlertDialog) {
        val gistUrl =
            "https://gist.github.com/aliencodingjava/8ef085a89b30d85e2e86fb6f148d80cb/raw/gistfile2.txt"

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val jsonText = URL(gistUrl).readText()
                val jsonObject = JSONObject(jsonText)
                val latestVersionCode = jsonObject.getInt("versionCode")
                val apkUrl = jsonObject.getString("apkUrl")
                val currentVersionCode = getAppVersionCode()
                val lastCheckedVersion = getLastCheckedVersion()

                withContext(Dispatchers.Main) {
                    initialDialog.dismiss()
                    if (latestVersionCode > currentVersionCode && latestVersionCode != lastCheckedVersion) {
                        showUpdateBottomSheet(apkUrl)
                        saveLastCheckedVersion(latestVersionCode)
                    } else {
                        showUpToDateBottomSheet()
                    }
                }
            } catch (_: Exception) {
                withContext(Dispatchers.Main) {
                    initialDialog.dismiss()
                    showUpdateErrorDialog()
                }
            }
        }
    }


    private fun showUpToDateBottomSheet() {
        // Use a FrameLayout (or any other ViewGroup) as the root for inflation
        val rootView = FrameLayout(this)

        // Inflate the layout into the FrameLayout root view
        val bottomSheetView =
            layoutInflater.inflate(R.layout.update_bottom_sheet_app_todate, rootView, false)

        // Find views within the inflated layout
        val titleView = bottomSheetView.findViewById<TextView>(R.id.app_update)
        val messageView = bottomSheetView.findViewById<TextView>(R.id.app_up_to_date)
        val okButton = bottomSheetView.findViewById<Button>(R.id.btnOk)

        titleView.text = getString(R.string.app_update_title)
        messageView.text = getString(R.string.app_up_to_date_message)

        // Create a BottomSheetDialog with the inflated view
        val bottomSheetDialog = BottomSheetDialog(this)
        bottomSheetDialog.setContentView(bottomSheetView)

        // Show the BottomSheetDialog
        bottomSheetDialog.show()

        // Handle the OK button click
        okButton.setOnClickListener {
            bottomSheetDialog.dismiss()  // Close the dialog when the button is clicked
        }
    }


    private fun showUpdateErrorDialog() {
        // Create an instance of NoInternetBottomSheetFragment
        val noInternetBottomSheetFragment = NoInternetBottomSheetFragment.newInstance()

        // Set up the callback for when the user clicks "Try Again"
        noInternetBottomSheetFragment.onTryAgainClicked = {
            // Dismiss the bottom sheet before checking for updates
            noInternetBottomSheetFragment.dismiss()

        }

        // Show the bottom sheet fragment
        noInternetBottomSheetFragment.show(supportFragmentManager, "NoInternetBottomSheetFragment")
    }


    private fun getLastCheckedVersion(): Int {
        // Implement this to retrieve the last checked version code from SharedPreferences
        return 0
    }

    private fun saveLastCheckedVersion(versionCode: Int) {
        val sharedPref = getSharedPreferences("your_shared_prefs_name", MODE_PRIVATE)
        sharedPref.edit {
            putInt("last_checked_version", versionCode)
        }
    }

    @SuppressLint("InflateParams")
    private fun showUpdateBottomSheet(apkUrl: String) {
        val bottomSheetDialog = BottomSheetDialog(this)

        // Inflate the layout and pass the root view of the BottomSheetDialog as the parent
        val bottomSheetView = layoutInflater.inflate(
            R.layout.update_bottom_sheet,
            null,  // Pass null here since we're not inflating into a view hierarchy
            false  // False here to prevent immediate attachment to parent
        )

        // Set the content view for the BottomSheetDialog
        bottomSheetDialog.setContentView(bottomSheetView)

                bottomSheetView.findViewById<TextView>(R.id.update_app_title)
                    .setText(R.string.update_app_title)

        // Set up RecyclerView
        val recyclerView = bottomSheetView.findViewById<RecyclerView>(R.id.updateRecyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)

        // Load the updates from strings.xml
        val updates = resources.getStringArray(R.array.update_messages).toList()
        recyclerView.adapter = UpdateAdapter(updates)

        bottomSheetView.findViewById<Button>(R.id.button_download).setOnClickListener {
            bottomSheetDialog.dismiss()
            showDownloadDialog(apkUrl)
        }


        bottomSheetView.findViewById<Button>(R.id.button_cancel).setOnClickListener {
            bottomSheetDialog.dismiss()
        }

        // Optional: Customize the BottomSheet's behavior (peek height, expanded state, etc.)
        val bottomSheet =
            bottomSheetDialog.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
        bottomSheet?.let {
            val behavior = BottomSheetBehavior.from(it)
            behavior.peekHeight = 600 // Adjust peek height as needed
            behavior.state = BottomSheetBehavior.STATE_EXPANDED // Set to expanded by default
        }

        // Show the BottomSheetDialog
        bottomSheetDialog.show()
    }


    private fun scheduleUpdateCheckWorker() {
        val workRequest =
            PeriodicWorkRequestBuilder<CheckForUpdatesWorker>(1, TimeUnit.DAYS).build()
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "checkForUpdates",
            ExistingPeriodicWorkPolicy.KEEP,
            workRequest
        )
    }


    private fun createDownloadUri(): Uri? {
        return try {
            val values = ContentValues().apply {
                put(MediaStore.Downloads.DISPLAY_NAME, "update_${System.currentTimeMillis()}.apk")
                put(MediaStore.Downloads.MIME_TYPE, "application/vnd.android.package-archive")
                put(
                    MediaStore.Downloads.RELATIVE_PATH,
                    "Download/"
                ) // Ensure this matches file_paths.xml
            }
            val uri = contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
            Log.d("DownloadURI", "Created URI: $uri")
            uri
        } catch (e: Exception) {
            Log.e("DownloadError", "Error creating download URI: ${e.message}", e)
            null
        }
    }


    private fun showInstallOptionBottomSheet(fileUri: Uri) {
        // Make sure progressDialog is only dismissed if it exists

        // Inflate custom bottom sheet layout with a valid root view
        val rootView = findViewById<View>(android.R.id.content) as? FrameLayout
        val bottomSheetView = layoutInflater.inflate(
            R.layout.custom_install_dialog,
            rootView,
            false // Ensure the layout is not immediately attached
        )

        val dialogTitle = bottomSheetView.findViewById<TextView>(R.id.dialogTitle)
        val dialogMessage = bottomSheetView.findViewById<TextView>(R.id.dialogMessage)
        val btnInstall = bottomSheetView.findViewById<Button>(R.id.install)
        val btnCancel = bottomSheetView.findViewById<Button>(R.id.button_cancel)

        dialogTitle.setText(R.string.download_complete_title)
        dialogMessage.text = getString(R.string.install_message_template, "")

        // Create a BottomSheetDialog
        val bottomSheetDialog = BottomSheetDialog(this)
        bottomSheetDialog.setContentView(bottomSheetView)
        bottomSheetDialog.setCancelable(false)

        // Set the background of the dialog to transparent
        bottomSheetDialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        btnInstall.setOnClickListener {
            installApk(fileUri)
            bottomSheetDialog.dismiss()
        }

        btnCancel.setOnClickListener {
            bottomSheetDialog.dismiss()
        }

        bottomSheetDialog.show()
    }


    private fun installApk(fileUri: Uri) {
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(fileUri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION) // Grant permission to the APK installer
        }

        try {
            startActivity(intent) // Launch the APK installer
        } catch (e: Exception) {
            Toast.makeText(this, "Unable to install APK: ${e.message}", Toast.LENGTH_LONG).show()
            Log.e("InstallApk", "Error: ${e.message}", e)
        }
    }


    private fun getAppVersionCode(): Long {
        val packageInfo = packageManager.getPackageInfo(packageName, 0)
        return packageInfo.longVersionCode
    }


    private fun calculateTimeLeft(totalBytesRead: Long, fileLength: Int, startTime: Long): String {
        val elapsedTime = System.currentTimeMillis() - startTime
        val downloadSpeed = totalBytesRead / (elapsedTime / 1000.0) // bytes per second
        val bytesRemaining = fileLength - totalBytesRead
        val estimatedTimeLeft = bytesRemaining / downloadSpeed // seconds

        val minutes = (estimatedTimeLeft / 60).toInt()
        val seconds = (estimatedTimeLeft % 60).toInt()
        return String.format(Locale.getDefault(), "%d:%02d", minutes, seconds)
    }


    private fun showErrorDialog(message: String) {
        AlertDialog.Builder(this)
            .setTitle("Error")
            .setMessage(message)
            .setPositiveButton("OK") { dialog, _ -> dialog.dismiss() }
            .setCancelable(false)
            .show()
    }


    private fun showDownloadDialog(apkUrl: String) {

        val dialogView = layoutInflater.inflate(
            R.layout.dialog_download_progress,
            findViewById(android.R.id.content), // Use the root view of the current activity
            false // Do not attach to the root now; the dialog will manage this
        )


        val progressBar = dialogView.findViewById<ProgressBar>(R.id.progressBar)
        val progressText = dialogView.findViewById<TextView>(R.id.progressText)
        val cardView =
            dialogView.findViewById<MaterialCardView>(R.id.animation_here)  // Find the card view
        val internetStrengthText = dialogView.findViewById<TextView>(R.id.internetStrengthText)
        val internetStrengthIcon: ImageView =
            dialogView.findViewById(R.id.internetStrengthIcon)  // Correctly refer to dialogView
        val speedTextView =
            dialogView.findViewById<TextView>(R.id.megabit_per_second) // Initialize speedTextView

        cardView.setOnClickListener {
            Toast.makeText(this, "Card clicked in Settings", Toast.LENGTH_SHORT).show()
        }
        // Check if any of the views are null
        if (internetStrengthText == null) {
            Log.e("DownloadDialog", "One or more views are not found!")
            return
        }

        val downloadDialog = Dialog(this)  // Use Dialog instead of BottomSheetDialog
        downloadDialog.setContentView(dialogView)
        downloadDialog.setCanceledOnTouchOutside(false)  // Prevent dismiss when touched outside
        downloadDialog.setCancelable(false)  // Prevent dismiss by back press or swipe

        val layoutParams = downloadDialog.window?.attributes
        layoutParams?.width = ViewGroup.LayoutParams.MATCH_PARENT
        layoutParams?.height = ViewGroup.LayoutParams.MATCH_PARENT
        downloadDialog.window?.attributes = layoutParams

        downloadDialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        // Add the card view to the container

        // Set the container as the content of the dialog
        downloadDialog.show()

        // Apply the scale animation to the entire dialog view
        ObjectAnimator.ofPropertyValuesHolder(
            dialogView,
            PropertyValuesHolder.ofFloat("scaleX", 0f, 1f),
            PropertyValuesHolder.ofFloat("scaleY", 0f, 1f)
        ).apply {
            duration = 200
            start()
        }





        // Start the download process
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                // Perform the download and get the URI for the downloaded APK
                val uri = downloadApkWithProgress(
                    apkUrl,
                    progressBar,
                    progressText,
                    speedTextView,
                    internetStrengthText,
                    internetStrengthIcon
                )

                withContext(Dispatchers.Main) {
                    // Animate the CardView (scale up first)
                    ObjectAnimator.ofFloat(cardView, "scaleX", 1f, 1.1f).apply {
                        duration = 800
                        start()
                    }
                    ObjectAnimator.ofFloat(cardView, "scaleY", 1f, 1.1f).apply {
                        duration = 500
                        start()
                    }

                    // After scaling up, apply rocket-like zoom-out
                    Handler(Looper.getMainLooper()).postDelayed({
                        ObjectAnimator.ofFloat(cardView, "scaleX", 1.1f, 0f).apply {
                            duration = 500
                            start()
                        }
                        ObjectAnimator.ofFloat(cardView, "scaleY", 1.1f, 0f).apply {
                            duration = 500
                            start()
                        }

                        Handler(Looper.getMainLooper()).postDelayed({
                            downloadDialog.dismiss()
                            showInstallOptionBottomSheet(uri)
                        }, 500)
                    }, 1500)
                }
            } catch (_: Exception) {
                withContext(Dispatchers.Main) {
                    downloadDialog.dismiss()
                    showErrorDialog("Download failed. Please try again.")
                }
            }
        }


    }

    enum class ConnectionStrength {
        STRONG, MODERATE, WEAK, NONE
    }

    fun getConnectionStrength(context: Context): ConnectionStrength {
        val cm = context.getSystemService(ConnectivityManager::class.java)
        val activeNetwork = cm?.activeNetworkInfo

        Log.d("ConnectionStrength", "Active Network: ${activeNetwork?.typeName}") // Debug log

        return if (activeNetwork != null) {
            when (activeNetwork.type) {
                ConnectivityManager.TYPE_WIFI -> {
                    Log.d("ConnectionStrength", "Wi-Fi detected") // Debug log
                    ConnectionStrength.STRONG // Wi-Fi
                }
                ConnectivityManager.TYPE_MOBILE -> {
                    Log.d("ConnectionStrength", "Cellular detected") // Debug log
                    ConnectionStrength.MODERATE // Cellular
                }
                else -> {
                    Log.d("ConnectionStrength", "Other connection type") // Debug log
                    ConnectionStrength.NONE
                }
            }
        } else {
            Log.d("ConnectionStrength", "No active network") // Debug log
            ConnectionStrength.WEAK
        }
    }


    @SuppressLint("SetTextI18n", "DefaultLocale")
    private suspend fun downloadApkWithProgress(
        apkUrl: String,
        progressBar: ProgressBar,
        progressText: TextView,
        speedTextView: TextView,
        internetStrengthText: TextView,
        internetStrengthIcon: ImageView,
    ): Uri {
        val connection = URL(apkUrl).openConnection() as HttpURLConnection
        connection.requestMethod = "GET"
        connection.connect()

        if (connection.responseCode != HttpURLConnection.HTTP_OK) {
            throw IOException("Server returned HTTP ${connection.responseCode}")
        }

        val fileLength = connection.contentLength
        val uri = createDownloadUri() ?: throw IOException("Failed to create URI for download.")

        val input = BufferedInputStream(connection.inputStream)
        val output = contentResolver.openOutputStream(uri)
            ?: throw IOException("Failed to open output stream.")

        val data = ByteArray(4096)
        var total: Long = 0
        var count: Int
        val startTime = System.currentTimeMillis()

        while (input.read(data).also { count = it } != -1) {
            total += count
            val progress = (total * 100 / fileLength).toInt()
            val timeLeft = calculateTimeLeft(total, fileLength, startTime)

            // Calculate download speed (MB/sec)
            val elapsedTime = (System.currentTimeMillis() - startTime) / 1000.0 // in seconds
            val downloadSpeed = if (elapsedTime > 0) total / 1024.0 / 1024.0 / elapsedTime else 0.0 // MB/sec
            val totalDownloaded = total / 1024.0 / 1024.0 // in MB

            // Check if the device is on Wi-Fi or Cellular connection
            val connectionStrength = getConnectionStrength(internetStrengthText.context)

            // Update progress, download speed, total downloaded size, and update the speed icon on the main thread
            withContext(Dispatchers.Main) {
                progressBar.progress = progress
                progressText.text = "$progress% ($timeLeft left)"
                speedTextView.text = String.format("%.2f MB (%.2f MB/sec)", totalDownloaded, downloadSpeed)

                // Check if the connection is Wi-Fi or Cellular and update the icon accordingly
                if (connectionStrength == ConnectionStrength.STRONG || connectionStrength == ConnectionStrength.MODERATE) {
                    if (isWifiConnected(internetStrengthText.context)) {
                        // Wi-Fi connection, update the Wi-Fi icon
                        updateWifiSpeedIcon(downloadSpeed, internetStrengthText, internetStrengthIcon)
                    } else {
                        // Mobile connection, update the Cellular icon
                        updateCellularSpeedIcon(downloadSpeed, internetStrengthText, internetStrengthIcon)
                    }
                }
            }
            output.write(data, 0, count)
        }

        output.flush()
        output.close()
        input.close()

        return uri
    }

    // Function to check if device is connected to Wi-Fi
    private fun isWifiConnected(context: Context): Boolean {
        val cm = context.getSystemService(ConnectivityManager::class.java)
        val activeNetwork = cm?.activeNetworkInfo
        return activeNetwork?.type == ConnectivityManager.TYPE_WIFI
    }




    private fun updateWifiSpeedIcon(
        downloadSpeed: Double,
        internetStrengthText: TextView,
        internetStrengthIcon: ImageView,
    ) {
        val (strengthLabel, drawableRes) = when {
            downloadSpeed < 0.5 -> Pair("Very Weak", R.drawable.network_wifi_1_bar_24dp_ffffff_fill0_wght400_grad0_opsz24)        // 1 bar (Very Weak)
            downloadSpeed < 1.0 -> Pair("Weak", R.drawable.network_wifi_2_bar_24dp_ffffff_fill0_wght400_grad0_opsz24)            // 2 bars (Weak)
            downloadSpeed < 2.0 -> Pair("Moderate", R.drawable.network_wifi_3_bar_24dp_ffffff_fill0_wght400_grad0_opsz24)        // 3 bars (Moderate)
            downloadSpeed < 3.0 -> Pair("Good", R.drawable.signal_wifi_4_bar_24dp_ffffff_fill0_wght400_grad0_opsz24)            // 4 bars (Good)
            else -> Pair("Strong", R.drawable.signal_wifi_4_bar_24dp_ffffff_fill0_wght400_grad0_opsz24)                     // 4 bars (Strong)
        }

        // Set the text and icon for Wi-Fi
        internetStrengthText.text = internetStrengthText.context.getString(R.string.internet_strength, strengthLabel)
        internetStrengthIcon.setImageResource(drawableRes)
        internetStrengthIcon.visibility = View.VISIBLE  // Show the Wi-Fi icon
    }

    private fun updateCellularSpeedIcon(
        downloadSpeed: Double,
        internetStrengthText: TextView,
        internetStrengthIcon: ImageView,
    ) {
        val (strengthLabel, drawableRes) = when {
            downloadSpeed < 0.2 -> Pair("No Internet", R.drawable.signal_cellular_connected_no_internet_0_bar_24dp_ffffff_fill0_wght400_grad0_opsz24)  // 0 bars
            downloadSpeed < 0.5 -> Pair("Weak", R.drawable.signal_cellular_1_bar_24dp_ffffff_fill0_wght400_grad0_opsz24)         // 1 bar
            downloadSpeed < 1.0 -> Pair("Moderate", R.drawable.signal_cellular_2_bar_24dp_ffffff_fill0_wght400_grad0_opsz24)     // 2 bars
            downloadSpeed < 1.5 -> Pair("Good", R.drawable.signal_cellular_3_bar_24dp_ffffff_fill0_wght400_grad0_opsz24)         // 3 bars
            downloadSpeed < 2.0 -> Pair("Strong", R.drawable.signal_cellular_4_bar_24dp_ffffff_fill0_wght400_grad0_opsz24)       // 4 bars
            downloadSpeed < 5.0 -> Pair("Very Strong", R.drawable._g_mobiledata_24dp_ffffff_fill0_wght400_grad0_opsz24)  // 5 bars (Very Strong)
            else -> Pair("5G", R.drawable._g_24dp_ffffff_fill0_wght400_grad0_opsz24)                                     // 5G icon for speeds > 5MB
        }

        // Set the text and icon for cellular network
        internetStrengthText.text = internetStrengthText.context.getString(R.string.internet_strength, strengthLabel)
        internetStrengthIcon.setImageResource(drawableRes)
        internetStrengthIcon.visibility = View.VISIBLE  // Show the cellular icon
    }





}
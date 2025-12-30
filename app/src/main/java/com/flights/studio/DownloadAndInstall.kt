package com.flights.studio

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import androidx.core.graphics.drawable.toDrawable
import androidx.lifecycle.lifecycleScope
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
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

class DownloadAndInstall : AppCompatActivity() {

    companion object {
        private const val REQUEST_CODE_PERMISSIONS = 101
        private const val PREF_INSTALLATION_DATE = "installation_date"
        private const val REQUEST_STORAGE_PERMISSION = 1001

    }
    private var currentApkUrl: String? = null
    private lateinit var progressDialog: AlertDialog
    private lateinit var progressBar: ProgressBar
    private lateinit var textViewTimeLeft: TextView
    private lateinit var textViewPercentage: TextView
    private lateinit var networkConnectivityHelper: NetworkConnectivityHelper



    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private val permissions = arrayOf(
        Manifest.permission.WRITE_EXTERNAL_STORAGE,
        Manifest.permission.POST_NOTIFICATIONS
    )

    @RequiresApi(Build.VERSION_CODES.VANILLA_ICE_CREAM)
    @Suppress("DEPRECATION")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_scrolling_settings)

        // Assuming you have a reference to your initial dialog
        val initialDialog = AlertDialog.Builder(this)
            .setMessage("Checking for updates...")
            .setCancelable(false)
            .create()

        // Call the checkForUpdates function
        checkForUpdates(initialDialog)
        // Initialize your networkConnectivityHelper
        networkConnectivityHelper = NetworkConnectivityHelper(this)

//        val apkUrl = intent.getStringExtra("apkUrl")
//        Log.d("SettingsActivity", "Received APK URL: $apkUrl")
//
//        if (apkUrl != null) {
//            val apkUrl = intent.getStringExtra("apkUrl")
//            if (apkUrl != null) {
//                showUpdateBottomSheet(apkUrl, emptyList())
//            }
//        }
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


    }



    private fun saveUpdateDate(context: Context, updateDate: String) {
        val sharedPreferences = context.getSharedPreferences("MyPrefs", MODE_PRIVATE)
        sharedPreferences.edit {
            putString("updateDate", updateDate)
        }
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
                    downloadAndInstallApk(it) // Reattempt the download using the stored URL
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

                // ✅ NEW: read updates list from JSON
                val updatesJson = jsonObject.optJSONArray("updates")
                val updates = buildList {
                    if (updatesJson != null) {
                        for (i in 0 until updatesJson.length()) {
                            val o = updatesJson.getJSONObject(i)
                            add(
                                UpdateBlock(
                                    title = o.optString("title"),
                                    body = o.optString("body")
                                )
                            )
                        }
                    }
                }

                val currentVersionCode = getAppVersionCode()
                val lastCheckedVersion = getLastCheckedVersion()

                withContext(Dispatchers.Main) {
                    initialDialog.dismiss()

                    if (latestVersionCode > currentVersionCode && latestVersionCode != lastCheckedVersion) {
                        showUpdateBottomSheet(apkUrl, updates) // ✅ pass updates
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
    private fun showUpdateBottomSheet(apkUrl: String, updates: List<UpdateBlock>) {
        val bottomSheetDialog = BottomSheetDialog(this)
        val root = FrameLayout(this)

        val bottomSheetView = layoutInflater.inflate(
            R.layout.update_bottom_sheet,
            root,
            false
        )


        bottomSheetDialog.setContentView(bottomSheetView)

        bottomSheetView.findViewById<TextView>(R.id.update_app_title)
            .setText(R.string.update_app_title)

        val recyclerView = bottomSheetView.findViewById<RecyclerView>(R.id.updateRecyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)

        // ✅ NO STRINGS.XML, NO FAKE FUNCTION
        recyclerView.adapter = UpdateAdapter(updates)

        bottomSheetView.findViewById<Button>(R.id.button_download).setOnClickListener {
            bottomSheetDialog.dismiss()
            downloadAndInstallApk(apkUrl)
        }

        bottomSheetView.findViewById<Button>(R.id.button_cancel).setOnClickListener {
            bottomSheetDialog.dismiss()
        }

        bottomSheetDialog.show()

        val bottomSheet = bottomSheetDialog.findViewById<View>(
            com.google.android.material.R.id.design_bottom_sheet
        )
        bottomSheet?.let {
            val behavior = BottomSheetBehavior.from(it)
            behavior.peekHeight = 600
            behavior.state = BottomSheetBehavior.STATE_EXPANDED
        }

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

    private fun downloadAndInstallApk(apkUrl: String) {
        // Show the progress dialog
        currentApkUrl = apkUrl
        showDownloadProgressDialog()

        // Check for storage permission
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM &&
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                REQUEST_STORAGE_PERMISSION
            )
            dismissProgressDialog(success = false)
            return
        }

        lifecycleScope.launch(Dispatchers.IO) {
            // 1) Suspend & get current connectivity
            val hasInternet = networkConnectivityHelper.isInternetAvailableFast()
            if (!hasInternet) {
                withContext(Dispatchers.Main) {
                    showErrorDialog("No internet connection.")
                    dismissProgressDialog(success = false)
                }
                return@launch
            }

            try {
                Log.d("DownloadAPK", "Starting download from: $apkUrl")

                val connection = URL(apkUrl).openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.connect()

                Log.d("DownloadAPK", "HTTP Response Code: ${connection.responseCode}")

                if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                    when (connection.responseCode) {
                        HttpURLConnection.HTTP_NOT_FOUND ->
                            throw IOException("File not found on server (404)")
                        HttpURLConnection.HTTP_INTERNAL_ERROR ->
                            throw IOException("Server error (500)")
                        else ->
                            throw IOException("HTTP response code: ${connection.responseCode}")
                    }
                }

                val fileLength = connection.contentLength
                Log.d("DownloadAPK", "File Length: $fileLength bytes")
                if (fileLength <= 0) {
                    throw IOException("Invalid file length: $fileLength")
                }

                val input = BufferedInputStream(connection.inputStream)
                val uri = createDownloadUri()
                Log.d("DownloadAPK", "File URI created: $uri")

                uri?.let { fileUri ->
                    val output = contentResolver.openOutputStream(fileUri)
                        ?: throw IOException("Failed to open output stream.")
                    Log.d("DownloadAPK", "Output stream opened successfully.")

                    val data = ByteArray(4096)
                    var total: Long = 0
                    var count: Int
                    val startTime = System.currentTimeMillis()

                    while (input.read(data).also { count = it } != -1) {
                        total += count
                        val progress = (total * 100 / fileLength).toInt()
                        val timeLeft = calculateTimeLeft(total, fileLength, startTime)
                        withContext(Dispatchers.Main) {
                            updateProgressDialog(progress, timeLeft)
                        }
                        output.write(data, 0, count)
                    }

                    output.flush()
                    output.close()
                    input.close()
                    Log.d("DownloadAPK", "Download completed successfully.")

                    withContext(Dispatchers.Main) {
                        dismissProgressDialog(success = true)
                        showInstallOptionBottomSheet(fileUri)
                    }
                } ?: throw IOException("Failed to create file URI.")
            } catch (e: IOException) {
                Log.e("DownloadError", "IOException: ${e.message}", e)
                withContext(Dispatchers.Main) {
                    dismissProgressDialog(success = false)
                    showErrorDialog("Download failed. Please try again.")
                }
            } catch (e: Exception) {
                Log.e("DownloadError", "Unexpected error: ${e.message}", e)
                withContext(Dispatchers.Main) {
                    dismissProgressDialog(success = false)
                    showErrorDialog("An unexpected error occurred.")
                }
            }
        }
    }


    private fun createDownloadUri(): Uri? {
        val contentResolver = applicationContext.contentResolver
        val values = ContentValues().apply {
            put(MediaStore.Downloads.DISPLAY_NAME, "update_${System.currentTimeMillis()}.apk")
            put(MediaStore.Downloads.MIME_TYPE, "application/vnd.android.package-archive")
            put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
        }
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
            } else {
                TODO("VERSION.SDK_INT < Q")
            }
        } catch (e: Exception) {
            Log.e("DownloadError", "Error creating download URI: ${e.message}", e)
            null
        }
    }


    private fun showInstallOptionBottomSheet(fileUri: Uri) {
        progressDialog.dismiss() // Dismiss the download progress dialog if it's showing

        // Inflate custom bottom sheet layout
        val bottomSheetView = layoutInflater.inflate(
            R.layout.custom_install_dialog,
            findViewById(R.id.bottom_sheet_container),
            false
        )
        val dialogTitle = bottomSheetView.findViewById<TextView>(R.id.dialogTitle)
        val dialogMessage = bottomSheetView.findViewById<TextView>(R.id.dialogMessage)
        val btnInstall = bottomSheetView.findViewById<Button>(R.id.install)
        val btnCancel = bottomSheetView.findViewById<Button>(R.id.button_cancel)

        dialogTitle.setText(R.string.download_complete_title)
        dialogMessage.text = getString(R.string.install_message_template, "")

        // Create a BottomSheetDialog with the custom layout
        val bottomSheetDialog = BottomSheetDialog(this)
        bottomSheetDialog.setContentView(bottomSheetView)
        bottomSheetDialog.setCancelable(false)

        // Set listeners for the buttons inside the bottom sheet dialog
        btnInstall.setOnClickListener {
            // Call the installApk function to install the downloaded APK
            installApk(fileUri)

            // Send a broadcast indicating that the download is complete
            val intent = Intent("com.flights.studio.DOWNLOAD_COMPLETE")
            intent.putExtra("fileUri", fileUri.toString())
            sendBroadcast(intent)

            bottomSheetDialog.dismiss()
        }

        btnCancel.setOnClickListener {
            bottomSheetDialog.dismiss()
        }

        // Show the custom bottom sheet dialog
        bottomSheetDialog.show()
    }


    private fun showDownloadProgressDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.progress_dialog, null)
        progressBar = dialogView.findViewById(R.id.progress_bar)
        textViewTimeLeft = dialogView.findViewById(R.id.text_view_time_left)
        textViewPercentage = dialogView.findViewById(R.id.text_view_percentage)
        progressBar.max = 100

        // Initialize textViewTimeLeft with initial text
        textViewTimeLeft.text = getString(R.string.time_left_initial)

        val dialogBuilder = AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(false)

        // Create the AlertDialog object
        progressDialog = dialogBuilder.create()

        // Apply the animation style directly to the AlertDialog
        progressDialog.window?.attributes?.windowAnimations = R.style.DialogSlideUpDown


        progressDialog.window?.setBackgroundDrawable(
            ContextCompat.getColor(this, R.color.box_alert_update).toDrawable()
        ) // For setting dialog background color programmatically

        progressDialog.show()
    }

    private fun installApk(fileUri: Uri) {
        // minSdk >= 26 → direct check
        if (!packageManager.canRequestPackageInstalls()) {
            Toast.makeText(
                this,
                "Enable “Install unknown apps” then press Install again.",
                Toast.LENGTH_LONG
            ).show()

            val settingsIntent = Intent(android.provider.Settings.ACTION_SECURITY_SETTINGS)
            startActivity(settingsIntent)

            startActivity(settingsIntent)
            return
        }

        val installIntent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(fileUri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            clipData = ClipData.newUri(contentResolver, "APK", fileUri)
        }

        // Samsung / Android 13–15 safety grant
        packageManager
            .queryIntentActivities(installIntent, PackageManager.MATCH_DEFAULT_ONLY)
            .forEach {
                grantUriPermission(
                    it.activityInfo.packageName,
                    fileUri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            }

        try {
            startActivity(installIntent)
        } catch (_: ActivityNotFoundException) {
            Toast.makeText(this, "No installer found for this APK.", Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            Toast.makeText(this, "Install failed: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }



    private fun getAppVersionCode(): Long {
        val packageInfo = packageManager.getPackageInfo(packageName, 0)
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            packageInfo.longVersionCode
        } else {
            TODO("VERSION.SDK_INT < P")
        }
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
        // Step 1: Build the AlertDialog using the Builder.
        val alertDialog = AlertDialog.Builder(this)
            .setTitle("Download Error")
            .setMessage(message)
            .setPositiveButton("OK") { dialog, _ ->
                dialog.dismiss()  // Dismiss dialog when 'OK' is clicked
            }
            .setCancelable(false) // Prevent dialog from being dismissed by back button or tapping outside
            .create()

        // Step 2: Customize dialog button and other properties
        alertDialog.setOnShowListener {
            alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).apply {
                setTextColor(
                    ContextCompat.getColor(
                        this@DownloadAndInstall,
                        R.color.color_ok_update_alert
                    )
                )
            }
        }

        // Step 3: Set window animations (ensure the `DialogAnimation` style is defined in styles.xml)
        alertDialog.window?.attributes?.windowAnimations = R.style.DialogSlideUpDown


        // Step 4: Set the background drawable for the AlertDialog window.
        alertDialog.window?.setBackgroundDrawable(
            ContextCompat.getColor(
                this,
                R.color.box_alert_update
            ).toDrawable()
        )

        // Step 5: Show the dialog.
        alertDialog.show()
    }


    private fun updateProgressDialog(progress: Int, timeLeft: String) {
        progressBar.progress = progress
        textViewPercentage.text = getString(R.string.percentage_placeholder, progress)
        textViewTimeLeft.text = getString(R.string.time_left, timeLeft)

        // Change the color of the ProgressBar based on progress
        when {
            progress < 50 -> {
                progressBar.progressTintList =
                    ColorStateList.valueOf(ContextCompat.getColor(this, R.color.color_warning))
                textViewPercentage.setTextColor(ContextCompat.getColor(this, R.color.color_warning))
            }

            progress < 100 -> {
                progressBar.progressTintList =
                    ColorStateList.valueOf(ContextCompat.getColor(this, R.color.color_progress))
                textViewPercentage.setTextColor(
                    ContextCompat.getColor(
                        this,
                        R.color.color_progress
                    )
                )
            }

            else -> {
                progressBar.progressTintList =
                    ColorStateList.valueOf(ContextCompat.getColor(this, R.color.color_success))
                textViewPercentage.setTextColor(ContextCompat.getColor(this, R.color.color_success))
            }
        }
    }

    private fun dismissProgressDialog(success: Boolean) {
        if (::progressDialog.isInitialized) {
            if (success) {
                Toast.makeText(this, "Download completed!", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Download failed.", Toast.LENGTH_SHORT).show()
            }
            progressDialog.dismiss()
        }
    }


}




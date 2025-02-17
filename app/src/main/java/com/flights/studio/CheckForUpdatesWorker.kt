package com.flights.studio

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.URL

class CheckForUpdatesWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val gistUrl = "https://gist.github.com/aliencodingjava/8ef085a89b30d85e2e86fb6f148d80cb/raw/gistfile2.txt"

        return withContext(Dispatchers.IO) {
            try {
                val jsonText = URL(gistUrl).readText()
                val jsonObject = JSONObject(jsonText)
                val latestVersionCode = jsonObject.getInt("versionCode")
                val apkUrl = jsonObject.getString("apkUrl")
                val currentVersionCode = getAppVersionCode(applicationContext)
                val lastCheckedVersion = getLastCheckedVersion(applicationContext)

                if (latestVersionCode > currentVersionCode && latestVersionCode != lastCheckedVersion) {
                    // Update available, show bottom sheet
                    showUpdateBottomSheet(applicationContext, apkUrl)
                    saveLastCheckedVersion(applicationContext, latestVersionCode)
                    Result.success()
                } else {
                    // No update available, return success
                    Result.success()
                }
            } catch (_: Exception) {
                Result.failure()
            }
        }
    }

    private fun getAppVersionCode(context: Context): Long {
        val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
        return if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            packageInfo.longVersionCode // For API level 28 and above (newer version)
        } else {
            @Suppress("DEPRECATION") // Suppress the deprecation warning for older versions
            packageInfo.versionCode.toLong() // For below API level 28 (older version)
        }
    }



    private fun getLastCheckedVersion(context: Context): Int {
        val sharedPreferences = context.getSharedPreferences("your_shared_prefs_name", Context.MODE_PRIVATE)
        return sharedPreferences.getInt("last_checked_version", 0)
    }

    private fun saveLastCheckedVersion(context: Context, version: Int) {
        val sharedPreferences = context.getSharedPreferences("your_shared_prefs_name", Context.MODE_PRIVATE)
        sharedPreferences.edit().putInt("last_checked_version", version).apply()
    }

    private fun showUpdateBottomSheet(context: Context, apkUrl: String) {
        Log.d("CheckForUpdatesWorker", "Starting SettingsActivity with APK URL: $apkUrl")
        val intent = Intent(context, SettingsActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("apkUrl", apkUrl)
        }
        context.startActivity(intent)
    }

}

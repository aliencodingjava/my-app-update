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
                val lastCheckedVersion = getLastCheckedVersion(applicationContext, apkUrl)

                if (latestVersionCode > currentVersionCode && latestVersionCode != lastCheckedVersion) {
                    // Update available, show bottom sheet
                    showUpdateBottomSheet(applicationContext, apkUrl)
                    saveLastCheckedVersion(applicationContext, apkUrl, latestVersionCode)
                    Result.success()
                } else {
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
            packageInfo.longVersionCode
        } else {
            @Suppress("DEPRECATION")
            packageInfo.versionCode.toLong()
        }
    }

    private fun getLastCheckedVersion(context: Context, apkUrl: String): Int {
        val prefs = context.getSharedPreferences("update_prefs", Context.MODE_PRIVATE)
        return prefs.getInt("last_checked_version_${apkUrl.hashCode()}", 0)
    }

    private fun saveLastCheckedVersion(context: Context, apkUrl: String, version: Int) {
        val prefs = context.getSharedPreferences("update_prefs", Context.MODE_PRIVATE)
        prefs.edit().putInt("last_checked_version_${apkUrl.hashCode()}", version).apply()
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

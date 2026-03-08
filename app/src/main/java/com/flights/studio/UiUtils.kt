package com.flights.studio

import android.content.Context
import android.util.Log
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import java.io.File

object UiUtils {

    fun showNoInternetDialog(activity: AppCompatActivity, errorImageView: ImageView?) {
        Log.d("UiUtils", "No internet — showing error icon")

        errorImageView?.apply {
            alpha = 0f
            visibility = ImageView.VISIBLE
            bringToFront()
            animate().alpha(1f).setDuration(500).start()
        }

        val noInternetDialog = NoInternetBottomSheetFragment.newInstance()
        noInternetDialog.onTryAgainClicked = {
            activity.recreate()
        }
        noInternetDialog.show(activity.supportFragmentManager, "NoInternetBottomSheet")
    }
             // this function is for webview cache
    fun getFolderSize(dir: File): Long {
        var size: Long = 0
        dir.listFiles()?.forEach { file ->
            size += if (file.isDirectory) getFolderSize(file) else file.length()
        }
        return size
    }


    fun getWebStorageStats(context: Context): WebStorageStats {
        Log.d("WEBVIEW_DEBUG", "getWebStorageStats called")
        fun dirSizeMB(file: File?): Float {
            if (file == null || !file.exists()) return 0f

            var size = 0L
            file.walkTopDown().forEach {
                if (it.isFile) size += it.length()
            }

            return size / (1024f * 1024f)
        }

        val dataDir = context.dataDir
        val cacheDir = context.cacheDir

        val appWebView = File(dataDir, "app_webview")
        val chromium = File(dataDir, "app_webview/Default")
        val webCache = File(cacheDir, "WebView")

        val appWebViewSize = dirSizeMB(appWebView)
        val chromiumSize = dirSizeMB(chromium)
        val webCacheSize = dirSizeMB(webCache)
        val cacheSize = dirSizeMB(cacheDir)

        Log.e("WEBVIEW_DEBUG", "app_webview = $appWebViewSize MB")
        Log.d("WEBVIEW_DEBUG", "app_webview/Default = $chromiumSize MB")
        Log.d("WEBVIEW_DEBUG", "cacheDir/WebView = $webCacheSize MB")
        Log.d("WEBVIEW_DEBUG", "cacheDir total = $cacheSize MB")

        val webDb =
            context.getDatabasePath("webview.db").length() +
                    context.getDatabasePath("webviewCache.db").length()

        val webDbMB = webDb / (1024f * 1024f)

        return WebStorageStats(
            httpCache = webCacheSize,
            cookies = 0f,
            webDb = webDbMB
        )
    }
}

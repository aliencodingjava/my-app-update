package com.flights.studio

import android.util.Log
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity

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
}

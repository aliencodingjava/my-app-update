package com.flights.studio

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.widget.FrameLayout
import android.widget.Toast
import androidx.activity.addCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.content.FileProvider
import androidx.core.graphics.createBitmap
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

@Suppress("DEPRECATION", "OVERRIDE_DEPRECATION")
class QRCodeActivity : AppCompatActivity() {
    private val coroutineScope = CoroutineScope(Dispatchers.Main)
    private var qrCodeBitmap: Bitmap? = null // Cache the bitmap

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_qrcode)
        onBackPressedDispatcher.addCallback(this) {
            finish()
            overridePendingTransition(R.anim.enter_animation, de.dlyt.yanndroid.samsung.R.anim.abc_tooltip_exit)
        }



        setSupportActionBar(findViewById(R.id.toolbar))
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setHomeButtonEnabled(true)
        val drawable = AppCompatResources.getDrawable(this, R.drawable.layered_arrow)
        supportActionBar?.setHomeAsUpIndicator(drawable)

        // Transparent status bar
        window.decorView.systemUiVisibility = (
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
                        View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
                        View.SYSTEM_UI_FLAG_FULLSCREEN
                )
        window.statusBarColor = Color.TRANSPARENT

        val shareButton: MaterialButton = findViewById(R.id.share_friends)
        shareButton.setOnClickListener {
            coroutineScope.launch {
                if (qrCodeBitmap == null) {
                    // Show a loading indicator
                    shareButton.isEnabled = false
                    shareButton.text = "Preparing..."

                    // Generate the bitmap in the background
                    qrCodeBitmap = withContext(Dispatchers.Default) { prepareQRCodeBitmap() }

                    // Restore the UI
                    shareButton.isEnabled = true
                    shareButton.text = "Share"
                }

                // Share the cached bitmap
                qrCodeBitmap?.let { shareImage(it) }
            }
        }


    }


    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                finish()
                overridePendingTransition(R.anim.enter_animation, de.dlyt.yanndroid.samsung.R.anim.abc_tooltip_exit)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }




    // Prepare the QR code bitmap
    private fun prepareQRCodeBitmap(): Bitmap {
        val inflater = LayoutInflater.from(this)
        val rootView = FrameLayout(this) // Dummy parent to resolve layout parameters
        val qrCodeView = inflater.inflate(R.layout.qr_code, rootView, false)

        // Measure and layout the view
        qrCodeView.measure(
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
        )
        qrCodeView.layout(0, 0, qrCodeView.measuredWidth, qrCodeView.measuredHeight)

        // Capture the view as a bitmap
        return captureViewAsBitmap(qrCodeView)
    }

    // Capture the content of a View as a Bitmap
    private fun captureViewAsBitmap(view: View): Bitmap {
        val bitmap = createBitmap(view.measuredWidth, view.measuredHeight)
        val canvas = Canvas(bitmap)
        view.draw(canvas)
        return bitmap
    }

    // Save the bitmap to a temporary file and return its URI
    private fun saveBitmapToFile(bitmap: Bitmap): Uri? {
        val file = File(getExternalFilesDir("shared/qr/"), "shared_qrcode.png")
        var outputStream: FileOutputStream? = null
        return try {
            outputStream = FileOutputStream(file)
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
            outputStream.flush()
            FileProvider.getUriForFile(this, "${packageName}.fileprovider", file)
        } catch (e: IOException) {
            Log.e("QRCodeActivity", "Error saving bitmap to file: ${e.message}", e)
            null
        } finally {
            outputStream?.close()
        }
    }



    // Share the image
    private fun shareImage(bitmap: Bitmap) {
        val uri = saveBitmapToFile(bitmap)
        if (uri != null) {
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "image/png"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_TEXT, "Check out this QR code!")
            }
            startActivity(Intent.createChooser(shareIntent, "Share Image"))
        } else {
            Log.e("QRCodeActivity", "Error: Failed to save image to file.")
            runOnUiThread {
                Toast.makeText(this, "Failed to prepare the QR code for sharing. Please try again.", Toast.LENGTH_SHORT).show()
            }
        }
    }

}

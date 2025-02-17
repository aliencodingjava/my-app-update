package com.flights.studio

import android.animation.ObjectAnimator
import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import kotlinx.coroutines.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

class FeedbackDialogFragment : DialogFragment() {

    private var statusText: TextView? = null
    private var statusIndicator: View? = null
    private var isServerOnline = false
    private var serverCheckJob: Job? = null
    private val checkInterval: Long = 15000

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.dialog_feedback, container, false)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val builder = AlertDialog.Builder(requireContext())
        val view = onCreateView(layoutInflater, null, savedInstanceState)

        builder.setView(view)

        view?.let {
            initializeUI(it)
        }

        val dialog = builder.create()
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent) // Set dialog background to transparent
        return dialog
    }


    override fun onDestroyView() {
        super.onDestroyView()
        stopPeriodicServerChecks()
    }

    private fun initializeUI(view: View) {
        val submitButton: Button = view.findViewById(R.id.submitFeedbackButton)
        val feedbackEditText: EditText = view.findViewById(R.id.feedbackEditText)
        val progressBar: ProgressBar = view.findViewById(R.id.feedbackProgressBar)
        val clearButton: Button = view.findViewById(R.id.clearFeedbackButton)

        statusText = view.findViewById(R.id.statusText)
        statusIndicator = view.findViewById(R.id.statusIndicator)

        submitButton.setOnClickListener {
            val feedbackText = feedbackEditText.text.toString().trim()
            if (feedbackText.isNotEmpty()) {
                handleFeedbackSubmission(feedbackText, submitButton, clearButton, progressBar)
            } else {
                Toast.makeText(requireContext(), "Please enter your feedback.", Toast.LENGTH_SHORT).show()
            }
        }

        startPeriodicServerChecks()
    }


    private fun handleFeedbackSubmission(
        feedbackText: String,
        submitButton: Button,
        clearButton: Button,
        progressBar: ProgressBar
    ) {
        progressBar.visibility = View.VISIBLE
        submitButton.isEnabled = false
        clearButton.isEnabled = false

        ObjectAnimator.ofInt(progressBar, "progress", 0, 100).apply {
            duration = 3000
            start()
        }

        Handler(Looper.getMainLooper()).postDelayed({
            if (isServerOnline) {
                sendFeedback(feedbackText)
            } else {
                queueFeedbackLocally(feedbackText)
            }
        }, 3000)

        Handler(Looper.getMainLooper()).postDelayed({
            dismiss()
        }, 5000)
    }

    private fun startPeriodicServerChecks() {
        serverCheckJob = CoroutineScope(Dispatchers.IO).launch {
            while (isActive) {
                checkServerStatus()
                delay(checkInterval)
            }
        }
    }

    private fun stopPeriodicServerChecks() {
        serverCheckJob?.cancel()
    }

    private suspend fun checkServerStatus() {
        withContext(Dispatchers.IO) {
            // Show "checking" status
            Handler(Looper.getMainLooper()).post {
                updateStatusUI(null) // Set yellow blinking state
            }
            val client = OkHttpClient()
            val request = Request.Builder()
                .url("http://192.168.50.100:4000/health")
                .get()
                .build()

            try {
                val response = client.newCall(request).execute()
                val responseBody = response.body?.string()
                Log.d("ServerStatus", "Response Body: $responseBody")

                // Parse the JSON response properly
                val isOnline = response.isSuccessful && responseBody?.let {
                    try {
                        val json = JSONObject(it)
                        json.optString("status") == "online"  // Checking if the "status" field is "online"
                    } catch (_: Exception) {
                        false  // In case of JSON parsing error
                    }
                } == true

                withContext(Dispatchers.Main) {
                    // Process queued feedback if server is online
                    if (!isServerOnline && isOnline) {
                        processQueuedFeedback()
                    }
                    isServerOnline = isOnline
                    updateStatusUI(isServerOnline)
                }
            } catch (e: Exception) {
                Log.e("ServerStatus", "Error checking server status: ${e.message}")
                withContext(Dispatchers.Main) {
                    // Set server as offline on error
                    isServerOnline = false
                    updateStatusUI(false)
                }
            }
        }
    }


    private fun sendFeedback(feedbackText: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val client = OkHttpClient()
                val json = JSONObject().apply { put("feedback", feedbackText) }

                val requestBody = json.toString().toRequestBody("application/json; charset=utf-8".toMediaType())
                val request = Request.Builder()
                    .url("http://192.168.50.100:4000/feedback")
                    .post(requestBody)
                    .build()

                val response = client.newCall(request).execute()
                val isSuccessful = response.isSuccessful

                withContext(Dispatchers.Main) {
                    if (isSuccessful) {
                        Toast.makeText(requireContext(), "Feedback sent successfully.", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(requireContext(), "Failed to send feedback. Try again later.", Toast.LENGTH_SHORT).show()
                        queueFeedbackLocally(feedbackText)
                    }
                }
            } catch (e: Exception) {
                Log.e("Feedback", "Error sending feedback: ${e.message}")
                withContext(Dispatchers.Main) {
                    queueFeedbackLocally(feedbackText)
                }
            }
        }
    }

    private fun queueFeedbackLocally(feedbackText: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val queueFile = File(requireContext().filesDir, "feedbackQueue.json")
                val feedbackQueue = if (queueFile.exists()) {
                    JSONObject(queueFile.readText()).getJSONArray("queue")
                } else {
                    JSONArray()
                }
                feedbackQueue.put(feedbackText)
                queueFile.writeText(JSONObject().put("queue", feedbackQueue).toString())

                withContext(Dispatchers.Main) {
                    Toast.makeText(requireContext(), "Feedback queued for later.", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Log.e("QueueFeedback", "Error queuing feedback: ${e.message}")
                withContext(Dispatchers.Main) {
                    Toast.makeText(requireContext(), "Failed to queue feedback.", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun processQueuedFeedback() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val queueFile = File(requireContext().filesDir, "feedbackQueue.json")
                if (!queueFile.exists()) return@launch

                val feedbackQueue = JSONObject(queueFile.readText()).getJSONArray("queue")
                val client = OkHttpClient()

                for (i in 0 until feedbackQueue.length()) {
                    val feedbackText = feedbackQueue.getString(i)

                    val json = JSONObject().apply {
                        put("feedback", feedbackText)
                    }

                    val requestBody = json.toString().toRequestBody("application/json; charset=utf-8".toMediaType())
                    val request = Request.Builder()
                        .url("http://192.168.50.100:4000/feedback")
                        .post(requestBody)
                        .build()

                    try {
                        val response = client.newCall(request).execute()
                        if (!response.isSuccessful) {
                            Log.e("ProcessQueue", "Failed to send queued feedback: $feedbackText")
                            continue
                        }
                    } catch (e: Exception) {
                        Log.e("ProcessQueue", "Error sending queued feedback: ${e.message}")
                        break
                    }
                }
                // Clear the queue after processing
                queueFile.writeText(JSONObject().put("queue", JSONArray()).toString())

                withContext(Dispatchers.Main) {
                    Toast.makeText(requireContext(), "Queued feedback sent successfully.", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Log.e("ProcessQueue", "Error processing feedback queue: ${e.message}")
            }
        }
    }

    private fun updateStatusUI(isOnline: Boolean?) {
        when (isOnline) {
            null -> {
                // Yellow blinking indicator for "checking" state
                statusText?.text = getString(R.string.checking_status)
                statusText?.visibility = View.VISIBLE
                statusIndicator?.setBackgroundResource(R.drawable.checking_status)

                // Start blinking animation
                val animator = ObjectAnimator.ofFloat(statusIndicator, "alpha", 0.5f, 1f).apply {
                    duration = 500 // 0.5 seconds for fade-in and fade-out
                    repeatMode = ObjectAnimator.REVERSE
                    repeatCount = ObjectAnimator.INFINITE
                }
                setBlinkingYellowIndicator(animator)

                // Attach animation to the statusIndicator view tag so we can stop it later
                statusIndicator?.tag = animator
            }
            true -> {
                // Green indicator for "online" state
                statusText?.text = getString(R.string.server_online)
                statusText?.visibility = View.VISIBLE
                statusIndicator?.setBackgroundResource(R.drawable.online_indicator)

                // Stop any blinking animation
                stopBlinkingAnimation()
            }
            false -> {
                // Red indicator for "offline" state
                statusText?.text = getString(R.string.server_offline)
                statusText?.visibility = View.VISIBLE
                statusIndicator?.setBackgroundResource(R.drawable.offline_indicator)

                // Stop any blinking animation
                stopBlinkingAnimation()
            }
        }
    }


    private fun setBlinkingYellowIndicator(animator: ObjectAnimator) {
        animator.start()
        // Store the animation in the tag to be cleared later
        statusIndicator?.tag = animator
    }

    private fun stopBlinkingAnimation() {
        // Stop the blinking animation if it is running
        (statusIndicator?.tag as? ObjectAnimator)?.let {
            it.cancel()
            statusIndicator?.alpha = 1f // Reset alpha to fully visible
            statusIndicator?.tag = null // Clear the animation reference
        }
    }
}
package com.flights.studio

import android.app.Dialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment


class SignUpBottomSheetDialogFragment : BottomSheetDialogFragment() {


    // Function to validate email address
    private fun isValidEmail(email: String): Boolean {
        return android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_sign_up_bottom_sheet, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
       // setStyle(STYLE_NORMAL, R.style.CustomBottomSheetDialogTheme)


        // Initialize your views here, for example:
        val submitButton: Button = view.findViewById(R.id.submitButton)
        val firstNameEditText: EditText = view.findViewById(R.id.firstNameEditText)
        val lastNameEditText: EditText = view.findViewById(R.id.lastNameEditText)
        val emailEditText: EditText = view.findViewById(R.id.emailEditText)

        submitButton.setOnClickListener {
            // Get the text entered by the user in the EditText fields
            val firstName = firstNameEditText.text.toString()
            val lastName = lastNameEditText.text.toString()
            val email = emailEditText.text.toString()

            // Validate email address
            if (isValidEmail(email)) {
                // Compose the email body with the user's details
                val emailBody = "First Name: $firstName\nLast Name: $lastName\nEmail: $email"

                // Constructing the mailto URI including the email address, subject, and body
                val mailto = "mailto:megan.jenkins@jhairport.org" +
                        "?subject=" + Uri.encode("User Experience Feedback for JacFlights") +
                        "&body=" + Uri.encode(emailBody)

                // Creating an ACTION_VIEW intent with the mailto URI
                val emailIntent = Intent(Intent.ACTION_VIEW, mailto.toUri())

                // Attempt to launch the email application
                try {
                    startActivity(emailIntent)
                    Log.d("EmailSending", "Email sent successfully to: $email")

                    // Instead of the Handler for delay, use this:
                    val bottomSheetDialog = dialog as? BottomSheetDialog
                    val bottomSheetInternal = bottomSheetDialog?.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)


                    // Delay before starting the animation
                    bottomSheetInternal?.postDelayed({
                    }, 1000) // Adjust the delay as needed

                } catch (e: Exception) {
                    Log.e("EmailSending", "Error sending email to: $email", e)
                    Toast.makeText(requireContext(), "Error sending email.", Toast.LENGTH_LONG).show()
                }
            } else {
                // If email is not valid, show a toast message
                Toast.makeText(requireContext(), "Please enter a valid email address.", Toast.LENGTH_LONG).show()
            }
        }


    }
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState)
        dialog.setOnShowListener { dialogInterface ->
            val bottomSheetDialog = dialogInterface as BottomSheetDialog
            val bottomSheet = bottomSheetDialog.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
            bottomSheet?.let {
                BottomSheetBehavior.from(it).state = BottomSheetBehavior.STATE_EXPANDED
                it.background = ContextCompat.getDrawable(requireContext(), R.drawable.bottom_sheet_background)
            }
        }
        return dialog
    }

}

package com.flights.studio

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.floatingactionbutton.FloatingActionButton

class CallConfirmationBottomSheetFragment : BottomSheetDialogFragment() {

    companion object {
        private const val PHONE_NUMBER_KEY = "phoneNumberKey"

        fun newInstance(phoneNumber: String): CallConfirmationBottomSheetFragment {
            val args = Bundle().apply {
                putString(PHONE_NUMBER_KEY, phoneNumber)
            }
            return CallConfirmationBottomSheetFragment().apply {
                arguments = args
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_call_confirmation_bottom_sheet, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Access the FloatingActionButton using findViewById
        val fab = view.findViewById<FloatingActionButton>(R.id.btn_call)

        val phoneNumber = arguments?.getString(PHONE_NUMBER_KEY) ?: return

        // Set the text for the call message
        view.findViewById<TextView>(R.id.call_message).text =
            getString(R.string.confirm_call_message, phoneNumber)


        val btnCancel = view.findViewById<FloatingActionButton>(R.id.btn_cancel)

        // Set up the onClick listener for the call button
        fab.setOnClickListener {
            makePhoneCall(phoneNumber)
        }

        // Set up the onClick listener for the cancel button
        btnCancel.setOnClickListener {
            dismiss() // Close the bottom sheet
        }
    }

    private fun makePhoneCall(phoneNumber: String) {
        val intent = Intent(Intent.ACTION_DIAL).apply {
            data = Uri.parse("tel:$phoneNumber")
        }
        startActivity(intent)
        dismiss() // Optionally close the bottom sheet after making the call
    }
}

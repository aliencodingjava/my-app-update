package com.flights.studio

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.net.toUri
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
        savedInstanceState: Bundle?,
    ): View? {
        return inflater.inflate(R.layout.fragment_call_confirmation_bottom_sheet, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val fab = view.findViewById<FloatingActionButton>(R.id.btn_call)
        val btnCancel = view.findViewById<FloatingActionButton>(R.id.btn_cancel)
        val phoneNumber = arguments?.getString(PHONE_NUMBER_KEY) ?: return
        val messageView = view.findViewById<TextView>(R.id.call_message)

        // Dynamic message based on area code
        val message = if (phoneNumber.startsWith("307")) {
            "Do you want to call Jackson Hole Airport at $phoneNumber?"
        } else {
            getString(R.string.confirm_call_message, phoneNumber)
        }

        messageView.text = message

        fab.setOnClickListener {
            makePhoneCall(phoneNumber)
        }

        btnCancel.setOnClickListener {
            dismiss()
        }
    }


    private fun makePhoneCall(phoneNumber: String) {
        val intent = Intent(Intent.ACTION_DIAL).apply {
            data = "tel:$phoneNumber".toUri()
        }
        startActivity(intent)
        dismiss() // Optionally close the bottom sheet after making the call
    }
}

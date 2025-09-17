package com.flights.studio

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class NoInternetBottomSheetFragment : BottomSheetDialogFragment() {

    var onTryAgainClicked: (() -> Unit)? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_no_internet_bottom_sheet, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        view.findViewById<Button>(R.id.btn_try_again).setOnClickListener {
            if (onTryAgainClicked == null) {
                Log.e("NoInternetFragment", "onTryAgainClicked is null")
            } else {
                onTryAgainClicked?.invoke()
            }
            dismiss()
        }
    }

    companion object {
        fun newInstance(): NoInternetBottomSheetFragment {
            return NoInternetBottomSheetFragment()
        }
    }
}

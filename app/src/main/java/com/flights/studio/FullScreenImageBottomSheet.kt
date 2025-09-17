package com.flights.studio

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.AppCompatImageView
import androidx.core.net.toUri
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.github.chrisbanes.photoview.PhotoView
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class FullScreenImageBottomSheet : BottomSheetDialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?) = BottomSheetDialog(requireContext()).apply {
        setOnShowListener { dialog ->
            val bottomSheetDialog = dialog as BottomSheetDialog
            val bottomSheet = bottomSheetDialog.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
            bottomSheet?.let {
                val behavior = BottomSheetBehavior.from(it)
                behavior.state = BottomSheetBehavior.STATE_EXPANDED
                behavior.skipCollapsed = true
                behavior.isHideable = true
                behavior.isFitToContents = true
                behavior.isDraggable = false // ✅ Prevents dismiss by dragging
            }
        }
        setCancelable(false) // ✅ Prevents dismiss when tapping outside
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        return inflater.inflate(R.layout.activity_fullscreen_image, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val fullScreenImageView1 = view.findViewById<PhotoView>(R.id.fullscreenImageView1)
        val fullScreenImageView2 = view.findViewById<PhotoView>(R.id.fullscreenImageView2)
        val fullScreenImageView3 = view.findViewById<PhotoView>(R.id.fullscreenImageView3)
        val closeButton = view.findViewById<AppCompatImageView>(R.id.closeButton)

        val imageUrl1 = arguments?.getString("IMAGE_URL_1") ?: ""
        val imageUrl2 = arguments?.getString("IMAGE_URL_2") ?: ""
        val imageUrl3 = arguments?.getString("IMAGE_URL_3") ?: ""

        Glide.with(this)
            .load("$imageUrl1?v=${System.currentTimeMillis()}".toUri())
            .transition(DrawableTransitionOptions.withCrossFade(400))
            .into(fullScreenImageView1)

        Glide.with(this)
            .load("$imageUrl2?v=${System.currentTimeMillis()}".toUri())
            .transition(DrawableTransitionOptions.withCrossFade(400))
            .into(fullScreenImageView2)

        Glide.with(this)
            .load("$imageUrl3?v=${System.currentTimeMillis()}".toUri())
            .transition(DrawableTransitionOptions.withCrossFade(400))
            .into(fullScreenImageView3)

        closeButton.setOnClickListener { dismiss() }
    }


    companion object {
        fun newInstance(imageUrl1: String, imageUrl2: String, imageUrl3: String): FullScreenImageBottomSheet {
            val fragment = FullScreenImageBottomSheet()
            val args = Bundle().apply {
                putString("IMAGE_URL_1", imageUrl1)
                putString("IMAGE_URL_2", imageUrl2)
                putString("IMAGE_URL_3", imageUrl3)
            }
            fragment.arguments = args
            return fragment
        }
    }
}

package com.flights.studio

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.flights.studio.databinding.FragmentPhotoCarouselBinding
import com.google.android.material.carousel.CarouselLayoutManager
import com.google.android.material.carousel.CarouselSnapHelper

class PhotoCarouselFragment : Fragment() {

    private var _binding: FragmentPhotoCarouselBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPhotoCarouselBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Setup photo carousel
        val photoAdapter = PhotoAdapter(getPhotoList()) { photoUrl ->
            showFullscreenImage(photoUrl)
        }

        binding.carouselRecyclerView.apply {
            layoutManager = CarouselLayoutManager()
            val snapHelper = CarouselSnapHelper()
            snapHelper.attachToRecyclerView(this)
            adapter = photoAdapter
        }

        // Handle hiding fullscreen when clicking anywhere or on close button
        binding.fullscreenContainer.setOnClickListener {
            hideFullscreenImage()
        }

        // Access the close button and set click listener
        val closeButton = binding.closeButton
        closeButton.setOnClickListener {
            hideFullscreenImage()
        }
    }

    private fun getPhotoList(): List<String> {
        val items = mutableListOf<String>()
        val assetManager = requireContext().assets

        try {
            val photoFiles = assetManager.list("photos") ?: emptyArray()
            photoFiles.forEach { photoFileName ->
                val photoUri = "file:///android_asset/photos/$photoFileName"
                items.add(photoUri)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return items
    }

    private fun showFullscreenImage(photoUrl: String) {
        binding.fullscreenContainer.apply {
            alpha = 0f
            visibility = View.VISIBLE
            animate().alpha(1f).setDuration(300).start()
        }
        Glide.with(binding.root.context)
            .load(photoUrl)
            .transition(DrawableTransitionOptions.withCrossFade())
            .error(R.drawable.error_logo)
            .placeholder(R.drawable.placeholder)
            .into(binding.fullscreenImageView)
    }

    private fun hideFullscreenImage() {
        binding.fullscreenContainer.animate().alpha(0f).setDuration(300).withEndAction {
            binding.fullscreenContainer.visibility = View.GONE
        }.start()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

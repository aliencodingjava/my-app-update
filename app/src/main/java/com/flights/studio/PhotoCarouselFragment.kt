package com.flights.studio

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
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
import java.io.File

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

        if (isInternetAvailable()) {
            fetchPhotosFromSupabase()
        } else {
            loadOfflineFallback()
        }

        binding.fullscreenContainer.setOnClickListener {
            hideFullscreenImage()
        }
        binding.closeButton.setOnClickListener {
            hideFullscreenImage()
        }
    }

    private fun isInternetAvailable(): Boolean {
        val connectivityManager = requireContext().getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }


    private fun fetchPhotosFromSupabase() {
        val baseUrl = "https://gdvhiudodnqdqhkyghsk.supabase.co/storage/v1/object/public/carousel-photos/"

        val fileNames = listOf(
            "20180608_183623.jpg", "20180608_191842.jpg", "20180608_192101.jpg",
            "20181231_163736.jpg", "20190301_162454.jpg", "20190301_162759.jpg",
            "20190816_164342.jpg", "20190816_170717.jpg", "20190816_171609.jpg",
            "20190929_112411.jpg", "20221115_153223.jpg", "20221115_153340.jpg",
            "20230627_130753.jpg", "20230810_154347.jpg", "20230810_160142.jpg",
            "20230925_134825.jpg", "20230925_135027.jpg", "20231011_170710.jpg",
            "photojack_1.jpg", "photojack_2.jpg"
        )

        val context = requireContext()
        val missingDownloads = mutableListOf<Pair<String, String>>() // (url, filename)

        // Build initial list of URIs if already downloaded
        val localUris = fileNames.mapNotNull { fileName ->
            val file = File(context.filesDir, fileName)
            if (file.exists()) {
                Uri.fromFile(file).toString()
            } else {
                missingDownloads.add("$baseUrl$fileName" to fileName)
                null
            }
        }

        if (missingDownloads.isEmpty()) {
            // All files exist locally
            setupCarousel(localUris)
        } else {
            // Download the missing ones first
            downloadAllImages(context, missingDownloads) {
                val finalUris = fileNames.mapNotNull { name ->
                    val file = File(context.filesDir, name)
                    if (file.exists()) Uri.fromFile(file).toString() else null
                }
                setupCarousel(finalUris)
            }
        }
    }
    private fun downloadAllImages(
        context: Context,
        items: List<Pair<String, String>>,
        onComplete: () -> Unit
    ) {
        val client = okhttp3.OkHttpClient()
        var completed = 0

        for ((url, fileName) in items) {
            val request = okhttp3.Request.Builder().url(url).build()

            client.newCall(request).enqueue(object : okhttp3.Callback {
                override fun onFailure(call: okhttp3.Call, e: java.io.IOException) {
                    e.printStackTrace()
                    checkComplete()
                }

                override fun onResponse(call: okhttp3.Call, response: okhttp3.Response) {
                    if (response.isSuccessful) {
                        val file = File(context.filesDir, fileName)
                        val inputStream = response.body?.byteStream()

                        inputStream?.use { input ->
                            file.outputStream().use { output ->
                                input.copyTo(output)
                            }
                        }
                    }
                    checkComplete()
                }

                private fun checkComplete() {
                    synchronized(this) {
                        completed++
                        if (completed == items.size) {
                            requireActivity().runOnUiThread {
                                onComplete()
                            }
                        }
                    }
                }
            })
        }
    }



    private fun loadOfflineFallback() {
        val fallbackUrls = listOf(
            "android.resource://${requireContext().packageName}/${R.drawable.placeholder}"
        )

        // Show the same placeholder multiple times to simulate a gallery
        val repeated = List(10) { fallbackUrls[0] }
        setupCarousel(repeated)
    }

    private fun setupCarousel(photoUrls: List<String>) {
        val photoAdapter = PhotoAdapter(photoUrls) { photoUrl ->
            showFullscreenImage(photoUrl)
        }

        binding.carouselRecyclerView.apply {
            layoutManager = CarouselLayoutManager()
            CarouselSnapHelper().attachToRecyclerView(this)
            adapter = photoAdapter
        }
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
            .error(R.drawable.placeholder)     // fallback on load failure
            .placeholder(R.drawable.placeholder) // while loading
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

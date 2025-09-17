package com.flights.studio

import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.flights.studio.databinding.ItemPhotoCarouselBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

class PhotoAdapter(
    private val photos: List<String>,
    private val onPhotoClick: (String) -> Unit
) : RecyclerView.Adapter<PhotoAdapter.PhotoViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PhotoViewHolder {
        val binding = ItemPhotoCarouselBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return PhotoViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PhotoViewHolder, position: Int) {
        holder.bind(photos[position])
    }

    override fun getItemCount() = photos.size

    inner class PhotoViewHolder(private val binding: ItemPhotoCarouselBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(photoUrl: String) {
            val context = binding.root.context
            val fileName = photoUrl.substringAfterLast("/")
            val localFile = File(context.filesDir, fileName)
            val localPath = localFile.absolutePath

            val imageToLoad = if (localFile.exists()) localPath else photoUrl

            Glide.with(context)
                .load(imageToLoad)
                .placeholder(R.drawable.placeholder_background)
                .into(binding.carouselImageView)

            // Save image if not already cached
            if (!localFile.exists()) {
                downloadAndSaveImage(photoUrl, localFile)
            }

            binding.carouselImageView.setOnClickListener {
                onPhotoClick(imageToLoad)
            }
        }

        private fun downloadAndSaveImage(url: String, outputFile: File) {
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val connection = URL(url).openConnection() as HttpURLConnection
                    connection.connectTimeout = 5000
                    connection.readTimeout = 5000
                    connection.inputStream.use { input ->
                        FileOutputStream(outputFile).use { output ->
                            input.copyTo(output)
                        }
                    }
                    Log.d("PhotoAdapter", "Saved image to ${outputFile.absolutePath}")
                } catch (e: Exception) {
                    Log.e("PhotoAdapter", "Error saving image: ${e.message}")
                }
            }
        }
    }
}

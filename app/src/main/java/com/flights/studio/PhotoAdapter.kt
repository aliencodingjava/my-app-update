package com.flights.studio

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.flights.studio.databinding.ItemPhotoCarouselBinding

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
            Glide.with(binding.root.context)
                .load(photoUrl)
                .placeholder(R.drawable.placeholder_background)
                .into(binding.carouselImageView)

            binding.carouselImageView.setOnClickListener {
                onPhotoClick(photoUrl)
            }
        }
    }
}

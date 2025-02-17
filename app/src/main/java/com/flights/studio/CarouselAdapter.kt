package com.flights.studio

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide

data class CarouselItem(val imageResId: String, val title: String)

class CarouselAdapter(
    private val items: List<CarouselItem>,
    private val listener: OnItemClickListener
) : RecyclerView.Adapter<CarouselAdapter.ViewHolder>() { // <- Updated

    interface OnItemClickListener {
        fun onItemClick(item: CarouselItem)
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imageView: ImageView = itemView.findViewById(R.id.carousel_image_view)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_photo_carousel, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        Glide.with(holder.itemView.context)
            .load(item.imageResId)
            .placeholder(R.drawable.placeholder_background)
            .into(holder.imageView)
        holder.itemView.setOnClickListener {
            listener.onItemClick(item)
        }
    }

    override fun getItemCount(): Int = items.size
}
package com.flights.studio

import android.net.Uri
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.github.chrisbanes.photoview.PhotoView

class ImagePagerAdapter(
    private val uris: List<Uri>,
    private val onSingleTap: (() -> Unit)? = null
) : RecyclerView.Adapter<ImagePagerAdapter.VH>() {

    init { setHasStableIds(true) }

    class VH(val root: FrameLayout, val photo: PhotoView) : RecyclerView.ViewHolder(root)

    override fun getItemId(position: Int): Long =
        uris[position].toString().hashCode().toLong()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val root = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_fullscreen_pager_image, parent, false) as FrameLayout

        val pv: PhotoView = root.findViewById(R.id.pager_photo)
        pv.scaleType = ImageView.ScaleType.FIT_CENTER
        pv.setZoomTransitionDuration(0)          // no zoom animation
        pv.maximumScale = 4f                     // optional: cap max zoom
        pv.mediumScale = 2f
        pv.minimumScale = 1f
        pv.setAllowParentInterceptOnEdge(true)   // lets ViewPager2 swipe when at edges

        return VH(root, pv)
    }

    override fun onBindViewHolder(h: VH, position: Int) {
        // reset zoom for recycled pages
        runCatching { h.photo.setScale(1f, false) }

        Glide.with(h.photo)
            .load(uris[position])
            .diskCacheStrategy(DiskCacheStrategy.AUTOMATIC)
            .dontAnimate() // keep it flicker-free
            .placeholder(R.drawable.placeholder_background)
            .error(R.drawable.placeholder_background)
            .into(h.photo)

        h.photo.setOnClickListener { onSingleTap?.invoke() }
    }

    override fun onViewRecycled(h: VH) {
        Glide.with(h.photo.context.applicationContext).clear(h.photo)
        h.photo.setImageDrawable(null)
        runCatching { h.photo.setScale(1f, false) }
        super.onViewRecycled(h)
    }

    override fun getItemCount(): Int = uris.size
}

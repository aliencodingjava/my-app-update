package com.flights.studio

import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView

@Suppress("DEPRECATION")
class SelectedImagesAdapter(
    private val items: MutableList<Uri> = mutableListOf(),
    private val onRemove: (Uri) -> Unit = {}
) : RecyclerView.Adapter<SelectedImagesAdapter.VH>() {

    class VH(v: View) : RecyclerView.ViewHolder(v) {
        val img: ImageView = v.findViewById(R.id.img)
        val btnRemove: ImageView = v.findViewById(R.id.btn_remove)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_note_image, parent, false)
        return VH(v)
    }

    override fun onBindViewHolder(h: VH, pos: Int) {
        val uri = items[pos]

        // 🔲 make each thumb a 120dp square (same as Edit)
        val size = (120f * h.itemView.resources.displayMetrics.density).toInt()
        (h.itemView.layoutParams as RecyclerView.LayoutParams).width = size
        h.img.layoutParams.apply {
            width = size
            height = size
        }

        // Use Glide and crop into the square (same as Edit)
        com.bumptech.glide.Glide.with(h.img)
            .load(uri)
            .centerCrop()
            .thumbnail(0.25f)
            .dontAnimate()
            .into(h.img)

        h.btnRemove.setOnClickListener {
            val idx = h.bindingAdapterPosition
            if (idx != RecyclerView.NO_POSITION) {
                val removed = items.removeAt(idx)
                notifyItemRemoved(idx)
                onRemove(removed)
            }
        }
    }

    override fun getItemCount(): Int = items.size

    /** Public read-only snapshot */
    fun getAll(): List<Uri> = items.toList()

    /** Add a single item if missing, with precise notification */
    fun add(uri: Uri) {
        if (items.contains(uri)) return
        items.add(uri)
        notifyItemInserted(items.lastIndex)
    }

    /** Add many (deduped), with a single range-insert if contiguous */
    fun addAll(uris: List<Uri>) {
        if (uris.isEmpty()) return
        val existing = items.toHashSet()
        val unique = uris.filter { u -> !existing.contains(u) }
        if (unique.isEmpty()) return

        val start = items.size
        items.addAll(unique)
        notifyItemRangeInserted(start, unique.size)
    }

    /** Move item and notify precisely (for drag & drop) */
    fun move(from: Int, to: Int) {
        if (from == to || from !in items.indices || to !in items.indices) return
        val item = items.removeAt(from)
        items.add(to, item)
        notifyItemMoved(from, to)
    }
}


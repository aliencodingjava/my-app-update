package com.flights.studio

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.text.HtmlCompat
import androidx.recyclerview.widget.RecyclerView

class UpdateAdapter(
    private val items: List<UpdateBlock>
) : RecyclerView.Adapter<UpdateAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_update_block, parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount() = items.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(items[position])
    }

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val title = view.findViewById<TextView>(R.id.updateTitle)
        private val body  = view.findViewById<TextView>(R.id.updateBody)

        fun bind(item: UpdateBlock) {
            title.text = HtmlCompat.fromHtml(item.title, HtmlCompat.FROM_HTML_MODE_LEGACY)
            body.text  = HtmlCompat.fromHtml(item.body, HtmlCompat.FROM_HTML_MODE_LEGACY)
        }
    }
}

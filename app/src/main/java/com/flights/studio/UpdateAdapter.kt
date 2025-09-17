package com.flights.studio

import android.text.Html
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class UpdateAdapter(private val updateList: List<String>) :
    RecyclerView.Adapter<UpdateAdapter.UpdateViewHolder>() {

    class UpdateViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val updateText: TextView = itemView.findViewById(R.id.updateTextView)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UpdateViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_update_text, parent, false)
        return UpdateViewHolder(view)
    }

    override fun onBindViewHolder(holder: UpdateViewHolder, position: Int) {
        val text = if (position == 0) {
            updateList[position]
        } else {
            "• ${updateList[position]}"
        }
        holder.updateText.text = Html.fromHtml(text, Html.FROM_HTML_MODE_LEGACY)
    }


    override fun getItemCount(): Int = updateList.size
}

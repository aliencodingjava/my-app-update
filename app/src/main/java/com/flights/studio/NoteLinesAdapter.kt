package com.flights.studio

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class NoteLinesAdapter(private val lines: List<String>) : RecyclerView.Adapter<NoteLinesAdapter.NoteLineViewHolder>() {

    inner class NoteLineViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val lineTextView: TextView = itemView.findViewById(R.id.lineTextView)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NoteLineViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_note_line, parent, false)
        return NoteLineViewHolder(view)
    }

    override fun onBindViewHolder(holder: NoteLineViewHolder, position: Int) {
        holder.lineTextView.text = lines[position]
    }

    override fun getItemCount(): Int = lines.size
}

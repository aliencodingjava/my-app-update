package com.flights.studio

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView

class NotesAdapter(
    private var notes: List<String>,
    private val onLongClick: (String) -> Unit,
    private val onClick: (String, Int) -> Unit, // Include position
    private val onEditIconClick: (String, Int) -> Unit // Callback for edit icon
) : RecyclerView.Adapter<NotesAdapter.NoteViewHolder>() {

    private val selectedNotes = mutableSetOf<String>() // Tracks selected notes

    inner class NoteViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val noteTextView: TextView = view.findViewById(R.id.tv_note)
        val editIcon: ImageView = view.findViewById(R.id.expandCollapseIcon) // Reference to the edit icon
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NoteViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_note, parent, false)
        return NoteViewHolder(view)
    }

    override fun onBindViewHolder(holder: NoteViewHolder, position: Int) {
        val note = notes[position]
        holder.noteTextView.text = note

        val cardView = holder.itemView as MaterialCardView
        cardView.setCardBackgroundColor(
            if (selectedNotes.contains(note)) {
                ContextCompat.getColor(holder.itemView.context, R.color.selected_note_color)
            } else {
                ContextCompat.getColor(holder.itemView.context, R.color.box_alert_update)
            }
        )

        // Handle clicks on the whole item
        holder.itemView.setOnClickListener { onClick(note, position) }
        holder.itemView.setOnLongClickListener {
            onLongClick(note)
            true
        }

        // Handle clicks on the edit icon
        holder.editIcon.setOnClickListener {
            onEditIconClick(note, position) // Notify the callback with note and position
        }
    }

    override fun getItemCount(): Int = notes.size

    fun updateList(newNotes: List<String>) {
        val diffCallback = NotesDiffCallback(notes, newNotes)
        val diffResult = DiffUtil.calculateDiff(diffCallback)

        notes = newNotes
        diffResult.dispatchUpdatesTo(this) // Notify the adapter of changes
    }

    class NotesDiffCallback(
        private val oldList: List<String>,
        private val newList: List<String>
    ) : DiffUtil.Callback() {

        override fun getOldListSize(): Int = oldList.size

        override fun getNewListSize(): Int = newList.size

        override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            return oldList[oldItemPosition] == newList[newItemPosition]
        }

        override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            return oldList[oldItemPosition] == newList[newItemPosition]
        }
    }

    fun toggleSelection(note: String) {
        val index = notes.indexOf(note)
        if (index != -1) {
            if (selectedNotes.contains(note)) {
                selectedNotes.remove(note)
            } else {
                selectedNotes.add(note)
            }
            notifyItemChanged(index)
        }
    }

    fun clearSelection() {
        val selectedIndices = selectedNotes.map { notes.indexOf(it) }
        selectedNotes.clear()
        for (index in selectedIndices) {
            if (index != -1) {
                notifyItemChanged(index)
            }
        }
    }
}

package com.flights.studio

import android.graphics.Color
import android.util.TypedValue
import android.view.Gravity
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class AlphabetAdapter(
    private val alphabet: List<String>,
    private val onLetterSelected: (String) -> Unit
) : RecyclerView.Adapter<AlphabetAdapter.AlphabetViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AlphabetViewHolder {
        val textView = TextView(parent.context).apply {
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
            setPadding(8, 8, 8, 8)
            setTextColor(Color.BLACK)
            gravity = Gravity.CENTER
        }
        return AlphabetViewHolder(textView)
    }

    override fun onBindViewHolder(holder: AlphabetViewHolder, position: Int) {
        val letter = alphabet[position]
        holder.textView.text = letter
        holder.textView.setOnClickListener {
            onLetterSelected(letter)
        }
    }

    override fun getItemCount() = alphabet.size

    class AlphabetViewHolder(val textView: TextView) : RecyclerView.ViewHolder(textView)
}

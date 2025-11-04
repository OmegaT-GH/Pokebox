package com.omegat.pokebox.adapters

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.omegat.pokebox.R
import com.omegat.pokebox.data.PokemonSet

class ListSetsAdapter(
    private val context: Context,
    private val sets: List<PokemonSet>,
    private val onItemClick: ((PokemonSet) -> Unit)? = null
) : RecyclerView.Adapter<ListSetsAdapter.ViewHolder>() {

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val setLogo: ImageView = itemView.findViewById(R.id.SetLogo)
        val setSymbol: ImageView = itemView.findViewById(R.id.SetSymbol)
        val setName: TextView = itemView.findViewById(R.id.SetName)
        val setCards: TextView = itemView.findViewById(R.id.SetCards)

        @SuppressLint("SetTextI18n")
        fun bind(set: PokemonSet) {
            Glide.with(context).load(set.images?.logo).into(setLogo)
            Glide.with(context).load(set.images?.symbol).override(60, 60).into(setSymbol)
            setName.text = set.name
            setCards.text = "${set.printedTotal}/${set.total}"

            itemView.setOnClickListener {
                onItemClick?.invoke(set)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(context)
            .inflate(R.layout.item_setlist, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(sets[position])
    }

    override fun getItemCount(): Int = sets.size
}
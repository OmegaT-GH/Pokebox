package com.omegat.pokebox.adapters

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.omegat.pokebox.R
import com.omegat.pokebox.data.PokemonSet

class ListPercentageAdapter(
    private val context: Context,
    private val sets: List<PokemonSet>,
    private val perc: MutableList<Int>,
    private val percamount: MutableList<Int>,
    private val onItemClick: ((PokemonSet) -> Unit)? = null
) : RecyclerView.Adapter<ListPercentageAdapter.ViewHolder>() {

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val setLogo: ImageView = itemView.findViewById(R.id.ivsplogo)
        val setName: TextView = itemView.findViewById(R.id.tvspname)
        val setPerc: TextView = itemView.findViewById(R.id.tvspperc)
        val setCards: TextView = itemView.findViewById(R.id.tvspcards)
        val pbar: ProgressBar = itemView.findViewById(R.id.pbarperc)

        @SuppressLint("SetTextI18n")
        fun bind(set: PokemonSet, p: Int, pa: Int) {

            Glide.with(context).load(set.images?.logo).into(setLogo)
            setName.text = set.name
            setPerc.text = "$p%"
            pbar.progress = p
            setCards.text = "$pa/${set.total}"

            itemView.setOnClickListener {
                onItemClick?.invoke(set)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(context)
            .inflate(R.layout.item_set_percentage, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(sets[position], perc[position], percamount[position])
    }

    override fun getItemCount(): Int = sets.size

    @SuppressLint("NotifyDataSetChanged")
    fun updateData(newPerc: List<Int>, newPercAmount: List<Int>) {
        perc.clear()
        percamount.clear()
        perc.addAll(newPerc)
        percamount.addAll(newPercAmount)
        notifyDataSetChanged()
    }

    fun currentList(): List<PokemonSet> = sets
}
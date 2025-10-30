package com.example.pokebox.adapters

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.pokebox.R
import com.example.pokebox.data.PokemonCard
import com.example.pokebox.data.PokemonSet

class ListCardsSearchAdapter (
    private val context: Context,
    private val set: PokemonSet,
    private val cards: List<PokemonCard>,
    private val onItemClick: ((PokemonCard) -> Unit)? = null
) : RecyclerView.Adapter<ListCardsSearchAdapter.ViewHolder>() {

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val cardImage: ImageView = itemView.findViewById(R.id.CardImage)
        val cardName: TextView = itemView.findViewById(R.id.CardName)
        val cardNumber: TextView = itemView.findViewById(R.id.CardNumber)
        val cardRarity: TextView = itemView.findViewById(R.id.CardRarity)
        val cardSupertype: TextView = itemView.findViewById(R.id.CardSuperType)

        @SuppressLint("SetTextI18n")
        fun bind(card: PokemonCard) {
            Glide.with(context).load(card.images?.small).fitCenter().into(cardImage)
            cardName.text = card.name
            cardNumber.text = "${card.number}/${set.printedTotal}"
            cardRarity.text = card.rarity
            cardSupertype.text = card.supertype

            itemView.setOnClickListener {
                onItemClick?.invoke(card)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(context)
            .inflate(R.layout.item_cardlist, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(cards[position])
    }

    override fun getItemCount(): Int = cards.size
}
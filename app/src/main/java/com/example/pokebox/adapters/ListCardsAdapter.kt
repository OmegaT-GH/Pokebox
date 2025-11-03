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

class ListCardsAdapter (
    private val context: Context,
    private val set: PokemonSet?,
    var cards: List<PokemonCard>,
    var cardAmounts: MutableList<Int>,
    private val onItemClick: ((PokemonCard) -> Unit)? = null
) : RecyclerView.Adapter<ListCardsAdapter.ViewHolder>() {

    constructor(
        context: Context,
        cards: List<PokemonCard>,
        cardAmounts: MutableList<Int> = MutableList(cards.size) { 0 },
        onItemClick: ((PokemonCard) -> Unit)? = null
    ) : this(context, null, cards, cardAmounts, onItemClick)

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val cardImage: ImageView = itemView.findViewById(R.id.CardImage)
        val cardName: TextView = itemView.findViewById(R.id.CardName)
        val cardNumber: TextView = itemView.findViewById(R.id.CardNumber)
        val cardRarity: TextView = itemView.findViewById(R.id.CardRarity)
        val cardSupertype: TextView = itemView.findViewById(R.id.CardSuperType)
        val cardAmount: TextView = itemView.findViewById(R.id.tvamount)

        @SuppressLint("SetTextI18n")
        fun bind(card: PokemonCard, amount: Int) {
            Glide.with(context).load(card.images?.small).fitCenter().into(cardImage)
            cardName.text = card.name
            cardNumber.text = if (set != null) {
                "${card.number}/${set.printedTotal}"
            } else {
                "#${card.number}"
            }
            cardRarity.text = card.rarity
            cardSupertype.text = card.supertype
            cardAmount.text = amount.toString()

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
        holder.bind(cards[position], cardAmounts[position])
    }

    override fun getItemCount(): Int = cards.size
    @SuppressLint("NotifyDataSetChanged")
    fun updateData(newCards: List<PokemonCard>, newCardAmounts: MutableList<Int>) {
        cards = newCards
        cardAmounts = newCardAmounts
        notifyDataSetChanged()
    }
}
package com.omegat.pokebox.adapters

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.omegat.pokebox.R
import com.omegat.pokebox.data.PokemonCard
import androidx.core.graphics.toColorInt

class ListDeckAdapter(
    private val context: Context,
    var cards: List<PokemonCard>,
    var cardOwned: MutableList<Int>,
    var cardAmounts: MutableList<Int>,
    val getCurrentColId: () -> Int,
    private val onItemClick: ((PokemonCard, Int) -> Unit)? = null
) : RecyclerView.Adapter<ListDeckAdapter.ViewHolder>() {

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val cardImage: ImageView = itemView.findViewById(R.id.ivDeckCardImage)
        val cardName: TextView = itemView.findViewById(R.id.tvDeckCardName)
        val cardSetcode: TextView = itemView.findViewById(R.id.tvDeckCardSet)
        val cardAmount: TextView = itemView.findViewById(R.id.tvDeckCardAmount)

        @SuppressLint("SetTextI18n")
        fun bind(card: PokemonCard, amount: Int, owned: Int) {
            Glide.with(context).load(card.images?.large).placeholder(R.drawable.placeholdercard)
                .error(R.drawable.placeholdercard).fitCenter().into(cardImage)

            cardName.text = card.name ?: "(Sin nombre)"
            cardSetcode.text = card.ptcgoCode ?: "N/A"

            cardAmount.text = "$owned / $amount"

            if (owned >= amount) {
                cardName.setTextColor("#20FF20".toColorInt())
                cardSetcode.setTextColor("#20FF20".toColorInt())
                cardAmount.setTextColor("#20FF20".toColorInt())
            } else {
                cardName.setTextColor("#FFFFFF".toColorInt())
                cardSetcode.setTextColor("#FFFFFF".toColorInt())
                cardAmount.setTextColor("#FFFFFF".toColorInt())
            }

            itemView.setOnClickListener {
                val colid = getCurrentColId()
                onItemClick?.invoke(card, colid)
            }

            if (absoluteAdapterPosition % 2 == 0) {
                itemView.background = ContextCompat.getDrawable(itemView.context, R.drawable.rounded_item_background)
            } else {
                itemView.background = ContextCompat.getDrawable(itemView.context, R.drawable.rounded_item_background_lighter)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.item_deckview, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(cards[position], cardAmounts[position], cardOwned[position])
    }

    override fun getItemCount(): Int = cards.size

    @SuppressLint("NotifyDataSetChanged")
    fun updateData(
        newCards: List<PokemonCard>,
        newCardAmounts: MutableList<Int>,
        newCardOwned: MutableList<Int>
    ) {
        cards = newCards
        cardAmounts = newCardAmounts
        cardOwned = newCardOwned
        notifyDataSetChanged()
    }
}
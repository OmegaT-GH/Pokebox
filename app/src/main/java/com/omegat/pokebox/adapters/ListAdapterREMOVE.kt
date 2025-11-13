package com.omegat.pokebox.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ImageView
import android.widget.TextView
import com.bumptech.glide.Glide
import com.omegat.pokebox.R
import com.omegat.pokebox.data.PokemonCard

class ListAdapterREMOVE(private val context: Context, private val cards: List<PokemonCard>) : BaseAdapter() {

    override fun getCount(): Int = cards.size
    override fun getItem(position: Int): Any = cards[position]
    override fun getItemId(position: Int): Long = position.toLong()

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        val view = convertView ?: LayoutInflater.from(context).inflate(R.layout.item_testlist, parent, false)

        val card = cards[position]
        val cardName = view.findViewById<TextView>(R.id.NombreCarta)
        val cardType = view.findViewById<TextView>(R.id.TipoCarta)
        val cardNumber = view.findViewById<TextView>(R.id.NumCarta)
        val cardImage = view.findViewById<ImageView>(R.id.ImagenCarta)

        cardName.text = card.name
        cardType.text = card.types?.joinToString(", ")
        cardNumber.text = card.number.toString()

        Glide.with(context).load(card.images?.small).into(cardImage)

        return view
    }
}
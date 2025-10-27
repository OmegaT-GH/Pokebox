package com.example.pokebox.activities

import android.annotation.SuppressLint
import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
//import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.bumptech.glide.Glide
import com.example.pokebox.R
import com.example.pokebox.data.PokemonCard
import com.example.pokebox.data.PokemonSet
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.google.gson.stream.JsonReader

class ViewCard : AppCompatActivity() {

    lateinit var pset : PokemonSet

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_view_card)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val card = intent.getParcelableExtra<PokemonCard>("pcard")

        val ivcard = findViewById<ImageView>(R.id.ivCardLarge)
        val name = findViewById<TextView>(R.id.tvCardName)
        val number = findViewById<TextView>(R.id.tvCardNumber)
        val supertype = findViewById<TextView>(R.id.tvCardSupertype)
        val subtype = findViewById<TextView>(R.id.tvCardSubtype)
        val set = findViewById<TextView>(R.id.tvCardSet)
        val type = findViewById<TextView>(R.id.tvCardType)
        val rarity = findViewById<TextView>(R.id.tvCardRarity)
        val artist = findViewById<TextView>(R.id.tvCardArtist)

        val SinputStream = assets.open("json/sets/en.json")
        val Sreader = JsonReader(SinputStream.reader())
        val Stype = object : TypeToken<List<PokemonSet>>() {}.type
        val sets: List<PokemonSet> = Gson().fromJson(Sreader, Stype)
        Sreader.close()

        val cardid = card?.id
        val setid = cardid?.substringBefore("-")
        //Toast.makeText(this, setid, Toast.LENGTH_SHORT).show()


        for (set in sets) {
            if (set.id == setid) {
                pset = set
            }
        }

        if (card?.subtypes?.contains("BREAK") == true) {
            ivcard.rotation = 90f
        }

        Glide.with(this).load(card?.images?.large).fitCenter().into(ivcard)
        name.text = "${card?.name}"
        number.text = "${card?.number}/${pset.printedTotal}"
        supertype.text = "${card?.supertype}"
        subtype.text = card?.subtypes?.joinToString(", ") ?: "-"
        set.text = "Set: ${pset.name}"
        type.text = "Type: ${card?.types?.joinToString(", ") ?: "-"}"
        rarity.text = "Rarity: ${card?.rarity}"
        artist.text = "Artist: ${card?.artist ?: "Info Not Available"}"






    }
}
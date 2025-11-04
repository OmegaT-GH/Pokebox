package com.omegat.pokebox.activities

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.omegat.pokebox.R
import com.omegat.pokebox.adapters.ListCardsAdapter
import com.omegat.pokebox.bd.DBHelper
import com.omegat.pokebox.data.PokemonCard
import com.omegat.pokebox.data.PokemonSet
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.google.gson.stream.JsonReader

class ListCards : AppCompatActivity() {

    lateinit var rviewadap: ListCardsAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_list_cards)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val rview = findViewById<RecyclerView>(R.id.rviewcardsearch)
        val db = DBHelper(this)
        val colid = intent.getIntExtra("col", -1)
        Log.d(null,colid.toString())

        val set = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra("pset", PokemonSet::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra("pset")
        }
        if (set != null) {

            val cinputStream = assets.open("json/cards/en/" + set.id + ".json")
            val creader = JsonReader(cinputStream.reader())
            val ctype = object : TypeToken<List<PokemonCard>>() {}.type
            val cards: List<PokemonCard> = Gson().fromJson(creader, ctype)
            creader.close()

            val lmanager = LinearLayoutManager(this)
            rview.layoutManager = lmanager
            rview.clipToPadding = false
            rview.setPadding(16, 16, 16, 16)

            val cardamounts = mutableListOf<Int>()
            for (card in cards) {
                if (colid != -1) {
                    val am = db.getCardAmount(colid, card.id)
                    cardamounts.add(am)
                } else {
                    cardamounts.add(0)
                }
            }

            rviewadap  = ListCardsAdapter(this, set, cards, cardamounts) { selectedCard ->
                //Toast.makeText(this, "Pulsado: ${selectedCard.name}", Toast.LENGTH_SHORT).show()

                val i = Intent(this, ViewCard::class.java)
                i.putExtra("pcard", selectedCard)
                this.startActivity(i)
            }
            rview.adapter = rviewadap




        }









    }

    @SuppressLint("NotifyDataSetChanged")
    override fun onResume() {
        super.onResume()

        val db = DBHelper(this)
        val colid = intent.getIntExtra("col", -1)

        if (colid != -1 && this::rviewadap.isInitialized) {
            for ((index, card) in rviewadap.cards.withIndex()) {
                val am = db.getCardAmount(colid, card.id)
                rviewadap.cardAmounts[index] = am
            }
            rviewadap.notifyDataSetChanged()
        }
    }
}
package com.omegat.pokebox.activities

import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.omegat.pokebox.R
import com.omegat.pokebox.data.CardRepository
import com.omegat.pokebox.data.PokemonCard
import com.omegat.pokebox.data.PokemonSet
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.google.gson.stream.JsonReader

class LoadingActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_loading)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        loadCards()
    }

    private fun loadCards() {
        Thread {
            val gson = Gson()

            val sinputStream = assets.open("json/sets/en.json")
            val sreader = JsonReader(sinputStream.reader())
            val stype = object : TypeToken<List<PokemonSet>>() {}.type
            val sets: List<PokemonSet> = gson.fromJson(sreader, stype)
            sreader.close()

            for (set in sets) {
                try {
                    val path = "json/cards/en/" + set.id + ".json"
                    val cinputStream = assets.open(path)
                    val creader = JsonReader(cinputStream.reader())
                    val ctype = object : TypeToken<List<PokemonCard>>() {}.type
                    val cards: List<PokemonCard> = Gson().fromJson(creader, ctype)

                    cards.forEach { card ->
                        card.releaseDate = set.releaseDate
                        card.ptcgoCode = set.ptcgoCode
                    }

                    CardRepository.addCards(cards)
                    creader.close()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            runOnUiThread {
                val i = Intent(this, MainActivity::class.java)
                startActivity(i)
                finish()
            }
        }.start()
    }

}
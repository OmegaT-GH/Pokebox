package com.example.pokebox.activities

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.os.Looper
import android.widget.ProgressBar
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.pokebox.R
import com.example.pokebox.data.CardRepository
import com.example.pokebox.data.PokemonCard
import com.example.pokebox.data.PokemonSet
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.google.gson.stream.JsonReader

class LoadingActivity : AppCompatActivity() {

    lateinit var pbar : ProgressBar
    lateinit var tvprog : TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_loading)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        pbar = findViewById(R.id.pbloading)
        tvprog = findViewById(R.id.tvloading)

        loadCards()

    }

    @SuppressLint("SetTextI18n")
    private fun loadCards() {

        Thread {

            val gson = Gson()

            val SinputStream = assets.open("json/sets/en.json")
            val Sreader = JsonReader(SinputStream.reader())
            val Stype = object : TypeToken<List<PokemonSet>>() {}.type
            val sets: List<PokemonSet> = gson.fromJson(Sreader, Stype)
            Sreader.close()

            val totalsets = sets.size
            var loadedsets = 0

            for (set in sets) {

                try {

                    val path = "json/cards/en/" + set.id + ".json"
                    val CinputStream = assets.open(path)
                    val Creader = JsonReader(CinputStream.reader())
                    val Ctype = object : TypeToken<List<PokemonCard>>() {}.type
                    val cards: List<PokemonCard> = Gson().fromJson(Creader, Ctype)

                    cards.forEach { card ->
                        card.releaseDate = set.releaseDate
                    }

                    CardRepository.addCards(cards)
                    Creader.close()

                } catch (e: Exception) {
                    e.printStackTrace()
                }

                loadedsets++
                val progress = ((loadedsets.toFloat() / totalsets) * 100).toInt()

                runOnUiThread {

                    pbar.progress = progress
                    tvprog.text = "Cargando sets... $loadedsets/$totalsets ($progress%)"

                }

            }

            runOnUiThread {

                tvprog.text = "Carga completa. (${CardRepository.getCards().size} cartas)"
                pbar.progress = 100

                android.os.Handler(Looper.getMainLooper()).postDelayed({
                    startActivity(Intent(this, AdvancedSearch::class.java))
                    finish()
                }, 1000)

            }

        }.start()

    }

}
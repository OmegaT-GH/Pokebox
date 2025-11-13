package com.omegat.pokebox.activities

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
import com.omegat.pokebox.R
import com.omegat.pokebox.data.CardRepository
import com.omegat.pokebox.data.PokemonCard
import com.omegat.pokebox.data.PokemonSet
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
        val type = intent.getStringExtra("type")
        Thread {

            val gson = Gson()

            val sinputStream = assets.open("json/sets/en.json")
            val sreader = JsonReader(sinputStream.reader())
            val stype = object : TypeToken<List<PokemonSet>>() {}.type
            val sets: List<PokemonSet> = gson.fromJson(sreader, stype)
            sreader.close()

            val totalsets = sets.size
            var loadedsets = 0

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

                loadedsets++
                val progress = ((loadedsets.toFloat() / totalsets) * 100).toInt()

                runOnUiThread {

                    pbar.progress = progress
                    tvprog.text = getString(R.string.cargando_sets) + " $loadedsets/$totalsets ($progress%)"

                }

            }

            runOnUiThread {

                tvprog.text = getString(R.string.carga_completa) + "${CardRepository.getCards().size} " + getString(
                    R.string.cartas)
                pbar.progress = 100

                android.os.Handler(Looper.getMainLooper()).postDelayed({


                    if (type == "search") {
                        val i = Intent(this, AdvancedSearch::class.java)
                        i.putExtra("col", intent.getIntExtra("col", -1))
                        this.startActivity(i)
                        finish()
                    } else if (type == "deck") {
                        val i = Intent(this, CheckDeck::class.java)
                        i.putExtra("col", intent.getIntExtra("col", -1))
                        this.startActivity(i)
                        finish()
                    }


                }, 1000)

            }

        }.start()

    }

}
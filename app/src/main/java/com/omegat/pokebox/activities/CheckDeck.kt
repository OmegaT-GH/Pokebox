package com.omegat.pokebox.activities

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.RecyclerView
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.google.gson.stream.JsonReader
import com.omegat.pokebox.R
import com.omegat.pokebox.adapters.ListDeckAdapter
import com.omegat.pokebox.bd.DBHelper
import com.omegat.pokebox.data.CardDeckCheck
import com.omegat.pokebox.data.CardRepository
import com.omegat.pokebox.data.PokemonCard
import com.omegat.pokebox.data.PokemonSet
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class CheckDeck : AppCompatActivity() {

    lateinit var sets: List<PokemonSet>
    lateinit var cards: List<PokemonCard>
    lateinit var rviewadap: ListDeckAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_check_deck)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        cards = CardRepository.getCards()
        val checkedcards = mutableListOf<PokemonCard>()



        val sinputStream = assets.open("json/sets/en.json")
        val sreader = JsonReader(sinputStream.reader())
        val stype = object : TypeToken<List<PokemonSet>>() {}.type
        sets = Gson().fromJson(sreader, stype)
        sreader.close()

        val db = DBHelper(this)
        val colid = intent.getIntExtra("col", -1)

        val spcols = findViewById<Spinner>(R.id.spDeckCollections)
        val et = findViewById<EditText>(R.id.etDecklist)
        val btcheck = findViewById<Button>(R.id.btDeckCheck)
        val rview = findViewById<RecyclerView>(R.id.rviewdeck)

        val divider = DividerItemDecoration(this, DividerItemDecoration.VERTICAL)
        rview.addItemDecoration(divider)

        loadSpinner(this, db, spcols, colid)

        btcheck.setOnClickListener {

            val selectedcollection = spcols.selectedItem.toString()
            val selcolid = db.getCollectionFromName(selectedcollection)
            Log.d("col", "coleccion:$selcolid/$colid")
            checkedcards.clear()

            val deck = getCards(et)
            val cardAmounts = mutableListOf<Int>()
            val cardOwned = mutableListOf<Int>()

            for (d in deck) {

                val set = sets.find { it.ptcgoCode.equals(d.ptcgocode, ignoreCase = true) }
                if (set != null) {
                    val found = cards.find { c ->
                        c.number == d.number.toString() && c.id?.startsWith(
                            set.id ?: "",
                            true
                        ) == true
                    }
                    if (found != null) {
                        checkedcards.add(found)
                        cardAmounts.add(d.count)

                        val cam = db.getCardAmount(selcolid, set.id+"-"+d.number)
                        cardOwned.add(cam)
                    }


                }

            }

            val checkedcardsL: List<PokemonCard> = checkedcards

            if (::rviewadap.isInitialized) {
                rviewadap.updateData(checkedcardsL, cardAmounts, cardOwned)
            } else {
                rviewadap = ListDeckAdapter(
                    this,
                    checkedcardsL,
                    cardOwned,
                    cardAmounts,
                    getCurrentColId = { spcols.selectedItem?.let { db.getCollectionFromName(it.toString()) } ?: -1 }
                ) { selectedCard, selcolid ->
                    val i = Intent(this, ViewCard::class.java)
                    i.putExtra("col", selcolid)
                    i.putExtra("pcard", selectedCard)
                    startActivity(i)
                }
                rview.setHasFixedSize(true)
                rview.adapter = rviewadap
            }

        }
    }

    fun loadSpinner(context: Context, db: DBHelper, spcols: Spinner, colid: Int) {
        lifecycleScope.launch(Dispatchers.IO) {

            val collections = mutableListOf<String>()
            db.readableDatabase.use { rdb ->
                val cur = rdb.rawQuery(
                    "SELECT ${DBHelper.COL_NAME} FROM ${DBHelper.TABLA_COLECCIONES}",
                    null
                )
                cur.use {
                    while (it.moveToNext()) {
                        val name = it.getString(it.getColumnIndexOrThrow(DBHelper.COL_NAME))
                        collections.add(name)
                    }
                }
            }

            withContext(Dispatchers.Main) {

                val adapter = ArrayAdapter<String>(context, android.R.layout.simple_spinner_item)
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)

                if (collections.isEmpty()) {
                    adapter.add(getString(R.string.no_existen_colecciones))
                    spcols.isEnabled = false
                } else {
                    for (name in collections) adapter.add(name)
                    spcols.isEnabled = true
                }
                spcols.adapter = adapter

                spcols.setSelection(adapter.getPosition(db.getCollectionFromID(colid)))

            }


        }
    }

    fun getCards(et: EditText): List<CardDeckCheck> {
        val input = et.text.toString()

        val regex =
            Regex("""^(\d+)\s+(.+?)\s+(?=[A-Z]{2,4}\s+\d+$)([A-Z]{2,4})\s+(\d+)$""")

        val cards = input.lines()
            .mapNotNull { line ->
                regex.matchEntire(line.trim())?.destructured?.let { (count, name, ptcgocode, number) ->
                    CardDeckCheck(count.toInt(), name.trim(), ptcgocode, number.toInt())
                }
            }
        return cards
    }
}
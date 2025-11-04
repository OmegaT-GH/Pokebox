package com.omegat.pokebox.activities

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.omegat.pokebox.R
import com.omegat.pokebox.adapters.ListPercentageAdapter
import com.omegat.pokebox.adapters.ListSetsAdapter
import com.omegat.pokebox.bd.DBHelper
import com.omegat.pokebox.data.PokemonSet
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.google.gson.stream.JsonReader

class ListSets : AppCompatActivity() {

    lateinit var rviewadap: ListPercentageAdapter
    lateinit var sets: List<PokemonSet>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_list_sets)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val rview = findViewById<RecyclerView>(R.id.rviewsetsearch)
        val db = DBHelper(this)

        val sinputStream = assets.open("json/sets/en.json")
        val sreader = JsonReader(sinputStream.reader())
        val stype = object : TypeToken<List<PokemonSet>>() {}.type
        sets = Gson().fromJson(sreader, stype)
        sreader.close()

        val lmanager = LinearLayoutManager(this)
        rview.layoutManager = lmanager
        rview.clipToPadding = false
        rview.setPadding(16, 16, 16, 16)


        val mode = intent.getStringExtra("mode")

        if (mode == "percentage") {

            val colid = intent.getIntExtra("col", -1)
            Log.d(null,colid.toString())
            val cur = db.getSetswithcards(colid)
            val setids = mutableListOf<String>()

            cur.use {
                while (it.moveToNext()) {
                    setids.add(it.getString(it.getColumnIndexOrThrow(DBHelper.SET_ID)))
                }
            }

            val setswithcards = sets.filter { it.id in setids }
            val perc = mutableListOf<Int>()
            val percamount = mutableListOf<Int>()

            for (set in setswithcards) {
                val p = db.getSetPercentage(set.id.toString(), colid)
                val pa = db.getSetCardAmount(set.id.toString(), colid)
                perc.add(p)
                percamount.add(pa)
            }

            rviewadap  = ListPercentageAdapter(this, setswithcards, perc, percamount) { selectedSet ->
                //Toast.makeText(this, "Pulsado: ${selectedSet.name}", Toast.LENGTH_SHORT).show()

                val i = Intent(this, ListCards::class.java)
                i.putExtra("pset", selectedSet)
                i.putExtra("col", colid)
                this.startActivity(i)

            }
            rview.adapter = rviewadap

        } else if (mode == "list") {

            val colid = intent.getIntExtra("col", -1)
            Log.d(null,colid.toString())

            rview.adapter = ListSetsAdapter(this, sets) { selectedSet ->
                //Toast.makeText(this, "Pulsado: ${selectedSet.name}", Toast.LENGTH_SHORT).show()

                val i = Intent(this, ListCards::class.java)
                i.putExtra("pset", selectedSet)
                i.putExtra("col", colid)
                this.startActivity(i)

            }

        }

    }

    @SuppressLint("NotifyDataSetChanged")
    override fun onResume() {
        super.onResume()

        if (intent.getStringExtra("mode") == "percentage" && this::rviewadap.isInitialized) {
            val db = DBHelper(this)
            val colid = intent.getIntExtra("col", -1)

            val setswithcards = rviewadap.currentList()

            val newPerc = mutableListOf<Int>()
            val newPercAmount = mutableListOf<Int>()

            for (set in setswithcards) {
                val perc = db.getSetPercentage(set.id, colid)
                val am = db.getSetCardAmount(set.id ?: "", colid)
                newPerc.add(perc)
                newPercAmount.add(am)
            }

            rviewadap.updateData(newPerc, newPercAmount)
        }
    }

}
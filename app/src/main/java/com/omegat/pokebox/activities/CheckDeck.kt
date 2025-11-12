package com.omegat.pokebox.activities

import android.content.Context
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Spinner
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.omegat.pokebox.R
import com.omegat.pokebox.bd.DBHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class CheckDeck : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_check_deck)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val db = DBHelper(this)
        val colid = intent.getIntExtra("col", -1)

        val spcols = findViewById<Spinner>(R.id.spDeckCollections)

        loadSpinner(this, db, spcols, colid)
    }

    fun loadSpinner (context: Context, db: DBHelper, spcols: Spinner, colid: Int) {
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

}
package com.example.pokebox.activities

//import android.widget.Toast
import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ImageView
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.example.pokebox.R
import com.example.pokebox.bd.DBHelper
import com.example.pokebox.data.PokemonCard
import com.example.pokebox.data.PokemonSet
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.google.gson.stream.JsonReader
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

class ViewCard : AppCompatActivity() {

    lateinit var pset: PokemonSet

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

        val card = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra("pcard", PokemonCard::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra<PokemonCard>("pcard")
        }

        val ivcard = findViewById<ImageView>(R.id.ivCardLarge)
        val name = findViewById<TextView>(R.id.tvCardName)
        val number = findViewById<TextView>(R.id.tvCardNumber)
        val supertype = findViewById<TextView>(R.id.tvCardSupertype)
        val subtype = findViewById<TextView>(R.id.tvCardSubtype)
        val set = findViewById<TextView>(R.id.tvCardSet)
        val type = findViewById<TextView>(R.id.tvCardType)
        val rarity = findViewById<TextView>(R.id.tvCardRarity)
        val artist = findViewById<TextView>(R.id.tvCardArtist)

        val btadd = findViewById<Button>(R.id.btAddToCollection)

        val sinputStream = assets.open("json/sets/en.json")
        val sreader = JsonReader(sinputStream.reader())
        val stype = object : TypeToken<List<PokemonSet>>() {}.type
        val sets: List<PokemonSet> = Gson().fromJson(sreader, stype)
        sreader.close()

        val cardid = card?.id
        val setid = cardid?.substringBefore("-")
        //Toast.makeText(this, setid, Toast.LENGTH_SHORT).show()

        val db = DBHelper(this)


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

        btadd.setOnClickListener {
            AddDialog(this, db, cardid.toString())
        }


    }

    fun AddDialog(context: Context, db: DBHelper, cardId: String) {
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
                val cont = ConstraintLayout(context)
                val spcols = Spinner(context)
                spcols.id = View.generateViewId()

                val params = ConstraintLayout.LayoutParams(
                    ConstraintLayout.LayoutParams.MATCH_PARENT,
                    ConstraintLayout.LayoutParams.WRAP_CONTENT
                )
                params.setMargins(24, 24, 24, 24)
                spcols.layoutParams = params
                cont.addView(spcols)

                val adapter = ArrayAdapter<String>(context, android.R.layout.simple_spinner_item)
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)

                if (collections.isEmpty()) {
                    adapter.add("No existen colecciones.")
                    spcols.isEnabled = false
                } else {
                    for (name in collections) adapter.add(name)
                    spcols.isEnabled = true
                }
                spcols.adapter = adapter

                val adbuilder = AlertDialog.Builder(context)
                adbuilder.setTitle("Seleccionar colección:")
                adbuilder.setView(cont)
                adbuilder.setPositiveButton("Aceptar", null)
                adbuilder.setNegativeButton("Cancelar") { dialog, which -> dialog.dismiss() }


                val dialog = adbuilder.create()

                dialog.setOnShowListener {
                    val positive = dialog.getButton(AlertDialog.BUTTON_POSITIVE)

                    positive.isEnabled = collections.isNotEmpty() && spcols.isEnabled

                    positive.setOnClickListener {
                        val colname = spcols.selectedItem.toString()
                        CoroutineScope(Dispatchers.IO).launch {
                            DBHelper.dbMutex.withLock {
                                val colid = db.getCollectionFromName(colname)
                                val wdb = db.writableDatabase

                                val success = db.addCardtoCollection(colid, cardId, wdb)
                                withContext(Dispatchers.Main) {
                                    if (success) {
                                        Toast.makeText(
                                            context,
                                            "Añadido a '$colname'",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    } else {
                                        Toast.makeText(
                                            context,
                                            "Error añadiendo a la colección.",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                    dialog.dismiss()
                                }
                            }
                        }
                    }
                }

                dialog.show()
            }
        }
    }

}
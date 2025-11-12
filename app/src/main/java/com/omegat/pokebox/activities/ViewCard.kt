package com.omegat.pokebox.activities

//import android.widget.Toast
import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ImageView
import android.widget.NumberPicker
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.google.gson.stream.JsonReader
import com.omegat.pokebox.R
import com.omegat.pokebox.bd.DBHelper
import com.omegat.pokebox.data.PokemonCard
import com.omegat.pokebox.data.PokemonSet
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

        val colid = intent.getIntExtra("col", -1)
        val card = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra("pcard", PokemonCard::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra("pcard")
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

        if (card?.subtypes?.contains("BREAK") == true || card?.subtypes?.contains("LEGEND") == true) {
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
        artist.text = "Artist: ${card?.artist ?: getString(R.string.info_not_available)}"

        btadd.setOnClickListener {
            addDialog(this, db, cardid.toString(), colid)
        }


    }

    fun addDialog(context: Context, db: DBHelper, cardId: String, colid: Int) {
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

                val inflater = LayoutInflater.from(context)
                val dialogView = inflater.inflate(R.layout.dialog_add_to_collection, null)

                val spCols = dialogView.findViewById<Spinner>(R.id.spColeccionesATC)
                val npCantidad = dialogView.findViewById<NumberPicker>(R.id.npCantidad)

                if (collections.isEmpty()) {
                    adapter.add(getString(R.string.no_existen_colecciones))
                    spCols.isEnabled = false
                    spCols.adapter = adapter
                } else {
                    for (name in collections) adapter.add(name)
                    spCols.isEnabled = true
                    spCols.adapter = adapter
                    spCols.setSelection(adapter.getPosition(db.getCollectionFromID(colid)))
                }

                npCantidad.minValue = 0
                npCantidad.maxValue = 99
                npCantidad.value = 0



                spCols.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                    override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                        val colName = spCols.selectedItem.toString()
                        lifecycleScope.launch(Dispatchers.IO) {
                            DBHelper.dbMutex.withLock {
                                val colId = db.getCollectionFromName(colName)
                                val cantidad = db.getCardAmount(colId, cardId)
                                withContext(Dispatchers.Main) {
                                    npCantidad.value = cantidad
                                }
                            }
                        }
                    }
                    override fun onNothingSelected(parent: AdapterView<*>?) {}
                }

                val adBuilder = AlertDialog.Builder(context)
                    .setTitle(getString(R.string.add_card_to_collection))
                    .setView(dialogView)
                    .setPositiveButton(getString(R.string.add), null)
                    .setNegativeButton(R.string.cancelar, null)

                val pickerdialog = adBuilder.create()

                pickerdialog.setOnShowListener {
                    val positive = pickerdialog.getButton(AlertDialog.BUTTON_POSITIVE)
                    positive.isEnabled = collections.isNotEmpty()

                    positive.setOnClickListener {
                        val colName = spCols.selectedItem.toString()
                        val cantidad = npCantidad.value

                        lifecycleScope.launch (Dispatchers.IO) {
                            DBHelper.dbMutex.withLock {
                                val colId = db.getCollectionFromName(colName)
                                val wdb = db.writableDatabase

                                val success = db.addCardtoCollection(colId, cardId, wdb, cantidad)

                                withContext(Dispatchers.Main) {
                                    Toast.makeText(
                                        context,
                                        if (success)
                                            "${getString(R.string.a_adido_a)} '$colName' ($cantidad)"
                                        else
                                            getString(R.string.error_a_adiendo_a_la_colecci_n),
                                        Toast.LENGTH_SHORT
                                    ).show()

                                    pickerdialog.dismiss()
                                }
                            }
                        }
                    }
                }

                pickerdialog.show()
            }
        }
    }

}
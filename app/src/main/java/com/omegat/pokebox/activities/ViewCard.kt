package com.omegat.pokebox.activities

import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.LayerDrawable

//import android.widget.Toast
import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.TextView
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
import io.pokemontcg.Pokemon
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

        Log.d("ViewCard", "coleccion:$colid")

        val ivcard = findViewById<ImageView>(R.id.ivCardLarge)
        val name = findViewById<TextView>(R.id.tvCardName)
        val number = findViewById<TextView>(R.id.tvCardNumber)
        val supertype = findViewById<TextView>(R.id.tvCardSupertype)
        val subtype = findViewById<TextView>(R.id.tvCardSubtype)
        val set = findViewById<TextView>(R.id.tvCardSet)
        val type = findViewById<TextView>(R.id.tvCardType)
        val rarity = findViewById<TextView>(R.id.tvCardRarity)
        val artist = findViewById<TextView>(R.id.tvCardArtist)

        val btadd = findViewById<soup.neumorphism.NeumorphCardView>(R.id.btAddToCollection)

        val sinputStream = assets.open("json/sets/en.json")
        val sreader = JsonReader(sinputStream.reader())
        val stype = object : TypeToken<List<PokemonSet>>() {}.type
        val sets: List<PokemonSet> = Gson().fromJson(sreader, stype)
        sreader.close()

        val cardid = card?.id
        val setid = cardid?.substringBefore("-")
        //Toast.makeText(this, setid, Toast.LENGTH_SHORT).show()

        val db = DBHelper(this)

        val poke = Pokemon("5d772eb0-136d-4069-80d3-74c11a3009a1")
        findCardValue(poke, cardid)


        for (set in sets) {
            if (set.id == setid) {
                pset = set
            }
        }

        if (card?.subtypes?.contains("BREAK") == true || card?.subtypes?.contains("LEGEND") == true) {
            ivcard.rotation = 90f
        }

        Glide.with(this)
            .load(card?.images?.large)
            .placeholder(R.drawable.placeholdercard)
            .error(R.drawable.placeholdercard)
            .fitCenter()
            .into(ivcard)
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

        // Set button color based on card types
        val types = card?.types
        if (!types.isNullOrEmpty()) {
            val buttonDrawable = when {
                types.size >= 2 -> createGradientDrawable(types[0], types[1])
                else -> createSolidDrawable(types[0])
            }
            btadd.setBackgroundColor(getTypeColor(types[0]))
        } else if (card?.supertype?.lowercase() == "trainer") {
            btadd.setBackgroundColor(getTypeColor("trainer"))
        }


    }

    @SuppressLint("SetTextI18n")
    private fun findCardValue(poke: Pokemon, cardid: String?) {

        val aptext = getString(R.string.avgprice)
        val tvPrice = findViewById<TextView>(R.id.tvAvgPrice)
        tvPrice.text = "$aptext: ${getString(R.string.loading)}"

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val card = poke.card().find(cardid.toString())
                val avgprice = card.cardMarket?.prices?.averageSellPrice

                withContext(Dispatchers.Main) {
                    tvPrice.text = "$aptext: ${avgprice ?: "N/A"}€"
                }

            } catch (e: Exception) {
                Log.e("findCardValue", "Error: ${e.message}")

                withContext(Dispatchers.Main) {
                    tvPrice.text = "$aptext: N/A"
                }
            }
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

                val adapter = ArrayAdapter(context, R.layout.spinner_item, collections)
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)

                val inflater = LayoutInflater.from(context)
                val dialogView = inflater.inflate(R.layout.dialog_add_to_collection, null)

                val spCols = dialogView.findViewById<Spinner>(R.id.spColeccionesATC)
                val tvCantidad = dialogView.findViewById<TextView>(R.id.tvCantidad)
                val btPlus = dialogView.findViewById<soup.neumorphism.NeumorphCardView>(R.id.btPlus)
                val btMinus = dialogView.findViewById<soup.neumorphism.NeumorphCardView>(R.id.btMinus)
                
                var cantidad = 0

                if (collections.isEmpty()) {
                    spCols.isEnabled = false
                } else {
                    spCols.isEnabled = true
                    spCols.adapter = adapter
                    spCols.setSelection(adapter.getPosition(db.getCollectionFromID(colid)))
                }

                btPlus.setOnClickListener {
                    if (cantidad < 99) {
                        cantidad++
                        tvCantidad.text = cantidad.toString()
                    }
                }

                btMinus.setOnClickListener {
                    if (cantidad > 0) {
                        cantidad--
                        tvCantidad.text = cantidad.toString()
                    }
                }

                spCols.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                    override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                        val colName = spCols.selectedItem.toString()
                        lifecycleScope.launch(Dispatchers.IO) {
                            DBHelper.dbMutex.withLock {
                                val colId = db.getCollectionFromName(colName)
                                cantidad = db.getCardAmount(colId, cardId)
                                withContext(Dispatchers.Main) {
                                    tvCantidad.text = cantidad.toString()
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

                        lifecycleScope.launch (Dispatchers.IO) {
                            DBHelper.dbMutex.withLock {
                                val colId = db.getCollectionFromName(colName)
                                val wdb = db.writableDatabase

                                val success = db.addCardtoCollection(colId, cardId, wdb, cantidad)

                                withContext(Dispatchers.Main) {
                                    if (success) {
                                        Log.d(
                                            "add",
                                            "${getString(R.string.a_adido_a)} '$colName' ($cantidad)"
                                        )
                                        setResult(RESULT_OK)
                                    } else {
                                        Log.d(
                                            "add",
                                            getString(R.string.error_a_adiendo_a_la_colecci_n)
                                        )
                                    }

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

    private fun getTypeColor(type: String): Int {
        return when (type.lowercase()) {
            "grass" -> 0xFF8FD460.toInt()
            "fire" -> 0xFFE85D4A.toInt()
            "water" -> 0xFF7BA3F0.toInt()
            "lightning", "electric" -> 0xFFFFC850.toInt()
            "psychic" -> 0xFFFF7AA8.toInt()
            "fighting" -> 0xFFFF9850.toInt()
            "darkness", "dark" -> 0xFF6A7A9A.toInt()
            "metal", "steel" -> 0xFFD0D0E0.toInt()
            "dragon" -> 0xFFD3C13E.toInt()
            "fairy" -> 0xFFFFB0C8.toInt()
            "colorless", "normal" -> 0xFFB0B098.toInt()
            "trainer" -> 0xFF6A6A6C.toInt()
            else -> 0xFF7DB3F5.toInt()
        }
    }

    private fun createSolidDrawable(type: String): LayerDrawable {
        val color = getTypeColor(type)
        val layers = arrayOfNulls<android.graphics.drawable.Drawable>(3)
        
        layers[0] = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            setColor(0xFF1C1C1E.toInt())
            cornerRadius = 28f
        }
        
        layers[1] = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            setColor(0xFF3C3C3E.toInt())
            cornerRadius = 28f
        }
        
        layers[2] = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            setColor(color)
            cornerRadius = 28f
        }
        
        val layerDrawable = LayerDrawable(layers)
        layerDrawable.setLayerInset(0, 0, 0, 8, 8)
        layerDrawable.setLayerInset(1, 8, 8, 0, 0)
        layerDrawable.setLayerInset(2, 4, 4, 4, 4)
        
        return layerDrawable
    }

    private fun createGradientDrawable(type1: String, type2: String): LayerDrawable {
        val color1 = getTypeColor(type1)
        val color2 = getTypeColor(type2)
        val layers = arrayOfNulls<android.graphics.drawable.Drawable>(3)
        
        layers[0] = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            setColor(0xFF1C1C1E.toInt())
            cornerRadius = 28f
        }
        
        layers[1] = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            setColor(0xFF3C3C3E.toInt())
            cornerRadius = 28f
        }
        
        layers[2] = GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT, intArrayOf(color1, color2)).apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = 28f
        }
        
        val layerDrawable = LayerDrawable(layers)
        layerDrawable.setLayerInset(0, 0, 0, 8, 8)
        layerDrawable.setLayerInset(1, 8, 8, 0, 0)
        layerDrawable.setLayerInset(2, 4, 4, 4, 4)
        
        return layerDrawable
    }

}
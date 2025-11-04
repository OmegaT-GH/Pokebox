package com.omegat.pokebox

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.ListView
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.bumptech.glide.Glide
import com.omegat.pokebox.activities.ListSets
import com.omegat.pokebox.activities.MainMenu
import com.omegat.pokebox.adapters.ListAdapter
import com.omegat.pokebox.data.PokemonCard
import com.omegat.pokebox.data.PokemonSet
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.google.gson.stream.JsonReader


class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        /*
        *
        * Filtros para la búsqueda
        * todos tendrán la opción de expandir o colapsar
        *
        * Búsqueda por nombre (EditText)
        *
        * Carta (checkboxes)
        * Pokémon, Entrenador, Energía
        *
        * Tipo de carta (checkboxes)
        * Añadir aqui todos los tipos (Fase 1, 2, Básico, EX, Prisma, etc)
        *
        * Tipo (checkboxes)
        * Planta, Fuego, Agua, etc. (si se puede, poner iconos)
        *
        * Legalidad (checkboxes)
        * Unlimited, Expanded, Standard
        *
        * Rareza (checkboxes)
        * Common, Uncommon, Rare, SIR, Amazing Rare, etc. (si se puede, poner iconos)
        *
        * Artista (List)
        * Lista de los artistas/ilustradores en orden alfabético
        *
        * Tiene Habilidad (una checkbox)
        *
        * Vida (rango Desde-Hasta. si no se pone nada será null/0-9999999)
        *
        * */

        /*

        * Tablas BD
        *
        * Set
        * -----------------
        * setID     Nombre
        * me1       Mega Evolution
        * neo1      Neo Genesis
        *
        * Card
        * -----------------
        * cardID    Set
        * me1-102   me1
        * neo1-5    neo1
        *
        * Collection
        * -----------------
        * colID     Nombre
        * 1         My Collection
        * 2         Yuka Morii Cards
        *
        * OwnedCards
        * -----------------
        * colID     cardID      amount
        * 1         me1-102     3
        * 1         me1-59      1
        * 2         me1-59      1
        * 2         neo1-23     2
        *
        * */

        /*
        * Pokémon: 22
        * 4 Dreepy TWM 128
        * 4 Drakloak TWM 129
        * 2 Dragapult ex TWM 130
        * 2 Duskull PRE 35
        * 2 Dusclops PRE 36
        * 1 Dusknoir PRE 37
        * 2 Budew PRE 4
        * 1 Hawlucha SVI 118
        * 1 Latias ex SSP 76
        * 1 Munkidori TWM 95
        * 1 Fezandipiti ex SFA 38
        * 1 Bloodmoon Ursaluna ex TWM 141
        *
        * Trainer: 31
        * 4 Iono PAL 185
        * 4 Professor's Research JTG 155
        * 3 Boss's Orders MEG 114
        * 2 Lillie's Determination MEG 119
        * 1 Professor Turo's Scenario PAR 171
        * 4 Ultra Ball MEG 131
        * 4 Buddy-Buddy Poffin TEF 144
        * 4 Night Stretcher SFA 61
        * 3 Counter Catcher PAR 160
        * 1 Nest Ball SVI 181
        * 1 Jamming Tower TWM 153
        *
        * Energy: 7
        * 3 Luminous Energy PAL 191
        * 2 Psychic Energy SVE 21
        * 1 Fire Energy SVE 18
        * 1 Neo Upper Energy TEF 162
        * */

        val btloadsets = findViewById<Button>(R.id.btLoadSets)
        val btsv9 = findViewById<Button>(R.id.btloadSV9)
        val btlista = findViewById<Button>(R.id.btcard)



        val sinputStream = assets.open("json/sets/en.json")
        val sreader = JsonReader(sinputStream.reader())
        val stype = object : TypeToken<List<PokemonSet>>() {}.type
        val sets: List<PokemonSet> = Gson().fromJson(sreader, stype)
        sreader.close()


        btloadsets.setOnClickListener{
            val i = Intent(this, ListSets::class.java)
            this.startActivity(i)
        }

        btsv9.setOnClickListener{
            cargardatos(sets, "sv9")
        }

        btlista.setOnClickListener {
            val i = Intent(this, MainMenu::class.java)
            this.startActivity(i)
        }




    }

    fun cargarinfoset(sets : List<PokemonSet>, id: String): PokemonSet? {
        val set : PokemonSet? = sets.find{it.id == id}
        return set
    }

    fun cargardatos(sets : List<PokemonSet>, id: String) {

        val listView = findViewById<ListView>(R.id.listview)
        val iview = findViewById<ImageView>(R.id.setimage)
        val tview = findViewById<TextView>(R.id.testtexto)

        val set : PokemonSet? = cargarinfoset(sets, id)
        val nombre : String = set?.name ?: "error"
        val series : String = set?.series ?: "error"
        val total : Int = set?.total ?: 0
        val printedtotal : Int = set?.printedTotal ?: 0


        """Set name: $nombre
    Series: $series
    Cards: $printedtotal/$total""".also { tview.text = it }

        Glide.with(applicationContext).load(set?.images?.symbol).into(iview)


        val cinputStream = assets.open("json/cards/en/$id.json")
        val creader = JsonReader(cinputStream.reader())
        val ctype = object : TypeToken<List<PokemonCard>>() {}.type
        val cards: List<PokemonCard> = Gson().fromJson(creader, ctype)
        creader.close()

        val adapter = ListAdapter(this, cards)
        listView.adapter = adapter
    }
}
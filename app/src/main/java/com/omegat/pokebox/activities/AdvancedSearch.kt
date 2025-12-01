package com.omegat.pokebox.activities

import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.CheckBox
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RelativeLayout
import android.widget.Spinner
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.children
import androidx.drawerlayout.widget.DrawerLayout
import com.omegat.pokebox.R
import com.omegat.pokebox.data.CardRepository
import androidx.core.view.isGone
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.omegat.pokebox.adapters.ListCardsAdapter
import com.omegat.pokebox.bd.DBHelper
import com.omegat.pokebox.data.CardFilter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AdvancedSearch : AppCompatActivity() {

    private val filterCheckboxes = mutableMapOf<String, List<CheckBox>>()
    private var artistSpinner: Spinner? = null
    private var minHpEditText: EditText? = null
    private var maxHpEditText: EditText? = null
    private var sortSpinner: Spinner? = null
    lateinit var rviewadap: ListCardsAdapter

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_advanced_search)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val db = DBHelper(this)
        val colid = intent.getIntExtra("col", -1)

        val btsearch = findViewById<LinearLayout>(R.id.btSearch)
        val btfilters = findViewById<LinearLayout>(R.id.btOpenFilters)
        val drawer = findViewById<DrawerLayout>(R.id.main)
        val rview = findViewById<RecyclerView>(R.id.rviewfilteredsearch)
        val lmanager = LinearLayoutManager(this)
        rview.layoutManager = lmanager


        val filterContainer = findViewById<LinearLayout>(R.id.contFiltro)
        val title = TextView(this).apply {
            text = context.getString(R.string.filtrosdebusqueda)
            textSize = 20f
            setPadding(16, 16, 16, 16)
            setTextAppearance(R.style.textboldbig)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = 100
                bottomMargin = 20
            }
        }
        filterContainer.addView(title)

        val allCards = CardRepository.getCards()

        val allSupertypes = allCards.mapNotNull { it.supertype }.distinct().sorted()
        val allSubtypes = allCards.flatMap { it.subtypes ?: emptyList() }.distinct().sorted()
        val allTypes = allCards.flatMap { it.types ?: emptyList() }.distinct().sorted()
        val allLegalities = allCards.flatMap { card ->
            val list = mutableListOf<String>()
            card.legalities?.let { l ->
                l.unlimited?.let { list.add("Unlimited $it") }
                l.expanded?.let { list.add("Expanded $it") }
                l.standard?.let { list.add("Standard $it") }
            }
            list
        }.distinct().sorted()
        val allRarities = allCards.mapNotNull { it.rarity }.distinct().sorted()
        val allArtists = allCards.mapNotNull { it.artist }.distinct().sorted()
        val hasAbility = listOf(getString(R.string.yes), getString(R.string.no))

        addFilterSection("supertype", getString( R.string.supertype), allSupertypes)
        addFilterSection("subtype", getString(R.string.subtype), allSubtypes)
        addFilterSection("type", getString(R.string.type), allTypes)
        addFilterSection("legality", getString(R.string.legality), allLegalities)
        addFilterSection("rarity", getString(R.string.rarity), allRarities)
        addSpinnerSection("artist", getString(R.string.artist), allArtists)
        addFilterSection("hasability", getString(R.string.has_ability), hasAbility)
        addHPSection()

        addSortSection()

        btsearch.setOnClickListener {

            val filter = collectFilters()

            lifecycleScope.launch(Dispatchers.IO) {

                val sortCriteria = sortSpinner?.selectedItem?.toString() ?: getString(R.string.m_s_recientes_primero)

                val filteredcards = CardRepository.getFilteredCards(filter)
                val sortedcards = when (sortCriteria) {
                    getString(R.string.relevant) -> filteredcards
                    getString(R.string.nombre_a_z) -> filteredcards.sortedBy { it.name?.lowercase() }
                    getString(R.string.nombre_z_a) -> filteredcards.sortedByDescending { it.name?.lowercase() }
                    getString(R.string.m_s_recientes_primero) -> filteredcards.sortedByDescending { it.releaseDate }
                    getString(R.string.m_s_antiguas_primero) -> filteredcards.sortedBy { it.releaseDate }
                    else -> filteredcards
                }

                val cardamounts = mutableListOf<Int>()
                for (card in sortedcards) {
                    if (colid != -1) {
                        val am = db.getCardAmount(colid, card.id)
                        cardamounts.add(am)
                    } else {
                        cardamounts.add(0)
                    }
                }

                withContext(Dispatchers.Main) {

                    if (::rviewadap.isInitialized) {
                        rviewadap.updateData(sortedcards, cardamounts)
                    } else {
                        rviewadap = ListCardsAdapter(this@AdvancedSearch, sortedcards, cardamounts) { selectedCard ->
                            val i = Intent(this@AdvancedSearch, ViewCard::class.java)
                            i.putExtra("pcard", selectedCard)
                            startActivity(i)
                        }

                        rview.setHasFixedSize(true)
                        rview.adapter = rviewadap
                    }

                }

            }




        }

        btfilters.setOnClickListener {
            drawer.openDrawer(GravityCompat.START)
        }

    }

    fun addFilterSection(key: String, title: String, options: List<String>) {
        val sectionLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }

        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(8, 8, 8, 8)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            gravity = Gravity.CENTER_VERTICAL
        }

        val titleView = TextView(this).apply {
            text = title
            textSize = 25f
            setTextAppearance(R.style.textboldbig)
            layoutParams = LinearLayout.LayoutParams(
                    0,
            LinearLayout.LayoutParams.WRAP_CONTENT,
            1f
            )
        }

        val arrow = ImageView(this).apply {
            setImageResource(R.drawable.arrow_right)
        }

        header.addView(titleView)
        header.addView(arrow)
        sectionLayout.addView(header)

        val contentLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            visibility = View.GONE
        }

        options.forEach { option ->
            val cb = CheckBox(this).apply {
                text = option
                setTextAppearance(R.style.text)
            }
            contentLayout.addView(cb)
        }

        sectionLayout.addView(contentLayout)

        header.setOnClickListener {
            if (contentLayout.isGone) {
                contentLayout.visibility = View.VISIBLE
                arrow.rotation = 90f
            } else {
                contentLayout.visibility = View.GONE
                arrow.rotation = 0f
            }
        }

        findViewById<LinearLayout>(R.id.contFiltro).addView(sectionLayout)
        filterCheckboxes[key] = contentLayout.children.filterIsInstance<CheckBox>().toList()
    }
    fun addSpinnerSection(key: String, title: String, options: List<String>) {
        val sectionLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }

        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(8,8,8,8)
        }

        val titleView = TextView(this).apply {
            text = title
            textSize = 25f
            setTextAppearance(R.style.textboldbig)
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }

        val arrow = ImageView(this).apply {
            setImageResource(R.drawable.arrow_right)
        }

        header.addView(titleView)
        header.addView(arrow)
        sectionLayout.addView(header)

        val spinnerContainer = RelativeLayout(this).apply {
            layoutParams = RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.MATCH_PARENT,
                RelativeLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, 8, 0, 8)
            }
            setBackgroundResource(R.drawable.spinner_background_white)
        }

        val spinner = Spinner(this).apply {
            val spinnerOptions = mutableListOf(context.getString(R.string.all))
            spinnerOptions.addAll(options)

            val adapter = object : ArrayAdapter<String>(context, android.R.layout.simple_spinner_item, spinnerOptions) {
                override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                    val v = super.getView(position, convertView, parent) as TextView
                    v.setTextAppearance(R.style.text)
                    return v
                }

                override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
                    val v = super.getDropDownView(position, convertView, parent) as TextView
                    v.setTextAppearance(R.style.text)
                    return v
                }
            }

            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            this.adapter = adapter

            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = 8
                bottomMargin = 8

            }
            setPadding(16, 8, 16, 8)
        }

        spinnerContainer.addView(spinner)
        spinnerContainer.visibility = View.GONE
        sectionLayout.addView(spinnerContainer)

        header.setOnClickListener {
            spinnerContainer.visibility = if (spinnerContainer.isGone) {
                arrow.rotation = 90f
                View.VISIBLE
            } else {
                arrow.rotation = 0f
                View.GONE
            }
        }

        findViewById<LinearLayout>(R.id.contFiltro).addView(sectionLayout)
        if (key == "artist") {
            artistSpinner = spinner
        }
    }
    fun addHPSection() {
        val sectionLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }

        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(8, 8, 8, 8)
        }

        val titleView = TextView(this).apply {
            text = context.getString(R.string.rangohp)
            textSize = 25f
            setTextAppearance(R.style.textboldbig)
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }

        val arrow = ImageView(this).apply {
            setImageResource(R.drawable.arrow_right)
        }

        header.addView(titleView)
        header.addView(arrow)
        sectionLayout.addView(header)

        val contentLayout = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            visibility = View.GONE
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = 8 }
        }

        val minHP = EditText(this).apply {
            hint = context.getString(R.string.min)
            inputType = InputType.TYPE_CLASS_NUMBER
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                marginEnd = 8
            }
            setPadding(16, 20, 16, 20)
        }

        val maxHP = EditText(this).apply {
            hint = context.getString(R.string.max)
            inputType = InputType.TYPE_CLASS_NUMBER
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            setPadding(16, 20, 16, 20)
        }

        contentLayout.addView(minHP)
        contentLayout.addView(maxHP)
        sectionLayout.addView(contentLayout)

        header.setOnClickListener {
            contentLayout.visibility = if (contentLayout.isGone) {
                arrow.rotation = 90f
                View.VISIBLE
            } else {
                arrow.rotation = 0f
                View.GONE
            }
        }

        findViewById<LinearLayout>(R.id.contFiltro).addView(sectionLayout)
        minHpEditText = minHP
        maxHpEditText = maxHP
    }
    private fun addSortSection() {
        val sortingSpinner = findViewById<Spinner>(R.id.spOrdenar)

        val options = listOf(getString(R.string.relevant), getString(R.string.m_s_recientes_primero), getString(R.string.m_s_antiguas_primero), getString(R.string.nombre_a_z), getString(R.string.nombre_z_a))

        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, options)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        sortingSpinner.adapter = adapter
        sortSpinner = sortingSpinner
    }
    private fun collectFilters(): CardFilter {
        val selectedSupertypes = filterCheckboxes["supertype"]?.filter { it.isChecked }?.map { it.text.toString() }.orEmpty()
        val selectedSubtypes = filterCheckboxes["subtype"]?.filter { it.isChecked }?.map { it.text.toString() }.orEmpty()
        val selectedTypes = filterCheckboxes["type"]?.filter { it.isChecked }?.map { it.text.toString() }.orEmpty()
        val selectedLegalities = filterCheckboxes["legality"]?.filter { it.isChecked }?.map { it.text.toString() }.orEmpty()
        val selectedRarities = filterCheckboxes["rarity"]?.filter { it.isChecked }?.map { it.text.toString() }.orEmpty()

        val hasAbilityFilter = filterCheckboxes["hasability"]?.firstOrNull { it.isChecked }?.text?.let {
            when (it) {
                getString(R.string.yes) -> true
                getString(R.string.no) -> false
                else -> null
            }
        }

        val name = findViewById<EditText>(R.id.etSearch)?.text?.toString()?.takeIf { it.isNotBlank() }

        val artist = artistSpinner?.selectedItem?.toString()?.takeIf { it != getString(R.string.all) }

        val minHP = minHpEditText?.text?.toString()?.toIntOrNull()
        val maxHP = maxHpEditText?.text?.toString()?.toIntOrNull()

        return CardFilter(
            nombre = name,
            supertype = selectedSupertypes,
            subtype = selectedSubtypes,
            type = selectedTypes,
            legality = selectedLegalities,
            rarity = selectedRarities,
            artist = artist,
            hasability = hasAbilityFilter,
            minHP = minHP,
            maxHP = maxHP
        )
    }
}
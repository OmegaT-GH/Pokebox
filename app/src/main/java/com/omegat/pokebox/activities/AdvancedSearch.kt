package com.omegat.pokebox.activities

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.ArrayAdapter
import android.widget.CheckBox
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isGone
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.omegat.pokebox.R
import com.omegat.pokebox.adapters.ListCardsAdapter
import com.omegat.pokebox.bd.DBHelper
import com.omegat.pokebox.data.CardFilter
import com.omegat.pokebox.data.CardRepository
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

        val btsearch = findViewById<soup.neumorphism.NeumorphCardView>(R.id.btSearch)
        val btfilters = findViewById<soup.neumorphism.NeumorphCardView>(R.id.btOpenFilters)
        val drawer = findViewById<DrawerLayout>(R.id.main)
        val rview = findViewById<RecyclerView>(R.id.rviewfilteredsearch)
        val lmanager = LinearLayoutManager(this)
        rview.layoutManager = lmanager

        setupFilterMenu()
        setupSortSpinner()

        btsearch.setOnClickListener {
            val filter = collectFilters()
            performSearch(filter, db, colid, rview)
        }

        btfilters.setOnClickListener {
            drawer.openDrawer(GravityCompat.START)
        }
    }

    private fun setupFilterMenu() {
        val filterContainer = findViewById<LinearLayout>(R.id.contFiltro)
        filterContainer.removeAllViews()

        // Add professional header
        addFilterHeader(filterContainer)

        val allCards = CardRepository.getCards()

        // Extract filter options
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

        // Add filter sections with new professional design
        addProfessionalFilterSection(filterContainer, "supertype", getString(R.string.supertype), allSupertypes)
        addProfessionalFilterSection(filterContainer, "subtype", getString(R.string.subtype), allSubtypes)
        addProfessionalFilterSection(filterContainer, "type", getString(R.string.type), allTypes)
        addProfessionalFilterSection(filterContainer, "legality", getString(R.string.legality), allLegalities)
        addProfessionalFilterSection(filterContainer, "rarity", getString(R.string.rarity), allRarities)
        addProfessionalSpinnerSection(filterContainer, "artist", getString(R.string.artist), allArtists)
        addProfessionalFilterSection(filterContainer, "hasability", getString(R.string.has_ability), hasAbility)
        addProfessionalHPSection(filterContainer)
    }

    private fun addFilterHeader(container: LinearLayout) {
        val headerView = LayoutInflater.from(this).inflate(R.layout.filter_section_header, container, false)
        val titleView = headerView.findViewById<TextView>(R.id.tvSectionTitle)
        val arrowView = headerView.findViewById<ImageView>(R.id.ivArrow)
        
        titleView.text = getString(R.string.filtrosdebusqueda)
        titleView.textSize = 20f
        titleView.setTypeface(null, android.graphics.Typeface.BOLD)
        arrowView.visibility = View.GONE
        
        // Make header non-clickable
        headerView.isClickable = false
        headerView.isFocusable = false
        headerView.background = null
        
        val layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            topMargin = 100
            bottomMargin = 24
        }
        headerView.layoutParams = layoutParams
        
        container.addView(headerView)
    }

    private fun addProfessionalFilterSection(container: LinearLayout, key: String, title: String, options: List<String>) {
        val sectionContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = 16
            }
        }

        // Create header
        val headerView = LayoutInflater.from(this).inflate(R.layout.filter_section_header, sectionContainer, false)
        val titleView = headerView.findViewById<TextView>(R.id.tvSectionTitle)
        val arrowView = headerView.findViewById<ImageView>(R.id.ivArrow)
        
        titleView.text = title
        sectionContainer.addView(headerView)

        // Create content container
        val contentContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            visibility = View.GONE
            setPadding(4, 4, 4, 4)
        }

        val checkboxes = mutableListOf<CheckBox>()

        // Add options
        options.forEach { option ->
            val optionView = LayoutInflater.from(this).inflate(R.layout.filter_option_item, contentContainer, false)
            val checkbox = optionView.findViewById<CheckBox>(R.id.cbFilterOption)
            val label = optionView.findViewById<TextView>(R.id.tvFilterLabel)
            
            checkbox.tag = option
            label.text = option
            checkboxes.add(checkbox)
            
            // Make the entire item clickable
            optionView.setOnClickListener {
                checkbox.isChecked = !checkbox.isChecked
            }
            
            contentContainer.addView(optionView)
        }

        sectionContainer.addView(contentContainer)

        // Handle expand/collapse
        headerView.setOnClickListener {
            if (contentContainer.isGone) {
                contentContainer.visibility = View.VISIBLE
                arrowView.rotation = 90f
            } else {
                contentContainer.visibility = View.GONE
                arrowView.rotation = 0f
            }
        }

        container.addView(sectionContainer)
        filterCheckboxes[key] = checkboxes
    }

    private fun addProfessionalSpinnerSection(container: LinearLayout, key: String, title: String, options: List<String>) {
        val sectionContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = 16
            }
        }

        // Create header
        val headerView = LayoutInflater.from(this).inflate(R.layout.filter_section_header, sectionContainer, false)
        val titleView = headerView.findViewById<TextView>(R.id.tvSectionTitle)
        val arrowView = headerView.findViewById<ImageView>(R.id.ivArrow)
        
        titleView.text = title
        sectionContainer.addView(headerView)

        // Create spinner container
        val spinnerView = LayoutInflater.from(this).inflate(R.layout.filter_spinner_item, sectionContainer, false)
        val spinner = spinnerView.findViewById<Spinner>(R.id.spFilterSpinner)
        
        val spinnerOptions = mutableListOf(getString(R.string.all))
        spinnerOptions.addAll(options)
        
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, spinnerOptions)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinner.adapter = adapter
        
        spinnerView.visibility = View.GONE
        sectionContainer.addView(spinnerView)

        // Handle expand/collapse
        headerView.setOnClickListener {
            if (spinnerView.isGone) {
                spinnerView.visibility = View.VISIBLE
                arrowView.rotation = 90f
            } else {
                spinnerView.visibility = View.GONE
                arrowView.rotation = 0f
            }
        }

        container.addView(sectionContainer)
        
        if (key == "artist") {
            artistSpinner = spinner
        }
    }

    private fun addProfessionalHPSection(container: LinearLayout) {
        val sectionContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = 16
            }
        }

        // Create header
        val headerView = LayoutInflater.from(this).inflate(R.layout.filter_section_header, sectionContainer, false)
        val titleView = headerView.findViewById<TextView>(R.id.tvSectionTitle)
        val arrowView = headerView.findViewById<ImageView>(R.id.ivArrow)
        
        titleView.text = getString(R.string.rangohp)
        sectionContainer.addView(headerView)

        // Create HP range container
        val hpView = LayoutInflater.from(this).inflate(R.layout.filter_hp_range, sectionContainer, false)
        val minHP = hpView.findViewById<EditText>(R.id.etMinHP)
        val maxHP = hpView.findViewById<EditText>(R.id.etMaxHP)
        
        hpView.visibility = View.GONE
        sectionContainer.addView(hpView)

        // Handle expand/collapse
        headerView.setOnClickListener {
            if (hpView.isGone) {
                hpView.visibility = View.VISIBLE
                arrowView.rotation = 90f
            } else {
                hpView.visibility = View.GONE
                arrowView.rotation = 0f
            }
        }

        container.addView(sectionContainer)
        minHpEditText = minHP
        maxHpEditText = maxHP
    }

    private fun setupSortSpinner() {
        val sortingSpinner = findViewById<Spinner>(R.id.spOrdenar)
        val options = listOf(
            getString(R.string.relevant),
            getString(R.string.m_s_recientes_primero),
            getString(R.string.m_s_antiguas_primero),
            getString(R.string.nombre_a_z),
            getString(R.string.nombre_z_a)
        )
        
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, options)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        sortingSpinner.adapter = adapter
        sortSpinner = sortingSpinner
    }

    private fun performSearch(filter: CardFilter, db: DBHelper, colid: Int, rview: RecyclerView) {
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

    private fun collectFilters(): CardFilter {
        val selectedSupertypes = filterCheckboxes["supertype"]?.filter { it.isChecked }?.map { it.tag.toString() }.orEmpty()
        val selectedSubtypes = filterCheckboxes["subtype"]?.filter { it.isChecked }?.map { it.tag.toString() }.orEmpty()
        val selectedTypes = filterCheckboxes["type"]?.filter { it.isChecked }?.map { it.tag.toString() }.orEmpty()
        val selectedLegalities = filterCheckboxes["legality"]?.filter { it.isChecked }?.map { it.tag.toString() }.orEmpty()
        val selectedRarities = filterCheckboxes["rarity"]?.filter { it.isChecked }?.map { it.tag.toString() }.orEmpty()

        val hasAbilityFilter = filterCheckboxes["hasability"]?.firstOrNull { it.isChecked }?.tag?.let {
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
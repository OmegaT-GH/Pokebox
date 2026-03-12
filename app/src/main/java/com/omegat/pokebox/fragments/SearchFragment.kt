package com.omegat.pokebox.fragments

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.CheckBox
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.TextView
import androidx.core.view.GravityCompat
import androidx.core.view.isGone
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.omegat.pokebox.R
import com.omegat.pokebox.activities.ViewCard
import com.omegat.pokebox.adapters.ListCardsAdapter
import com.omegat.pokebox.bd.DBHelper
import com.omegat.pokebox.data.CardFilter
import com.omegat.pokebox.data.CardRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SearchFragment : Fragment() {

    private val filterCheckboxes = mutableMapOf<String, List<CheckBox>>()
    private var artistSpinner: Spinner? = null
    private var minHpEditText: EditText? = null
    private var maxHpEditText: EditText? = null
    private var sortSpinner: Spinner? = null
    private lateinit var rviewadap: ListCardsAdapter
    private var rootView: View? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        rootView = inflater.inflate(R.layout.fragment_search, container, false)
        return rootView
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        val btsearch = view.findViewById<soup.neumorphism.NeumorphCardView>(R.id.btSearch)
        val btfilters = view.findViewById<soup.neumorphism.NeumorphCardView>(R.id.btOpenFilters)
        val drawer = view.findViewById<DrawerLayout>(R.id.main)
        val rview = view.findViewById<RecyclerView>(R.id.rviewfilteredsearch)
        val lmanager = LinearLayoutManager(requireContext())
        rview.layoutManager = lmanager

        setupFilterMenu()
        setupSortSpinner()

        btsearch.setOnClickListener {
            val filter = collectFilters()
            performSearch(filter, rview)
        }

        btfilters.setOnClickListener {
            drawer.openDrawer(GravityCompat.START)
        }
    }

    private fun setupFilterMenu() {
        val filterContainer = rootView?.findViewById<LinearLayout>(R.id.contFiltro) ?: return
        filterContainer.removeAllViews()

        addFilterHeader(filterContainer)

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
        val headerView = LayoutInflater.from(requireContext()).inflate(R.layout.filter_section_header, container, false)
        val titleView = headerView.findViewById<TextView>(R.id.tvSectionTitle)
        val arrowView = headerView.findViewById<ImageView>(R.id.ivArrow)
        
        titleView.text = getString(R.string.filtrosdebusqueda)
        titleView.textSize = 20f
        titleView.setTypeface(null, android.graphics.Typeface.BOLD)
        arrowView.visibility = View.GONE
        
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
        val sectionContainer = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = 16
            }
        }

        val headerView = LayoutInflater.from(requireContext()).inflate(R.layout.filter_section_header, sectionContainer, false)
        val titleView = headerView.findViewById<TextView>(R.id.tvSectionTitle)
        val arrowView = headerView.findViewById<ImageView>(R.id.ivArrow)
        
        titleView.text = title
        sectionContainer.addView(headerView)

        val contentContainer = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            visibility = View.GONE
            setPadding(4, 4, 4, 4)
        }

        val checkboxes = mutableListOf<CheckBox>()

        options.forEach { option ->
            val optionView = LayoutInflater.from(requireContext()).inflate(R.layout.filter_option_item, contentContainer, false)
            val checkbox = optionView.findViewById<CheckBox>(R.id.cbFilterOption)
            val label = optionView.findViewById<TextView>(R.id.tvFilterLabel)
            
            checkbox.tag = option
            label.text = option
            checkboxes.add(checkbox)
            
            optionView.setOnClickListener {
                checkbox.isChecked = !checkbox.isChecked
            }
            
            contentContainer.addView(optionView)
        }

        sectionContainer.addView(contentContainer)

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
        val sectionContainer = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = 16
            }
        }

        val headerView = LayoutInflater.from(requireContext()).inflate(R.layout.filter_section_header, sectionContainer, false)
        val titleView = headerView.findViewById<TextView>(R.id.tvSectionTitle)
        val arrowView = headerView.findViewById<ImageView>(R.id.ivArrow)
        
        titleView.text = title
        sectionContainer.addView(headerView)

        val spinnerView = LayoutInflater.from(requireContext()).inflate(R.layout.filter_spinner_item, sectionContainer, false)
        val spinner = spinnerView.findViewById<Spinner>(R.id.spFilterSpinner)
        
        val spinnerOptions = mutableListOf(getString(R.string.all))
        spinnerOptions.addAll(options)
        
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, spinnerOptions)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinner.adapter = adapter
        
        spinnerView.visibility = View.GONE
        sectionContainer.addView(spinnerView)

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
        val sectionContainer = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = 16
            }
        }

        val headerView = LayoutInflater.from(requireContext()).inflate(R.layout.filter_section_header, sectionContainer, false)
        val titleView = headerView.findViewById<TextView>(R.id.tvSectionTitle)
        val arrowView = headerView.findViewById<ImageView>(R.id.ivArrow)
        
        titleView.text = getString(R.string.rangohp)
        sectionContainer.addView(headerView)

        val hpView = LayoutInflater.from(requireContext()).inflate(R.layout.filter_hp_range, sectionContainer, false)
        val minHP = hpView.findViewById<EditText>(R.id.etMinHP)
        val maxHP = hpView.findViewById<EditText>(R.id.etMaxHP)
        
        hpView.visibility = View.GONE
        sectionContainer.addView(hpView)

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
        val sortingSpinner = rootView?.findViewById<Spinner>(R.id.spOrdenar) ?: return
        val options = listOf(
            getString(R.string.relevant),
            getString(R.string.m_s_recientes_primero),
            getString(R.string.m_s_antiguas_primero),
            getString(R.string.nombre_a_z),
            getString(R.string.nombre_z_a)
        )
        
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, options)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        sortingSpinner.adapter = adapter
        sortSpinner = sortingSpinner
    }

    private fun performSearch(filter: CardFilter, rview: RecyclerView) {
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
            
            val cardamounts = MutableList(sortedcards.size) { 0 }
            
            withContext(Dispatchers.Main) {
                if (::rviewadap.isInitialized) {
                    rviewadap.updateData(sortedcards, cardamounts)
                } else {
                    rviewadap = ListCardsAdapter(requireContext(), sortedcards, cardamounts) { selectedCard ->
                        val i = Intent(requireContext(), ViewCard::class.java)
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

        val name = rootView?.findViewById<EditText>(R.id.etSearch)?.text?.toString()?.takeIf { it.isNotBlank() }
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

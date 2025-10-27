package com.example.pokebox.activities

import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.drawerlayout.widget.DrawerLayout
import com.example.pokebox.DEBUG_ListFilters
import com.example.pokebox.R
import com.example.pokebox.data.CardRepository
import androidx.core.view.isGone

class AdvancedSearch : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_advanced_search)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val btdebug = findViewById<Button>(R.id.btSearch)
        val btfilters = findViewById<Button>(R.id.btOpenFilters)
        val drawer = findViewById<DrawerLayout>(R.id.main)


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
        val hasAbility = listOf("Yes", "No")
        /*
        val minHP: Int? = null,\n\n
        val maxHP: Int? = null"
        */

        addFilterSection("Supertype", allSupertypes)
        addFilterSection("Subtype", allSubtypes)
        addFilterSection("Type", allTypes)
        addFilterSection("Legality", allLegalities)
        addFilterSection("Rarity", allRarities)
        addSpinnerSection("Artist", allArtists)
        addFilterSection("Has Ability", hasAbility)
        addHPSection()

        addSortSection()

        btdebug.setOnClickListener {
            val i = Intent(this, DEBUG_ListFilters::class.java)
            this.startActivity(i)
        }

        btfilters.setOnClickListener {
            drawer.openDrawer(GravityCompat.START)
        }

    }

    fun addFilterSection(title: String, options: List<String>) {
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

        //header.isBaselineAligned = true

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
    }
    fun addSpinnerSection(title: String, options: List<String>) {
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

        val spinner = Spinner(this).apply {
            val spinnerOptions = mutableListOf("Todos")
            spinnerOptions.addAll(options)

            val adapter = object : ArrayAdapter<String>(context, android.R.layout.simple_spinner_item, spinnerOptions) {
                override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                    val v = super.getView(position, convertView, parent) as TextView
                    v.setTextAppearance(context, R.style.text) // aquí aplicas tu estilo
                    return v
                }

                override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
                    val v = super.getDropDownView(position, convertView, parent) as TextView
                    v.setTextAppearance(context, R.style.text) // mismo estilo para dropdown
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

        spinner.visibility = View.GONE
        sectionLayout.addView(spinner)

        header.setOnClickListener {
            spinner.visibility = if (spinner.isGone) {
                arrow.rotation = 90f
                View.VISIBLE
            } else {
                arrow.rotation = 0f
                View.GONE
            }
        }

        findViewById<LinearLayout>(R.id.contFiltro).addView(sectionLayout)
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
            hint = "Mín."
            inputType = InputType.TYPE_CLASS_NUMBER
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                marginEnd = 8
            }
            setPadding(16, 20, 16, 20)
        }

        val maxHP = EditText(this).apply {
            hint = "Máx."
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
    }
    private fun addSortSection() {
        val sortingSpinner = findViewById<Spinner>(R.id.spOrdenar)

        val options = listOf("Nombre (A-Z)", "Nombre (Z-A)", "Más recientes primero", "Más antiguas primero")

        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, options)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        sortingSpinner.adapter = adapter
    }
}
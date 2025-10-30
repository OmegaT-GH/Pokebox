package com.example.pokebox.activities

import android.content.Intent
import android.database.Cursor
import android.os.Bundle
import android.text.InputType
import android.util.Log
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.Spinner
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.example.pokebox.R
import com.example.pokebox.bd.DBHelper
import com.example.pokebox.bd.InitializeBD
import com.example.pokebox.data.CardRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

class MainMenu : AppCompatActivity() {

    lateinit var ncolname: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main_menu)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        ncolname = ""

        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        val btsetsearch = findViewById<Button>(R.id.btListSets)
        val btadvsearch = findViewById<Button>(R.id.btAdvSearch)
        val btsetperc = findViewById<Button>(R.id.btListSetsPerc)
        val btcompmazo = findViewById<Button>(R.id.btCompMazo)
        val btaddcol = findViewById<Button>(R.id.btCrearColeccion)

        val rlay = findViewById<ConstraintLayout>(R.id.main)

        val overlay = View(this).apply {
            setBackgroundColor(getColor(R.color.LowOpacityBlack))
            visibility = View.GONE
            isClickable = true
            isFocusable = true
            id = View.generateViewId()
        }
        val overlayParams = ConstraintLayout.LayoutParams(
            ConstraintLayout.LayoutParams.MATCH_PARENT,
            ConstraintLayout.LayoutParams.MATCH_PARENT
        )
        overlay.layoutParams = overlayParams
        rlay.addView(overlay)


        val pbar = ProgressBar(this).apply {
            isIndeterminate = true
            visibility = View.GONE
            id = View.generateViewId()
            layoutParams = ConstraintLayout.LayoutParams(150,150)
        }
        rlay.addView(pbar)

        val set = ConstraintSet()
        set.clone(rlay)
        set.connect(pbar.id, ConstraintSet.TOP, rlay.id, ConstraintSet.TOP)
        set.connect(pbar.id, ConstraintSet.BOTTOM, rlay.id, ConstraintSet.BOTTOM)
        set.connect(pbar.id, ConstraintSet.START, rlay.id, ConstraintSet.START)
        set.connect(pbar.id, ConstraintSet.END, rlay.id, ConstraintSet.END)
        set.applyTo(rlay)


        // NO SEPARAR BLOQUE -- Carga/Actualiza BD y Spinner de colecciones -----------------
        val db = DBHelper(this)
        val initdb = InitializeBD()
        val spcol = findViewById<Spinner>(R.id.spColecciones)
        lifecycleScope.launch(Dispatchers.IO) {
            withContext(Dispatchers.Main) {
                overlay.visibility = View.VISIBLE
                pbar.visibility = View.VISIBLE
            }
            DBHelper.dbMutex.withLock {
                initdb.setsycartas(this@MainMenu, db)
            }
            withContext(Dispatchers.Main) {
                reloadspinner(db, btsetperc, btcompmazo, spcol)
            }
            val colecciones = withContext(Dispatchers.Main) {
                val adapter = spcol.adapter
                List(adapter.count) { i -> adapter.getItem(i).toString() }
            }
            for (nombre in colecciones) {
                DBHelper.dbMutex.withLock {
                    initdb.actualizarcoleccion(this@MainMenu, db, nombre)
                    Log.d("BD", "Actualizando la colección: $nombre")
                }
            }
            withContext(Dispatchers.Main) {
                overlay.visibility = View.GONE
                pbar.visibility = View.GONE
            }
        }
        // ----------------------------------------------------------------------------------


        btsetsearch.setOnClickListener {
            val i = Intent(this, ListSetsSearch::class.java)
            this.startActivity(i)
        }

        btadvsearch.setOnClickListener {
            if (CardRepository.getCards().isEmpty()) {
                val i = Intent(this, LoadingActivity::class.java)
                this.startActivity(i)
            } else {
                val i = Intent(this, AdvancedSearch::class.java)
                this.startActivity(i)
            }
        }

        btaddcol.setOnClickListener {
            val adbuilder = AlertDialog.Builder(this)
            adbuilder.setTitle("Nombre de la colección:")

            val etName = EditText(this)
            etName.inputType = InputType.TYPE_CLASS_TEXT

            adbuilder.setView(etName)
            adbuilder.setPositiveButton(
                "Crear"
            ) { dialog, which ->
                if (etName.text.isEmpty()) {
                    Toast.makeText(
                        this,
                        "El nombre de la colección no puede estar en blanco.",
                        Toast.LENGTH_SHORT
                    ).show()
                } else {
                    ncolname = etName.text.toString()

                    val id = db.getCollectionFromName(ncolname)
                    if (id == null) {
                        Toast.makeText(
                            this,
                            "Coleccion '$ncolname' creada correctamente",
                            Toast.LENGTH_SHORT
                        ).show()
                        initdb.crearcoleccion(this, db, ncolname)
                        reloadspinner(db, btsetperc, btcompmazo, spcol)
                    } else {
                        Toast.makeText(
                            this,
                            "Una colección con ese nombre ya existe.",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }


            }
            adbuilder.setNegativeButton(
                "Cancelar"
            ) { dialog, which ->
                etName.text.clear()
            }
            adbuilder.show()
        }


    }

    fun reloadspinner(db: DBHelper, btsetperc: Button, btcompmazo: Button, spcol: Spinner) {
        val collist: ArrayList<String> = ArrayList()
        val rdb = db.readableDatabase
        val cur: Cursor = rdb.rawQuery("SELECT * FROM Coleccion", null)
        cur.use {
            while (it.moveToNext()) {
                collist.add(it.getString(it.getColumnIndexOrThrow("colName")))
            }
        }
        if (collist.isEmpty()) {

            val adapter = ArrayAdapter<String?>(
                this,
                android.R.layout.simple_spinner_item
            )
            adapter.add("No existen colecciones.")
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            spcol.setAdapter(adapter)
            spcol.isEnabled = false
            btsetperc.isEnabled = false
            btsetperc.setBackgroundColor(getColor(R.color.DisabledButton))
            btcompmazo.isEnabled = false
            btcompmazo.setBackgroundColor(getColor(R.color.DisabledButton))

        } else {

            val adapter = ArrayAdapter<String?>(
                this,
                android.R.layout.simple_spinner_item
            )
            adapter.addAll(collist)
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            spcol.setAdapter(adapter)
            spcol.isEnabled = true
            btsetperc.isEnabled = true
            btsetperc.setBackgroundColor(getColor(R.color.Primary))
            btcompmazo.isEnabled = true
            btcompmazo.setBackgroundColor(getColor(R.color.Primary))

        }
    }
}
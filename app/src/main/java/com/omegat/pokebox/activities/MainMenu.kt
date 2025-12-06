package com.omegat.pokebox.activities

import android.annotation.SuppressLint
import android.content.Intent
import android.database.Cursor
import android.os.Bundle
import android.text.InputType
import android.util.Log
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.omegat.pokebox.R
import com.omegat.pokebox.bd.DBHelper
import com.omegat.pokebox.bd.InitializeBD
import com.omegat.pokebox.data.CardRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext


class MainMenu : AppCompatActivity() {

    lateinit var ncolname: String

    @SuppressLint("SetTextI18n")
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
        val btsetsearch = findViewById<LinearLayout>(R.id.btListSets)
        val btadvsearch = findViewById<LinearLayout>(R.id.btAdvSearch)
        val btsetperc = findViewById<LinearLayout>(R.id.btListSetsPerc)
        val btcompmazo = findViewById<LinearLayout>(R.id.btCompMazo)
        val btaddcol = findViewById<Button>(R.id.btCrearColeccion)
        val btdelcol = findViewById<Button>(R.id.btEliminarColeccion)

        val tvversion = findViewById<TextView>(R.id.tvVersion)
        val version = this.packageManager.getPackageInfo(this.packageName,0).versionName
        tvversion.text = "v$version"


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
                reloadspinner(db, btsetperc, btcompmazo, spcol, btdelcol, null)
            }
            val colecciones = withContext(Dispatchers.Main) {
                val adapter = spcol.adapter
                List(adapter.count) { i -> adapter.getItem(i).toString() }
            }
            for (nombre in colecciones) {
                DBHelper.dbMutex.withLock {
                    initdb.actualizarcoleccion(db, nombre)
                    Log.d("BD", getString(R.string.actualizando_la_colecci_n) + " $nombre")
                }
            }
            withContext(Dispatchers.Main) {
                overlay.visibility = View.GONE
                pbar.visibility = View.GONE
            }
        }
        // ----------------------------------------------------------------------------------

        btsetsearch.setOnClickListener {
            val colid = db.getCollectionFromName(spcol.selectedItem.toString())
            val i = Intent(this, ListSets::class.java)
            i.putExtra("mode", "list")
            i.putExtra("col", colid)
            this.startActivity(i)
        }

        btadvsearch.setOnClickListener {
            val colid = db.getCollectionFromName(spcol.selectedItem.toString())
            if (CardRepository.getCards().isEmpty()) {
                val i = Intent(this, LoadingActivity::class.java)
                i.putExtra("col", colid)
                i.putExtra("type", "search")
                this.startActivity(i)
            } else {
                val i = Intent(this, AdvancedSearch::class.java)
                i.putExtra("col", colid)
                this.startActivity(i)
            }
        }

        btaddcol.setOnClickListener {
            val adbuilder = AlertDialog.Builder(this)
            adbuilder.setTitle(getString(R.string.nombre_de_la_colecci_n))

            val etName = EditText(this)
            etName.inputType = InputType.TYPE_CLASS_TEXT

            val container = LinearLayout(this)
            container.orientation = LinearLayout.VERTICAL
            val scale = resources.displayMetrics.density
            val margLR = (scale * 16 / .5f).toInt()
            val margUD = (scale * 4 / .5f).toInt()
            val params = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            params.setMargins(margLR, margUD, margLR, margUD)
            etName.layoutParams = params
            container.addView(etName)

            adbuilder.setView(container)
            adbuilder.setPositiveButton(
                getString(R.string.crear)
            ) { dialog, which ->
                if (etName.text.isEmpty()) {
                    Toast.makeText(
                        this,
                        getString(R.string.el_nombre_de_la_colecci_n_no_puede_estar_en_blanco),
                        Toast.LENGTH_SHORT
                    ).show()
                } else {
                    ncolname = etName.text.toString()

                    val id = db.getCollectionFromName(ncolname)
                    if (id == null) {
                        Log.d("created", getString(R.string.coleccion) +  " '$ncolname' " + getString(R.string.creada_correctamente))
                        initdb.crearcoleccion(this, db, ncolname)
                        val newid = db.getCollectionFromName(ncolname)
                        reloadspinner(db, btsetperc, btcompmazo, spcol, btdelcol, newid)
                    } else {
                        Toast.makeText(
                            this,
                            getString(R.string.una_colecci_n_con_ese_nombre_ya_existe),
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }


            }
            adbuilder.setNegativeButton(
                getString(R.string.cancelar)
            ) { dialog, which ->
                etName.text.clear()
            }
            adbuilder.show()
        }

        btsetperc.setOnClickListener {

            val selectedcollection = spcol.selectedItem.toString()
            val colid = db.getCollectionFromName(selectedcollection)
            val i = Intent(this, ListSets::class.java)
            i.putExtra("mode", "percentage")
            i.putExtra("col", colid)
            this.startActivity(i)
        }

        btcompmazo.setOnClickListener {
            val selectedcollection = spcol.selectedItem.toString()
            val colid = db.getCollectionFromName(selectedcollection)

            if (CardRepository.getCards().isEmpty()) {
                val i = Intent(this, LoadingActivity::class.java)
                i.putExtra("col", colid)
                i.putExtra("type", "deck")
                this.startActivity(i)
            } else {
                val i = Intent(this, CheckDeck::class.java)
                i.putExtra("col", colid)
                this.startActivity(i)
            }
        }

        btdelcol.setOnClickListener {
            val colid = db.getCollectionFromName(spcol.selectedItem.toString())

            AlertDialog.Builder(this)
                .setTitle(getString(R.string.delete_collection))
                .setMessage(getString(R.string.are_you_sure_you_want_to_delete, spcol.selectedItem))
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setPositiveButton(android.R.string.ok
                ) { dialog, whichButton ->
                    db.removeCollection(colid)
                    reloadspinner(db, btsetperc, btcompmazo, spcol, btdelcol, null)
                }
                .setNegativeButton(android.R.string.cancel, null).show()

        }


    }

    fun reloadspinner(db: DBHelper, btsetperc: LinearLayout, btcompmazo: LinearLayout, spcol: Spinner, btdelcol: Button, id: Int?) {
        val collist: ArrayList<String> = ArrayList()
        val rdb = db.readableDatabase
        val cur: Cursor = rdb.rawQuery("SELECT * FROM Coleccion", null)
        cur.use {
            while (it.moveToNext()) {
                collist.add(it.getString(it.getColumnIndexOrThrow("colName")))
            }
        }

        val adapter = ArrayAdapter<String?>(
            this,
            android.R.layout.simple_spinner_item
        )

        if (collist.isEmpty()) {

            adapter.add(getString(R.string.no_existen_colecciones))
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            spcol.adapter = adapter
            spcol.isEnabled = false
            btsetperc.isClickable = false
            btsetperc.background = ContextCompat.getDrawable(this, R.drawable.menubuttondisabled)
            btcompmazo.isClickable = false
            btcompmazo.background = ContextCompat.getDrawable(this, R.drawable.menubuttondisabled)
            btdelcol.isEnabled = false
            btdelcol.setBackgroundColor(getColor(R.color.DisabledButton))

        } else {

            adapter.addAll(collist)
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            spcol.adapter = adapter
            spcol.isEnabled = true
            btsetperc.isClickable = true
            btsetperc.background = ContextCompat.getDrawable(this, R.drawable.menubutton)
            btcompmazo.isClickable = true
            btcompmazo.background = ContextCompat.getDrawable(this, R.drawable.menubutton)
            btdelcol.isEnabled = true
            btdelcol.setBackgroundColor(getColor(R.color.Primary))

        }

        spcol.setSelection(adapter.getPosition(db.getCollectionFromID(id)))
    }
}
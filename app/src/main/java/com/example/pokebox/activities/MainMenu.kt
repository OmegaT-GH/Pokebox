package com.example.pokebox.activities

import android.content.Intent
import android.database.Cursor
import android.os.Bundle
import android.text.InputType
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.pokebox.R
import com.example.pokebox.bd.DBHelper
import com.example.pokebox.bd.InitializeBD
import com.example.pokebox.data.CardRepository

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


        // NO SEPARAR BLOQUE -- Carga/Actualiza BD y Spinner de colecciones
        val db = DBHelper(this)
        val initdb = InitializeBD()
        initdb.setsycartas(this, db)
        reloadspinner(db, btsetperc, btcompmazo)
        // ----------------------------------------------------------------


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
                        initdb.crearcoleccion(this, db,ncolname)
                        reloadspinner(db, btsetperc, btcompmazo)
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

    fun reloadspinner(db: DBHelper, btsetperc: Button, btcompmazo: Button) {
        val spcol = findViewById<Spinner>(R.id.spColecciones)
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
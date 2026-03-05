package com.omegat.pokebox.fragments

import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.omegat.pokebox.R
import com.omegat.pokebox.activities.AdvancedSearch
import com.omegat.pokebox.activities.CheckDeck
import com.omegat.pokebox.activities.ListSets
import com.omegat.pokebox.bd.DBHelper
import com.omegat.pokebox.bd.InitializeBD
import com.omegat.pokebox.data.CardRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

class HomeFragment : Fragment() {

    private lateinit var db: DBHelper
    private var ncolname: String = ""

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_home, container, false)
        
        db = DBHelper(requireContext())
        setupViews(view)
        
        return view
    }

    private fun setupViews(view: View) {
        val btsetsearch = view.findViewById<soup.neumorphism.NeumorphCardView>(R.id.btListSets)
        val btadvsearch = view.findViewById<soup.neumorphism.NeumorphCardView>(R.id.btAdvSearch)
        val btsetperc = view.findViewById<soup.neumorphism.NeumorphCardView>(R.id.btListSetsPerc)
        val btcompmazo = view.findViewById<soup.neumorphism.NeumorphCardView>(R.id.btCompMazo)
        val btaddcol = view.findViewById<soup.neumorphism.NeumorphCardView>(R.id.btCrearColeccion)
        val btdelcol = view.findViewById<soup.neumorphism.NeumorphCardView>(R.id.btEliminarColeccion)
        val spcol = view.findViewById<Spinner>(R.id.spColecciones)
        val tvversion = view.findViewById<TextView>(R.id.tvVersion)
        
        val version = requireContext().packageManager.getPackageInfo(requireContext().packageName, 0).versionName
        tvversion.text = "v$version"

        val rlay = view.findViewById<ConstraintLayout>(R.id.main)
        val overlay = View(requireContext()).apply {
            setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.LowOpacityBlack))
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

        val initdb = InitializeBD()
        lifecycleScope.launch(Dispatchers.IO) {
            withContext(Dispatchers.Main) {
                overlay.visibility = View.VISIBLE
            }
            DBHelper.dbMutex.withLock {
                initdb.setsycartas(requireContext(), db)
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
                }
            }
            withContext(Dispatchers.Main) {
                overlay.visibility = View.GONE
            }
        }

        btsetsearch.setOnClickListener {
            val colid = db.getCollectionFromName(spcol.selectedItem?.toString() ?: "")
            val i = Intent(requireContext(), ListSets::class.java)
            i.putExtra("mode", "list")
            i.putExtra("col", colid)
            startActivity(i)
        }

        btadvsearch.setOnClickListener {
            val colid = db.getCollectionFromName(spcol.selectedItem?.toString() ?: "")
            val i = Intent(requireContext(), AdvancedSearch::class.java)
            i.putExtra("col", colid)
            startActivity(i)
        }

        btsetperc.setOnClickListener {
            val selectedcollection = spcol.selectedItem?.toString() ?: ""
            val colid = db.getCollectionFromName(selectedcollection)
            val i = Intent(requireContext(), ListSets::class.java)
            i.putExtra("mode", "percentage")
            i.putExtra("col", colid)
            startActivity(i)
        }

        btcompmazo.setOnClickListener {
            val selectedcollection = spcol.selectedItem?.toString() ?: ""
            val colid = db.getCollectionFromName(selectedcollection)
            val i = Intent(requireContext(), CheckDeck::class.java)
            i.putExtra("col", colid)
            startActivity(i)
        }

        btaddcol.setOnClickListener {
            val adbuilder = AlertDialog.Builder(requireContext())
            adbuilder.setTitle(getString(R.string.nombre_de_la_colecci_n))

            val etName = EditText(requireContext())
            etName.inputType = InputType.TYPE_CLASS_TEXT

            val container = LinearLayout(requireContext())
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
            adbuilder.setPositiveButton(getString(R.string.crear)) { dialog, which ->
                if (etName.text.isEmpty()) {
                    Toast.makeText(
                        requireContext(),
                        getString(R.string.el_nombre_de_la_colecci_n_no_puede_estar_en_blanco),
                        Toast.LENGTH_SHORT
                    ).show()
                } else {
                    ncolname = etName.text.toString()
                    val id = db.getCollectionFromName(ncolname)
                    if (id == null) {
                        initdb.crearcoleccion(requireContext(), db, ncolname)
                        val newid = db.getCollectionFromName(ncolname)
                        reloadspinner(db, btsetperc, btcompmazo, spcol, btdelcol, newid)
                    } else {
                        Toast.makeText(
                            requireContext(),
                            getString(R.string.una_colecci_n_con_ese_nombre_ya_existe),
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
            adbuilder.setNegativeButton(getString(R.string.cancelar)) { dialog, which ->
                etName.text.clear()
            }
            adbuilder.show()
        }

        btdelcol.setOnClickListener {
            val colid = db.getCollectionFromName(spcol.selectedItem?.toString() ?: "")
            AlertDialog.Builder(requireContext())
                .setTitle(getString(R.string.delete_collection))
                .setMessage(getString(R.string.are_you_sure_you_want_to_delete, spcol.selectedItem?.toString()))
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setPositiveButton(android.R.string.ok) { dialog, whichButton ->
                    db.removeCollection(colid)
                    reloadspinner(db, btsetperc, btcompmazo, spcol, btdelcol, null)
                }
                .setNegativeButton(android.R.string.cancel, null).show()
        }
    }

    private fun reloadspinner(
        db: DBHelper,
        btsetperc: soup.neumorphism.NeumorphCardView,
        btcompmazo: soup.neumorphism.NeumorphCardView,
        spcol: Spinner,
        btdelcol: soup.neumorphism.NeumorphCardView,
        id: Int?
    ) {
        val collist: ArrayList<String> = ArrayList()
        val rdb = db.readableDatabase
        val cur = rdb.rawQuery("SELECT * FROM Coleccion", null)
        cur.use {
            while (it.moveToNext()) {
                collist.add(it.getString(it.getColumnIndexOrThrow("colName")))
            }
        }

        val adapter = ArrayAdapter<String?>(
            requireContext(),
            R.layout.spinner_item
        )

        if (collist.isEmpty()) {
            adapter.add(getString(R.string.no_existen_colecciones))
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            spcol.adapter = adapter
            spcol.isEnabled = false
            btsetperc.isClickable = false
            btsetperc.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.DisabledButton))
            btcompmazo.isClickable = false
            btcompmazo.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.DisabledButton))
            btdelcol.isEnabled = false
            btdelcol.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.DisabledButton))
        } else {
            adapter.addAll(collist)
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            spcol.adapter = adapter
            spcol.isEnabled = true
            btsetperc.isClickable = true
            btsetperc.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.Background))
            btcompmazo.isClickable = true
            btcompmazo.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.Background))
            btdelcol.isEnabled = true
            btdelcol.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.Background))
        }

        spcol.setSelection(adapter.getPosition(db.getCollectionFromID(id)))
    }
}

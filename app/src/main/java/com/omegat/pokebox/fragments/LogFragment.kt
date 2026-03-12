package com.omegat.pokebox.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.google.gson.stream.JsonReader
import com.omegat.pokebox.R
import com.omegat.pokebox.adapters.LogAdapter
import com.omegat.pokebox.bd.DBHelper
import com.omegat.pokebox.data.CardRepository
import com.omegat.pokebox.data.LogMovimiento
import com.omegat.pokebox.data.PokemonSet
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class LogFragment : Fragment() {

    private lateinit var db: DBHelper
    private lateinit var logAdapter: LogAdapter
    private lateinit var sets: List<PokemonSet>
    private var selectedCollectionId: Int? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_log, container, false)
        
        db = DBHelper(requireContext())
        
        // Cargar sets
        val sinputStream = requireContext().assets.open("json/sets/en.json")
        val sreader = JsonReader(sinputStream.reader())
        val stype = object : TypeToken<List<PokemonSet>>() {}.type
        sets = Gson().fromJson(sreader, stype)
        sreader.close()
        
        setupViews(view)
        
        return view
    }

    private fun setupViews(view: View) {
        val spCollections = view.findViewById<Spinner>(R.id.spLogCollections)
        val rvLog = view.findViewById<RecyclerView>(R.id.rvLogMovimientos)
        val tvEmpty = view.findViewById<TextView>(R.id.tvEmptyLog)
        
        logAdapter = LogAdapter(requireContext(), emptyList())
        rvLog.adapter = logAdapter
        
        loadCollections(spCollections, tvEmpty, rvLog)
        
        spCollections.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val colName = spCollections.selectedItem.toString()
                if (colName != getString(R.string.no_existen_colecciones)) {
                    selectedCollectionId = db.getCollectionFromName(colName)
                    loadLogs(selectedCollectionId, tvEmpty, rvLog)
                }
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun loadCollections(spinner: Spinner, tvEmpty: TextView, rvLog: RecyclerView) {
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
                val adapter = ArrayAdapter(requireContext(), R.layout.spinner_item, collections)
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)

                if (collections.isEmpty()) {
                    adapter.add(getString(R.string.no_existen_colecciones))
                    spinner.isEnabled = false
                    tvEmpty.visibility = View.VISIBLE
                    tvEmpty.text = getString(R.string.no_existen_colecciones)
                    rvLog.visibility = View.GONE
                } else {
                    spinner.isEnabled = true
                    spinner.adapter = adapter
                    
                    // Seleccionar la primera colección por defecto
                    if (collections.isNotEmpty()) {
                        selectedCollectionId = db.getCollectionFromName(collections[0])
                        loadLogs(selectedCollectionId, tvEmpty, rvLog)
                    }
                }
            }
        }
    }

    private fun loadLogs(colId: Int?, tvEmpty: TextView, rvLog: RecyclerView) {
        if (colId == null) return
        
        lifecycleScope.launch(Dispatchers.IO) {
            val logs = mutableListOf<LogMovimiento>()
            val cards = CardRepository.getCards()
            
            val cursor = db.getLogMovimientos(colId)
            cursor.use {
                while (it.moveToNext()) {
                    val logId = it.getInt(it.getColumnIndexOrThrow(DBHelper.LOG_ID))
                    val colIdDb = it.getInt(it.getColumnIndexOrThrow(DBHelper.COL_ID))
                    val cardId = it.getString(it.getColumnIndexOrThrow(DBHelper.CARD_ID))
                    val cantAnterior = it.getInt(it.getColumnIndexOrThrow(DBHelper.LOG_CANTIDAD_ANTERIOR))
                    val cantNueva = it.getInt(it.getColumnIndexOrThrow(DBHelper.LOG_CANTIDAD_NUEVA))
                    val timestamp = it.getLong(it.getColumnIndexOrThrow(DBHelper.LOG_TIMESTAMP))
                    
                    // Buscar información de la carta
                    val card = cards.find { c -> c.id == cardId }
                    val setId = cardId.substringBefore("-")
                    val set = sets.find { s -> s.id == setId }
                    
                    logs.add(
                        LogMovimiento(
                            logId = logId,
                            colId = colIdDb,
                            cardId = cardId,
                            cantidadAnterior = cantAnterior,
                            cantidadNueva = cantNueva,
                            timestamp = timestamp,
                            cardName = card?.name,
                            setName = set?.name,
                            cardNumber = card?.number,
                            cardImageUrl = card?.images?.small
                        )
                    )
                }
            }
            
            withContext(Dispatchers.Main) {
                if (logs.isEmpty()) {
                    tvEmpty.visibility = View.VISIBLE
                    tvEmpty.text = getString(R.string.no_log_movements)
                    rvLog.visibility = View.GONE
                } else {
                    tvEmpty.visibility = View.GONE
                    rvLog.visibility = View.VISIBLE
                    logAdapter.updateData(logs)
                }
            }
        }
    }
}

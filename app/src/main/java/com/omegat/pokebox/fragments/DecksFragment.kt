package com.omegat.pokebox.fragments

import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.google.gson.stream.JsonReader
import com.omegat.pokebox.R
import com.omegat.pokebox.activities.ViewCard
import com.omegat.pokebox.adapters.ListDeckAdapter
import com.omegat.pokebox.bd.DBHelper
import com.omegat.pokebox.data.CardDeckCheck
import com.omegat.pokebox.data.CardRepository
import com.omegat.pokebox.data.PokemonCard
import com.omegat.pokebox.data.PokemonSet
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class DecksFragment : Fragment() {

    private lateinit var sets: List<PokemonSet>
    private lateinit var cards: List<PokemonCard>
    private lateinit var rviewadap: ListDeckAdapter
    private var decklistText: String = ""
    private var hasDecklistLoaded = false
    private lateinit var db: DBHelper

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_decks, container, false)
        
        db = DBHelper(requireContext())
        cards = CardRepository.getCards()
        
        val sinputStream = requireContext().assets.open("json/sets/en.json")
        val sreader = JsonReader(sinputStream.reader())
        val stype = object : TypeToken<List<PokemonSet>>() {}.type
        sets = Gson().fromJson(sreader, stype)
        sreader.close()
        
        setupViews(view)
        
        return view
    }

    private fun setupViews(view: View) {
        val spcols = view.findViewById<Spinner>(R.id.spDeckCollections)
        val btPasteDecklist = view.findViewById<Button>(R.id.btPasteDecklist)
        val btcheck = view.findViewById<soup.neumorphism.NeumorphCardView>(R.id.btDeckCheck)
        val rview = view.findViewById<RecyclerView>(R.id.rviewdeck)

        updateDecklistButtonText(btPasteDecklist)
        loadSpinner(requireContext(), db, spcols, -1)

        btPasteDecklist.setOnClickListener {
            showDecklistDialog()
        }

        btcheck.setOnClickListener {
            if (decklistText.isEmpty()) return@setOnClickListener

            val selectedcollection = spcols.selectedItem.toString()
            val selcolid = db.getCollectionFromName(selectedcollection)
            val checkedcards = mutableListOf<PokemonCard>()

            val deck = getCardsFromText(decklistText)
            val cardAmounts = mutableListOf<Int>()
            val cardOwned = mutableListOf<Int>()

            for (d in deck) {
                val set = sets.find { it.ptcgoCode.equals(d.ptcgocode, ignoreCase = true) }
                if (set != null) {
                    val found = cards.find { c ->
                        c.number == d.number.toString() && c.id?.startsWith(
                            set.id ?: "",
                            true
                        ) == true
                    }
                    if (found != null) {
                        checkedcards.add(found)
                        cardAmounts.add(d.count)

                        val cam = db.getCardAmount(selcolid, set.id + "-" + d.number)
                        cardOwned.add(cam)
                    }
                }
            }

            val checkedcardsL: List<PokemonCard> = checkedcards

            if (::rviewadap.isInitialized) {
                rviewadap.updateData(checkedcardsL, cardAmounts, cardOwned)
            } else {
                rviewadap = ListDeckAdapter(
                    requireContext(),
                    checkedcardsL,
                    cardOwned,
                    cardAmounts,
                    getCurrentColId = { spcols.selectedItem?.let { db.getCollectionFromName(it.toString()) } ?: -1 }
                ) { selectedCard, selcolid ->
                    val i = Intent(requireContext(), ViewCard::class.java)
                    i.putExtra("col", selcolid)
                    i.putExtra("pcard", selectedCard)
                    startActivityForResult(i, REQUEST_VIEW_CARD)
                }
                rview.setHasFixedSize(true)
                rview.adapter = rviewadap
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_VIEW_CARD) {
            refreshDeckList()
        }
    }

    private fun refreshDeckList() {
        if (::rviewadap.isInitialized && hasDecklistLoaded) {
            val spcols = view?.findViewById<Spinner>(R.id.spDeckCollections) ?: return
            val selectedcollection = spcols.selectedItem.toString()
            val selcolid = db.getCollectionFromName(selectedcollection)

            val newCardOwned = mutableListOf<Int>()
            for (card in rviewadap.cards) {
                val cam = db.getCardAmount(selcolid, card.id ?: "")
                newCardOwned.add(cam)
            }
            rviewadap.updateData(rviewadap.cards, rviewadap.cardAmounts, newCardOwned)
        }
    }

    private fun showDecklistDialog() {
        val inflater = LayoutInflater.from(requireContext())
        val dialogView = inflater.inflate(R.layout.dialog_paste_decklist, null)
        val etDialog = dialogView.findViewById<EditText>(R.id.etDecklistDialog)

        val clipboard = requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clipData = clipboard.primaryClip
        if (clipData != null && clipData.itemCount > 0) {
            val clipText = clipData.getItemAt(0).text?.toString() ?: ""
            etDialog.setText(clipText)
        } else {
            etDialog.setText(decklistText)
        }

        AlertDialog.Builder(requireContext())
            .setTitle(getString(R.string.paste_decklist))
            .setView(dialogView)
            .setPositiveButton(getString(R.string.add)) { _, _ ->
                decklistText = etDialog.text.toString()
                hasDecklistLoaded = decklistText.isNotEmpty()
                view?.findViewById<Button>(R.id.btPasteDecklist)?.let { updateDecklistButtonText(it) }
            }
            .setNegativeButton(getString(R.string.cancelar), null)
            .show()
    }

    private fun updateDecklistButtonText(button: Button) {
        button.text = if (hasDecklistLoaded) {
            getString(R.string.decklist_loaded)
        } else {
            getString(R.string.paste_decklist)
        }
    }

    private fun loadSpinner(context: Context, db: DBHelper, spcols: Spinner, colid: Int) {
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
                val adapter = ArrayAdapter<String>(context, android.R.layout.simple_spinner_item)
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)

                if (collections.isEmpty()) {
                    adapter.add(getString(R.string.no_existen_colecciones))
                    spcols.isEnabled = false
                } else {
                    for (name in collections) adapter.add(name)
                    spcols.isEnabled = true
                }
                spcols.adapter = adapter

                spcols.setSelection(adapter.getPosition(db.getCollectionFromID(colid)))
            }
        }
    }

    private fun getCardsFromText(input: String): List<CardDeckCheck> {
        val regex = Regex("""^(\d+)\s+(.+?)\s+(?=[A-Z]{2,4}\s+\d+$)([A-Z]{2,4})\s+(\d+)$""")

        val cards = input.lines()
            .mapNotNull { line ->
                regex.matchEntire(line.trim())?.destructured?.let { (count, name, ptcgocode, number) ->
                    CardDeckCheck(count.toInt(), name.trim(), ptcgocode, number.toInt())
                }
            }
        return cards
    }

    companion object {
        private const val REQUEST_VIEW_CARD = 1001
    }
}

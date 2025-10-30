package com.example.pokebox.bd

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.util.Log
import androidx.core.database.sqlite.transaction
import com.example.pokebox.data.PokemonCard
import com.example.pokebox.data.PokemonSet
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.google.gson.stream.JsonReader

class InitializeBD() {

    fun setsycartas(c: Context, db: DBHelper) {
        val sinputStream = c.assets.open("json/sets/en.json")
        val sreader = JsonReader(sinputStream.reader())
        val stype = object : TypeToken<List<PokemonSet>>() {}.type
        val sets: List<PokemonSet> = Gson().fromJson(sreader, stype)
        sreader.close()

        val database = db.writableDatabase
        database.transaction {

            for (set in sets) {
                val cursor = db.getSetByID(set.id)
                val exists = cursor.use { it.moveToFirst() }

                if (!exists) {
                    db.addSet(set.id, set.name, set.ptcgoCode, this)

                    val path = "json/cards/en/${set.id}.json"
                    val cinputStream = c.assets.open(path)
                    val creader = JsonReader(cinputStream.reader())
                    val ctype = object : TypeToken<List<PokemonCard>>() {}.type
                    val cards: List<PokemonCard> = Gson().fromJson(creader, ctype)
                    creader.close()
                    Log.d(null, "Cargando set: ${set.name}")

                    for (card in cards) {
                        db.addCard(card.id, set.id, this)
                    }
                }
            }

        }
    }

    fun crearcoleccion(c: Context, db: DBHelper, name: String) {

        db.addCollection(name)
        val colid = db.getCollectionFromName(name)

        val sinputStream = c.assets.open("json/sets/en.json")
        val sreader = JsonReader(sinputStream.reader())
        val stype = object : TypeToken<List<PokemonSet>>() {}.type
        val sets: List<PokemonSet> = Gson().fromJson(sreader, stype)
        sreader.close()

        val wdb = db.writableDatabase
        wdb.transaction {

            for (set in sets) {

                val cur: Cursor = db.getCardsFromSet(set.id)
                cur.use {
                    while (it.moveToNext()) {

                        val cardid = it.getString(it.getColumnIndexOrThrow("cardID"))
                        db.addCardtoCollection(colid, cardid, wdb)

                    }
                }

            }

        }

    }

    fun actualizarcoleccion(c: Context, db: DBHelper, cname: String) {
        val wdb = db.writableDatabase

        val colID = db.getCollectionFromName(cname) ?: return

        wdb.transaction {
            val allCards = mutableListOf<String>()
            val existingCards = mutableSetOf<String>()

            wdb.rawQuery("SELECT ${DBHelper.CARD_ID} FROM ${DBHelper.TABLA_CARTAS}", null).use { cur ->
                while (cur.moveToNext()) {
                    allCards.add(cur.getString(cur.getColumnIndexOrThrow(DBHelper.CARD_ID)))
                }
            }

            wdb.rawQuery(
                "SELECT ${DBHelper.CARD_ID} FROM ${DBHelper.TABLA_CARTASCOLECCION} WHERE ${DBHelper.COL_ID} = ?",
                arrayOf(colID.toString())
            ).use { cur ->
                while (cur.moveToNext()) {
                    existingCards.add(cur.getString(cur.getColumnIndexOrThrow(DBHelper.CARD_ID)))
                }
            }

            val values = ContentValues()
            for (cardID in allCards) {
                if (!existingCards.contains(cardID)) {
                    values.clear()
                    values.put(DBHelper.COL_ID, colID)
                    values.put(DBHelper.CARD_ID, cardID)
                    values.put(DBHelper.CC_AMOUNT, 0)
                    insert(DBHelper.TABLA_CARTASCOLECCION, null, values)
                }
            }

        }
    }
}

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
        val SinputStream = c.assets.open("json/sets/en.json")
        val Sreader = JsonReader(SinputStream.reader())
        val Stype = object : TypeToken<List<PokemonSet>>() {}.type
        val sets: List<PokemonSet> = Gson().fromJson(Sreader, Stype)
        Sreader.close()

        val database = db.writableDatabase
        database.transaction {

            for (set in sets) {
                val cursor = db.getSetByID(set.id)
                val exists = cursor.use { it.moveToFirst() }

                if (!exists) {
                    db.addSet(set.id, set.name, set.ptcgoCode, this)

                    val path = "json/cards/en/${set.id}.json"
                    val CinputStream = c.assets.open(path)
                    val Creader = JsonReader(CinputStream.reader())
                    val Ctype = object : TypeToken<List<PokemonCard>>() {}.type
                    val cards: List<PokemonCard> = Gson().fromJson(Creader, Ctype)
                    Creader.close()
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

        val SinputStream = c.assets.open("json/sets/en.json")
        val Sreader = JsonReader(SinputStream.reader())
        val Stype = object : TypeToken<List<PokemonSet>>() {}.type
        val sets: List<PokemonSet> = Gson().fromJson(Sreader, Stype)
        Sreader.close()

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
        val rdb = db.readableDatabase
        val wdb = db.writableDatabase

        val colID = db.getCollectionFromName(cname)
        if (colID == null) return

        wdb.beginTransaction()

        val allCards = rdb.rawQuery("SELECT ${DBHelper.cardID} FROM ${DBHelper.TABLA_CARTAS}", null)

        allCards.use { cur ->
            while (cur.moveToNext()) {
                val cardID = cur.getString(cur.getColumnIndexOrThrow(DBHelper.cardID))

                val existsCursor = rdb.rawQuery(
                    "SELECT 1 FROM ${DBHelper.TABLA_CARTASCOLECCION} WHERE ${DBHelper.colID} = ? AND ${DBHelper.cardID} = ? LIMIT 1",
                    arrayOf(colID.toString(), cardID)
                )

                val exists = existsCursor.use { it.moveToFirst() }

                if (!exists) {
                    val values = ContentValues().apply {
                        put(DBHelper.colID, colID)
                        put(DBHelper.cardID, cardID)
                        put(DBHelper.ccamount, 0)
                    }
                    wdb.insert(DBHelper.TABLA_CARTASCOLECCION, null, values)
                }
            }
        }

        wdb.setTransactionSuccessful()
    }
}
package com.example.pokebox.bd

import android.content.Context
import android.util.Log
import com.example.pokebox.data.PokemonCard
import com.example.pokebox.data.PokemonSet
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.google.gson.stream.JsonReader

class InitializeBD () {

    fun setsycartas (c: Context, db: DBHelper) {
        val SinputStream = c.assets.open("json/sets/en.json")
        val Sreader = JsonReader(SinputStream.reader())
        val Stype = object : TypeToken<List<PokemonSet>>() {}.type
        val sets: List<PokemonSet> = Gson().fromJson(Sreader, Stype)
        Sreader.close()

        val database = db.writableDatabase
        database.beginTransaction()

        try {
            for (set in sets) {
                val cursor = db.getSetByID(set.id)
                val exists = cursor.use { it.moveToFirst() }

                if (!exists) {
                    db.addSet(set.id, set.name, database)

                    val path = "json/cards/en/${set.id}.json"
                    val CinputStream = c.assets.open(path)
                    val Creader = JsonReader(CinputStream.reader())
                    val Ctype = object : TypeToken<List<PokemonCard>>() {}.type
                    val cards: List<PokemonCard> = Gson().fromJson(Creader, Ctype)
                    Creader.close()
                    Log.d(null, "Cargando set: ${set.name}")

                    for (card in cards) {
                        db.addCard(card.id, set.id, database)
                    }
                }
            }

            database.setTransactionSuccessful()
        } finally {
            database.endTransaction()
            database.close()
        }
    }

    fun crearcoleccion (c: Context, db: DBHelper, name: String) {

    }

    //Comprobar si se han añadido sets para en la colección añadir esas cartas nuevas con amount 0
    fun actualizarcoleccion (c: Context, db: DBHelper, cID: String) {



    }

}
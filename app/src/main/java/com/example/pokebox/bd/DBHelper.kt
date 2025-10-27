package com.example.pokebox.bd

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class DBHelper(context: Context?) :
    SQLiteOpenHelper(context, DB_NAME, null, DATABASE_VERSION) {

    override fun onConfigure(db: SQLiteDatabase?) {
        super.onConfigure(db)
        db?.setForeignKeyConstraintsEnabled(true)
    }

    override fun onCreate(db: SQLiteDatabase?) {
        val CREATE_TABLA_SETS =
            "CREATE TABLE ${TABLA_SETS} ($setID TEXT PRIMARY KEY, $setName TEXT)"
        val CREATE_TABLA_CARTAS =
            "CREATE TABLE ${TABLA_CARTAS} ($cardID TEXT PRIMARY KEY NOT NULL, $setID TEXT NOT NULL, FOREIGN KEY ($setID) REFERENCES $TABLA_SETS($setID))"
        val CREATE_TABLA_COLECCIONES =
            "CREATE TABLE ${TABLA_COLECCIONES} ($colID INTEGER PRIMARY KEY AUTOINCREMENT, $colName TEXT)"
        val CREATE_TABLA_CARTASCOLECCION =
            "CREATE TABLE ${TABLA_CARTASCOLECCION} ($cardID TEXT, $colID INTEGER, $ccamount INTEGER, PRIMARY KEY ($cardID, $colID), FOREIGN KEY ($cardID) REFERENCES $TABLA_CARTAS($cardID), FOREIGN KEY ($colID) REFERENCES $TABLA_COLECCIONES($colID))"

        db!!.execSQL(CREATE_TABLA_SETS)
        db.execSQL(CREATE_TABLA_CARTAS)
        db.execSQL(CREATE_TABLA_COLECCIONES)
        db.execSQL(CREATE_TABLA_CARTASCOLECCION)
    }

    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
        db?.execSQL("DROP TABLE IF EXISTS ${TABLA_CARTASCOLECCION}")
        db?.execSQL("DROP TABLE IF EXISTS  ${TABLA_CARTAS}")
        db?.execSQL("DROP TABLE IF EXISTS ${TABLA_COLECCIONES}")
        db?.execSQL("DROP TABLE IF EXISTS  ${TABLA_SETS}")

        onCreate(db)
    }

    fun addSet(id: String?, name: String?, db: SQLiteDatabase? = null): Boolean {
        val database = db ?: writableDatabase
        val values = ContentValues().apply {
            put(setID, id)
            put(setName, name)
        }
        return database.insert(TABLA_SETS, null, values) != -1L
    }

    fun addCard(id: String?, setIDx: String?, db: SQLiteDatabase? = null): Boolean {
        val database = db ?: writableDatabase
        val values = ContentValues().apply {
            put(cardID, id)
            put(setID, setIDx)
        }
        return database.insert(TABLA_CARTAS, null, values) != -1L
    }

    fun addCollection (name: String?): Boolean {
        val values = ContentValues().apply {
            put(colName, name)
        }

        return writableDatabase.use { db ->
            db.insert(TABLA_COLECCIONES, null, values) != -1L
        }
    }

    fun getSetByID (setIDq: String?): Cursor {
        return readableDatabase.rawQuery("SELECT * FROM $TABLA_SETS WHERE $setID = ?", arrayOf(setIDq))
    }

    fun getCardByID (cardIDq: String?): Cursor {
        return readableDatabase.rawQuery("SELECT * FROM $TABLA_CARTAS WHERE $cardID = ?", arrayOf(cardIDq))
    }

    fun getCardAmount (colIDq: Int?, cardIDq: String?): Int {
        readableDatabase.use { db ->
            val cursor = db.rawQuery(
                "SELECT $ccamount FROM $TABLA_CARTASCOLECCION WHERE $cardID = ? AND $colID = ?",
                arrayOf(cardIDq, colIDq.toString()))

            cursor.use {
                return if (it.moveToFirst()) {
                    it.getInt(it.getColumnIndexOrThrow(ccamount))
                } else {
                    0
                }
            }

        }
    }

    fun getSetPercentage (setIDq: String?, colIDq: Int?): Int {
        readableDatabase.use { db ->
            val cursor = db.rawQuery("""
            SELECT (COUNT(DISTINCT c.$cardID) * 100) / t.total AS percentage
            FROM $TABLA_CARTAS c
            LEFT JOIN $TABLA_CARTASCOLECCION cc 
                ON c.$cardID = cc.$cardID AND cc.$colID = ?
            JOIN (
                SELECT COUNT(*) AS total
                FROM $TABLA_CARTAS
                WHERE $setID = ?
            ) t
            WHERE c.$setID = ? AND cc.$ccamount > 0
        """.trimIndent(), arrayOf(colIDq.toString(), setIDq, setIDq))

            cursor.use {
                return if (it.moveToFirst()) {
                    it.getInt(it.getColumnIndexOrThrow("percentage"))
                } else {
                    0
                }
            }
        }
    }

    companion object {
        private const val DB_NAME: String = "dbcoleccion"
        private const val DATABASE_VERSION = 1
        const val TABLA_SETS = "Sets"
        const val TABLA_CARTAS = "Carta"
        const val TABLA_COLECCIONES = "Coleccion"
        const val TABLA_CARTASCOLECCION = "CartasColeccion"
        const val setID = "setID"
        const val setName = "setName"
        const val cardID = "cardID"
        const val colID = "colID"
        const val colName = "colName"
        const val ccamount = "ccamount"
    }
}


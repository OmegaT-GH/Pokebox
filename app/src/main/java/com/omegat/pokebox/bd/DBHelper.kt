package com.omegat.pokebox.bd

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.util.Log
import kotlinx.coroutines.sync.Mutex

class DBHelper(context: Context?) :
    SQLiteOpenHelper(context, DB_NAME, null, DATABASE_VERSION) {

    override fun onConfigure(db: SQLiteDatabase?) {
        super.onConfigure(db)
        db?.setForeignKeyConstraintsEnabled(true)
    }

    override fun onCreate(db: SQLiteDatabase?) {
        val createTablaSets =
            "CREATE TABLE $TABLA_SETS ($SET_ID TEXT PRIMARY KEY, $SET_NAME TEXT, $SET_CODE TEXT)"
        val createTablaCartas =
            "CREATE TABLE $TABLA_CARTAS ($CARD_ID TEXT PRIMARY KEY NOT NULL, $SET_ID TEXT NOT NULL, FOREIGN KEY ($SET_ID) REFERENCES $TABLA_SETS($SET_ID))"
        val createTablaColecciones =
            "CREATE TABLE $TABLA_COLECCIONES ($COL_ID INTEGER PRIMARY KEY AUTOINCREMENT, $COL_NAME TEXT UNIQUE)"
        val createTablaCartasColeccion =
            "CREATE TABLE $TABLA_CARTASCOLECCION ($CARD_ID TEXT, $COL_ID INTEGER, $CC_AMOUNT INTEGER, PRIMARY KEY ($CARD_ID, $COL_ID), FOREIGN KEY ($CARD_ID) REFERENCES $TABLA_CARTAS($CARD_ID), FOREIGN KEY ($COL_ID) REFERENCES $TABLA_COLECCIONES($COL_ID))"

        db!!.execSQL(createTablaSets)
        db.execSQL(createTablaCartas)
        db.execSQL(createTablaColecciones)
        db.execSQL(createTablaCartasColeccion)
    }

    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
        db?.execSQL("DROP TABLE IF EXISTS $TABLA_CARTASCOLECCION")
        db?.execSQL("DROP TABLE IF EXISTS  $TABLA_CARTAS")
        db?.execSQL("DROP TABLE IF EXISTS $TABLA_COLECCIONES")
        db?.execSQL("DROP TABLE IF EXISTS  $TABLA_SETS")

        onCreate(db)
    }

    fun addSet(id: String?, name: String?, code: String?, db: SQLiteDatabase? = null): Boolean {
        val database = db ?: writableDatabase
        val values = ContentValues().apply {
            put(SET_ID, id)
            put(SET_NAME, name)
            put(SET_CODE, code)
        }
        return database.insert(TABLA_SETS, null, values) != -1L
    }

    fun addCard(id: String?, setIDx: String?, db: SQLiteDatabase? = null): Boolean {
        val database = db ?: writableDatabase
        val values = ContentValues().apply {
            put(CARD_ID, id)
            put(SET_ID, setIDx)
        }
        return database.insert(TABLA_CARTAS, null, values) != -1L
    }

    fun addCollection(name: String?): Boolean {
        val values = ContentValues().apply {
            put(COL_NAME, name)
        }

        return writableDatabase.use { db ->
            db.insert(TABLA_COLECCIONES, null, values) != -1L
        }
    }

    fun addEmptyCardtoCollection(colIDq: Int?, cardIDq: String?, db: SQLiteDatabase): Boolean {
        val values = ContentValues().apply {
            put(COL_ID, colIDq)
            put(CARD_ID, cardIDq)
            put(CC_AMOUNT, 0)
        }

        return db.insert(TABLA_CARTASCOLECCION, null, values) != 1L
    }

    fun addCardtoCollection(colIDq: Int?, cardIDq: String?, db: SQLiteDatabase): Boolean {
        val query = """
        UPDATE $TABLA_CARTASCOLECCION
        SET $CC_AMOUNT = $CC_AMOUNT + 1
        WHERE $COL_ID = ? AND $CARD_ID = ?
        """.trimIndent()

        return try {
            db.execSQL(query, arrayOf(colIDq.toString(), cardIDq))
            true
        } catch (e: Exception) {
            Log.e("DB", "Error incrementando cantidad de carta", e)
            false
        }
    }

    fun getSetByID(setIDq: String?): Cursor {
        return readableDatabase.rawQuery(
            "SELECT * FROM $TABLA_SETS WHERE $SET_ID = ?",
            arrayOf(setIDq)
        )
    }

//    fun getCardByID (cardIDq: String?): Cursor {
//        return readableDatabase.rawQuery("SELECT * FROM $TABLA_CARTAS WHERE $cardID = ?", arrayOf(cardIDq))
//    }

    fun getCardsFromSet(setIDq: String?): Cursor {
        return readableDatabase.rawQuery(
            "SELECT * FROM $TABLA_CARTAS WHERE $SET_ID = ?",
            arrayOf(setIDq)
        )
    }

    fun getCollectionFromName(colnameq: String?): Int? {
        val cur: Cursor = readableDatabase.rawQuery(
            "SELECT $COL_ID FROM $TABLA_COLECCIONES WHERE $COL_NAME = ?",
            arrayOf(colnameq)
        )
        cur.use {
            return if (it.moveToFirst()) it.getInt(it.getColumnIndexOrThrow(COL_ID)) else null
        }
    }

    fun getSetswithcards(colIDq: Int?): Cursor {
        val query = """
        SELECT DISTINCT s.$SET_ID
        FROM $TABLA_SETS s
        JOIN $TABLA_CARTAS c ON s.$SET_ID = c.$SET_ID
        JOIN $TABLA_CARTASCOLECCION cc 
        ON c.$CARD_ID = cc.$CARD_ID AND cc.$COL_ID = ?
        WHERE cc.$CC_AMOUNT > 0
    """.trimIndent()

        return readableDatabase.rawQuery(query, arrayOf(colIDq.toString()))
    }

    fun getCardAmount(colIDq: Int?, cardIDq: String?): Int {
        readableDatabase.use { db ->
            val cursor = db.rawQuery(
                "SELECT $CC_AMOUNT FROM $TABLA_CARTASCOLECCION WHERE $CARD_ID = ? AND $COL_ID = ?",
                arrayOf(cardIDq, colIDq.toString())
            )

            cursor.use {
                return if (it.moveToFirst()) {
                    it.getInt(it.getColumnIndexOrThrow(CC_AMOUNT))
                } else {
                    0
                }
            }

        }
    }

    fun getSetCardAmount(setIDq: String, colIDq: Int?): Int {
        val query = """
        SELECT COUNT(DISTINCT c.$CARD_ID) AS cardCount
        FROM $TABLA_CARTAS c
        JOIN $TABLA_CARTASCOLECCION cc ON c.$CARD_ID = cc.$CARD_ID
        WHERE c.$SET_ID = ? AND cc.$COL_ID = ? AND cc.$CC_AMOUNT > 0
    """.trimIndent()


        val cur = readableDatabase.rawQuery(query, arrayOf(setIDq, colIDq.toString()))
        cur.use {
            return if (cur.moveToFirst()) cur.getInt(cur.getColumnIndexOrThrow("cardCount")) else 0
        }
    }

    fun debugRemoveAllCollections() {
        val wdb = writableDatabase
        val query = """
            DELETE FROM $TABLA_CARTASCOLECCION
        """.trimIndent()
        val query2 = """
            DELETE FROM $TABLA_COLECCIONES
        """.trimIndent()
        wdb.execSQL(query)
        wdb.execSQL(query2)
    }

    fun getSetPercentage(setIDq: String?, colIDq: Int?): Int {
        readableDatabase.use { db ->
            val cursor = db.rawQuery(
                """
            SELECT (COUNT(DISTINCT c.$CARD_ID) * 100) / t.total AS percentage
            FROM $TABLA_CARTAS c
            LEFT JOIN $TABLA_CARTASCOLECCION cc 
                ON c.$CARD_ID = cc.$CARD_ID AND cc.$COL_ID = ?
            JOIN (
                SELECT COUNT(*) AS total
                FROM $TABLA_CARTAS
                WHERE $SET_ID = ?
            ) t
            WHERE c.$SET_ID = ? AND cc.$CC_AMOUNT > 0
        """.trimIndent(), arrayOf(colIDq.toString(), setIDq, setIDq)
            )

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
        const val SET_ID = "setID"
        const val SET_NAME = "setName"
        const val SET_CODE = "setCode"
        const val CARD_ID = "cardID"
        const val COL_ID = "colID"
        const val COL_NAME = "colName"
        const val CC_AMOUNT = "ccamount"
        val dbMutex = Mutex()
    }
}


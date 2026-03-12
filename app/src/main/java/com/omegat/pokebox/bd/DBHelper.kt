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
            "CREATE TABLE $TABLA_CARTASCOLECCION ($CARD_ID TEXT, $COL_ID INTEGER, $CC_AMOUNT INTEGER, $CC_LAST_MODIFIED INTEGER, PRIMARY KEY ($CARD_ID, $COL_ID), FOREIGN KEY ($CARD_ID) REFERENCES $TABLA_CARTAS($CARD_ID), FOREIGN KEY ($COL_ID) REFERENCES $TABLA_COLECCIONES($COL_ID))"
        val createTablaLogMovimientos =
            "CREATE TABLE $TABLA_LOG_MOVIMIENTOS ($LOG_ID INTEGER PRIMARY KEY AUTOINCREMENT, $COL_ID INTEGER NOT NULL, $CARD_ID TEXT NOT NULL, $LOG_CANTIDAD_ANTERIOR INTEGER NOT NULL, $LOG_CANTIDAD_NUEVA INTEGER NOT NULL, $LOG_TIMESTAMP INTEGER NOT NULL, FOREIGN KEY ($COL_ID) REFERENCES $TABLA_COLECCIONES($COL_ID), FOREIGN KEY ($CARD_ID) REFERENCES $TABLA_CARTAS($CARD_ID))"

        db!!.execSQL(createTablaSets)
        db.execSQL(createTablaCartas)
        db.execSQL(createTablaColecciones)
        db.execSQL(createTablaCartasColeccion)
        db.execSQL(createTablaLogMovimientos)
    }

    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 2) {
            db?.execSQL("ALTER TABLE $TABLA_CARTASCOLECCION ADD COLUMN $CC_LAST_MODIFIED INTEGER DEFAULT NULL")
        }
        if (oldVersion < 3) {
            val createTablaLogMovimientos =
                "CREATE TABLE $TABLA_LOG_MOVIMIENTOS ($LOG_ID INTEGER PRIMARY KEY AUTOINCREMENT, $COL_ID INTEGER NOT NULL, $CARD_ID TEXT NOT NULL, $LOG_CANTIDAD_ANTERIOR INTEGER NOT NULL, $LOG_CANTIDAD_NUEVA INTEGER NOT NULL, $LOG_TIMESTAMP INTEGER NOT NULL, FOREIGN KEY ($COL_ID) REFERENCES $TABLA_COLECCIONES($COL_ID), FOREIGN KEY ($CARD_ID) REFERENCES $TABLA_CARTAS($CARD_ID))"
            db?.execSQL(createTablaLogMovimientos)
        }
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

        return db.insert(TABLA_CARTASCOLECCION, null, values) != -1L
    }

    fun addCardtoCollection(colIDq: Int?, cardIDq: String?, db: SQLiteDatabase, amount: Int): Boolean {
        return try {
            // Obtener cantidad anterior
            val previousAmount = getCardAmount(colIDq, cardIDq)
            
            // Actualizar cantidad y timestamp
            val timestamp = if (amount > 0) System.currentTimeMillis() else null
            val query = """
            UPDATE $TABLA_CARTASCOLECCION
            SET $CC_AMOUNT = ?, $CC_LAST_MODIFIED = ?
            WHERE $COL_ID = ? AND $CARD_ID = ?
            """.trimIndent()
            db.execSQL(query, arrayOf(amount.toString(), timestamp?.toString(), colIDq.toString(), cardIDq))
            
            // Registrar en log si la cantidad cambió y la nueva cantidad es > 0
            if (previousAmount != amount && amount > 0) {
                addLogEntry(colIDq, cardIDq, previousAmount, amount, db)
            }
            
            true
        } catch (e: Exception) {
            Log.e("DB", "Error incrementando cantidad de carta", e)
            false
        }
    }
    
    private fun addLogEntry(colIDq: Int?, cardIDq: String?, previousAmount: Int, newAmount: Int, db: SQLiteDatabase) {
        val values = ContentValues().apply {
            put(COL_ID, colIDq)
            put(CARD_ID, cardIDq)
            put(LOG_CANTIDAD_ANTERIOR, previousAmount)
            put(LOG_CANTIDAD_NUEVA, newAmount)
            put(LOG_TIMESTAMP, System.currentTimeMillis())
        }
        db.insert(TABLA_LOG_MOVIMIENTOS, null, values)
    }
    
    fun cleanOldLogEntries() {
        writableDatabase.use { db ->
            try {
                val query = """
                DELETE FROM $TABLA_LOG_MOVIMIENTOS 
                WHERE $LOG_ID NOT IN (
                    SELECT $LOG_ID 
                    FROM (
                        SELECT $LOG_ID, 
                               ROW_NUMBER() OVER (PARTITION BY $COL_ID ORDER BY $LOG_TIMESTAMP DESC) as rn
                        FROM $TABLA_LOG_MOVIMIENTOS
                    )
                    WHERE rn <= 30
                )
                """.trimIndent()
                db.execSQL(query)
            } catch (e: Exception) {
                Log.e("DB", "Error limpiando logs antiguos", e)
            }
        }
    }
    
    fun getLogMovimientos(colIDq: Int?): Cursor {
        val query = """
        SELECT * FROM $TABLA_LOG_MOVIMIENTOS
        WHERE $COL_ID = ?
        ORDER BY $LOG_TIMESTAMP DESC
        """.trimIndent()
        return readableDatabase.rawQuery(query, arrayOf(colIDq.toString()))
    }

    fun getSetByID(setIDq: String?): Cursor {
        return readableDatabase.rawQuery(
            "SELECT * FROM $TABLA_SETS WHERE $SET_ID = ?",
            arrayOf(setIDq)
        )
    }

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

    fun getCollectionFromID(colIDq: Int?): String? {
        val cur: Cursor = readableDatabase.rawQuery(
            "SELECT $COL_NAME FROM $TABLA_COLECCIONES WHERE $COL_ID = ?",
            arrayOf(colIDq.toString())
        )
        cur.use {
            return if (it.moveToFirst()) it.getString(it.getColumnIndexOrThrow(COL_NAME)) else null
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
        val db = readableDatabase
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

    fun removeCollection(colIDq: Int?) {
        val wdb = writableDatabase
        val query1 = """
            DELETE FROM $TABLA_LOG_MOVIMIENTOS WHERE $COL_ID = ?
        """.trimIndent()
        val query2 = """
            DELETE FROM $TABLA_CARTASCOLECCION WHERE $COL_ID = ?
        """.trimIndent()
        val query3 = """
            DELETE FROM $TABLA_COLECCIONES WHERE $COL_ID = ?
        """.trimIndent()
        wdb.execSQL(query1, arrayOf(colIDq))
        wdb.execSQL(query2, arrayOf(colIDq))
        wdb.execSQL(query3, arrayOf(colIDq))
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
        private const val DATABASE_VERSION = 3
        const val TABLA_SETS = "Sets"
        const val TABLA_CARTAS = "Carta"
        const val TABLA_COLECCIONES = "Coleccion"
        const val TABLA_CARTASCOLECCION = "CartasColeccion"
        const val TABLA_LOG_MOVIMIENTOS = "LogMovimientos"
        const val SET_ID = "setID"
        const val SET_NAME = "setName"
        const val SET_CODE = "setCode"
        const val CARD_ID = "cardID"
        const val COL_ID = "colID"
        const val COL_NAME = "colName"
        const val CC_AMOUNT = "ccamount"
        const val CC_LAST_MODIFIED = "cclastmodified"
        const val LOG_ID = "logID"
        const val LOG_CANTIDAD_ANTERIOR = "cantidadAnterior"
        const val LOG_CANTIDAD_NUEVA = "cantidadNueva"
        const val LOG_TIMESTAMP = "timestamp"
        val dbMutex = Mutex()
    }
}


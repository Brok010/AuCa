package com.example.logindb

import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.util.Log
import android.widget.Toast
import com.example.logindb.Classes.Card
import com.example.logindb.Classes.Deck
import com.example.logindb.Classes.User
import java.io.File
import java.io.FileInputStream

class DatabaseHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    private val CREATE_TABLE_USER = "CREATE TABLE $TABLE_USER (" +
            "$COL_USER_ID INTEGER PRIMARY KEY AUTOINCREMENT, " +
            "$COL_USER_NAME TEXT, " +
            "$COL_USER_EMAIL TEXT, " +
            "$COL_USER_PASSWORD TEXT)"

    private val CREATE_TABLE_DECK = "CREATE TABLE $TABLE_DECK (" +
            "$COL_DECK_ID INTEGER PRIMARY KEY AUTOINCREMENT, " +
            "$COL_DECK_NAME TEXT, " +
            "$COL_DECK_CARD_COUNT INTEGER, " +
            "$COL_USER_ID_FK INTEGER, " +
            "FOREIGN KEY($COL_USER_ID_FK) REFERENCES $TABLE_USER($COL_USER_ID))"

    private val CREATE_TABLE_CARD = "CREATE TABLE $TABLE_CARD (" +
            "$COL_CARD_ID INTEGER PRIMARY KEY AUTOINCREMENT, " +
            "$COL_CARD_TOP TEXT, " +
            "$COL_CARD_BOTTOM TEXT, " +
            "$COL_CARD_TOP_FILE_PATH TEXT, " +
            "$COL_CARD_BOTTOM_FILE_PATH TEXT, " +
            "$COL_DECK_ID_FK INTEGER, " +
            "$COL_CARD_USER_ID_FK INTEGER, " +
            "$COL_CARD_TIMEOUT INTEGER, " +
            "$COL_CARD_LAST_UPDATE_TIME INTEGER, " +
            "$COL_CARD_COEFFICIENT REAL, " +
            "FOREIGN KEY($COL_DECK_ID_FK) REFERENCES $TABLE_DECK($COL_DECK_ID), " +
            "FOREIGN KEY($COL_CARD_USER_ID_FK) REFERENCES $TABLE_USER($COL_USER_ID))"



    override fun onCreate(db: SQLiteDatabase?) {
        db?.execSQL(CREATE_TABLE_USER)
        db?.execSQL(CREATE_TABLE_DECK)
        db?.execSQL(CREATE_TABLE_CARD)
    }

    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
        // Handle database upgrades by dropping and recreating tables
        db?.execSQL("DROP TABLE IF EXISTS $TABLE_USER")
        db?.execSQL("DROP TABLE IF EXISTS $TABLE_DECK")
        db?.execSQL("DROP TABLE IF EXISTS $TABLE_CARD")

        // Recreate tables with updated schema
        onCreate(db)
    }

    fun registerUser (user: User){
        val db = this.writableDatabase
        val value = ContentValues()
        value.put(COL_USER_NAME, user.username)
        value.put(COL_USER_EMAIL, user.email)
        value.put(COL_USER_PASSWORD, user.password)

        db.insert(TABLE_USER, null, value)
        db.close()
    }

    fun getAllCards(userId: Int): List<Card> {
        val cards = mutableListOf<Card>()

        val query = "SELECT * FROM $TABLE_CARD " +
                "INNER JOIN $TABLE_DECK ON $TABLE_CARD.$COL_DECK_ID_FK = $TABLE_DECK.$COL_DECK_ID " +
                "WHERE $TABLE_DECK.$COL_USER_ID_FK = ?"

        val selectionArgs = arrayOf(userId.toString())

        val cursor = readableDatabase.rawQuery(query, selectionArgs)

        try {
            while (cursor.moveToNext()) {
                val card = createCardFromCursor(cursor)
                cards.add(card)
            }
        } finally {
            cursor.close()
        }

        return cards
    }

    @SuppressLint("Range")
    private fun createCardFromCursor(cursor: Cursor): Card {
        return Card(
            userId = cursor.getInt(cursor.getColumnIndex(COL_CARD_USER_ID_FK)),
            deckId = cursor.getInt(cursor.getColumnIndex(COL_DECK_ID_FK)),
            id = cursor.getInt(cursor.getColumnIndex(COL_CARD_ID)),
//            cardTop = cursor.getBlob(cursor.getColumnIndex(COL_CARD_TOP)),
//            cardBottom = cursor.getBlob(cursor.getColumnIndex(COL_CARD_BOTTOM)),
            timeout = cursor.getLong(cursor.getColumnIndex(COL_CARD_TIMEOUT)),
            lastUpdateTime = cursor.getLong(cursor.getColumnIndex(COL_CARD_LAST_UPDATE_TIME))
//            coefficient = cursor.getDouble(cursor.getColumnIndex(COL_CARD_COEFFICIENT))
        )
    }

    fun updateCardTimeouts(userId: Int) {
        val cards = getAllCards(userId)
        val currentTimeSeconds = System.currentTimeMillis() / 1000

        for (card in cards) {
            val elapsedTimeSeconds = (currentTimeSeconds - card.lastUpdateTime).toInt()

            // Update the timeout based on the elapsed time
            var updatedTimeout = card.timeout - elapsedTimeSeconds
            if (updatedTimeout < 0) {
                updatedTimeout = 0
            }
            updateCardTimeout(card.id, updatedTimeout, currentTimeSeconds)
        }
    }

    private fun updateCardTimeout(cardId: Int, newTimeout: Long, newLastUpdateTime: Long) {
        val contentValues = ContentValues()
        contentValues.put(COL_CARD_TIMEOUT, newTimeout)
        contentValues.put(COL_CARD_LAST_UPDATE_TIME, newLastUpdateTime)

        val whereClause = "$COL_CARD_ID = ?"
        val whereArgs = arrayOf(cardId.toString())

        writableDatabase.update(TABLE_CARD, contentValues, whereClause, whereArgs)
    }

    @SuppressLint("Range")
    fun getDeckName(userId: Int, deckId: Int): String{
        val db = this.readableDatabase
        var deckName = ""

        val query = "SELECT $COL_DECK_NAME FROM $TABLE_DECK WHERE $COL_USER_ID_FK = $userId AND $COL_DECK_ID = $deckId"
        val cursor = db.rawQuery(query, null)

        try {
            if (cursor.moveToFirst()) {
                deckName = cursor.getString(cursor.getColumnIndex(COL_DECK_NAME))
            }
        } catch (e: Exception) {
            // Handle exceptions, such as SQLiteException
            e.printStackTrace()
        } finally {
            cursor.close()
            db.close()
        }

        return deckName
    }

    fun getReadyCardCount(deckId: Int): Int {
        val db = this.readableDatabase
        var readyCardCount = 0

        val query = "SELECT COUNT(*) FROM $TABLE_CARD WHERE $COL_DECK_ID_FK = ? AND $COL_CARD_TIMEOUT = 0"
        val selectionArgs = arrayOf(deckId.toString())

        try {
            val cursor = db.rawQuery(query, selectionArgs)
            if (cursor.moveToFirst()) {
                readyCardCount = cursor.getInt(0)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            db.close()
        }

        return readyCardCount
    }

    fun deleteCard(card: Card, deckId: Int, userId: Int) {
        val pathTop = card.cardTopFilePath
        val pathBottom = card.cardBottomFilePath

        if (pathTop.isNotEmpty() && pathTop != "") {
            deleteFile(pathTop)
        }
        if (pathBottom.isNotEmpty() && pathBottom != "" ) {
            deleteFile(pathBottom)
        }

        val db = this.writableDatabase
        try {
            // Define the WHERE clause to match the cardId, deckId, and userId
            val whereClause = "$COL_CARD_ID=? AND $COL_DECK_ID_FK=? AND $COL_CARD_USER_ID_FK=?"
            val whereArgs = arrayOf(card.id.toString(), deckId.toString(), userId.toString())

            // Delete the card from the database
            db.delete(TABLE_CARD, whereClause, whereArgs)
        } catch (e: Exception) {
            // Handle exceptions, such as SQLiteException
            e.printStackTrace()
        } finally {
            db.close()
        }
    }

   private fun deleteFile(filePath: String) {
        try {
            val file = File(filePath)
            if (file.exists()) {
                file.delete()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun loginUser (username: String, password: String): Pair<Boolean, Long?>{
        val columns = arrayOf(COL_USER_ID)
        val db = this.readableDatabase
        val selection = "$COL_USER_NAME = ? AND $COL_USER_PASSWORD = ?"
        val selectionArgs = arrayOf(username, password)

        val cursor = db.query(
            TABLE_USER,
            columns,
            selection,
            selectionArgs,
            null,
            null,
            null)

        val loginSuccessful = cursor.moveToFirst()
        val userId: Long? = if (loginSuccessful) {
            val columnIndex = cursor.getColumnIndex(COL_USER_ID)
            if (columnIndex != -1) {
                cursor.getLong(columnIndex)
            } else {
                null
            }
        } else {
            null
        }

        cursor.close()
        db.close()

        return Pair(loginSuccessful, userId)
    }

    @SuppressLint("Range")
    fun getCardTimeOut(cardId: Int, deckId: Int, userId: Int): Int {
        val db = this.readableDatabase
        var cardTimeout = 0

        val query = "SELECT $COL_CARD_TIMEOUT FROM $TABLE_CARD WHERE $COL_CARD_ID = $cardId AND $COL_DECK_ID_FK = $deckId AND $COL_CARD_USER_ID_FK = $userId"
        val cursor = db.rawQuery(query, null)

        try {
            if (cursor.moveToFirst()) {
                cardTimeout = cursor.getInt(cursor.getColumnIndex(COL_CARD_TIMEOUT))
            }
        } catch (e: Exception) {
            // Handle exceptions, such as SQLiteException
            e.printStackTrace()
        } finally {
            cursor.close()
            db.close()
        }

        return cardTimeout
    }


    @SuppressLint("Range")
    fun getAllCards(userId: Int, deckId: Int):  MutableList<Card> {
        val db = this.readableDatabase
        val cards = mutableListOf<Card>()

        val query = "SELECT * FROM $TABLE_CARD WHERE $COL_CARD_USER_ID_FK = $userId AND $COL_DECK_ID_FK = $deckId"
        val cursor = db.rawQuery(query, null)

        try {
            while (cursor.moveToNext()) {
                val cardId = cursor.getInt(cursor.getColumnIndex(COL_CARD_ID))
                val cardTop = cursor.getString(cursor.getColumnIndex(COL_CARD_TOP))
                val cardBottom = cursor.getString(cursor.getColumnIndex(COL_CARD_BOTTOM))
                val cardTopFilePath = cursor.getString(cursor.getColumnIndex(COL_CARD_TOP_FILE_PATH))
                val cardBottomFilePath = cursor.getString(cursor.getColumnIndex(COL_CARD_BOTTOM_FILE_PATH))
                val cardTimeout = cursor.getLong(cursor.getColumnIndex(COL_CARD_TIMEOUT))
                val cardLastUpdateTime = cursor.getLong(cursor.getColumnIndex(COL_CARD_LAST_UPDATE_TIME))
                val cardCoefficient = cursor.getDouble(cursor.getColumnIndex(COL_CARD_COEFFICIENT))

                val card = Card(userId, deckId, cardId, cardTop, cardBottom, cardTopFilePath, cardBottomFilePath, cardTimeout, cardLastUpdateTime, cardCoefficient)
                cards.add(card)
            }
        } catch (e: Exception) {
            // Handle exceptions, such as SQLiteException
            e.printStackTrace()
        } finally {
            cursor.close()
            db.close()
        }

        return cards
    }

    fun getAllDecks(userId: Long): MutableList<Deck> {
        val deckList = mutableListOf<Deck>()
        val db = this.readableDatabase
        val columns = arrayOf(COL_DECK_ID, COL_DECK_NAME, COL_DECK_CARD_COUNT)

        val selection = "$COL_USER_ID_FK = ?"
        val selectionArgs = arrayOf(userId.toString())

        val cursor = db.query(
            TABLE_DECK,
            columns,
            selection,
            selectionArgs,
            null,
            null,
            null
        )

        if (cursor.moveToFirst()) {
            do {
                val deckId = cursor.getLong(cursor.getColumnIndexOrThrow(COL_DECK_ID)).toInt()
                val deckName = cursor.getString(cursor.getColumnIndexOrThrow(COL_DECK_NAME))
                val deckCardCount = cursor.getInt(cursor.getColumnIndexOrThrow(COL_DECK_CARD_COUNT))

                val deck = Deck(deckId, deckName, deckCardCount) // Assuming 0 for card count, update accordingly
                deckList.add(deck)
            } while (cursor.moveToNext())
        }

        cursor.close()
        db.close()

        return deckList
    }

    fun removeDeck(userId: Long, deckId: Int) {
        var cards = getAllCards(userId.toInt(), deckId)
        for (Card in cards) {
            deleteCard(Card, userId.toInt(), deckId)
        }

        val db = this.writableDatabase
        val whereClause = "$COL_DECK_ID = ? AND $COL_USER_ID_FK = ?"
        val whereArgs = arrayOf(deckId.toString(), userId.toString())
        db.delete(TABLE_DECK, whereClause, whereArgs)
        db.close()
    }

    fun renameDeck(newDeckName: String, userId: Long, deckId: Int) {
        writableDatabase.use { db ->
            val values = ContentValues().apply {
                put(COL_DECK_NAME, newDeckName)
            }

            val whereClause = "$COL_DECK_ID = ? AND $COL_USER_ID_FK = ?"
            val whereArgs = arrayOf(deckId.toString(), userId.toString())

            db.update(TABLE_DECK, values, whereClause, whereArgs)
        }
    }

    fun addDeck(deckName: String, userId: Long) {
        val db = this.writableDatabase
        val values = ContentValues().apply {
            put(COL_DECK_NAME, deckName)
            put(COL_USER_ID_FK, userId)
        }
        db.insert(TABLE_DECK, null, values)
        db.close()
    }

    fun incrementCardCount(deckId: Int) {
        // Fetch the current cardCount value from the database
        val currentCardCount = getCardCount(deckId)
        val db = this.writableDatabase
        val contentValues = ContentValues()

        if (currentCardCount != -1) {
            // Increment the cardCount value
            contentValues.put(COL_DECK_CARD_COUNT, currentCardCount + 1)

            // Update the database with the new cardCount value
            db.update(TABLE_DECK, contentValues, "$COL_DECK_ID=?", arrayOf(deckId.toString()))
        }

        db.close()
    }

    @SuppressLint("Range")
    fun getCardCount(deckId: Int): Int {
        val db = this.readableDatabase
        var cardCount = -1

        // Define the column name
        val column = COL_DECK_CARD_COUNT

        // Use try-catch to handle potential exceptions
        try {
            // Fetch the current cardCount value from the database
            val query = "SELECT $column FROM $TABLE_DECK WHERE $COL_DECK_ID = $deckId"
            val cursor = db.rawQuery(query, null)

            // Check if the cursor has results
            if (cursor.moveToFirst()) {
                // Retrieve the card count from the cursor
                cardCount = cursor.getInt(cursor.getColumnIndex(column))
            }
        } catch (e: Exception) {
            // Handle exceptions, such as SQLiteException
            e.printStackTrace()
        } finally {
            // Close the cursor
            db.close()
        }

        return cardCount
    }

    fun addCard(userId: Int, deckId: Int, topCard: String, bottomCard: String, pathTop: String, pathBottom: String) {
        val db = this.writableDatabase
        val contentValues = ContentValues()

        contentValues.put(COL_CARD_TOP, topCard)
        contentValues.put(COL_CARD_BOTTOM, bottomCard)
        contentValues.put(COL_CARD_TOP_FILE_PATH, pathTop)
        contentValues.put(COL_CARD_BOTTOM_FILE_PATH, pathBottom)
        contentValues.put(COL_CARD_COEFFICIENT, 2.0)
        contentValues.put(COL_CARD_TIMEOUT, 0)
        contentValues.put(COL_DECK_ID_FK, deckId)
        contentValues.put(COL_CARD_USER_ID_FK, userId)


        // Insert into database
        val result = db.insert(TABLE_CARD, null, contentValues)

        if (result == -1L) {
            // Insertion failed, handle the error (e.g., log, show a message)
            Log.e("Database", "Error inserting card into the database")
        } else {
            // Insertion successful
            Log.d("Database", "Card inserted successfully")
        }

        db.close()
    }

    fun updateCoefficient(userId: Int, deckId: Int, cardId: Int, feedback: String) {
        val card = getCardContents(userId, deckId, cardId)

        when (feedback) {
            "Good" -> card.coefficient *= 3.0
            "Avg"  -> card.coefficient /= 2.0
            "Bad"  -> card.coefficient *= 0.1667
        }

        val contentValues = ContentValues()
        contentValues.put(COL_CARD_COEFFICIENT, card.coefficient)

        val whereClause = "$COL_CARD_ID = ? AND $COL_DECK_ID_FK = ? AND $COL_CARD_USER_ID_FK = ?"
        val whereArgs = arrayOf(cardId.toString(), deckId.toString(), userId.toString())

        writableDatabase.update(TABLE_CARD, contentValues, whereClause, whereArgs)
    }



    @SuppressLint("Range")
    // gets a card of the deck that has timeout on zero
    fun getCardContents(userId: Int, deckId: Int): Card {
        val db = this.readableDatabase
        var card = Card()

        val query = "SELECT * FROM $TABLE_CARD WHERE $COL_CARD_USER_ID_FK = ? AND $COL_DECK_ID_FK = ? AND $COL_CARD_TIMEOUT = 0"
        val selectionArgs = arrayOf(userId.toString(), deckId.toString())

        try {
            db.rawQuery(query, selectionArgs).use { cursor ->
                if (cursor.moveToFirst()) {
                    card = Card(
                        userId = userId,
                        deckId = deckId,
                        id = cursor.getInt(cursor.getColumnIndex(COL_CARD_ID)),
                        cardTop = cursor.getString(cursor.getColumnIndex(COL_CARD_TOP)),
                        cardBottom = cursor.getString(cursor.getColumnIndex(COL_CARD_BOTTOM)),
                        cardTopFilePath = cursor.getString(cursor.getColumnIndex(COL_CARD_TOP_FILE_PATH)),
                        cardBottomFilePath = cursor.getString(cursor.getColumnIndex(COL_CARD_BOTTOM_FILE_PATH)),
                        coefficient = cursor.getDouble(cursor.getColumnIndex(COL_CARD_COEFFICIENT)),
                        timeout = cursor.getLong(cursor.getColumnIndex(COL_CARD_TIMEOUT))
                    )
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            db.close()
        }
        return card
    }

    // add timeout after interacting with a card
    fun updateTimeout(userId: Int, deckId: Int, cardId: Int, feedback: String){
        val card = getCardContents(userId, deckId, cardId)
        val timeOut = when (feedback) {
            "Good" -> calculateTimeout(card.coefficient, 1.0)
            "Avg"  -> calculateTimeout(card.coefficient, 0.5)
            "Bad"  -> calculateTimeout(card.coefficient, 0.2)
            else -> 0
        }

        val contentValues = ContentValues()
        contentValues.put(COL_CARD_TIMEOUT, timeOut)

        val whereClause = "$COL_CARD_ID = ? AND $COL_DECK_ID_FK = ? AND $COL_CARD_USER_ID_FK = ?"
        val whereArgs = arrayOf(cardId.toString(), deckId.toString(), userId.toString())

        writableDatabase.update(TABLE_CARD, contentValues, whereClause, whereArgs)
    }

    private fun calculateTimeout(coefficient: Double, factor: Double): Long {
        val secondsInDay = 24 * 60 * 60
        return (coefficient * factor * secondsInDay).toLong()
    }

    @SuppressLint("Range")
    fun getCardContents(userId: Int, deckId: Int, cardId: Int): Card {
        val db = this.readableDatabase
        var card = Card()

        val query = "SELECT * FROM $TABLE_CARD WHERE $COL_CARD_USER_ID_FK = ? AND $COL_DECK_ID_FK = ? AND $COL_CARD_ID = ?"
        val selectionArgs = arrayOf(userId.toString(), deckId.toString(), cardId.toString())

        try {
            db.rawQuery(query, selectionArgs).use { cursor ->
                if (cursor.moveToFirst()) {
                    card = Card(
                        userId = userId,
                        deckId = deckId,
                        id = cursor.getInt(cursor.getColumnIndex(COL_CARD_ID)),
                        cardTop = cursor.getString(cursor.getColumnIndex(COL_CARD_TOP)),
                        cardBottom = cursor.getString(cursor.getColumnIndex(COL_CARD_BOTTOM)),
                        cardTopFilePath = cursor.getString(cursor.getColumnIndex(COL_CARD_TOP_FILE_PATH)),
                        cardBottomFilePath = cursor.getString(cursor.getColumnIndex(COL_CARD_BOTTOM_FILE_PATH)),
                        coefficient = cursor.getDouble(cursor.getColumnIndex(COL_CARD_COEFFICIENT)),
                        timeout = cursor.getLong(cursor.getColumnIndex(COL_CARD_TIMEOUT))
                    )
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            db.close()
        }
        return card
    }



    companion object{
        private const val DATABASE_VERSION = 2
        private const val DATABASE_NAME = "user.db"

        private const val TABLE_USER = "tbl_user"
        private const val COL_USER_ID = "user_id"
        private const val COL_USER_NAME = "user_name"
        private const val COL_USER_EMAIL = "user_email"
        private const val COL_USER_PASSWORD = "user_password"

        private const val TABLE_DECK = "tbl_deck"
        private const val COL_DECK_ID = "deck_id"
        private const val COL_DECK_NAME = "deck_name"
        private const val COL_USER_ID_FK = "deck_user_id_fk"
        private const val COL_DECK_CARD_COUNT = "deck_card_count"

        private const val TABLE_CARD = "tbl_card"
        private const val COL_CARD_ID = "card_id"
        private const val COL_CARD_TOP = "card_top"
        private const val COL_CARD_BOTTOM = "card_bottom"
        private const val COL_CARD_TOP_FILE_PATH = "card_top_file_path"
        private const val COL_CARD_BOTTOM_FILE_PATH = "card_bottom_file_path"
        private const val COL_CARD_COEFFICIENT = "card_coefficient"
        private const val COL_CARD_TIMEOUT = "card_time_out"
        private const val COL_CARD_LAST_UPDATE_TIME = "card_last_update_time"
        private const val COL_DECK_ID_FK = "card_deck_id_fk"
        private const val COL_CARD_USER_ID_FK = "card_user_id_fk"
    }
}
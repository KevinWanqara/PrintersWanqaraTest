package com.example.printerswanqaratest.data

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class TokenDatabaseHelper(context: Context) : SQLiteOpenHelper(context, DB_NAME, null, DB_VERSION) {
    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("""
            CREATE TABLE $TABLE_TOKEN (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                token TEXT NOT NULL
            )
        """)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_TOKEN")
        onCreate(db)
    }

    fun saveToken(token: String) {
        writableDatabase.execSQL("DELETE FROM $TABLE_TOKEN")
        writableDatabase.execSQL("INSERT INTO $TABLE_TOKEN (token) VALUES (?)", arrayOf(token))
    }

    fun getToken(): String? {
        val cursor = readableDatabase.rawQuery("SELECT token FROM $TABLE_TOKEN LIMIT 1", null)
        return if (cursor.moveToFirst()) {
            val token = cursor.getString(0)
            cursor.close()
            token
        } else {
            cursor.close()
            null
        }
    }

    companion object {
        private const val DB_NAME = "auth_token.db"
        private const val DB_VERSION = 1
        const val TABLE_TOKEN = "token_table"
    }
}


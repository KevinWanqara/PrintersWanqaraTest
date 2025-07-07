package com.example.printerswanqaratest.data.database

import android.content.Context
import androidx.room.Room

object DatabaseProvider {
    @Volatile
    private var INSTANCE: DBConnection? = null

    fun getDatabase(context: Context): DBConnection {
        return INSTANCE ?: synchronized(this) {
            val instance = Room.databaseBuilder(
                context.applicationContext,
                DBConnection::class.java,
                "printers_db"
            ).build()
            INSTANCE = instance
            instance
        }
    }
}

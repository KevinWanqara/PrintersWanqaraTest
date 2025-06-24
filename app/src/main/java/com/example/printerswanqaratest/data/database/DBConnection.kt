package com.example.printerswanqaratest.data.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.printerswanqaratest.data.database.dao.PrinterDAO
import com.example.printerswanqaratest.data.database.entities.PrintersEntity


@Database(entities = [PrintersEntity::class], version = 4)
abstract class DBConnection : RoomDatabase() {
    abstract fun printersDAO(): PrinterDAO

}
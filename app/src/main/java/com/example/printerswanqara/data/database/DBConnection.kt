package com.example.printerswanqara.data.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.printerswanqara.data.database.dao.PrinterDAO
import com.example.printerswanqara.data.database.entities.PrintersEntity


@Database(entities = [PrintersEntity::class], version = 4)
abstract class DBConnection : RoomDatabase() {
    abstract fun printersDAO(): PrinterDAO

}
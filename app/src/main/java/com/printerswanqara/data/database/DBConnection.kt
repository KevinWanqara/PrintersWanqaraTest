package com.printerswanqara.data.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.printerswanqara.data.database.dao.PrinterDAO
import com.printerswanqara.data.database.entities.PrintersEntity


@Database(entities = [PrintersEntity::class], version = 4)
abstract class DBConnection : RoomDatabase() {
    abstract fun printersDAO(): PrinterDAO

}
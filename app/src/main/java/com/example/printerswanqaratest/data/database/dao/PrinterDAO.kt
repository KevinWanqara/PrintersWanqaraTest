package com.example.printerswanqaratest.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.printerswanqaratest.data.database.entities.PrintersEntity

@Dao
interface PrinterDAO {

    @Query("SELECT * FROM PrintersEntity")
    fun getAll(): List<PrintersEntity>

    @Query("SELECT * FROM PrintersEntity WHERE document_type IN (:documentType)")
    fun getPrinterByDocumentType(documentType:String): PrintersEntity?

    @Query("SELECT * FROM PrintersEntity WHERE id IN (:userIds)")
    fun loadAllByIds(userIds: IntArray): List<PrintersEntity>

    @Query("SELECT * FROM PrintersEntity WHERE id = :printerId")
    fun findById(printerId:String): PrintersEntity

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertAll(vararg users: PrintersEntity)

    @Query("DELETE FROM PrintersEntity WHERE id = (:printer)")
    fun delete(printer: Int)

    @Update
    fun update(printer: PrintersEntity)
}
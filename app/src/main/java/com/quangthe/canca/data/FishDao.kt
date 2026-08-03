package com.quangthe.canca.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface FishDao {
    // Tickets
    @Query("SELECT * FROM fish_tickets WHERE isDeleted = 0 ORDER BY createdAt DESC")
    fun getActiveTickets(): Flow<List<FishTicket>>

    @Query("SELECT * FROM fish_tickets WHERE isDeleted = 1 ORDER BY createdAt DESC")
    fun getDeletedTickets(): Flow<List<FishTicket>>

    @Query("SELECT * FROM fish_tickets WHERE id = :id")
    suspend fun getTicketById(id: Int): FishTicket?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTicket(ticket: FishTicket): Long

    @Update
    suspend fun updateTicket(ticket: FishTicket)

    @Query("UPDATE fish_tickets SET isDeleted = 1 WHERE id = :id")
    suspend fun softDeleteTicket(id: Int)

    @Query("UPDATE fish_tickets SET isDeleted = 0 WHERE id = :id")
    suspend fun restoreTicket(id: Int)

    @Query("DELETE FROM fish_tickets WHERE id = :id")
    suspend fun permanentDeleteTicket(id: Int)

    // Fish Sheets
    @Query("SELECT * FROM fish_sheets WHERE ticketId = :ticketId ORDER BY sheetIndex ASC")
    fun getSheetsForTicket(ticketId: Int): Flow<List<FishSheet>>

    @Query("DELETE FROM fish_sheets WHERE ticketId = :ticketId")
    suspend fun deleteSheetsForTicket(ticketId: Int)

    @Query("DELETE FROM fish_cells WHERE sheetId IN (SELECT id FROM fish_sheets WHERE ticketId = :ticketId)")
    suspend fun deleteCellsForTicket(ticketId: Int)

    @Query("SELECT * FROM fish_sheets WHERE ticketId = :ticketId AND sheetIndex = :sheetIndex")
    suspend fun getSheetByIndex(ticketId: Int, sheetIndex: Int): FishSheet?

    @Transaction
    suspend fun deleteSheetsAndCellsForTicket(ticketId: Int) {
        deleteCellsForTicket(ticketId)
        deleteSheetsForTicket(ticketId)
    }

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSheet(sheet: FishSheet): Long

    // Fish Cells
    @Query("SELECT * FROM fish_cells WHERE sheetId = :sheetId")
    fun getCellsForSheet(sheetId: Int): Flow<List<FishCell>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCell(cell: FishCell)

    @Query("DELETE FROM fish_cells WHERE sheetId = :sheetId AND rowIndex = :rowIndex AND colIndex = :colIndex")
    suspend fun deleteCell(sheetId: Int, rowIndex: Int, colIndex: Int)

    // App Settings
    @Query("SELECT * FROM app_settings WHERE id = 1")
    fun getAppSettings(): Flow<AppSettings?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun updateAppSettings(settings: AppSettings)

    // Backup/Restore
    @Query("SELECT * FROM fish_sheets")
    suspend fun getAllSheets(): List<FishSheet>

    @Query("SELECT * FROM fish_cells")
    suspend fun getAllCells(): List<FishCell>

    @Query("SELECT * FROM fish_tickets")
    suspend fun getAllTickets(): List<FishTicket>

    @Transaction
    suspend fun clearAndRestore(tickets: List<FishTicket>, sheets: List<FishSheet>, cells: List<FishCell>) {
        deleteAllTickets()
        deleteAllSheets()
        deleteAllCells()
        insertTickets(tickets)
        insertSheets(sheets)
        insertCells(cells)
    }

    @Query("DELETE FROM fish_tickets")
    suspend fun deleteAllTickets()

    @Query("DELETE FROM fish_sheets")
    suspend fun deleteAllSheets()

    @Query("DELETE FROM fish_cells")
    suspend fun deleteAllCells()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTickets(tickets: List<FishTicket>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSheets(sheets: List<FishSheet>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCells(cells: List<FishCell>)
}

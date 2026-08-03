package com.quangthe.canca.data

import kotlinx.coroutines.flow.Flow

class FishRepository(private val fishDao: FishDao) {
    val activeTickets: Flow<List<FishTicket>> = fishDao.getActiveTickets()
    val deletedTickets: Flow<List<FishTicket>> = fishDao.getDeletedTickets()
    val appSettings: Flow<AppSettings?> = fishDao.getAppSettings()

    suspend fun insertTicket(ticket: FishTicket): Long = fishDao.insertTicket(ticket)
    suspend fun updateTicket(ticket: FishTicket) = fishDao.updateTicket(ticket)
    suspend fun softDeleteTicket(id: Int) = fishDao.softDeleteTicket(id)
    suspend fun restoreTicket(id: Int) = fishDao.restoreTicket(id)
    suspend fun permanentDeleteTicket(id: Int) = fishDao.permanentDeleteTicket(id)
    suspend fun getTicketById(id: Int) = fishDao.getTicketById(id)

    // Sheets
    fun getSheetsForTicket(ticketId: Int) = fishDao.getSheetsForTicket(ticketId)
    suspend fun getSheetByIndex(ticketId: Int, sheetIndex: Int) = fishDao.getSheetByIndex(ticketId, sheetIndex)
    suspend fun insertSheet(sheet: FishSheet) = fishDao.insertSheet(sheet)
    suspend fun deleteSheetsForTicket(ticketId: Int) = fishDao.deleteSheetsAndCellsForTicket(ticketId)

    // Cells
    fun getCellsForSheet(sheetId: Int) = fishDao.getCellsForSheet(sheetId)
    suspend fun insertCell(cell: FishCell) = fishDao.insertCell(cell)

    suspend fun updateAppSettings(settings: AppSettings) = fishDao.updateAppSettings(settings)

    // Backup/Restore
    suspend fun getAllTickets(): List<FishTicket> = fishDao.getAllTickets()
    suspend fun getAllSheets(): List<FishSheet> = fishDao.getAllSheets()
    suspend fun getAllCells(): List<FishCell> = fishDao.getAllCells()
    suspend fun clearAndRestore(tickets: List<FishTicket>, sheets: List<FishSheet>, cells: List<FishCell>) = 
        fishDao.clearAndRestore(tickets, sheets, cells)
}

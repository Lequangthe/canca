package com.quangthe.canca.utils

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import com.quangthe.canca.data.FishCell
import com.quangthe.canca.data.FishSheet
import com.quangthe.canca.data.FishTicket
import com.google.gson.Gson
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

object ExportUtils {

    fun exportToExcel(
        context: Context,
        ticket: FishTicket,
        sheets: List<FishSheet>,
        allCells: List<FishCell>,
        totalWeight: Double,
        totalPrice: Double
    ) {
        val workbook = XSSFWorkbook()
        val gson = Gson()

        sheets.forEach { sheet ->
            val sheetName = "Bảng ${sheet.sheetIndex + 1}"
            val poiSheet = workbook.createSheet(sheetName)
            val cells = allCells.filter { it.sheetId == sheet.id }
            
            val colTitles: List<String> = try {
                gson.fromJson(sheet.colTitles, Array<String>::class.java).toList()
            } catch (e: Exception) {
                (1..sheet.numCols).map { "C$it" }
            }

            // Header Row
            val headerRow = poiSheet.createRow(0)
            colTitles.forEachIndexed { index, title ->
                headerRow.createCell(index).setCellValue(title)
            }

            // Data Rows
            for (r in 0 until sheet.numRows) {
                val row = poiSheet.createRow(r + 1)
                for (c in 0 until sheet.numCols) {
                    val cellValue = cells.find { it.rowIndex == r && it.colIndex == c }?.value ?: 0.0
                    if (cellValue != 0.0) {
                        row.createCell(c).setCellValue(cellValue)
                    }
                }
            }

            // Summary Row for each sheet
            val lastRowIndex = sheet.numRows + 1
            val summaryRow = poiSheet.createRow(lastRowIndex)
            summaryRow.createCell(0).setCellValue("TỔNG BẢNG")
            val sheetSum = cells.sumOf { it.value }
            summaryRow.createCell(1).setCellValue(sheetSum)
        }

        // Summary Sheet
        val summarySheet = workbook.createSheet("TỔNG HỢP")
        var rowIdx = 0
        summarySheet.createRow(rowIdx++).apply {
            createCell(0).setCellValue("Tên phiếu:")
            createCell(1).setCellValue(ticket.ticketName)
        }
        summarySheet.createRow(rowIdx++).apply {
            createCell(0).setCellValue("Ngày tạo:")
            createCell(1).setCellValue(SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date(ticket.createdAt)))
        }
        summarySheet.createRow(rowIdx++).apply {
            createCell(0).setCellValue("Tổng khối lượng (kg):")
            createCell(1).setCellValue(totalWeight)
        }
        summarySheet.createRow(rowIdx++).apply {
            createCell(0).setCellValue("Trừ bì thùng/giỏ:")
            val numBags = allCells.count { it.value > 0 }
            createCell(1).setCellValue("${ticket.tarePerBag} kg / mã (Tổng: ${numBags * ticket.tarePerBag} kg)")
        }
        summarySheet.createRow(rowIdx++).apply {
            createCell(0).setCellValue("Khối lượng sau khi trừ bì & tạp chất:")
            val numBags = allCells.count { it.value > 0 }
            val totalTare = numBags * ticket.tarePerBag
            val totalImpurity = (totalWeight / 1000.0) * ticket.impurityPerTon
            createCell(1).setCellValue(totalWeight - totalTare - totalImpurity)
        }
        summarySheet.createRow(rowIdx++).apply {
            createCell(0).setCellValue("Khấu trừ phao/nước:")
            val weightAfterTare = totalWeight - (allCells.count { it.value > 0 } * ticket.tarePerBag) - ((totalWeight / 1000.0) * ticket.impurityPerTon)
            val deduction = if (ticket.deductionType == 0) {
                "${ticket.deductionValue}% (Tổng: ${weightAfterTare * (ticket.deductionValue / 100.0)} kg)"
            } else {
                "${ticket.deductionValue} kg"
            }
            createCell(1).setCellValue(deduction)
        }
        summarySheet.createRow(rowIdx++).apply {
            createCell(0).setCellValue("Đơn giá:")
            createCell(1).setCellValue(ticket.unitPrice.toDouble())
        }
        summarySheet.createRow(rowIdx++).apply {
            createCell(0).setCellValue("Thành tiền:")
            createCell(1).setCellValue(totalPrice)
        }
        summarySheet.createRow(rowIdx++).apply {
            createCell(0).setCellValue("Tiền cọc/ứng:")
            createCell(1).setCellValue(ticket.deposit.toDouble())
        }
        summarySheet.createRow(rowIdx++).apply {
            createCell(0).setCellValue("Còn lại:")
            createCell(1).setCellValue(totalPrice - ticket.deposit)
        }

        val exportDir = File(context.cacheDir, "exports").also { it.mkdirs() }
        val fileName = "Phieu_${ticket.ticketName.replace(" ", "_")}_${System.currentTimeMillis()}.xlsx"
        val file = File(exportDir, fileName)
        
        try {
            FileOutputStream(file).use { workbook.write(it) }
            workbook.close()
            shareFile(context, file, "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun exportDatabaseToCsv(
        context: Context,
        tickets: List<FishTicket>,
        sheets: List<FishSheet>,
        cells: List<FishCell>
    ) {
        val sb = StringBuilder()
        
        // Export Tickets
        sb.append("--- TICKETS ---\n")
        sb.append("id,ticketName,tarePerBag,impurityPerTon,unitPrice,deposit,isDeleted,deductionType,deductionValue,createdAt\n")
        tickets.forEach { t ->
            sb.append("${t.id},\"${t.ticketName}\",${t.tarePerBag},${t.impurityPerTon},${t.unitPrice},${t.deposit},${t.isDeleted},${t.deductionType},${t.deductionValue},${t.createdAt}\n")
        }

        // Export Sheets
        sb.append("\n--- SHEETS ---\n")
        sb.append("id,ticketId,sheetIndex,numRows,numCols,colTitles\n")
        sheets.forEach { s ->
            sb.append("${s.id},${s.ticketId},${s.sheetIndex},${s.numRows},${s.numCols},\"${s.colTitles.replace("\"", "\"\"")}\"\n")
        }

        // Export Cells
        sb.append("\n--- CELLS ---\n")
        sb.append("sheetId,rowIndex,colIndex,value\n")
        cells.forEach { c ->
            sb.append("${c.sheetId},${c.rowIndex},${c.colIndex},${c.value}\n")
        }

        val exportDir = File(context.cacheDir, "exports").also { it.mkdirs() }
        val fileName = "Backup_CanCa_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())}.csv"
        val file = File(exportDir, fileName)
        
        try {
            file.writeText(sb.toString())
            shareFile(context, file, "text/csv")
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun exportMultipleToExcel(
        context: Context,
        data: List<Triple<FishTicket, List<FishSheet>, List<FishCell>>>
    ) {
        val workbook = XSSFWorkbook()
        val gson = Gson()

        data.forEach { (ticket, sheets, allCells) ->
            // For multiple tickets, we create one sheet per ticket or group sheets
            // Let's create a main summary sheet for each ticket if multiple
            val ticketPrefix = ticket.ticketName.take(15).replace(" ", "_")
            
            sheets.forEach { sheet ->
                val sheetName = "${ticketPrefix}_B${sheet.sheetIndex + 1}"
                val poiSheet = workbook.createSheet(sheetName)
                val cells = allCells.filter { it.sheetId == sheet.id }
                
                val colTitles: List<String> = try {
                    gson.fromJson(sheet.colTitles, Array<String>::class.java).toList()
                } catch (e: Exception) {
                    (1..sheet.numCols).map { "C$it" }
                }

                val headerRow = poiSheet.createRow(0)
                colTitles.forEachIndexed { index, title ->
                    headerRow.createCell(index).setCellValue(title)
                }

                for (r in 0 until sheet.numRows) {
                    val row = poiSheet.createRow(r + 1)
                    for (c in 0 until sheet.numCols) {
                        val cellValue = cells.find { it.rowIndex == r && it.colIndex == c }?.value ?: 0.0
                        if (cellValue != 0.0) {
                            row.createCell(c).setCellValue(cellValue)
                        }
                    }
                }
            }
        }

        // Global Summary Sheet
        val summarySheet = workbook.createSheet("TỔNG HỢP CHUNG")
        var rowIdx = 0
        val header = summarySheet.createRow(rowIdx++)
        header.createCell(0).setCellValue("Tên phiếu")
        header.createCell(1).setCellValue("Tổng khối lượng (kg)")
        header.createCell(2).setCellValue("Thành tiền (VNĐ)")
        header.createCell(3).setCellValue("Tiền cọc/ứng")
        header.createCell(4).setCellValue("Còn lại")

        var grandTotalWeight = 0.0
        var grandTotalPrice = 0.0
        var grandTotalBalance = 0.0

        data.forEach { (ticket, sheets, allCells) ->
            val totalWeight = allCells.sumOf { it.value }
            val totalBags = allCells.count { it.value > 0 }
            val totalTare = totalBags * ticket.tarePerBag
            val totalImpurity = (totalWeight / 1000.0) * ticket.impurityPerTon
            
            val weightAfterTare = totalWeight - totalTare - totalImpurity
            
            val deductionAmount = if (ticket.deductionType == 0) {
                weightAfterTare * (ticket.deductionValue / 100.0)
            } else {
                ticket.deductionValue
            }
            
            val remainingWeight = weightAfterTare - deductionAmount
            val totalPrice = (remainingWeight * ticket.unitPrice)
            val balance = totalPrice - ticket.deposit

            val row = summarySheet.createRow(rowIdx++)
            row.createCell(0).setCellValue(ticket.ticketName)
            row.createCell(1).setCellValue(remainingWeight)
            row.createCell(2).setCellValue(totalPrice.toDouble())
            row.createCell(3).setCellValue(ticket.deposit.toDouble())
            row.createCell(4).setCellValue(balance.toDouble())

            grandTotalWeight += remainingWeight
            grandTotalPrice += totalPrice.toDouble()
            grandTotalBalance += balance.toDouble()
        }

        summarySheet.createRow(rowIdx++).createCell(0).setCellValue("")
        val footer = summarySheet.createRow(rowIdx++)
        footer.createCell(0).setCellValue("TỔNG CỘNG")
        footer.createCell(1).setCellValue(grandTotalWeight)
        footer.createCell(2).setCellValue(grandTotalPrice)
        footer.createCell(4).setCellValue(grandTotalBalance)

        val exportDir = File(context.cacheDir, "exports").also { it.mkdirs() }
        val fileName = "Tong_Hop_Can_Ca_${System.currentTimeMillis()}.xlsx"
        val file = File(exportDir, fileName)
        
        try {
            FileOutputStream(file).use { workbook.write(it) }
            workbook.close()
            shareFile(context, file, "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun shareFile(context: Context, file: File, mimeType: String) {
        val uri: Uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Chia sẻ qua"))
    }
}

package com.quangthe.canca.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.quangthe.canca.data.FishCell
import com.quangthe.canca.data.FishTicket
import com.quangthe.canca.ui.theme.*
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun LongTicketShareView(
    ticket: FishTicket,
    allCells: List<FishCell>,
    totalWeight: Double,
    totalPrice: Double
) {
    val df = DecimalFormat("#,###.#")
    val currencyFmt = DecimalFormat("#,###")
    val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())

    val numBags = allCells.count { it.value > 0 }
    val totalTare = numBags * ticket.tarePerBag
    val totalImpurity = (totalWeight / 1000.0) * ticket.impurityPerTon
    val weightAfterTare = totalWeight - totalTare - totalImpurity
    
    val deductionAmount = if (ticket.deductionType == 0) {
        weightAfterTare * (ticket.deductionValue / 100.0)
    } else {
        ticket.deductionValue
    }
    
    val remainingWeight = weightAfterTare - deductionAmount
    val balance = totalPrice - ticket.deposit

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
            .padding(24.dp)
    ) {
        // Header
        Text(
            text = "PHIẾU CÂN CÁ",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Black,
            color = DetailPrimaryGreen,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = ticket.ticketName,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = Color.Black,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center
        )

        Text(
            text = "Ngày tạo: ${sdf.format(Date(ticket.createdAt))}",
            style = MaterialTheme.typography.bodyMedium,
            color = Color.Gray,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Summary Table
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, DetailBorderColor)
                .background(DashboardBlue.copy(alpha = 0.3f))
        ) {
            SummaryRow("Tổng khối lượng (Gross)", "${df.format(totalWeight)} kg", isBold = true)
            SummaryRow("Tổng số mã", "$numBags mã")
            SummaryRow("Trừ bì (${ticket.tarePerBag} kg/mã)", "${df.format(totalTare)} kg")
            
            val deductionLabel = if (ticket.deductionType == 0) "${ticket.deductionValue}%" else "${ticket.deductionValue} kg"
            SummaryRow("Khấu trừ phao/nước ($deductionLabel)", "${df.format(deductionAmount)} kg")
            
            Divider(color = DetailBorderColor)
            
            SummaryRow("Khối lượng còn lại (Net)", "${df.format(remainingWeight)} kg", isBold = true, textColor = DetailPrimaryGreen)
            SummaryRow("Đơn giá", "${currencyFmt.format(ticket.unitPrice)} đ/kg")
            
            Divider(color = DetailBorderColor)
            
            SummaryRow("THÀNH TIỀN", "${currencyFmt.format(totalPrice)} đ", isBold = true, backgroundColor = SummaryGold.copy(alpha = 0.2f))
            SummaryRow("Tiền cọc, tiền ứng", "${currencyFmt.format(ticket.deposit)} đ")
            
            Divider(color = DetailBorderColor)
            
            SummaryRow("CÒN PHẢI TRẢ", "${currencyFmt.format(balance)} đ", isBold = true, textColor = BalanceRed, backgroundColor = SummaryGold.copy(alpha = 0.4f))
        }

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = "CHI TIẾT CÁC MÃ CÂN",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.ExtraBold,
            color = DetailPrimaryGreen
        )
        
        Spacer(modifier = Modifier.height(8.dp))

        // Grid of cells
        val activeCells = allCells.filter { it.value > 0 }.sortedWith(compareBy({ it.sheetId }, { it.rowIndex }, { it.colIndex }))
        
        val chunkSize = 5
        activeCells.chunked(chunkSize).forEach { rowCells ->
            Row(modifier = Modifier.fillMaxWidth()) {
                rowCells.forEach { cell ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .border(0.5.dp, Color.LightGray)
                            .padding(vertical = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = df.format(cell.value),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color.Black
                        )
                    }
                }
                repeat(chunkSize - rowCells.size) {
                    Box(modifier = Modifier.weight(1f))
                }
            }
        }

        Spacer(modifier = Modifier.height(40.dp))
        
        Text(
            text = "Cảm ơn quý khách!",
            style = MaterialTheme.typography.bodySmall,
            color = Color.Gray,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun SummaryRow(
    label: String,
    value: String,
    isBold: Boolean = false,
    textColor: Color = Color.Black,
    backgroundColor: Color = Color.Transparent
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(backgroundColor)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (isBold) FontWeight.Bold else FontWeight.Normal,
            color = Color.DarkGray
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = if (isBold) FontWeight.ExtraBold else FontWeight.Bold,
            color = textColor
        )
    }
}

@Composable
fun Divider(color: Color) {
    HorizontalDivider(modifier = Modifier.fillMaxWidth(), thickness = 1.dp, color = color)
}

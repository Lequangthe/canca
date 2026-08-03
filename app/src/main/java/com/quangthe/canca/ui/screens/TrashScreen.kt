package com.quangthe.canca.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.quangthe.canca.data.FishTicket
import com.quangthe.canca.ui.theme.*
import com.quangthe.canca.viewmodel.FishViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrashScreen(
    viewModel: FishViewModel,
    onBack: () -> Unit
) {
    val deletedTickets by viewModel.deletedTickets.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Thùng rác") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Quay lại")
                    }
                }
            )
        }
    ) { padding ->
        if (deletedTickets.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Thùng rác trống")
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                items(deletedTickets) { ticket ->
                    TrashTicketCard(
                        ticket = ticket,
                        viewModel = viewModel
                    )
                }
            }
        }
    }
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
private fun TrashTicketCard(
    ticket: FishTicket,
    viewModel: FishViewModel
) {
    var showDeleteConfirm by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp, horizontal = 16.dp),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(TicketGreen)
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = ticket.ticketName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date(ticket.createdAt)),
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White
                        )
                    }
                    Row {
                        IconButton(onClick = { viewModel.restoreTicket(ticket.id) }) {
                            Icon(
                                imageVector = Icons.Default.Restore,
                                contentDescription = "Khôi phục",
                                tint = Color.White
                            )
                        }
                        IconButton(onClick = { showDeleteConfirm = true }) {
                            Icon(
                                imageVector = Icons.Default.DeleteForever,
                                contentDescription = "Xóa vĩnh viễn",
                                tint = Color.White
                            )
                        }
                    }
                }
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(AppBackground)
                    .padding(16.dp)
            ) {
                Text(
                    text = "Phiếu đã bị xoá",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.DarkGray
                )
            }
        }

        if (showDeleteConfirm) {
            AlertDialog(
                onDismissRequest = { showDeleteConfirm = false },
                title = { Text("Xác nhận xoá vĩnh viễn") },
                text = {
                    Text("Bạn có chắc chắn muốn xoá vĩnh viễn phiếu '${ticket.ticketName}'? Dữ liệu sẽ không thể khôi phục.")
                },
                confirmButton = {
                    TextButton(onClick = {
                        viewModel.permanentDeleteTicket(ticket.id)
                        showDeleteConfirm = false
                    }) {
                        Text("XOÁ VĨNH VIỄN", color = Color.Red, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteConfirm = false }) {
                        Text("HUỶ")
                    }
                }
            )
        }
    }
}

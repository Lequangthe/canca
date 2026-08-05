package com.quangthe.canca.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.quangthe.canca.data.FishTicket
import com.quangthe.canca.ui.theme.*
import com.quangthe.canca.viewmodel.FishMultiTicketTotals
import com.quangthe.canca.viewmodel.FishViewModel
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    viewModel: FishViewModel,
    onNavigateToTrash: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onTicketClick: (Int) -> Unit
) {
    val activeTickets by viewModel.activeTickets.collectAsState()
    var searchQuery by remember { mutableStateOf("") }
    var debouncedQuery by remember { mutableStateOf("") }
    val coroutineScope = rememberCoroutineScope()
    var searchJob by remember { mutableStateOf<Job?>(null) }
    val filteredTickets = activeTickets.filter { it.ticketName.contains(debouncedQuery, ignoreCase = true) }
    
    var showAddDialog by remember { mutableStateOf(false) }
    var newTicketName by remember { mutableStateOf("") }

    val selectedTicketIds = remember { mutableStateListOf<Int>() }
    var showMultiSelectMode by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = AppBackground,
        topBar = {
            Column(modifier = Modifier.background(HeaderGreen)) {
                CenterAlignedTopAppBar(
                    title = { 
                        Text(
                            if (showMultiSelectMode) "Đã chọn ${selectedTicketIds.size} phiếu" else "Cân cá by Quang Thế",
                            fontWeight = FontWeight.ExtraBold, 
                            color = Color.White,
                            fontSize = 18.sp
                        ) 
                    },
                    actions = {
                        if (showMultiSelectMode) {
                            IconButton(onClick = {
                                showMultiSelectMode = false
                                selectedTicketIds.clear()
                            }) {
                                Icon(Icons.Default.Close, contentDescription = "Đóng", tint = Color.White)
                            }

                            val context = LocalContext.current
                            val isExporting by viewModel.isExporting.collectAsState()
                            IconButton(
                                onClick = {
                                    if (selectedTicketIds.isNotEmpty()) {
                                        viewModel.exportMultipleTicketsToExcel(context, selectedTicketIds.toList())
                                    } else {
                                        android.widget.Toast.makeText(context, "Vui lòng chọn ít nhất một phiếu", android.widget.Toast.LENGTH_SHORT).show()
                                    }
                                },
                                enabled = !isExporting
                            ) {
                                if (isExporting) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(20.dp),
                                        color = Color.White,
                                        strokeWidth = 2.dp
                                    )
                                } else {
                                    Icon(Icons.Default.Share, contentDescription = "Xuất nhiều", tint = Color.White)
                                }
                            }
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = HeaderGreen
                    )
                )
                
                // Search Bar
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = {
                        searchQuery = it
                        searchJob?.cancel()
                        searchJob = coroutineScope.launch {
                            delay(300)
                            debouncedQuery = it
                        }
                    },
                    placeholder = { Text("Tìm kiếm...", color = Color.Gray.copy(alpha = 0.6f)) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Color.Gray) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .height(56.dp),
                    shape = RoundedCornerShape(25.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color.White.copy(alpha = 0.5f),
                        unfocusedBorderColor = Color.White.copy(alpha = 0.3f),
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White,
                        cursorColor = HeaderGreen
                    ),
                    singleLine = true
                )
                
                // Nút Tính tổng các phiếu
                Button(
                    onClick = {
                        selectedTicketIds.clear()
                        selectedTicketIds.addAll(filteredTickets.map { it.id })
                        showMultiSelectMode = true
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = TicketGreen),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Functions, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Tính tổng các phiếu", fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(8.dp))
            }
        },
        bottomBar = {
            Surface(
                modifier = Modifier.shadow(elevation = 8.dp),
                color = Color.White
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .height(80.dp)
                        .padding(horizontal = 24.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Thùng rác (bên trái)
                    IconButton(onClick = onNavigateToTrash) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Thùng rác",
                            tint = TrashBlue,
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    // Nút Tạo mới (ở giữa)
                    Button(
                        onClick = { showAddDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = CreateButtonOrange),
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier.height(44.dp).padding(horizontal = 8.dp),
                        contentPadding = PaddingValues(horizontal = 24.dp)
                    ) {
                        Text("Tạo mới", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }

                    // Nút Cài đặt (bên phải)
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(
                            Icons.Default.Settings,
                            contentDescription = "Cài đặt",
                            tint = SettingsBlue,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            Box(modifier = Modifier.weight(1f)) {
                if (filteredTickets.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            if (searchQuery.isEmpty()) "Hãy bấm Tạo mới để bắt đầu" else "Không tìm thấy phiếu nào",
                            style = MaterialTheme.typography.bodyLarge,
                            color = Color.Gray,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp)
                ) {
                    // Tổng hợp các phiếu đã chọn
                    if (showMultiSelectMode && selectedTicketIds.isNotEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .padding(vertical = 12.dp)
                            ) {
                                MultiSelectSummaryCard(
                                    ticketIds = selectedTicketIds,
                                    viewModel = viewModel
                                )
                            }
                        }
                    }

                    if (showMultiSelectMode) {
                        item {
                            val allIds = filteredTickets.map { it.id }
                            val isAllSelected = selectedTicketIds.size == allIds.size && allIds.isNotEmpty()
                            
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp)
                                    .clickable {
                                        if (isAllSelected) {
                                            selectedTicketIds.clear()
                                            showMultiSelectMode = false
                                        } else {
                                            selectedTicketIds.clear()
                                            selectedTicketIds.addAll(allIds)
                                        }
                                    },
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = isAllSelected,
                                    onCheckedChange = { checked ->
                                        if (checked) {
                                            selectedTicketIds.clear()
                                            selectedTicketIds.addAll(allIds)
                                        } else {
                                            selectedTicketIds.clear()
                                            showMultiSelectMode = false
                                        }
                                    },
                                    colors = CheckboxDefaults.colors(checkedColor = TicketGreen)
                                )
                                Text(
                                    "Chọn tất cả (${filteredTickets.size} mã)",
                                    fontWeight = FontWeight.Bold,
                                    color = TicketGreen,
                                    fontSize = 16.sp
                                )
                            }
                        }
                    }
                    
                    item { Spacer(modifier = Modifier.height(12.dp)) }
                    items(filteredTickets, key = { it.id }) { ticket ->
                        TicketCard(
                            ticket = ticket,
                            viewModel = viewModel,
                            isSelected = selectedTicketIds.contains(ticket.id),
                            isMultiSelectMode = showMultiSelectMode,
                            onLongClick = {
                                showMultiSelectMode = true
                                if (!selectedTicketIds.contains(ticket.id)) selectedTicketIds.add(ticket.id)
                            },
                            onToggleSelect = {
                                if (selectedTicketIds.contains(ticket.id)) {
                                    selectedTicketIds.remove(ticket.id)
                                    if (selectedTicketIds.isEmpty()) showMultiSelectMode = false
                                } else {
                                    selectedTicketIds.add(ticket.id)
                                }
                            },
                            onClick = { 
                                if (showMultiSelectMode) {
                                    if (selectedTicketIds.contains(ticket.id)) {
                                        selectedTicketIds.remove(ticket.id)
                                        if (selectedTicketIds.isEmpty()) showMultiSelectMode = false
                                    } else {
                                        selectedTicketIds.add(ticket.id)
                                    }
                                } else {
                                    onTicketClick(ticket.id) 
                                }
                            }
                        )
                    }
                    item { Spacer(modifier = Modifier.height(16.dp)) }
                }
            }
        }

        if (showAddDialog) {
            AlertDialog(
                onDismissRequest = { showAddDialog = false },
                title = { Text("Tạo phiếu mới", color = HeaderGreen, fontWeight = FontWeight.Bold) },
                text = {
                    OutlinedTextField(
                        value = newTicketName,
                        onValueChange = { newTicketName = it },
                        label = { Text("Tên phiếu") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = HeaderGreen,
                            focusedLabelColor = HeaderGreen
                        )
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (newTicketName.isNotBlank()) {
                                viewModel.createNewTicket(newTicketName)
                                showAddDialog = false
                                newTicketName = ""
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = HeaderGreen)
                    ) {
                        Text("Tạo")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showAddDialog = false }) {
                        Text("Hủy", color = HeaderGreen)
                    }
                }
            )
        }
    }
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun TicketCard(
    ticket: FishTicket,
    viewModel: FishViewModel,
    isSelected: Boolean = false,
    isMultiSelectMode: Boolean = false,
    onClick: () -> Unit,
    onLongClick: () -> Unit = {},
    onToggleSelect: () -> Unit = {}
) {
    val cells by viewModel.getAllCellsForTicket(ticket.id).collectAsState(initial = emptyList())
    
    val totalWeight = cells.sumOf { it.value }
    val totalBags = cells.count { it.value > 0 }
    
    val appSettings by viewModel.appSettings.collectAsState()
    val decimalPlaces = appSettings.decimalPlaces
    val factor = Math.pow(10.0, decimalPlaces.toDouble())
    
    val rawRemainingWeight = totalWeight - (totalBags * ticket.tarePerBag)
    val remainingWeight = Math.round(rawRemainingWeight * factor) / factor
    val finalAmount = Math.round(remainingWeight * ticket.unitPrice)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .shadow(elevation = 4.dp, shape = RoundedCornerShape(16.dp))
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            ),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) Color(0xFFE8F5E9) else Color.White
        ),
        border = if (isSelected) BorderStroke(2.dp, TicketGreen) else null
    ) {
        Column {
            // Upper Part (Green #2f7d32)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(if (isSelected) TicketGreen.copy(alpha = 0.8f) else TicketGreen)
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (isMultiSelectMode) {
                        Checkbox(
                            checked = isSelected,
                            onCheckedChange = { onToggleSelect() },
                            colors = CheckboxDefaults.colors(checkedColor = Color.White, uncheckedColor = Color.White)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    
                    Column(modifier = Modifier.weight(1f)) {
                        // Tiêu đề với icon người
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = ticket.ticketName,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(4.dp))
                        
                        // Ngày tháng với icon lịch
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.CalendarToday,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = SimpleDateFormat("dd/MM/yyyy    HH:mm", Locale.getDefault()).format(Date(ticket.createdAt)),
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White
                            )
                        }
                    }

                    // Nút Xoá và Mở
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        var showDeleteConfirm by remember { mutableStateOf(false) }
                        
                        IconButton(onClick = { showDeleteConfirm = true }) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Xoá phiếu",
                                tint = Color.White.copy(alpha = 0.9f),
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        if (showDeleteConfirm) {
                            AlertDialog(
                                onDismissRequest = { showDeleteConfirm = false },
                                title = { Text("Xác nhận xoá") },
                                text = { Text("Bạn có chắc chắn muốn xoá phiếu '${ticket.ticketName}' không?") },
                                confirmButton = {
                                    TextButton(onClick = {
                                        viewModel.softDeleteTicket(ticket.id)
                                        showDeleteConfirm = false
                                    }) {
                                        Text("XOÁ", color = Color.Red, fontWeight = FontWeight.Bold)
                                    }
                                },
                                dismissButton = {
                                    TextButton(onClick = { showDeleteConfirm = false }) {
                                        Text("HUỶ")
                                    }
                                }
                            )
                        }

                        Spacer(modifier = Modifier.width(4.dp))
                        
                        Button(
                            onClick = onClick,
                            colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                            shape = RoundedCornerShape(20.dp),
                            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 0.dp),
                            modifier = Modifier.height(36.dp)
                        ) {
                            Text("Mở", color = TicketGreen, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Lower Part (Grey #edeff1)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(AppBackground)
                    .padding(16.dp)
            ) {
                Text(
                    text = "Tổng: ${"%.1f".format(Locale.US, totalWeight)} kg / $totalBags mã",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.DarkGray
                )
                Text(
                    text = "Đơn giá: ${DecimalFormat("#,###").format(ticket.unitPrice)} VNĐ",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.DarkGray
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                // Thành tiền: Label đen, số cam
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Thành tiền: ",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                    Text(
                        text = "${DecimalFormat("#,###").format(finalAmount)} VNĐ",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = AmountOrange
                    )
                }
            }
        }
    }
}

@Composable
fun MultiSelectSummaryCard(
    ticketIds: List<Int>,
    viewModel: FishViewModel
) {
    val totals by viewModel.getTotalsForTickets(ticketIds).collectAsState(initial = FishMultiTicketTotals())
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(elevation = 12.dp, shape = RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(2.dp, SummaryGold)
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(SummaryGold)
                    .padding(16.dp)
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Summarize,
                            contentDescription = null,
                            tint = Color.Black,
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "TỔNG HỢP CÁC PHIẾU",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.Black
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Surface(
                        color = Color.Black,
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text(
                            "Đã chọn: ${ticketIds.size} phiếu",
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.ExtraBold,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                        )
                    }
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(SummaryGoldLight)
                    .padding(16.dp)
            ) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(
                        text = "Tổng khối lượng:",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.DarkGray
                    )
                    Text(
                        text = "${"%.1f".format(Locale.US, totals.totalWeight)} kg",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Black,
                        color = Color.Black
                    )
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(
                        text = "Tổng số mã:",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.DarkGray
                    )
                    Text(
                        text = "${totals.totalBags} mã",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Black,
                        color = Color.Black
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(color = Color.Black.copy(alpha = 0.1f), thickness = 1.dp)
                Spacer(modifier = Modifier.height(16.dp))

                // TỔNG TIỀN CÁ: Xuống dòng
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "TỔNG TIỀN CÁ:",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.DarkGray
                    )
                    Text(
                        text = "${DecimalFormat("#,###").format(totals.totalFishValue)} VNĐ",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Black,
                        color = Color.Black
                    )
                }

                if (totals.totalFishValue != totals.totalBalance) {
                    Spacer(modifier = Modifier.height(16.dp))
                    // TỔNG CÒN LẠI: Xuống dòng
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "TỔNG CÒN LẠI:",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.DarkGray
                        )
                        Text(
                            text = "${DecimalFormat("#,###").format(totals.totalBalance)} VNĐ",
                            style = MaterialTheme.typography.headlineLarge,
                            fontWeight = FontWeight.Black,
                            color = BalanceRed
                        )
                    }
                    Text(
                        text = "(Đã trừ cọc & ứng các phiếu)",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Gray,
                        modifier = Modifier.align(Alignment.End).padding(top = 4.dp)
                    )
                } else {
                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider(color = Color.Black.copy(alpha = 0.1f))
                    Spacer(modifier = Modifier.height(16.dp))
                    // TỔNG THANH TOÁN: Xuống dòng
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "TỔNG THANH TOÁN:",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.DarkGray
                        )
                        Text(
                            text = "${DecimalFormat("#,###").format(totals.totalBalance)} VNĐ",
                            style = MaterialTheme.typography.headlineLarge,
                            fontWeight = FontWeight.Black,
                            color = BalanceRed
                        )
                    }
                }
            }
        }
    }
}

package com.quangthe.canca.ui.screens

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Scale
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.RemoveCircleOutline
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Sell
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.quangthe.canca.data.FishCell
import com.quangthe.canca.data.FishSheet
import com.quangthe.canca.data.FishTicket
import com.quangthe.canca.ui.theme.*
import com.quangthe.canca.ui.theme.DetailPrimaryGreen
import com.quangthe.canca.utils.ScreenshotUtils
import com.quangthe.canca.viewmodel.FishViewModel
import com.google.gson.Gson
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.launch
import java.text.DecimalFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, kotlinx.coroutines.ExperimentalCoroutinesApi::class)
@Composable
fun TicketDetailScreen(
    viewModel: FishViewModel,
    ticketId: Int,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val ticket by viewModel.selectedTicket.collectAsState()
    val sheets by viewModel.sheets.collectAsState()
    val appSettings by viewModel.appSettings.collectAsState()

    val scrollState = rememberScrollState()
    val coroutineScope = rememberCoroutineScope()
    var isEditMode by remember { mutableStateOf(false) }
    var isDashboardExpanded by remember { mutableStateOf(true) }
    var showEditNameDialog by remember { mutableStateOf(false) }
    val unitPriceFocusRequester = remember { FocusRequester() }

    LaunchedEffect(ticketId) {
        viewModel.selectTicket(ticketId)
    }

    if (ticket == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = DetailPrimaryGreen)
        }
        return
    }

    val allCellsBySheetId by remember(sheets) {
        if (sheets.isEmpty()) flowOf(emptyMap<Int, List<FishCell>>())
        else {
            val flows = sheets.map { sheet ->
                viewModel.getCellsForSheet(sheet.id).map { cells -> sheet.id to cells }
            }
            combine(flows) { array -> array.toMap() }
        }
    }.collectAsState(initial = emptyMap())

    val totalWeight by viewModel.totalWeight.collectAsState()
    val allCells = allCellsBySheetId.values.flatten()
    val numBags = allCells.count { it.value > 0 }
    val totalTare = numBags * (ticket?.tarePerBag ?: 0.0)
    val totalImpurity = (totalWeight / 1000.0) * (ticket?.impurityPerTon ?: 0)
    
    val weightAfterTare = totalWeight - totalTare - totalImpurity
    val deductionAmount = if (ticket?.deductionType == 0) {
        weightAfterTare * ((ticket?.deductionValue ?: 0.0) / 100.0)
    } else {
        ticket?.deductionValue ?: 0.0
    }
    
    val rawRemainingWeight = weightAfterTare - deductionAmount
    
    // Làm tròn khối lượng còn lại theo cấu hình (ví dụ 1 chữ số thập phân) trước khi tính tiền
    // để kết quả khớp với con số hiển thị trên màn hình và máy tính tay.
    val factor = Math.pow(10.0, appSettings.decimalPlaces.toDouble())
    val remainingWeight = Math.round(rawRemainingWeight * factor) / factor
    val totalPrice = Math.round(remainingWeight * (ticket?.unitPrice ?: 0))

    Scaffold(
        containerColor = DetailAppBackground,
        topBar = {
            Column {
                TopAppBar(
                    title = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.clickable { if (isEditMode) showEditNameDialog = true }
                        ) {
                            Text(
                                ticket?.ticketName ?: "Chi tiết phiếu",
                                fontWeight = FontWeight.ExtraBold,
                                color = DetailPrimaryGreen
                            )
                            if (isEditMode) {
                                Icon(
                                    Icons.Default.Edit,
                                    contentDescription = null,
                                    tint = DetailPrimaryGreen,
                                    modifier = Modifier.size(16.dp).padding(start = 4.dp)
                                )
                            }
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Quay lại", tint = DetailPrimaryGreen)
                        }
                    },
                    actions = {
                        IconButton(onClick = {
                            ticket?.let { t ->
                                viewModel.exportTicketToExcelFixed(context, t, sheets, allCells, totalWeight, totalPrice)
                            }
                        }) {
                            Icon(Icons.Default.Share, contentDescription = "Xuất Excel", tint = DetailPrimaryGreen)
                        }

                        Surface(
                            modifier = Modifier
                                .padding(end = 8.dp)
                                .clip(RoundedCornerShape(20.dp))
                                .clickable { isEditMode = !isEditMode },
                            color = SummaryGold
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = if (isEditMode) Icons.Default.Check else Icons.Default.Edit,
                                    contentDescription = null,
                                    tint = Color.Black,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(Modifier.width(4.dp))
                                Text(
                                    text = if (isEditMode) "XONG" else "SỬA",
                                    color = Color.Black,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
                )
                HorizontalDivider(color = DetailBorderColor, thickness = 1.dp)
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding()
                .verticalScroll(scrollState)
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(12.dp))

            DashboardContent(
                totalWeight = totalWeight,
                numBags = numBags,
                totalTare = totalTare,
                weightAfterTare = weightAfterTare,
                deductionAmount = deductionAmount,
                remainingWeight = remainingWeight,
                totalPrice = totalPrice.toDouble(),
                ticket = ticket!!,
                isExpanded = isDashboardExpanded,
                onToggleExpand = { isDashboardExpanded = !isDashboardExpanded },
                onTicketUpdate = { viewModel.updateTicket(it) },
                unitPriceFocusRequester = unitPriceFocusRequester,
                isEditMode = isEditMode
            )

            Spacer(modifier = Modifier.height(12.dp))

            val currentSheetIndex by viewModel.currentSheetIndex.collectAsState()
            val currentCells by viewModel.currentCells.collectAsState()
            val currentSheet = sheets.getOrNull(currentSheetIndex)

            if (currentSheet != null) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = { viewModel.setCurrentSheet(currentSheetIndex - 1) },
                            enabled = currentSheetIndex > 0
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Bảng trước",
                                tint = if(currentSheetIndex > 0) DetailPrimaryGreen else Color.Gray
                            )
                        }

                        Text(
                            text = "BẢNG ${currentSheetIndex + 1} / ${sheets.size}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Black,
                            color = DetailPrimaryGreen,
                            modifier = Modifier.padding(horizontal = 24.dp)
                        )

                        IconButton(
                            onClick = { viewModel.setCurrentSheet(currentSheetIndex + 1) },
                            enabled = currentSheetIndex < sheets.size - 1
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = "Bảng sau",
                                tint = if(currentSheetIndex < sheets.size - 1) DetailPrimaryGreen else Color.Gray
                            )
                        }
                    }

                    DetailDynamicTable(
                        sheet = currentSheet,
                        cells = currentCells,
                        onCellValueChange = { r, c, v ->
                            viewModel.updateTableCell(r, c, v)
                        },
                        onFocusCell = { r, c ->
                            viewModel.setLastFocusPosition(currentSheetIndex, r, c)
                        },
                        onSpeakNumber = { viewModel.speakNumber(it) },
                        onSpeakText = { viewModel.speak(it) },
                        decimalPlaces = appSettings.decimalPlaces,
                        maxIntegerDigits = appSettings.maxIntegerDigits,
                        speakOnColumnComplete = appSettings.speakOnColumnComplete,
                        onAutoNextSheet = {
                            viewModel.setLastFocusPosition(currentSheetIndex + 1, 0, 0)
                            viewModel.autoCreateNextSheet()
                        },
                        onVibrate = { viewModel.triggerVibration() },
                        initialFocus = viewModel.getLastFocusPosition(currentSheetIndex),
                        isEditMode = isEditMode,
                        isLatestSheet = currentSheetIndex == sheets.size - 1,
                        scrollable = false,
                        tableFontSize = appSettings.tableFontSize
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            SendButton(
                onClick = {
                    coroutineScope.launch {
                        try {
                            Toast.makeText(context, "Đang tạo ảnh chất lượng cao...", Toast.LENGTH_SHORT).show()
                            
                            val ticketData = ticket ?: return@launch
                            val weightData = totalWeight
                            val priceData = totalPrice
                            
                            viewModel.getAllCellsForTicket(ticketData.id).take(1).collect { cells ->
                                val bitmap = ScreenshotUtils.generateBitmapFromComposable(context) {
                                    LongTicketShareView(
                                        ticket = ticketData,
                                        allCells = cells,
                                        totalWeight = weightData,
                                        remainingWeight = remainingWeight,
                                        totalPrice = priceData,
                                        fontSize = appSettings.tableFontSize
                                    )
                                }
                                
                                val fileName = "Phieu_Can_Ca_${ticketData.ticketName.replace(" ", "_")}.png"
                                ScreenshotUtils.shareBitmap(context, bitmap, fileName)
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                            Toast.makeText(context, "Lỗi: ${e.message}", Toast.LENGTH_LONG).show()
                        }
                    }
                }
            )

            Spacer(modifier = Modifier.height(140.dp))
        }

        if (showEditNameDialog && ticket != null) {
            var tempName by remember { mutableStateOf(ticket!!.ticketName) }
            AlertDialog(
                onDismissRequest = { showEditNameDialog = false },
                title = { Text("Sửa tên phiếu") },
                text = {
                    OutlinedTextField(
                        value = tempName,
                        onValueChange = { tempName = it },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                },
                confirmButton = {
                    TextButton(onClick = {
                        viewModel.updateTicket(ticket!!.copy(ticketName = tempName))
                        showEditNameDialog = false
                    }) { Text("Lưu") }
                },
                dismissButton = {
                    TextButton(onClick = { showEditNameDialog = false }) { Text("Hủy") }
                }
            )
        }
    }
}

@Composable
fun DashboardContent(
    totalWeight: Double,
    numBags: Int,
    totalTare: Double,
    weightAfterTare: Double,
    deductionAmount: Double,
    remainingWeight: Double,
    totalPrice: Double,
    ticket: FishTicket,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    onTicketUpdate: (FishTicket) -> Unit,
    unitPriceFocusRequester: FocusRequester,
    isEditMode: Boolean
) {
    var showTareDialog by remember { mutableStateOf(false) }
    var showDeductionDialog by remember { mutableStateOf(false) }
    var showPriceDialog by remember { mutableStateOf(false) }
    var showDepositDialog by remember { mutableStateOf(false) }

    val depositAndAdvance = ticket.deposit
    val balance = totalPrice - depositAndAdvance

    Column(modifier = Modifier.fillMaxWidth()) {
        // Nhóm 1: Thông tin khách hàng
        Card(
            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(2.dp)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Person, contentDescription = null, tint = DetailPrimaryGreen, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Thông tin khách hàng", fontWeight = FontWeight.Bold, color = DetailPrimaryGreen)
                }
                Spacer(Modifier.height(8.dp))
                
                // Tên khách hàng (Có thể sửa)
                var showNameDialog by remember { mutableStateOf(false) }
                Row(
                    modifier = Modifier.fillMaxWidth().clickable { if(isEditMode) showNameDialog = true }.padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Edit, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(8.dp))
                    Column {
                        Text("Tên khách hàng", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                        Text(ticket.ticketName, fontWeight = FontWeight.Bold, color = Color.Black)
                    }
                }
                
                // Số điện thoại (Có thể sửa)
                var showPhoneDialog by remember { mutableStateOf(false) }
                Row(
                    modifier = Modifier.fillMaxWidth().clickable { if(isEditMode) showPhoneDialog = true }.padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Call, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(8.dp))
                    Column {
                        Text("Số điện thoại", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                        Text(if(ticket.phoneNumber.isEmpty()) "Chưa có" else ticket.phoneNumber, fontWeight = FontWeight.Bold, color = Color.Black)
                    }
                }

                if (showNameDialog) {
                    var tempName by remember { mutableStateOf(ticket.ticketName) }
                    AlertDialog(
                        onDismissRequest = { showNameDialog = false },
                        title = { Text("Sửa tên khách hàng") },
                        text = { OutlinedTextField(value = tempName, onValueChange = { tempName = it }, modifier = Modifier.fillMaxWidth(), singleLine = true) },
                        confirmButton = { TextButton(onClick = { onTicketUpdate(ticket.copy(ticketName = tempName)); showNameDialog = false }) { Text("Lưu") } },
                        dismissButton = { TextButton(onClick = { showNameDialog = false }) { Text("Hủy") } }
                    )
                }
                if (showPhoneDialog) {
                    var tempPhone by remember { mutableStateOf(ticket.phoneNumber) }
                    AlertDialog(
                        onDismissRequest = { showPhoneDialog = false },
                        title = { Text("Sửa số điện thoại") },
                        text = { OutlinedTextField(value = tempPhone, onValueChange = { tempPhone = it }, modifier = Modifier.fillMaxWidth(), singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone)) },
                        confirmButton = { TextButton(onClick = { onTicketUpdate(ticket.copy(phoneNumber = tempPhone)); showPhoneDialog = false }) { Text("Lưu") } },
                        dismissButton = { TextButton(onClick = { showPhoneDialog = false }) { Text("Hủy") } }
                    )
                }
            }
        }

        // Nhóm 2: StatCards (Ô vuông thông số)
        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            StatSquare(
                label = "Tổng khối lượng",
                subLabel = "(Chưa trừ bì)",
                value = String.format(Locale.US, "%.1f", totalWeight),
                unit = "kg",
                icon = Icons.Default.Scale,
                iconColor = DetailPrimaryGreen,
                valueColor = DetailPrimaryGreen,
                modifier = Modifier.weight(1f)
            )
            StatSquare(
                label = "Số mã cân",
                subLabel = "",
                value = "$numBags",
                unit = "mã",
                icon = Icons.Default.Inventory2,
                iconColor = Color(0xFF8D6E63), // Brownish for bags
                valueColor = Color(0xFF8D6E63),
                modifier = Modifier.weight(1f)
            )
        }

        // Nhóm 3: Khấu trừ & Kết quả Net
        Card(
            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(2.dp)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                // Trừ bì
                EditableStatRow(
                    label = "Trừ bì",
                    subLabel = "${ticket.tarePerBag} kg/mã",
                    value = String.format(Locale.US, "-%.1f", totalTare),
                    unit = "kg",
                    icon = Icons.Default.RemoveCircleOutline,
                    iconColor = BalanceRed,
                    isEditMode = isEditMode,
                    onClick = { showTareDialog = true }
                )

                // Khấu trừ phao
                if (deductionAmount > 0 || isEditMode) {
                    val deductionLabel = if (ticket.deductionType == 0) "${ticket.deductionValue}%" else "${ticket.deductionValue} kg"
                    EditableStatRow(
                        label = "Khấu trừ phao",
                        subLabel = deductionLabel,
                        value = String.format(Locale.US, "-%.1f", deductionAmount),
                        unit = "kg",
                        icon = Icons.Default.RemoveCircleOutline,
                        iconColor = Color.Gray,
                        isEditMode = isEditMode,
                        onClick = { showDeductionDialog = true }
                    )
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = Color.LightGray.copy(alpha = 0.5f))

                // Khối lượng còn lại (Net)
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column {
                            Text("Khối lượng còn lại", fontWeight = FontWeight.Bold, color = DetailPrimaryGreen)
                            Surface(color = DetailPrimaryGreen.copy(alpha = 0.1f), shape = RoundedCornerShape(4.dp)) {
                                Text("Đã trừ bì", fontSize = 10.sp, color = DetailPrimaryGreen, modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp), fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                    Spacer(Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        Text(String.format(Locale.US, "%.1f", remainingWeight), style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Black, color = DetailPrimaryGreen)
                        Spacer(Modifier.width(4.dp))
                        Text("kg", color = DetailPrimaryGreen, modifier = Modifier.padding(bottom = 12.dp), fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                }
            }
        }

        // Nhóm 4: Thanh toán
        Card(
            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(2.dp)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Payments, contentDescription = null, tint = DetailPrimaryGreen, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Thành tiền", fontWeight = FontWeight.Bold, color = DetailPrimaryGreen)
                }
                Spacer(Modifier.height(12.dp))

                // Đơn giá
                EditableStatRow(
                    label = "Đơn giá",
                    subLabel = "",
                    value = DecimalFormat("#,###").format(ticket.unitPrice),
                    unit = "đ/kg",
                    icon = Icons.Default.Sell,
                    iconColor = Color(0xFFE64A19), // Orange-Red
                    isEditMode = isEditMode,
                    onClick = { showPriceDialog = true }
                )

                // Thành tiền
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Thành tiền", fontWeight = FontWeight.Bold, color = Color.Gray)
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(DecimalFormat("#,###").format(totalPrice), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black, color = DetailPrimaryGreen)
                        Spacer(Modifier.width(4.dp))
                        Text("đ", color = DetailPrimaryGreen, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = Color.LightGray.copy(alpha = 0.5f))

                // Cọc/Ứng
                EditableStatRow(
                    label = "Tiền cọc, ứng",
                    subLabel = "",
                    value = DecimalFormat("#,###").format(ticket.deposit),
                    unit = "đ",
                    icon = Icons.Default.AccountBalanceWallet,
                    iconColor = Color(0xFFFBC02D), // Gold
                    isEditMode = isEditMode,
                    onClick = { showDepositDialog = true }
                )

                Spacer(Modifier.height(8.dp))

                // Dòng CÒN LẠI màu xanh đậm
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    color = DetailPrimaryGreen
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 12.dp)
                    ) {
                        Text("CÒN LẠI", fontWeight = FontWeight.Black, color = Color.White)
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "${DecimalFormat("#,###").format(balance)} đ",
                            style = MaterialTheme.typography.headlineLarge,
                            fontWeight = FontWeight.Black,
                            color = Color.White,
                            modifier = Modifier.align(Alignment.End)
                        )
                    }
                }
            }
        }
    }

    if (showDeductionDialog && ticket != null) {
        var tempType by remember { mutableStateOf(ticket.deductionType) }
        var tempValue by remember { mutableStateOf(ticket.deductionValue.toString()) }
        
        AlertDialog(
            onDismissRequest = { showDeductionDialog = false },
            title = { Text("Cài đặt khấu trừ phao/nước") },
            text = {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = tempType == 0, onClick = { tempType = 0 })
                        Text("Trừ theo %", modifier = Modifier.clickable { tempType = 0 })
                        Spacer(Modifier.width(16.dp))
                        RadioButton(selected = tempType == 1, onClick = { tempType = 1 })
                        Text("Trừ theo kg", modifier = Modifier.clickable { tempType = 1 })
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(if (tempType == 0) "Nhập tỷ lệ %:" else "Nhập số kg trừ cố định:")
                    OutlinedTextField(
                        value = tempValue,
                        onValueChange = { tempValue = it },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth(),
                        suffix = { Text(if (tempType == 0) "%" else "kg") }
                    )
                    if (tempType == 0) {
                        Text(
                            text = "Khấu trừ = (Tổng - Bì) x $tempValue%",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    onTicketUpdate(ticket.copy(
                        deductionType = tempType,
                        deductionValue = tempValue.toDoubleOrNull() ?: 0.0
                    ))
                    showDeductionDialog = false
                }) { Text("Lưu") }
            },
            dismissButton = {
                TextButton(onClick = { showDeductionDialog = false }) { Text("Hủy") }
            }
        )
    }

    if (showTareDialog && ticket != null) {
        var tempValue by remember { mutableStateOf(ticket.tarePerBag.toString()) }
        AlertDialog(
            onDismissRequest = { showTareDialog = false },
            title = { Text("Cài đặt trừ bì mã cân") },
            text = {
                Column {
                    Text("Nhập số kg bì cho 1 mã (thùng/giỏ):")
                    OutlinedTextField(
                        value = tempValue,
                        onValueChange = { tempValue = it },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    onTicketUpdate(ticket.copy(tarePerBag = tempValue.toDoubleOrNull() ?: 0.0))
                    showTareDialog = false
                }) { Text("Lưu") }
            },
            dismissButton = {
                TextButton(onClick = { showTareDialog = false }) { Text("Hủy") }
            }
        )
    }

    if (showPriceDialog && ticket != null) {
        var tempValue by remember {
            val s = ticket.unitPrice.toString()
            mutableStateOf(TextFieldValue(text = s, selection = TextRange(0, s.length)))
        }
        AlertDialog(
            onDismissRequest = { showPriceDialog = false },
            title = { Text("Cài đặt đơn giá") },
            text = {
                Column {
                    Text("Đơn giá (VNĐ/kg):")
                    OutlinedTextField(
                        value = tempValue,
                        onValueChange = { tempValue = it },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    onTicketUpdate(ticket.copy(unitPrice = tempValue.text.toIntOrNull() ?: 0))
                    showPriceDialog = false
                }) { Text("Lưu") }
            },
            dismissButton = {
                TextButton(onClick = { showPriceDialog = false }) { Text("Hủy") }
            }
        )
    }

    if (showDepositDialog && ticket != null) {
        var tempValue by remember { mutableStateOf(ticket.deposit.toString()) }
        AlertDialog(
            onDismissRequest = { showDepositDialog = false },
            title = { Text("Cài đặt tiền cọc, ứng") },
            text = {
                Column {
                    Text("Nhập số tiền cọc, ứng (đ):")
                    OutlinedTextField(
                        value = tempValue,
                        onValueChange = { tempValue = it.filter { c -> c.isDigit() } },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    onTicketUpdate(ticket.copy(deposit = tempValue.toLongOrNull() ?: 0L))
                    showDepositDialog = false
                }) { Text("Lưu") }
            },
            dismissButton = {
                TextButton(onClick = { showDepositDialog = false }) { Text("Hủy") }
            }
        )
    }
}

@Composable
fun StatSquare(
    label: String,
    subLabel: String,
    value: String,
    unit: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconColor: Color,
    valueColor: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.heightIn(min = 160.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(28.dp))
            Spacer(Modifier.height(8.dp))
            Text(
                label,
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            if (subLabel.isNotEmpty()) {
                Text(
                    subLabel,
                    fontSize = 9.sp,
                    color = Color.Gray,
                    textAlign = TextAlign.Center
                )
            }
            Spacer(Modifier.height(4.dp))
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    value,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Black,
                    color = valueColor,
                    textAlign = TextAlign.Center
                )
                Text(
                    unit,
                    fontSize = 15.sp,
                    color = valueColor,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
fun EditableStatRow(
    label: String,
    subLabel: String,
    value: String,
    unit: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconColor: Color,
    isEditMode: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { if (isEditMode) onClick() }
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(32.dp).background(iconColor.copy(alpha = 0.1f), RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(18.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.bodyMedium, color = Color.Black, fontWeight = FontWeight.Bold)
            if (subLabel.isNotEmpty()) {
                Text(subLabel, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (isEditMode) {
                Icon(Icons.Default.Edit, contentDescription = null, tint = Color.LightGray, modifier = Modifier.size(14.dp).padding(end = 4.dp))
            }
            Text(value, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold, color = iconColor)
            Spacer(Modifier.width(4.dp))
            Text(unit, style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
        }
    }
}

@Composable
fun DashboardEditableRow(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    isEditMode: Boolean,
    suffix: String = "",
    focusRequester: FocusRequester? = null,
    onSettingsClick: (() -> Unit)? = null
) {
    var textFieldValue by remember(value) {
        mutableStateOf(TextFieldValue(text = value, selection = TextRange(value.length)))
    }

    val backgroundColor = if (isEditMode) Color.White else DetailPrimaryGreen
    val contentColor = if (isEditMode) DetailPrimaryGreen else Color.White

    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = Color.Black, fontWeight = FontWeight.Bold)

        Spacer(modifier = Modifier.height(2.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp)
                .background(backgroundColor, RoundedCornerShape(8.dp))
                .border(1.dp, DetailPrimaryGreen.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                .padding(horizontal = 8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxSize()
            ) {
                if (isEditMode) {
                    BasicTextField(
                        value = textFieldValue,
                        onValueChange = {
                            textFieldValue = it.copy(selection = TextRange(it.text.length))
                            onValueChange(it.text)
                        },
                        modifier = Modifier
                            .weight(1f)
                            .then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier),
                        textStyle = TextStyle(textAlign = TextAlign.Start, fontWeight = FontWeight.Bold, color = contentColor),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        cursorBrush = SolidColor(DetailPrimaryGreen)
                    )
                } else {
                    Text(
                        text = if (value.isEmpty()) "0" else value,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                        color = contentColor,
                        modifier = Modifier.weight(1f)
                    )
                }

                if (suffix.isNotEmpty()) {
                    Text(suffix, style = MaterialTheme.typography.bodySmall, color = contentColor, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 8.dp))
                }

                if (onSettingsClick != null) {
                    IconButton(onClick = onSettingsClick, modifier = Modifier.size(32.dp).padding(start = 4.dp)) {
                        Icon(Icons.Default.Settings, contentDescription = null, tint = contentColor, modifier = Modifier.size(18.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun DashboardItemRow(
    label: String,
    value: String,
    isBold: Boolean = false,
    valueColor: Color = Color.White,
    backgroundColor: Color = DetailPrimaryGreen
) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = Color.Black, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(2.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 44.dp)
                .background(backgroundColor, RoundedCornerShape(8.dp))
                .border(1.dp, backgroundColor.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                .padding(horizontal = 8.dp, vertical = 8.dp)
        ) {
            Text(
                value,
                style = if (isBold) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                color = valueColor,
                modifier = Modifier.align(Alignment.CenterStart)
            )
        }
    }
}

@Composable
fun SendButton(onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp),
        shape = RoundedCornerShape(28.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = DetailDarkGreen
        ),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
    ) {
        Text("GỬI KẾT QUẢ", fontWeight = FontWeight.Bold, color = Color.White)
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DetailDynamicTable(
    sheet: FishSheet,
    cells: List<FishCell>,
    onCellValueChange: (Int, Int, Double) -> Unit,
    onFocusCell: (Int, Int) -> Unit,
    onSpeakNumber: (Double) -> Unit,
    onSpeakText: (String) -> Unit = {},
    decimalPlaces: Int,
    maxIntegerDigits: Int,
    speakOnColumnComplete: Boolean,
    onAutoNextSheet: () -> Unit,
    onVibrate: () -> Unit,
    initialFocus: Pair<Int, Int> = Pair(0, 0),
    isEditMode: Boolean = false,
    isLatestSheet: Boolean = true,
    scrollable: Boolean = true,
    tableFontSize: Float = 24f
) {
    val focusManager = LocalFocusManager.current
    val numRows = sheet.numRows
    val numCols = sheet.numCols
    val gson = remember { Gson() }
    val colTitles: List<String> = try { gson.fromJson(sheet.colTitles, Array<String>::class.java).toList() } catch (e: Exception) { emptyList() }

    val df = remember(decimalPlaces) {
        when (decimalPlaces) {
            1 -> DecimalFormat("0.0")
            2 -> DecimalFormat("0.00")
            else -> DecimalFormat("#")
        }
    }

    val focusRequesters = remember(sheet.id, numRows, numCols) {
        Array(numRows) { Array(numCols) { FocusRequester() } }
    }

    LaunchedEffect(sheet.id, isEditMode) {
        if (isEditMode && isLatestSheet) {
            kotlinx.coroutines.delay(150)
            try {
                focusRequesters[initialFocus.first][initialFocus.second].requestFocus()
            } catch (e: Exception) {}
        }
    }

    val tableModifier = if (scrollable) {
        Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
    } else {
        Modifier.fillMaxWidth()
    }

    Column(modifier = tableModifier
        .clip(RoundedCornerShape(12.dp))
        .background(Color.White)
        .border(1.dp, DetailTableBorder, RoundedCornerShape(12.dp))
    ) {
        Row(modifier = Modifier.fillMaxWidth().background(DetailPrimaryGreen)) {
            for (c in 0 until numCols) {
                DetailTableCell(
                    text = colTitles.getOrElse(c) { "C${c + 1}" }.uppercase(),
                    modifier = Modifier.weight(1f),
                    isHeader = true,
                    isColumnHeader = true,
                    fontSize = tableFontSize
                )
            }
        }

        for (r in 0 until numRows) {
            Row(modifier = Modifier.fillMaxWidth()) {
                for (c in 0 until numCols) {
                    val cellValue = cells.find { it.rowIndex == r && it.colIndex == c }?.value ?: 0.0

                    var textFieldValue by remember(sheet.id, r, c) {
                        val initialText = if (cellValue == 0.0) "0" else {
                            if (decimalPlaces > 0) {
                                (cellValue * Math.pow(10.0, decimalPlaces.toDouble())).toLong().toString()
                            } else {
                                cellValue.toLong().toString()
                            }
                        }
                        mutableStateOf(TextFieldValue(text = initialText, selection = TextRange(initialText.length)))
                    }

                    LaunchedEffect(cellValue) {
                        val expectedText = if (cellValue == 0.0) "0" else {
                            if (decimalPlaces > 0) {
                                (cellValue * Math.pow(10.0, decimalPlaces.toDouble())).toLong().toString()
                            } else {
                                cellValue.toLong().toString()
                            }
                        }
                        if (textFieldValue.text != expectedText) {
                            textFieldValue = TextFieldValue(text = expectedText, selection = TextRange(expectedText.length))
                        }
                    }

                    var isFocused by remember { mutableStateOf(false) }
                    val bringIntoViewRequester = remember { BringIntoViewRequester() }
                    val scope = rememberCoroutineScope()

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .background(if(isFocused) DetailLightGreen.copy(alpha = 0.3f) else Color.White)
                            .border(0.5.dp, DetailTableBorder)
                            .padding(2.dp)
                            .bringIntoViewRequester(bringIntoViewRequester)
                            .then(if (isFocused) Modifier.border(1.5.dp, DetailPrimaryGreen) else Modifier),
                        contentAlignment = Alignment.Center
                    ) {
                        NumberInputField(
                            value = textFieldValue,
                            onValueChange = { newValue ->
                                if (!isEditMode) return@NumberInputField
                                val input = newValue.text
                                val filtered = input.filter { it.isDigit() }.let {
                                    if (it.startsWith("0") && it.length > 1) it.substring(1) else it
                                }

                                if (filtered.length <= (maxIntegerDigits + decimalPlaces)) {
                                    val newText = if (filtered.isEmpty()) "0" else filtered
                                    textFieldValue = newValue.copy(
                                        text = newText,
                                        selection = TextRange(newText.length)
                                    )

                                    val rawValue = filtered.toDoubleOrNull() ?: 0.0
                                    val finalValue = if (decimalPlaces > 0) rawValue / Math.pow(10.0, decimalPlaces.toDouble()) else rawValue
                                    onCellValueChange(r, c, finalValue)

                                    if (filtered.length == (maxIntegerDigits + decimalPlaces)) {
                                        onSpeakNumber(finalValue)
                                        if (r < numRows - 1) {
                                            focusRequesters[r + 1][c].requestFocus()
                                        } else if (c < numCols - 1) {
                                            onVibrate()
                                            if (speakOnColumnComplete) {
                                                onSpeakText("hết cột")
                                            }
                                            focusRequesters[0][c + 1].requestFocus()
                                        } else {
                                            onVibrate()
                                            if (speakOnColumnComplete) {
                                                onSpeakText("hết cột hết bảng")
                                            }
                                            focusManager.clearFocus(force = true)
                                            onAutoNextSheet()
                                        }
                                    }
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .focusRequester(focusRequesters[r][c]),
                            readOnly = !isEditMode,
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.NumberPassword,
                                imeAction = ImeAction.None
                            ),
                            decorationBox = { innerTextField ->
                                Box(contentAlignment = Alignment.Center) {
                                    innerTextField()
                                }
                            },
                            textAlign = TextAlign.Center,
                            onFocusChange = {
                                isFocused = it
                                if (it) {
                                    onFocusCell(r, c)
                                    scope.launch {
                                        bringIntoViewRequester.bringIntoView()
                                    }
                                }
                            },
                            fontSize = tableFontSize
                        )
                    }
                }
            }
        }

        Row(modifier = Modifier.fillMaxWidth().background(SummaryGold).border(0.5.dp, DetailTableBorder)) {
            for (c in 0 until numCols) {
                val colSum = cells.filter { it.colIndex == c }.sumOf { it.value }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp)
                        .background(SummaryGold)
                        .border(1.dp, DetailBorderColor),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = df.format(colSum),
                        fontWeight = FontWeight.Black,
                        color = Color.Black,
                        fontSize = tableFontSize.sp,
                        maxLines = 1
                    )
                }
            }
        }

        val sheetSum = cells.sumOf { it.value }
        Row(modifier = Modifier.fillMaxWidth().background(SummaryGold)) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(56.dp)
                    .background(SummaryGold)
                    .border(1.dp, DetailBorderColor)
                    .padding(horizontal = 16.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Text(
                    "TỔNG BẢNG",
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.Black,
                    fontSize = tableFontSize.sp
                )
            }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(56.dp)
                    .background(SummaryGoldLight)
                    .border(1.dp, DetailBorderColor)
                    .padding(horizontal = 16.dp),
                contentAlignment = Alignment.CenterEnd
            ) {
                Text(
                    df.format(sheetSum),
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.Black,
                    fontSize = tableFontSize.sp
                )
            }
        }
    }
}

@Composable
fun DetailTableCell(
    text: String,
    modifier: Modifier = Modifier,
    isHeader: Boolean = false,
    isColumnHeader: Boolean = false,
    backgroundColor: Color? = null,
    textColor: Color? = null,
    fontSize: Float = 16f
) {
    Box(
        modifier = modifier
            .height(44.dp)
            .background(backgroundColor ?: Color.Transparent)
            .then(if(!isColumnHeader) Modifier.border(0.5.dp, DetailTableBorder) else Modifier),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            textAlign = TextAlign.Center,
            style = (if (isHeader) MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Black) else MaterialTheme.typography.bodyMedium)
                .copy(fontSize = fontSize.sp, fontWeight = FontWeight.Bold),
            color = textColor ?: if (isColumnHeader) Color.White else DetailTextColor,
            maxLines = 1,
            softWrap = false
        )
    }
}

@Composable
private fun NumberInputField(
    value: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    modifier: Modifier,
    readOnly: Boolean = false,
    keyboardOptions: KeyboardOptions,
    decorationBox: @Composable (innerTextField: @Composable () -> Unit) -> Unit,
    textAlign: TextAlign,
    onFocusChange: (Boolean) -> Unit,
    fontSize: Float = 16f
) {
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.onFocusChanged { onFocusChange(it.isFocused) },
        readOnly = readOnly,
        keyboardOptions = keyboardOptions,
        decorationBox = decorationBox,
        textStyle = TextStyle(
            textAlign = textAlign,
            fontWeight = FontWeight.Bold,
            color = DetailTextColor,
            fontSize = fontSize.sp
        ),
        cursorBrush = SolidColor(DetailPrimaryGreen)
    )
}

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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.MoreVert
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
    val remainingWeight = totalWeight - totalTare - totalImpurity
    val totalPrice = remainingWeight * (ticket?.unitPrice ?: 0)

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
                        var showMenu by remember { mutableStateOf(false) }

                        IconButton(onClick = {
                            ticket?.let { t ->
                                viewModel.exportTicketToExcelFixed(context, t, sheets, allCells, totalWeight, totalPrice)
                            }
                        }) {
                            Icon(Icons.Default.Share, contentDescription = "Xuất Excel", tint = DetailPrimaryGreen)
                        }

                        Box {
                            IconButton(onClick = { showMenu = true }) {
                                Icon(Icons.Default.MoreVert, contentDescription = "Thêm", tint = DetailPrimaryGreen)
                            }
                            DropdownMenu(
                                expanded = showMenu,
                                onDismissRequest = { showMenu = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Xuất file Excel (.xlsx)") },
                                    leadingIcon = { Icon(Icons.Default.Share, contentDescription = null) },
                                    onClick = {
                                        showMenu = false
                                        ticket?.let { t ->
                                            viewModel.exportTicketToExcelFixed(context, t, sheets, allCells, totalWeight, totalPrice)
                                        }
                                    }
                                )
                            }
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
                .verticalScroll(scrollState)
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(12.dp))

            DashboardContent(
                totalWeight = totalWeight,
                numBags = numBags,
                totalTare = totalTare,
                remainingWeight = remainingWeight,
                totalPrice = totalPrice,
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
                        scrollable = false
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
                                        totalPrice = priceData
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

            Spacer(modifier = Modifier.height(40.dp))
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
    var showPriceDialog by remember { mutableStateOf(false) }

    val depositAndAdvance = ticket.deposit
    val balance = totalPrice - depositAndAdvance

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = DashboardBlue),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth().clickable { onToggleExpand() },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.End
            ) {
                Icon(
                    imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = null,
                    tint = DetailPrimaryGreen
                )
            }

            AnimatedVisibility(visible = isExpanded) {
                Column(modifier = Modifier.padding(top = 4.dp)) {
                    DashboardItemRow("Tổng khối lượng (Gross)", String.format(Locale.US, "%.1f", totalWeight) + " kg", isBold = true)
                    DashboardItemRow("Tổng số mã", "$numBags mã")

                    HorizontalDivider(modifier = Modifier.padding(vertical = 2.dp), color = Color.White.copy(alpha = 0.5f))

                    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
                        Text("Trừ bì thùng/giỏ", style = MaterialTheme.typography.bodyMedium, color = Color.Black, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(2.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(44.dp)
                                .background(DetailPrimaryGreen, RoundedCornerShape(8.dp))
                                .border(1.dp, DetailPrimaryGreen.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                                .padding(horizontal = 8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxSize(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(String.format(Locale.US, "%.1f", totalTare) + " Kg", fontWeight = FontWeight.Bold, color = Color.White, modifier = Modifier.weight(1f))
                                Text("Trừ ${ticket.tarePerBag} kg / mã", color = Color.White, fontWeight = FontWeight.Bold)
                                IconButton(onClick = { showTareDialog = true }, modifier = Modifier.size(32.dp).padding(start = 4.dp)) {
                                    Icon(Icons.Default.Settings, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                                }
                            }
                        }
                    }

                    if (ticket.impurityPerTon > 0) {
                        val imp = (totalWeight / 1000.0) * ticket.impurityPerTon
                        DashboardItemRow("Trừ tạp chất (${ticket.impurityPerTon} kg/tấn)", String.format(Locale.US, "%.1f", imp) + " kg")
                    }

                    DashboardItemRow("Khối lượng còn lại (Net)", String.format(Locale.US, "%.1f", remainingWeight) + " kg", isBold = true)

                    HorizontalDivider(modifier = Modifier.padding(vertical = 2.dp), color = Color.White.copy(alpha = 0.5f))

                    DashboardEditableRow(
                        label = "Đơn giá (VNĐ/kg)",
                        value = if (ticket.unitPrice == 0) "" else ticket.unitPrice.toString(),
                        onValueChange = { onTicketUpdate(ticket.copy(unitPrice = it.toIntOrNull() ?: 0)) },
                        isEditMode = isEditMode,
                        focusRequester = unitPriceFocusRequester,
                        onSettingsClick = { showPriceDialog = true }
                    )

                    HorizontalDivider(modifier = Modifier.padding(vertical = 2.dp), color = Color.White.copy(alpha = 0.5f))

                    val priceCalculation = "${String.format(Locale.US, "%.1f", remainingWeight)} kg x ${DecimalFormat("#,###").format(ticket.unitPrice)} ="
                    DashboardItemRow("Thành tiền", "$priceCalculation ${DecimalFormat("#,###").format(totalPrice)} đ", isBold = true)

                    DashboardEditableRow(
                        label = "TIỀN CỌC, TIỀN ỨNG",
                        value = if (ticket.deposit == 0L) "" else ticket.deposit.toString(),
                        onValueChange = { onTicketUpdate(ticket.copy(deposit = it.toLongOrNull() ?: 0L)) },
                        isEditMode = isEditMode
                    )

                    Spacer(modifier = Modifier.height(2.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color.White.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                            .padding(8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("CÒN PHẢI TRẢ", fontWeight = FontWeight.Black, color = Color.Black)
                        Text(
                            "${DecimalFormat("#,###").format(balance)} đ",
                            fontWeight = FontWeight.Black,
                            color = BalanceRed
                        )
                    }
                }
            }
        }
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
                .height(44.dp)
                .background(backgroundColor, RoundedCornerShape(8.dp))
                .border(1.dp, backgroundColor.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                .padding(horizontal = 8.dp)
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
    scrollable: Boolean = true
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
                    isColumnHeader = true
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
                            }
                        )
                    }
                }
            }
        }

        Row(modifier = Modifier.fillMaxWidth().background(SummaryGold).border(0.5.dp, DetailTableBorder)) {
            for (c in 0 until numCols) {
                val colSum = cells.filter { it.colIndex == c }.sumOf { it.value }
                DetailTableCell(
                    text = df.format(colSum),
                    modifier = Modifier.weight(1f),
                    isHeader = true,
                    backgroundColor = SummaryGold,
                    textColor = Color.Black
                )
            }
        }

        val sheetSum = cells.sumOf { it.value }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(SummaryGold)
                .border(0.5.dp, DetailTableBorder)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("TỔNG BẢNG", fontWeight = FontWeight.ExtraBold, color = Color.Black)
            Text(df.format(sheetSum), fontWeight = FontWeight.ExtraBold, color = Color.Black)
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
    textColor: Color? = null
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
            style = if (isHeader) MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Black) else MaterialTheme.typography.bodyMedium,
            color = textColor ?: if (isColumnHeader) Color.White else DetailTextColor
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
    onFocusChange: (Boolean) -> Unit
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
            color = DetailTextColor
        ),
        cursorBrush = SolidColor(DetailPrimaryGreen)
    )
}

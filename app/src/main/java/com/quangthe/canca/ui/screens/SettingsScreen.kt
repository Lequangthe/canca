package com.quangthe.canca.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.quangthe.canca.data.AppSettings
import com.quangthe.canca.ui.theme.*
import com.quangthe.canca.viewmodel.FishViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: FishViewModel,
    onBack: () -> Unit
) {
    val appSettings by viewModel.appSettings.collectAsState()

    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Cấu hình bảng", "Tùy chọn")

    Scaffold(
        containerColor = SettingsAppBackground,
        topBar = {
            TopAppBar(
                title = { Text("Cài đặt", fontWeight = FontWeight.Bold, color = SettingsPrimaryGreen) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Quay lại", tint = SettingsPrimaryGreen)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = Color.White,
                contentColor = SettingsPrimaryGreen,
                indicator = { tabPositions ->
                    if (selectedTab < tabPositions.size) {
                        TabRowDefaults.SecondaryIndicator(
                            Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                            color = SettingsPrimaryGreen
                        )
                    }
                }
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { 
                            Text(
                                title, 
                                color = if (selectedTab == index) SettingsPrimaryGreen else Color.Gray,
                                fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal
                            ) 
                        }
                    )
                }
            }

            when (selectedTab) {
                0 -> TableConfigTab(viewModel, appSettings)
                1 -> OptionsTab(viewModel, appSettings)
            }
        }
    }
}

@Composable
fun TableConfigTab(viewModel: FishViewModel, settings: AppSettings) {
    var numCols by remember { mutableStateOf(settings.defaultNumCols.toString()) }
    var numRows by remember { mutableStateOf(settings.defaultNumRows.toString()) }

    LazyColumn(modifier = Modifier.padding(16.dp)) {
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Cấu hình mặc định cho bảng mới", style = MaterialTheme.typography.titleSmall, color = SettingsPrimaryGreen, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = numCols,
                        onValueChange = { numCols = it },
                        label = { Text("Số cột (1-10)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = SettingsPrimaryGreen,
                            focusedLabelColor = SettingsPrimaryGreen
                        )
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = numRows,
                        onValueChange = { numRows = it },
                        label = { Text("Số hàng (1-50)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = SettingsPrimaryGreen,
                            focusedLabelColor = SettingsPrimaryGreen
                        )
                    )
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = {
                    val cols = numCols.toIntOrNull()?.coerceIn(1, 10) ?: settings.defaultNumCols
                    val rows = numRows.toIntOrNull()?.coerceIn(1, 50) ?: settings.defaultNumRows
                    viewModel.updateAppSettings(settings.copy(defaultNumCols = cols, defaultNumRows = rows))
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = SettingsPrimaryGreen),
                shape = RoundedCornerShape(30.dp)
            ) {
                Text("Lưu cấu hình mặc định", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun OptionsTab(viewModel: FishViewModel, settings: AppSettings) {
    LazyColumn(modifier = Modifier.padding(16.dp)) {
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Quy cách nhập số", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = SettingsPrimaryGreen)
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Text("Phần nguyên tối đa:", style = MaterialTheme.typography.bodyMedium)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        (1..4).forEach { digits ->
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(end = 8.dp)) {
                                RadioButton(
                                    selected = settings.maxIntegerDigits == digits,
                                    onClick = { viewModel.updateAppSettings(settings.copy(maxIntegerDigits = digits)) },
                                    colors = RadioButtonDefaults.colors(selectedColor = SettingsPrimaryGreen)
                                )
                                Text(digits.toString())
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Số chữ số sau dấu phẩy:", style = MaterialTheme.typography.bodyMedium)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        (0..2).forEach { places ->
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(end = 12.dp)) {
                                RadioButton(
                                    selected = settings.decimalPlaces == places,
                                    onClick = { viewModel.updateAppSettings(settings.copy(decimalPlaces = places)) },
                                    colors = RadioButtonDefaults.colors(selectedColor = SettingsPrimaryGreen)
                                )
                                Text(places.toString())
                            }
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Giọng nói (TTS)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = SettingsPrimaryGreen)
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Text("Tốc độ đọc: ${String.format("%.1f", settings.ttsSpeechRate)}", style = MaterialTheme.typography.bodyMedium)
                    Slider(
                        value = settings.ttsSpeechRate,
                        onValueChange = { viewModel.updateAppSettings(settings.copy(ttsSpeechRate = it)) },
                        valueRange = 0.5f..2.0f,
                        steps = 15,
                        colors = SliderDefaults.colors(thumbColor = SettingsPrimaryGreen, activeTrackColor = SettingsPrimaryGreen)
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Cao độ (Pitch): ${String.format("%.1f", settings.ttsPitch)}", style = MaterialTheme.typography.bodyMedium)
                    Slider(
                        value = settings.ttsPitch,
                        onValueChange = { viewModel.updateAppSettings(settings.copy(ttsPitch = it)) },
                        valueRange = 0.5f..2.0f,
                        steps = 15,
                        colors = SliderDefaults.colors(thumbColor = SettingsPrimaryGreen, activeTrackColor = SettingsPrimaryGreen)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Cỡ chữ hiển thị trong bảng", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = SettingsPrimaryGreen)
                    Spacer(modifier = Modifier.height(12.dp))

                    Text("Kích thước: ${settings.tableFontSize.toInt()} sp", style = MaterialTheme.typography.bodyMedium)
                    Slider(
                        value = settings.tableFontSize,
                        onValueChange = { viewModel.updateAppSettings(settings.copy(tableFontSize = it)) },
                        valueRange = 12f..24f,
                        steps = 11,
                        colors = SliderDefaults.colors(thumbColor = SettingsPrimaryGreen, activeTrackColor = SettingsPrimaryGreen)
                    )

                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Xem trước (Preview):", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // Preview Box
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, Color.LightGray, RoundedCornerShape(8.dp))
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "125.5",
                            fontSize = settings.tableFontSize.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Black,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Cỡ chữ toàn ứng dụng", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = SettingsPrimaryGreen)
                    Spacer(modifier = Modifier.height(12.dp))

                    Text("Tỉ lệ: ${String.format("%.1f", settings.globalFontScale)}x", style = MaterialTheme.typography.bodyMedium)
                    Slider(
                        value = settings.globalFontScale,
                        onValueChange = { viewModel.updateAppSettings(settings.copy(globalFontScale = it)) },
                        valueRange = 0.8f..1.5f,
                        steps = 6,
                        colors = SliderDefaults.colors(thumbColor = SettingsPrimaryGreen, activeTrackColor = SettingsPrimaryGreen)
                    )
                    
                    Text(
                        "Lưu ý: Thay đổi này sẽ làm to/nhỏ tất cả các chữ trong ứng dụng.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Hỗ trợ nhập liệu", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = SettingsPrimaryGreen)
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    val options = listOf(
                        "Đọc số khi nhập xong 1 ô" to settings.speakOnCellComplete,
                        "Đọc khi hết cột/bảng" to settings.speakOnColumnComplete,
                        "Rung sau khi nhập xong một cột" to settings.vibrateOnColumnComplete
                    )
                    
                    options.forEach { (label, value) ->
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                            Checkbox(
                                checked = value,
                                onCheckedChange = { checked ->
                                    val newSettings = when(label) {
                                        "Đọc số khi nhập xong 1 ô" -> settings.copy(speakOnCellComplete = checked)
                                        "Đọc khi hết cột/bảng" -> settings.copy(speakOnColumnComplete = checked)
                                        "Rung sau khi nhập xong một cột" -> settings.copy(vibrateOnColumnComplete = checked)
                                        else -> settings
                                    }
                                    viewModel.updateAppSettings(newSettings)
                                },
                                colors = CheckboxDefaults.colors(checkedColor = SettingsPrimaryGreen)
                            )
                            Text(label)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            val context = LocalContext.current
            val restoreLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
                uri?.let { viewModel.restoreData(it, context) }
            }

            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Sao lưu & Khôi phục", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = SettingsPrimaryGreen)
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        val isBackingUp by viewModel.isBackingUp.collectAsState()
                        Button(
                            onClick = { viewModel.backupData(context) },
                            modifier = Modifier.weight(1f),
                            enabled = !isBackingUp,
                            colors = ButtonDefaults.buttonColors(containerColor = SettingsPrimaryGreen),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            if (isBackingUp) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    color = Color.White,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Icon(Icons.Default.Backup, contentDescription = null)
                            }
                            Spacer(Modifier.width(8.dp))
                            Text(if (isBackingUp) "Đang sao lưu..." else "Sao lưu (CSV)")
                        }

                        val isRestoring by viewModel.isRestoring.collectAsState()
                        OutlinedButton(
                            onClick = { restoreLauncher.launch("text/*") },
                            modifier = Modifier.weight(1f),
                            enabled = !isRestoring,
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = SettingsPrimaryGreen),
                            shape = RoundedCornerShape(8.dp),
                            border = ButtonDefaults.outlinedButtonBorder.copy(brush = androidx.compose.ui.graphics.SolidColor(SettingsPrimaryGreen))
                        ) {
                            if (isRestoring) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    color = SettingsPrimaryGreen,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Icon(Icons.Default.Restore, contentDescription = null)
                            }
                            Spacer(Modifier.width(8.dp))
                            Text(if (isRestoring) "Đang khôi phục..." else "Khôi phục")
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Lưu ý: Khôi phục sẽ xóa toàn bộ dữ liệu hiện tại và thay thế bằng dữ liệu từ tệp sao lưu.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                }
            }
        }
    }
}

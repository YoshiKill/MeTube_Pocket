package com.medialtube.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.medialtube.app.data.api.DownloadItem
import com.medialtube.app.ui.theme.AppThemeStyle
import com.medialtube.app.ui.theme.LocalExtendedColors
import com.medialtube.app.ui.theme.retro3DBorder

@Composable
fun MainScreen(viewModel: MainViewModel) {
    val currentScreen by viewModel.currentScreen.collectAsState()
    val extColors = LocalExtendedColors.current

    val backgroundModifier = when (extColors.retroType) {
        AppThemeStyle.RETRO_XP -> Modifier.background(
            brush = Brush.verticalGradient(
                colors = listOf(Color(0xFF2B78E4), Color(0xFF87CEEB), Color(0xFF68A34D), Color(0xFF458B00)),
                startY = 0f,
                endY = Float.POSITIVE_INFINITY
            )
        )
        AppThemeStyle.RETRO_98 -> Modifier.background(extColors.desktopBackground)
        else -> Modifier.background(MaterialTheme.colorScheme.background)
    }

    Scaffold(
        bottomBar = { AppBottomNavigation(currentScreen, viewModel::navigateTo) }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .then(backgroundModifier)
                .padding(innerPadding)
        ) {
            when (currentScreen) {
                Screen.DOWNLOADS -> DownloadsScreen(viewModel)
                Screen.NEW_VIDEO -> NewVideoScreen(viewModel)
                Screen.SETTINGS -> SettingsScreen(viewModel)
            }
        }
    }
}

@Composable
fun AppBottomNavigation(currentScreen: Screen, onNavigate: (Screen) -> Unit) {
    val extColors = LocalExtendedColors.current
    if (extColors.retroType == AppThemeStyle.RETRO_XP) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(Color(0xFF1F42F4), Color(0xFF245EDC), Color(0xFF0F2695))
                    )
                ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(Color(0xFF4CB151), Color(0xFF3C9E3F), Color(0xFF246125))
                        )
                    )
                    .clickable { onNavigate(Screen.SETTINGS) }
                    .padding(horizontal = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("Start", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            }
            Spacer(modifier = Modifier.width(8.dp))
            XpTaskbarButton("Downloads", currentScreen == Screen.DOWNLOADS) { onNavigate(Screen.DOWNLOADS) }
            XpTaskbarButton("New", currentScreen == Screen.NEW_VIDEO) { onNavigate(Screen.NEW_VIDEO) }
        }
    } else {
        NavigationBar(
            containerColor = if (extColors.isRetro) extColors.cardBackground else MaterialTheme.colorScheme.surface
        ) {
            NavigationBarItem(
                icon = { Icon(Icons.Default.List, "Downloads") },
                label = { Text("Загрузки") },
                selected = currentScreen == Screen.DOWNLOADS,
                onClick = { onNavigate(Screen.DOWNLOADS) }
            )
            NavigationBarItem(
                icon = { Icon(Icons.Default.Add, "New") },
                label = { Text("Новое") },
                selected = currentScreen == Screen.NEW_VIDEO,
                onClick = { onNavigate(Screen.NEW_VIDEO) }
            )
            NavigationBarItem(
                icon = { Icon(Icons.Default.Settings, "Settings") },
                label = { Text("Настройки") },
                selected = currentScreen == Screen.SETTINGS,
                onClick = { onNavigate(Screen.SETTINGS) }
            )
        }
    }
}

@Composable
fun XpTaskbarButton(text: String, selected: Boolean, onClick: () -> Unit) {
    val bgColor = if (selected) Color(0xFF1941A5) else Color.Transparent
    Box(
        modifier = Modifier
            .fillMaxHeight()
            .clickable(onClick = onClick)
            .background(bgColor)
            .padding(horizontal = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text, color = Color.White, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal)
    }
}

@Composable
fun RetroWindow(title: String, content: @Composable () -> Unit) {
    val extColors = LocalExtendedColors.current
    if (extColors.isRetro) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .retro3DBorder(
                    lightColor = extColors.borderLight,
                    darkColor = extColors.borderDark,
                    borderWidth = 2.dp
                )
                .background(extColors.cardBackground)
                .padding(2.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        if (extColors.retroType == AppThemeStyle.RETRO_XP)
                            Brush.verticalGradient(listOf(Color(0xFF0058EE), Color(0xFF003DD5)))
                        else Brush.horizontalGradient(listOf(Color(0xFF000080), Color(0xFF1084d0)))
                    )
                    .padding(horizontal = 4.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Row {
                    RetroWindowButton("_")
                    RetroWindowButton("□")
                    RetroWindowButton("✕")
                }
            }
            Box(modifier = Modifier.padding(12.dp)) {
                content()
            }
        }
    } else {
        Card(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(16.dp))
                content()
            }
        }
    }
}

@Composable
fun RetroWindowButton(symbol: String) {
    val extColors = LocalExtendedColors.current
    Box(
        modifier = Modifier
            .padding(start = 2.dp)
            .size(18.dp)
            .retro3DBorder(
                lightColor = extColors.borderLight,
                darkColor = extColors.borderDark,
                borderWidth = 1.dp
            )
            .background(extColors.cardBackground),
        contentAlignment = Alignment.Center
    ) {
        Text(symbol, fontSize = 10.sp, color = Color.Black, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun DownloadsScreen(viewModel: MainViewModel) {
    val downloads by viewModel.downloads.collectAsState()
    
    RetroWindow("Загрузки MeTube") {
        if (downloads.isEmpty()) {
            Text("Список пуст", modifier = Modifier.padding(16.dp))
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(downloads) { item ->
                    DownloadItemRow(item, viewModel)
                    Divider()
                }
            }
        }
    }
}

fun formatSpeed(speed: Double?): String {
    val bytes = speed ?: 0.0
    if (bytes <= 0.0) return ""
    val kb = bytes / 1024
    if (kb < 1024) return "${"%.1f".format(kb)} KB/s"
    val mb = kb / 1024
    return "${"%.1f".format(mb)} MB/s"
}

@Composable
fun DownloadItemRow(item: DownloadItem, viewModel: MainViewModel) {
    var menuExpanded by remember { mutableStateOf(false) }

    Box(modifier = Modifier
        .fillMaxWidth()
        .clickable { menuExpanded = true }
        .padding(vertical = 8.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = item.title ?: item.url ?: "Неизвестно", 
                fontWeight = FontWeight.Bold, 
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(4.dp))
            
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(text = "Статус: ${item.status ?: "pending"}", fontSize = 12.sp)
                if (item.status == "downloading") {
                    Text(text = formatSpeed(item.speed), fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                }
            }

            if (item.status == "downloading" && item.percent != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    LinearProgressIndicator(
                        progress = (item.percent / 100).toFloat().coerceIn(0f, 1f),
                        modifier = Modifier.weight(1f).height(6.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "${"%.1f".format(item.percent)}%", fontSize = 12.sp)
                }
            }

            if (!item.error.isNullOrEmpty()) {
                Text(text = item.error!!, color = Color.Red, fontSize = 11.sp, maxLines = 2)
            }
        }

        // Обновлённое меню, которое отправляет URL задачи
        DropdownMenu(
            expanded = menuExpanded,
            onDismissRequest = { menuExpanded = false }
        ) {
            val targetIdentifier = item.url ?: item.id
            
            if (item.status == "downloading" || item.status == "pending") {
                DropdownMenuItem(
                    text = { Text("Отменить загрузку") },
                    onClick = {
                        targetIdentifier?.let { viewModel.cancelAction(it) }
                        menuExpanded = false
                    }
                )
            } else {
                DropdownMenuItem(
                    text = { Text("Удалить из списка") },
                    onClick = {
                        targetIdentifier?.let { viewModel.clearAction(it) }
                        menuExpanded = false
                    }
                )
            }
        }
    }
}

@Composable
fun NewVideoScreen(viewModel: MainViewModel) {
    val url by viewModel.sharedUrl.collectAsState()
    val type by viewModel.downloadType.collectAsState()
    val quality by viewModel.quality.collectAsState()
    val format by viewModel.format.collectAsState()
    val codec by viewModel.codec.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    RetroWindow("Добавить загрузку") {
        Column {
            OutlinedTextField(
                value = url,
                onValueChange = { viewModel.sharedUrl.value = it },
                label = { Text("URL видео") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
            
            RetroDropdown("Тип", type, listOf("video", "audio", "thumbnail")) { viewModel.downloadType.value = it }
            
            if (type != "thumbnail") {
                RetroDropdown("Качество", quality, listOf("best", "1080p", "720p", "480p", "worst")) { viewModel.quality.value = it }
                
                val formatOptions = if (type == "audio") listOf("any", "mp3", "m4a", "flac", "wav") else listOf("any", "mp4", "mkv", "webm")
                RetroDropdown("Формат", format, formatOptions) { viewModel.format.value = it }
                
                val codecOptions = if (type == "audio") listOf("auto", "aac", "mp3", "opus") else listOf("auto", "h264", "h265", "vp9", "av1")
                RetroDropdown("Кодек", codec, codecOptions) { viewModel.codec.value = it }
            }

            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = { viewModel.submitDownload({}, {}) },
                enabled = url.isNotBlank() && !isLoading,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (isLoading) "Отправка..." else "Скачать")
            }
        }
    }
}

@Composable
fun RetroDropdown(label: String, selected: String, options: List<String>, onSelect: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Text(label, fontSize = 12.sp)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, Color.Gray)
                .background(MaterialTheme.colorScheme.surface)
                .clickable { expanded = true }
                .padding(12.dp)
        ) {
            Text(selected)
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option) },
                    onClick = {
                        onSelect(option)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
fun SettingsScreen(viewModel: MainViewModel) {
    val serverUrl by viewModel.serverUrl.collectAsState()
    val status by viewModel.connectionStatus.collectAsState()
    val selectedTheme by viewModel.selectedTheme.collectAsState()
    val logs by viewModel.logs.collectAsState()

    var showLogs by remember { mutableStateOf(false) }
    val clipboardManager = LocalClipboardManager.current

    RetroWindow("Свойства: MeTube Pocket") {
        Column {
            OutlinedTextField(
                value = serverUrl,
                onValueChange = { viewModel.saveServerUrl(it) },
                label = { Text("IP сервера (с http://)") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
            Button(onClick = { viewModel.testConnection() }, modifier = Modifier.fillMaxWidth()) {
                Text("Проверить соединение")
            }
            if (status != null) {
                Text(status!!, modifier = Modifier.padding(vertical = 8.dp), fontSize = 12.sp)
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text("Тема оформления", fontWeight = FontWeight.Bold)
            AppThemeStyle.values().forEach { theme ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(
                        selected = selectedTheme == theme,
                        onClick = { viewModel.saveTheme(theme) }
                    )
                    Text(theme.name)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            Divider()
            
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showLogs = !showLogs }
                    .padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Журнал событий (Логи)", fontWeight = FontWeight.Bold)
                Text(if (showLogs) "▲ Скрыть" else "▼ Показать", fontSize = 12.sp)
            }

            if (showLogs) {
                Button(
                    onClick = { clipboardManager.setText(AnnotatedString(logs.joinToString("\n"))) },
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                ) {
                    Text("Копировать весь лог")
                }
                
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .background(Color.Black)
                        .padding(8.dp)
                ) {
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        items(logs) { log ->
                            Text(log, color = Color.Green, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                        }
                    }
                }
            }
        }
    }
}

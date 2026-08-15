package com.medialtube.app.ui

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.medialtube.app.data.api.DownloadItem
import com.medialtube.app.ui.theme.AppThemeStyle
import com.medialtube.app.ui.theme.LocalExtendedColors
import com.medialtube.app.ui.theme.retro3DBorder

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(viewModel: MainViewModel) {
    val currentScreen by viewModel.currentScreen.collectAsState()
    val context = LocalContext.current
    val extendedColors = LocalExtendedColors.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        text = "me_dial_tube", 
                        fontWeight = FontWeight.Bold,
                        color = if (extendedColors.isRetro) extendedColors.windowHeaderFg else MaterialTheme.colorScheme.onPrimary
                    ) 
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = if (extendedColors.isRetro) extendedColors.windowHeaderBg else MaterialTheme.colorScheme.primary
                )
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                NavigationBarItem(
                    selected = currentScreen == Screen.DOWNLOADS,
                    onClick = { viewModel.navigateTo(Screen.DOWNLOADS) },
                    icon = { Icon(Icons.Default.List, contentDescription = "Загрузки") },
                    label = { Text("Загрузки") }
                )
                NavigationBarItem(
                    selected = currentScreen == Screen.SETTINGS,
                    onClick = { viewModel.navigateTo(Screen.SETTINGS) },
                    icon = { Icon(Icons.Default.Settings, contentDescription = "Настройки") },
                    label = { Text("Настройки") }
                )
            }
        },
        floatingActionButton = {
            if (currentScreen == Screen.DOWNLOADS) {
                FloatingActionButton(
                    onClick = { viewModel.navigateTo(Screen.NEW_VIDEO) },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    modifier = if (extendedColors.isRetro) {
                        Modifier.retro3DBorder(
                            lightColor = extendedColors.borderLight,
                            darkColor = extendedColors.borderDark
                        )
                    } else Modifier
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Добавить")
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
        ) {
            when (currentScreen) {
                Screen.DOWNLOADS -> DownloadsScreen(viewModel)
                Screen.NEW_VIDEO -> NewVideoScreen(viewModel)
                Screen.SETTINGS -> SettingsScreen(viewModel)
            }
        }
    }
}

// --- ЭКРАН 1: ЗАГРУЗКИ ---
@Composable
fun DownloadsScreen(viewModel: MainViewModel) {
    val downloads by viewModel.downloads.collectAsState()
    var selectedFilter by remember { mutableStateOf("Все") }

    val filteredList = when (selectedFilter) {
        "Активные" -> downloads.filter { it.status == "downloading" || it.status == "pending" }
        "Завершённые" -> downloads.filter { it.status == "finished" || it.status == "done" }
        "Ошибки" -> downloads.filter { it.status == "error" }
        else -> downloads
    }

    Column(modifier = Modifier.fillMaxSize().padding(8.dp)) {
        // Фильтры
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            listOf("Все", "Активные", "Завершённые", "Ошибки").forEach { filter ->
                FilterChip(
                    selected = selectedFilter == filter,
                    onClick = { selectedFilter = filter },
                    label = { Text(filter, fontSize = 12.sp) }
                )
            }
        }

        if (filteredList.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Список загрузок пуст", color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f))
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(filteredList) { item ->
                    DownloadCard(item)
                }
            }
        }
    }
}

@Composable
fun DownloadCard(item: DownloadItem) {
    val extendedColors = LocalExtendedColors.current

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (extendedColors.isRetro) {
                    Modifier.retro3DBorder(
                        lightColor = extendedColors.borderLight,
                        darkColor = extendedColors.borderDark
                    )
                } else Modifier
            ),
        colors = CardDefaults.cardColors(
            containerColor = if (extendedColors.isRetro) extendedColors.cardBackground else MaterialTheme.colorScheme.surface
        )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = item.title ?: item.url ?: "Без названия",
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(4.dp))

            val progress = (item.progress ?: 0.0) / 100.0
            if (item.status == "downloading") {
                LinearProgressIndicator(
                    progress = { progress.toFloat() },
                    modifier = Modifier.fillMaxWidth().height(8.dp)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Скачивается ${(progress * 100).toInt()}%", fontSize = 12.sp)
                    item.speed?.let {
                        Text("${(it / 1024 / 1024).toInt()} MB/s", fontSize = 12.sp)
                    }
                }
            } else if (item.status == "finished" || item.status == "done") {
                Text("✓ Завершено", color = Color(0xFF2E7D32), fontWeight = FontWeight.Medium)
            } else if (item.status == "error") {
                Text("✕ Ошибка: ${item.error ?: "Неизвестная ошибка"}", color = MaterialTheme.colorScheme.error)
            } else {
                Text("В очереди...", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
            }
        }
    }
}

// --- ЭКРАН 2: НОВОЕ ВИДЕО ---
@Composable
fun NewVideoScreen(viewModel: MainViewModel) {
    val context = LocalContext.current
    val url by viewModel.sharedUrl.collectAsState()
    val type by viewModel.downloadType.collectAsState()
    val quality by viewModel.quality.collectAsState()
    val format by viewModel.format.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Новое видео", fontSize = 20.sp, fontWeight = FontWeight.Bold)

        OutlinedTextField(
            value = url,
            onValueChange = { viewModel.sharedUrl.value = it },
            label = { Text("URL видео") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Text("Тип загрузки", fontWeight = FontWeight.SemiBold)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("Video", "Audio").forEach { item ->
                ElevatedButton(
                    onClick = { viewModel.downloadType.value = item },
                    colors = ButtonDefaults.elevatedButtonColors(
                        containerColor = if (type == item) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                        contentColor = if (type == item) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                    )
                ) {
                    Text(item)
                }
            }
        }

        Text("Качество", fontWeight = FontWeight.SemiBold)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("best", "1080p", "720p", "480p", "worst").forEach { q ->
                FilterChip(
                    selected = quality == q,
                    onClick = { viewModel.quality.value = q },
                    label = { Text(q) }
                )
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        Button(
            onClick = {
                viewModel.submitDownload(
                    onSuccess = { Toast.makeText(context, "Задание добавлено!", Toast.LENGTH_SHORT).show() },
                    onError = { err -> Toast.makeText(context, err, Toast.LENGTH_LONG).show() }
                )
            },
            enabled = !isLoading && url.isNotBlank(),
            modifier = Modifier.fillMaxWidth().height(50.dp)
        ) {
            if (isLoading) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(24.dp))
            } else {
                Text("Добавить загрузку")
            }
        }
    }
}

// --- ЭКРАН 3: НАСТРОЙКИ ---
@Composable
fun SettingsScreen(viewModel: MainViewModel) {
    val serverUrl by viewModel.serverUrl.collectAsState()
    val selectedTheme by viewModel.selectedTheme.collectAsState()
    val connectionStatus by viewModel.connectionStatus.collectAsState()
    var tempUrl by remember { mutableStateOf(serverUrl) }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Настройки", fontSize = 20.sp, fontWeight = FontWeight.Bold)

        Text("Сервер MeTube", fontWeight = FontWeight.SemiBold)
        OutlinedTextField(
            value = tempUrl,
            onValueChange = { tempUrl = it },
            label = { Text("Адрес сервера") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { viewModel.saveServerUrl(tempUrl) }) {
                Text("Сохранить")
            }
            OutlinedButton(onClick = { viewModel.testConnection() }) {
                Text("Проверить соединение")
            }
        }

        connectionStatus?.let {
            Text(it, color = if (it.startsWith("✓")) Color(0xFF2E7D32) else MaterialTheme.colorScheme.error)
        }

        Divider()

        Text("Тема интерфейса", fontWeight = FontWeight.SemiBold)
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            AppThemeStyle.values().forEach { theme ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(
                        selected = selectedTheme == theme,
                        onClick = { viewModel.saveTheme(theme) }
                    )
                    Text(
                        text = when (theme) {
                            AppThemeStyle.SYSTEM -> "Системная"
                            AppThemeStyle.LIGHT -> "Светлая"
                            AppThemeStyle.DARK -> "Тёмная"
                            AppThemeStyle.RETRO_98 -> "Ретро98 (Windows 98)"
                            AppThemeStyle.RETRO_XP -> "Ретро-XP (Windows XP)"
                        }
                    )
                }
            }
        }
    }
}

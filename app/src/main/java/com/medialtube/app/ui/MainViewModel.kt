package com.medialtube.app.ui

import android.app.Application
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.medialtube.app.data.SettingsRepository
import com.medialtube.app.data.api.AddRequest
import com.medialtube.app.data.api.DownloadItem
import com.medialtube.app.data.api.IdsRequest
import com.medialtube.app.data.api.NetworkClient
import com.medialtube.app.ui.theme.AppThemeStyle
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class Screen {
    DOWNLOADS, NEW_VIDEO, SETTINGS
}

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = SettingsRepository(application)

    private val _currentScreen = MutableStateFlow(Screen.DOWNLOADS)
    val currentScreen: StateFlow<Screen> = _currentScreen.asStateFlow()

    val sharedUrl = MutableStateFlow("")
    val downloadType = MutableStateFlow("video")
    val quality = MutableStateFlow("best")
    val format = MutableStateFlow("any")
    val codec = MutableStateFlow("auto")

    val serverUrl = MutableStateFlow("http://192.168.1.100:8081")
    val selectedTheme = MutableStateFlow(AppThemeStyle.SYSTEM)

    private val _downloads = MutableStateFlow<List<DownloadItem>>(emptyList())
    val downloads: StateFlow<List<DownloadItem>> = _downloads.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _connectionStatus = MutableStateFlow<String?>(null)
    val connectionStatus: StateFlow<String?> = _connectionStatus.asStateFlow()

    // --- Система логов ---
    private val _logs = MutableStateFlow<List<String>>(emptyList())
    val logs: StateFlow<List<String>> = _logs.asStateFlow()

    private fun addLog(message: String) {
        val time = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
        val logLine = "[$time] $message"
        _logs.value = _logs.value + logLine
    }

    private var pollingJob: Job? = null
    private var isFirstLoad = true

    init {
        addLog("Приложение запущено, читаем настройки...")
        viewModelScope.launch {
            repository.serverUrl.collect { url -> 
                serverUrl.value = url
                addLog("URL сервера загружен: $url")
                if (isFirstLoad) {
                    isFirstLoad = false
                    addLog("Авто-старт поллинга при запуске")
                    startPolling()
                }
            }
        }
        viewModelScope.launch {
            repository.selectedTheme.collect { themeStr ->
                selectedTheme.value = runCatching { AppThemeStyle.valueOf(themeStr) }.getOrDefault(AppThemeStyle.SYSTEM)
            }
        }
        viewModelScope.launch {
            downloadType.value = repository.defaultType.first()
            quality.value = repository.defaultQuality.first()
            format.value = repository.defaultFormat.first()
            codec.value = repository.defaultCodec.first()
        }
    }

    fun handleShareIntent(intent: Intent?) {
        if (intent?.action == Intent.ACTION_SEND && intent.type == "text/plain") {
            val text = intent.getStringExtra(Intent.EXTRA_TEXT) ?: return
            val extractedUrl = Regex("""https?://[^\s]+""").find(text)?.value ?: text.trim()
            if (extractedUrl.isNotEmpty()) {
                addLog("Перехвачена ссылка из Share: $extractedUrl")
                sharedUrl.value = extractedUrl
                _currentScreen.value = Screen.NEW_VIDEO
            }
        }
    }

    fun navigateTo(screen: Screen) {
        _currentScreen.value = screen
        if (screen == Screen.DOWNLOADS) startPolling() else stopPolling()
    }

    fun startPolling() {
        stopPolling()
        pollingJob = viewModelScope.launch {
            while (true) {
                fetchHistory()
                delay(3000)
            }
        }
    }

    fun stopPolling() {
        pollingJob?.cancel()
        pollingJob = null
    }

    suspend fun fetchHistory() {
        try {
            val response = NetworkClient.createApi(serverUrl.value).getHistory()
            if (response.isSuccessful) {
                val body = response.body()
                val list = mutableListOf<DownloadItem>()
                body?.queue?.let { list.addAll(it) }
                body?.done?.let { list.addAll(it) }
                body?.history?.let { list.addAll(it) }
                _downloads.value = list.distinctBy { it.id ?: it.title ?: it.url }
            } else {
                addLog("fetchHistory ошибка HTTP: ${response.code()}")
            }
        } catch (e: Exception) {
            if (_downloads.value.isEmpty()) addLog("Сбой fetchHistory: ${e.message}")
        }
    }

    fun submitDownload(onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true
            addLog("Отправка нового задания: ${sharedUrl.value}")
            try {
                val finalQuality = when {
                    downloadType.value == "audio" -> "audio"
                    quality.value == "best" -> null
                    else -> quality.value
                }
                val finalFormat = if (format.value == "any") null else format.value

                val response = NetworkClient.createApi(serverUrl.value).addDownload(
                    AddRequest(url = sharedUrl.value, quality = finalQuality, format = finalFormat)
                )
                _isLoading.value = false
                if (response.isSuccessful) {
                    addLog("Задание успешно добавлено (Код: ${response.code()})")
                    sharedUrl.value = ""
                    navigateTo(Screen.DOWNLOADS)
                    onSuccess()
                } else {
                    val err = "Ошибка сервера: ${response.code()}"
                    addLog(err)
                    onError(err)
                }
            } catch (e: Exception) {
                _isLoading.value = false
                val err = "Сервер MeTube недоступен: ${e.message}"
                addLog(err)
                onError(err)
            }
        }
    }

    // --- Обработчики меню с логированием ---
    
    fun cancelDownload(id: String) {
        viewModelScope.launch {
            addLog("-> Вызвана отмена (cancel) для ID: $id")
            try {
                val response = NetworkClient.createApi(serverUrl.value).deleteDownloads(IdsRequest(listOf(id)))
                addLog("<- Ответ отмены: Код ${response.code()}, Body: ${response.errorBody()?.string() ?: "OK"}")
                fetchHistory()
            } catch (e: Exception) {
                addLog("<- Исключение отмены: ${e.message}")
            }
        }
    }

    fun retryDownload(id: String) {
        viewModelScope.launch {
            addLog("-> Вызван повтор (retry) для ID: $id")
            try {
                val response = NetworkClient.createApi(serverUrl.value).retryDownloads(IdsRequest(listOf(id)))
                addLog("<- Ответ повтора: Код ${response.code()}")
                fetchHistory()
            } catch (e: Exception) {
                addLog("<- Исключение повтора: ${e.message}")
            }
        }
    }

    fun deleteFromHistory(id: String) {
        viewModelScope.launch {
            addLog("-> Вызвано удаление из истории для ID: $id")
            try {
                val response = NetworkClient.createApi(serverUrl.value).deleteDownloads(IdsRequest(listOf(id)))
                addLog("<- Ответ удаления: Код ${response.code()}, Body: ${response.errorBody()?.string() ?: "OK"}")
                fetchHistory()
            } catch (e: Exception) {
                addLog("<- Исключение удаления: ${e.message}")
            }
        }
    }

    fun deleteWithFile(id: String) {
        viewModelScope.launch {
            addLog("-> Вызвано полное удаление с файлом для ID: $id")
            try {
                val response = NetworkClient.createApi(serverUrl.value).deleteFiles(IdsRequest(listOf(id)))
                addLog("<- Ответ полного удаления: Код ${response.code()}, Body: ${response.errorBody()?.string() ?: "OK"}")
                fetchHistory()
            } catch (e: Exception) {
                addLog("<- Исключение полного удаления: ${e.message}")
            }
        }
    }

    fun testConnection() {
        viewModelScope.launch {
            val targetUrl = if (serverUrl.value.endsWith("/")) serverUrl.value else "${serverUrl.value}/"
            _connectionStatus.value = "Запрос к: ${targetUrl}history ..."
            addLog("Тест соединения с: ${targetUrl}history")
            try {
                val response = NetworkClient.createApi(serverUrl.value).getHistory()
                if (response.isSuccessful) {
                    _connectionStatus.value = "✓ Сервер доступен (Код 200 OK)"
                    addLog("Тест успешен: HTTP 200")
                } else {
                    val errorBody = response.errorBody()?.string()?.take(100) ?: "нет деталей"
                    _connectionStatus.value = "✕ Ошибка HTTP ${response.code()}: $errorBody"
                    addLog("Тест провален: HTTP ${response.code()} - $errorBody")
                }
            } catch (e: Exception) {
                _connectionStatus.value = "✕ Ошибка сети: ${e.localizedMessage}"
                addLog("Тест провален (Исключение): ${e.localizedMessage}")
            }
        }
    }

    fun saveServerUrl(url: String) {
        serverUrl.value = url
        viewModelScope.launch { repository.saveServerUrl(url) }
    }

    fun saveTheme(theme: AppThemeStyle) {
        selectedTheme.value = theme
        viewModelScope.launch { repository.saveTheme(theme.name) }
    }

    fun saveDefaults(type: String, q: String, f: String, c: String) {
        downloadType.value = type
        quality.value = q
        format.value = f
        codec.value = c
        viewModelScope.launch { repository.saveDefaults(type, q, f, c) }
    }
}

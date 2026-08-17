package com.medialtube.app.ui

import android.app.Application
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.google.gson.JsonElement
import com.medialtube.app.data.SettingsRepository
import com.medialtube.app.data.api.ActionRequest
import com.medialtube.app.data.api.AddRequest
import com.medialtube.app.data.api.DownloadItem
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
        addLog("Запуск MainViewModel...")
        viewModelScope.launch {
            repository.serverUrl.collect { url ->
                serverUrl.value = url
                if (isFirstLoad) {
                    isFirstLoad = false
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
                val gson = Gson()

                fun parseSection(element: JsonElement?) {
                    if (element == null) return
                    if (element.isJsonObject) {
                        element.asJsonObject.entrySet().forEach { entry ->
                            val item = gson.fromJson(entry.value, DownloadItem::class.java)
                            list.add(item)
                        }
                    } else if (element.isJsonArray) {
                        element.asJsonArray.forEach { arrayElem ->
                            if (arrayElem.isJsonObject) {
                                val item = gson.fromJson(arrayElem, DownloadItem::class.java)
                                list.add(item)
                            }
                        }
                    }
                }

                parseSection(body?.get("queue"))
                parseSection(body?.get("done"))
                parseSection(body?.get("history"))

                // MeTube оперирует URL для идентификации, фильтруем по нему
                _downloads.value = list.distinctBy { it.url ?: it.id }
            }
        } catch (e: Exception) {
            if (_downloads.value.isEmpty()) addLog("Сбой истории: ${e.message}")
        }
    }

    fun submitDownload(onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true
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
                    sharedUrl.value = ""
                    navigateTo(Screen.DOWNLOADS)
                    onSuccess()
                } else {
                    onError("Ошибка сервера: ${response.code()}")
                }
            } catch (e: Exception) {
                _isLoading.value = false
                onError("Сервер недоступен")
            }
        }
    }

    // ВАЖНО: MeTube использует поле URL для команд cancel и clear
    fun cancelAction(url: String) {
        viewModelScope.launch {
            addLog("-> Отмена загрузки (cancel) для: $url")
            try {
                val response = NetworkClient.createApi(serverUrl.value).cancelDownload(ActionRequest(url))
                addLog("<- Ответ cancel: ${response.code()}")
                fetchHistory()
            } catch (e: Exception) {
                addLog("<- Ошибка cancel: ${e.message}")
            }
        }
    }

    fun clearAction(url: String) {
        viewModelScope.launch {
            addLog("-> Очистка истории (clear) для: $url")
            try {
                val response = NetworkClient.createApi(serverUrl.value).clearDownload(ActionRequest(url))
                addLog("<- Ответ clear: ${response.code()}")
                fetchHistory()
            } catch (e: Exception) {
                addLog("<- Ошибка clear: ${e.message}")
            }
        }
    }

    fun testConnection() {
        viewModelScope.launch {
            val targetUrl = if (serverUrl.value.endsWith("/")) serverUrl.value else "${serverUrl.value}/"
            _connectionStatus.value = "Запрос к: ${targetUrl}history ..."
            addLog("Тест соединения...")
            try {
                val response = NetworkClient.createApi(serverUrl.value).getHistory()
                if (response.isSuccessful) {
                    _connectionStatus.value = "✓ Сервер доступен"
                    addLog("Тест успешен: 200 OK")
                } else {
                    _connectionStatus.value = "✕ Ошибка HTTP ${response.code()}"
                    addLog("Тест провален: HTTP ${response.code()}")
                }
            } catch (e: Exception) {
                _connectionStatus.value = "✕ Ошибка сети"
                addLog("Тест провален: ${e.localizedMessage}")
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

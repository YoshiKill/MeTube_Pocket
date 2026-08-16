package com.medialtube.app.ui

import android.app.Application
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.medialtube.app.data.SettingsRepository
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

enum class Screen {
    DOWNLOADS, NEW_VIDEO, SETTINGS
}

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = SettingsRepository(application)

    private val _currentScreen = MutableStateFlow(Screen.DOWNLOADS)
    val currentScreen: StateFlow<Screen> = _currentScreen.asStateFlow()

    // Настройки загрузки
    val sharedUrl = MutableStateFlow("")
    val downloadType = MutableStateFlow("video") // video, audio, thumbnail
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

    private var pollingJob: Job? = null

    init {
        viewModelScope.launch {
            repository.serverUrl.collect { url -> serverUrl.value = url }
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
            val extractedUrl = extractUrl(text)
            if (extractedUrl.isNotEmpty()) {
                sharedUrl.value = extractedUrl
                _currentScreen.value = Screen.NEW_VIDEO
            }
        }
    }

    private fun extractUrl(text: String): String {
        val regex = Regex("""https?://[^\s]+""")
        return regex.find(text)?.value ?: text.trim()
    }

    fun navigateTo(screen: Screen) {
        _currentScreen.value = screen
        if (screen == Screen.DOWNLOADS) {
            startPolling()
        } else {
            stopPolling()
        }
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

    private suspend fun fetchHistory() {
        try {
            val api = NetworkClient.createApi(serverUrl.value)
            val response = api.getHistory()
            if (response.isSuccessful) {
                val body = response.body()
                val list = mutableListOf<DownloadItem>()
                body?.queue?.values?.let { list.addAll(it) }
                body?.done?.values?.let { list.addAll(it) }
                body?.history?.values?.let { list.addAll(it) }
                _downloads.value = list.distinctBy { it.id ?: it.title ?: it.url }
            }
        } catch (_: Exception) {
            // Игнорируем фоновые ошибки сети при polling
        }
    }

    fun submitDownload(onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val api = NetworkClient.createApi(serverUrl.value)
                val response = api.addDownload(
                    AddRequest(
                        url = sharedUrl.value,
                        quality = quality.value,
                        format = format.value,
                        downloadType = downloadType.value
                    )
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
                onError("Сервер MeTube недоступен")
            }
        }
    }

    fun testConnection() {
        viewModelScope.launch {
            val targetUrl = if (serverUrl.value.endsWith("/")) serverUrl.value else "${serverUrl.value}/"
            _connectionStatus.value = "Запрос к: ${targetUrl}history ..."
            try {
                val api = NetworkClient.createApi(serverUrl.value)
                val response = api.getHistory()
                if (response.isSuccessful) {
                    _connectionStatus.value = "✓ Сервер доступен (Код 200 OK)"
                } else {
                    val errorBody = response.errorBody()?.string()?.take(100) ?: "нет деталей"
                    _connectionStatus.value = "✕ Ошибка HTTP ${response.code()}: $errorBody\nURL: ${targetUrl}history"
                }
            } catch (e: Exception) {
                _connectionStatus.value = "✕ Ошибка сети: ${e.localizedMessage ?: e.javaClass.simpleName}\nПроверьте доступность IP и порта"
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

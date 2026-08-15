package com.medialtube.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.medialtube.app.ui.MainScreen
import com.medialtube.app.ui.MainViewModel
import com.medialtube.app.ui.theme.MeDialTubeTheme

class MainActivity : ComponentActivity() {

    // viewModels() автоматически создаст и привяжет MainViewModel к жизненному циклу Activity
    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Обрабатываем интент, если приложение было запущено через "Поделиться"
        viewModel.handleShareIntent(intent)

        setContent {
            // Подписываемся на изменение темы. 
            // При выборе Ретро98 или Ретро-XP интерфейс перерисуется мгновенно без перезапуска.
            val themeStyle by viewModel.selectedTheme.collectAsState()

            MeDialTubeTheme(themeStyle = themeStyle) {
                MainScreen(viewModel = viewModel)
            }
        }
    }

    // Этот метод обязательно нужен! Он вызывается, если приложение уже висит в памяти,
    // и пользователь снова нажал "Поделиться -> me_dial_tube" в YouTube.
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent) // Обновляем текущий интент Activity
        viewModel.handleShareIntent(intent)
    }
}

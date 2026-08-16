package com.medialtube.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

enum class AppThemeStyle {
    SYSTEM, LIGHT, DARK, RETRO_98, RETRO_XP
}

// Расширенные параметры стиля для ретро-дизайна
data class ExtendedColors(
    val isRetro: Boolean = false,
    val retroType: AppThemeStyle = AppThemeStyle.SYSTEM,
    val windowHeaderBg: Color = Color.Unspecified,
    val windowHeaderFg: Color = Color.Unspecified,
    val borderLight: Color = Color.Unspecified,
    val borderDark: Color = Color.Unspecified,
    val cardBackground: Color = Color.Unspecified,
    val desktopBackground: Color = Color.Unspecified, // Фон рабочего стола
    val xpTaskbarBlue: Color = Color.Unspecified,     // Панель задач XP
    val xpStartGreen: Color = Color.Unspecified       // Кнопка Пуск XP
)

val LocalExtendedColors = staticCompositionLocalOf { ExtendedColors() }

private val LightColors = lightColorScheme(
    primary = Color(0xFFE53935), // Акцент в стиле YouTube
    secondary = Color(0xFF546E7A),
    background = Color(0xFFF5F5F5),
    surface = Color.White,
    onPrimary = Color.White,
    onBackground = Color(0xFF212121),
    onSurface = Color(0xFF212121)
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFEF5350),
    secondary = Color(0xFF78909C),
    background = Color(0xFF121212),
    surface = Color(0xFF1E1E1E),
    onPrimary = Color.Black,
    onBackground = Color(0xFFE0E0E0),
    onSurface = Color(0xFFE0E0E0)
)

private val Retro98Colors = lightColorScheme(
    primary = Color(0xFF000080),     
    secondary = Color(0xFF808080),   
    background = Color(0xFF008080),  // Бирюзовый рабочий стол 98
    surface = Color(0xFFC0C0C0),     // Серые окна
    onPrimary = Color.White,
    onBackground = Color.Black,
    onSurface = Color.Black
)

private val RetroXPColors = lightColorScheme(
    primary = Color(0xFF0055EA),     
    secondary = Color(0xFF245EDC),   
    background = Color(0xFF87CEEB),  // Голубое небо XP
    surface = Color(0xFFECE9D8),     // Окна XP
    onPrimary = Color.White,
    onBackground = Color.Black,
    onSurface = Color.Black
)

@Composable
fun MeDialTubeTheme(
    themeStyle: AppThemeStyle = AppThemeStyle.SYSTEM,
    content: @Composable () -> Unit
) {
    val darkTheme = isSystemInDarkTheme()
    
    val colorScheme = when (themeStyle) {
        AppThemeStyle.SYSTEM -> if (darkTheme) DarkColors else LightColors
        AppThemeStyle.LIGHT -> LightColors
        AppThemeStyle.DARK -> DarkColors
        AppThemeStyle.RETRO_98 -> Retro98Colors
        AppThemeStyle.RETRO_XP -> RetroXPColors
    }

    val extendedColors = when (themeStyle) {
        AppThemeStyle.RETRO_98 -> ExtendedColors(
            isRetro = true,
            retroType = AppThemeStyle.RETRO_98,
            windowHeaderBg = Color(0xFF000080),
            windowHeaderFg = Color.White,
            borderLight = Color.White,
            borderDark = Color(0xFF808080),
            cardBackground = Color(0xFFC0C0C0),
            desktopBackground = Color(0xFF008080)
        )
        AppThemeStyle.RETRO_XP -> ExtendedColors(
            isRetro = true,
            retroType = AppThemeStyle.RETRO_XP,
            windowHeaderBg = Color(0xFF0055EA),
            windowHeaderFg = Color.White,
            borderLight = Color(0xFF0053E1),
            borderDark = Color(0xFF003C9D),
            cardBackground = Color(0xFFECE9D8),
            desktopBackground = Color(0xFF87CEEB), // Небо "Безмятежности"
            xpTaskbarBlue = Color(0xFF245EDC),
            xpStartGreen = Color(0xFF3C9E3F)
        )
        else -> ExtendedColors(isRetro = false)
    }

    CompositionLocalProvider(
        LocalExtendedColors provides extendedColors
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            content = content
        )
    }
}

// Модификатор для рисования классического 3D-бордюра
fun Modifier.retro3DBorder(
    lightColor: Color = Color.White,
    darkColor: Color = Color(0xFF808080),
    borderWidth: Dp = 2.dp,
    pressed: Boolean = false
): Modifier = this.drawBehind {
    val strokeWidth = borderWidth.toPx()
    val topLeft = if (pressed) darkColor else lightColor
    val bottomRight = if (pressed) lightColor else darkColor

    drawLine(topLeft, Offset(0f, strokeWidth / 2), Offset(size.width, strokeWidth / 2), strokeWidth)
    drawLine(topLeft, Offset(strokeWidth / 2, 0f), Offset(strokeWidth / 2, size.height), strokeWidth)
    drawLine(bottomRight, Offset(0f, size.height - strokeWidth / 2), Offset(size.width, size.height - strokeWidth / 2), strokeWidth)
    drawLine(bottomRight, Offset(size.width - strokeWidth / 2, 0f), Offset(size.width - strokeWidth / 2, size.height), strokeWidth)
}

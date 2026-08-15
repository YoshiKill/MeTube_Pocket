package com.medialtube.app.ui.theme

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalOf
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

// Типы доступных тем
enum class AppThemeStyle {
    SYSTEM, LIGHT, DARK, RETRO_98, RETRO_XP
}

// Дополнительные параметры стиля для ретро-дизайна
data class ExtendedColors(
    val isRetro: Boolean = false,
    val retroType: AppThemeStyle = AppThemeStyle.SYSTEM,
    val windowHeaderBg: Color = Color.Unspecified,
    val windowHeaderFg: Color = Color.Unspecified,
    val borderLight: Color = Color.Unspecified,
    val borderDark: Color = Color.Unspecified,
    val cardBackground: Color = Color.Unspecified
)

val LocalExtendedColors = staticCompositionLocalOf { ExtendedColors() }

// Цветовые палитры
private val LightColors = lightColorScheme(
    primary = Color(0xFF4A6572),
    secondary = Color(0xFF344955),
    background = Color(0xFFF5F5F5),
    surface = Color.White,
    onPrimary = Color.White,
    onBackground = Color(0xFF212121),
    onSurface = Color(0xFF212121)
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF78909C),
    secondary = Color(0xFF90A4AE),
    background = Color(0xFF1E1E1E),
    surface = Color(0xFF2D2D2D),
    onPrimary = Color.Black,
    onBackground = Color(0xFFE0E0E0),
    onSurface = Color(0xFFE0E0E0)
)

// Палитра Windows 98
private val Retro98Colors = lightColorScheme(
    primary = Color(0xFF000080),     // Классический синий заголовок
    secondary = Color(0xFF808080),   // Серый рамки
    background = Color(0xFFC0C0C0),  // Серый фон Windows 98
    surface = Color(0xFFFFFFFF),     // Белые области ввода/списков
    onPrimary = Color.White,
    onBackground = Color.Black,
    onSurface = Color.Black
)

// Палитра Windows XP (Luna Blue)
private val RetroXPColors = lightColorScheme(
    primary = Color(0xFF0055EA),     // Синяя шапка XP
    secondary = Color(0xFF225AD6),   // Акцент XP
    background = Color(0xFFECE9D8),  // Светло-серый/бежевый XP
    surface = Color(0xFFFFFFFF),
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
            cardBackground = Color(0xFFC0C0C0)
        )
        AppThemeStyle.RETRO_XP -> ExtendedColors(
            isRetro = true,
            retroType = AppThemeStyle.RETRO_XP,
            windowHeaderBg = Color(0xFF0055EA),
            windowHeaderFg = Color.White,
            borderLight = Color(0xFF0053E1),
            borderDark = Color(0xFF003C9D),
            cardBackground = Color(0xFFF7F5EF)
        )
        else -> ExtendedColors(isRetro = false)
    }

    androidx.compose.runtime.CompositionLocalProvider(
        LocalExtendedColors provides extendedColors
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            content = content
        )
    }
}

// Модификатор для рисования классического 3D-бордюра Win98/WinXP
fun Modifier.retro3DBorder(
    lightColor: Color = Color.White,
    darkColor: Color = Color(0xFF808080),
    borderWidth: Dp = 2.dp,
    pressed: Boolean = false
): Modifier = this.drawBehind {
    val strokeWidth = borderWidth.toPx()
    val topLeft = if (pressed) darkColor else lightColor
    val bottomRight = if (pressed) lightColor else darkColor

    // Top
    drawLine(topLeft, Offset(0f, strokeWidth / 2), Offset(size.width, strokeWidth / 2), strokeWidth)
    // Left
    drawLine(topLeft, Offset(strokeWidth / 2, 0f), Offset(strokeWidth / 2, size.height), strokeWidth)
    // Bottom
    drawLine(bottomRight, Offset(0f, size.height - strokeWidth / 2), Offset(size.width, size.height - strokeWidth / 2), strokeWidth)
    // Right
    drawLine(bottomRight, Offset(size.width - strokeWidth / 2, 0f), Offset(size.width - strokeWidth / 2, size.height), strokeWidth)
}

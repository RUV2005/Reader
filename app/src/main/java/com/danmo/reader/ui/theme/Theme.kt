package com.danmo.reader.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

/**
 * 高对比度配色方案（针对弱视用户）
 */
private val HighContrastColorScheme = darkColorScheme(
    primary = Color(0xFFFFFF00),       // 高亮黄
    onPrimary = Color.Black,
    primaryContainer = Color(0xFFFFFF00).copy(alpha = 0.2f),
    onPrimaryContainer = Color(0xFFFFFF00),
    secondary = Color(0xFF00FF00),     // 高亮绿
    onSecondary = Color.Black,
    background = Color.Black,
    onBackground = Color.White,
    surface = Color(0xFF121212),
    onSurface = Color.White,
    surfaceVariant = Color(0xFF2A2A2A),
    onSurfaceVariant = Color(0xFFDDDDDD),
    outline = Color(0xFFFFFF00)
)

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF4A6FA5),
    secondary = Color(0xFF6B8CBB),
    tertiary = Color(0xFF8B9DC3),
    background = Color(0xFF121212),
    surface = Color(0xFF1E1E1E),
    onPrimary = Color.White,
    onBackground = Color.White,
    onSurface = Color.White
)

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF4A6FA5),
    secondary = Color(0xFF6B8CBB),
    tertiary = Color(0xFF8B9DC3),
    background = Color(0xFFF5F5F5),
    surface = Color.White,
    onPrimary = Color.White,
    onBackground = Color(0xFF333333),
    onSurface = Color(0xFF333333)
)

@Composable
fun ReaderTheme(
    themeSetting: String = "system",
    isHighContrast: Boolean = false,
    dynamicColor: Boolean = false, // 默认关闭动态取色，优先保证品牌一致性
    content: @Composable () -> Unit
) {
    val darkTheme = when (themeSetting) {
        "dark" -> true
        "light" -> false
        else -> isSystemInDarkTheme()
    }

    val colorScheme = when {
        isHighContrast -> HighContrastColorScheme
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    // 更新系统状态栏和导航栏颜色
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            window.navigationBarColor = colorScheme.background.toArgb()
            
            // 设置状态栏图标颜色（深色背景用白图标，浅色背景用黑图标）
            val isDarkBackground = isHighContrast || darkTheme
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !isDarkBackground
            WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = !isDarkBackground
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

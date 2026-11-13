package com.hamza.reelcraft.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val ReelDarkColorScheme = darkColorScheme(
    primary = ReelPrimaryViolet,
    secondary = ReelSecondaryCyan,
    background = ReelBackground,
    surface = ReelSurface,
    surfaceVariant = ReelSurfaceVariant,
    onBackground = ReelTextPrimary,
    onSurface = ReelTextPrimary,
    error = ReelError
)

@Composable
fun ReelCraftTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = ReelDarkColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            window.navigationBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = false
                isAppearanceLightNavigationBars = false
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = ReelTypography,
        shapes = ReelShapes,
        content = content
    )
}

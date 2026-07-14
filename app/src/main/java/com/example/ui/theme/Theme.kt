package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = CosmosPrimary,
    secondary = CosmosSecondary,
    tertiary = CosmosTertiary,
    background = CosmosBackground,
    surface = CosmosSurface,
    surfaceVariant = CosmosSurfaceVariant,
    error = CosmosError,
    onPrimary = CosmosBackground,
    onSecondary = CosmosBackground,
    onTertiary = CosmosBackground,
    onBackground = androidx.compose.ui.graphics.Color.White,
    onSurface = androidx.compose.ui.graphics.Color.White,
    onSurfaceVariant = androidx.compose.ui.graphics.Color.LightGray
)

private val LightColorScheme = lightColorScheme(
    primary = CosmosPrimaryLight,
    secondary = CosmosSecondaryLight,
    tertiary = CosmosTertiaryLight,
    background = CosmosBackgroundLight,
    surface = CosmosSurfaceLight,
    surfaceVariant = CosmosSurfaceVariantLight,
    error = CosmosError,
    onPrimary = androidx.compose.ui.graphics.Color.White,
    onSecondary = androidx.compose.ui.graphics.Color.White,
    onTertiary = androidx.compose.ui.graphics.Color.White,
    onBackground = androidx.compose.ui.graphics.Color.Black,
    onSurface = androidx.compose.ui.graphics.Color.Black,
    onSurfaceVariant = androidx.compose.ui.graphics.Color.DarkGray
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = false, // Disable dynamic colors by default to preserve the beautiful Cosmos signature brand
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

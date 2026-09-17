package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = BentoTerracottaLight,
    onPrimary = Color(0xFF381005),
    primaryContainer = BentoTerracottaDark,
    onPrimaryContainer = BentoPeachContainer,
    secondary = BentoOliveLight,
    onSecondary = Color(0xFF241A00),
    secondaryContainer = BentoOliveSecondary,
    onSecondaryContainer = BentoOliveContainer,
    tertiary = BudgetAmber,
    onTertiary = Color.White,
    background = BentoDarkBackground,
    onBackground = Color(0xFFEDE0DD),
    surface = BentoDarkSurface,
    onSurface = Color(0xFFEDE0DD),
    surfaceVariant = BentoDarkSurfaceVariant,
    onSurfaceVariant = Color(0xFFD8C2BC),
    error = ExpenseRed,
    onError = Color.White,
    errorContainer = Color(0xFF93000A),
    onErrorContainer = ExpenseRedContainer,
    outline = BentoDarkCardBorder
)

private val LightColorScheme = lightColorScheme(
    primary = BentoTerracottaPrimary,
    onPrimary = Color.White,
    primaryContainer = BentoPeachContainer,
    onPrimaryContainer = BentoOnPeachContainer,
    secondary = BentoOliveSecondary,
    onSecondary = Color.White,
    secondaryContainer = BentoOliveContainer,
    onSecondaryContainer = BentoOnOliveContainer,
    tertiary = BudgetAmber,
    onTertiary = Color.White,
    background = BentoLightBackground,
    onBackground = Color(0xFF201A19),
    surface = BentoLightSurface,
    onSurface = Color(0xFF201A19),
    surfaceVariant = BentoLightSurfaceVariant,
    onSurfaceVariant = Color(0xFF53433F),
    error = ExpenseRed,
    onError = Color.White,
    errorContainer = ExpenseRedContainer,
    onErrorContainer = OnExpenseRedContainer,
    outline = BentoLightCardBorder
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Use our brand colors for consistent financial visual identity
    content: @Composable () -> Unit
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

package eu.tutorials.chefproj.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val LightColorScheme = lightColorScheme(
    primary = ChefOrange,
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = CreamDeep,
    onPrimaryContainer = ChefOrangeDark,

    secondary = HerbGreen,
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFD1F0D6),
    onSecondaryContainer = HerbGreenDark,

    tertiary = Gold,
    onTertiary = WarmGray900,
    tertiaryContainer = Color(0xFFFFF3C4),
    onTertiaryContainer = Color(0xFF7A5A00),

    error = ErrorRed,
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFFE4E4),
    onErrorContainer = Color(0xFF8B0000),

    background = LightBg,
    onBackground = WarmGray900,
    surface = LightSurface,
    onSurface = WarmGray900,
    surfaceVariant = LightSurfaceVar,
    onSurfaceVariant = WarmGray600,

    outline = WarmGray300,
    outlineVariant = WarmGray200,
    scrim = Color(0x99000000),
    inverseSurface = WarmGray800,
    inverseOnSurface = WarmGray100,
    inversePrimary = ChefOrangeLight,
    surfaceTint = ChefOrange
)

private val DarkColorScheme = darkColorScheme(
    primary = ChefOrangeLight,
    onPrimary = WarmGray900,
    primaryContainer = ChefOrangeDark,
    onPrimaryContainer = Color(0xFFFFDDCC),

    secondary = HerbGreenLight,
    onSecondary = WarmGray900,
    secondaryContainer = HerbGreenDark,
    onSecondaryContainer = Color(0xFFCCF0D3),

    tertiary = GoldLight,
    onTertiary = WarmGray900,
    tertiaryContainer = Color(0xFF7A5A00),
    onTertiaryContainer = Color(0xFFFFF3C4),

    error = Color(0xFFFF6B6B),
    onError = WarmGray900,
    errorContainer = Color(0xFF8B0000),
    onErrorContainer = Color(0xFFFFE4E4),

    background = DarkBg,
    onBackground = WarmGray100,
    surface = DarkSurface,
    onSurface = WarmGray100,
    surfaceVariant = DarkSurfaceVar,
    onSurfaceVariant = WarmGray400,

    outline = WarmGray600,
    outlineVariant = WarmGray700,
    scrim = Color(0x99000000),
    inverseSurface = WarmGray200,
    inverseOnSurface = WarmGray800,
    inversePrimary = ChefOrangeDark,
    surfaceTint = ChefOrangeLight
)

@Composable
fun ChefProjTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Keep false to use our custom palette
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

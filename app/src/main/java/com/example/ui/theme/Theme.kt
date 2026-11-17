package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

enum class ThemeMode {
    SYSTEM,
    LIGHT,
    DARK,
}

@Immutable
data class PastelExtendedColors(
    val gradientStart: Color,
    val gradientMid: Color,
    val gradientEnd: Color,
    val auraRose: Color,
    val auraLavender: Color,
    val auraSky: Color,
    val glassSurface: Color,
    val glassBorderStart: Color,
    val glassBorderMid: Color,
    val glassBorderEnd: Color,
    val success: Color,
    val successContainer: Color,
    val wrong: Color,
    val wrongContainer: Color,
    val starGold: Color,
    val starGoldSoft: Color,
    val cardElevated: Color,
)

private val LightExtendedColors = PastelExtendedColors(
    gradientStart = Color(0xFFFFEFF6),
    gradientMid = Color(0xFFF5EEFF),
    gradientEnd = Color(0xFFEAF6FF),
    auraRose = Color(0x66FF9EC6),
    auraLavender = Color(0x5CC8B4FF),
    auraSky = Color(0x5C99D6FF),
    glassSurface = Color(0xE8FFFFFF),
    glassBorderStart = Color(0x70F39FC1),
    glassBorderMid = Color(0x66C0AEF7),
    glassBorderEnd = Color(0x6692CEFA),
    success = MintSuccessLight,
    successContainer = MintSuccessContainerLight,
    wrong = CoralWrongLight,
    wrongContainer = CoralWrongContainerLight,
    starGold = StarGold,
    starGoldSoft = StarGoldSoft,
    cardElevated = Color(0xFFFEFAFF),
)

private val DarkExtendedColors = PastelExtendedColors(
    gradientStart = Color(0xFF23162D),
    gradientMid = Color(0xFF1C1830),
    gradientEnd = Color(0xFF141E30),
    auraRose = Color(0x47E67CA8),
    auraLavender = Color(0x409880EB),
    auraSky = Color(0x3D62B3EA),
    glassSurface = Color(0xDE271F37),
    glassBorderStart = Color(0x66F6A5C4),
    glassBorderMid = Color(0x5CCBBDF8),
    glassBorderEnd = Color(0x5C9AD2F7),
    success = MintSuccessDark,
    successContainer = MintSuccessContainerDark,
    wrong = CoralWrongDark,
    wrongContainer = CoralWrongContainerDark,
    starGold = Color(0xFFF4C86A),
    starGoldSoft = Color(0xFF433319),
    cardElevated = Color(0xFF2B223D),
)

val LocalPastelColors = staticCompositionLocalOf { LightExtendedColors }

private val LightColorScheme = lightColorScheme(
    primary = RosePinkPrimary,
    onPrimary = Color.White,
    primaryContainer = RosePinkContainer,
    onPrimaryContainer = OnRosePinkContainer,
    secondary = LavenderSecondary,
    onSecondary = Color.White,
    secondaryContainer = LavenderContainer,
    onSecondaryContainer = OnLavenderContainer,
    tertiary = SkyBlueTertiary,
    onTertiary = Color.White,
    tertiaryContainer = SkyBlueContainer,
    onTertiaryContainer = OnSkyBlueContainer,
    background = LightBackground,
    onBackground = LightOnBackground,
    surface = LightSurface,
    onSurface = LightOnBackground,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = LightOnSurfaceVariant,
    outline = LightOutline,
    outlineVariant = Color(0xFFF0E5F5),
)

private val DarkColorScheme = darkColorScheme(
    primary = DarkRosePrimary,
    onPrimary = Color(0xFF3E1023),
    primaryContainer = DarkRoseContainer,
    onPrimaryContainer = DarkOnRoseContainer,
    secondary = DarkLavenderSecondary,
    onSecondary = Color(0xFF281C48),
    secondaryContainer = DarkLavenderContainer,
    onSecondaryContainer = DarkOnLavenderContainer,
    tertiary = DarkSkyBlueTertiary,
    onTertiary = Color(0xFF0D2D42),
    tertiaryContainer = DarkSkyBlueContainer,
    onTertiaryContainer = DarkOnSkyBlueContainer,
    background = DarkBackground,
    onBackground = DarkOnBackground,
    surface = DarkSurface,
    onSurface = DarkOnBackground,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkOnSurfaceVariant,
    outline = DarkOutline,
    outlineVariant = Color(0xFF382F4D),
)

val AppShapes = Shapes(
    small = RoundedCornerShape(14.dp),
    medium = RoundedCornerShape(22.dp),
    large = RoundedCornerShape(30.dp),
)

@Composable
fun PastelPawsTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val isDark = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }

    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (isDark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        isDark -> DarkColorScheme
        else -> LightColorScheme
    }

    val extended = if (isDark) DarkExtendedColors else LightExtendedColors

    CompositionLocalProvider(LocalPastelColors provides extended) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = AppTypography,
            shapes = AppShapes,
            content = content,
        )
    }
}

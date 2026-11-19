package com.hisabbondhu.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val LightColors = lightColorScheme(
    primary = LightPrimary,
    onPrimary = LightOnPrimary,
    primaryContainer = LightPrimaryContainer,
    onPrimaryContainer = LightOnPrimaryContainer,
    secondary = LightSecondary,
    onSecondary = LightOnSecondary,
    secondaryContainer = LightSecondaryContainer,
    onSecondaryContainer = LightOnSecondaryContainer,
    tertiary = LightTertiary,
    tertiaryContainer = LightTertiaryContainer,
    onTertiaryContainer = LightOnTertiaryContainer,
    error = LightError,
    errorContainer = LightErrorContainer,
    onErrorContainer = LightOnErrorContainer,
    background = LightBackground,
    onBackground = LightOnSurface,
    surface = LightSurface,
    onSurface = LightOnSurface,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = LightOnSurfaceVariant,
    outline = LightOutline,
    outlineVariant = LightOutlineVariant,
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color(0xFFF6F8F7),
    surfaceContainer = Color(0xFFF1F4F2),
    surfaceContainerHigh = Color(0xFFEBEFED),
    surfaceContainerHighest = Color(0xFFE5EAE7),
)

private val DarkColors = darkColorScheme(
    primary = DarkPrimary,
    onPrimary = DarkOnPrimary,
    primaryContainer = DarkPrimaryContainer,
    onPrimaryContainer = DarkOnPrimaryContainer,
    secondary = DarkSecondary,
    onSecondary = DarkOnSecondary,
    secondaryContainer = DarkSecondaryContainer,
    onSecondaryContainer = DarkOnSecondaryContainer,
    tertiary = DarkTertiary,
    tertiaryContainer = DarkTertiaryContainer,
    onTertiaryContainer = DarkOnTertiaryContainer,
    error = DarkError,
    errorContainer = DarkErrorContainer,
    onErrorContainer = DarkOnErrorContainer,
    background = DarkBackground,
    onBackground = DarkOnSurface,
    surface = DarkSurface,
    onSurface = DarkOnSurface,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkOnSurfaceVariant,
    outline = DarkOutline,
    outlineVariant = DarkOutlineVariant,
    surfaceContainerLowest = Color(0xFF0B100E),
    surfaceContainerLow = Color(0xFF131A18),
    surfaceContainer = Color(0xFF18201D),
    surfaceContainerHigh = Color(0xFF1F2825),
    surfaceContainerHighest = Color(0xFF27312E),
)

/** Semantic money colours used across the app. */
@Immutable
data class MoneyColors(
    val positive: Color,   // money in / they owe us (পাবো)
    val negative: Color,   // money out / we owe (দেবো)
    val warning: Color,
    val sale: Color,
    val paymentIn: Color,
    val expense: Color,
    val purchase: Color,
    val heroStart: Color,
    val heroEnd: Color,
)

private val LightMoney = MoneyColors(
    positive = Success,
    negative = Danger,
    warning = Warning,
    sale = BrandGreen,
    paymentIn = BrandBlue,
    expense = Danger,
    purchase = Color(0xFFD9480F),
    heroStart = BrandGreen,
    heroEnd = BrandGreenDark,
)

private val DarkMoney = MoneyColors(
    positive = Color(0xFF69DB7C),
    negative = Color(0xFFFF8787),
    warning = Color(0xFFFFC078),
    sale = Color(0xFF63E6BE),
    paymentIn = Color(0xFF74C0FC),
    expense = Color(0xFFFF8787),
    purchase = Color(0xFFFFA94D),
    heroStart = Color(0xFF0B5D45),
    heroEnd = Color(0xFF073B2C),
)

val LocalMoneyColors = staticCompositionLocalOf { LightMoney }

val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

@Composable
fun HisabTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalMoneyColors provides if (darkTheme) DarkMoney else LightMoney) {
        MaterialTheme(
            colorScheme = if (darkTheme) DarkColors else LightColors,
            typography = AppTypography,
            shapes = AppShapes,
            content = content,
        )
    }
}

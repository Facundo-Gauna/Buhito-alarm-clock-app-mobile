package com.buhito.alarm.ui.theme

import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import com.buhito.alarm.R
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

object BuhitoColors {
    val Night0 = Color(0xFF060A1A)
    val Night1 = Color(0xFF0B1230)
    val Night2 = Color(0xFF111C47)
    val Surface = Color(0xFF121B40)
    val SurfaceHigh = Color(0xFF1A2556)
    val SurfaceHighest = Color(0xFF222F66)
    val Outline = Color(0xFF2F3E80)
    val OutlineSoft = Color(0xFF222E63)
    val Moon = Color(0xFF8CA9FF)
    val OnMoon = Color(0xFF0A1236)
    val Amber = Color(0xFFFFC76B)
    val OnAmber = Color(0xFF2B1B00)
    val Ink = Color(0xFFEAF0FF)
    val InkDim = Color(0xFF9AA8D6)
    val Danger = Color(0xFFFF7A8A)
}

private val Scheme = darkColorScheme(
    primary = BuhitoColors.Moon,
    onPrimary = BuhitoColors.OnMoon,
    primaryContainer = Color(0xFF26397E),
    onPrimaryContainer = Color(0xFFDCE5FF),
    secondary = BuhitoColors.Amber,
    onSecondary = BuhitoColors.OnAmber,
    secondaryContainer = Color(0xFF3F3012),
    onSecondaryContainer = Color(0xFFFFE2B0),
    tertiary = Color(0xFFFF9FB5),
    background = BuhitoColors.Night1,
    onBackground = BuhitoColors.Ink,
    surface = BuhitoColors.Surface,
    onSurface = BuhitoColors.Ink,
    surfaceVariant = BuhitoColors.SurfaceHigh,
    onSurfaceVariant = BuhitoColors.InkDim,
    outline = BuhitoColors.Outline,
    outlineVariant = BuhitoColors.OutlineSoft,
    error = BuhitoColors.Danger,
    onError = Color(0xFF3A0010),
    surfaceContainerLowest = BuhitoColors.Night0,
    surfaceContainerLow = BuhitoColors.Surface,
    surfaceContainer = BuhitoColors.Surface,
    surfaceContainerHigh = BuhitoColors.SurfaceHigh,
    surfaceContainerHighest = BuhitoColors.SurfaceHighest,
)

val BuhitoFont = FontFamily(
    Font(R.font.outfit_regular, FontWeight.Normal),
    Font(R.font.outfit_regular, FontWeight.Medium),
    Font(R.font.outfit_bold, FontWeight.SemiBold),
    Font(R.font.outfit_bold, FontWeight.Bold),
)

private val BuhitoTypography = Typography().let { t ->
    fun TextStyle.f() = copy(fontFamily = BuhitoFont)
    t.copy(
        displayLarge = t.displayLarge.f(), displayMedium = t.displayMedium.f(), displaySmall = t.displaySmall.f(),
        headlineLarge = TextStyle(fontSize = 36.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.5).sp).f(),
        headlineMedium = TextStyle(fontSize = 28.sp, fontWeight = FontWeight.SemiBold).f(),
        headlineSmall = t.headlineSmall.f(),
        titleLarge = TextStyle(fontSize = 22.sp, fontWeight = FontWeight.SemiBold).f(),
        titleMedium = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.SemiBold).f(),
        titleSmall = t.titleSmall.f(),
        bodyLarge = TextStyle(fontSize = 16.sp).f(),
        bodyMedium = TextStyle(fontSize = 14.sp).f(),
        bodySmall = t.bodySmall.f(),
        labelLarge = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.SemiBold).f(),
        labelMedium = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 1.sp).f(),
        labelSmall = t.labelSmall.f(),
    )
}

private val BuhitoShapes = Shapes(
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(28.dp),
)

@Composable
fun BuhitoTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = Scheme,
        typography = BuhitoTypography,
        shapes = BuhitoShapes,
    ) {
        CompositionLocalProvider(LocalContentColor provides Scheme.onBackground, content = content)
    }
}

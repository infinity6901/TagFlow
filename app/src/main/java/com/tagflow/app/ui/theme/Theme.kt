package com.tagflow.app.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import com.tagflow.app.data.AppSettings

val AccentHues = listOf(255f, 175f, 340f, 35f, 145f, 205f)
val AccentNames = listOf("Indigo", "Teal", "Rose", "Amber", "Green", "Blue")

val TagColors = listOf(
    Color(0xFF6366F1), Color(0xFF14B8A6), Color(0xFFF43F5E), Color(0xFFF59E0B),
    Color(0xFF10B981), Color(0xFF0EA5E9), Color(0xFF8B5CF6), Color(0xFFF97316)
)

private fun hsv(h: Float, s: Float, v: Float): Color =
    Color(android.graphics.Color.HSVToColor(floatArrayOf(((h % 360f) + 360f) % 360f, s, v)))

fun accentPreview(index: Int): Color = hsv(AccentHues[index.coerceIn(0, AccentHues.lastIndex)], .62f, .75f)

private fun lightScheme(h: Float) = lightColorScheme(
    primary = hsv(h, .62f, .62f), onPrimary = Color.White,
    primaryContainer = hsv(h, .14f, 1f), onPrimaryContainer = hsv(h, .85f, .28f),
    secondary = hsv(h, .30f, .50f), onSecondary = Color.White,
    secondaryContainer = hsv(h, .10f, .96f), onSecondaryContainer = hsv(h, .55f, .26f),
    tertiary = hsv(h + 55, .50f, .60f), onTertiary = Color.White,
    tertiaryContainer = hsv(h + 55, .14f, 1f), onTertiaryContainer = hsv(h + 55, .80f, .28f),
    background = hsv(h, .025f, .99f), onBackground = hsv(h, .30f, .12f),
    surface = hsv(h, .025f, .99f), onSurface = hsv(h, .30f, .12f),
    surfaceVariant = hsv(h, .08f, .93f), onSurfaceVariant = hsv(h, .20f, .35f),
    surfaceContainerLowest = Color.White, surfaceContainerLow = hsv(h, .03f, .97f),
    surfaceContainer = hsv(h, .05f, .95f), surfaceContainerHigh = hsv(h, .06f, .93f),
    surfaceContainerHighest = hsv(h, .08f, .91f),
    outline = hsv(h, .15f, .50f), outlineVariant = hsv(h, .10f, .82f),
    error = Color(0xFFBA1A1A), onError = Color.White,
    errorContainer = Color(0xFFFFDAD6), onErrorContainer = Color(0xFF410002)
)

private fun darkScheme(h: Float) = darkColorScheme(
    primary = hsv(h, .38f, 1f), onPrimary = hsv(h, .90f, .30f),
    primaryContainer = hsv(h, .70f, .45f), onPrimaryContainer = hsv(h, .15f, 1f),
    secondary = hsv(h, .20f, .85f), onSecondary = hsv(h, .70f, .25f),
    secondaryContainer = hsv(h, .45f, .35f), onSecondaryContainer = hsv(h, .12f, .95f),
    tertiary = hsv(h + 55, .35f, .95f), onTertiary = hsv(h + 55, .90f, .28f),
    tertiaryContainer = hsv(h + 55, .60f, .38f), onTertiaryContainer = hsv(h + 55, .12f, .97f),
    background = hsv(h, .20f, .07f), onBackground = hsv(h, .06f, .93f),
    surface = hsv(h, .20f, .07f), onSurface = hsv(h, .06f, .93f),
    surfaceVariant = hsv(h, .18f, .24f), onSurfaceVariant = hsv(h, .10f, .80f),
    surfaceContainerLowest = hsv(h, .22f, .05f), surfaceContainerLow = hsv(h, .20f, .10f),
    surfaceContainer = hsv(h, .20f, .12f), surfaceContainerHigh = hsv(h, .19f, .16f),
    surfaceContainerHighest = hsv(h, .18f, .20f),
    outline = hsv(h, .10f, .60f), outlineVariant = hsv(h, .15f, .30f),
    error = Color(0xFFFFB4AB), onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A), onErrorContainer = Color(0xFFFFDAD6)
)

private fun amoled(c: ColorScheme) = c.copy(
    background = Color.Black, surface = Color.Black,
    surfaceContainerLowest = Color.Black, surfaceContainerLow = Color(0xFF0B0B0D),
    surfaceContainer = Color(0xFF121214), surfaceContainerHigh = Color(0xFF19191C),
    surfaceContainerHighest = Color(0xFF212125)
)

private val AppTypography = Typography().let { t ->
    t.copy(
        headlineLarge = t.headlineLarge.copy(fontWeight = FontWeight.Bold),
        headlineMedium = t.headlineMedium.copy(fontWeight = FontWeight.Bold),
        headlineSmall = t.headlineSmall.copy(fontWeight = FontWeight.SemiBold),
        titleLarge = t.titleLarge.copy(fontWeight = FontWeight.SemiBold),
        titleMedium = t.titleMedium.copy(fontWeight = FontWeight.SemiBold),
        labelLarge = t.labelLarge.copy(fontWeight = FontWeight.SemiBold)
    )
}

private val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp)
)

@Composable
fun TagFlowTheme(settings: AppSettings, content: @Composable () -> Unit) {
    val dark = when (settings.theme) {
        1 -> false
        2 -> true
        else -> isSystemInDarkTheme()
    }
    val ctx = LocalContext.current
    var scheme = if (settings.dynamic && Build.VERSION.SDK_INT >= 31) {
        if (dark) dynamicDarkColorScheme(ctx) else dynamicLightColorScheme(ctx)
    } else {
        val h = AccentHues[settings.accent.coerceIn(0, AccentHues.lastIndex)]
        if (dark) darkScheme(h) else lightScheme(h)
    }
    if (dark && settings.amoled) scheme = amoled(scheme)

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                val c = WindowCompat.getInsetsController(window, view)
                c.isAppearanceLightStatusBars = !dark
                c.isAppearanceLightNavigationBars = !dark
            }
        }
    }
    MaterialTheme(colorScheme = scheme, typography = AppTypography, shapes = AppShapes, content = content)
}

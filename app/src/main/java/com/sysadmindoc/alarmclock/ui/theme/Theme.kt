package com.sysadmindoc.alarmclock.ui.theme

import android.app.Activity
import android.database.ContentObserver
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import com.sysadmindoc.alarmclock.util.MotionPolicy

val LocalAccentColor = compositionLocalOf { AccentBlue }
val LocalExpressiveMode = compositionLocalOf { false }
val LocalMotionEnabled = compositionLocalOf { true }

data class AppShapeTokens(
    val card: Shape,
    val tile: Shape,
    val chip: Shape,
    val iconContainer: Shape,
    val bottomNav: Shape
)

// WakeSync: Google Clock-inspired geometry without copying Google's palette,
// navigation structure, or WakeSync's sleep-mode functionality.
private val StandardShapeTokens = AppShapeTokens(
    card = RoundedCornerShape(28.dp),
    tile = RoundedCornerShape(22.dp),
    chip = RoundedCornerShape(50),
    iconContainer = RoundedCornerShape(20.dp),
    bottomNav = RoundedCornerShape(28.dp)
)

private val ExpressiveShapeTokens = AppShapeTokens(
    card = RoundedCornerShape(32.dp),
    tile = RoundedCornerShape(26.dp),
    chip = RoundedCornerShape(50),
    iconContainer = RoundedCornerShape(22.dp),
    bottomNav = RoundedCornerShape(30.dp)
)

val LocalAppShapeTokens = compositionLocalOf { StandardShapeTokens }

private val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(12.dp),
    small = RoundedCornerShape(18.dp),
    medium = RoundedCornerShape(28.dp),
    large = RoundedCornerShape(32.dp),
    extraLarge = RoundedCornerShape(36.dp)
)

private val ExpressiveMaterialShapes = Shapes(
    extraSmall = RoundedCornerShape(14.dp),
    small = RoundedCornerShape(20.dp),
    medium = RoundedCornerShape(30.dp),
    large = RoundedCornerShape(34.dp),
    extraLarge = RoundedCornerShape(38.dp)
)

private val DarkColorScheme = darkColorScheme(
    primary = BluePrimary,
    onPrimary = TextPrimary,
    primaryContainer = BlueDark,
    onPrimaryContainer = TextPrimary,
    secondary = BlueLight,
    onSecondary = TextPrimary,
    secondaryContainer = SurfaceLight,
    onSecondaryContainer = TextPrimary,
    tertiary = SnoozeYellow,
    onTertiary = SurfaceDark,
    background = SurfaceDark,
    onBackground = TextPrimary,
    surface = SurfaceMedium,
    onSurface = TextPrimary,
    surfaceVariant = SurfaceCard,
    onSurfaceVariant = TextSecondary,
    error = AccentRed,
    onError = TextPrimary,
    outline = TextMuted,
    outlineVariant = SurfaceLight,
    surfaceTint = BluePrimary,
)

@Composable
fun AlarmClockXtremeTheme(
    accentColorHex: String? = null,
    dynamicColor: Boolean = false,
    expressiveMode: Boolean = false,
    reduceMotionAndFlashing: Boolean = false,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    var animatorDurationScale by remember(context) {
        mutableFloatStateOf(MotionPolicy.animatorDurationScale(context))
    }
    DisposableEffect(context) {
        val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) {
                animatorDurationScale = MotionPolicy.animatorDurationScale(context)
            }
        }
        context.contentResolver.registerContentObserver(
            Settings.Global.getUriFor(Settings.Global.ANIMATOR_DURATION_SCALE), false, observer
        )
        onDispose { context.contentResolver.unregisterContentObserver(observer) }
    }
    val motionEnabled = MotionPolicy.allowsMotion(reduceMotionAndFlashing, animatorDurationScale)
    val parsedAccent = if (accentColorHex != null && accentColorHex.startsWith("#")) {
        try { Color(android.graphics.Color.parseColor(accentColorHex)) } catch (_: Exception) { AccentBlue }
    } else AccentBlue
    val supportsDynamic = dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    val baseColorScheme = if (supportsDynamic) {
        dynamicDarkColorScheme(context).copy(
            background = SurfaceDark,
            surface = SurfaceMedium,
            surfaceVariant = SurfaceCard
        )
    } else {
        DarkColorScheme.copy(
            primary = parsedAccent,
            secondary = parsedAccent,
            surfaceTint = parsedAccent
        )
    }
    val accent = if (supportsDynamic) baseColorScheme.primary else parsedAccent
    val colorScheme = if (expressiveMode) {
        baseColorScheme.copy(
            secondary = SnoozeYellow,
            tertiary = DismissGreen,
            surfaceTint = accent
        )
    } else baseColorScheme
    val materialShapes = if (expressiveMode) ExpressiveMaterialShapes else AppShapes
    val appShapeTokens = if (expressiveMode) ExpressiveShapeTokens else StandardShapeTokens
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window ?: return@SideEffect
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.VANILLA_ICE_CREAM) {
                @Suppress("DEPRECATION") window.statusBarColor = android.graphics.Color.TRANSPARENT
                @Suppress("DEPRECATION") window.navigationBarColor = SurfaceDark.toArgb()
            }
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = false
                isAppearanceLightNavigationBars = false
            }
        }
    }
    CompositionLocalProvider(
        LocalAccentColor provides accent,
        LocalExpressiveMode provides expressiveMode,
        LocalMotionEnabled provides motionEnabled,
        LocalAppShapeTokens provides appShapeTokens
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = AppTypography,
            shapes = materialShapes,
            content = content
        )
    }
}

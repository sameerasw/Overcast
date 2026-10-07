package com.sameerasw.overcast.ui.features.weather

import androidx.compose.foundation.background
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsIgnoringVisibility
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.sameerasw.overcast.ui.components.LocalWeatherIconStyle
import com.sameerasw.overcast.ui.modifiers.rainOnGlass
import com.sameerasw.overcast.weather.effects.WeatherEffectLayer
import com.sameerasw.overcast.weather.effects.WeatherEffectSpec
import com.sameerasw.overcast.weather.effects.WeatherEffects
import com.sameerasw.overcast.weather.model.WeatherSnapshot
import kotlinx.coroutines.delay
import java.util.Calendar

internal val AMBIENT_EDGE_GAP = 16.dp
internal const val AMBIENT_SURFACE_PREFIX = "ambient-hourly"

// The gradient and the sun or moon, shared by the main screen and the screensaver.
@Composable
internal fun BoxScope.WeatherSkyBackground(snapshot: WeatherSnapshot?, now: Long, palette: WeatherPalette, collapse: () -> Float) {
    Box(
        Modifier
            .matchParentSize()
            .background(
                Brush.verticalGradient(
                    0f to palette.glow,
                    0.4f to palette.glowSecondary,
                    0.8f to palette.base,
                    1f to palette.base,
                ),
            ),
    )
    snapshot?.let { SkyBody(it, now, collapse) }
}

// The rain-on-glass refraction, lit from wherever the sun or moon currently is.
@Composable
internal fun Modifier.weatherGlass(
    spec: WeatherEffectSpec,
    snapshot: WeatherSnapshot?,
    now: Long,
    collapse: () -> Float,
    intensityScale: Float = 1f,
    enabled: Boolean = true,
): Modifier {
    val density = LocalDensity.current
    val rain = spec.layers.filterIsInstance<WeatherEffectLayer.Rain>().maxByOrNull { it.intensity }.takeIf { enabled }
    val sky = snapshot?.let { skyState(it, now) }
    val skyHeightPx = with(density) { SKY_HEIGHT.toPx() }
    val collapseShiftPx = with(density) { 60.dp.toPx() }
    val fallbackYPx = with(density) { 150.dp.toPx() }
    return rainOnGlass((rain?.intensity ?: 0f) * intensityScale, rain?.slant ?: 0f) {
        if (sky == null) {
            Offset(0.5f, fallbackYPx - collapse() * collapseShiftPx)
        } else {
            val t = sky.first
            Offset(
                0.9f - 0.8f * t,
                skyHeightPx * (0.66f - 0.4f * kotlin.math.sin(Math.PI.toFloat() * t)) - collapse() * collapseShiftPx,
            )
        }
    }
}

@Composable
internal fun rememberMinuteClock(): State<Long> = produceState(System.currentTimeMillis()) {
    while (true) {
        value = System.currentTimeMillis()
        delay(60_000L - value % 60_000L)
    }
}

// The hourly forecast pinned to the bottom edge, with the same gap the clock leaves at the top.
@Composable
internal fun AmbientForecast(
    ambient: () -> Float,
    snapshot: WeatherSnapshot,
    unit: com.sameerasw.overcast.weather.TemperatureUnit,
    palette: WeatherPalette,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier
            .windowInsetsPadding(WindowInsets.navigationBarsIgnoringVisibility)
            .padding(bottom = AMBIENT_EDGE_GAP)
            .graphicsLayer {
                val reveal = ((ambient() - 0.4f) / 0.6f).coerceIn(0f, 1f)
                alpha = reveal
                translationY = (1f - reveal) * 16.dp.toPx()
            },
    ) {
        HourlySection(snapshot, unit, palette, AMBIENT_SURFACE_PREFIX)
    }
}

// The screensaver: the main screen's ambient mode, standing on its own.
@Composable
internal fun AmbientWeatherScene(
    presentation: WeatherPresentation,
    showForecast: Boolean,
    modifier: Modifier = Modifier,
    onDismiss: (() -> Unit)? = null,
) {
    val snapshot = presentation.snapshot
    val spec = presentation.effectSpec
    val minute by rememberMinuteClock()
    // A slow drift keeps static text from sitting on the same pixels for hours.
    val steps = Calendar.getInstance().apply { timeInMillis = minute }.get(Calendar.MINUTE) / 5
    val rainSurfaces = remember { mutableStateMapOf<String, RainSurfaceSource>() }
    val cards = { rainSurfaces.entries.mapNotNull { (key, source) -> source.resolve(key) } }
    CompositionLocalProvider(LocalWeatherIconStyle provides presentation.iconStyle, LocalRainSurfaces provides rainSurfaces) {
        BoxWithConstraints(modifier.fillMaxSize().weatherGlass(spec, snapshot, presentation.now, { 0f }).dismissOnTouch(onDismiss)) {
            WeatherSkyBackground(snapshot, presentation.now, presentation.palette) { 0f }
            if (!spec.isEmpty) {
                WeatherEffects(spec = spec, modifier = Modifier.matchParentSize(), strength = 1.7f, surfaces = cards, cover = cards)
            }
            if (snapshot != null) {
                Box(Modifier.fillMaxSize().offset(x = ((steps % 5) - 2).dp * 3, y = (((steps / 5) % 3) - 1).dp * 3)) {
                    Header(
                        snapshot,
                        presentation.units.temperature,
                        presentation.palette,
                        0f,
                        1f,
                        Modifier.padding(horizontal = SIDE_PADDING).ambientCenter(1f, this@BoxWithConstraints.constraints.maxHeight),
                    )
                    AmbientClock({ 1f }, presentation.palette, Modifier.align(Alignment.TopCenter))
                    if (showForecast) {
                        AmbientForecast({ 1f }, snapshot, presentation.units.temperature, presentation.palette, Modifier.align(Alignment.BottomCenter))
                    }
                }
            }
        }
    }
}

private fun Modifier.dismissOnTouch(onDismiss: (() -> Unit)?): Modifier =
    if (onDismiss == null) {
        this
    } else {
        this
            .pointerInput(onDismiss) { detectTapGestures(onTap = { onDismiss() }) }
            .pointerInput(onDismiss) { detectVerticalDragGestures { _, _ -> onDismiss() } }
    }

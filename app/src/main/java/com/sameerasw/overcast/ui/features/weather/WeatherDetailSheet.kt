package com.sameerasw.overcast.ui.features.weather

import android.content.Context
import com.sameerasw.overcast.weather.effects.WeatherEffectHaptics
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.runtime.mutableStateOf
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.ui.layout.Layout
import com.sameerasw.overcast.ui.modifiers.progressiveBlur
import com.sameerasw.overcast.ui.modifiers.BlurDirection
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.util.lerp
import androidx.compose.runtime.State
import androidx.compose.runtime.produceState
import androidx.window.layout.FoldingFeature
import androidx.window.layout.WindowInfoTracker
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.layout
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.ui.geometry.Offset
import com.sameerasw.overcast.ui.modifiers.heatHaze
import com.sameerasw.overcast.ui.modifiers.rainOnGlass
import com.sameerasw.overcast.weather.effects.RainSurface
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.platform.LocalDensity
import android.text.format.DateFormat
import android.text.format.DateUtils
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.carousel.HorizontalMultiBrowseCarousel
import androidx.compose.material3.carousel.rememberCarouselState
import androidx.compose.material3.Surface
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.foundation.layout.PaddingValues
import com.sameerasw.overcast.ui.core.containers.RoundedCardContainer
import com.sameerasw.overcast.ui.theme.Shapes
import com.sameerasw.overcast.ui.theme.Typography
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.SplitButtonLayout
import androidx.compose.material3.SplitButtonDefaults
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.foundation.layout.statusBarsIgnoringVisibility
import androidx.compose.foundation.layout.navigationBarsIgnoringVisibility
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.MutableFloatState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animate
import androidx.core.view.WindowInsetsControllerCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowCompat
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.LinearEasing
import androidx.activity.compose.BackHandler
import android.view.Window
import android.content.ContextWrapper
import android.app.Activity
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sameerasw.overcast.R
import com.sameerasw.overcast.data.repository.SettingsRepository
import com.sameerasw.overcast.utils.DeviceUtils
import com.sameerasw.overcast.utils.CompatibilityMode
import com.sameerasw.overcast.utils.HapticUtil
import com.sameerasw.overcast.weather.WeatherFormat
import com.sameerasw.overcast.weather.WeatherRepository
import com.sameerasw.overcast.weather.effects.DeviceWeatherHaptics
import com.sameerasw.overcast.weather.location.DeviceLocationSource
import com.sameerasw.overcast.weather.effects.WeatherEffectSpec
import com.sameerasw.overcast.weather.effects.WeatherEffects
import com.sameerasw.overcast.weather.effects.WeatherSimulation
import com.sameerasw.overcast.weather.model.DailyForecast
import com.sameerasw.overcast.ui.components.LocalWeatherIconStyle
import com.sameerasw.overcast.ui.components.WeatherDetailIcon
import com.sameerasw.overcast.ui.components.WeatherIcon
import com.sameerasw.overcast.weather.TemperatureUnit
import com.sameerasw.overcast.weather.WeatherDetailSlot
import com.sameerasw.overcast.weather.WeatherIconStyle
import com.sameerasw.overcast.weather.WeatherUnits
import com.sameerasw.overcast.weather.model.WeatherAlert
import com.sameerasw.overcast.weather.model.WeatherError
import com.sameerasw.overcast.weather.model.WeatherSnapshot
import com.sameerasw.overcast.weather.provider.WeatherProviders
import com.sameerasw.overcast.weather.MoonPhase
import com.sameerasw.overcast.weather.moonPhaseAt
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.drop
import androidx.compose.runtime.SideEffect
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.snapshotFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt
import android.content.SharedPreferences
import androidx.compose.foundation.Canvas
import androidx.compose.material3.FilledTonalButton
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import com.sameerasw.overcast.weather.effects.WeatherEffectLayer
import java.util.concurrent.ConcurrentHashMap

internal class WeatherPresentation(
    val snapshot: WeatherSnapshot?,
    val palette: WeatherPalette,
    val effectSpec: WeatherEffectSpec,
    val units: WeatherUnits,
    val now: Long,
    val haptics: WeatherEffectHaptics?,
    val iconStyle: WeatherIconStyle,
    val timeline: WeatherTimeline? = null,
    val clock: () -> Long = { now },
    val clockLabel: Long = now,
    val timelineAtNow: Boolean = true,
)

@Composable
internal fun rememberWeatherPresentation(real: WeatherSnapshot?, timeline: WeatherTimeline? = null): WeatherPresentation {
    val context = LocalContext.current
    val settings = remember { SettingsRepository(context) }
    var settingsVersion by remember { mutableIntStateOf(0) }
    DisposableEffect(context) {
        val prefs = context.getSharedPreferences(SettingsRepository.PREFS_NAME, Context.MODE_PRIVATE)
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, _ -> settingsVersion++ }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        onDispose { prefs.unregisterOnSharedPreferenceChangeListener(listener) }
    }
    val simulation = remember(settingsVersion) { settings.getSimulatedWeather() }
    val timeOverride = remember(settingsVersion) { settings.getSimulatedTimeOfDay() }
    val tempOverride = remember(settingsVersion) { settings.getSimulatedTempC() }
    val alertOverride = remember(settingsVersion) { settings.getSimulatedAlertId() }
    val simulated = real?.let { r -> simulation?.let { WeatherSimulation.apply(r, it) } ?: r }
    val baseSnapshot = simulated?.let { WeatherSimulation.withTimeOfDay(it, timeOverride) }?.let { s -> tempOverride?.let { s.copy(tempC = it) } ?: s }
        ?.let { s -> WeatherSimulation.alertsFor(alertOverride, System.currentTimeMillis())?.let { s.copy(alerts = it) } ?: s }
    val units = remember(settingsVersion) { WeatherUnits.from(settings) }
    val iconStyle = remember(settingsVersion) { WeatherIconStyle.fromId(settings.getWeatherIconStyle()) }
    var clock by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(60_000L)
            clock = System.currentTimeMillis()
        }
    }
    val now = WeatherSimulation.timeFor(baseSnapshot, timeOverride, clock)
    val timelineEnabled = remember(settingsVersion) { settings.isAmbientTimelineEnabled() }
    val looping = timeline?.loop == true
    val points = remember(baseSnapshot, now / 60_000L, looping) { baseSnapshot?.let { timelinePoints(it, now, looping) }.orEmpty() }
    val timelineActive = timeline != null && timelineEnabled && points.size > 1
    SideEffect { timeline?.points = if (timelineEnabled) points else emptyList() }
    val step by remember(timeline) { derivedStateOf { timeline?.value?.roundToInt() ?: 0 } }
    val atNow by remember(timeline) { derivedStateOf { (timeline?.value ?: 0f) < 0.001f } }
    val tempHalfDegrees by remember(timeline) { derivedStateOf { ((timeline?.tempAt(timeline.value) ?: 0.0) * 2).roundToInt() } }
    val virtualQuarterHour by remember(timeline) { derivedStateOf { (timeline?.timeAt(timeline.value) ?: 0L) / QUARTER_HOUR_MS } }
    val snapshot = if (!timelineActive || baseSnapshot == null || atNow) {
        baseSnapshot
    } else {
        val point = points[step.coerceIn(0, points.lastIndex)]
        baseSnapshot.copy(
            tempC = tempHalfDegrees / 2.0,
            condition = point.condition,
            conditionText = point.conditionText ?: conditionLabel(context, point.condition),
            isDay = point.isDay,
            chanceOfRain = point.chanceOfRain ?: baseSnapshot.chanceOfRain,
        )
    }
    val paletteNow = if (timelineActive && !atNow) virtualQuarterHour * QUARTER_HOUR_MS else now
    val palette = animatedPalette(
        remember(snapshot?.condition, snapshot?.isDay, snapshot?.extras?.sunriseMillis, snapshot?.extras?.sunsetMillis, paletteNow / 60_000L, timeOverride) {
            WeatherPalette.from(snapshot, paletteNow, timeOverride)
        },
    )
    val effects = remember(settingsVersion) { settings.isWeatherEffectsEnabled() && !DeviceUtils.isPowerSaveMode(context) }
    val haptics = remember(settingsVersion, effects) {
        DeviceWeatherHaptics(context).takeIf { effects && settings.isWeatherHapticsEnabled() }
    }
    val effectSpec = remember(effects, snapshot?.condition, snapshot?.isDay, snapshot?.windKph, simulation?.id) {
        snapshot?.takeIf { effects }?.let { simulation?.spec ?: WeatherEffectSpec.from(it) } ?: WeatherEffectSpec.None
    }
    val activeTimeline = timeline?.takeIf { timelineActive }
    val clockProvider = remember(activeTimeline, now) { if (activeTimeline != null) ({ activeTimeline.timeAt(activeTimeline.value) }) else ({ now }) }
    val clockLabel = if (timelineActive && step > 0) points[step.coerceIn(0, points.lastIndex)].timeMillis else now
    return WeatherPresentation(snapshot, palette, effectSpec, units, now, haptics, iconStyle, activeTimeline, clockProvider, clockLabel, step == 0)
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun WeatherScreen(onOpenSettings: () -> Unit = {}) {
    val context = LocalContext.current
    val view = LocalView.current
    val scope = rememberCoroutineScope()
    val settings = remember { SettingsRepository(context) }
    val state by WeatherRepository.state.collectAsState()
    val timeline = remember { WeatherTimeline() }
    val presentation = rememberWeatherPresentation(state.snapshot, timeline)
    val snapshot = presentation.snapshot
    val units = presentation.units
    val now = presentation.now
    val palette = presentation.palette
    val effectSpec = presentation.effectSpec
    val effectHaptics = presentation.haptics

    val requestLocation = rememberLocationPermissionRequest { granted ->
        if (granted) scope.launch { WeatherRepository.refresh(context, force = true) }
    }
    var showLocations by remember { mutableStateOf(false) }
    val sheetTop = remember { mutableFloatStateOf(Float.NaN) }
    val sheetOpen = showLocations
    val screenWidthPx = with(LocalDensity.current) { LocalConfiguration.current.screenWidthDp.dp.toPx() }
    val screenHeightPx = with(LocalDensity.current) { LocalConfiguration.current.screenHeightDp.dp.toPx() }
    val sheetCornerPx = with(LocalDensity.current) { 28.dp.toPx() }

    // Staged intro: content fades in first, the weather effects ease in afterwards, and the network refresh waits until it settles.
    val hasSnapshot = snapshot != null
    var effectsGo by remember { mutableStateOf(false) }
    LaunchedEffect(hasSnapshot) {
        if (hasSnapshot) {
            delay(EFFECTS_DELAY_MS)
            effectsGo = true
        }
    }
    val compatibility = CompatibilityMode.enabled.value
    val contentAlpha by animateFloatAsState(if (hasSnapshot) 1f else 0f, if (compatibility) snap() else tween(CONTENT_FADE_MS), label = "contentAlpha")
    val effectRamp by animateFloatAsState(if (effectsGo) 1f else 0f, if (compatibility) snap() else tween(EFFECTS_FADE_MS, easing = LinearOutSlowInEasing), label = "effectRamp")

    LaunchedEffect(Unit) {
        WeatherRepository.ensureLoaded(context)
        if (WeatherRepository.state.value.snapshot != null) delay(REFRESH_DELAY_MS)
        val needsLocation = settings.getWeatherLocationMode() != "manual" && !DeviceLocationSource.hasPermission(context)
        if (needsLocation) {
            requestLocation()
        } else {
            val stale = WeatherRepository.isStale(context)
            val moved = !stale && WeatherRepository.hasMoved(context)
            if (stale || moved) WeatherRepository.refresh(context, force = moved)
        }
    }

    val rainSurfaces = remember { androidx.compose.runtime.mutableStateMapOf<String, RainSurfaceSource>() }
    val headerBottom = remember { mutableFloatStateOf(0f) }

    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = palette.accent,
            onPrimary = Color.Black,
            surface = palette.base,
            onSurface = palette.onBase,
            onSurfaceVariant = palette.onBaseMuted,
            background = palette.base,
            onBackground = palette.onBase,
        ),
        typography = Typography,
        shapes = Shapes,
    ) {
        CompositionLocalProvider(LocalRainSurfaces provides rainSurfaces, LocalWeatherIconStyle provides presentation.iconStyle) {
        if (showLocations) {
            LocationsBottomSheet(
                onDismissRequest = { showLocations = false },
                onSelectCurrent = {
                    settings.setWeatherLocationMode("device")
                    val permitted = DeviceLocationSource.hasPermission(context)
                    scope.launch {
                        WeatherRepository.clear(context)
                        if (permitted) WeatherRepository.refresh(context, force = true)
                    }
                    if (!permitted) requestLocation()
                },
                onSelectPlace = { place ->
                    settings.setWeatherManualLocation(place.latitude, place.longitude, place.name)
                    settings.setWeatherLocationMode("manual")
                    scope.launch {
                        WeatherRepository.clear(context)
                        WeatherRepository.refresh(context, force = true)
                    }
                },
                onTopChanged = { sheetTop.floatValue = it },
            )
        }
        val density = LocalDensity.current
        val collapse = remember { mutableFloatStateOf(0f) }
        val scrollTick = remember { mutableIntStateOf(0) }
        val ambient = remember { mutableFloatStateOf(0f) }
        val immersive = ambient.floatValue > 0.5f
        val rootHeightPx = remember { mutableIntStateOf(0) }
        val rootWidthPx = remember { mutableIntStateOf(0) }
        val ambientClockPx = remember { mutableIntStateOf(0) }
        val ambientForecastPx = remember { mutableIntStateOf(0) }
        val timelineActive = presentation.timeline != null
        val showTimelineReturn by remember { derivedStateOf { timeline.value > 0.05f } }
        LaunchedEffect(immersive) {
            if (!immersive) timeline.animateTo(scope, 0f)
        }
        LaunchedEffect(timeline) {
            snapshotFlow { timeline.value.roundToInt() }.drop(1).collect {
                if (ambient.floatValue > 0.5f) HapticUtil.performLightHaptic(view)
            }
        }
        BackHandler(enabled = immersive) { scope.launch { settleAmbient(ambient, 0f, compatibility) } }
        val window = remember(view) { view.context.findWindow() }
        DisposableEffect(immersive) {
            val controller = window?.let { WindowCompat.getInsetsController(it, view) }
            controller?.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            if (immersive) controller?.hide(WindowInsetsCompat.Type.systemBars()) else controller?.show(WindowInsetsCompat.Type.systemBars())
            onDispose {}
        }
        DisposableEffect(Unit) {
            onDispose { window?.let { WindowCompat.getInsetsController(it, view).show(WindowInsetsCompat.Type.systemBars()) } }
        }
        Box(Modifier.fillMaxSize().onSizeChanged { rootHeightPx.intValue = it.height; rootWidthPx.intValue = it.width }.weatherGlass(effectSpec, snapshot, presentation.clock, { collapse.floatValue }, effectRamp, !sheetOpen)) {
                WeatherSkyBackground(snapshot, presentation.clock, palette, collapse = { collapse.floatValue })
                val topFadePx = if (headerBottom.floatValue > 0f) {
                    val expandedFade = with(density) { 36.dp.toPx() }
                    val collapsedFade = headerBottom.floatValue + with(density) { 40.dp.toPx() }
                    expandedFade + (collapsedFade - expandedFade) * collapse.floatValue
                } else {
                    0f
                }
                if (effectRamp > 0f) {
                    val effectsBottomFadePx = WindowInsets.navigationBars.getBottom(density) + with(density) { 20.dp.toPx() }
                    WeatherEffects(
                        spec = effectSpec,
                        modifier = Modifier
                            .matchParentSize()
                            .progressiveBlur(blurRadius = 40f, height = topFadePx, direction = BlurDirection.TOP, showGradientOverlay = false)
                            .progressiveBlur(blurRadius = 14f, height = effectsBottomFadePx, direction = BlurDirection.BOTTOM, showGradientOverlay = false),
                        strength = 1.7f * effectRamp,
                        haptics = effectHaptics.takeIf { effectRamp > 0.6f },
                        surfaces = {
                            val all = rainSurfaces.entries.mapNotNull { (key, source) -> source.resolve(key) }
                            if (ambient.floatValue > 0.3f) return@WeatherEffects all.filter { it.key.startsWith(AMBIENT_SURFACE_PREFIX) }
                            val top = sheetTop.floatValue
                            val cards = all.filter { !it.key.startsWith(AMBIENT_SURFACE_PREFIX) && it.rect.top >= headerBottom.floatValue }
                            if (top.isNaN()) {
                                cards
                            } else {
                                cards.filter { it.rect.bottom <= top } +
                                    RainSurface("locations-sheet", Rect(0f, top, screenWidthPx, screenHeightPx), sheetCornerPx, 0f, screenWidthPx)
                            }
                        },
                        cover = {
                            val all = rainSurfaces.entries.mapNotNull { (key, source) -> source.resolve(key) }
                            if (ambient.floatValue > 0.3f) {
                                all.filter { it.key.startsWith(AMBIENT_SURFACE_PREFIX) }
                            } else {
                                all.filter { !it.key.startsWith(AMBIENT_SURFACE_PREFIX) }
                            }
                        },
                        scrollTick = { scrollTick.intValue },
                    )
                }
                val scrollState = rememberScrollState()
                val wide = LocalConfiguration.current.screenWidthDp >= TWO_PANE_MIN_WIDTH_DP
                val fold by rememberVerticalFold()
                val rootWidth = rootWidthPx.intValue
                val foldSplit = fold?.takeIf { rootWidth > 0 && it.left > 0 && it.right < rootWidth }
                val leftFraction = foldSplit?.let { it.left.toFloat() / rootWidth } ?: TWO_PANE_LEFT_WEIGHT
                val hingeFraction = foldSplit?.let { (it.right - it.left).toFloat() / rootWidth } ?: 0f
                val maxCollapsePx = with(density) { COLLAPSE_RANGE.toPx() }
                val ambientRangePx = with(density) { AMBIENT_RANGE_DP.dp.toPx() }
                val connection = remember(maxCollapsePx, ambientRangePx, compatibility, wide) {
                    object : NestedScrollConnection {
                        override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                            if (available.y != 0f) scrollTick.intValue++
                            if (ambient.floatValue > 0f && source == NestedScrollSource.UserInput) {
                                val next = (ambient.floatValue + available.y / ambientRangePx).coerceIn(0f, 1f)
                                val used = (next - ambient.floatValue) * ambientRangePx
                                ambient.floatValue = next
                                return Offset(0f, used)
                            }
                            if (wide || available.y >= 0f || collapse.floatValue >= 1f) return Offset.Zero
                            val next = (collapse.floatValue - available.y / maxCollapsePx).coerceIn(0f, 1f)
                            val consumed = -(next - collapse.floatValue) * maxCollapsePx
                            collapse.floatValue = next
                            return Offset(0f, consumed)
                        }

                        override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
                            if (available.y > 0f && collapse.floatValue <= 0f && source == NestedScrollSource.UserInput) {
                                val next = (ambient.floatValue + available.y / ambientRangePx).coerceIn(0f, 1f)
                                val used = (next - ambient.floatValue) * ambientRangePx
                                ambient.floatValue = next
                                return Offset(0f, used)
                            }
                            if (available.y <= 0f || collapse.floatValue <= 0f) return Offset.Zero
                            val next = (collapse.floatValue - available.y / maxCollapsePx).coerceIn(0f, 1f)
                            val used = -(next - collapse.floatValue) * maxCollapsePx
                            collapse.floatValue = next
                            return Offset(0f, used)
                        }

                        override suspend fun onPreFling(available: Velocity): Velocity {
                            val current = ambient.floatValue
                            if (current <= 0f || current >= 1f) return Velocity.Zero
                            val target = when {
                                available.y > AMBIENT_SETTLE_VELOCITY -> 1f
                                available.y < -AMBIENT_SETTLE_VELOCITY -> 0f
                                current > 0.5f -> 1f
                                else -> 0f
                            }
                            settleAmbient(ambient, target, compatibility)
                            if (target == 1f && current < 1f) HapticUtil.performConfirmHaptic(view)
                            return available
                        }
                    }
                }
                if (snapshot == null) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        if (state.loading || state.error == null) {
                            LoadingIndicator(Modifier.size(96.dp))
                        } else {
                            IconButton(
                                onClick = {
                                    HapticUtil.performVirtualKeyHaptic(view)
                                    scope.launch { WeatherRepository.refresh(context, force = true) }
                                },
                                modifier = Modifier.size(96.dp),
                            ) {
                                Icon(painterResource(R.drawable.rounded_refresh_24), stringResource(R.string.weather_refresh), tint = palette.onBase, modifier = Modifier.size(40.dp))
                            }
                        }
                        Spacer(Modifier.height(32.dp))
                        IconButton(
                            onClick = {
                                HapticUtil.performVirtualKeyHaptic(view)
                                onOpenSettings()
                            },
                            modifier = Modifier.size(56.dp),
                        ) {
                            Icon(painterResource(R.drawable.rounded_settings_24), stringResource(R.string.weather_open_settings), tint = palette.onBase, modifier = Modifier.size(28.dp))
                        }
                    }
                } else {
                    var topPx by headerBottom
                    val bottomFadePx = WindowInsets.navigationBars.getBottom(density) + with(density) { 20.dp.toPx() }
                    Box(Modifier.fillMaxSize()) {
                        Box(Modifier.fillMaxSize().graphicsLayer { alpha = contentAlpha }.nestedScroll(connection)) {
                            Box(
                                Modifier
                                    .fillMaxSize()
                                    .graphicsLayer {
                                        compositingStrategy = CompositingStrategy.Offscreen
                                        alpha = (1f - ambient.floatValue * 1.8f).coerceIn(0f, 1f)
                                    }
                                    .drawWithContent {
                                        drawContent()
                                        if (topFadePx > 0f) {
                                            drawRect(
                                                brush = Brush.verticalGradient(
                                                    colors = listOf(Color.Transparent, Color.Black),
                                                    startY = 0f,
                                                    endY = topFadePx,
                                                ),
                                                blendMode = BlendMode.DstIn,
                                            )
                                        }
                                        drawRect(
                                            brush = Brush.verticalGradient(
                                                colors = listOf(Color.Black, Color.Transparent),
                                                startY = size.height - bottomFadePx,
                                                endY = size.height,
                                            ),
                                            topLeft = Offset(0f, size.height - bottomFadePx),
                                            size = Size(size.width, bottomFadePx),
                                            blendMode = BlendMode.DstIn,
                                        )
                                    }
                                    .progressiveBlur(blurRadius = 40f, height = topFadePx, direction = BlurDirection.TOP, showGradientOverlay = false)
                                    .progressiveBlur(blurRadius = 14f, height = bottomFadePx, direction = BlurDirection.BOTTOM, showGradientOverlay = false),
                            ) {
                                if (!wide) {
                                    Column(
                                        Modifier
                                            .fillMaxSize()
                                            .verticalScroll(scrollState)
                                            .padding(top = with(density) { topPx.toDp() } + 28.dp)
                                            .navigationBarsPadding()
                                            .padding(bottom = 40.dp),
                                        verticalArrangement = Arrangement.spacedBy(20.dp),
                                    ) {
                                        AlertsSection(snapshot.activeAlerts().sortedByDescending { it.severity.ordinal }, palette)
                                        HourlySection(snapshot, units.temperature, palette)
                                        snapshot.upcomingDays().takeIf { it.isNotEmpty() }
                                            ?.let { DailySection(it, units.temperature, palette, Modifier.padding(horizontal = SIDE_PADDING)) }
                                        DetailsSection(snapshot, units, palette, Modifier.padding(horizontal = SIDE_PADDING))
                                        AiAdviceSection(snapshot, palette, Modifier.padding(horizontal = SIDE_PADDING))
                                        AqiSection(palette, Modifier.padding(horizontal = SIDE_PADDING))
                                        SunSection(snapshot, palette, Modifier.padding(horizontal = SIDE_PADDING))
                                        Footer(
                                            modifier = Modifier.padding(horizontal = SIDE_PADDING).rainSurface("footer"),
                                            snapshot = snapshot,
                                            error = state.error,
                                            palette = palette,
                                            refreshing = state.loading,
                                            onRefresh = {
                                                HapticUtil.performVirtualKeyHaptic(view)
                                                scope.launch { WeatherRepository.refresh(context, force = true) }
                                            },
                                            onOpenSettings = onOpenSettings,
                                        )
                                    }
                                } else {
                                    Column(
                                        Modifier
                                            .fillMaxSize()
                                            .verticalScroll(scrollState)
                                            .navigationBarsPadding()
                                            .padding(bottom = 40.dp),
                                    ) {
                                        Row(Modifier.fillMaxWidth()) {
                                            Column(
                                                Modifier
                                                    .weight(leftFraction)
                                                    .padding(top = with(density) { topPx.toDp() } + 28.dp),
                                                verticalArrangement = Arrangement.spacedBy(20.dp),
                                            ) {
                                            AlertsSection(snapshot.activeAlerts().sortedByDescending { it.severity.ordinal }, palette)
                                            snapshot.upcomingDays().takeIf { it.isNotEmpty() }
                                                ?.let { DailySection(it, units.temperature, palette, Modifier.padding(horizontal = SIDE_PADDING)) }
                                            SunSection(snapshot, palette, Modifier.padding(horizontal = SIDE_PADDING))
                                            }
                                            if (hingeFraction > 0f) Spacer(Modifier.weight(hingeFraction))
                                            Column(
                                                Modifier
                                                    .weight((1f - leftFraction - hingeFraction).coerceAtLeast(0.05f))
                                                    .statusBarsPadding()
                                                    .padding(top = 28.dp),
                                                verticalArrangement = Arrangement.spacedBy(20.dp),
                                            ) {
                                            HourlySection(snapshot, units.temperature, palette)
                                            DetailsSection(snapshot, units, palette, Modifier.padding(horizontal = SIDE_PADDING))
                                            AiAdviceSection(snapshot, palette, Modifier.padding(horizontal = SIDE_PADDING))
                                            AqiSection(palette, Modifier.padding(horizontal = SIDE_PADDING))
                                            Footer(
                                                modifier = Modifier.padding(horizontal = SIDE_PADDING).rainSurface("footer"),
                                                snapshot = snapshot,
                                                error = state.error,
                                                palette = palette,
                                                refreshing = state.loading,
                                                onRefresh = {
                                                    HapticUtil.performVirtualKeyHaptic(view)
                                                    scope.launch { WeatherRepository.refresh(context, force = true) }
                                                },
                                                onOpenSettings = onOpenSettings,
                                            )
                                            }
                                        }
                                    }
                                }
                            }
                            if (timelineActive && immersive) {
                                Box(
                                    Modifier
                                        .fillMaxSize()
                                        .timelineGestures(
                                            timeline = timeline,
                                            scope = scope,
                                            stepPx = (rootWidthPx.intValue / TIMELINE_STEPS_PER_SCREEN).coerceAtLeast(1f),
                                            onVerticalDrag = { dy -> ambient.floatValue = (ambient.floatValue + dy / ambientRangePx).coerceIn(0f, 1f) },
                                            onVerticalEnd = {
                                                scope.launch { settleAmbient(ambient, if (ambient.floatValue > 0.5f) 1f else 0f, compatibility) }
                                            },
                                        ),
                                )
                            }
                            val showAmbientForecast = settings.isAmbientForecastEnabled() && !timelineActive &&
                                ambientForecastFits(rootHeightPx.intValue, ambientClockPx.intValue, ambientForecastPx.intValue)
                            Column(
                                Modifier
                                    .align(if (wide) Alignment.TopStart else Alignment.TopCenter)
                                    .then(if (wide) Modifier.fillMaxWidth(lerp(leftFraction, 1f, ambient.floatValue)) else Modifier.fillMaxWidth())
                                    .graphicsLayer {
                                        if (wide && ambient.floatValue == 0f) translationY = -scrollState.value.toFloat()
                                    }
                                    .onSizeChanged { if (ambient.floatValue == 0f) topPx = it.height.toFloat() }
                                    .statusBarsPadding(),
                                horizontalAlignment = Alignment.CenterHorizontally,
                            ) {
                                val progress = collapse.floatValue
                                Spacer(Modifier.height(14.dp))
                                Box(Modifier.foldAway(maxOf(((progress - 0.25f) / 0.5f).coerceIn(0f, 1f), ambient.floatValue))) {
                                    LocationChip(snapshot, palette, Modifier.padding(horizontal = SIDE_PADDING), onClick = { showLocations = true })
                                }
                                val ambientInset = WindowInsets.statusBars.getTop(density) + with(density) { 14.dp.roundToPx() }
                                Header(
                                    snapshot,
                                    units.temperature,
                                    palette,
                                    progress,
                                    ambient.floatValue,
                                    Modifier
                                        .padding(horizontal = SIDE_PADDING)
                                        .ambientCenter(
                                            ambient.floatValue,
                                            (rootHeightPx.intValue - ambientInset * 2).coerceAtLeast(0),
                                            rememberAnimatedAvailable(
                                                ambientAvailable(rootHeightPx.intValue, ambientClockPx.intValue, if (showAmbientForecast || timelineActive) ambientForecastPx.intValue else 0),
                                            ),
                                        ),
                                )
                            }
                            if (ambient.floatValue > 0f) {
                                AmbientClock(
                                    { ambient.floatValue },
                                    palette,
                                    Modifier.align(Alignment.TopCenter).onSizeChanged { ambientClockPx.intValue = it.height },
                                    timeMillis = presentation.clockLabel.takeIf { timelineActive },
                                    showReturn = timelineActive && showTimelineReturn,
                                    atNow = presentation.timelineAtNow,
                                    onReturn = {
                                        HapticUtil.performVirtualKeyHaptic(view)
                                        timeline.animateTo(scope, 0f)
                                    },
                                )
                                if (timelineActive) {
                                    TimelineRain(
                                        { ambient.floatValue },
                                        snapshot?.chanceOfRain,
                                        palette,
                                        Modifier.align(Alignment.BottomCenter).onSizeChanged { ambientForecastPx.intValue = it.height },
                                    )
                                }
                                if (showAmbientForecast) {
                                    AmbientForecast({ ambient.floatValue }, snapshot, units.temperature, palette, Modifier.align(Alignment.BottomCenter).onSizeChanged { ambientForecastPx.intValue = it.height })
                                }
                            }
                        }
                    }
                }
        }
        }
    }
}

internal fun skyState(snapshot: WeatherSnapshot, now: Long): Pair<Float, Boolean>? {
    val rise = snapshot.extras?.sunriseMillis ?: return null
    val set = snapshot.extras?.sunsetMillis ?: return null
    val day = 24 * 60 * 60_000L
    val shift = Math.floorDiv(now - rise, day) * day
    val r = rise + shift
    val s = set + shift
    val isSun = now in r..s
    val t = when {
        isSun -> (now - r).toFloat() / (s - r).coerceAtLeast(1L)
        now > s -> (now - s).toFloat() / ((r + day) - s).coerceAtLeast(1L)
        else -> (now - (s - day)).toFloat() / (r - (s - day)).coerceAtLeast(1L)
    }.coerceIn(0f, 1f)
    return t to isSun
}

internal val SKY_HEIGHT = 360.dp

@Composable
internal fun SkyBody(snapshot: WeatherSnapshot, now: () -> Long, collapse: () -> Float, rise: () -> Float = { 1f }) {
    val hide = (1f - collapse() * 1.6f).coerceIn(0f, 1f)
    if (hide <= 0f) return
    Canvas(
        Modifier
            .fillMaxWidth()
            .height(SKY_HEIGHT)
            .graphicsLayer { translationY = -collapse() * 60.dp.toPx() },
    ) {
        val time = now()
        val (t, isSun) = skyState(snapshot, time) ?: return@Canvas
        val color = if (isSun) Color(0xFFFFE2A8) else Color(0xFFE6ECFF)
        val moonPhase = snapshot.moonPhaseAt(time)
        val shown = t * rise()
        val horizonFade = (minOf(shown, 1f - shown) / 0.1f).coerceIn(0f, 1f)
        val x = size.width * (0.9f - 0.8f * shown)
        val y = size.height * 0.66f - size.height * 0.4f * kotlin.math.sin(Math.PI.toFloat() * shown)
        val alpha = 0.5f * hide * horizonFade
        val glowRadius = 110.dp.toPx()
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(color.copy(alpha = alpha * 0.55f), Color.Transparent),
                center = Offset(x, y),
                radius = glowRadius,
            ),
            radius = glowRadius,
            center = Offset(x, y),
        )
        if (isSun) {
            drawCircle(color.copy(alpha = alpha * 0.7f), radius = 20.dp.toPx(), center = Offset(x, y))
        } else {
            drawMoonPhase(Offset(x, y), 20.dp.toPx(), moonPhase, color, litAlpha = alpha * 0.8f, darkAlpha = alpha * 0.16f)
        }
    }
}

internal val LocalRainSurfaces = androidx.compose.runtime.staticCompositionLocalOf<SnapshotStateMap<String, RainSurfaceSource>?> { null }

internal class RainSurfaceSource(val corner: Float, val mask: (() -> Rect)?) {
    var coordinates: LayoutCoordinates? = null

    fun resolve(key: String): RainSurface? {
        val c = coordinates?.takeIf { it.isAttached } ?: return null
        val position = c.positionInRoot()
        val local = Rect(Offset.Zero, Size(c.size.width.toFloat(), c.size.height.toFloat()))
        val visible = (mask?.invoke()?.intersect(local) ?: local).translate(position)
        if (visible.width <= 0f || visible.height <= 0f) return null
        return RainSurface(key, visible, corner, position.x, local.width)
    }
}

@Composable
internal fun Modifier.rainSurface(key: String, corner: Dp = 28.dp, mask: (() -> Rect)? = null): Modifier {
    val registry = LocalRainSurfaces.current ?: return this
    val cornerPx = with(LocalDensity.current) { corner.toPx() }
    val source = remember(key, cornerPx) { RainSurfaceSource(cornerPx, mask) }
    DisposableEffect(key, source) {
        registry[key] = source
        onDispose { registry.remove(key) }
    }
    return this.onGloballyPositioned { source.coordinates = it }
}

internal val SIDE_PADDING = 20.dp
private const val CONTENT_FADE_MS = 500
private const val EFFECTS_DELAY_MS = 450L
private const val EFFECTS_FADE_MS = 1800
private const val REFRESH_DELAY_MS = 600L
private val COLLAPSE_RANGE = 520.dp
private const val QUARTER_HOUR_MS = 15 * 60_000L
private const val TIMELINE_STEPS_PER_SCREEN = 4f
private val TIMELINE_RETURN_SIZE = 48.dp
private const val TWO_PANE_MIN_WIDTH_DP = 600

@Composable
private fun rememberVerticalFold(): State<android.graphics.Rect?> {
    val context = LocalContext.current
    val activity = remember(context) { context.findActivity() }
    return produceState<android.graphics.Rect?>(null, activity) {
        if (activity == null) return@produceState
        WindowInfoTracker.getOrCreate(activity).windowLayoutInfo(activity).collect { info ->
            value = info.displayFeatures
                .filterIsInstance<FoldingFeature>()
                .firstOrNull { it.isSeparating && it.orientation == FoldingFeature.Orientation.VERTICAL }
                ?.bounds
        }
    }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
private const val TWO_PANE_LEFT_WEIGHT = 0.42f
private const val COMBINE_AT = 0.9f
private const val HEADER_EXTRAS_DP = 170f

@Composable
private fun LocationChip(snapshot: WeatherSnapshot, palette: WeatherPalette, modifier: Modifier, onClick: () -> Unit) {
    val view = LocalView.current
    val place = listOf(snapshot.locationName, snapshot.region).filter { it.isNotBlank() }.joinToString(", ")
        .ifBlank { stringResource(R.string.weather_choose_location) }
    AssistChip(
        onClick = {
            HapticUtil.performVirtualKeyHaptic(view)
            onClick()
        },
        modifier = modifier,
        label = { Text(place, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.titleMedium) },
        leadingIcon = { Icon(painterResource(R.drawable.rounded_location_on_24), null, modifier = Modifier.size(20.dp)) },
        shape = CircleShape,
        border = null,
        colors = AssistChipDefaults.assistChipColors(
            containerColor = palette.card,
            labelColor = palette.onBase,
            leadingIconContentColor = palette.accent,
        ),
    )
}

internal fun Modifier.foldAway(fraction: Float): Modifier =
    this
        .layout { measurable, constraints ->
            val placeable = measurable.measure(constraints)
            layout(placeable.width, (placeable.height * (1f - fraction)).roundToInt()) {
                if (fraction < 1f / 1.4f) placeable.placeRelative(0, 0)
            }
        }
        .graphicsLayer { alpha = (1f - fraction * 1.4f).coerceIn(0f, 1f) }

private val temperatureFonts = ConcurrentHashMap<Int, FontFamily>()

@OptIn(ExperimentalTextApi::class)
internal fun temperatureFont(widthAxis: Int, weightAxis: Int): FontFamily =
    temperatureFonts.getOrPut(widthAxis * 10_000 + weightAxis) {
        FontFamily(
            Font(
                R.font.google_sans_flex,
                weight = FontWeight(weightAxis),
                variationSettings = FontVariation.Settings(
                    FontVariation.width(widthAxis.toFloat()),
                    FontVariation.weight(weightAxis),
                    FontVariation.Setting("ROND", 100f),
                ),
            ),
        )
    }

@Composable
internal fun Header(snapshot: WeatherSnapshot, unit: TemperatureUnit, palette: WeatherPalette, progress: Float, ambient: Float, modifier: Modifier) {
    val number = WeatherFormat.temperature(snapshot.tempC, unit).removeSuffix("°")
    var previousNumber by remember { mutableStateOf(number) }
    val numberRising = (number.toIntOrNull() ?: 0) >= (previousNumber.toIntOrNull() ?: 0)
    SideEffect { previousNumber = number }
    
    val screenHeightDp = LocalConfiguration.current.screenHeightDp
    val fontScale = LocalDensity.current.fontScale
    
    val heightCap = ((screenHeightDp * 0.6f - HEADER_EXTRAS_DP) / 0.86f / fontScale).coerceAtLeast(64f)
    val expanded = minOf(if (number.length >= 3) 290f else 390f, heightCap)
    val font = temperatureFont(lerp(52f, 125f, progress).roundToInt(), lerp(1f, 800f, progress).roundToInt())
    val ambientMax = screenHeightDp * 0.72f / 0.86f / fontScale
    val letterSpacing = lerp(lerp(-14f, -1f, progress), -14f * ambientMax / 390f, ambient)
    val textMeasurer = rememberTextMeasurer()
    val hazeStrength = ((snapshot.tempC - 30.0) / 12.0).toFloat().coerceIn(0f, 1f) * (1f - progress * 5f).coerceIn(0f, 1f)
    var combined by remember { mutableStateOf(false) }
    if (progress >= COMBINE_AT) combined = true else if (progress < COMBINE_AT - 0.08f) combined = false
    val morph by animateFloatAsState(if (combined) 1f else 0f, tween(320), label = "weatherHeaderMorph")
    BoxWithConstraints(modifier.fillMaxWidth()) {
        val size = fitDigitSize(textMeasurer, number, font, letterSpacing, lerp(lerp(expanded, 44f, progress), ambientMax, ambient), constraints.maxWidth * DIGITS_MAX_WIDTH_FRACTION).sp
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            Spacer(Modifier.height(lerp(20f, 0f, progress).dp))
            TemperatureAndCondition(
                progress = morph,
                gap = lerp(4f, 16f, morph).dp,
                digits = {
                    Row(verticalAlignment = Alignment.Top, modifier = Modifier.heatHaze(hazeStrength)) {
                        DegreeSign(size, font, Color.Transparent)
                        RollingClock(
                            number,
                            palette.onBase,
                            numberRising,
                            TextStyle(fontFamily = font, fontSize = size, lineHeight = size * 0.86f, letterSpacing = letterSpacing.sp),
                        )
                        DegreeSign(size, font, palette.onBase)
                    }
                },
                condition = {
                    AnimatedContent(
                        targetState = Triple(snapshot.condition, snapshot.isDay, snapshot.conditionText),
                        transitionSpec = { (slideInVertically { it } + fadeIn()) togetherWith (slideOutVertically { -it } + fadeOut()) },
                        label = "headerCondition",
                    ) { (condition, isDay, text) ->
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            WeatherIcon(condition, isDay, palette.accent, Modifier.size(32.dp))
                            Text(text, color = palette.onBase, style = MaterialTheme.typography.headlineSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                },
            )
            Column(Modifier.foldAway(maxOf((progress / 0.5f).coerceIn(0f, 1f), ambient))) {
                Spacer(Modifier.height(6.dp))
                Text(
                    listOf(
                        stringResource(R.string.weather_high_low, WeatherFormat.temperature(snapshot.todayHighC(), unit), WeatherFormat.temperature(snapshot.todayLowC(), unit)),
                        stringResource(R.string.weather_feels_like, WeatherFormat.temperature(snapshot.feelsLikeC, unit)),
                    ).joinToString(" · "),
                    color = palette.onBaseMuted,
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
        }
    }
}

private const val DIGITS_MAX_WIDTH_FRACTION = 0.95f

private fun fitDigitSize(
    measurer: TextMeasurer,
    number: String,
    font: FontFamily,
    letterSpacing: Float,
    sizeSp: Float,
    maxWidthPx: Float,
): Float {
    if (maxWidthPx <= 0f) return sizeSp
    var size = sizeSp
    repeat(3) {
        val digits = measurer.measure(
            number,
            TextStyle(fontFamily = font, fontSize = size.sp, letterSpacing = letterSpacing.sp),
            maxLines = 1,
            softWrap = false,
        ).size.width
        val degree = measurer.measure(
            "\u00b0",
            TextStyle(fontFamily = font, fontSize = (size * 0.42f).sp),
            maxLines = 1,
            softWrap = false,
        ).size.width
        val total = digits + 2 * degree
        if (total <= maxWidthPx) return size
        size *= maxWidthPx / total
    }
    return size
}

@Composable
private fun TemperatureAndCondition(
    progress: Float,
    gap: Dp,
    digits: @Composable () -> Unit,
    condition: @Composable () -> Unit,
) {
    Layout(content = { digits(); condition() }) { measurables, constraints ->
        val gapPx = gap.roundToPx()
        val loose = constraints.copy(minWidth = 0, minHeight = 0)
        val d = measurables[0].measure(loose)
        val cMax = lerp(constraints.maxWidth.toFloat(), (constraints.maxWidth - d.width - gapPx).coerceAtLeast(0).toFloat(), progress).roundToInt()
        val c = measurables[1].measure(loose.copy(maxWidth = cMax))
        val width = constraints.maxWidth
        val expandedHeight = d.height + gapPx + c.height
        val collapsedHeight = maxOf(d.height, c.height)
        val height = lerp(expandedHeight.toFloat(), collapsedHeight.toFloat(), progress).roundToInt()
        val total = d.width + gapPx + c.width
        val collapsedStart = (width - total) / 2
        layout(width, height) {
            val dx = lerp(((width - d.width) / 2).toFloat(), collapsedStart.toFloat(), progress).roundToInt()
            val dy = lerp(0f, ((collapsedHeight - d.height) / 2).toFloat(), progress).roundToInt()
            val cx = lerp(((width - c.width) / 2).toFloat(), (collapsedStart + d.width + gapPx).toFloat(), progress).roundToInt()
            val cy = lerp((d.height + gapPx).toFloat(), ((collapsedHeight - c.height) / 2).toFloat(), progress).roundToInt()
            d.placeRelative(dx, dy)
            c.placeRelative(cx, cy)
        }
    }
}

@Composable
private fun DegreeSign(digitSize: TextUnit, font: FontFamily, color: Color) {
    val size = digitSize * 0.42f
    Text(
        "°",
        color = color,
        fontFamily = font,
        fontSize = size,
        lineHeight = size,
        maxLines = 1,
        softWrap = false,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AlertsSection(alerts: List<WeatherAlert>, palette: WeatherPalette) {
    when {
        alerts.isEmpty() -> Unit
        alerts.size == 1 -> {
            val alert = alerts.first()
            AlertCard(
                alert,
                palette,
                Modifier
                    .padding(horizontal = SIDE_PADDING)
                    .rainSurface("alert:${alert.id}")
                    .fillMaxWidth()
                    .clip(MaterialTheme.shapes.extraLarge),
                compact = false,
            )
        }

        else -> BoxWithConstraints(Modifier.fillMaxWidth()) {
            val spacing = 4.dp
            val smallItemWidth = 44.dp
            val carouselState = rememberCarouselState { alerts.size }
            HorizontalMultiBrowseCarousel(
                state = carouselState,
                preferredItemWidth = maxWidth - SIDE_PADDING * 2 - smallItemWidth - spacing,
                itemSpacing = spacing,
                minSmallItemWidth = smallItemWidth,
                maxSmallItemWidth = smallItemWidth,
                contentPadding = PaddingValues(horizontal = SIDE_PADDING),
                modifier = Modifier.fillMaxWidth().height(ALERT_CAROUSEL_HEIGHT),
            ) { index ->
                val alert = alerts[index]
                AlertCard(
                    alert,
                    palette,
                    Modifier
                        .rainSurface("alert:${alert.id}", mask = { carouselItemDrawInfo.maskRect })
                        .fillMaxSize()
                        .maskClip(MaterialTheme.shapes.extraLarge),
                    compact = true,
                )
            }
        }
    }
}

private val ALERT_CAROUSEL_HEIGHT = 190.dp

@Composable
private fun AlertCard(alert: WeatherAlert, palette: WeatherPalette, modifier: Modifier, compact: Boolean) {
    val context = LocalContext.current
    val alertColor = MaterialTheme.colorScheme.error
    Column(
        modifier
            .background(alertColor.copy(alpha = 0.22f))
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Icon(painterResource(R.drawable.rounded_warning_24), null, tint = alertColor, modifier = Modifier.size(24.dp))
            Text(
                alert.event,
                color = palette.onBase,
                style = MaterialTheme.typography.titleMedium,
                maxLines = if (compact) 1 else Int.MAX_VALUE,
                softWrap = !compact,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
        }
        alert.expiresMillis?.let {
            Text(
                stringResource(R.string.weather_alert_until, formatTime(context, it)),
                color = palette.onBaseMuted,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                softWrap = false,
            )
        }
        if (alert.headline != alert.event) {
            Text(
                alert.headline,
                color = palette.onBase,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = if (compact) 2 else Int.MAX_VALUE,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (alert.description.isNotBlank()) {
            Text(
                alert.description,
                color = palette.onBaseMuted,
                style = MaterialTheme.typography.bodySmall,
                maxLines = if (compact) 3 else Int.MAX_VALUE,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun HourlySection(snapshot: WeatherSnapshot, unit: TemperatureUnit, palette: WeatherPalette, surfacePrefix: String = "hourly") {
    val context = LocalContext.current
    val now by rememberMinuteClock()
    val hours = remember(snapshot, now) { snapshot.upcomingHours(now) }
    if (hours.isEmpty()) return
    val carouselState = key(hours.size) { rememberCarouselState { hours.size } }
    Column {
        HorizontalMultiBrowseCarousel(
            state = carouselState,
            preferredItemWidth = 104.dp,
            itemSpacing = 4.dp,
            contentPadding = PaddingValues(horizontal = SIDE_PADDING),
            modifier = Modifier.fillMaxWidth().height(176.dp),
        ) { index ->
            val hour = hours[index]
            Column(
                Modifier
                    .rainSurface("$surfacePrefix:$index", mask = { carouselItemDrawInfo.maskRect })
                    .fillMaxSize()
                    .maskClip(MaterialTheme.shapes.extraLarge)
                    .background(palette.card)
                    .padding(vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(formatHour(context, hour.timeMillis), color = palette.onBaseMuted, style = MaterialTheme.typography.labelLarge, maxLines = 1, softWrap = false)
                WeatherIcon(hour.condition, hour.isDay, palette.accent, Modifier.size(32.dp))
                Text(WeatherFormat.temperature(hour.tempC, unit), color = palette.onBase, style = MaterialTheme.typography.titleLarge, maxLines = 1, softWrap = false)
                Text(
                    if (hour.chanceOfRain > 0) "${hour.chanceOfRain}%" else " ",
                    color = palette.accent,
                    style = MaterialTheme.typography.labelMedium,
                    maxLines = 1,
                    softWrap = false,
                )
            }
        }
    }
}

@Composable
private fun DailySection(days: List<DailyForecast>, unit: TemperatureUnit, palette: WeatherPalette, modifier: Modifier) {
    val low = days.minOf { it.lowC }
    val high = days.maxOf { it.highC }
    val span = (high - low).coerceAtLeast(1.0)
    Column(modifier) {
        RoundedCardContainer(modifier = Modifier.rainSurface("daily"), spacing = 2.dp, cornerRadius = 28.dp) {
            days.forEachIndexed { index, day ->
                Surface(color = palette.card, shape = MaterialTheme.shapes.extraSmall, modifier = Modifier.fillMaxWidth()) {
                    Row(
                        Modifier.padding(horizontal = 20.dp, vertical = 18.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Text(
                            if (index == 0) stringResource(R.string.weather_detail_today) else dayName(day.dayMillis),
                            color = palette.onBase,
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.width(64.dp),
                            maxLines = 1,
                        )
                        WeatherIcon(day.condition, true, palette.accent, Modifier.size(26.dp))
                        Text(
                            if (day.chanceOfRain > 0) "${day.chanceOfRain}%" else "",
                            color = palette.accent,
                            style = MaterialTheme.typography.labelMedium,
                            modifier = Modifier.width(36.dp),
                        )
                        Text(WeatherFormat.temperature(day.lowC, unit), color = palette.onBaseMuted, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.width(38.dp), textAlign = TextAlign.End)
                        Box(Modifier.weight(1f).height(8.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.12f))) {
                            val start = ((day.lowC - low) / span).toFloat().coerceIn(0f, 1f)
                            val end = ((day.highC - low) / span).toFloat().coerceIn(start, 1f)
                            Row(Modifier.matchParentSize()) {
                                if (start > 0f) Spacer(Modifier.weight(start))
                                Box(Modifier.weight((end - start).coerceAtLeast(0.05f)).fillMaxHeight().clip(CircleShape).background(palette.accent))
                                if (end < 1f) Spacer(Modifier.weight(1f - end))
                            }
                        }
                        Text(WeatherFormat.temperature(day.highC, unit), color = palette.onBase, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.width(38.dp))
                    }
                }
            }
        }
    }
}

private class Detail(val slot: WeatherDetailSlot, val label: Int, val value: String)

@Composable
private fun DetailsSection(snapshot: WeatherSnapshot, units: WeatherUnits, palette: WeatherPalette, modifier: Modifier) {
    val extras = snapshot.extras
    val details = buildList {
        snapshot.humidity?.let { add(Detail(WeatherDetailSlot.HUMIDITY, R.string.weather_detail_humidity, "$it%")) }
        add(
            Detail(
                WeatherDetailSlot.WIND,
                R.string.weather_detail_wind,
                WeatherFormat.wind(snapshot.windKph, units.wind) + (extras?.windDirectionDeg?.let { " ${compass(it)}" } ?: ""),
            ),
        )
        extras?.windGustKph?.let { add(Detail(WeatherDetailSlot.GUSTS, R.string.weather_detail_gusts, WeatherFormat.wind(it, units.wind))) }
        snapshot.chanceOfRain?.let { add(Detail(WeatherDetailSlot.RAIN_CHANCE, R.string.weather_detail_rain_chance, "$it%")) }
        extras?.precipitationMm?.let {
            add(Detail(WeatherDetailSlot.PRECIPITATION, R.string.weather_detail_precipitation, WeatherFormat.precipitation(it, units.precipitation)))
        }
        extras?.uvIndex?.let { add(Detail(WeatherDetailSlot.UV, R.string.weather_detail_uv, it.roundToInt().toString())) }
        extras?.pressureHpa?.let {
            add(Detail(WeatherDetailSlot.PRESSURE, R.string.weather_detail_pressure, WeatherFormat.pressure(it, units.pressure)))
        }
        extras?.visibilityKm?.let {
            add(Detail(WeatherDetailSlot.VISIBILITY, R.string.weather_detail_visibility, WeatherFormat.distance(it, units.distance)))
        }
        extras?.dewPointC?.let { add(Detail(WeatherDetailSlot.DEW_POINT, R.string.weather_detail_dew_point, WeatherFormat.temperature(it, units.temperature))) }
        extras?.cloudCover?.let { add(Detail(WeatherDetailSlot.CLOUD_COVER, R.string.weather_detail_cloud_cover, "$it%")) }
    }
    Column(modifier) {
        Surface(color = palette.card, shape = MaterialTheme.shapes.extraLarge, modifier = Modifier.rainSurface("details").fillMaxWidth()) {
            Column(Modifier.padding(vertical = 12.dp, horizontal = 8.dp)) {
                details.chunked(2).forEach { row ->
                    Row(Modifier.fillMaxWidth()) {
                        row.forEach { DetailTile(it, palette, Modifier.weight(1f)) }
                        if (row.size == 1) Spacer(Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
private fun DetailTile(detail: Detail, palette: WeatherPalette, modifier: Modifier) {
    Row(
        modifier.padding(horizontal = 12.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        WeatherDetailIcon(detail.slot, palette.accent, Modifier.size(26.dp))
        Column {
            Text(stringResource(detail.label), color = palette.onBaseMuted, style = MaterialTheme.typography.labelMedium)
            Text(detail.value, color = palette.onBase, style = MaterialTheme.typography.titleMedium, maxLines = 1)
        }
    }
}

@Composable
private fun SunSection(snapshot: WeatherSnapshot, palette: WeatherPalette, modifier: Modifier) {
    val rise = snapshot.extras?.sunriseMillis
    val set = snapshot.extras?.sunsetMillis
    val context = LocalContext.current
    val now by rememberMinuteClock()
    val phase = snapshot.moonPhaseAt(now)
    Column(modifier) {
        RoundedCardContainer(modifier = Modifier.rainSurface("sun"), spacing = 2.dp, cornerRadius = 28.dp) {
            if (rise != null && set != null) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    SunTile(R.string.weather_detail_sunrise, formatTime(context, rise), palette, Modifier.weight(1f))
                    SunTile(R.string.weather_detail_sunset, formatTime(context, set), palette, Modifier.weight(1f))
                }
            }
            MoonTile(phase, palette, Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun MoonTile(phase: Double, palette: WeatherPalette, modifier: Modifier) {
    Surface(color = palette.card, shape = MaterialTheme.shapes.extraSmall, modifier = modifier) {
        Row(Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Canvas(Modifier.size(40.dp)) {
                drawMoonPhase(center, size.minDimension / 2f, phase, palette.onBase, litAlpha = 0.95f, darkAlpha = 0.18f)
            }
            Column {
                Text(stringResource(R.string.weather_detail_moon), color = palette.onBaseMuted, style = MaterialTheme.typography.labelMedium)
                Text(stringResource(MoonPhase.nameRes(phase)), color = palette.onBase, style = MaterialTheme.typography.headlineSmall)
                Text("${MoonPhase.illuminationPercent(phase)}%", color = palette.onBaseMuted, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@Composable
private fun SunTile(label: Int, time: String, palette: WeatherPalette, modifier: Modifier) {
    Surface(color = palette.card, shape = MaterialTheme.shapes.extraSmall, modifier = modifier) {
        Column(Modifier.padding(20.dp)) {
            Text(stringResource(label), color = palette.onBaseMuted, style = MaterialTheme.typography.labelMedium)
            Text(time, color = palette.onBase, style = MaterialTheme.typography.headlineSmall)
        }
    }
}

@Composable
private fun Footer(
    modifier: Modifier,
    snapshot: WeatherSnapshot,
    error: WeatherError?,
    palette: WeatherPalette,
    refreshing: Boolean,
    onRefresh: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    val context = LocalContext.current
    val view = LocalView.current
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(snapshot.updatedAt) {
        while (true) {
            now = System.currentTimeMillis()
            delay(30_000L)
        }
    }
    val age = DateUtils.getRelativeTimeSpanString(
        snapshot.updatedAt,
        maxOf(now, snapshot.updatedAt),
        DateUtils.MINUTE_IN_MILLIS,
        DateUtils.FORMAT_ABBREV_RELATIVE,
    ).toString()
    val colors = ButtonDefaults.buttonColors(containerColor = palette.card, contentColor = palette.onBase)
    val height = SplitButtonDefaults.MediumContainerHeight
    SplitButtonLayout(
        modifier = modifier.fillMaxWidth(),
        leadingButton = {
            Surface(
                onClick = onRefresh,
                modifier = Modifier.fillMaxWidth().height(height),
                shape = SplitButtonDefaults.leadingButtonShapesFor(height).shape,
                color = palette.card,
                contentColor = palette.onBase,
            ) {
                Row(
                    modifier = Modifier.padding(start = 20.dp, end = 20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.Center,
                    ) {
                        Text(
                            text = error?.let { errorLabel(context, it) } ?: providerLabel(snapshot.providerId),
                            color = palette.accent,
                            style = MaterialTheme.typography.titleSmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text = stringResource(R.string.weather_updated_at, age),
                            style = MaterialTheme.typography.labelMedium,
                            color = palette.onBaseMuted,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    RefreshIcon(refreshing, palette.accent)
                }
            }
        },
        trailingButton = {
            SplitButtonDefaults.TrailingButton(
                onClick = {
                    HapticUtil.performVirtualKeyHaptic(view)
                    onOpenSettings()
                },
                shapes = SplitButtonDefaults.trailingButtonShapesFor(height),
                colors = colors,
                contentPadding = SplitButtonDefaults.trailingButtonContentPaddingFor(height),
            ) {
                Icon(
                    painter = painterResource(R.drawable.rounded_settings_24),
                    contentDescription = stringResource(R.string.weather_open_settings),
                    tint = palette.accent,
                    modifier = Modifier.size(SplitButtonDefaults.trailingButtonIconSizeFor(height)),
                )
            }
        },
    )
}

@Composable
private fun SettingsButton(palette: WeatherPalette, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val view = LocalView.current
    Button(
        onClick = {
            HapticUtil.performVirtualKeyHaptic(view)
            onClick()
        },
        colors = ButtonDefaults.buttonColors(containerColor = palette.card, contentColor = palette.onBase),
        modifier = modifier.fillMaxWidth(),
    ) {
        Icon(painterResource(R.drawable.rounded_settings_24), contentDescription = null, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(8.dp))
        Text(stringResource(R.string.weather_open_settings))
    }
}

private fun errorLabel(context: Context, error: WeatherError?): String {
    val base = context.getString(
        when (error) {
            WeatherError.MissingApiKey -> R.string.weather_error_missing_key
            WeatherError.InvalidApiKey -> R.string.weather_error_invalid_key
            WeatherError.LocationPermission -> R.string.weather_error_location_permission
            WeatherError.NoLocation -> R.string.weather_error_no_location
            WeatherError.Network -> R.string.weather_error_network
            WeatherError.RateLimited -> R.string.weather_error_rate_limited
            is WeatherError.Unknown, null -> R.string.weather_error_unknown
        },
    )
    val detail = (error as? WeatherError.Unknown)?.message?.takeIf { it.isNotBlank() }
    return if (detail != null) "$base: $detail" else base
}

private fun compass(degrees: Double): String {
    val points = listOf("N", "NE", "E", "SE", "S", "SW", "W", "NW")
    return points[(((degrees % 360 + 360) % 360) / 45.0).roundToInt() % 8]
}

private fun dayName(millis: Long): String = SimpleDateFormat("EEE", Locale.getDefault()).format(Date(millis))

private fun formatTime(context: Context, millis: Long): String {
    val pattern = if (DateFormat.is24HourFormat(context)) "HH:mm" else "h:mm a"
    return SimpleDateFormat(pattern, Locale.getDefault()).format(Date(millis))
}

private fun formatHour(context: Context, millis: Long): String {
    val pattern = if (DateFormat.is24HourFormat(context)) "HH:mm" else "ha"
    return SimpleDateFormat(pattern, Locale.getDefault()).format(Date(millis)).lowercase(Locale.getDefault())
}

@Composable
private fun RefreshIcon(refreshing: Boolean, tint: Color) {
    if (refreshing) {
        val angle by rememberInfiniteTransition(label = "refreshSpin").animateFloat(
            initialValue = 0f,
            targetValue = 360f,
            animationSpec = infiniteRepeatable(tween(900, easing = LinearEasing)),
            label = "refreshAngle",
        )
        Icon(painterResource(R.drawable.rounded_refresh_24), null, tint = tint, modifier = Modifier.size(24.dp).graphicsLayer { rotationZ = angle })
    } else {
        Icon(painterResource(R.drawable.rounded_refresh_24), stringResource(R.string.weather_refresh), tint = tint, modifier = Modifier.size(24.dp))
    }
}

private tailrec fun Context.findWindow(): Window? = when (this) {
    is Activity -> window
    is ContextWrapper -> baseContext.findWindow()
    else -> null
}

private const val AMBIENT_RANGE_DP = 260
private const val AMBIENT_SETTLE_VELOCITY = 1200f

internal fun ambientAvailable(rootPx: Int, clockPx: Int, forecastPx: Int): Int =
    if (rootPx <= 0 || clockPx <= 0) 0 else (rootPx - 2 * maxOf(clockPx, forecastPx)).coerceAtLeast(1)

internal fun ambientForecastFits(rootPx: Int, clockPx: Int, forecastPx: Int): Boolean =
    rootPx <= 0 || ambientAvailable(rootPx, clockPx, forecastPx) >= rootPx / 3

internal fun Modifier.ambientCenter(fraction: Float, fullHeightPx: Int, availablePx: () -> Int = { 0 }): Modifier = layout { measurable, constraints ->
    val placeable = measurable.measure(constraints)
    val height = lerp(placeable.height.toFloat(), maxOf(fullHeightPx, placeable.height).toFloat(), fraction).roundToInt()
    val available = availablePx()
    val fit = if (available > 0 && placeable.height > available) available.toFloat() / placeable.height else 1f
    val scale = lerp(1f, fit, fraction)
    layout(placeable.width, height) {
        placeable.placeRelativeWithLayer(0, ((height - placeable.height) / 2f * fraction).roundToInt()) {
            scaleX = scale
            scaleY = scale
        }
    }
}

private suspend fun settleAmbient(state: MutableFloatState, target: Float, instant: Boolean) {
    if (instant) {
        state.floatValue = target
        return
    }
    animate(state.floatValue, target, animationSpec = spring(stiffness = Spring.StiffnessMediumLow)) { value, _ ->
        state.floatValue = value
    }
}

@Composable
internal fun AmbientClock(
    ambient: () -> Float,
    palette: WeatherPalette,
    modifier: Modifier,
    timeMillis: Long? = null,
    showReturn: Boolean = false,
    atNow: Boolean = true,
    onReturn: () -> Unit = {},
) {
    val context = LocalContext.current
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            now = System.currentTimeMillis()
            delay(60_000L - now % 60_000L)
        }
    }
    val is24Hour = DateFormat.is24HourFormat(context)
    val pattern = if (is24Hour) "HH:mm" else "h:mm"
    val shown = timeMillis ?: now
    val text = remember(shown, pattern) { SimpleDateFormat(pattern, Locale.getDefault()).format(Date(shown)) }
    var previous by remember { mutableLongStateOf(shown) }
    val forward = shown >= previous
    SideEffect { previous = shown }
    val clockStyle = TextStyle(fontFamily = temperatureFont(70, 200), fontSize = 40.sp)
    Column(
        modifier = modifier
            .windowInsetsPadding(WindowInsets.statusBarsIgnoringVisibility)
            .padding(top = AMBIENT_EDGE_GAP)
            .graphicsLayer {
                val reveal = ((ambient() - 0.4f) / 0.6f).coerceIn(0f, 1f)
                alpha = reveal
                translationY = (1f - reveal) * 16.dp.toPx()
            },
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (timeMillis != null) {
            val dayLabel = if (atNow) context.getString(R.string.timeline_now) else timelineDayLabel(context, shown, now)
            CounterText(dayLabel, palette.onBaseMuted, MaterialTheme.typography.titleMedium, forward = forward)
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
        if (timeMillis != null) {
            Box(Modifier.size(TIMELINE_RETURN_SIZE), contentAlignment = Alignment.Center) {
                val reveal by animateFloatAsState(if (showReturn) 1f else 0f, spring(stiffness = Spring.StiffnessMediumLow), label = "timelineReturn")
                IconButton(
                    onClick = onReturn,
                    enabled = showReturn,
                    modifier = Modifier.graphicsLayer {
                        alpha = reveal
                        scaleX = 0.6f + 0.4f * reveal
                        scaleY = 0.6f + 0.4f * reveal
                    },
                ) {
                    Icon(painterResource(R.drawable.rounded_history_24), stringResource(R.string.timeline_back_to_now), tint = palette.onBase)
                }
            }
        }
        RollingClock(text, palette.onBase, forward, clockStyle)
        if (timeMillis != null) {
            Box(Modifier.width(TIMELINE_RETURN_SIZE).align(Alignment.Bottom).padding(start = 4.dp, bottom = 8.dp)) {
                if (!is24Hour) {
                    val meridiem = remember(shown) { SimpleDateFormat("a", Locale.getDefault()).format(Date(shown)) }
                    CounterText(meridiem, palette.onBase, TextStyle(fontFamily = temperatureFont(70, 200), fontSize = 16.sp), forward = forward)
                }
            }
        }
        }
    }
}

private fun timelineDayLabel(context: Context, shown: Long, now: Long): String {
    val zone = java.time.ZoneId.systemDefault()
    val days = java.time.temporal.ChronoUnit.DAYS.between(
        java.time.Instant.ofEpochMilli(now).atZone(zone).toLocalDate(),
        java.time.Instant.ofEpochMilli(shown).atZone(zone).toLocalDate(),
    )
    return when (days) {
        0L -> context.getString(R.string.weather_detail_today)
        1L -> context.getString(R.string.timeline_tomorrow)
        else -> SimpleDateFormat("EEEE", Locale.getDefault()).format(Date(shown))
    }
}

@Composable
internal fun TimelineRain(ambient: () -> Float, chance: Int?, palette: WeatherPalette, modifier: Modifier = Modifier) {
    var last by remember { mutableIntStateOf(chance ?: 0) }
    val shown = chance ?: last
    val forward = shown >= last
    SideEffect { last = shown }
    Row(
        modifier
            .windowInsetsPadding(WindowInsets.navigationBarsIgnoringVisibility)
            .padding(bottom = AMBIENT_EDGE_GAP)
            .graphicsLayer {
                val reveal = ((ambient() - 0.4f) / 0.6f).coerceIn(0f, 1f)
                alpha = if (chance == null) 0f else reveal
                translationY = (1f - reveal) * 16.dp.toPx()
            },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(painterResource(R.drawable.rounded_rainy_24), null, tint = palette.accent, modifier = Modifier.size(28.dp))
        RollingClock("$shown%", palette.onBase, forward, MaterialTheme.typography.headlineMedium)
    }
}

@Composable
internal fun providerLabel(providerId: String): String {
    val provider = WeatherProviders.byId(providerId)
    return provider.attributionRes?.let { stringResource(it) } ?: provider.displayName
}

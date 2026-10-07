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
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.SplitButtonLayout
import androidx.compose.material3.SplitButtonDefaults
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
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
import kotlinx.coroutines.delay
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
)

@Composable
internal fun rememberWeatherPresentation(real: WeatherSnapshot?): WeatherPresentation {
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
    val snapshot = simulated?.let { WeatherSimulation.withTimeOfDay(it, timeOverride) }?.let { s -> tempOverride?.let { s.copy(tempC = it) } ?: s }
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
    val now = WeatherSimulation.timeFor(snapshot, timeOverride, clock)
    val palette = animatedPalette(
        remember(snapshot?.condition, snapshot?.isDay, snapshot?.extras?.sunriseMillis, snapshot?.extras?.sunsetMillis, now / 60_000L, timeOverride) {
            WeatherPalette.from(snapshot, now, timeOverride)
        },
    )
    val effects = remember(settingsVersion) { settings.isWeatherEffectsEnabled() && !DeviceUtils.isPowerSaveMode(context) }
    val haptics = remember(settingsVersion, effects) {
        DeviceWeatherHaptics(context).takeIf { effects && settings.isWeatherHapticsEnabled() }
    }
    val effectSpec = remember(effects, snapshot?.condition, snapshot?.isDay, snapshot?.windKph, simulation?.id) {
        snapshot?.takeIf { effects }?.let { simulation?.spec ?: WeatherEffectSpec.from(it) } ?: WeatherEffectSpec.None
    }
    return WeatherPresentation(snapshot, palette, effectSpec, units, now, haptics, iconStyle)
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun WeatherScreen(onOpenSettings: () -> Unit = {}) {
    val context = LocalContext.current
    val view = LocalView.current
    val scope = rememberCoroutineScope()
    val settings = remember { SettingsRepository(context) }
    val state by WeatherRepository.state.collectAsState()
    val presentation = rememberWeatherPresentation(state.snapshot)
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
        val glassRain = effectSpec.layers.filterIsInstance<WeatherEffectLayer.Rain>().maxByOrNull { it.intensity }.takeUnless { sheetOpen }
        val density = LocalDensity.current
        val collapse = remember { mutableFloatStateOf(0f) }
        val scrollTick = remember { mutableIntStateOf(0) }
        val ambient = remember { mutableFloatStateOf(0f) }
        val immersive = ambient.floatValue > 0.5f
        val rootHeightPx = remember { mutableIntStateOf(0) }
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
        val sky = snapshot?.let { skyState(it, now) }
        val skyHeightPx = with(density) { SKY_HEIGHT.toPx() }
        val collapseShiftPx = with(density) { 60.dp.toPx() }
        val fallbackYPx = with(density) { 150.dp.toPx() }
        Box(Modifier.fillMaxSize().onSizeChanged { rootHeightPx.intValue = it.height }.rainOnGlass((glassRain?.intensity ?: 0f) * effectRamp, glassRain?.slant ?: 0f) {
            if (sky == null) {
                Offset(0.5f, fallbackYPx - collapse.floatValue * collapseShiftPx)
            } else {
                val t = sky.first
                Offset(
                    0.9f - 0.8f * t,
                    skyHeightPx * (0.66f - 0.4f * kotlin.math.sin(Math.PI.toFloat() * t)) - collapse.floatValue * collapseShiftPx,
                )
            }
        }) {
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
                val topFadePx = if (headerBottom.floatValue > 0f) {
                    val expandedFade = with(density) { 36.dp.toPx() }
                    val collapsedFade = headerBottom.floatValue + with(density) { 40.dp.toPx() }
                    expandedFade + (collapsedFade - expandedFade) * collapse.floatValue
                } else {
                    0f
                }
                snapshot?.let { SkyBody(it, now, { collapse.floatValue }) }
                if (!effectSpec.isEmpty && effectRamp > 0f) {
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
                            if (ambient.floatValue > 0.3f) return@WeatherEffects emptyList()
                            val top = sheetTop.floatValue
                            val cards = rainSurfaces.entries.mapNotNull { (key, source) -> source.resolve(key) }
                                .filter { it.rect.top >= headerBottom.floatValue }
                            if (top.isNaN()) {
                                cards
                            } else {
                                cards.filter { it.rect.bottom <= top } +
                                    RainSurface("locations-sheet", Rect(0f, top, screenWidthPx, screenHeightPx), sheetCornerPx, 0f, screenWidthPx)
                            }
                        },
                        cover = { if (ambient.floatValue > 0.3f) emptyList() else rainSurfaces.entries.mapNotNull { (key, source) -> source.resolve(key) } },
                        scrollTick = { scrollTick.intValue },
                    )
                }
                val scrollState = rememberScrollState()
                val maxCollapsePx = with(density) { COLLAPSE_RANGE.toPx() }
                val ambientRangePx = with(density) { AMBIENT_RANGE_DP.dp.toPx() }
                val connection = remember(maxCollapsePx, ambientRangePx, compatibility) {
                    object : NestedScrollConnection {
                        override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                            if (available.y != 0f) scrollTick.intValue++
                            if (ambient.floatValue > 0f && source == NestedScrollSource.UserInput) {
                                val next = (ambient.floatValue + available.y / ambientRangePx).coerceIn(0f, 1f)
                                val used = (next - ambient.floatValue) * ambientRangePx
                                ambient.floatValue = next
                                return Offset(0f, used)
                            }
                            if (available.y >= 0f || collapse.floatValue >= 1f) return Offset.Zero
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
                    Column(Modifier.fillMaxSize()) {
                        Spacer(Modifier.height(120.dp))
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                            if (state.loading) LoadingIndicator() else Text(errorLabel(context, state.error), color = palette.onBaseMuted)
                        }
                        if (state.error == WeatherError.LocationPermission) {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                                FilledTonalButton(onClick = { requestLocation() }) {
                                    Text(stringResource(R.string.weather_grant_location))
                                }
                            }
                        }
                        if (!state.loading) {
                            Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.Center) {
                                FilledTonalButton(
                                    onClick = {
                                        HapticUtil.performVirtualKeyHaptic(view)
                                        showLocations = true
                                    },
                                ) {
                                    Text(stringResource(R.string.weather_choose_location))
                                }
                            }
                        }
                        Spacer(Modifier.height(24.dp))
                        SettingsButton(palette, onOpenSettings, Modifier.padding(horizontal = SIDE_PADDING).rainSurface("settings-button", corner = 20.dp))
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
                                    snapshot.daily.orEmpty().takeIf { it.isNotEmpty() }
                                        ?.let { DailySection(it, units.temperature, palette, Modifier.padding(horizontal = SIDE_PADDING)) }
                                    DetailsSection(snapshot, units, palette, Modifier.padding(horizontal = SIDE_PADDING))
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
                            }
                            Column(
                                Modifier
                                    .align(Alignment.TopCenter)
                                    .fillMaxWidth()
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
                                        .ambientCenter(ambient.floatValue, (rootHeightPx.intValue - ambientInset * 2).coerceAtLeast(0)),
                                )
                            }
                            if (ambient.floatValue > 0f) {
                                AmbientClock({ ambient.floatValue }, palette, Modifier.align(Alignment.BottomCenter))
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

private val SKY_HEIGHT = 360.dp

@Composable
private fun SkyBody(snapshot: WeatherSnapshot, now: Long, collapse: () -> Float) {
    val (t, isSun) = skyState(snapshot, now) ?: return
    val hide = (1f - collapse() * 1.6f).coerceIn(0f, 1f)
    if (hide <= 0f) return
    val horizonFade = (minOf(t, 1f - t) / 0.1f).coerceIn(0f, 1f)
    val color = if (isSun) Color(0xFFFFE2A8) else Color(0xFFE6ECFF)
    Canvas(
        Modifier
            .fillMaxWidth()
            .height(SKY_HEIGHT)
            .graphicsLayer { translationY = -collapse() * 60.dp.toPx() },
    ) {
        val x = size.width * (0.9f - 0.8f * t)
        val y = size.height * 0.66f - size.height * 0.4f * kotlin.math.sin(Math.PI.toFloat() * t)
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
        drawCircle(color.copy(alpha = alpha * 0.7f), radius = (if (isSun) 20.dp else 16.dp).toPx(), center = Offset(x, y))
    }
}

private val LocalRainSurfaces = androidx.compose.runtime.staticCompositionLocalOf<SnapshotStateMap<String, RainSurfaceSource>?> { null }

private class RainSurfaceSource(val corner: Float, val mask: (() -> Rect)?) {
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
private fun Modifier.rainSurface(key: String, corner: Dp = 28.dp, mask: (() -> Rect)? = null): Modifier {
    val registry = LocalRainSurfaces.current ?: return this
    val cornerPx = with(LocalDensity.current) { corner.toPx() }
    val source = remember(key, cornerPx) { RainSurfaceSource(cornerPx, mask) }
    DisposableEffect(key, source) {
        registry[key] = source
        onDispose { registry.remove(key) }
    }
    return this.onGloballyPositioned { source.coordinates = it }
}

private val SIDE_PADDING = 20.dp
private const val CONTENT_FADE_MS = 500
private const val EFFECTS_DELAY_MS = 450L
private const val EFFECTS_FADE_MS = 1800
private const val REFRESH_DELAY_MS = 600L
private val COLLAPSE_RANGE = 520.dp
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
            layout(placeable.width, (placeable.height * (1f - fraction)).roundToInt()) { placeable.placeRelative(0, 0) }
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
private fun Header(snapshot: WeatherSnapshot, unit: TemperatureUnit, palette: WeatherPalette, progress: Float, ambient: Float, modifier: Modifier) {
    val number = WeatherFormat.temperature(snapshot.tempC, unit).removeSuffix("°")
    
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
                        Text(
                            number,
                            color = palette.onBase,
                            fontFamily = font,
                            fontSize = size,
                            lineHeight = size * 0.86f,
                            letterSpacing = letterSpacing.sp,
                            maxLines = 1,
                            softWrap = false,
                        )
                        DegreeSign(size, font, palette.onBase)
                    }
                },
                condition = {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        WeatherIcon(snapshot.condition, snapshot.isDay, palette.accent, Modifier.size(32.dp))
                        Text(snapshot.conditionText, color = palette.onBase, style = MaterialTheme.typography.headlineSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                },
            )
            Column(Modifier.foldAway(maxOf((progress / 0.5f).coerceIn(0f, 1f), ambient))) {
                Spacer(Modifier.height(6.dp))
                Text(
                    listOf(
                        stringResource(R.string.weather_high_low, WeatherFormat.temperature(snapshot.highC, unit), WeatherFormat.temperature(snapshot.lowC, unit)),
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
private fun HourlySection(snapshot: WeatherSnapshot, unit: TemperatureUnit, palette: WeatherPalette) {
    if (snapshot.hourly.isEmpty()) return
    val context = LocalContext.current
    val hours = snapshot.hourly
    val carouselState = rememberCarouselState { hours.size }
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
                    .rainSurface("hourly:$index", mask = { carouselItemDrawInfo.maskRect })
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
        add(Detail(WeatherDetailSlot.HUMIDITY, R.string.weather_detail_humidity, "${snapshot.humidity}%"))
        add(
            Detail(
                WeatherDetailSlot.WIND,
                R.string.weather_detail_wind,
                WeatherFormat.wind(snapshot.windKph, units.wind) + (extras?.windDirectionDeg?.let { " ${compass(it)}" } ?: ""),
            ),
        )
        extras?.windGustKph?.let { add(Detail(WeatherDetailSlot.GUSTS, R.string.weather_detail_gusts, WeatherFormat.wind(it, units.wind))) }
        add(Detail(WeatherDetailSlot.RAIN_CHANCE, R.string.weather_detail_rain_chance, "${snapshot.chanceOfRain}%"))
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
    val rise = snapshot.extras?.sunriseMillis ?: return
    val set = snapshot.extras?.sunsetMillis ?: return
    val context = LocalContext.current
    Column(modifier) {
        RoundedCardContainer(modifier = Modifier.rainSurface("sun"), spacing = 2.dp, cornerRadius = 28.dp) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                SunTile(R.string.weather_detail_sunrise, formatTime(context, rise), palette, Modifier.weight(1f))
                SunTile(R.string.weather_detail_sunset, formatTime(context, set), palette, Modifier.weight(1f))
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
                            text = error?.let { errorLabel(context, it) } ?: WeatherProviders.byId(snapshot.providerId).displayName,
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

private fun Modifier.ambientCenter(fraction: Float, fullHeightPx: Int): Modifier = layout { measurable, constraints ->
    val placeable = measurable.measure(constraints)
    val height = lerp(placeable.height.toFloat(), maxOf(fullHeightPx, placeable.height).toFloat(), fraction).roundToInt()
    layout(placeable.width, height) {
        placeable.placeRelative(0, ((height - placeable.height) / 2f * fraction).roundToInt())
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
private fun AmbientClock(ambient: () -> Float, palette: WeatherPalette, modifier: Modifier) {
    val context = LocalContext.current
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            now = System.currentTimeMillis()
            delay(60_000L - now % 60_000L)
        }
    }
    val pattern = if (DateFormat.is24HourFormat(context)) "HH:mm" else "hh:mm"
    val text = remember(now, pattern) { SimpleDateFormat(pattern, Locale.getDefault()).format(Date(now)) }
    Text(
        text = text,
        color = palette.onBase,
        fontFamily = temperatureFont(70, 200),
        fontSize = 56.sp,
        maxLines = 1,
        modifier = modifier
            .navigationBarsPadding()
            .padding(bottom = 40.dp)
            .graphicsLayer {
                val reveal = ((ambient() - 0.4f) / 0.6f).coerceIn(0f, 1f)
                alpha = reveal
                translationY = (1f - reveal) * 16.dp.toPx()
            },
    )
}

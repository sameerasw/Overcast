package com.sameerasw.overcast.ui.features.onboarding

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import kotlin.math.PI
import kotlin.math.atan2
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import com.sameerasw.overcast.R
import com.sameerasw.overcast.data.repository.SettingsRepository
import com.sameerasw.overcast.ui.core.containers.RoundedCardContainer
import com.sameerasw.overcast.ui.features.weather.LocationsBottomSheet
import com.sameerasw.overcast.ui.theme.GoogleSansFlexRounded
import com.sameerasw.overcast.utils.HapticUtil
import com.sameerasw.overcast.weather.WeatherRepository
import com.sameerasw.overcast.weather.location.DeviceLocationSource
import kotlinx.coroutines.launch

private enum class OnboardingStep { WELCOME, LOCATION }

private fun hasBackgroundLocation(context: android.content.Context): Boolean =
    Build.VERSION.SDK_INT < Build.VERSION_CODES.Q ||
        context.checkSelfPermission(Manifest.permission.ACCESS_BACKGROUND_LOCATION) == PackageManager.PERMISSION_GRANTED

@Composable
fun OnboardingScreen(onFinished: () -> Unit) {
    val context = LocalContext.current
    val view = LocalView.current
    val scope = rememberCoroutineScope()
    val settings = remember { SettingsRepository(context) }

    var step by remember { mutableStateOf(OnboardingStep.WELCOME) }
    var hasLocation by remember { mutableStateOf(DeviceLocationSource.hasPermission(context)) }
    var hasBackground by remember { mutableStateOf(hasBackgroundLocation(context)) }
    var pickedPlace by remember { mutableStateOf<String?>(null) }
    var showSheet by remember { mutableStateOf(false) }

    val backgroundLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        hasBackground = hasBackgroundLocation(context)
    }
    val locationLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        hasLocation = granted
        if (granted) {
            settings.setWeatherLocationMode("device")
            pickedPlace = null
            scope.launch {
                WeatherRepository.clear(context)
                WeatherRepository.refresh(context, force = true)
            }
        }
    }

    BackHandler(enabled = step == OnboardingStep.LOCATION) { step = OnboardingStep.WELCOME }

    if (showSheet) {
        LocationsBottomSheet(
            onDismissRequest = { showSheet = false },
            onSelectCurrent = {
                if (hasLocation) {
                    settings.setWeatherLocationMode("device")
                    pickedPlace = null
                } else {
                    locationLauncher.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
                }
            },
            onSelectPlace = { place ->
                settings.setWeatherManualLocation(place.latitude, place.longitude, place.name)
                settings.setWeatherLocationMode("manual")
                pickedPlace = place.name
                scope.launch {
                    WeatherRepository.clear(context)
                    WeatherRepository.refresh(context, force = true)
                }
            },
            onTopChanged = {},
        )
    }

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.surfaceContainer) {
        AnimatedContent(
            targetState = step,
            transitionSpec = {
                if (targetState.ordinal > initialState.ordinal) {
                    (slideInHorizontally { it } + fadeIn(tween(400))).togetherWith(slideOutHorizontally { -it } + fadeOut(tween(400)))
                } else {
                    (slideInHorizontally { -it } + fadeIn(tween(400))).togetherWith(slideOutHorizontally { it } + fadeOut(tween(400)))
                }
            },
            label = "OnboardingTransition",
        ) { current ->
            when (current) {
                OnboardingStep.WELCOME -> WelcomeStep(
                    onNext = {
                        HapticUtil.performVirtualKeyHaptic(view)
                        step = OnboardingStep.LOCATION
                    },
                )

                OnboardingStep.LOCATION -> LocationStep(
                    hasLocation = hasLocation,
                    hasBackground = hasBackground,
                    pickedPlace = pickedPlace,
                    onAllowLocation = { locationLauncher.launch(Manifest.permission.ACCESS_COARSE_LOCATION) },
                    onAllowBackground = {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) backgroundLauncher.launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
                    },
                    onSearch = { showSheet = true },
                    onBack = {
                        HapticUtil.performVirtualKeyHaptic(view)
                        step = OnboardingStep.WELCOME
                    },
                    onFinish = {
                        HapticUtil.performConfirmHaptic(view)
                        if (pickedPlace == null && hasLocation) settings.setWeatherLocationMode("device")
                        onFinished()
                    },
                )
            }
        }
    }
}

@Composable
private fun WelcomeStep(onNext: () -> Unit) {
    val context = LocalContext.current
    val view = LocalView.current
    val scope = rememberCoroutineScope()
    val rotationAnimatable = remember { Animatable(0f) }
    var center by remember { mutableStateOf(Offset.Zero) }
    Column(Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Spacer(Modifier.statusBarsPadding())
            Spacer(Modifier.weight(1f))
            Image(
                painter = painterResource(R.drawable.ic_launcher_foreground),
                contentDescription = null,
                modifier = Modifier
                    .size(240.dp)
                    .onSizeChanged { center = Offset(it.width / 2f, it.height / 2f) }
                    .pointerInput(Unit) {
                        val majorStep = 60f
                        val minorStep = 2f
                        var rotation = 0f
                        var lastMajor = 0
                        var lastMinor = 0
                        detectDragGestures(
                            onDragStart = {
                                scope.launch { rotationAnimatable.stop() }
                                rotation = rotationAnimatable.value
                                lastMajor = kotlin.math.round(rotation / majorStep).toInt()
                                lastMinor = kotlin.math.round(rotation / minorStep).toInt()
                            },
                            onDrag = { change, _ ->
                                val oldAngle = atan2(change.previousPosition.y - center.y, change.previousPosition.x - center.x)
                                val newAngle = atan2(change.position.y - center.y, change.position.x - center.x)
                                var delta = (newAngle - oldAngle) * 180 / PI
                                if (delta > 180) delta -= 360
                                if (delta < -180) delta += 360
                                rotation += delta.toFloat()
                                val minor = kotlin.math.round(rotation / minorStep).toInt()
                                if (minor != lastMinor) {
                                    HapticUtil.performMicroHaptic(view)
                                    lastMinor = minor
                                }
                                lastMajor = kotlin.math.round(rotation / majorStep).toInt()
                                scope.launch { rotationAnimatable.snapTo(rotation) }
                            },
                            onDragEnd = {
                                scope.launch {
                                    rotationAnimatable.animateTo(
                                        targetValue = 0f,
                                        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
                                    ) {
                                        val major = kotlin.math.round(value / majorStep).toInt()
                                        if (major != lastMajor) {
                                            HapticUtil.performMediumHaptic(view)
                                            lastMajor = major
                                        }
                                    }
                                    rotation = 0f
                                    lastMajor = 0
                                    lastMinor = 0
                                }
                            },
                        )
                    }
                    .graphicsLayer { rotationZ = rotationAnimatable.value },
            )
            Spacer(Modifier.height(18.dp))
            Text(
                text = stringResource(R.string.welcome_title),
                style = MaterialTheme.typography.headlineMedium.copy(fontFamily = GoogleSansFlexRounded, fontWeight = FontWeight.SemiBold),
                textAlign = TextAlign.Center,
            )
            Text(
                text = stringResource(R.string.app_description),
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.weight(1f))
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(100.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer)
                    .clickable {
                        context.startActivity(Intent(Intent.ACTION_VIEW, "https://sameerasw.com".toUri()).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                    }
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Image(
                    painter = painterResource(R.drawable.avatar),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.size(32.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceContainerHigh),
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.welcome_developer_attribution),
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.padding(end = 4.dp),
                )
            }
            Spacer(Modifier.height(16.dp))
        }
        Button(
            onClick = onNext,
            modifier = Modifier.fillMaxWidth().navigationBarsPadding().padding(16.dp).height(56.dp),
        ) {
            Text(
                text = stringResource(R.string.action_lets_begin),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.weight(1f))
            Icon(painterResource(R.drawable.rounded_arrow_forward_24), contentDescription = null, modifier = Modifier.size(24.dp))
        }
    }
}

@Composable
private fun LocationStep(
    hasLocation: Boolean,
    hasBackground: Boolean,
    pickedPlace: String?,
    onAllowLocation: () -> Unit,
    onAllowBackground: () -> Unit,
    onSearch: () -> Unit,
    onBack: () -> Unit,
    onFinish: () -> Unit,
) {
    val view = LocalView.current
    val ready = hasLocation || pickedPlace != null
    Column(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        Column(
            modifier = Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.statusBarsPadding())
            Spacer(Modifier.height(32.dp))
            Text(
                text = stringResource(R.string.onboarding_location_title),
                style = MaterialTheme.typography.headlineLarge.copy(fontFamily = GoogleSansFlexRounded, fontWeight = FontWeight.Bold),
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.onboarding_location_subtitle),
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(24.dp))
            RoundedCardContainer(modifier = Modifier.fillMaxWidth(), spacing = 2.dp, cornerRadius = 24.dp) {
                PermissionCard(
                    iconRes = R.drawable.rounded_my_location_24,
                    title = stringResource(R.string.onboarding_perm_location_title),
                    usage = stringResource(R.string.onboarding_perm_location_usage),
                    done = hasLocation,
                    actionLabel = stringResource(R.string.onboarding_allow),
                    onAction = {
                        HapticUtil.performVirtualKeyHaptic(view)
                        onAllowLocation()
                    },
                )
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    PermissionCard(
                        iconRes = R.drawable.rounded_refresh_24,
                        title = stringResource(R.string.onboarding_perm_background_title),
                        usage = stringResource(R.string.onboarding_perm_background_usage),
                        done = hasBackground,
                        enabled = hasLocation,
                        actionLabel = stringResource(R.string.onboarding_allow),
                        onAction = {
                            HapticUtil.performVirtualKeyHaptic(view)
                            onAllowBackground()
                        },
                    )
                }
                PermissionCard(
                    iconRes = R.drawable.rounded_search_24,
                    title = stringResource(R.string.onboarding_search_title),
                    usage = pickedPlace ?: stringResource(R.string.onboarding_search_usage),
                    done = pickedPlace != null,
                    actionLabel = stringResource(R.string.onboarding_search_action),
                    onAction = {
                        HapticUtil.performVirtualKeyHaptic(view)
                        onSearch()
                    },
                )
            }
            Spacer(Modifier.height(16.dp))
        }
        Row(
            modifier = Modifier.fillMaxWidth().navigationBarsPadding().padding(vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            OutlinedButton(
                onClick = onBack,
                modifier = Modifier.size(56.dp),
                shape = RoundedCornerShape(16.dp),
                contentPadding = PaddingValues(0.dp),
            ) {
                Icon(painterResource(R.drawable.rounded_arrow_back_24), contentDescription = stringResource(R.string.action_back), modifier = Modifier.size(24.dp))
            }
            Button(onClick = onFinish, enabled = ready, modifier = Modifier.weight(1f).height(56.dp)) {
                Text(
                    text = stringResource(R.string.onboarding_finish),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(Modifier.weight(1f))
                Icon(painterResource(R.drawable.rounded_check_24), contentDescription = null, modifier = Modifier.size(24.dp))
            }
        }
    }
}

@Composable
private fun PermissionCard(
    iconRes: Int,
    title: String,
    usage: String,
    done: Boolean,
    actionLabel: String,
    onAction: () -> Unit,
    enabled: Boolean = true,
) {
    Surface(color = MaterialTheme.colorScheme.surfaceBright, shape = MaterialTheme.shapes.extraSmall, modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Box(
                modifier = Modifier.size(44.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center,
            ) {
                Icon(painterResource(iconRes), contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.size(24.dp))
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                Text(usage, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (done) {
                Icon(painterResource(R.drawable.rounded_check_24), contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
            } else {
                FilledTonalButton(onClick = onAction, enabled = enabled) { Text(actionLabel) }
            }
        }
    }
}

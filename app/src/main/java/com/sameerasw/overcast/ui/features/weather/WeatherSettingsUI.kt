/*
 * Copyright (c) 2026 sameerasw.com
 * License: MIT License
 *
 * Feature Module: UI Feature - Weather
 * File: WeatherSettingsUI.kt
 * Description: Weather source, display and debug settings.
 */

package com.sameerasw.overcast.ui.features.weather

import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.sameerasw.overcast.R
import com.sameerasw.overcast.data.repository.SettingsRepository
import com.sameerasw.overcast.ui.components.menus.SegmentedDropdownMenuItem
import com.sameerasw.overcast.ui.core.cards.ConfigPickerItem
import com.sameerasw.overcast.ui.core.cards.IconToggleItem
import com.sameerasw.overcast.ui.core.containers.RoundedCardContainer
import com.sameerasw.overcast.ui.core.sheets.OvercastBottomSheet
import com.sameerasw.overcast.utils.HapticUtil
import com.sameerasw.overcast.weather.DistanceUnit
import com.sameerasw.overcast.weather.PrecipitationUnit
import com.sameerasw.overcast.weather.PressureUnit
import com.sameerasw.overcast.weather.TemperatureUnit
import com.sameerasw.overcast.weather.WeatherIconStyle
import com.sameerasw.overcast.weather.WeatherRepository
import com.sameerasw.overcast.weather.WeatherUnits
import com.sameerasw.overcast.weather.WindSpeedUnit
import com.sameerasw.overcast.weather.model.WeatherError
import com.sameerasw.overcast.weather.provider.OpenMeteoModels
import com.sameerasw.overcast.weather.provider.WeatherProviders
import kotlinx.coroutines.launch


private val REFRESH_OPTIONS = listOf(30, 60, 120, 180, 360)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun WeatherSettingsUI(
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val view = LocalView.current
    val scope = rememberCoroutineScope()
    val settings = remember { SettingsRepository(context) }
    var providerId by remember { mutableStateOf(WeatherProviders.resolve(settings.getWeatherProvider(), settings.getWeatherApiKey("weatherapi")).id) }
    val provider = WeatherProviders.byId(providerId)
    val weatherState by WeatherRepository.state.collectAsState()

    var compatibility by remember { mutableStateOf(settings.isCompatibilityMode()) }
    var ambientForecast by remember { mutableStateOf(settings.isAmbientForecastEnabled()) }
    var unitsExpanded by remember { mutableStateOf(false) }
    var showCompatibilityInfo by remember { mutableStateOf(false) }
    var effects by remember { mutableStateOf(settings.isWeatherEffectsEnabled()) }
    var weatherHaptics by remember { mutableStateOf(settings.isWeatherHapticsEnabled()) }
    var refreshMinutes by remember { mutableIntStateOf(settings.getWeatherRefreshMinutes()) }
    var savedKey by remember { mutableStateOf(settings.getWeatherApiKey(providerId).orEmpty()) }
    var keyInput by remember { mutableStateOf(savedKey) }
    var keyVisible by remember { mutableStateOf(false) }

    fun refreshNow() {
        scope.launch { WeatherRepository.refresh(context, force = true) }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
            SectionTitle(R.string.weather_section_source)
            RoundedCardContainer(spacing = 2.dp, cornerRadius = 24.dp) {
                ConfigPickerItem(
                    title = stringResource(R.string.weather_provider_title),
                    selectedValue = provider.displayName,
                    iconRes = R.drawable.rounded_cloud_24,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    WeatherProviders.all.forEach { option ->
                        SegmentedDropdownMenuItem(
                            text = { Text(option.displayName) },
                            onClick = {
                                if (option.id != providerId) {
                                    settings.setWeatherProvider(option.id)
                                    providerId = option.id
                                    savedKey = settings.getWeatherApiKey(option.id).orEmpty()
                                    keyInput = savedKey
                                    scope.launch {
                                        WeatherRepository.clear(context)
                                        WeatherRepository.refresh(context, force = true)
                                    }
                                }
                            },
                        )
                    }
                }
                if (provider.id == "openmeteo") {
                    var modelId by remember { mutableStateOf(settings.getWeatherOpenMeteoModel()) }
                    ConfigPickerItem(
                        title = stringResource(R.string.weather_model_title),
                        selectedValue = OpenMeteoModels.label(modelId),
                        iconRes = R.drawable.rounded_model_training_24,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        OpenMeteoModels.all.forEach { option ->
                            SegmentedDropdownMenuItem(
                                text = { Text(option.label) },
                                onClick = {
                                    if (option.id != modelId) {
                                        settings.setWeatherOpenMeteoModel(option.id)
                                        modelId = option.id
                                        scope.launch {
                                            WeatherRepository.clear(context)
                                            WeatherRepository.refresh(context, force = true)
                                        }
                                    }
                                },
                            )
                        }
                    }
                }
                if (provider.requiresApiKey) {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceBright,
                        shape = MaterialTheme.shapes.extraSmall,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = keyInput,
                                onValueChange = { keyInput = it.trim() },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                label = { Text(stringResource(R.string.weather_api_key_title)) },
                                leadingIcon = { Icon(painterResource(R.drawable.rounded_key_24), contentDescription = null) },
                                trailingIcon = {
                                    Row {
                                        IconButton(onClick = {
                                            HapticUtil.performVirtualKeyHaptic(view)
                                            clipboardText(context)?.let { keyInput = it.trim() }
                                        }) {
                                            Icon(painterResource(R.drawable.rounded_content_paste_24), contentDescription = stringResource(R.string.shorten_paste_button))
                                        }
                                        IconButton(onClick = {
                                            HapticUtil.performVirtualKeyHaptic(view)
                                            keyVisible = !keyVisible
                                        }) {
                                            Icon(
                                                painterResource(if (keyVisible) R.drawable.rounded_visibility_off_24 else R.drawable.rounded_visibility_24),
                                                contentDescription = null,
                                            )
                                        }
                                    }
                                },
                                visualTransformation = if (keyVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Password,
                                    autoCorrectEnabled = false,
                                    imeAction = ImeAction.Done,
                                ),
                                shape = RoundedCornerShape(16.dp),
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                provider.signupUrl?.let { url ->
                                    TextButton(onClick = {
                                        HapticUtil.performVirtualKeyHaptic(view)
                                        openUrl(context, url)
                                    }) {
                                        Text(stringResource(R.string.weather_get_api_key))
                                    }
                                }
                                Spacer(Modifier.weight(1f))
                                Button(
                                    onClick = {
                                        HapticUtil.performVirtualKeyHaptic(view)
                                        settings.setWeatherApiKey(providerId, keyInput)
                                        savedKey = keyInput
                                        refreshNow()
                                    },
                                    enabled = keyInput != savedKey,
                                ) {
                                    Text(stringResource(R.string.action_save))
                                }
                            }
                        }
                    }
                }
                ConfigPickerItem(
                    title = stringResource(R.string.weather_refresh_interval_title),
                    selectedValue = intervalLabel(context, refreshMinutes),
                    iconRes = R.drawable.rounded_schedule_24,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    REFRESH_OPTIONS.forEach { minutes ->
                        SegmentedDropdownMenuItem(
                            text = { Text(intervalLabel(context, minutes)) },
                            onClick = {
                                refreshMinutes = minutes
                                settings.setWeatherRefreshMinutes(minutes)
                            },
                        )
                    }
                }
                IconToggleItem(
                    iconRes = R.drawable.rounded_straighten_24,
                    title = stringResource(R.string.weather_section_units),
                    showToggle = false,
                    onClick = { unitsExpanded = !unitsExpanded },
                    trailingContent = {
                        Icon(
                            painter = painterResource(if (unitsExpanded) R.drawable.rounded_keyboard_arrow_up_24 else R.drawable.rounded_keyboard_arrow_down_24),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    },
                )
                AnimatedVisibility(visible = unitsExpanded) {
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        UnitPickerItem(
                            title = R.string.weather_units_temperature,
                            options = TemperatureUnit.entries,
                            iconRes = R.drawable.rounded_thermostat_24,
                            id = { it.id },
                            symbol = { it.symbol },
                            system = WeatherUnits.systemTemperature(),
                            stored = settings.getWeatherUnits(),
                            onSelected = settings::setWeatherUnits,
                        )
                        UnitPickerItem(
                            title = R.string.weather_units_wind,
                            options = WindSpeedUnit.entries,
                            iconRes = R.drawable.rounded_air_24,
                            id = { it.id },
                            symbol = { it.symbol },
                            system = WeatherUnits.systemWind(),
                            stored = settings.getWindUnit(),
                            onSelected = settings::setWindUnit,
                        )
                        UnitPickerItem(
                            title = R.string.weather_units_pressure,
                            options = PressureUnit.entries,
                            iconRes = R.drawable.rounded_compress_24,
                            id = { it.id },
                            symbol = { it.symbol },
                            system = WeatherUnits.systemPressure(),
                            stored = settings.getPressureUnit(),
                            onSelected = settings::setPressureUnit,
                        )
                        UnitPickerItem(
                            title = R.string.weather_units_distance,
                            options = DistanceUnit.entries,
                            iconRes = R.drawable.rounded_visibility_24,
                            id = { it.id },
                            symbol = { it.symbol },
                            system = WeatherUnits.systemDistance(),
                            stored = settings.getDistanceUnit(),
                            onSelected = settings::setDistanceUnit,
                        )
                        UnitPickerItem(
                            title = R.string.weather_units_precipitation,
                            options = PrecipitationUnit.entries,
                            iconRes = R.drawable.rounded_water_drop_24,
                            id = { it.id },
                            symbol = { it.symbol },
                            system = WeatherUnits.systemPrecipitation(),
                            stored = settings.getPrecipitationUnit(),
                            onSelected = settings::setPrecipitationUnit,
                        )
                    }
                }
                val sourceError = weatherState.error?.takeIf { it !is WeatherError.LocationPermission && it !is WeatherError.NoLocation }
                AnimatedVisibility(visible = sourceError != null) {
                    val shownError = remember { mutableStateOf(sourceError) }
                    if (sourceError != null) shownError.value = sourceError
                    Surface(
                        color = MaterialTheme.colorScheme.errorContainer,
                        shape = MaterialTheme.shapes.extraSmall,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Icon(
                                painterResource(R.drawable.rounded_warning_24),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onErrorContainer,
                            )
                            Text(
                                text = shownError.value?.let { errorText(context, it) }.orEmpty(),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                            )
                        }
                    }
                }
            }

            SectionTitle(R.string.weather_section_display)
            RoundedCardContainer(spacing = 2.dp, cornerRadius = 24.dp) {
                var iconStyle by remember { mutableStateOf(WeatherIconStyle.fromId(settings.getWeatherIconStyle())) }
                Surface(color = MaterialTheme.colorScheme.surfaceBright, shape = MaterialTheme.shapes.extraSmall, modifier = Modifier.fillMaxWidth()) {
                    WeatherIconStyleCarousel(
                        selected = iconStyle,
                        onSelected = {
                            iconStyle = it
                            settings.setWeatherIconStyle(it.id)
                        },
                    )
                }
                IconToggleItem(
                    iconRes = R.drawable.rounded_rainy_24,
                    title = stringResource(R.string.weather_effects_title),
                    isChecked = effects,
                    onCheckedChange = {
                        HapticUtil.performVirtualKeyHaptic(view)
                        effects = it
                        settings.setWeatherEffectsEnabled(it)
                    },
                )
                IconToggleItem(
                    iconRes = R.drawable.rounded_vibration_24,
                    title = stringResource(R.string.weather_haptics_title),
                    isChecked = weatherHaptics,
                    enabled = effects,
                    onCheckedChange = {
                        HapticUtil.performVirtualKeyHaptic(view)
                        weatherHaptics = it
                        settings.setWeatherHapticsEnabled(it)
                    },
                )
                IconToggleItem(
                    iconRes = R.drawable.rounded_speed_24,
                    title = stringResource(R.string.compatibility_mode_title),
                    onInfoClick = { showCompatibilityInfo = true },
                    isChecked = compatibility,
                    onCheckedChange = {
                        HapticUtil.performVirtualKeyHaptic(view)
                        compatibility = it
                        settings.setCompatibilityMode(it)
                    },
                )
            }

            SectionTitle(R.string.weather_section_screensaver)
            RoundedCardContainer(spacing = 2.dp, cornerRadius = 24.dp) {
                IconToggleItem(
                    iconRes = R.drawable.rounded_dashboard_24,
                    title = stringResource(R.string.screensaver_show_forecast),
                    isChecked = ambientForecast,
                    onCheckedChange = {
                        HapticUtil.performVirtualKeyHaptic(view)
                        ambientForecast = it
                        settings.setAmbientForecastEnabled(it)
                    },
                )
            }
    }

    if (showCompatibilityInfo) {
        CompatibilityInfoSheet(onDismiss = { showCompatibilityInfo = false })
    }
}

@Composable
private fun CompatibilityInfoSheet(onDismiss: () -> Unit) {
    OvercastBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = stringResource(R.string.compatibility_mode_title),
                style = MaterialTheme.typography.headlineSmall,
            )
            Text(
                text = stringResource(R.string.compatibility_info),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.size(8.dp))
        }
    }
}

@Composable
private fun SectionTitle(res: Int) {
    Text(
        text = stringResource(res),
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(start = 8.dp),
    )
}

private fun intervalLabel(context: Context, minutes: Int): String =
    if (minutes < 60) {
        context.getString(R.string.weather_interval_minutes, minutes)
    } else {
        context.getString(R.string.weather_interval_hours, minutes / 60)
    }

private fun errorText(context: Context, error: WeatherError): String {
    val base = context.getString(
        when (error) {
            WeatherError.MissingApiKey -> R.string.weather_error_missing_key
            WeatherError.InvalidApiKey -> R.string.weather_error_invalid_key
            WeatherError.LocationPermission -> R.string.weather_error_location_permission
            WeatherError.NoLocation -> R.string.weather_error_no_location
            WeatherError.Network -> R.string.weather_error_network
            is WeatherError.Unknown -> R.string.weather_error_unknown
        },
    )
    val detail = (error as? WeatherError.Unknown)?.message?.takeIf { it.isNotBlank() }
    return if (detail != null) "$base: $detail" else base
}

private fun clipboardText(context: Context): String? {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager ?: return null
    return clipboard.primaryClip?.takeIf { it.itemCount > 0 }?.getItemAt(0)?.coerceToText(context)?.toString()
}

private fun openUrl(context: Context, url: String) {
    try {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    } catch (_: Exception) {
    }
}

@Composable
private fun <T> UnitPickerItem(
    title: Int,
    options: List<T>,
    iconRes: Int,
    id: (T) -> String,
    symbol: (T) -> String,
    system: T,
    stored: String,
    onSelected: (String) -> Unit,
) {
    var selected by remember { mutableStateOf(stored) }
    val systemLabel = stringResource(R.string.weather_units_system)
    val current = options.firstOrNull { id(it) == selected }
    ConfigPickerItem(
        title = stringResource(title),
        selectedValue = current?.let(symbol) ?: "$systemLabel (${symbol(system)})",
        iconRes = iconRes,
        modifier = Modifier.fillMaxWidth(),
    ) {
        SegmentedDropdownMenuItem(
            text = { Text("$systemLabel (${symbol(system)})") },
            onClick = {
                selected = SettingsRepository.WEATHER_UNITS_SYSTEM
                onSelected(SettingsRepository.WEATHER_UNITS_SYSTEM)
            },
        )
        options.forEach { option ->
            SegmentedDropdownMenuItem(
                text = { Text(symbol(option)) },
                onClick = {
                    selected = id(option)
                    onSelected(id(option))
                },
            )
        }
    }
}

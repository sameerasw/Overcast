package com.sameerasw.overcast.ui.features.weather

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.sameerasw.overcast.R
import com.sameerasw.overcast.data.repository.SettingsRepository
import com.sameerasw.overcast.ui.components.menus.SegmentedDropdownMenuItem
import com.sameerasw.overcast.ui.core.cards.ConfigPickerItem
import com.sameerasw.overcast.ui.core.containers.RoundedCardContainer
import com.sameerasw.overcast.ui.core.pickers.SegmentedPicker
import com.sameerasw.overcast.utils.HapticUtil
import com.sameerasw.overcast.weather.effects.WeatherSimulation

@Composable
fun WeatherExperimentsUI(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val view = LocalView.current
    val settings = remember { SettingsRepository(context) }
    var simulatedWeather by remember { mutableStateOf(settings.getString(SettingsRepository.KEY_DEBUG_SIMULATED_WEATHER, WeatherSimulation.OFF) ?: WeatherSimulation.OFF) }
    var simulatedAlert by remember { mutableStateOf(settings.getString(SettingsRepository.KEY_DEBUG_SIMULATED_ALERT, WeatherSimulation.OFF) ?: WeatherSimulation.OFF) }
    var simulatedTime by remember { mutableStateOf(settings.getString(SettingsRepository.KEY_DEBUG_SIMULATED_TIME, "auto") ?: "auto") }
    var simulatedTemp by remember { mutableStateOf(settings.getString(SettingsRepository.KEY_DEBUG_SIMULATED_TEMP, "auto") ?: "auto") }

    RoundedCardContainer(modifier = modifier, spacing = 2.dp, cornerRadius = 24.dp) {
        ConfigPickerItem(
            title = stringResource(R.string.dev_simulate_weather_title),
            iconRes = R.drawable.rounded_partly_cloudy_day_24,
            selectedValue = WeatherSimulation.presets.firstOrNull { it.id == simulatedWeather }?.label.orEmpty(),
            modifier = Modifier.fillMaxWidth(),
        ) {
            WeatherSimulation.presets.forEach { preset ->
                SegmentedDropdownMenuItem(
                    text = { Text(preset.label) },
                    onClick = {
                        HapticUtil.performVirtualKeyHaptic(view)
                        simulatedWeather = preset.id
                        settings.putString(SettingsRepository.KEY_DEBUG_SIMULATED_WEATHER, preset.id)
                    },
                )
            }
        }
        ConfigPickerItem(
            title = stringResource(R.string.dev_simulate_alerts_title),
            iconRes = R.drawable.rounded_warning_24,
            selectedValue = WeatherSimulation.alertPresets.firstOrNull { it.id == simulatedAlert }?.label.orEmpty(),
            modifier = Modifier.fillMaxWidth(),
        ) {
            WeatherSimulation.alertPresets.forEach { preset ->
                SegmentedDropdownMenuItem(
                    text = { Text(preset.label) },
                    onClick = {
                        HapticUtil.performVirtualKeyHaptic(view)
                        simulatedAlert = preset.id
                        settings.putString(SettingsRepository.KEY_DEBUG_SIMULATED_ALERT, preset.id)
                    },
                )
            }
        }
        SegmentedPicker(
            items = listOf("auto", "dawn", "day", "dusk", "night"),
            selectedItem = simulatedTime,
            onItemSelected = {
                simulatedTime = it
                settings.putString(SettingsRepository.KEY_DEBUG_SIMULATED_TIME, it)
            },
            labelProvider = {
                context.getString(
                    when (it) {
                        "dawn" -> R.string.weather_sim_time_dawn
                        "day" -> R.string.weather_sim_time_day
                        "dusk" -> R.string.weather_sim_time_dusk
                        "night" -> R.string.weather_sim_time_night
                        else -> R.string.weather_sim_time_auto
                    },
                )
            },
            modifier = Modifier.fillMaxWidth(),
        )
        SegmentedPicker(
            items = listOf("auto", "-15", "5", "22", "38", "50"),
            selectedItem = simulatedTemp,
            onItemSelected = {
                simulatedTemp = it
                settings.putString(SettingsRepository.KEY_DEBUG_SIMULATED_TEMP, it)
            },
            labelProvider = { if (it == "auto") context.getString(R.string.weather_sim_time_auto) else "$it°" },
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

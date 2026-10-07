package com.sameerasw.overcast.weather.widget

import android.content.Context
import com.sameerasw.overcast.data.repository.SettingsRepository
import com.sameerasw.overcast.weather.WeatherIconStyle
import com.sameerasw.overcast.weather.WeatherRepository
import com.sameerasw.overcast.weather.WeatherUnits
import com.sameerasw.overcast.weather.effects.WeatherEffectSpec
import com.sameerasw.overcast.weather.effects.WeatherSimulation
import com.sameerasw.overcast.weather.model.WeatherSnapshot

internal class WidgetWeather(
    val snapshot: WeatherSnapshot?,
    val now: Long,
    val timeOverride: String?,
    val units: WeatherUnits,
    val style: WeatherIconStyle,
    val effectSpec: WeatherEffectSpec,
)

internal suspend fun loadWidgetWeather(context: Context): WidgetWeather {
    WeatherRepository.ensureLoaded(context)
    val settings = SettingsRepository(context)
    val simulation = settings.getSimulatedWeather()
    val timeOverride = settings.getSimulatedTimeOfDay()
    val tempOverride = settings.getSimulatedTempC()
    val alertOverride = settings.getSimulatedAlertId()
    val real = WeatherRepository.state.value.snapshot
    val simulated = real?.let { r -> simulation?.let { WeatherSimulation.apply(r, it) } ?: r }
    val snapshot = simulated?.let { WeatherSimulation.withTimeOfDay(it, timeOverride) }
        ?.let { s -> tempOverride?.let { s.copy(tempC = it) } ?: s }
        ?.let { s -> WeatherSimulation.alertsFor(alertOverride, System.currentTimeMillis())?.let { s.copy(alerts = it) } ?: s }
    return WidgetWeather(
        snapshot = snapshot,
        now = WeatherSimulation.timeFor(snapshot, timeOverride, System.currentTimeMillis()),
        timeOverride = timeOverride,
        units = WeatherUnits.from(settings),
        style = WeatherIconStyle.fromId(settings.getWeatherIconStyle()),
        effectSpec = snapshot?.takeIf { settings.isWeatherEffectsEnabled() }
            ?.let { simulation?.spec ?: WeatherEffectSpec.from(it) } ?: WeatherEffectSpec.None,
    )
}

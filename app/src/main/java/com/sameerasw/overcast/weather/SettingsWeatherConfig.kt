package com.sameerasw.overcast.weather

import com.sameerasw.overcast.data.repository.SettingsRepository
import com.sameerasw.overcast.weather.model.WeatherLocation
import com.sameerasw.overcast.weather.provider.WeatherProviders

class SettingsWeatherConfig(private val settings: SettingsRepository) : WeatherConfig {
    override val providerId: String
        get() = WeatherProviders.resolve(settings.getWeatherProvider(), settings.getWeatherApiKey("weatherapi")).id
    override val apiKey: String? get() = settings.getWeatherApiKey(providerId)
    override val locationMode: WeatherLocationMode
        get() = if (settings.getWeatherLocationMode() == "manual") WeatherLocationMode.MANUAL else WeatherLocationMode.DEVICE
    override val manualLocation: WeatherLocation?
        get() = settings.getWeatherManualLocation()?.let { (lat, lon, name) -> WeatherLocation(lat, lon, name) }
    override val refreshIntervalMinutes: Int get() = settings.getWeatherRefreshMinutes()
}

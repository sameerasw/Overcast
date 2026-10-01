package com.sameerasw.overcast.weather

import com.sameerasw.overcast.weather.model.WeatherLocation

enum class WeatherLocationMode { DEVICE, MANUAL }

interface WeatherConfig {
    val providerId: String
    val apiKey: String?
    val locationMode: WeatherLocationMode
    val manualLocation: WeatherLocation?
    val refreshIntervalMinutes: Int
}

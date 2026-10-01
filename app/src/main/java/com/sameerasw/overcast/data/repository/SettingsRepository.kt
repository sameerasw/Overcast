package com.sameerasw.overcast.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.sameerasw.overcast.weather.effects.WeatherSimulation
import com.sameerasw.overcast.weather.effects.WeatherSimulationPreset

class SettingsRepository(
    context: Context,
) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    companion object {
        const val PREFS_NAME = "overcast_prefs"

        const val KEY_DEVELOPER_MODE_ENABLED = "developer_mode_enabled"

        const val KEY_WEATHER_PROVIDER = "weather_provider"
        const val KEY_WEATHER_API_KEY = "weather_api_key"
        const val KEY_WEATHER_OPENMETEO_MODEL = "weather_openmeteo_model"
        const val KEY_WEATHER_LOCATION_MODE = "weather_location_mode"
        const val KEY_WEATHER_MANUAL_LOCATION = "weather_manual_location"
        const val KEY_WEATHER_UNITS = "weather_units"
        const val KEY_WEATHER_REFRESH_MINUTES = "weather_refresh_minutes"
        const val KEY_WEATHER_EFFECTS = "weather_effects"
        const val KEY_WEATHER_HAPTICS = "weather_haptics"
        const val KEY_DEBUG_WEATHER_EXPERIMENTAL = "debug_weather_experimental"
        const val KEY_DEBUG_SIMULATED_WEATHER = "debug_simulated_weather"
        const val KEY_DEBUG_SIMULATED_TIME = "debug_simulated_time"
        const val KEY_DEBUG_SIMULATED_TEMP = "debug_simulated_temp"

        const val WEATHER_UNITS_SYSTEM = "system"
        const val WEATHER_UNITS_CELSIUS = "celsius"
        const val WEATHER_UNITS_FAHRENHEIT = "fahrenheit"

        fun weatherApiKeyName(providerId: String) = "${KEY_WEATHER_API_KEY}_$providerId"
    }

    fun getBoolean(key: String, default: Boolean = false): Boolean = prefs.getBoolean(key, default)

    fun getString(key: String, default: String? = null): String? = prefs.getString(key, default)

    fun getInt(key: String, default: Int = 0): Int = prefs.getInt(key, default)

    fun putBoolean(key: String, value: Boolean) = prefs.edit().putBoolean(key, value).apply()

    fun putString(key: String, value: String?) = prefs.edit().putString(key, value).apply()

    fun putInt(key: String, value: Int) = prefs.edit().putInt(key, value).apply()

    fun isDeveloperModeEnabled(): Boolean = getBoolean(KEY_DEVELOPER_MODE_ENABLED, false)
    fun setDeveloperModeEnabled(enabled: Boolean) = putBoolean(KEY_DEVELOPER_MODE_ENABLED, enabled)

    fun isWeatherEffectsEnabled(): Boolean = getBoolean(KEY_WEATHER_EFFECTS, true)
    fun setWeatherEffectsEnabled(enabled: Boolean) = putBoolean(KEY_WEATHER_EFFECTS, enabled)

    fun isWeatherHapticsEnabled(): Boolean = getBoolean(KEY_WEATHER_HAPTICS, true)
    fun setWeatherHapticsEnabled(enabled: Boolean) = putBoolean(KEY_WEATHER_HAPTICS, enabled)

    fun getWeatherProvider(): String? = getString(KEY_WEATHER_PROVIDER, null)
    fun setWeatherProvider(id: String) = putString(KEY_WEATHER_PROVIDER, id)

    fun isWeatherExperimentalEnabled(): Boolean = getBoolean(KEY_DEBUG_WEATHER_EXPERIMENTAL, false)

    fun getSimulatedWeather(): WeatherSimulationPreset? =
        if (isWeatherExperimentalEnabled()) {
            WeatherSimulation.find(getString(KEY_DEBUG_SIMULATED_WEATHER, WeatherSimulation.OFF))
        } else {
            null
        }

    fun getSimulatedTimeOfDay(): String? =
        if (isWeatherExperimentalEnabled()) getString(KEY_DEBUG_SIMULATED_TIME, "auto")?.takeIf { it != "auto" } else null

    fun getSimulatedTempC(): Double? =
        if (isWeatherExperimentalEnabled()) getString(KEY_DEBUG_SIMULATED_TEMP, "auto")?.toDoubleOrNull() else null

    fun getWeatherOpenMeteoModel(): String? = getString(KEY_WEATHER_OPENMETEO_MODEL, null)?.takeIf { it.isNotBlank() }
    fun setWeatherOpenMeteoModel(model: String?) = putString(KEY_WEATHER_OPENMETEO_MODEL, model.orEmpty())

    fun getWeatherApiKey(providerId: String): String? =
        getString(weatherApiKeyName(providerId), null)?.takeIf { it.isNotBlank() }

    fun setWeatherApiKey(providerId: String, key: String?) = putString(weatherApiKeyName(providerId), key?.trim().orEmpty())

    fun getWeatherLocationMode(): String = getString(KEY_WEATHER_LOCATION_MODE, "device") ?: "device"
    fun setWeatherLocationMode(mode: String) = putString(KEY_WEATHER_LOCATION_MODE, mode)

    // Stored as "lat|lon|name".
    fun getWeatherManualLocation(): Triple<Double, Double, String>? {
        val parts = getString(KEY_WEATHER_MANUAL_LOCATION, null)?.split("|", limit = 3) ?: return null
        if (parts.size != 3) return null
        val lat = parts[0].toDoubleOrNull() ?: return null
        val lon = parts[1].toDoubleOrNull() ?: return null
        return Triple(lat, lon, parts[2])
    }

    fun setWeatherManualLocation(latitude: Double, longitude: Double, name: String) =
        putString(KEY_WEATHER_MANUAL_LOCATION, "$latitude|$longitude|${name.replace("|", " ")}")

    fun getWeatherUnits(): String = getString(KEY_WEATHER_UNITS, WEATHER_UNITS_SYSTEM) ?: WEATHER_UNITS_SYSTEM
    fun setWeatherUnits(units: String) = putString(KEY_WEATHER_UNITS, units)

    fun getWeatherRefreshMinutes(): Int = getInt(KEY_WEATHER_REFRESH_MINUTES, 60)
    fun setWeatherRefreshMinutes(minutes: Int) = putInt(KEY_WEATHER_REFRESH_MINUTES, minutes)
}

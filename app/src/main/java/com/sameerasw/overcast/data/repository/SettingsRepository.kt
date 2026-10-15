package com.sameerasw.overcast.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.sameerasw.overcast.utils.CompatibilityMode
import com.sameerasw.overcast.utils.DeviceUtils
import com.sameerasw.overcast.weather.WeatherIconStyle
import com.sameerasw.overcast.weather.effects.WeatherSimulation
import com.sameerasw.overcast.weather.widget.WeatherWidgetUpdater
import com.sameerasw.overcast.weather.effects.WeatherSimulationPreset

class SettingsRepository(
    context: Context,
) {
    private val appContext = context.applicationContext
    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    companion object {
        const val PREFS_NAME = "overcast_prefs"

        const val KEY_ONBOARDING_COMPLETED = "onboarding_completed"
        const val KEY_DEVELOPER_MODE_ENABLED = "developer_mode_enabled"

        const val KEY_WEATHER_PROVIDER = "weather_provider"
        const val KEY_WEATHER_API_KEY = "weather_api_key"
        const val KEY_WEATHER_OPENMETEO_MODEL = "weather_openmeteo_model"
        const val KEY_WEATHER_LOCATION_MODE = "weather_location_mode"
        const val KEY_WEATHER_MANUAL_LOCATION = "weather_manual_location"
        const val KEY_WEATHER_SAVED_PLACES = "weather_saved_places"
        const val KEY_WEATHER_RECENT_PLACES = "weather_recent_places"
        const val KEY_WEATHER_UNITS = "weather_units"
        const val KEY_WEATHER_WIND_UNIT = "weather_wind_unit"
        const val KEY_WEATHER_PRESSURE_UNIT = "weather_pressure_unit"
        const val KEY_WEATHER_DISTANCE_UNIT = "weather_distance_unit"
        const val KEY_WEATHER_PRECIPITATION_UNIT = "weather_precipitation_unit"
        const val KEY_WEATHER_ICON_STYLE = "weather_icon_style"
        const val KEY_COMPATIBILITY_MODE = "compatibility_mode"
        const val KEY_AMBIENT_FORECAST = "ambient_show_forecast"
        const val KEY_AMBIENT_TIMELINE = "ambient_timeline_mode"
        const val KEY_SCREENSAVER_AUTO_ANIMATE = "screensaver_auto_animate"
        const val KEY_WEATHER_REFRESH_MINUTES = "weather_refresh_minutes"
        const val KEY_WEATHER_EFFECTS = "weather_effects"
        const val KEY_WEATHER_HAPTICS = "weather_haptics"
        const val KEY_AI_PROVIDER = "ai_provider"
        const val KEY_AI_API_KEY = "ai_api_key"
        const val KEY_AI_MODEL = "ai_model"
        const val KEY_AI_BASE_URL = "ai_base_url"
        const val KEY_DEBUG_SIMULATED_WEATHER = "debug_simulated_weather"
        const val KEY_DEBUG_SIMULATED_ALERT = "debug_simulated_alert"
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

    fun putBoolean(key: String, value: Boolean) {
        prefs.edit().putBoolean(key, value).apply()
        refreshWidgetsIfSimulation(key)
    }

    fun putString(key: String, value: String?) {
        prefs.edit().putString(key, value).apply()
        refreshWidgetsIfSimulation(key)
    }

    private fun refreshWidgetsIfSimulation(key: String) {
        if (key.startsWith("debug_") || key == KEY_DEVELOPER_MODE_ENABLED) WeatherWidgetUpdater.updateAll(appContext)
    }

    fun putInt(key: String, value: Int) = prefs.edit().putInt(key, value).apply()

    fun isOnboardingCompleted(): Boolean = prefs.getBoolean(KEY_ONBOARDING_COMPLETED, false)

    fun setOnboardingCompleted(completed: Boolean) = putBoolean(KEY_ONBOARDING_COMPLETED, completed)

    fun isDeveloperModeEnabled(): Boolean = getBoolean(KEY_DEVELOPER_MODE_ENABLED, false)
    fun setDeveloperModeEnabled(enabled: Boolean) = putBoolean(KEY_DEVELOPER_MODE_ENABLED, enabled)



    fun registerChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener) =
        prefs.registerOnSharedPreferenceChangeListener(listener)

    fun unregisterChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener) =
        prefs.unregisterOnSharedPreferenceChangeListener(listener)

    fun isWeatherEffectsEnabled(): Boolean = getBoolean(KEY_WEATHER_EFFECTS, true)
    fun setWeatherEffectsEnabled(enabled: Boolean) = putBoolean(KEY_WEATHER_EFFECTS, enabled)

    fun isWeatherHapticsEnabled(): Boolean = getBoolean(KEY_WEATHER_HAPTICS, true)
    fun setWeatherHapticsEnabled(enabled: Boolean) = putBoolean(KEY_WEATHER_HAPTICS, enabled)

    fun getWeatherProvider(): String? = getString(KEY_WEATHER_PROVIDER, null)
    fun setWeatherProvider(id: String) = putString(KEY_WEATHER_PROVIDER, id)

    fun isWeatherExperimentalEnabled(): Boolean = isDeveloperModeEnabled()

    fun getSimulatedWeather(): WeatherSimulationPreset? =
        if (isWeatherExperimentalEnabled()) {
            WeatherSimulation.find(getString(KEY_DEBUG_SIMULATED_WEATHER, WeatherSimulation.OFF))
        } else {
            null
        }

    fun getSimulatedAlertId(): String? =
        if (isWeatherExperimentalEnabled()) getString(KEY_DEBUG_SIMULATED_ALERT, WeatherSimulation.OFF)?.takeIf { it != WeatherSimulation.OFF } else null

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
    fun setWeatherUnits(units: String) {
        putString(KEY_WEATHER_UNITS, units)
        WeatherWidgetUpdater.updateAll(appContext)
    }

    fun getWindUnit(): String = getString(KEY_WEATHER_WIND_UNIT, WEATHER_UNITS_SYSTEM) ?: WEATHER_UNITS_SYSTEM
    fun setWindUnit(unit: String) = putString(KEY_WEATHER_WIND_UNIT, unit)

    fun getPressureUnit(): String = getString(KEY_WEATHER_PRESSURE_UNIT, WEATHER_UNITS_SYSTEM) ?: WEATHER_UNITS_SYSTEM
    fun setPressureUnit(unit: String) = putString(KEY_WEATHER_PRESSURE_UNIT, unit)

    fun getDistanceUnit(): String = getString(KEY_WEATHER_DISTANCE_UNIT, WEATHER_UNITS_SYSTEM) ?: WEATHER_UNITS_SYSTEM
    fun setDistanceUnit(unit: String) = putString(KEY_WEATHER_DISTANCE_UNIT, unit)

    fun getPrecipitationUnit(): String = getString(KEY_WEATHER_PRECIPITATION_UNIT, WEATHER_UNITS_SYSTEM) ?: WEATHER_UNITS_SYSTEM
    fun setPrecipitationUnit(unit: String) = putString(KEY_WEATHER_PRECIPITATION_UNIT, unit)

    fun getWeatherIconStyle(): String = getString(KEY_WEATHER_ICON_STYLE, null) ?: WeatherIconStyle.Default.id
    fun setWeatherIconStyle(id: String) {
        putString(KEY_WEATHER_ICON_STYLE, id)
        WeatherWidgetUpdater.updateAll(appContext)
        WeatherWidgetUpdater.refreshPreview(appContext)
    }

    fun isAmbientForecastEnabled(): Boolean = getBoolean(KEY_AMBIENT_FORECAST, true)

    fun isAmbientTimelineEnabled(): Boolean = getBoolean(KEY_AMBIENT_TIMELINE, false)

    fun setAmbientTimelineEnabled(enabled: Boolean) = putBoolean(KEY_AMBIENT_TIMELINE, enabled)

    fun isScreensaverAutoAnimateEnabled(): Boolean = getBoolean(KEY_SCREENSAVER_AUTO_ANIMATE, false)

    fun setScreensaverAutoAnimateEnabled(enabled: Boolean) = putBoolean(KEY_SCREENSAVER_AUTO_ANIMATE, enabled)

    fun setAmbientForecastEnabled(enabled: Boolean) = putBoolean(KEY_AMBIENT_FORECAST, enabled)

    fun isCompatibilityMode(): Boolean =
        if (prefs.contains(KEY_COMPATIBILITY_MODE)) getBoolean(KEY_COMPATIBILITY_MODE, false) else DeviceUtils.isLowEndDevice(appContext)

    fun setCompatibilityMode(enabled: Boolean) {
        putBoolean(KEY_COMPATIBILITY_MODE, enabled)
        CompatibilityMode.enabled.value = enabled
    }

    fun getWeatherRefreshMinutes(): Int = getInt(KEY_WEATHER_REFRESH_MINUTES, 60)
    fun setWeatherRefreshMinutes(minutes: Int) = putInt(KEY_WEATHER_REFRESH_MINUTES, minutes)

    fun aiApiKeyName(providerId: String) = "${KEY_AI_API_KEY}_$providerId"

    fun getAiProvider(): String = getString(KEY_AI_PROVIDER, "gemini") ?: "gemini"
    fun setAiProvider(id: String) = putString(KEY_AI_PROVIDER, id)

    fun getAiApiKey(providerId: String): String? =
        getString(aiApiKeyName(providerId), null)?.takeIf { it.isNotBlank() }

    fun setAiApiKey(providerId: String, key: String?) = putString(aiApiKeyName(providerId), key?.trim().orEmpty())

    fun getAiModel(providerId: String): String? =
        getString("${KEY_AI_MODEL}_$providerId", null)?.takeIf { it.isNotBlank() }

    fun setAiModel(providerId: String, model: String?) = putString("${KEY_AI_MODEL}_$providerId", model?.trim().orEmpty())

    fun getAiBaseUrl(providerId: String): String? =
        getString("${KEY_AI_BASE_URL}_$providerId", null)?.takeIf { it.isNotBlank() }

    fun setAiBaseUrl(providerId: String, url: String?) = putString("${KEY_AI_BASE_URL}_$providerId", url?.trim().orEmpty())
}

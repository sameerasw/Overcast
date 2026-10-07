package com.sameerasw.overcast.weather

import android.icu.util.LocaleData
import android.icu.util.ULocale
import androidx.core.text.util.LocalePreferences
import com.sameerasw.overcast.data.repository.SettingsRepository

enum class TemperatureUnit(val id: String, val symbol: String) {
    CELSIUS("celsius", "°C"),
    FAHRENHEIT("fahrenheit", "°F"),
}

enum class WindSpeedUnit(val id: String, val symbol: String) {
    KPH("kph", "km/h"),
    MPH("mph", "mph"),
    MPS("mps", "m/s"),
    KNOTS("knots", "kn"),
}

enum class PressureUnit(val id: String, val symbol: String) {
    HPA("hpa", "hPa"),
    INHG("inhg", "inHg"),
    MMHG("mmhg", "mmHg"),
    KPA("kpa", "kPa"),
}

enum class DistanceUnit(val id: String, val symbol: String) {
    KM("km", "km"),
    MILES("mi", "mi"),
}

enum class PrecipitationUnit(val id: String, val symbol: String) {
    MM("mm", "mm"),
    INCHES("in", "in"),
}

data class WeatherUnits(
    val temperature: TemperatureUnit,
    val wind: WindSpeedUnit,
    val pressure: PressureUnit,
    val distance: DistanceUnit,
    val precipitation: PrecipitationUnit,
) {
    companion object {
        fun from(settings: SettingsRepository): WeatherUnits = WeatherUnits(
            temperature = resolve(settings.getWeatherUnits(), TemperatureUnit.entries, { it.id }, ::systemTemperature),
            wind = resolve(settings.getWindUnit(), WindSpeedUnit.entries, { it.id }, ::systemWind),
            pressure = resolve(settings.getPressureUnit(), PressureUnit.entries, { it.id }, ::systemPressure),
            distance = resolve(settings.getDistanceUnit(), DistanceUnit.entries, { it.id }, ::systemDistance),
            precipitation = resolve(settings.getPrecipitationUnit(), PrecipitationUnit.entries, { it.id }, ::systemPrecipitation),
        )

        // Unknown or stale stored values fall back to the auto-detected unit.
        private fun <T> resolve(setting: String, options: List<T>, id: (T) -> String, system: () -> T): T =
            options.firstOrNull { id(it) == setting } ?: system()

        private enum class Region { METRIC, UK, US }

        private fun region(): Region {
            val system = LocaleData.getMeasurementSystem(ULocale.forLocale(java.util.Locale.getDefault()))
            return when (system) {
                LocaleData.MeasurementSystem.US -> Region.US
                LocaleData.MeasurementSystem.UK -> Region.UK
                else -> Region.METRIC
            }
        }

        fun systemTemperature(): TemperatureUnit =
            if (LocalePreferences.getTemperatureUnit() == LocalePreferences.TemperatureUnit.FAHRENHEIT) {
                TemperatureUnit.FAHRENHEIT
            } else {
                TemperatureUnit.CELSIUS
            }

        fun systemWind(): WindSpeedUnit = if (region() == Region.METRIC) WindSpeedUnit.KPH else WindSpeedUnit.MPH

        fun systemPressure(): PressureUnit = if (region() == Region.US) PressureUnit.INHG else PressureUnit.HPA

        fun systemDistance(): DistanceUnit = if (region() == Region.METRIC) DistanceUnit.KM else DistanceUnit.MILES

        fun systemPrecipitation(): PrecipitationUnit = if (region() == Region.US) PrecipitationUnit.INCHES else PrecipitationUnit.MM
    }
}

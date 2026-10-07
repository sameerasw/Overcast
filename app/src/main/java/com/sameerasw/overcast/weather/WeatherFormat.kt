package com.sameerasw.overcast.weather

import com.sameerasw.overcast.R
import com.sameerasw.overcast.weather.model.WeatherCondition
import java.util.Locale
import kotlin.math.roundToInt

object WeatherFormat {
    private const val MISSING = "--"
    private const val KM_PER_MILE = 1.609344

    // Providers can leave values out or send NaN/infinity; show a placeholder instead of garbage.
    private inline fun format(value: Double, block: (Double) -> String): String =
        if (value.isFinite()) block(value) else MISSING

    fun temperature(celsius: Double, unit: TemperatureUnit): String = format(celsius) {
        val converted = when (unit) {
            TemperatureUnit.CELSIUS -> it
            TemperatureUnit.FAHRENHEIT -> it * 9.0 / 5.0 + 32.0
        }
        "${converted.roundToInt()}°"
    }

    fun wind(kph: Double, unit: WindSpeedUnit): String = format(kph) {
        val converted = when (unit) {
            WindSpeedUnit.KPH -> it
            WindSpeedUnit.MPH -> it / KM_PER_MILE
            WindSpeedUnit.MPS -> it / 3.6
            WindSpeedUnit.KNOTS -> it / 1.852
        }
        "${converted.roundToInt()} ${unit.symbol}"
    }

    fun pressure(hpa: Double, unit: PressureUnit): String = format(hpa) {
        when (unit) {
            PressureUnit.HPA -> "${it.roundToInt()} hPa"
            PressureUnit.KPA -> "%.1f kPa".format(Locale.US, it / 10.0)
            PressureUnit.INHG -> "%.2f inHg".format(Locale.US, it * 0.02953)
            PressureUnit.MMHG -> "${(it * 0.750062).roundToInt()} mmHg"
        }
    }

    fun distance(km: Double, unit: DistanceUnit): String = format(km) {
        when (unit) {
            DistanceUnit.KM -> "%.1f km".format(Locale.US, it)
            DistanceUnit.MILES -> "%.1f mi".format(Locale.US, it / KM_PER_MILE)
        }
    }

    fun precipitation(mm: Double, unit: PrecipitationUnit): String = format(mm) {
        when (unit) {
            PrecipitationUnit.MM -> "%.1f mm".format(Locale.US, it)
            PrecipitationUnit.INCHES -> "%.2f in".format(Locale.US, it / 25.4)
        }
    }
}

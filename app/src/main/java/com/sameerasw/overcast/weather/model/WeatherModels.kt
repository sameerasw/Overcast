package com.sameerasw.overcast.weather.model

import androidx.annotation.Keep
import java.util.Locale

@Keep
enum class WeatherCondition {
    CLEAR,
    PARTLY_CLOUDY,
    CLOUDY,
    FOG,
    DRIZZLE,
    RAIN,
    HEAVY_RAIN,
    SLEET,
    SNOW,
    HAIL,
    THUNDERSTORM,
    UNKNOWN,
}

@Keep
enum class AlertSeverity {
    MINOR,
    MODERATE,
    SEVERE,
    EXTREME,
    UNKNOWN,
    ;

    val isSevere: Boolean get() = this == SEVERE || this == EXTREME
}

@Keep
data class WeatherLocation(
    val latitude: Double,
    val longitude: Double,
    val name: String? = null,
) {
    // ~1km precision is plenty for weather and keeps the exact position off the wire.
    val query: String get() = "%.2f,%.2f".format(Locale.US, latitude, longitude)
}

data class CityResult(
    val name: String,
    val region: String,
    val country: String,
    val latitude: Double,
    val longitude: Double,
) {
    val label: String get() = listOf(name, region, country).filter { it.isNotBlank() }.distinct().joinToString(", ")
}

@Keep
data class HourlyForecast(
    val timeMillis: Long,
    val tempC: Double,
    val condition: WeatherCondition,
    val isDay: Boolean,
    val chanceOfRain: Int,
    val precipitationMm: Double? = null,
    val snowfallCm: Double? = null,
    val feelsLikeC: Double? = null,
    val humidity: Int? = null,
    val windKph: Double? = null,
    val windGustKph: Double? = null,
    val windDirectionDeg: Double? = null,
    val uvIndex: Double? = null,
    val cloudCover: Int? = null,
    val cape: Double? = null,
    val lightningPotential: Double? = null,
)

@Keep
data class DailyForecast(
    val dayMillis: Long,
    val highC: Double,
    val lowC: Double,
    val condition: WeatherCondition,
    val chanceOfRain: Int,
    val precipitationMm: Double? = null,
    val precipitationHours: Double? = null,
    val snowfallCm: Double? = null,
    val sunshineSeconds: Double? = null,
    val daylightSeconds: Double? = null,
    val windMaxKph: Double? = null,
    val windGustMaxKph: Double? = null,
    val windDirectionDeg: Double? = null,
    val uvIndex: Double? = null,
    val sunriseMillis: Long? = null,
    val sunsetMillis: Long? = null,
)

@Keep
data class MinutelyPrecipitation(val timeMillis: Long, val mm: Double)

// Optional extras; providers fill what they offer and the detail sheet hides the rest.
@Keep
data class WeatherExtras(
    val pressureHpa: Double? = null,
    val visibilityKm: Double? = null,
    val dewPointC: Double? = null,
    val cloudCover: Int? = null,
    val uvIndex: Double? = null,
    val windGustKph: Double? = null,
    val windDirectionDeg: Double? = null,
    val precipitationMm: Double? = null,
    val sunriseMillis: Long? = null,
    val sunsetMillis: Long? = null,
    val moonPhase: Double? = null,
    val snowDepthCm: Double? = null,
    val freezingLevelM: Double? = null,
    val minutely: List<MinutelyPrecipitation>? = null,
)

@Keep
data class WeatherAlert(
    val id: String,
    val event: String,
    val headline: String,
    val severity: AlertSeverity,
    val description: String,
    val effectiveMillis: Long?,
    val expiresMillis: Long?,
) {
    fun isActive(now: Long): Boolean = expiresMillis == null || expiresMillis > now
}

@Keep
data class WeatherSnapshot(
    val locationName: String,
    val region: String,
    val tempC: Double,
    val feelsLikeC: Double,
    val condition: WeatherCondition,
    val conditionText: String,
    val isDay: Boolean,
    val humidity: Int?,
    val windKph: Double,
    val chanceOfRain: Int?,
    val highC: Double,
    val lowC: Double,
    val hourly: List<HourlyForecast>,
    val alerts: List<WeatherAlert>,
    val updatedAt: Long,
    val providerId: String,
    val extras: WeatherExtras? = null,
    val daily: List<DailyForecast>? = null,
) {
    fun activeAlerts(now: Long = System.currentTimeMillis()): List<WeatherAlert> = alerts.filter { it.isActive(now) }

    fun upcomingHours(now: Long = System.currentTimeMillis()): List<HourlyForecast> = hourly.filter { it.timeMillis + HOUR_MS > now }

    fun todayHighC(now: Long = System.currentTimeMillis()): Double = todayEntry(now)?.highC ?: highC

    fun todayLowC(now: Long = System.currentTimeMillis()): Double = todayEntry(now)?.lowC ?: lowC

    private fun todayEntry(now: Long): DailyForecast? =
        upcomingDays(now).firstOrNull()?.takeIf { it.dayMillis <= now && updatedAt < it.dayMillis }

    fun upcomingDays(now: Long = System.currentTimeMillis()): List<DailyForecast> = daily.orEmpty().filter { it.dayMillis + DAY_MS > now }

    fun isDayAt(now: Long = System.currentTimeMillis()): Boolean {
        val todayDaily = daily?.firstOrNull { it.dayMillis <= now && it.dayMillis + DAY_MS > now }
        val sunrise = todayDaily?.sunriseMillis ?: extras?.sunriseMillis
        val sunset = todayDaily?.sunsetMillis ?: extras?.sunsetMillis
        if (sunrise != null && sunset != null) {
            val shift = Math.floorDiv(now - sunrise, DAY_MS) * DAY_MS
            val r = sunrise + shift
            val s = sunset + shift
            return now in r..s
        }
        val currentHour = upcomingHours(now).firstOrNull()
        if (currentHour != null) {
            return currentHour.isDay
        }
        val hour = java.util.Calendar.getInstance().apply { timeInMillis = now }.get(java.util.Calendar.HOUR_OF_DAY)
        return hour in 6..18
    }

    fun atTime(now: Long = System.currentTimeMillis(), context: android.content.Context? = null): WeatherSnapshot {
        if (hourly.isEmpty()) return copy(isDay = isDayAt(now), highC = todayHighC(now), lowC = todayLowC(now))
        val isDayNow = isDayAt(now)
        val ageMs = now - updatedAt
        if (kotlin.math.abs(ageMs) < 30 * 60_000L) {
            return copy(isDay = isDayNow, highC = todayHighC(now), lowC = todayLowC(now))
        }
        val upcoming = upcomingHours(now)
        val currentHour = upcoming.firstOrNull() ?: hourly.lastOrNull() ?: return this
        val nextHour = upcoming.getOrNull(1)

        val tempNow = if (nextHour != null && nextHour.timeMillis > currentHour.timeMillis && now >= currentHour.timeMillis) {
            val progress = ((now - currentHour.timeMillis).toDouble() / (nextHour.timeMillis - currentHour.timeMillis)).coerceIn(0.0, 1.0)
            currentHour.tempC + (nextHour.tempC - currentHour.tempC) * progress
        } else {
            currentHour.tempC
        }

        val feelsLikeNow = if (nextHour != null && currentHour.feelsLikeC != null && nextHour.feelsLikeC != null && nextHour.timeMillis > currentHour.timeMillis && now >= currentHour.timeMillis) {
            val progress = ((now - currentHour.timeMillis).toDouble() / (nextHour.timeMillis - currentHour.timeMillis)).coerceIn(0.0, 1.0)
            currentHour.feelsLikeC + (nextHour.feelsLikeC - currentHour.feelsLikeC) * progress
        } else {
            currentHour.feelsLikeC ?: tempNow
        }

        val conditionNow = currentHour.condition
        val conditionTextNow = if (context != null) {
            com.sameerasw.overcast.weather.WeatherFormat.conditionLabel(context, conditionNow)
        } else {
            if (conditionNow == condition) conditionText else conditionNow.name.lowercase().replaceFirstChar(Char::uppercase)
        }

        return copy(
            tempC = tempNow,
            feelsLikeC = feelsLikeNow,
            condition = conditionNow,
            conditionText = conditionTextNow,
            isDay = isDayNow,
            humidity = currentHour.humidity ?: humidity,
            windKph = currentHour.windKph ?: windKph,
            chanceOfRain = currentHour.chanceOfRain,
            highC = todayHighC(now),
            lowC = todayLowC(now),
        )
    }
}

sealed interface WeatherError {
    data object MissingApiKey : WeatherError
    data object InvalidApiKey : WeatherError
    data object NoLocation : WeatherError
    data object LocationPermission : WeatherError
    data object Network : WeatherError
    data object RateLimited : WeatherError
    data class Unknown(val message: String?) : WeatherError
}

data class WeatherState(
    val snapshot: WeatherSnapshot? = null,
    val loading: Boolean = false,
    val error: WeatherError? = null,
)

private const val HOUR_MS = 60 * 60_000L
private const val DAY_MS = 24 * HOUR_MS

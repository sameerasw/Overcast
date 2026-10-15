package com.sameerasw.overcast.weather.aqi

import android.content.Context
import com.sameerasw.overcast.data.repository.SettingsRepository
import com.sameerasw.overcast.weather.location.DeviceLocationSource
import com.sameerasw.overcast.weather.model.WeatherLocation
import com.sameerasw.overcast.weather.provider.ProviderHttp
import java.util.Locale
import org.json.JSONObject

data class AqiData(
    val aqi: Int?,
    val pm25: Double?,
    val pm10: Double?,
    val ozone: Double?,
    val no2: Double?,
) {
    val category: AqiCategory get() = AqiCategory.from(aqi)
}

enum class AqiCategory(val max: Int) {
    GOOD(50),
    MODERATE(100),
    SENSITIVE(150),
    UNHEALTHY(200),
    VERY_UNHEALTHY(300),
    HAZARDOUS(Int.MAX_VALUE),
    UNKNOWN(Int.MAX_VALUE),
    ;

    companion object {
        fun from(aqi: Int?): AqiCategory = when {
            aqi == null -> UNKNOWN
            aqi <= 50 -> GOOD
            aqi <= 100 -> MODERATE
            aqi <= 150 -> SENSITIVE
            aqi <= 200 -> UNHEALTHY
            aqi <= 300 -> VERY_UNHEALTHY
            else -> HAZARDOUS
        }
    }
}

object AqiRepository {
    suspend fun current(context: Context): AqiData {
        val loc = resolveLocation(context) ?: throw AqiException("No location for AQI")
        val lat = "%.2f".format(Locale.US, loc.latitude)
        val lon = "%.2f".format(Locale.US, loc.longitude)
        val url = "https://air-quality-api.open-meteo.com/v1/air-quality" +
            "?latitude=$lat&longitude=$lon&current=us_aqi,pm2_5,pm10,ozone,nitrogen_dioxide&timezone=auto"
        val raw = try {
            ProviderHttp.get(url)
        } catch (e: Exception) {
            throw AqiException(e.message ?: "AQI request failed")
        }
        val current = JSONObject(raw).optJSONObject("current") ?: throw AqiException("Bad AQI response")
        return AqiData(
            aqi = current.optInt("us_aqi", -1).takeIf { it >= 0 },
            pm25 = current.optDouble("pm2_5").takeIf { !it.isNaN() },
            pm10 = current.optDouble("pm10").takeIf { !it.isNaN() },
            ozone = current.optDouble("ozone").takeIf { !it.isNaN() },
            no2 = current.optDouble("nitrogen_dioxide").takeIf { !it.isNaN() },
        )
    }

    private suspend fun resolveLocation(context: Context): WeatherLocation? {
        val app = context.applicationContext
        val settings = SettingsRepository(app)
        if (settings.getWeatherLocationMode() == "manual") {
            settings.getWeatherManualLocation()?.let { return WeatherLocation(it.first, it.second, it.third) }
        }
        DeviceLocationSource.current(app)?.let { return it }
        return null
    }
}

class AqiException(message: String?) : Exception(message)

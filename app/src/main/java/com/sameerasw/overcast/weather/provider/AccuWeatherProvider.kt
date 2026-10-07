package com.sameerasw.overcast.weather.provider

import com.sameerasw.overcast.weather.model.CityResult
import com.sameerasw.overcast.weather.model.DailyForecast
import com.sameerasw.overcast.weather.model.HourlyForecast
import com.sameerasw.overcast.weather.model.WeatherCondition
import com.sameerasw.overcast.weather.model.WeatherExtras
import com.sameerasw.overcast.weather.model.WeatherLocation
import com.sameerasw.overcast.weather.model.WeatherSnapshot
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.util.concurrent.ConcurrentHashMap

class AccuWeatherProvider : WeatherProvider {
    override val id = "accuweather"
    override val displayName = "AccuWeather"
    override val requiresApiKey = true
    override val signupUrl = "https://developer.accuweather.com/home"

    override suspend fun fetch(location: WeatherLocation, apiKey: String?): WeatherSnapshot {
        val key = requireKey(apiKey)
        return try {
            val place = place(location, key)
            val current = JSONArray(get("currentconditions/v1/${place.key}", key, "details=true")).getJSONObject(0)
            val hourlyArray = JSONArray(get("forecasts/v1/hourly/12hour/${place.key}", key, "metric=true"))
            val dailyJson = JSONObject(get("forecasts/v1/daily/5day/${place.key}", key, "metric=true&details=true"))
            parse(location, place, current, hourlyArray, dailyJson)
        } catch (e: JSONException) {
            throw WeatherProviderException(WeatherProviderException.Reason.BAD_RESPONSE, e.message)
        }
    }

    override suspend fun searchCities(query: String, apiKey: String?): List<CityResult> {
        val key = requireKey(apiKey)
        if (query.isBlank()) return emptyList()
        return try {
            val array = JSONArray(get("locations/v1/cities/search", key, "q=${ProviderHttp.encode(query.trim())}"))
            (0 until minOf(array.length(), MAX_RESULTS)).map { i ->
                val o = array.getJSONObject(i)
                val geo = o.getJSONObject("GeoPosition")
                CityResult(
                    name = o.optString("LocalizedName"),
                    region = o.optJSONObject("AdministrativeArea")?.optString("LocalizedName").orEmpty(),
                    country = o.optJSONObject("Country")?.optString("LocalizedName").orEmpty(),
                    latitude = geo.getDouble("Latitude"),
                    longitude = geo.getDouble("Longitude"),
                )
            }
        } catch (e: JSONException) {
            throw WeatherProviderException(WeatherProviderException.Reason.BAD_RESPONSE, e.message)
        }
    }

    private class Place(val key: String, val name: String, val region: String)

    // The free plan is capped per day, so the coordinate lookup is only paid for once per spot.
    private suspend fun place(location: WeatherLocation, key: String): Place {
        placeCache[location.query]?.let { return it }
        val o = JSONObject(get("locations/v1/cities/geoposition/search", key, "q=${ProviderHttp.encode(location.query)}"))
        val place = Place(
            key = o.getString("Key"),
            name = o.optString("LocalizedName"),
            region = o.optJSONObject("AdministrativeArea")?.optString("LocalizedName")?.takeIf { it.isNotBlank() }
                ?: o.optJSONObject("Country")?.optString("LocalizedName").orEmpty(),
        )
        placeCache[location.query] = place
        return place
    }

    // AccuWeather answers 503 once the daily allowance is used up.
    private suspend fun get(path: String, key: String, query: String): String = try {
        ProviderHttp.get("$BASE_URL/$path?apikey=${ProviderHttp.encode(key)}&$query")
    } catch (e: WeatherProviderException) {
        if (e.reason == WeatherProviderException.Reason.BAD_RESPONSE && e.message == "HTTP 503") {
            throw WeatherProviderException(WeatherProviderException.Reason.RATE_LIMITED)
        }
        throw e
    }

    private fun requireKey(apiKey: String?): String {
        val key = apiKey?.trim().orEmpty()
        if (key.isEmpty()) throw WeatherProviderException(WeatherProviderException.Reason.INVALID_KEY)
        return key
    }

    private fun parse(
        requested: WeatherLocation,
        place: Place,
        current: JSONObject,
        hourlyArray: JSONArray,
        dailyJson: JSONObject,
    ): WeatherSnapshot {
        val now = System.currentTimeMillis()
        val days = dailyJson.getJSONArray("DailyForecasts")
        val today = days.getJSONObject(0)
        val todayTemp = today.getJSONObject("Temperature")
        val isDay = current.optBoolean("IsDayTime", true)
        val tempC = metric(current, "Temperature") ?: 0.0
        val sun = today.optJSONObject("Sun")

        val hourly = (0 until hourlyArray.length()).map { i ->
            val h = hourlyArray.getJSONObject(i)
            HourlyForecast(
                timeMillis = (if (h.has("EpochDateTime")) h.getLong("EpochDateTime") else h.getLong("EpochDate")) * 1000L,
                tempC = h.getJSONObject("Temperature").getDouble("Value"),
                condition = conditionFor(h.optInt("WeatherIcon")),
                isDay = h.optBoolean("IsDaylight", true),
                chanceOfRain = h.optInt("PrecipitationProbability"),
            )
        }

        return WeatherSnapshot(
            locationName = requested.name ?: place.name,
            region = place.region,
            tempC = tempC,
            feelsLikeC = metric(current, "RealFeelTemperature") ?: tempC,
            condition = conditionFor(current.optInt("WeatherIcon")),
            conditionText = current.optString("WeatherText"),
            isDay = isDay,
            humidity = current.optInt("RelativeHumidity"),
            windKph = nested(current, "Wind", "Speed") ?: 0.0,
            chanceOfRain = today.optJSONObject("Day")?.optInt("PrecipitationProbability") ?: 0,
            highC = todayTemp.getJSONObject("Maximum").getDouble("Value"),
            lowC = todayTemp.getJSONObject("Minimum").getDouble("Value"),
            hourly = hourly,
            alerts = emptyList(),
            updatedAt = now,
            providerId = id,
            extras = WeatherExtras(
                pressureHpa = metric(current, "Pressure"),
                visibilityKm = metric(current, "Visibility"),
                dewPointC = metric(current, "DewPoint"),
                cloudCover = current.optInt("CloudCover", -1).takeIf { it >= 0 },
                uvIndex = current.optDouble("UVIndex").takeUnless { it.isNaN() },
                windGustKph = nested(current, "WindGust", "Speed"),
                windDirectionDeg = current.optJSONObject("Wind")?.optJSONObject("Direction")
                    ?.optDouble("Degrees")?.takeUnless { it.isNaN() },
                precipitationMm = metric(current, "Precip1hr"),
                sunriseMillis = sun?.optLong("EpochRise", 0L)?.takeIf { it > 0 }?.times(1000L),
                sunsetMillis = sun?.optLong("EpochSet", 0L)?.takeIf { it > 0 }?.times(1000L),
            ),
            daily = (0 until days.length()).map { i ->
                val d = days.getJSONObject(i)
                val t = d.getJSONObject("Temperature")
                val day = d.optJSONObject("Day")
                DailyForecast(
                    dayMillis = (if (d.has("EpochDate")) d.getLong("EpochDate") else d.getLong("EpochDateTime")) * 1000L,
                    highC = t.getJSONObject("Maximum").getDouble("Value"),
                    lowC = t.getJSONObject("Minimum").getDouble("Value"),
                    condition = conditionFor(day?.optInt("Icon") ?: 0),
                    chanceOfRain = day?.optInt("PrecipitationProbability") ?: 0,
                )
            },
        )
    }

    // Current conditions nest values as { Metric: { Value }, Imperial: { Value } }.
    private fun metric(o: JSONObject, name: String): Double? =
        o.optJSONObject(name)?.optJSONObject("Metric")?.optDouble("Value")?.takeUnless { it.isNaN() }

    private fun nested(o: JSONObject, name: String, inner: String): Double? =
        o.optJSONObject(name)?.optJSONObject(inner)?.optJSONObject("Metric")?.optDouble("Value")?.takeUnless { it.isNaN() }

    // https://developer.accuweather.com/weather-icons
    private fun conditionFor(icon: Int): WeatherCondition = when (icon) {
        1, 2, 30, 31, 32, 33, 34 -> WeatherCondition.CLEAR
        3, 4, 5, 35, 36, 37 -> WeatherCondition.PARTLY_CLOUDY
        6, 7, 8, 38 -> WeatherCondition.CLOUDY
        11 -> WeatherCondition.FOG
        12, 13, 14, 39, 40 -> WeatherCondition.DRIZZLE
        18 -> WeatherCondition.RAIN
        15, 16, 17, 41, 42 -> WeatherCondition.THUNDERSTORM
        19, 20, 21, 22, 23, 43, 44 -> WeatherCondition.SNOW
        24, 25, 26, 29 -> WeatherCondition.SLEET
        else -> WeatherCondition.UNKNOWN
    }

    private companion object {
        const val BASE_URL = "https://dataservice.accuweather.com"
        const val MAX_RESULTS = 8
        val placeCache = ConcurrentHashMap<String, Place>()
    }
}

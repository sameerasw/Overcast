package com.sameerasw.overcast.weather.location

import android.content.Context
import androidx.annotation.Keep
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.sameerasw.overcast.data.repository.SettingsRepository
import com.sameerasw.overcast.weather.model.CityResult
import java.util.Locale
import kotlin.math.abs

@Keep
data class WeatherPlace(
    val name: String,
    val label: String,
    val latitude: Double,
    val longitude: Double,
) {
    val id: String get() = "%.3f,%.3f".format(Locale.US, latitude, longitude)

    fun sameSpotAs(latitude: Double, longitude: Double): Boolean =
        abs(this.latitude - latitude) < 0.001 && abs(this.longitude - longitude) < 0.001

    companion object {
        fun from(city: CityResult) = WeatherPlace(city.name, city.label, city.latitude, city.longitude)
    }
}

class WeatherPlaces(context: Context) {
    private val settings = SettingsRepository(context.applicationContext)
    private val gson = Gson()
    private val listType = object : TypeToken<List<WeatherPlace>>() {}.type

    fun saved(): List<WeatherPlace> = read(SettingsRepository.KEY_WEATHER_SAVED_PLACES)

    fun recent(): List<WeatherPlace> = read(SettingsRepository.KEY_WEATHER_RECENT_PLACES)

    fun isSaved(place: WeatherPlace): Boolean = saved().any { it.id == place.id }

    fun save(place: WeatherPlace) {
        write(SettingsRepository.KEY_WEATHER_SAVED_PLACES, saved().filterNot { it.id == place.id } + place)
        removeRecent(place)
    }

    fun unsave(place: WeatherPlace) {
        write(SettingsRepository.KEY_WEATHER_SAVED_PLACES, saved().filterNot { it.id == place.id })
    }

    fun addRecent(place: WeatherPlace) {
        if (isSaved(place)) return
        write(SettingsRepository.KEY_WEATHER_RECENT_PLACES, (listOf(place) + recent().filterNot { it.id == place.id }).take(MAX_RECENT))
    }

    fun removeRecent(place: WeatherPlace) {
        write(SettingsRepository.KEY_WEATHER_RECENT_PLACES, recent().filterNot { it.id == place.id })
    }

    private fun read(key: String): List<WeatherPlace> =
        try {
            gson.fromJson<List<WeatherPlace>>(settings.getString(key, null), listType).orEmpty()
        } catch (_: Exception) {
            emptyList()
        }

    private fun write(key: String, places: List<WeatherPlace>) = settings.putString(key, gson.toJson(places))

    companion object {
        const val MAX_RECENT = 5
    }
}

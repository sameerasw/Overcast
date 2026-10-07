package com.sameerasw.overcast.weather

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.sameerasw.overcast.R
import com.sameerasw.overcast.weather.model.WeatherCondition

// Every distinct picture a style has to provide. Day and night differ only for clear and partly cloudy skies.
enum class WeatherIconSlot {
    CLEAR_DAY,
    CLEAR_NIGHT,
    PARTLY_CLOUDY_DAY,
    PARTLY_CLOUDY_NIGHT,
    CLOUDY,
    FOG,
    DRIZZLE,
    RAIN,
    HEAVY_RAIN,
    SLEET,
    SNOW,
    HAIL,
    THUNDERSTORM,
    ;

    companion object {
        fun of(condition: WeatherCondition, isDay: Boolean): WeatherIconSlot = when (condition) {
            WeatherCondition.CLEAR -> if (isDay) CLEAR_DAY else CLEAR_NIGHT
            WeatherCondition.PARTLY_CLOUDY -> if (isDay) PARTLY_CLOUDY_DAY else PARTLY_CLOUDY_NIGHT
            WeatherCondition.CLOUDY, WeatherCondition.UNKNOWN -> CLOUDY
            WeatherCondition.FOG -> FOG
            WeatherCondition.DRIZZLE -> DRIZZLE
            WeatherCondition.RAIN -> RAIN
            WeatherCondition.HEAVY_RAIN -> HEAVY_RAIN
            WeatherCondition.SLEET -> SLEET
            WeatherCondition.SNOW -> SNOW
            WeatherCondition.HAIL -> HAIL
            WeatherCondition.THUNDERSTORM -> THUNDERSTORM
        }
    }
}

// tintable styles are single-colour glyphs drawn in the palette accent; the rest keep their own colours.
enum class WeatherIconStyle(
    val id: String,
    @StringRes val labelRes: Int,
    val tintable: Boolean,
    private val icons: Map<WeatherIconSlot, Int>,
) {
    ROUNDED(
        "rounded",
        R.string.weather_icon_style_rounded,
        true,
        mapOf(
            WeatherIconSlot.CLEAR_DAY to R.drawable.rounded_sunny_24,
            WeatherIconSlot.CLEAR_NIGHT to R.drawable.rounded_bedtime_24,
            WeatherIconSlot.PARTLY_CLOUDY_DAY to R.drawable.rounded_partly_cloudy_day_24,
            WeatherIconSlot.PARTLY_CLOUDY_NIGHT to R.drawable.rounded_partly_cloudy_night_24,
            WeatherIconSlot.CLOUDY to R.drawable.rounded_cloud_24,
            WeatherIconSlot.FOG to R.drawable.rounded_foggy_24,
            WeatherIconSlot.DRIZZLE to R.drawable.rounded_rainy_light_24,
            WeatherIconSlot.RAIN to R.drawable.rounded_rainy_24,
            WeatherIconSlot.HEAVY_RAIN to R.drawable.rounded_rainy_heavy_24,
            WeatherIconSlot.SLEET to R.drawable.rounded_rainy_snow_24,
            WeatherIconSlot.SNOW to R.drawable.rounded_weather_snowy_24,
            WeatherIconSlot.HAIL to R.drawable.rounded_weather_hail_24,
            WeatherIconSlot.THUNDERSTORM to R.drawable.rounded_thunderstorm_24,
        ),
    ),
    FILLED(
        "filled",
        R.string.weather_icon_style_filled,
        true,
        mapOf(
            WeatherIconSlot.CLEAR_DAY to R.drawable.filled_sunny_24,
            WeatherIconSlot.CLEAR_NIGHT to R.drawable.filled_bedtime_24,
            WeatherIconSlot.PARTLY_CLOUDY_DAY to R.drawable.filled_partly_cloudy_day_24,
            WeatherIconSlot.PARTLY_CLOUDY_NIGHT to R.drawable.filled_partly_cloudy_night_24,
            WeatherIconSlot.CLOUDY to R.drawable.filled_cloud_24,
            WeatherIconSlot.FOG to R.drawable.filled_foggy_24,
            WeatherIconSlot.DRIZZLE to R.drawable.filled_rainy_light_24,
            WeatherIconSlot.RAIN to R.drawable.filled_rainy_24,
            WeatherIconSlot.HEAVY_RAIN to R.drawable.filled_rainy_heavy_24,
            WeatherIconSlot.SLEET to R.drawable.filled_rainy_snow_24,
            WeatherIconSlot.SNOW to R.drawable.filled_weather_snowy_24,
            WeatherIconSlot.HAIL to R.drawable.filled_weather_hail_24,
            WeatherIconSlot.THUNDERSTORM to R.drawable.filled_thunderstorm_24,
        ),
    ),
    METEOCONS(
        "meteocons",
        R.string.weather_icon_style_meteocons,
        false,
        mapOf(
            WeatherIconSlot.CLEAR_DAY to R.drawable.meteocons_clear_day,
            WeatherIconSlot.CLEAR_NIGHT to R.drawable.meteocons_clear_night,
            WeatherIconSlot.PARTLY_CLOUDY_DAY to R.drawable.meteocons_partly_cloudy_day,
            WeatherIconSlot.PARTLY_CLOUDY_NIGHT to R.drawable.meteocons_partly_cloudy_night,
            WeatherIconSlot.CLOUDY to R.drawable.meteocons_cloudy,
            WeatherIconSlot.FOG to R.drawable.meteocons_fog,
            WeatherIconSlot.DRIZZLE to R.drawable.meteocons_drizzle,
            WeatherIconSlot.RAIN to R.drawable.meteocons_rain,
            WeatherIconSlot.HEAVY_RAIN to R.drawable.meteocons_heavy_rain,
            WeatherIconSlot.SLEET to R.drawable.meteocons_sleet,
            WeatherIconSlot.SNOW to R.drawable.meteocons_snow,
            WeatherIconSlot.HAIL to R.drawable.meteocons_hail,
            WeatherIconSlot.THUNDERSTORM to R.drawable.meteocons_thunderstorm,
        ),
    ),
    WEATHER_ICONS(
        "weather_icons",
        R.string.weather_icon_style_weather_icons,
        true,
        mapOf(
            WeatherIconSlot.CLEAR_DAY to R.drawable.wi_day_sunny,
            WeatherIconSlot.CLEAR_NIGHT to R.drawable.wi_night_clear,
            WeatherIconSlot.PARTLY_CLOUDY_DAY to R.drawable.wi_day_cloudy,
            WeatherIconSlot.PARTLY_CLOUDY_NIGHT to R.drawable.wi_night_alt_cloudy,
            WeatherIconSlot.CLOUDY to R.drawable.wi_cloudy,
            WeatherIconSlot.FOG to R.drawable.wi_fog,
            WeatherIconSlot.DRIZZLE to R.drawable.wi_sprinkle,
            WeatherIconSlot.RAIN to R.drawable.wi_rain,
            WeatherIconSlot.HEAVY_RAIN to R.drawable.wi_rain_wind,
            WeatherIconSlot.SLEET to R.drawable.wi_sleet,
            WeatherIconSlot.SNOW to R.drawable.wi_snow,
            WeatherIconSlot.HAIL to R.drawable.wi_hail,
            WeatherIconSlot.THUNDERSTORM to R.drawable.wi_thunderstorm,
        ),
    ),
    FLUENT_3D(
        "fluent_3d",
        R.string.weather_icon_style_fluent_3d,
        false,
        mapOf(
            WeatherIconSlot.CLEAR_DAY to R.drawable.fluent3d_clear_day,
            WeatherIconSlot.CLEAR_NIGHT to R.drawable.fluent3d_clear_night,
            WeatherIconSlot.PARTLY_CLOUDY_DAY to R.drawable.fluent3d_partly_cloudy_day,
            WeatherIconSlot.PARTLY_CLOUDY_NIGHT to R.drawable.fluent3d_partly_cloudy_night,
            WeatherIconSlot.CLOUDY to R.drawable.fluent3d_cloudy,
            WeatherIconSlot.FOG to R.drawable.fluent3d_fog,
            WeatherIconSlot.DRIZZLE to R.drawable.fluent3d_drizzle,
            WeatherIconSlot.RAIN to R.drawable.fluent3d_rain,
            WeatherIconSlot.HEAVY_RAIN to R.drawable.fluent3d_heavy_rain,
            WeatherIconSlot.SLEET to R.drawable.fluent3d_sleet,
            WeatherIconSlot.SNOW to R.drawable.fluent3d_snow,
            WeatherIconSlot.HAIL to R.drawable.fluent3d_hail,
            WeatherIconSlot.THUNDERSTORM to R.drawable.fluent3d_thunderstorm,
        ),
    ),
    NOTO_3D(
        "noto_3d",
        R.string.weather_icon_style_noto_3d,
        false,
        mapOf(
            WeatherIconSlot.CLEAR_DAY to R.drawable.noto3d_clear_day,
            WeatherIconSlot.CLEAR_NIGHT to R.drawable.noto3d_clear_night,
            WeatherIconSlot.PARTLY_CLOUDY_DAY to R.drawable.noto3d_partly_cloudy_day,
            WeatherIconSlot.PARTLY_CLOUDY_NIGHT to R.drawable.noto3d_partly_cloudy_night,
            WeatherIconSlot.CLOUDY to R.drawable.noto3d_cloudy,
            WeatherIconSlot.FOG to R.drawable.noto3d_fog,
            WeatherIconSlot.DRIZZLE to R.drawable.noto3d_drizzle,
            WeatherIconSlot.RAIN to R.drawable.noto3d_rain,
            WeatherIconSlot.HEAVY_RAIN to R.drawable.noto3d_heavy_rain,
            WeatherIconSlot.SLEET to R.drawable.noto3d_sleet,
            WeatherIconSlot.SNOW to R.drawable.noto3d_snow,
            WeatherIconSlot.HAIL to R.drawable.noto3d_hail,
            WeatherIconSlot.THUNDERSTORM to R.drawable.noto3d_thunderstorm,
        ),
    ),
    PHOSPHOR(
        "phosphor",
        R.string.weather_icon_style_phosphor,
        true,
        mapOf(
            WeatherIconSlot.CLEAR_DAY to R.drawable.ph_clear_day,
            WeatherIconSlot.CLEAR_NIGHT to R.drawable.ph_clear_night,
            WeatherIconSlot.PARTLY_CLOUDY_DAY to R.drawable.ph_partly_cloudy_day,
            WeatherIconSlot.PARTLY_CLOUDY_NIGHT to R.drawable.ph_partly_cloudy_night,
            WeatherIconSlot.CLOUDY to R.drawable.ph_cloudy,
            WeatherIconSlot.FOG to R.drawable.ph_fog,
            WeatherIconSlot.DRIZZLE to R.drawable.ph_drizzle,
            WeatherIconSlot.RAIN to R.drawable.ph_rain,
            WeatherIconSlot.HEAVY_RAIN to R.drawable.ph_heavy_rain,
            WeatherIconSlot.SLEET to R.drawable.ph_sleet,
            WeatherIconSlot.SNOW to R.drawable.ph_snow,
            WeatherIconSlot.HAIL to R.drawable.ph_hail,
            WeatherIconSlot.THUNDERSTORM to R.drawable.ph_thunderstorm,
        ),
    ),
    TABLER(
        "tabler",
        R.string.weather_icon_style_tabler,
        true,
        mapOf(
            WeatherIconSlot.CLEAR_DAY to R.drawable.tb_clear_day,
            WeatherIconSlot.CLEAR_NIGHT to R.drawable.tb_clear_night,
            WeatherIconSlot.PARTLY_CLOUDY_DAY to R.drawable.tb_partly_cloudy_day,
            WeatherIconSlot.PARTLY_CLOUDY_NIGHT to R.drawable.tb_partly_cloudy_night,
            WeatherIconSlot.CLOUDY to R.drawable.tb_cloudy,
            WeatherIconSlot.FOG to R.drawable.tb_fog,
            WeatherIconSlot.DRIZZLE to R.drawable.tb_drizzle,
            WeatherIconSlot.RAIN to R.drawable.tb_rain,
            WeatherIconSlot.HEAVY_RAIN to R.drawable.tb_heavy_rain,
            WeatherIconSlot.SLEET to R.drawable.tb_sleet,
            WeatherIconSlot.SNOW to R.drawable.tb_snow,
            WeatherIconSlot.HAIL to R.drawable.tb_hail,
            WeatherIconSlot.THUNDERSTORM to R.drawable.tb_thunderstorm,
        ),
    ),
    ;

    @DrawableRes
    fun icon(slot: WeatherIconSlot): Int = icons.getValue(slot)

    @DrawableRes
    fun icon(condition: WeatherCondition, isDay: Boolean): Int = icon(WeatherIconSlot.of(condition, isDay))

    companion object {
        val Default = ROUNDED

        fun fromId(id: String?): WeatherIconStyle = entries.firstOrNull { it.id == id } ?: Default
    }
}

// The small glyphs on the detail tiles. Packs without a dedicated glyph reuse a neighbouring weather icon.
enum class WeatherDetailSlot {
    HUMIDITY,
    WIND,
    GUSTS,
    RAIN_CHANCE,
    PRECIPITATION,
    UV,
    PRESSURE,
    VISIBILITY,
    DEW_POINT,
    CLOUD_COVER,
}

@DrawableRes
fun WeatherIconStyle.detailIcon(slot: WeatherDetailSlot): Int = when (this) {
        WeatherIconStyle.ROUNDED -> when (slot) {
            WeatherDetailSlot.HUMIDITY -> R.drawable.rounded_water_drop_24
            WeatherDetailSlot.WIND -> R.drawable.rounded_air_24
            WeatherDetailSlot.GUSTS -> R.drawable.rounded_air_24
            WeatherDetailSlot.RAIN_CHANCE -> R.drawable.rounded_rainy_24
            WeatherDetailSlot.PRECIPITATION -> R.drawable.rounded_rainy_24
            WeatherDetailSlot.UV -> R.drawable.rounded_wb_sunny_24
            WeatherDetailSlot.PRESSURE -> R.drawable.rounded_cloud_24
            WeatherDetailSlot.VISIBILITY -> R.drawable.rounded_visibility_24
            WeatherDetailSlot.DEW_POINT -> R.drawable.rounded_water_drop_24
            WeatherDetailSlot.CLOUD_COVER -> R.drawable.rounded_cloud_24
        }
        WeatherIconStyle.FILLED -> when (slot) {
            WeatherDetailSlot.HUMIDITY -> R.drawable.filled_water_drop_24
            WeatherDetailSlot.WIND -> R.drawable.filled_air_24
            WeatherDetailSlot.GUSTS -> R.drawable.filled_air_24
            WeatherDetailSlot.RAIN_CHANCE -> R.drawable.filled_rainy_24
            WeatherDetailSlot.PRECIPITATION -> R.drawable.filled_rainy_24
            WeatherDetailSlot.UV -> R.drawable.filled_wb_sunny_24
            WeatherDetailSlot.PRESSURE -> R.drawable.filled_cloud_24
            WeatherDetailSlot.VISIBILITY -> R.drawable.filled_visibility_24
            WeatherDetailSlot.DEW_POINT -> R.drawable.filled_water_drop_24
            WeatherDetailSlot.CLOUD_COVER -> R.drawable.filled_cloud_24
        }
        WeatherIconStyle.METEOCONS -> when (slot) {
            WeatherDetailSlot.HUMIDITY -> R.drawable.meteocons_d_humidity
            WeatherDetailSlot.WIND -> R.drawable.meteocons_d_wind
            WeatherDetailSlot.GUSTS -> R.drawable.meteocons_d_gusts
            WeatherDetailSlot.RAIN_CHANCE -> R.drawable.meteocons_d_rain_chance
            WeatherDetailSlot.PRECIPITATION -> R.drawable.meteocons_d_precipitation
            WeatherDetailSlot.UV -> R.drawable.meteocons_d_uv
            WeatherDetailSlot.PRESSURE -> R.drawable.meteocons_d_pressure
            WeatherDetailSlot.VISIBILITY -> R.drawable.meteocons_d_visibility
            WeatherDetailSlot.DEW_POINT -> R.drawable.meteocons_d_dew_point
            WeatherDetailSlot.CLOUD_COVER -> R.drawable.meteocons_cloudy
        }
        WeatherIconStyle.WEATHER_ICONS -> when (slot) {
            WeatherDetailSlot.HUMIDITY -> R.drawable.wi_d_humidity
            WeatherDetailSlot.WIND -> R.drawable.wi_d_wind
            WeatherDetailSlot.GUSTS -> R.drawable.wi_d_gusts
            WeatherDetailSlot.RAIN_CHANCE -> R.drawable.wi_d_rain_chance
            WeatherDetailSlot.PRECIPITATION -> R.drawable.wi_d_precipitation
            WeatherDetailSlot.UV -> R.drawable.wi_d_uv
            WeatherDetailSlot.PRESSURE -> R.drawable.wi_d_pressure
            WeatherDetailSlot.VISIBILITY -> R.drawable.wi_d_visibility
            WeatherDetailSlot.DEW_POINT -> R.drawable.wi_d_dew_point
            WeatherDetailSlot.CLOUD_COVER -> R.drawable.wi_d_cloud_cover
        }
        WeatherIconStyle.FLUENT_3D -> when (slot) {
            WeatherDetailSlot.HUMIDITY -> R.drawable.fluent3d_d_humidity
            WeatherDetailSlot.WIND -> R.drawable.fluent3d_d_wind
            WeatherDetailSlot.GUSTS -> R.drawable.fluent3d_d_gusts
            WeatherDetailSlot.RAIN_CHANCE -> R.drawable.fluent3d_heavy_rain
            WeatherDetailSlot.PRECIPITATION -> R.drawable.fluent3d_rain
            WeatherDetailSlot.UV -> R.drawable.fluent3d_clear_day
            WeatherDetailSlot.PRESSURE -> R.drawable.fluent3d_d_pressure
            WeatherDetailSlot.VISIBILITY -> R.drawable.fluent3d_d_visibility
            WeatherDetailSlot.DEW_POINT -> R.drawable.fluent3d_d_dew_point
            WeatherDetailSlot.CLOUD_COVER -> R.drawable.fluent3d_cloudy
        }
        WeatherIconStyle.NOTO_3D -> when (slot) {
            WeatherDetailSlot.HUMIDITY -> R.drawable.noto3d_d_humidity
            WeatherDetailSlot.WIND -> R.drawable.noto3d_d_wind
            WeatherDetailSlot.GUSTS -> R.drawable.noto3d_d_gusts
            WeatherDetailSlot.RAIN_CHANCE -> R.drawable.noto3d_heavy_rain
            WeatherDetailSlot.PRECIPITATION -> R.drawable.noto3d_rain
            WeatherDetailSlot.UV -> R.drawable.noto3d_clear_day
            WeatherDetailSlot.PRESSURE -> R.drawable.noto3d_d_pressure
            WeatherDetailSlot.VISIBILITY -> R.drawable.noto3d_d_visibility
            WeatherDetailSlot.DEW_POINT -> R.drawable.noto3d_d_dew_point
            WeatherDetailSlot.CLOUD_COVER -> R.drawable.noto3d_cloudy
        }
        WeatherIconStyle.PHOSPHOR -> when (slot) {
            WeatherDetailSlot.HUMIDITY -> R.drawable.ph_d_humidity
            WeatherDetailSlot.WIND -> R.drawable.ph_d_wind
            WeatherDetailSlot.GUSTS -> R.drawable.ph_d_wind
            WeatherDetailSlot.RAIN_CHANCE -> R.drawable.ph_heavy_rain
            WeatherDetailSlot.PRECIPITATION -> R.drawable.ph_rain
            WeatherDetailSlot.UV -> R.drawable.ph_clear_day
            WeatherDetailSlot.PRESSURE -> R.drawable.ph_d_pressure
            WeatherDetailSlot.VISIBILITY -> R.drawable.ph_d_visibility
            WeatherDetailSlot.DEW_POINT -> R.drawable.ph_d_dew_point
            WeatherDetailSlot.CLOUD_COVER -> R.drawable.ph_cloudy
        }
        WeatherIconStyle.TABLER -> when (slot) {
            WeatherDetailSlot.HUMIDITY -> R.drawable.tb_d_humidity
            WeatherDetailSlot.WIND -> R.drawable.tb_d_wind
            WeatherDetailSlot.GUSTS -> R.drawable.tb_d_wind
            WeatherDetailSlot.RAIN_CHANCE -> R.drawable.tb_heavy_rain
            WeatherDetailSlot.PRECIPITATION -> R.drawable.tb_rain
            WeatherDetailSlot.UV -> R.drawable.tb_clear_day
            WeatherDetailSlot.PRESSURE -> R.drawable.tb_d_pressure
            WeatherDetailSlot.VISIBILITY -> R.drawable.tb_d_visibility
            WeatherDetailSlot.DEW_POINT -> R.drawable.tb_d_dew_point
            WeatherDetailSlot.CLOUD_COVER -> R.drawable.tb_cloudy
        }
}

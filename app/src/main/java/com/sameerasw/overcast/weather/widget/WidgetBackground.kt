package com.sameerasw.overcast.weather.widget

import android.content.Context
import androidx.annotation.StringRes
import com.sameerasw.overcast.R

enum class WidgetBackground(val id: String, @StringRes val labelRes: Int) {
    NONE("none", R.string.widget_bg_none),
    AMBIENT("ambient", R.string.widget_bg_ambient),
    MATERIAL("material", R.string.widget_bg_material),
    ;

    companion object {
        fun fromId(id: String?): WidgetBackground = entries.firstOrNull { it.id == id } ?: NONE
    }
}

enum class WidgetForecast(val id: String, @StringRes val labelRes: Int) {
    OFF("off", R.string.widget_forecast_off),
    HOURLY("hourly", R.string.widget_forecast_hourly),
    DAILY("daily", R.string.widget_forecast_daily),
    ;

    companion object {
        fun fromId(id: String?): WidgetForecast = entries.firstOrNull { it.id == id } ?: OFF
    }
}

// Each placed widget remembers its own background and forecast row.
class WidgetConfigStore(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("overcast_widget_prefs", Context.MODE_PRIVATE)

    fun background(appWidgetId: Int): WidgetBackground = WidgetBackground.fromId(prefs.getString(key(appWidgetId), null))

    fun setBackground(appWidgetId: Int, background: WidgetBackground) {
        prefs.edit().putString(key(appWidgetId), background.id).apply()
    }

    fun forecast(appWidgetId: Int): WidgetForecast = WidgetForecast.fromId(prefs.getString("forecast_$appWidgetId", null))

    fun setForecast(appWidgetId: Int, forecast: WidgetForecast) {
        prefs.edit().putString("forecast_$appWidgetId", forecast.id).apply()
    }

    
    fun spacing(appWidgetId: Int): Float? = prefs.getFloat("spacing_$appWidgetId", Float.NaN).takeUnless { it.isNaN() }

    fun setSpacing(appWidgetId: Int, spacing: Float) {
        prefs.edit().putFloat("spacing_$appWidgetId", spacing).apply()
    }

    fun remove(appWidgetId: Int) {
        prefs.edit().remove(key(appWidgetId)).remove("forecast_$appWidgetId").remove("spacing_$appWidgetId").apply()
    }

    private fun key(appWidgetId: Int) = "background_$appWidgetId"
}

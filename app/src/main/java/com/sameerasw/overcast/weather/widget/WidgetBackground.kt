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

// Each placed widget remembers its own background.
class WidgetConfigStore(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("overcast_widget_prefs", Context.MODE_PRIVATE)

    fun background(appWidgetId: Int): WidgetBackground = WidgetBackground.fromId(prefs.getString(key(appWidgetId), null))

    fun setBackground(appWidgetId: Int, background: WidgetBackground) {
        prefs.edit().putString(key(appWidgetId), background.id).apply()
    }

    fun remove(appWidgetId: Int) {
        prefs.edit().remove(key(appWidgetId)).apply()
    }

    private fun key(appWidgetId: Int) = "background_$appWidgetId"
}

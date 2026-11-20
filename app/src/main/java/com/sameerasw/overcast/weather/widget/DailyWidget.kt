package com.sameerasw.overcast.weather.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.LocalContext
import androidx.glance.LocalGlanceId
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.unit.ColorProvider
import com.sameerasw.overcast.R
import com.sameerasw.overcast.data.repository.SettingsRepository
import com.sameerasw.overcast.weather.WeatherIconSlot
import com.sameerasw.overcast.weather.WeatherIconStyle
import com.sameerasw.overcast.weather.WeatherRepository
import kotlinx.coroutines.withTimeoutOrNull

class DailyWidget : GlanceAppWidget() {
    // The visible days are worked out from the live width, so widening the widget reveals more of them.
    override val sizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        if (WeatherRepository.isStale(context)) {
            withTimeoutOrNull(4000L) {
                runCatching { WeatherRepository.refresh(context, fallbackToSaved = true) }
            }
        }
        WidgetHub.refresh(context)
        provideContent {
            GlanceTheme {
                DailyRoot()
            }
        }
    }

    // Android 15+ asks for this when it shows the widget picker.
    override suspend fun providePreview(context: Context, widgetCategory: Int) {
        val style = WeatherIconStyle.fromId(SettingsRepository(context).getWeatherIconStyle())
        val sample = listOf(
            ForecastItem(context.getString(R.string.weather_detail_today), style.icon(WeatherIconSlot.PARTLY_CLOUDY_DAY), "23°", "15°"),
            ForecastItem("Thu", style.icon(WeatherIconSlot.CLEAR_DAY), "24°", "16°"),
            ForecastItem("Fri", style.icon(WeatherIconSlot.CLOUDY), "21°", "14°"),
            ForecastItem("Sat", style.icon(WeatherIconSlot.RAIN), "18°", "12°"),
            ForecastItem("Sun", style.icon(WeatherIconSlot.PARTLY_CLOUDY_DAY), "20°", "13°"),
        )
        provideContent {
            GlanceTheme {
                ForecastWidgetContent(
                    items = sample,
                    tintable = style.tintable,
                    emptyText = "",
                    mode = WidgetBackground.NONE,
                    weather = null,
                    background = GlanceModifier.background(ColorProvider(Color(0xFF14609F))).cornerRadius(WIDGET_CORNER),
                )
            }
        }
    }
}

class DailyWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = DailyWidget()

    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        WidgetAlarmScheduler.scheduleNext(context)
    }

    override fun onDisabled(context: Context) {
        super.onDisabled(context)
        if (!hasActiveWidgets(context)) {
            WidgetAlarmScheduler.cancel(context)
        }
    }

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        super.onDeleted(context, appWidgetIds)
        val store = WidgetConfigStore(context)
        appWidgetIds.forEach { store.remove(it) }
        if (!hasActiveWidgets(context)) {
            WidgetAlarmScheduler.cancel(context)
        }
    }
}

@Composable
private fun DailyRoot() {
    val context = LocalContext.current
    val glanceId = LocalGlanceId.current
    val weather by WidgetHub.weather.collectAsState()
    val revision by WidgetHub.revision.collectAsState()
    val mode = remember(revision) { WidgetConfigStore(context).background(GlanceAppWidgetManager(context).getAppWidgetId(glanceId)) }
    val spacing = remember(revision, mode) { resolveSpacing(context, GlanceAppWidgetManager(context).getAppWidgetId(glanceId), mode) }
    val current = weather ?: return
    val items = remember(current) { dailyItems(context, current) }
    ForecastWidgetContent(items, current.style.tintable, context.getString(R.string.widget_weather_empty), mode, current, spacing)
}

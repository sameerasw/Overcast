package com.sameerasw.overcast.weather.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.LocalGlanceId
import androidx.glance.LocalSize
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.padding
import androidx.glance.unit.ColorProvider
import com.sameerasw.overcast.R
import com.sameerasw.overcast.data.repository.SettingsRepository
import com.sameerasw.overcast.weather.WeatherIconSlot
import com.sameerasw.overcast.weather.WeatherIconStyle

class HourlyWidget : GlanceAppWidget() {
    // The visible hours are worked out from the live width, so widening the widget reveals more of them.
    override val sizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        WidgetHub.refresh(context)
        provideContent {
            GlanceTheme {
                HourlyRoot()
            }
        }
    }

    // Android 15+ asks for this when it shows the widget picker.
    override suspend fun providePreview(context: Context, widgetCategory: Int) {
        val style = WeatherIconStyle.fromId(SettingsRepository(context).getWeatherIconStyle())
        val sample = listOf(
            Triple(context.getString(R.string.widget_hourly_now), "21°", WeatherIconSlot.PARTLY_CLOUDY_DAY),
            Triple("1pm", "22°", WeatherIconSlot.CLEAR_DAY),
            Triple("2pm", "23°", WeatherIconSlot.CLEAR_DAY),
            Triple("3pm", "22°", WeatherIconSlot.CLOUDY),
            Triple("4pm", "20°", WeatherIconSlot.RAIN),
            Triple("5pm", "19°", WeatherIconSlot.RAIN),
        ).map { ForecastItem(it.first, style.icon(it.third), it.second) }
        provideContent {
            GlanceTheme {
                HourlyContent(
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

class HourlyWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = HourlyWidget()

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        super.onDeleted(context, appWidgetIds)
        val store = WidgetConfigStore(context)
        appWidgetIds.forEach { store.remove(it) }
    }
}

@Composable
private fun HourlyRoot() {
    val context = LocalContext.current
    val glanceId = LocalGlanceId.current
    val weather by WidgetHub.weather.collectAsState()
    val revision by WidgetHub.revision.collectAsState()
    val mode = remember(revision) { WidgetConfigStore(context).background(GlanceAppWidgetManager(context).getAppWidgetId(glanceId)) }
    val spacing = remember(revision, mode) { resolveSpacing(context, GlanceAppWidgetManager(context).getAppWidgetId(glanceId), mode) }
    val current = weather ?: return
    val items = remember(current) { hourlyItems(context, current) }
    HourlyContent(items, current.style.tintable, context.getString(R.string.widget_weather_empty), mode, current, spacing)
}

@Composable
private fun HourlyContent(
    items: List<ForecastItem>,
    tintable: Boolean,
    emptyText: String,
    mode: WidgetBackground,
    weather: WidgetWeather?,
    spacing: Dp = widgetPadding(mode),
    background: GlanceModifier = GlanceModifier,
) {
    val context = LocalContext.current
    val size = LocalSize.current
    val density = context.resources.displayMetrics.density
    val padding = spacing
    val shadow = mode == WidgetBackground.NONE

    WidgetSurface(mode, weather, background) {
        if (items.isEmpty()) {
            val textArgb = widgetTextArgb(mode)
            val message = remember(emptyText, textArgb, shadow) {
                WidgetTemperatureRenderer.renderText(
                    context, emptyText.ifEmpty { " " }, 14f * density * context.resources.configuration.fontScale,
                    (size.width.value * density).toInt(), textArgb, shadow = shadow,
                )
            }
            Column(
                modifier = GlanceModifier.fillMaxSize().padding(padding),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Image(provider = ImageProvider(message), contentDescription = emptyText)
            }
        } else {
            ForecastRow(
                items = items,
                tintable = tintable,
                mode = mode,
                width = size.width - padding * 2,
                height = size.height - padding * 2,
                gap = spacing * 0.35f,
                modifier = GlanceModifier.fillMaxSize().padding(padding),
            )
        }
    }
}

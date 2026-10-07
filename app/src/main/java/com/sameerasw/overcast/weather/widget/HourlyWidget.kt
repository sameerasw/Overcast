package com.sameerasw.overcast.weather.widget

import android.content.Context
import android.text.format.DateFormat
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.glance.ColorFilter
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
import androidx.glance.layout.ContentScale
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxHeight
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.unit.ColorProvider
import com.sameerasw.overcast.R
import com.sameerasw.overcast.data.repository.SettingsRepository
import com.sameerasw.overcast.weather.WeatherFormat
import com.sameerasw.overcast.weather.WeatherIconSlot
import com.sameerasw.overcast.weather.WeatherIconStyle
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs

private const val MIN_ITEM_DP = 52f
private const val MAX_HOURS = 14
private const val HOUR_MS = 60 * 60_000L

private class HourData(val time: String, val temperature: String, val icon: Int)

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
        ).map { HourData(it.first, it.second, style.icon(it.third)) }
        provideContent {
            GlanceTheme {
                HourlyContent(
                    hours = sample,
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

private fun formatHour(context: Context, millis: Long): String {
    val pattern = if (DateFormat.is24HourFormat(context)) "HH:mm" else "ha"
    return SimpleDateFormat(pattern, Locale.getDefault()).format(Date(millis)).lowercase(Locale.getDefault())
}

@Composable
private fun HourlyRoot() {
    val context = LocalContext.current
    val glanceId = LocalGlanceId.current
    val weather by WidgetHub.weather.collectAsState()
    val revision by WidgetHub.revision.collectAsState()
    val mode = remember(revision) { WidgetConfigStore(context).background(GlanceAppWidgetManager(context).getAppWidgetId(glanceId)) }
    val current = weather ?: return
    val hours = remember(current) {
        val now = System.currentTimeMillis()
        current.snapshot?.hourly.orEmpty()
            .filter { it.timeMillis > now - HOUR_MS }
            .take(MAX_HOURS)
            .map {
                HourData(
                    time = if (abs(it.timeMillis - now) < HOUR_MS * 3 / 4) context.getString(R.string.widget_hourly_now) else formatHour(context, it.timeMillis),
                    temperature = WeatherFormat.temperature(it.tempC, current.units.temperature),
                    icon = current.style.icon(it.condition, it.isDay),
                )
            }
    }
    HourlyContent(hours, current.style.tintable, context.getString(R.string.widget_weather_empty), mode, current)
}

@Composable
private fun HourlyContent(
    hours: List<HourData>,
    tintable: Boolean,
    emptyText: String,
    mode: WidgetBackground,
    weather: WidgetWeather?,
    background: GlanceModifier = GlanceModifier,
) {
    val context = LocalContext.current
    val size = LocalSize.current
    val resources = context.resources
    val density = resources.displayMetrics.density
    val fontScale = resources.configuration.fontScale
    val textColor = widgetTextColor(mode)
    val textArgb = widgetTextArgb(mode)
    
    val shadow = mode == WidgetBackground.NONE
    val padding = widgetPadding(mode)

    if (hours.isEmpty()) {
        val message = remember(emptyText, textArgb, shadow) {
            WidgetTemperatureRenderer.renderText(context, emptyText.ifEmpty { " " }, 14f * density * fontScale, (size.width.value * density).toInt(), textArgb, shadow = shadow)
        }
        WidgetSurface(mode, weather, background) {
            Column(
                modifier = GlanceModifier.fillMaxSize().padding(padding),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Image(provider = ImageProvider(message), contentDescription = emptyText)
            }
        }
        return
    }

    val available = size.width - padding * 2
    val count = (available.value / MIN_ITEM_DP).toInt().coerceIn(1, hours.size)
    val itemWidth = available / count
    val iconSize = (size.height * 0.28f).coerceIn(20.dp, 40.dp)
    val tempHeight = (size.height * 0.22f).coerceIn(18.dp, 36.dp)
    val timeSp = (size.height.value * 0.12f).coerceIn(10f, 14f)
    val tempWidthPx = (itemWidth.value * 0.8f * density).toInt()
    val tempHeightPx = (tempHeight.value * density).toInt()
    val timeWidthPx = (itemWidth.value * density).toInt()

    WidgetSurface(mode, weather, background) {
        Row(
            modifier = GlanceModifier.fillMaxSize().padding(padding),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            hours.take(count).forEach { hour ->
                val tempBitmap = remember(hour.temperature, tempWidthPx, tempHeightPx, textArgb, shadow) {
                    WidgetTemperatureRenderer.render(context, hour.temperature, tempWidthPx, tempHeightPx, textArgb, widthAxis = 60, weightAxis = 300, shadow = shadow)
                }
                val timeBitmap = remember(hour.time, timeSp, timeWidthPx, textArgb, shadow) {
                    WidgetTemperatureRenderer.renderText(context, hour.time, timeSp * density * fontScale, timeWidthPx, textArgb, shadow = shadow)
                }
                Column(
                    modifier = GlanceModifier.defaultWeight().fillMaxHeight(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Image(provider = ImageProvider(timeBitmap), contentDescription = hour.time)
                    Spacer(GlanceModifier.height(4.dp))
                    Image(
                        provider = ImageProvider(hour.icon),
                        contentDescription = null,
                        colorFilter = if (tintable) ColorFilter.tint(textColor) else null,
                        modifier = GlanceModifier.size(iconSize),
                    )
                    Spacer(GlanceModifier.height(4.dp))
                    Image(
                        provider = ImageProvider(tempBitmap),
                        contentDescription = hour.temperature,
                        contentScale = ContentScale.Fit,
                        modifier = GlanceModifier.fillMaxWidth().height(tempHeight),
                    )
                }
            }
        }
    }
}

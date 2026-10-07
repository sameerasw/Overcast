package com.sameerasw.overcast.weather.widget

import android.content.Context
import android.content.Intent
import android.text.format.DateFormat
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.ColorFilter
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.LocalSize
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
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
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.sameerasw.overcast.R
import com.sameerasw.overcast.data.repository.SettingsRepository
import com.sameerasw.overcast.ui.activities.WeatherActivity
import com.sameerasw.overcast.weather.WeatherFormat
import com.sameerasw.overcast.weather.WeatherIconSlot
import com.sameerasw.overcast.weather.WeatherIconStyle
import com.sameerasw.overcast.weather.WeatherRepository
import com.sameerasw.overcast.weather.WeatherUnits
import com.sameerasw.overcast.weather.model.WeatherCondition
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
        WeatherRepository.ensureLoaded(context)
        val settings = SettingsRepository(context)
        val units = WeatherUnits.from(settings)
        val style = WeatherIconStyle.fromId(settings.getWeatherIconStyle())
        val snapshot = WeatherRepository.state.value.snapshot
        val now = System.currentTimeMillis()
        val hours = snapshot?.hourly.orEmpty()
            .filter { it.timeMillis > now - HOUR_MS }
            .take(MAX_HOURS)
            .map {
                HourData(
                    time = if (abs(it.timeMillis - now) < HOUR_MS * 3 / 4) context.getString(R.string.widget_hourly_now) else formatHour(context, it.timeMillis),
                    temperature = WeatherFormat.temperature(it.tempC, units.temperature),
                    icon = style.icon(it.condition, it.isDay),
                )
            }
        provideContent {
            HourlyContent(hours, style.tintable, context.getString(R.string.widget_weather_empty))
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
            HourlyContent(
                hours = sample,
                tintable = style.tintable,
                emptyText = "",
                background = GlanceModifier.background(ColorProvider(Color(0xFF14609F))).cornerRadius(28.dp),
            )
        }
    }
}

class HourlyWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = HourlyWidget()
}

private fun formatHour(context: Context, millis: Long): String {
    val pattern = if (DateFormat.is24HourFormat(context)) "HH:mm" else "ha"
    return SimpleDateFormat(pattern, Locale.getDefault()).format(Date(millis)).lowercase(Locale.getDefault())
}

@Composable
private fun HourlyContent(
    hours: List<HourData>,
    tintable: Boolean,
    emptyText: String,
    background: GlanceModifier = GlanceModifier,
) {
    val context = LocalContext.current
    val size = LocalSize.current
    val density = context.resources.displayMetrics.density
    val white = ColorProvider(Color.White)
    val padding = 8.dp
    val open = actionStartActivity(Intent(context, WeatherActivity::class.java))

    if (hours.isEmpty()) {
        Box(
            modifier = background.fillMaxSize().padding(padding).clickable(open),
            contentAlignment = Alignment.Center,
        ) {
            Text(emptyText, style = TextStyle(color = white, fontSize = 14.sp, fontWeight = FontWeight.Medium, textAlign = TextAlign.Center))
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

    Row(
        modifier = background.fillMaxSize().padding(padding).clickable(open),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        hours.take(count).forEach { hour ->
            val bitmap = remember(hour.temperature, tempWidthPx, tempHeightPx) {
                WidgetTemperatureRenderer.render(context, hour.temperature, tempWidthPx, tempHeightPx, android.graphics.Color.WHITE, widthAxis = 60, weightAxis = 300)
            }
            Column(
                modifier = GlanceModifier.defaultWeight().fillMaxHeight(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = hour.time,
                    maxLines = 1,
                    style = TextStyle(color = white, fontSize = timeSp.sp, fontWeight = FontWeight.Medium, textAlign = TextAlign.Center),
                )
                Spacer(GlanceModifier.height(4.dp))
                Image(
                    provider = ImageProvider(hour.icon),
                    contentDescription = null,
                    colorFilter = if (tintable) ColorFilter.tint(white) else null,
                    modifier = GlanceModifier.size(iconSize),
                )
                Spacer(GlanceModifier.height(4.dp))
                Image(
                    provider = ImageProvider(bitmap),
                    contentDescription = hour.temperature,
                    contentScale = ContentScale.Fit,
                    modifier = GlanceModifier.fillMaxWidth().height(tempHeight),
                )
            }
        }
    }
}

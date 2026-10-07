package com.sameerasw.overcast.weather.widget

import android.content.Context
import android.os.Build
import androidx.compose.runtime.Composable
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
import androidx.glance.LocalSize
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.updateAll
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.ContentScale
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.unit.ColorProvider
import com.sameerasw.overcast.R
import com.sameerasw.overcast.data.repository.SettingsRepository
import com.sameerasw.overcast.weather.WeatherFormat
import com.sameerasw.overcast.weather.WeatherIconSlot
import com.sameerasw.overcast.weather.WeatherIconStyle
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class WeatherWidget : GlanceAppWidget() {
    // Exact gives us the real size on every resize, so the temperature is redrawn to fit rather than picked from presets.
    override val sizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val weather = loadWidgetWeather(context)
        val snapshot = weather.snapshot
        val temperature = snapshot?.let { WeatherFormat.temperature(it.tempC, weather.units.temperature) } ?: "--°"
        val subtitle = snapshot?.conditionText ?: context.getString(R.string.widget_weather_empty)
        val icon = snapshot?.let { weather.style.icon(it.condition, it.isDay) } ?: weather.style.icon(WeatherIconSlot.CLOUDY)
        val mode = WidgetConfigStore(context).background(GlanceAppWidgetManager(context).getAppWidgetId(id))
        provideContent {
            GlanceTheme {
                WeatherWidgetContent(temperature, subtitle, icon, weather.style.tintable, mode, weather)
            }
        }
    }

    // Android 15+ asks for this when it shows the widget picker; it follows the chosen icon style.
    override suspend fun providePreview(context: Context, widgetCategory: Int) {
        val style = WeatherIconStyle.fromId(SettingsRepository(context).getWeatherIconStyle())
        val icon = style.icon(WeatherIconSlot.PARTLY_CLOUDY_DAY)
        provideContent {
            GlanceTheme {
                WeatherWidgetContent(
                    temperature = "21°",
                    subtitle = "Partly cloudy",
                    icon = icon,
                    tintable = style.tintable,
                    mode = WidgetBackground.NONE,
                    weather = null,
                    background = GlanceModifier.background(ColorProvider(Color(0xFF14609F))).cornerRadius(WIDGET_CORNER),
                )
            }
        }
    }
}

class WeatherWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = WeatherWidget()

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        super.onDeleted(context, appWidgetIds)
        val store = WidgetConfigStore(context)
        appWidgetIds.forEach { store.remove(it) }
    }
}

@Composable
private fun WeatherWidgetContent(
    temperature: String,
    subtitle: String,
    icon: Int,
    tintable: Boolean,
    mode: WidgetBackground,
    weather: WidgetWeather?,
    background: GlanceModifier = GlanceModifier,
) {
    val context = LocalContext.current
    val size = LocalSize.current
    val resources = context.resources
    val density = resources.displayMetrics.density
    val textColor = widgetTextColor(mode)
    val textArgb = widgetTextArgb(mode)
    val shadow = mode != WidgetBackground.MATERIAL
    val padding = widgetPadding(mode)
    val showSubtitle = size.height >= 72.dp
    val subtitleHeight = if (showSubtitle) (size.height * 0.2f).coerceIn(24.dp, 34.dp) else 0.dp
    val iconSize = subtitleHeight * 0.7f
    val tempWidth = ((size.width - padding * 2).value * density).toInt()
    val tempHeight = ((size.height - padding * 2 - subtitleHeight).value * density).toInt()
    val bitmap = remember(temperature, tempWidth, tempHeight, textArgb, shadow) {
        WidgetTemperatureRenderer.render(context, temperature, tempWidth, tempHeight, textArgb, shadow = shadow)
    }
    val subtitleSp = (subtitleHeight.value * 0.5f).coerceIn(11f, 16f)
    val subtitleMaxWidth = ((size.width - padding * 2 - iconSize - 6.dp).value * density).toInt()
    val subtitleBitmap = remember(subtitle, subtitleSp, subtitleMaxWidth, textArgb, shadow) {
        WidgetTemperatureRenderer.renderText(
            context, subtitle, subtitleSp * density * resources.configuration.fontScale, subtitleMaxWidth, textArgb, shadow = shadow,
        )
    }
    WidgetSurface(mode, weather, background) {
        Column(
            modifier = GlanceModifier.fillMaxSize().padding(padding),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Image(
                provider = ImageProvider(bitmap),
                contentDescription = temperature,
                contentScale = ContentScale.Fit,
                modifier = GlanceModifier.fillMaxWidth().defaultWeight(),
            )
            if (showSubtitle) {
                Row(
                    modifier = GlanceModifier.fillMaxWidth().height(subtitleHeight),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Image(
                        provider = ImageProvider(icon),
                        contentDescription = null,
                        colorFilter = if (tintable) ColorFilter.tint(textColor) else null,
                        modifier = GlanceModifier.size(iconSize),
                    )
                    Spacer(GlanceModifier.width(6.dp))
                    Image(
                        provider = ImageProvider(subtitleBitmap),
                        contentDescription = subtitle,
                    )
                }
            }
        }
    }
}

object WeatherWidgetUpdater {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    // Cheap to call from anywhere; does nothing when no widget is placed.
    fun updateAll(context: Context) {
        val app = context.applicationContext
        scope.launch {
            runCatching { WeatherWidget().updateAll(app) }
            runCatching { HourlyWidget().updateAll(app) }
        }
    }

    // Generated previews only exist on Android 15+, and the system rate-limits how often they can be set.
    fun refreshPreview(context: Context) {
        if (Build.VERSION.SDK_INT < 35) return
        val app = context.applicationContext
        scope.launch {
            val manager = GlanceAppWidgetManager(app)
            runCatching { manager.setWidgetPreviews(WeatherWidgetReceiver::class) }
            runCatching { manager.setWidgetPreviews(HourlyWidgetReceiver::class) }
        }
    }
}

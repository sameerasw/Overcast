package com.sameerasw.overcast.weather.widget

import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.compose.runtime.Composable
import com.sameerasw.overcast.weather.model.WeatherSnapshot
import androidx.glance.layout.Box
import androidx.glance.GlanceTheme
import androidx.compose.ui.graphics.toArgb
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
import androidx.glance.background
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.updateAll
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
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class WeatherWidget : GlanceAppWidget() {
    // Exact gives us the real size on every resize, so the temperature is redrawn to fit rather than picked from presets.
    override val sizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        WeatherRepository.ensureLoaded(context)
        val settings = SettingsRepository(context)
        val units = WeatherUnits.from(settings)
        val style = WeatherIconStyle.fromId(settings.getWeatherIconStyle())
        val snapshot = WeatherRepository.state.value.snapshot
        val temperature = snapshot?.let { WeatherFormat.temperature(it.tempC, units.temperature) } ?: "--°"
        val subtitle = snapshot?.conditionText ?: context.getString(R.string.widget_weather_empty)
        val icon = snapshot?.let { style.icon(it.condition, it.isDay) } ?: style.icon(WeatherIconSlot.CLOUDY)
        val appWidgetId = GlanceAppWidgetManager(context).getAppWidgetId(id)
        val mode = WidgetConfigStore(context).background(appWidgetId)
        val now = System.currentTimeMillis()
        provideContent {
            GlanceTheme {
                WeatherWidgetContent(temperature, subtitle, icon, style.tintable, mode, snapshot, now)
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
                background = GlanceModifier.background(ColorProvider(Color(0xFF14609F))).cornerRadius(28.dp),
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
    mode: WidgetBackground = WidgetBackground.NONE,
    snapshot: WeatherSnapshot? = null,
    now: Long = 0L,
    background: GlanceModifier = GlanceModifier,
) {
    val context = LocalContext.current
    val size = LocalSize.current
    val density = context.resources.displayMetrics.density
    val textColor = if (mode == WidgetBackground.MATERIAL) GlanceTheme.colors.onSurface else ColorProvider(Color.White)
    val textArgb = if (mode == WidgetBackground.MATERIAL) textColor.getColor(context).toArgb() else android.graphics.Color.WHITE
    val padding = if (mode == WidgetBackground.NONE) 8.dp else 14.dp
    val corner = 28.dp
    val showSubtitle = size.height >= 72.dp
    val subtitleHeight = if (showSubtitle) (size.height * 0.2f).coerceIn(24.dp, 34.dp) else 0.dp
    val tempWidth = ((size.width - padding * 2).value * density).toInt()
    val tempHeight = ((size.height - padding * 2 - subtitleHeight).value * density).toInt()
    val bitmap = remember(temperature, tempWidth, tempHeight, textArgb) {
        WidgetTemperatureRenderer.render(context, temperature, tempWidth, tempHeight, textArgb)
    }
    val ambient = if (mode == WidgetBackground.AMBIENT) {
        remember(snapshot, now, size) {
            WidgetAmbientRenderer.render(snapshot, now, (size.width.value * density).toInt(), (size.height.value * density).toInt(), corner.value * density)
        }
    } else {
        null
    }
    val plate = if (mode == WidgetBackground.MATERIAL) {
        background.background(GlanceTheme.colors.widgetBackground).cornerRadius(corner)
    } else {
        background
    }
    Box(
        modifier = plate.fillMaxSize().clickable(actionStartActivity(Intent(context, WeatherActivity::class.java))),
    ) {
        if (ambient != null) {
            Image(
                provider = ImageProvider(ambient),
                contentDescription = null,
                contentScale = ContentScale.FillBounds,
                modifier = GlanceModifier.fillMaxSize(),
            )
        }
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
                        modifier = GlanceModifier.size(subtitleHeight * 0.7f),
                    )
                    Spacer(GlanceModifier.width(6.dp))
                    Text(
                        text = subtitle,
                        maxLines = 1,
                        style = TextStyle(
                            color = textColor,
                            fontSize = (subtitleHeight.value * 0.5f).coerceIn(11f, 16f).sp,
                            fontWeight = FontWeight.Medium,
                        ),
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

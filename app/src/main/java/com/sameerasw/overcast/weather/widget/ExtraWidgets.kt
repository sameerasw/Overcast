package com.sameerasw.overcast.weather.widget

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
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
import androidx.glance.appwidget.provideContent
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.ColorFilter
import androidx.glance.layout.ContentScale
import com.sameerasw.overcast.R
import com.sameerasw.overcast.weather.Nowcast
import com.sameerasw.overcast.weather.WeatherRepository
import com.sameerasw.overcast.weather.aqi.AqiCategory
import com.sameerasw.overcast.weather.aqi.AqiData
import com.sameerasw.overcast.weather.aqi.AqiRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.withTimeoutOrNull

private fun widgetModeAndSpacing(context: Context, glanceId: GlanceId): Pair<WidgetBackground, Dp> {
    val id = GlanceAppWidgetManager(context).getAppWidgetId(glanceId)
    val mode = WidgetConfigStore(context).background(id)
    return mode to resolveSpacing(context, id, mode)
}

private fun rainBars(slots: List<Double>, widthPx: Int, heightPx: Int): Bitmap {
    val bitmap = Bitmap.createBitmap(widthPx.coerceAtLeast(1), heightPx.coerceAtLeast(1), Bitmap.Config.ARGB_8888)
    if (slots.isEmpty()) return bitmap
    val canvas = Canvas(bitmap)
    val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    val gap = widthPx * 0.03f
    val barWidth = (widthPx - gap * (slots.size - 1)) / slots.size
    val peak = slots.max().coerceAtLeast(1.0)
    slots.forEachIndexed { index, mm ->
        val wet = mm >= Nowcast.RAIN_THRESHOLD_MM
        val barHeight = (heightPx * (mm / peak)).toFloat().coerceAtLeast(heightPx * 0.08f)
        paint.color = Color.argb(if (wet) 255 else 70, 255, 255, 255)
        val left = index * (barWidth + gap)
        canvas.drawRoundRect(RectF(left, heightPx - barHeight, left + barWidth, heightPx.toFloat()), barWidth / 2, barWidth / 2, paint)
    }
    return bitmap
}

class NowcastWidget : GlanceAppWidget() {
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
                NowcastRoot()
            }
        }
    }
}

class NowcastWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = NowcastWidget()

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
private fun NowcastRoot() {
    val context = LocalContext.current
    val glanceId = LocalGlanceId.current
    val size = LocalSize.current
    val weather by WidgetHub.weather.collectAsState()
    val revision by WidgetHub.revision.collectAsState()
    val (mode, spacing) = remember(revision) { widgetModeAndSpacing(context, glanceId) }
    val current = weather ?: return
    val density = context.resources.displayMetrics.density
    val slots = current.snapshot?.extras?.minutely.orEmpty().filter { it.timeMillis + 15 * 60_000L > current.now }
    val message = Nowcast.message(context, slots, current.now) ?: context.getString(R.string.nowcast_none)
    val shadow = mode == WidgetBackground.NONE
    val widthPx = ((size.width - spacing * 2).value * density).toInt()
    val textBitmap = remember(message, widthPx, shadow) {
        WidgetTemperatureRenderer.renderText(context, message, 15f * density * context.resources.configuration.fontScale, widthPx, WIDGET_TEXT_ARGB, shadow = shadow)
    }
    val barsHeightPx = (((size.height - spacing * 2).value * 0.5f) * density).toInt()
    val bars = remember(slots, widthPx, barsHeightPx) { rainBars(slots.map { it.mm }, widthPx, barsHeightPx) }
    WidgetSurface(mode, current) {
        Column(
            modifier = GlanceModifier.fillMaxSize().padding(spacing),
            verticalAlignment = Alignment.CenterVertically,
            horizontalAlignment = Alignment.Start,
        ) {
            Image(ImageProvider(textBitmap), contentDescription = message, colorFilter = widgetTextTint(mode))
            if (slots.isNotEmpty()) {
                Spacer(GlanceModifier.height(10.dp))
                Image(
                    ImageProvider(bars),
                    contentDescription = null,
                    contentScale = ContentScale.FillBounds,
                    colorFilter = if (mode == WidgetBackground.MATERIAL) ColorFilter.tint(GlanceTheme.colors.primary) else null,
                    modifier = GlanceModifier.fillMaxWidth().defaultWeight(),
                )
            }
        }
    }
}

class AqiWidget : GlanceAppWidget() {
    override val sizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        if (WeatherRepository.isStale(context)) {
            withTimeoutOrNull(4000L) {
                runCatching { WeatherRepository.refresh(context, fallbackToSaved = true) }
            }
        }
        WidgetHub.refresh(context)
        val aqi = try {
            AqiRepository.current(context)
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            AqiRepository.cached()
        }
        provideContent {
            GlanceTheme {
                AqiRoot(aqi)
            }
        }
    }
}

class AqiWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = AqiWidget()

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

private fun aqiArgb(category: AqiCategory): Int = when (category) {
    AqiCategory.GOOD -> 0xFF7ED491
    AqiCategory.MODERATE -> 0xFFFFD166
    AqiCategory.SENSITIVE -> 0xFFFF9E5E
    AqiCategory.UNHEALTHY -> 0xFFFF6B6B
    AqiCategory.VERY_UNHEALTHY -> 0xFFC77DFF
    AqiCategory.HAZARDOUS -> 0xFFD9466A
    AqiCategory.UNKNOWN -> 0xFFFFFFFF
}.toInt()

private fun aqiLabelRes(category: AqiCategory): Int = when (category) {
    AqiCategory.GOOD -> R.string.aqi_good
    AqiCategory.MODERATE -> R.string.aqi_moderate
    AqiCategory.SENSITIVE -> R.string.aqi_sensitive
    AqiCategory.UNHEALTHY -> R.string.aqi_unhealthy
    AqiCategory.VERY_UNHEALTHY -> R.string.aqi_very_unhealthy
    AqiCategory.HAZARDOUS -> R.string.aqi_hazardous
    AqiCategory.UNKNOWN -> R.string.condition_unknown
}

@Composable
private fun AqiRoot(aqi: AqiData?) {
    val context = LocalContext.current
    val glanceId = LocalGlanceId.current
    val size = LocalSize.current
    val weather by WidgetHub.weather.collectAsState()
    val revision by WidgetHub.revision.collectAsState()
    val (mode, spacing) = remember(revision) { widgetModeAndSpacing(context, glanceId) }
    val density = context.resources.displayMetrics.density
    val shadow = mode == WidgetBackground.NONE
    val widthPx = ((size.width - spacing * 2).value * density).toInt()
    val heightPx = ((size.height - spacing * 2).value * density).toInt()
    val category = aqi?.category ?: AqiCategory.UNKNOWN
    val number = aqi?.aqi?.toString() ?: "—"
    val label = context.getString(if (aqi == null) R.string.aqi_error else aqiLabelRes(category))
    val numberBitmap = remember(number, widthPx, heightPx, shadow) {
        WidgetTemperatureRenderer.render(context, number, widthPx, (heightPx * 0.6f).toInt(), aqiArgb(category), shadow = shadow)
    }
    val labelBitmap = remember(label, widthPx, shadow) {
        WidgetTemperatureRenderer.renderText(context, label, 13f * density * context.resources.configuration.fontScale, widthPx, WIDGET_TEXT_ARGB, shadow = shadow)
    }
    WidgetSurface(mode, weather) {
        Column(
            modifier = GlanceModifier.fillMaxSize().padding(spacing),
            verticalAlignment = Alignment.CenterVertically,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Image(
                ImageProvider(numberBitmap),
                contentDescription = number,
                contentScale = ContentScale.Fit,
                modifier = GlanceModifier.fillMaxWidth().defaultWeight(),
            )
            Spacer(GlanceModifier.height(4.dp))
            Image(ImageProvider(labelBitmap), contentDescription = label, colorFilter = widgetTextTint(mode))
        }
    }
}

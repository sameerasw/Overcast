package com.sameerasw.overcast.weather.widget

import android.content.Context
import android.text.format.DateFormat
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.graphics.ColorUtils
import androidx.glance.ColorFilter
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.ContentScale
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxHeight
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.size
import com.sameerasw.overcast.R
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs

private const val MIN_ITEM_DP = 52f
private const val MAX_ITEMS = 14
private const val HOUR_MS = 60 * 60_000L

// One column of a forecast row: a label, an icon, a temperature and optionally a muted second temperature.
internal class ForecastItem(val label: String, val icon: Int, val primary: String, val secondary: String? = null)

internal fun hourlyItems(context: Context, weather: WidgetWeather): List<ForecastItem> {
    val now = System.currentTimeMillis()
    val pattern = if (DateFormat.is24HourFormat(context)) "HH:mm" else "ha"
    val format = SimpleDateFormat(pattern, Locale.getDefault())
    return weather.snapshot?.hourly.orEmpty()
        .filter { it.timeMillis > now - HOUR_MS }
        .take(MAX_ITEMS)
        .map {
            ForecastItem(
                label = if (abs(it.timeMillis - now) < HOUR_MS * 3 / 4) {
                    context.getString(R.string.widget_hourly_now)
                } else {
                    format.format(Date(it.timeMillis)).lowercase(Locale.getDefault())
                },
                icon = weather.style.icon(it.condition, it.isDay),
                primary = com.sameerasw.overcast.weather.WeatherFormat.temperature(it.tempC, weather.units.temperature),
            )
        }
}

internal fun dailyItems(context: Context, weather: WidgetWeather): List<ForecastItem> {
    val format = SimpleDateFormat("EEE", Locale.getDefault())
    return weather.snapshot?.daily.orEmpty().take(MAX_ITEMS).mapIndexed { index, day ->
        ForecastItem(
            label = if (index == 0) context.getString(R.string.weather_detail_today) else format.format(Date(day.dayMillis)),
            icon = weather.style.icon(day.condition, true),
            primary = com.sameerasw.overcast.weather.WeatherFormat.temperature(day.highC, weather.units.temperature),
            secondary = com.sameerasw.overcast.weather.WeatherFormat.temperature(day.lowC, weather.units.temperature),
        )
    }
}

// Shows as many columns as fit the width, sharing the space evenly, and scales its contents to the height it's given.
@Composable
internal fun ForecastRow(
    items: List<ForecastItem>,
    tintable: Boolean,
    mode: WidgetBackground,
    width: Dp,
    height: Dp,
    modifier: GlanceModifier = GlanceModifier,
) {
    val context = LocalContext.current
    val resources = context.resources
    val density = resources.displayMetrics.density
    val fontScale = resources.configuration.fontScale
    val textColor = widgetTextColor(mode)
    val textArgb = widgetTextArgb(mode)
    val shadow = mode == WidgetBackground.NONE
    val hasSecondary = items.any { it.secondary != null }

    val count = (width.value / MIN_ITEM_DP).toInt().coerceIn(1, items.size.coerceAtLeast(1))
    val itemWidth = width / count
    val iconSize = (height * if (hasSecondary) 0.28f else 0.32f).coerceIn(18.dp, 40.dp)
    val tempHeight = (height * 0.2f).coerceIn(16.dp, 34.dp)
    val lowHeight = tempHeight * 0.8f
    val labelSp = (height.value * 0.13f).coerceIn(10f, 14f)
    val tempWidthPx = (itemWidth.value * 0.8f * density).toInt()
    val labelWidthPx = (itemWidth.value * density).toInt()
    val mutedArgb = ColorUtils.setAlphaComponent(textArgb, 170)

    Row(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        items.take(count).forEach { item ->
            val labelBitmap = remember(item.label, labelSp, labelWidthPx, textArgb, shadow) {
                WidgetTemperatureRenderer.renderText(context, item.label, labelSp * density * fontScale, labelWidthPx, textArgb, shadow = shadow)
            }
            val primaryBitmap = remember(item.primary, tempWidthPx, tempHeight, textArgb, shadow) {
                WidgetTemperatureRenderer.render(context, item.primary, tempWidthPx, (tempHeight.value * density).toInt(), textArgb, widthAxis = 60, weightAxis = 300, shadow = shadow)
            }
            val secondaryBitmap = item.secondary?.let { low ->
                remember(low, tempWidthPx, lowHeight, mutedArgb, shadow) {
                    WidgetTemperatureRenderer.render(context, low, tempWidthPx, (lowHeight.value * density).toInt(), mutedArgb, widthAxis = 60, weightAxis = 300, shadow = shadow)
                }
            }
            Column(
                modifier = GlanceModifier.defaultWeight().fillMaxHeight(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Image(provider = ImageProvider(labelBitmap), contentDescription = item.label)
                Spacer(GlanceModifier.height(3.dp))
                Image(
                    provider = ImageProvider(item.icon),
                    contentDescription = null,
                    colorFilter = if (tintable) ColorFilter.tint(textColor) else null,
                    modifier = GlanceModifier.size(iconSize),
                )
                Spacer(GlanceModifier.height(3.dp))
                Image(
                    provider = ImageProvider(primaryBitmap),
                    contentDescription = item.primary,
                    contentScale = ContentScale.Fit,
                    modifier = GlanceModifier.fillMaxWidth().height(tempHeight),
                )
                if (secondaryBitmap != null) {
                    Image(
                        provider = ImageProvider(secondaryBitmap),
                        contentDescription = item.secondary,
                        contentScale = ContentScale.Fit,
                        modifier = GlanceModifier.fillMaxWidth().height(lowHeight),
                    )
                }
            }
        }
    }
}

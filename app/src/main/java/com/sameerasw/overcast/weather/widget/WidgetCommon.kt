package com.sameerasw.overcast.weather.widget

import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.LocalSize
import androidx.glance.action.clickable
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.background
import androidx.glance.layout.Box
import androidx.glance.layout.ContentScale
import androidx.glance.layout.fillMaxSize
import androidx.glance.unit.ColorProvider
import com.sameerasw.overcast.ui.activities.WeatherActivity

internal val WIDGET_CORNER: Dp = 28.dp

internal fun widgetPadding(mode: WidgetBackground): Dp = if (mode == WidgetBackground.NONE) 8.dp else 14.dp

@Composable
internal fun widgetTextColor(mode: WidgetBackground): ColorProvider =
    if (mode == WidgetBackground.MATERIAL) GlanceTheme.colors.onSurface else ColorProvider(Color.White)

@Composable
internal fun widgetTextArgb(mode: WidgetBackground): Int =
    if (mode == WidgetBackground.MATERIAL) widgetTextColor(mode).getColor(LocalContext.current).toArgb() else android.graphics.Color.WHITE

@Composable
internal fun WidgetSurface(
    mode: WidgetBackground,
    weather: WidgetWeather?,
    background: GlanceModifier = GlanceModifier,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val size = LocalSize.current
    val density = context.resources.displayMetrics.density
    val ambient = if (mode == WidgetBackground.AMBIENT) {
        remember(weather?.snapshot, weather?.now, weather?.timeOverride, size) {
            WidgetAmbientRenderer.render(
                weather?.snapshot,
                weather?.now ?: System.currentTimeMillis(),
                weather?.timeOverride,
                (size.width.value * density).toInt(),
                (size.height.value * density).toInt(),
                WIDGET_CORNER.value * density,
            )
        }
    } else {
        null
    }
    val plate = if (mode == WidgetBackground.MATERIAL) {
        background.background(GlanceTheme.colors.widgetBackground).cornerRadius(WIDGET_CORNER)
    } else {
        background
    }
    Box(modifier = plate.fillMaxSize().clickable(actionStartActivity(Intent(context, WeatherActivity::class.java)))) {
        if (ambient != null) {
            Image(
                provider = ImageProvider(ambient),
                contentDescription = null,
                contentScale = ContentScale.FillBounds,
                modifier = GlanceModifier.fillMaxSize(),
            )
        }
        content()
    }
}

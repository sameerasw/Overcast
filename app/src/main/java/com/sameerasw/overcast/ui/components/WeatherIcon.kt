package com.sameerasw.overcast.ui.components

import androidx.compose.foundation.Image
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import com.sameerasw.overcast.weather.WeatherIconSlot
import com.sameerasw.overcast.weather.WeatherIconStyle
import com.sameerasw.overcast.weather.model.WeatherCondition

val LocalWeatherIconStyle = staticCompositionLocalOf { WeatherIconStyle.Default }

@Composable
fun WeatherIcon(
    condition: WeatherCondition,
    isDay: Boolean,
    tint: Color,
    modifier: Modifier = Modifier,
    style: WeatherIconStyle = LocalWeatherIconStyle.current,
) = WeatherIcon(WeatherIconSlot.of(condition, isDay), tint, modifier, style)

@Composable
fun WeatherIcon(
    slot: WeatherIconSlot,
    tint: Color,
    modifier: Modifier = Modifier,
    style: WeatherIconStyle = LocalWeatherIconStyle.current,
) {
    val painter = painterResource(style.icon(slot))
    if (style.tintable) {
        Icon(painter, contentDescription = null, tint = tint, modifier = modifier)
    } else {
        Image(painter, contentDescription = null, modifier = modifier)
    }
}

package com.sameerasw.overcast.ui.features.weather

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.carousel.HorizontalMultiBrowseCarousel
import androidx.compose.material3.carousel.rememberCarouselState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.sameerasw.overcast.ui.components.WeatherIcon
import com.sameerasw.overcast.utils.HapticUtil
import com.sameerasw.overcast.weather.WeatherIconSlot
import com.sameerasw.overcast.weather.WeatherIconStyle
import kotlinx.coroutines.delay

private const val CYCLE_MS = 3_000L
private val SLOTS = WeatherIconSlot.entries

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WeatherIconStyleCarousel(
    selected: WeatherIconStyle,
    onSelected: (WeatherIconStyle) -> Unit,
    modifier: Modifier = Modifier,
) {
    val view = LocalView.current
    val styles = WeatherIconStyle.entries
    // One shared clock so every style shows the same weather at the same moment.
    var slotIndex by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(CYCLE_MS)
            slotIndex = (slotIndex + 1) % SLOTS.size
        }
    }
    val state = rememberCarouselState { styles.size }
    Column(modifier.fillMaxWidth().padding(vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            stringResource(com.sameerasw.overcast.R.string.weather_icon_style_title),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(horizontal = 16.dp),
        )
        HorizontalMultiBrowseCarousel(
            state = state,
            preferredItemWidth = 148.dp,
            itemSpacing = 8.dp,
            contentPadding = PaddingValues(horizontal = 16.dp),
            modifier = Modifier.fillMaxWidth().height(156.dp),
        ) { index ->
            val style = styles[index]
            val isSelected = style == selected
            Surface(
                color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.background,
                contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
                modifier = Modifier
                    .fillMaxSize()
                    .maskClip(MaterialTheme.shapes.extraLarge)
                    .clickable {
                        HapticUtil.performVirtualKeyHaptic(view)
                        onSelected(style)
                    },
            ) {
                Column(
                    Modifier.fillMaxSize().padding(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Crossfade(targetState = slotIndex, animationSpec = tween(500), label = "iconCycle") { current ->
                        WeatherIcon(
                            slot = SLOTS[current],
                            tint = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.primary,
                            style = style,
                            modifier = Modifier.size(64.dp),
                        )
                    }
                    Text(
                        stringResource(style.labelRes),
                        style = MaterialTheme.typography.labelLarge,
                        maxLines = 1,
                        modifier = Modifier.padding(top = 12.dp),
                    )
                }
            }
        }
    }
}

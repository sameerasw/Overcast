package com.sameerasw.overcast.ui.features.weather

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.sameerasw.overcast.R
import com.sameerasw.overcast.data.repository.SettingsRepository
import com.sameerasw.overcast.ui.components.menus.SegmentedDropdownMenuItem
import com.sameerasw.overcast.ui.core.cards.IconToggleItem
import com.sameerasw.overcast.ui.core.containers.RoundedCardContainer
import com.sameerasw.overcast.ui.core.sheets.OvercastBottomSheet
import com.sameerasw.overcast.utils.HapticUtil
import com.sameerasw.overcast.weather.location.DeviceLocationSource
import com.sameerasw.overcast.weather.location.WeatherPlace
import com.sameerasw.overcast.weather.location.WeatherPlaces
import com.sameerasw.overcast.weather.model.CityResult
import com.sameerasw.overcast.weather.model.WeatherLocation
import com.sameerasw.overcast.weather.provider.ProviderHttp
import com.sameerasw.overcast.weather.provider.WeatherProviderException
import com.sameerasw.overcast.weather.provider.WeatherProviders
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val SEARCH_DEBOUNCE_MS = 400L

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun LocationsBottomSheet(
    onDismissRequest: () -> Unit,
    onSelectCurrent: () -> Unit,
    onSelectPlace: (WeatherPlace) -> Unit,
    onTopChanged: (Float) -> Unit,
) {
    val context = LocalContext.current
    val view = LocalView.current
    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val settings = remember { SettingsRepository(context) }
    val places = remember { WeatherPlaces(context) }
    val provider = remember {
        val apiKey = settings.getWeatherApiKey("weatherapi")
        WeatherProviders.resolve(settings.getWeatherProvider(), apiKey)
    }
    val deviceSelected = remember { settings.getWeatherLocationMode() != "manual" }
    val manualLocation = remember { settings.getWeatherManualLocation() }
    val hasLocationPermission = remember { DeviceLocationSource.hasPermission(context) }

    var deviceLocation by remember { mutableStateOf<WeatherLocation?>(null) }
    var saved by remember { mutableStateOf(places.saved()) }
    var recent by remember { mutableStateOf(places.recent()) }
    var query by remember { mutableStateOf("") }
    var results by remember { mutableStateOf<List<CityResult>>(emptyList()) }
    var searching by remember { mutableStateOf(false) }
    var searchError by remember { mutableStateOf<Int?>(null) }

    DisposableEffect(Unit) {
        onDispose { onTopChanged(Float.NaN) }
    }

    LaunchedEffect(Unit) {
        if (hasLocationPermission) deviceLocation = DeviceLocationSource.current(context)
    }

    LaunchedEffect(query) {
        val trimmed = query.trim()
        searchError = null
        if (trimmed.isEmpty()) {
            results = emptyList()
            searching = false
            return@LaunchedEffect
        }
        searching = true
        delay(SEARCH_DEBOUNCE_MS)
        try {
            results = provider.searchCities(trimmed, settings.getWeatherApiKey(provider.id))
            if (results.isEmpty()) searchError = R.string.weather_search_no_results
        } catch (e: CancellationException) {
            throw e
        } catch (e: WeatherProviderException) {
            results = emptyList()
            searchError = if (e.reason == WeatherProviderException.Reason.INVALID_KEY) {
                R.string.weather_error_missing_key
            } else if (e.reason == WeatherProviderException.Reason.RATE_LIMITED) {
                R.string.weather_error_rate_limited
            } else {
                R.string.weather_error_network
            }
        } catch (_: Exception) {
            results = emptyList()
            searchError = R.string.weather_error_unknown
        }
        searching = false
    }

    fun close(afterClose: () -> Unit) {
        scope.launch { sheetState.hide() }.invokeOnCompletion {
            onDismissRequest()
            afterClose()
        }
    }

    fun choose(place: WeatherPlace) {
        places.addRecent(place)
        close { onSelectPlace(place) }
    }

    fun toggleCurrentSaved() {
        val location = deviceLocation ?: return
        val existing = saved.firstOrNull { it.sameSpotAs(location.latitude, location.longitude) }
        if (existing != null) {
            places.unsave(existing)
            saved = places.saved()
            return
        }
        scope.launch {
            val geo = ProviderHttp.reverseGeocode(location.latitude, location.longitude)
            val name = geo?.first ?: context.getString(R.string.weather_location_device)
            val label = geo?.let { "${it.first}, ${it.second}" } ?: name
            places.save(WeatherPlace(name, label, location.latitude, location.longitude))
            saved = places.saved()
            recent = places.recent()
        }
    }

    fun toggleSaved(place: WeatherPlace) {
        if (places.isSaved(place)) places.unsave(place) else places.save(place)
        saved = places.saved()
        recent = places.recent()
    }

    OvercastBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        modifier = Modifier.onGloballyPositioned { onTopChanged(it.positionInWindow().y) },
        scrimColor = Color.Black.copy(alpha = 0.32f),
    ) {
        LocationSearchField(
            query = query,
            searching = searching,
            onQueryChange = { query = it },
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f, fill = false)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (query.isBlank()) {
                item {
                    RoundedCardContainer {
                        IconToggleItem(
                            iconRes = R.drawable.rounded_my_location_24,
                            title = stringResource(R.string.weather_location_device),
                            description = if (hasLocationPermission) null else stringResource(R.string.weather_error_location_permission),
                            showToggle = false,
                            onClick = { close { onSelectCurrent() } },
                            trailingContent = if (deviceSelected || deviceLocation != null) {
                                {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        if (deviceSelected) SelectedMark()
                                        deviceLocation?.let { location ->
                                            val currentSaved = saved.any { it.sameSpotAs(location.latitude, location.longitude) }
                                            IconButton(
                                                onClick = {
                                                    HapticUtil.performVirtualKeyHaptic(view)
                                                    toggleCurrentSaved()
                                                },
                                            ) {
                                                Icon(
                                                    painter = painterResource(
                                                        if (currentSaved) R.drawable.rounded_bookmark_remove_24 else R.drawable.rounded_bookmark_24,
                                                    ),
                                                    contentDescription = stringResource(
                                                        if (currentSaved) R.string.weather_place_remove_saved else R.string.weather_place_save,
                                                    ),
                                                    tint = if (currentSaved) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                                )
                                            }
                                        }
                                    }
                                }
                            } else {
                                null
                            },
                        )
                    }
                }
                if (saved.isNotEmpty()) {
                    item { SheetSectionTitle(R.string.weather_section_saved) }
                    item {
                        RoundedCardContainer {
                            saved.forEach { place ->
                                PlaceRow(
                                    place = place,
                                    iconRes = R.drawable.rounded_location_on_24,
                                    selected = manualLocation?.let { !deviceSelected && place.sameSpotAs(it.first, it.second) } == true,
                                    onClick = { choose(place) },
                                    menu = {
                                        SegmentedDropdownMenuItem(
                                            text = { Text(stringResource(R.string.weather_place_remove_saved)) },
                                            leadingIcon = { MenuIcon(R.drawable.rounded_bookmark_remove_24) },
                                            onClick = { toggleSaved(place) },
                                        )
                                    },
                                )
                            }
                        }
                    }
                }
                if (recent.isNotEmpty()) {
                    item { SheetSectionTitle(R.string.weather_section_recent) }
                    item {
                        RoundedCardContainer {
                            recent.forEach { place ->
                                PlaceRow(
                                    place = place,
                                    iconRes = R.drawable.rounded_history_24,
                                    selected = manualLocation?.let { !deviceSelected && place.sameSpotAs(it.first, it.second) } == true,
                                    onClick = { choose(place) },
                                    menu = {
                                        SegmentedDropdownMenuItem(
                                            text = { Text(stringResource(R.string.weather_place_save)) },
                                            leadingIcon = { MenuIcon(R.drawable.rounded_bookmark_24) },
                                            onClick = { toggleSaved(place) },
                                        )
                                        SegmentedDropdownMenuItem(
                                            text = { Text(stringResource(R.string.weather_place_remove_recent)) },
                                            leadingIcon = { MenuIcon(R.drawable.rounded_delete_24) },
                                            onClick = {
                                                places.removeRecent(place)
                                                recent = places.recent()
                                            },
                                        )
                                    },
                                )
                            }
                        }
                    }
                }
            } else {
                if (results.isNotEmpty()) {
                    item {
                        RoundedCardContainer {
                            results.forEach { city ->
                                val place = WeatherPlace.from(city)
                                val isSaved = saved.any { it.id == place.id }
                                IconToggleItem(
                                    iconRes = R.drawable.rounded_location_city_24,
                                    title = city.name,
                                    description = listOf(city.region, city.country).filter { it.isNotBlank() }.distinct().joinToString(", ")
                                        .takeIf { it.isNotBlank() },
                                    showToggle = false,
                                    onClick = { choose(place) },
                                    trailingContent = {
                                        IconButton(
                                            onClick = {
                                                HapticUtil.performVirtualKeyHaptic(view)
                                                toggleSaved(place)
                                            },
                                        ) {
                                            Icon(
                                                painter = painterResource(
                                                    if (isSaved) R.drawable.rounded_bookmark_remove_24 else R.drawable.rounded_bookmark_24,
                                                ),
                                                contentDescription = stringResource(
                                                    if (isSaved) R.string.weather_place_remove_saved else R.string.weather_place_save,
                                                ),
                                                tint = if (isSaved) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                            )
                                        }
                                    },
                                )
                            }
                        }
                    }
                }
                searchError?.let {
                    item {
                        Text(
                            text = stringResource(it),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp, horizontal = 8.dp),
                        )
                    }
                }
            }
            item { Spacer(Modifier.height(16.dp)) }
        }
    }
}

@Composable
private fun LocationSearchField(
    query: String,
    searching: Boolean,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = modifier.fillMaxWidth(),
        leadingIcon = {
            Box(Modifier.padding(start = 12.dp, end = 4.dp).size(40.dp), contentAlignment = Alignment.Center) {
                AnimatedVisibility(visible = searching, enter = fadeIn(), exit = fadeOut()) {
                    LoadingIndicator(Modifier.size(28.dp))
                }
                AnimatedVisibility(visible = !searching, enter = fadeIn(), exit = fadeOut()) {
                    Icon(
                        painter = painterResource(R.drawable.rounded_search_24),
                        contentDescription = stringResource(R.string.action_search),
                        modifier = Modifier.size(24.dp),
                    )
                }
            }
        },
        placeholder = {
            Text(
                text = stringResource(R.string.weather_search_city),
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                modifier = Modifier.basicMarquee(),
            )
        },
        trailingIcon = {
            if (query.isNotEmpty()) {
                val view = LocalView.current
                IconButton(
                    onClick = {
                        HapticUtil.performVirtualKeyHaptic(view)
                        onQueryChange("")
                    },
                ) {
                    Icon(painter = painterResource(R.drawable.rounded_close_24), contentDescription = stringResource(R.string.action_clear))
                }
            }
        },
        shape = MaterialTheme.shapes.extraExtraLarge,
        singleLine = true,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            unfocusedBorderColor = MaterialTheme.colorScheme.onSurfaceVariant,
            focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
        ),
    )
}

@Composable
private fun PlaceRow(
    place: WeatherPlace,
    iconRes: Int,
    selected: Boolean,
    onClick: () -> Unit,
    menu: @Composable ColumnScope.() -> Unit,
) {
    val parts = place.label.split(", ", limit = 2)
    IconToggleItem(
        iconRes = iconRes,
        title = place.name,
        description = parts.getOrNull(1)?.takeIf { it.isNotBlank() },
        showToggle = false,
        onClick = onClick,
        trailingContent = if (selected) {
            { SelectedMark() }
        } else {
            null
        },
        longClickMenu = menu,
    )
}

@Composable
private fun SelectedMark() {
    Icon(
        painter = painterResource(R.drawable.rounded_check_24),
        contentDescription = null,
        tint = MaterialTheme.colorScheme.primary,
        modifier = Modifier.size(24.dp),
    )
}

@Composable
private fun MenuIcon(iconRes: Int) {
    Icon(painter = painterResource(iconRes), contentDescription = null, modifier = Modifier.size(20.dp))
}

@Composable
private fun SheetSectionTitle(res: Int) {
    Row(Modifier.fillMaxWidth().padding(start = 8.dp, top = 8.dp)) {
        Text(
            text = stringResource(res),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

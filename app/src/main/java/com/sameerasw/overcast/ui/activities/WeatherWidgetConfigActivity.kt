package com.sameerasw.overcast.ui.activities

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.material3.Text
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.sameerasw.overcast.R
import com.sameerasw.overcast.ui.components.ReusableTopAppBar
import com.sameerasw.overcast.ui.core.containers.RoundedCardContainer
import com.sameerasw.overcast.ui.core.pickers.SegmentedPicker
import com.sameerasw.overcast.ui.theme.OvercastTheme
import com.sameerasw.overcast.utils.HapticUtil
import com.sameerasw.overcast.weather.widget.WeatherWidgetUpdater
import com.sameerasw.overcast.weather.widget.WidgetBackground
import com.sameerasw.overcast.weather.widget.WidgetConfigStore
import com.sameerasw.overcast.weather.widget.WidgetForecast
import com.sameerasw.overcast.weather.widget.WeatherWidgetReceiver

class WeatherWidgetConfigActivity : ComponentActivity() {
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        val appWidgetId = intent?.extras?.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
            ?: AppWidgetManager.INVALID_APPWIDGET_ID
        // Backing out must cancel, otherwise the launcher would keep a half-configured widget.
        setResult(Activity.RESULT_CANCELED, Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId))
        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish()
            return
        }
        // The hourly widget is already a forecast, so only the temperature widget gets the forecast row.
        val supportsForecast = AppWidgetManager.getInstance(this).getAppWidgetInfo(appWidgetId)
            ?.provider?.className == WeatherWidgetReceiver::class.java.name
        setContent {
            OvercastTheme {
                val context = LocalContext.current
                val view = LocalView.current
                val store = remember { WidgetConfigStore(context) }
                var selected by remember { mutableStateOf(store.background(appWidgetId)) }
                var forecast by remember { mutableStateOf(store.forecast(appWidgetId)) }
                val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
                Scaffold(
                    modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
                    containerColor = MaterialTheme.colorScheme.surfaceContainer,
                    topBar = {
                        ReusableTopAppBar(
                            title = R.string.widget_config_title,
                            onBackClick = { finish() },
                            scrollBehavior = scrollBehavior,
                            actions = {
                                IconButton(
                                    onClick = {
                                        HapticUtil.performVirtualKeyHaptic(view)
                                        store.setBackground(appWidgetId, selected)
                                        if (supportsForecast) store.setForecast(appWidgetId, forecast)
                                        WeatherWidgetUpdater.updateAll(context)
                                        setResult(Activity.RESULT_OK, Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId))
                                        finish()
                                    },
                                    colors = IconButtonDefaults.iconButtonColors(containerColor = MaterialTheme.colorScheme.surfaceBright),
                                ) {
                                    Icon(
                                        painter = painterResource(R.drawable.rounded_check_24),
                                        contentDescription = stringResource(R.string.action_save),
                                    )
                                }
                            },
                        )
                    },
                ) { padding ->
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(padding)
                            .padding(horizontal = 16.dp, vertical = 16.dp),
                    ) {
                        SectionTitle(R.string.widget_config_section_background)
                        RoundedCardContainer(spacing = 2.dp, cornerRadius = 24.dp) {
                            SegmentedPicker(
                                items = WidgetBackground.entries.toList(),
                                selectedItem = selected,
                                onItemSelected = { selected = it },
                                labelProvider = { context.getString(it.labelRes) },
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                        if (supportsForecast) {
                            Spacer(Modifier.height(16.dp))
                            SectionTitle(R.string.widget_config_section_forecast)
                            RoundedCardContainer(spacing = 2.dp, cornerRadius = 24.dp) {
                                SegmentedPicker(
                                    items = WidgetForecast.entries.toList(),
                                    selectedItem = forecast,
                                    onItemSelected = { forecast = it },
                                    labelProvider = { context.getString(it.labelRes) },
                                    modifier = Modifier.fillMaxWidth(),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionTitle(res: Int) {
    Text(
        text = stringResource(res),
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(start = 8.dp, bottom = 8.dp),
    )
}

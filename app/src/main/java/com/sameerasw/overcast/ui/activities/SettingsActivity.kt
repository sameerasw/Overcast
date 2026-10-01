package com.sameerasw.overcast.ui.activities

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.sameerasw.overcast.R
import com.sameerasw.overcast.data.repository.SettingsRepository
import com.sameerasw.overcast.ui.components.ReusableTopAppBar
import com.sameerasw.overcast.ui.components.dialogs.AboutSection
import com.sameerasw.overcast.ui.core.cards.IconToggleItem
import com.sameerasw.overcast.ui.core.containers.RoundedCardContainer
import com.sameerasw.overcast.ui.features.weather.WeatherSettingsUI
import com.sameerasw.overcast.ui.theme.OvercastTheme
import com.sameerasw.overcast.utils.HapticUtil

class SettingsActivity : ComponentActivity() {
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            OvercastTheme {
                val context = LocalContext.current
                val view = LocalView.current
                val settings = remember { SettingsRepository(context) }
                var developerMode by remember { mutableStateOf(settings.isDeveloperModeEnabled()) }
                var weatherExperimental by remember { mutableStateOf(settings.isWeatherExperimentalEnabled()) }
                val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

                Scaffold(
                    modifier = Modifier.fillMaxSize().nestedScroll(scrollBehavior.nestedScrollConnection),
                    containerColor = MaterialTheme.colorScheme.surfaceContainer,
                    topBar = {
                        ReusableTopAppBar(
                            title = R.string.settings_activity_label,
                            onBackClick = { finish() },
                            scrollBehavior = scrollBehavior,
                        )
                    },
                ) { innerPadding ->
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                            .verticalScroll(rememberScrollState())
                            .navigationBarsPadding(),
                    ) {
                        WeatherSettingsUI(modifier = Modifier.padding(top = 16.dp))

                        Spacer(Modifier.height(16.dp))

                        RoundedCardContainer(modifier = Modifier.padding(horizontal = 16.dp)) {
                            AboutSection(
                                onAvatarLongClick = {
                                    developerMode = !developerMode
                                    settings.setDeveloperModeEnabled(developerMode)
                                    Toast.makeText(
                                        context,
                                        if (developerMode) R.string.developer_options_enabled else R.string.developer_options_disabled,
                                        Toast.LENGTH_SHORT,
                                    ).show()
                                },
                            )
                        }

                        if (developerMode) {
                            Column(
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    text = stringResource(R.string.developer_options_title),
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(start = 8.dp),
                                )
                                RoundedCardContainer {
                                    IconToggleItem(
                                        iconRes = R.drawable.rounded_partly_cloudy_day_24,
                                        title = stringResource(R.string.dev_weather_experimental_title),
                                        isChecked = weatherExperimental,
                                        onCheckedChange = {
                                            HapticUtil.performVirtualKeyHaptic(view)
                                            weatherExperimental = it
                                            settings.putBoolean(SettingsRepository.KEY_DEBUG_WEATHER_EXPERIMENTAL, it)
                                        },
                                    )
                                }
                            }
                        }

                        Spacer(Modifier.height(32.dp))
                    }
                }
            }
        }
    }
}

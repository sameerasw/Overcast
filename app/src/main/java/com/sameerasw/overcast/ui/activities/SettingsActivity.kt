package com.sameerasw.overcast.ui.activities

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.sameerasw.overcast.R
import com.sameerasw.overcast.data.repository.SettingsRepository
import com.sameerasw.overcast.ui.components.ReusableTopAppBar
import com.sameerasw.overcast.ui.components.dialogs.AboutSection
import com.sameerasw.overcast.ui.core.containers.RoundedCardContainer
import com.sameerasw.overcast.ui.features.weather.WeatherExperimentsUI
import com.sameerasw.overcast.ui.features.weather.WeatherSettingsUI
import com.sameerasw.overcast.ui.modifiers.BlurDirection
import com.sameerasw.overcast.ui.modifiers.progressiveBlur
import com.sameerasw.overcast.ui.theme.OvercastTheme

class SettingsActivity : ComponentActivity() {
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            OvercastTheme {
                val context = LocalContext.current
                val density = LocalDensity.current
                val settings = remember { SettingsRepository(context) }
                var developerMode by remember { mutableStateOf(settings.isDeveloperModeEnabled()) }
                val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
                val topBarHeight = remember { mutableFloatStateOf(0f) }
                val topFadePx = topBarHeight.floatValue + with(density) { 24.dp.toPx() }
                val bottomFadePx = WindowInsets.navigationBars.getBottom(density) + with(density) { 32.dp.toPx() }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.surfaceContainer)
                        .nestedScroll(scrollBehavior.nestedScrollConnection),
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
                            .drawWithContent {
                                drawContent()
                                if (topFadePx > 0f) {
                                    drawRect(
                                        brush = Brush.verticalGradient(
                                            colors = listOf(Color.Transparent, Color.Black),
                                            startY = 0f,
                                            endY = topFadePx,
                                        ),
                                        blendMode = BlendMode.DstIn,
                                    )
                                }
                                drawRect(
                                    brush = Brush.verticalGradient(
                                        colors = listOf(Color.Black, Color.Transparent),
                                        startY = size.height - bottomFadePx,
                                        endY = size.height,
                                    ),
                                    topLeft = Offset(0f, size.height - bottomFadePx),
                                    size = Size(size.width, bottomFadePx),
                                    blendMode = BlendMode.DstIn,
                                )
                            }
                            .progressiveBlur(blurRadius = 40f, height = topFadePx, direction = BlurDirection.TOP, showGradientOverlay = false)
                            .progressiveBlur(blurRadius = 14f, height = bottomFadePx, direction = BlurDirection.BOTTOM, showGradientOverlay = false),
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState())
                                .padding(top = with(density) { topBarHeight.floatValue.toDp() }),
                        ) {
                            WeatherSettingsUI(modifier = Modifier.padding(top = 16.dp))

                            Spacer(Modifier.height(32.dp))

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
                                    WeatherExperimentsUI()
                                }
                            }

                            Spacer(Modifier.height(with(density) { bottomFadePx.toDp() } + 16.dp))
                        }
                    }

                    Box(
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .fillMaxWidth()
                            .onSizeChanged { topBarHeight.floatValue = it.height.toFloat() },
                    ) {
                        ReusableTopAppBar(
                            title = R.string.settings_activity_label,
                            onBackClick = { finish() },
                            scrollBehavior = scrollBehavior,
                        )
                    }
                }
            }
        }
    }
}

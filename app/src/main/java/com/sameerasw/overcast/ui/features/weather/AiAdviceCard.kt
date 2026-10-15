package com.sameerasw.overcast.ui.features.weather

import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.sameerasw.overcast.R
import com.sameerasw.overcast.data.repository.SettingsRepository
import com.sameerasw.overcast.ui.activities.SettingsActivity
import com.sameerasw.overcast.utils.HapticUtil
import androidx.compose.ui.platform.LocalView
import com.sameerasw.overcast.weather.ai.AiAdvisor
import com.sameerasw.overcast.weather.ai.AiProviders
import com.sameerasw.overcast.weather.model.WeatherSnapshot
import java.util.Locale
import kotlinx.coroutines.launch

@Composable
fun AiAdviceSection(snapshot: WeatherSnapshot, palette: WeatherPalette, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val view = LocalView.current
    val scope = rememberCoroutineScope()
    val settings = remember { SettingsRepository(context) }
    var providerId by remember { mutableStateOf(settings.getAiProvider()) }
    var advice by remember(snapshot.updatedAt) { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var hasKey by remember { mutableStateOf(settings.getAiApiKey(providerId) != null) }

    fun load() {
        val key = settings.getAiApiKey(providerId)
        if (key.isNullOrBlank()) {
            hasKey = false
            return
        }
        hasKey = true
        scope.launch {
            loading = true
            error = null
            try {
                val provider = AiProviders.byId(providerId)
                advice = AiAdvisor.advise(
                    providerId = provider.id,
                    apiKey = key,
                    model = settings.getAiModel(provider.id),
                    baseUrl = settings.getAiBaseUrl(provider.id),
                    snapshot = snapshot,
                    locale = Locale.getDefault(),
                )
            } catch (e: Exception) {
                error = e.message
            } finally {
                loading = false
            }
        }
    }

    LaunchedEffect(snapshot.updatedAt, providerId) {
        providerId = settings.getAiProvider()
        hasKey = settings.getAiApiKey(providerId) != null
        if (advice == null && hasKey) load()
    }

    Column(modifier) {
        Surface(
            color = palette.card,
            shape = MaterialTheme.shapes.extraLarge,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Icon(
                        painterResource(R.drawable.rounded_model_training_24),
                        contentDescription = null,
                        tint = palette.accent,
                        modifier = Modifier.size(24.dp),
                    )
                    Text(
                        stringResource(R.string.ai_advice_title),
                        color = palette.onBase,
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.weight(1f),
                    )
                    IconButton(
                        onClick = {
                            HapticUtil.performVirtualKeyHaptic(view)
                            load()
                        },
                        modifier = Modifier.size(32.dp),
                    ) {
                        Icon(
                            painterResource(R.drawable.rounded_refresh_24),
                            contentDescription = stringResource(R.string.weather_refresh),
                            tint = palette.onBaseMuted,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }
                when {
                    !hasKey -> {
                        Text(
                            stringResource(R.string.ai_advice_no_key),
                            color = palette.onBaseMuted,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        Button(
                            onClick = {
                                context.startActivity(Intent(context, SettingsActivity::class.java))
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = palette.accent, contentColor = androidx.compose.ui.graphics.Color.Black),
                        ) {
                            Text(stringResource(R.string.weather_open_settings))
                        }
                    }
                    loading && advice == null -> LoadingIndicator(Modifier.size(48.dp))
                    error != null && advice == null -> Text(
                        error ?: stringResource(R.string.weather_error_unknown),
                        color = palette.onBaseMuted,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    advice != null -> {
                        Text(advice!!, color = palette.onBase, style = MaterialTheme.typography.bodyMedium)
                        if (loading) LoadingIndicator(Modifier.size(32.dp))
                    }
                }
            }
        }
    }
}

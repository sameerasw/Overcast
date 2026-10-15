package com.sameerasw.overcast.ui.features.weather

import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.sameerasw.overcast.R
import com.sameerasw.overcast.data.repository.SettingsRepository
import com.sameerasw.overcast.ui.components.menus.SegmentedDropdownMenuItem
import com.sameerasw.overcast.ui.core.cards.ConfigPickerItem
import com.sameerasw.overcast.ui.core.containers.RoundedCardContainer
import com.sameerasw.overcast.utils.HapticUtil
import com.sameerasw.overcast.weather.ai.AiProviders

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiSettingsUI(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val view = LocalView.current
    val settings = remember { SettingsRepository(context) }
    var providerId by remember { mutableStateOf(settings.getAiProvider()) }
    val provider = AiProviders.byId(providerId)
    var savedKey by remember(providerId) { mutableStateOf(settings.getAiApiKey(providerId).orEmpty()) }
    var keyInput by remember(providerId) { mutableStateOf(savedKey) }
    var keyVisible by remember { mutableStateOf(false) }
    var modelInput by remember(providerId) { mutableStateOf(settings.getAiModel(providerId) ?: provider.defaultModel) }
    var baseUrlInput by remember(providerId) { mutableStateOf(settings.getAiBaseUrl(providerId) ?: provider.defaultBaseUrl.orEmpty()) }

    Column(
        modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = stringResource(R.string.ai_section_title),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 8.dp),
        )
        RoundedCardContainer(spacing = 2.dp, cornerRadius = 24.dp) {
            ConfigPickerItem(
                title = stringResource(R.string.ai_provider_title),
                selectedValue = provider.displayName,
                iconRes = R.drawable.rounded_model_training_24,
                modifier = Modifier.fillMaxWidth(),
            ) {
                AiProviders.all.forEach { option ->
                    SegmentedDropdownMenuItem(
                        text = { Text(option.displayName) },
                        onClick = {
                            if (option.id != providerId) {
                                settings.setAiProvider(option.id)
                                providerId = option.id
                            }
                        },
                    )
                }
            }
            Surface(
                color = MaterialTheme.colorScheme.surfaceBright,
                shape = MaterialTheme.shapes.extraSmall,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = keyInput,
                        onValueChange = { keyInput = it.trim() },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        label = { Text(stringResource(R.string.weather_api_key_title)) },
                        leadingIcon = { Icon(painterResource(R.drawable.rounded_key_24), contentDescription = null) },
                        trailingIcon = {
                            Row {
                                IconButton(onClick = {
                                    HapticUtil.performVirtualKeyHaptic(view)
                                    clipboardText(context)?.let { keyInput = it.trim() }
                                }) {
                                    Icon(painterResource(R.drawable.rounded_content_paste_24), contentDescription = stringResource(R.string.shorten_paste_button))
                                }
                                IconButton(onClick = {
                                    HapticUtil.performVirtualKeyHaptic(view)
                                    keyVisible = !keyVisible
                                }) {
                                    Icon(
                                        painterResource(if (keyVisible) R.drawable.rounded_visibility_off_24 else R.drawable.rounded_visibility_24),
                                        contentDescription = null,
                                    )
                                }
                            }
                        },
                        visualTransformation = if (keyVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            autoCorrectEnabled = false,
                            imeAction = ImeAction.Done,
                        ),
                        shape = RoundedCornerShape(16.dp),
                    )
                    OutlinedTextField(
                        value = modelInput,
                        onValueChange = { modelInput = it.trim() },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        label = { Text(stringResource(R.string.ai_model_title)) },
                        placeholder = { Text(provider.defaultModel) },
                        keyboardOptions = KeyboardOptions(autoCorrectEnabled = false, imeAction = ImeAction.Done),
                        shape = RoundedCornerShape(16.dp),
                    )
                    if (provider.needsBaseUrl) {
                        OutlinedTextField(
                            value = baseUrlInput,
                            onValueChange = { baseUrlInput = it.trim() },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            label = { Text(stringResource(R.string.ai_base_url_title)) },
                            placeholder = { Text(provider.defaultBaseUrl.orEmpty()) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri, autoCorrectEnabled = false, imeAction = ImeAction.Done),
                            shape = RoundedCornerShape(16.dp),
                        )
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        provider.signupUrl?.let { url ->
                            TextButton(onClick = {
                                HapticUtil.performVirtualKeyHaptic(view)
                                openAiUrl(context, url)
                            }) {
                                Text(stringResource(R.string.weather_get_api_key))
                            }
                        }
                        Spacer(Modifier.weight(1f))
                        Button(
                            onClick = {
                                HapticUtil.performVirtualKeyHaptic(view)
                                settings.setAiApiKey(providerId, keyInput)
                                settings.setAiModel(providerId, modelInput.ifBlank { provider.defaultModel })
                                if (provider.needsBaseUrl) settings.setAiBaseUrl(providerId, baseUrlInput.ifBlank { provider.defaultBaseUrl })
                                savedKey = keyInput
                            },
                            enabled = keyInput != savedKey || modelInput != (settings.getAiModel(providerId) ?: provider.defaultModel),
                        ) {
                            Text(stringResource(R.string.action_save))
                        }
                    }
                }
            }
        }
    }
}

private fun clipboardText(context: Context): String? {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager ?: return null
    return clipboard.primaryClip?.takeIf { it.itemCount > 0 }?.getItemAt(0)?.coerceToText(context)?.toString()
}

private fun openAiUrl(context: Context, url: String) {
    try {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    } catch (_: Exception) {
    }
}

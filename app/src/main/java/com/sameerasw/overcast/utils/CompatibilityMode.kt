package com.sameerasw.overcast.utils

import android.content.Context
import androidx.compose.runtime.mutableStateOf
import com.sameerasw.overcast.data.repository.SettingsRepository

object CompatibilityMode {
    val enabled = mutableStateOf(false)

    fun initialize(context: Context) {
        enabled.value = SettingsRepository(context).isCompatibilityMode()
    }
}

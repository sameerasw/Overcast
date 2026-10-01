package com.sameerasw.overcast.utils

import android.content.Context
import android.os.PowerManager

object DeviceUtils {
    fun isPowerSaveMode(context: Context): Boolean {
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
        return powerManager?.isPowerSaveMode == true
    }
}

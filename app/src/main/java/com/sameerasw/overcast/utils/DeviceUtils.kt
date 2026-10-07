package com.sameerasw.overcast.utils

import android.app.ActivityManager
import android.content.Context
import android.os.PowerManager

object DeviceUtils {
    
    private const val LOW_RAM_BYTES = 3_000_000_000L

    fun isPowerSaveMode(context: Context): Boolean {
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
        return powerManager?.isPowerSaveMode == true
    }

    
    fun isLowEndDevice(context: Context): Boolean {
        val manager = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager ?: return false
        if (manager.isLowRamDevice) return true
        val info = ActivityManager.MemoryInfo().also { manager.getMemoryInfo(it) }
        return info.totalMem in 1 until LOW_RAM_BYTES
    }
}

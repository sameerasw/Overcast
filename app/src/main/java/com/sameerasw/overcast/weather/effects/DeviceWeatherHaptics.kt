package com.sameerasw.overcast.weather.effects

import android.content.Context
import android.os.SystemClock
import com.sameerasw.overcast.utils.HapticUtil

class DeviceWeatherHaptics(private val context: Context) : WeatherEffectHaptics {
    private var lastDropMs = 0L

    // Every landing ticks, but never faster than the motor can render distinct taps.
    override fun onDrop(intensity: Float, weight: Float) {
        val now = SystemClock.uptimeMillis()
        if (now - lastDropMs < MIN_DROP_GAP_MS) return
        lastDropMs = now
        HapticUtil.performCustomHaptic(context, (0.22f + 0.22f * intensity) * (0.7f + 0.6f * weight))
    }

    override fun onHail(intensity: Float, weight: Float) =
        HapticUtil.performCustomHaptic(context, (0.4f + 0.2f * intensity) * (0.8f + 0.4f * weight))

    override fun onStrike(intensity: Float) = HapticUtil.performThunderRumble(context, intensity)
}

private const val MIN_DROP_GAP_MS = 40L

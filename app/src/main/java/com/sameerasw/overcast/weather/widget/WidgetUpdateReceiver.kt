package com.sameerasw.overcast.weather.widget

import android.app.AlarmManager
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import com.sameerasw.overcast.weather.WeatherRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Checks whether any Overcast Glance widgets are currently active on the home screen.
 */
fun hasActiveWidgets(context: Context): Boolean {
    val manager = AppWidgetManager.getInstance(context) ?: return false
    val widgetReceivers = listOf(
        WeatherWidgetReceiver::class.java,
        HourlyWidgetReceiver::class.java,
        DailyWidgetReceiver::class.java,
        NowcastWidgetReceiver::class.java,
        AqiWidgetReceiver::class.java,
    )
    return widgetReceivers.any { cls ->
        manager.getAppWidgetIds(ComponentName(context, cls)).isNotEmpty()
    }
}

/**
 * Manages recurring device-wakeup alarms using [AlarmManager.setAndAllowWhileIdle].
 * This allows widget updates to wake up from deep Doze mode on restricted OSs
 * without requiring the restrictive SCHEDULE_EXACT_ALARM permission.
 */
object WidgetAlarmScheduler {
    const val ACTION_WIDGET_UPDATE = "com.sameerasw.overcast.action.WIDGET_UPDATE"
    private const val REQUEST_CODE = 0x574944 // "WID"
    private const val DEFAULT_INTERVAL_MS = 30 * 60 * 1000L // 30 minutes

    fun scheduleNext(context: Context, delayMillis: Long = DEFAULT_INTERVAL_MS) {
        val app = context.applicationContext
        val alarmManager = app.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val intent = Intent(app, WidgetUpdateReceiver::class.java).apply {
            action = ACTION_WIDGET_UPDATE
        }
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        val pendingIntent = PendingIntent.getBroadcast(app, REQUEST_CODE, intent, flags)

        val triggerAt = System.currentTimeMillis() + delayMillis
        try {
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent)
        } catch (_: Exception) {
            // Guard against device-specific restrictions
        }
    }

    fun cancel(context: Context) {
        val app = context.applicationContext
        val alarmManager = app.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val intent = Intent(app, WidgetUpdateReceiver::class.java).apply {
            action = ACTION_WIDGET_UPDATE
        }
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        val pendingIntent = PendingIntent.getBroadcast(app, REQUEST_CODE, intent, flags)
        try {
            alarmManager.cancel(pendingIntent)
        } catch (_: Exception) {
        }
    }
}

/**
 * BroadcastReceiver triggered on:
 * 1. [Intent.ACTION_USER_PRESENT] (Screen unlocked - whitelisted implicit broadcast)
 * 2. [Intent.ACTION_BOOT_COMPLETED] & [Intent.ACTION_MY_PACKAGE_REPLACED]
 * 3. [WidgetAlarmScheduler.ACTION_WIDGET_UPDATE] (Doze-exempt wakeup alarm)
 *
 * Ensures widgets immediately compute daylight and temperatures for the current time
 * from cached hourly forecasts and triggers a stale refresh if connected.
 */
class WidgetUpdateReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val app = context.applicationContext
        if (!hasActiveWidgets(app)) {
            WidgetAlarmScheduler.cancel(app)
            return
        }

        // Re-arm for the next periodic update
        WidgetAlarmScheduler.scheduleNext(app)

        val pendingResult = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.Default).launch {
            try {
                // 1. Immediately redraw all widgets using the time-evaluated hourly forecast
                WeatherWidgetUpdater.updateAllSync(app)

                // 2. If data is stale, attempt a network refresh with a timeout
                if (WeatherRepository.isStale(app)) {
                    val refreshed = withTimeoutOrNull(8000L) {
                        runCatching { WeatherRepository.refresh(app, fallbackToSaved = true) }.getOrDefault(false)
                    } ?: false

                    if (refreshed) {
                        WeatherWidgetUpdater.updateAllSync(app)
                    }
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}


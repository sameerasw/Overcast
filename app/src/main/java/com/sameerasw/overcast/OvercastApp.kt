package com.sameerasw.overcast

import android.app.Application
import android.content.Context
import com.sameerasw.overcast.utils.HapticUtil
import com.sameerasw.overcast.weather.WeatherRepository
import com.sameerasw.overcast.weather.work.WeatherScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class OvercastApp : Application() {
    companion object {
        lateinit var context: Context
            private set
    }

    override fun onCreate() {
        super.onCreate()
        context = applicationContext
        HapticUtil.initialize(this)
        // Warm the cached snapshot and set up background work off the main thread so the first frames aren't competing with it.
        CoroutineScope(SupervisorJob() + Dispatchers.Default).launch {
            WeatherRepository.ensureLoaded(applicationContext)
            WeatherScheduler.schedule(applicationContext, WeatherRepository.config(applicationContext).refreshIntervalMinutes)
        }
    }
}

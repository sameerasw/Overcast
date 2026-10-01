package com.sameerasw.overcast

import android.app.Application
import android.content.Context
import com.sameerasw.overcast.utils.HapticUtil
import com.sameerasw.overcast.weather.WeatherRepository
import com.sameerasw.overcast.weather.work.WeatherScheduler

class OvercastApp : Application() {
    companion object {
        lateinit var context: Context
            private set
    }

    override fun onCreate() {
        super.onCreate()
        context = applicationContext
        HapticUtil.initialize(this)
        WeatherScheduler.schedule(this, WeatherRepository.config(this).refreshIntervalMinutes)
    }
}

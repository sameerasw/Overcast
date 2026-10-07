package com.sameerasw.overcast.ui.activities

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.os.SystemClock
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.sameerasw.overcast.ui.features.weather.WeatherScreen
import com.sameerasw.overcast.weather.WeatherRepository

class WeatherActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        val splash = installSplashScreen()
        val startedAt = SystemClock.uptimeMillis()
        // Hold the splash until the cached weather is ready so the UI doesn't pop from empty to full, with a cap so it can't hang.
        splash.setKeepOnScreenCondition { !WeatherRepository.isLoaded && SystemClock.uptimeMillis() - startedAt < SPLASH_MAX_MS }
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
        )
        super.onCreate(savedInstanceState)
        window.isNavigationBarContrastEnforced = false
        window.isStatusBarContrastEnforced = false
        window.attributes = window.attributes.apply {
            layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
        }
        setContent {
            WeatherScreen(onOpenSettings = { startActivity(Intent(this, SettingsActivity::class.java)) })
        }
    }
}

private const val SPLASH_MAX_MS = 1200L

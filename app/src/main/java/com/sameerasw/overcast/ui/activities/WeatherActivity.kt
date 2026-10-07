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
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.sameerasw.overcast.data.repository.SettingsRepository
import com.sameerasw.overcast.ui.features.onboarding.OnboardingScreen
import com.sameerasw.overcast.ui.features.weather.WeatherScreen
import com.sameerasw.overcast.ui.theme.OvercastTheme
import com.sameerasw.overcast.weather.WeatherRepository

class WeatherActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        val splash = installSplashScreen()
        val startedAt = SystemClock.uptimeMillis()
        // Hold the splash until the cached weather is ready so the UI doesn't pop from empty to full, with a cap so it can't hang.
        val settings = SettingsRepository(this)
        val onboarding = !settings.isOnboardingCompleted()
        splash.setKeepOnScreenCondition { !onboarding && !WeatherRepository.isLoaded && SystemClock.uptimeMillis() - startedAt < SPLASH_MAX_MS }
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
            var onboarded by remember { mutableStateOf(!onboarding) }
            Box(Modifier.fillMaxSize()) {
                if (onboarded) {
                    WeatherScreen(onOpenSettings = { startActivity(Intent(this@WeatherActivity, SettingsActivity::class.java)) })
                }
                AnimatedVisibility(
                    visible = !onboarded,
                    enter = fadeIn() + slideInVertically { it },
                    exit = fadeOut() + slideOutVertically { it },
                ) {
                    OvercastTheme(darkTheme = true) {
                        OnboardingScreen(
                            onFinished = {
                                settings.setOnboardingCompleted(true)
                                onboarded = true
                            },
                        )
                    }
                }
            }
        }
    }
}

private const val SPLASH_MAX_MS = 1200L

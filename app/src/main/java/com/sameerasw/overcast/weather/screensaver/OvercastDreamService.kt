package com.sameerasw.overcast.weather.screensaver

import android.service.dreams.DreamService
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.sameerasw.overcast.data.repository.SettingsRepository
import com.sameerasw.overcast.ui.features.weather.AmbientWeatherScene
import com.sameerasw.overcast.ui.features.weather.rememberWeatherPresentation
import com.sameerasw.overcast.ui.theme.OvercastTheme
import com.sameerasw.overcast.weather.WeatherRepository

class OvercastDreamService : DreamService(), LifecycleOwner, SavedStateRegistryOwner, ViewModelStoreOwner {
    private val lifecycleRegistry = LifecycleRegistry(this)
    private val savedStateController = SavedStateRegistryController.create(this)
    private val store = ViewModelStore()

    override val lifecycle: Lifecycle get() = lifecycleRegistry
    override val savedStateRegistry: SavedStateRegistry get() = savedStateController.savedStateRegistry
    override val viewModelStore: ViewModelStore get() = store

    override fun onCreate() {
        super.onCreate()
        savedStateController.performAttach()
        savedStateController.performRestore(null)
        lifecycleRegistry.currentState = Lifecycle.State.CREATED
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        isInteractive = true
        isFullscreen = true
        isScreenBright = true
        val decor = window.decorView
        decor.setViewTreeLifecycleOwner(this)
        decor.setViewTreeSavedStateRegistryOwner(this)
        decor.setViewTreeViewModelStoreOwner(this)
        setContentView(ComposeView(this).apply { setContent { OvercastTheme { DreamContent(onDismiss = { this@OvercastDreamService.finish() }) } } })
        lifecycleRegistry.currentState = Lifecycle.State.STARTED
    }

    override fun onDreamingStarted() {
        super.onDreamingStarted()
        lifecycleRegistry.currentState = Lifecycle.State.RESUMED
    }

    override fun onDreamingStopped() {
        lifecycleRegistry.currentState = Lifecycle.State.STARTED
        super.onDreamingStopped()
    }

    override fun onDetachedFromWindow() {
        lifecycleRegistry.currentState = Lifecycle.State.DESTROYED
        store.clear()
        super.onDetachedFromWindow()
    }
}

@Composable
private fun DreamContent(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val state by WeatherRepository.state.collectAsState()
    val settings = remember { SettingsRepository(context) }
    LaunchedEffect(Unit) {
        WeatherRepository.ensureLoaded(context)
        if (WeatherRepository.isStale(context)) WeatherRepository.refresh(context)
    }
    AmbientWeatherScene(rememberWeatherPresentation(state.snapshot), settings.isAmbientForecastEnabled(), onDismiss = onDismiss, animateIntro = true)
}

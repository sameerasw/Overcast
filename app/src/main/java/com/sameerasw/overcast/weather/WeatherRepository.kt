package com.sameerasw.overcast.weather

import android.content.Context
import androidx.annotation.Keep
import com.google.gson.Gson
import com.sameerasw.overcast.data.repository.SettingsRepository
import com.sameerasw.overcast.weather.location.DeviceLocationSource
import com.sameerasw.overcast.weather.location.WeatherPlaces
import com.sameerasw.overcast.weather.model.WeatherError
import com.sameerasw.overcast.weather.model.WeatherLocation
import com.sameerasw.overcast.weather.model.WeatherSnapshot
import com.sameerasw.overcast.weather.model.WeatherState
import com.sameerasw.overcast.weather.provider.WeatherProviderException
import com.sameerasw.overcast.weather.share.WeatherContentProvider
import com.sameerasw.overcast.weather.provider.WeatherProviders
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File

object WeatherRepository {
    private const val CACHE_FILE = "weather_cache.json"
    private const val MIN_INTERVAL_MS = 5 * 60_000L
    private const val MAX_LOCATION_AGE_MS = 3 * 60 * 60_000L

    private val gson = Gson()
    private val mutex = Mutex()
    private val _state = MutableStateFlow(WeatherState())
    val state: StateFlow<WeatherState> = _state.asStateFlow()

    private var cache = WeatherCache()
    @Volatile private var loaded = false
    val isLoaded: Boolean get() = loaded

    @Keep
    private data class WeatherCache(
        val snapshot: WeatherSnapshot? = null,
        val lastDeviceLocation: WeatherLocation? = null,
        val lastDeviceLocationAt: Long? = null,
        val notifiedAlertIds: Set<String>? = null,
    )

    fun config(context: Context): WeatherConfig = SettingsWeatherConfig(SettingsRepository(context.applicationContext))

    suspend fun ensureLoaded(context: Context) {
        if (loaded) return
        mutex.withLock {
            if (loaded) return
            cache = withContext(Dispatchers.IO) {
                try {
                    File(context.applicationContext.cacheDir, CACHE_FILE).takeIf { it.exists() }?.readText()
                        ?.let { gson.fromJson(it, WeatherCache::class.java) } ?: WeatherCache()
                } catch (_: Exception) {
                    WeatherCache()
                }
            }
            loaded = true
            _state.update { it.copy(snapshot = cache.snapshot) }
        }
    }

    fun isStale(context: Context): Boolean {
        val snapshot = _state.value.snapshot ?: return true
        val interval = config(context).refreshIntervalMinutes * 60_000L
        return System.currentTimeMillis() - snapshot.updatedAt >= interval
    }

    // Returns true when fresh data was stored.
    suspend fun refresh(context: Context, force: Boolean = false, fallbackToSaved: Boolean = false): Boolean {
        val app = context.applicationContext
        ensureLoaded(app)
        return mutex.withLock {
            val current = _state.value.snapshot
            if (!force && current != null && System.currentTimeMillis() - current.updatedAt < MIN_INTERVAL_MS) {
                return@withLock false
            }
            val config = config(app)
            val provider = WeatherProviders.byId(config.providerId)
            if (provider.requiresApiKey && config.apiKey.isNullOrBlank()) {
                _state.update { it.copy(error = WeatherError.MissingApiKey, loading = false) }
                return@withLock false
            }
            _state.update { it.copy(loading = true) }
            val location = resolveLocation(app, config)
                ?: if (fallbackToSaved) savedFallback(app) else null
            if (location == null) {
                val error = if (config.locationMode == WeatherLocationMode.DEVICE && !DeviceLocationSource.hasPermission(app)) {
                    WeatherError.LocationPermission
                } else {
                    WeatherError.NoLocation
                }
                _state.update { it.copy(error = error, loading = false) }
                return@withLock false
            }
            try {
                val snapshot = provider.fetch(location, config.apiKey)
                cache = cache.copy(snapshot = snapshot)
                persist(app)
                _state.value = WeatherState(snapshot = snapshot)
                WeatherContentProvider.notifyChanged(app)
                true
            } catch (e: WeatherProviderException) {
                val error = when (e.reason) {
                    WeatherProviderException.Reason.INVALID_KEY -> WeatherError.InvalidApiKey
                    WeatherProviderException.Reason.NETWORK -> WeatherError.Network
                    WeatherProviderException.Reason.BAD_RESPONSE -> WeatherError.Unknown(e.message)
                }
                _state.update { it.copy(error = error, loading = false) }
                false
            } catch (e: Exception) {
                _state.update { it.copy(error = WeatherError.Unknown(e.message), loading = false) }
                false
            }
        }
    }

    suspend fun clear(context: Context) {
        ensureLoaded(context)
        mutex.withLock {
            cache = cache.copy(snapshot = null)
            persist(context.applicationContext)
            _state.value = WeatherState()
        }
    }

    // Returns the ids that haven't been announced yet and records them as announced.
    suspend fun takeUnnotifiedAlerts(context: Context, ids: Collection<String>): Set<String> {
        ensureLoaded(context)
        return mutex.withLock {
            val notified = cache.notifiedAlertIds.orEmpty()
            val fresh = ids.filterNot { it in notified }.toSet()
            if (fresh.isNotEmpty()) {
                val activeIds = _state.value.snapshot?.alerts?.map { it.id }?.toSet().orEmpty()
                cache = cache.copy(notifiedAlertIds = (notified.filter { it in activeIds } + fresh).toSet())
                persist(context.applicationContext)
            }
            fresh
        }
    }

    private fun savedFallback(context: Context): WeatherLocation? =
        WeatherPlaces(context).saved().firstOrNull()?.let { WeatherLocation(it.latitude, it.longitude, it.name) }

    private suspend fun resolveLocation(context: Context, config: WeatherConfig): WeatherLocation? =
        when (config.locationMode) {
            WeatherLocationMode.MANUAL -> config.manualLocation
            WeatherLocationMode.DEVICE -> {
                val fresh = DeviceLocationSource.current(context)
                if (fresh != null) {
                    cache = cache.copy(lastDeviceLocation = fresh, lastDeviceLocationAt = System.currentTimeMillis())
                    fresh
                } else {
                    val age = System.currentTimeMillis() - (cache.lastDeviceLocationAt ?: 0L)
                    cache.lastDeviceLocation.takeIf { DeviceLocationSource.hasPermission(context) && age < MAX_LOCATION_AGE_MS }
                }
            }
        }

    private suspend fun persist(context: Context) = withContext(Dispatchers.IO) {
        try {
            File(context.cacheDir, CACHE_FILE).writeText(gson.toJson(cache))
        } catch (_: Exception) {
        }
    }
}

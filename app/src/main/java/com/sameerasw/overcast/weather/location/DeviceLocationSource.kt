package com.sameerasw.overcast.weather.location

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import com.google.android.gms.tasks.Task
import com.sameerasw.overcast.weather.model.WeatherLocation
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume

object DeviceLocationSource {
    private const val TIMEOUT_MS = 10_000L
    private const val MAX_LAST_KNOWN_AGE_MS = 60 * 60_000L

    fun hasPermission(context: Context): Boolean =
        context.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
            context.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED

    // Coarse, one-shot fix. Falls back to the last known location, which is what background refreshes usually get.
    @SuppressLint("MissingPermission")
    suspend fun current(context: Context): WeatherLocation? {
        if (!hasPermission(context)) return null
        val client = LocationServices.getFusedLocationProviderClient(context)
        val tokenSource = CancellationTokenSource()
        val fresh = withTimeoutOrNull(TIMEOUT_MS) {
            try {
                client.getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, tokenSource.token).awaitOrNull()
            } catch (_: SecurityException) {
                null
            }
        }
        tokenSource.cancel()
        // An old last-known fix is likely wherever we were hours ago (often home), so it isn't trusted.
        val location = fresh ?: try {
            client.lastLocation.awaitOrNull()?.takeIf { System.currentTimeMillis() - it.time <= MAX_LAST_KNOWN_AGE_MS }
        } catch (_: SecurityException) {
            null
        }
        return location?.let { WeatherLocation(it.latitude, it.longitude) }
    }

    private suspend fun Task<Location>.awaitOrNull(): Location? = suspendCancellableCoroutine { cont ->
        addOnSuccessListener { if (cont.isActive) cont.resume(it) }
        addOnFailureListener { if (cont.isActive) cont.resume(null) }
        addOnCanceledListener { if (cont.isActive) cont.resume(null) }
    }
}

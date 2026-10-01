package com.sameerasw.overcast.weather.share

import android.content.ContentProvider
import android.content.Context
import android.content.ContentValues
import android.content.pm.PackageManager
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri
import android.os.Bundle
import android.os.SystemClock
import androidx.core.net.toUri
import com.google.gson.Gson
import com.sameerasw.overcast.weather.WeatherRepository
import kotlinx.coroutines.runBlocking

class WeatherContentProvider : ContentProvider() {
    private val gson = Gson()
    @Volatile
    private var lastRefreshAt = -MIN_REFRESH_GAP_MS

    override fun onCreate(): Boolean = true

    override fun query(
        uri: Uri,
        projection: Array<out String>?,
        selection: String?,
        selectionArgs: Array<out String>?,
        sortOrder: String?,
    ): Cursor? {
        val context = context ?: return null
        if (uri.path != PATH_SNAPSHOT) return null
        enforceCaller(context)
        val snapshot = runBlocking {
            WeatherRepository.ensureLoaded(context)
            WeatherRepository.state.value.snapshot ?: run {
                WeatherRepository.refresh(context, fallbackToSaved = true)
                WeatherRepository.state.value.snapshot
            }
        }
        return MatrixCursor(arrayOf(COLUMN_JSON, COLUMN_UPDATED_AT, COLUMN_SCHEMA)).apply {
            snapshot?.let { addRow(arrayOf(gson.toJson(it), it.updatedAt, SCHEMA_VERSION)) }
            setNotificationUri(context.contentResolver, SNAPSHOT_URI)
        }
    }

    // call() isn't covered by the manifest permission on every API level, so the caller is checked here too.
    override fun call(method: String, arg: String?, extras: Bundle?): Bundle? {
        val context = context ?: return null
        if (method != METHOD_REFRESH) return null
        enforceCaller(context)
        val now = SystemClock.elapsedRealtime()
        if (now - lastRefreshAt < MIN_REFRESH_GAP_MS) return Bundle().apply { putBoolean(KEY_SUCCESS, false) }
        lastRefreshAt = now
        val refreshed = runBlocking { WeatherRepository.refresh(context, force = true, fallbackToSaved = true) }
        return Bundle().apply { putBoolean(KEY_SUCCESS, refreshed) }
    }

    // Only Essentials, and only while it holds the permission, may read weather.
    private fun enforceCaller(context: Context) {
        if (callingPackage != ALLOWED_CALLER_PACKAGE) {
            throw SecurityException("Caller $callingPackage isn't allowed to read weather")
        }
        if (context.checkCallingPermission(PERMISSION_READ_WEATHER) != PackageManager.PERMISSION_GRANTED) {
            throw SecurityException("Missing $PERMISSION_READ_WEATHER")
        }
    }

    override fun getType(uri: Uri): String? = null

    override fun insert(uri: Uri, values: ContentValues?): Uri? = null

    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int = 0

    override fun update(uri: Uri, values: ContentValues?, selection: String?, selectionArgs: Array<out String>?): Int = 0

    companion object {
        val SNAPSHOT_URI: Uri = "content://com.sameerasw.overcast.weather/snapshot".toUri()

        fun notifyChanged(context: Context) {
            context.contentResolver.notifyChange(SNAPSHOT_URI, null)
        }

        const val ALLOWED_CALLER_PACKAGE = "com.sameerasw.essentials"
        private const val MIN_REFRESH_GAP_MS = 30_000L
        const val PERMISSION_READ_WEATHER = "com.sameerasw.overcast.permission.READ_WEATHER"
        const val PATH_SNAPSHOT = "/snapshot"
        const val METHOD_REFRESH = "refresh"
        const val COLUMN_JSON = "json"
        const val COLUMN_UPDATED_AT = "updated_at"
        const val COLUMN_SCHEMA = "schema"
        const val KEY_SUCCESS = "success"
        const val SCHEMA_VERSION = 1
    }
}

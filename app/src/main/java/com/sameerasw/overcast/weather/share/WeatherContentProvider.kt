package com.sameerasw.overcast.weather.share

import android.content.ContentProvider
import android.content.ContentValues
import android.content.pm.PackageManager
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri
import android.os.Bundle
import com.google.gson.Gson
import com.sameerasw.overcast.weather.WeatherRepository
import kotlinx.coroutines.runBlocking

class WeatherContentProvider : ContentProvider() {
    private val gson = Gson()

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
        val snapshot = runBlocking {
            WeatherRepository.ensureLoaded(context)
            WeatherRepository.state.value.snapshot ?: run {
                WeatherRepository.refresh(context, fallbackToSaved = true)
                WeatherRepository.state.value.snapshot
            }
        }
        return MatrixCursor(arrayOf(COLUMN_JSON, COLUMN_UPDATED_AT, COLUMN_SCHEMA)).apply {
            snapshot?.let { addRow(arrayOf(gson.toJson(it), it.updatedAt, SCHEMA_VERSION)) }
        }
    }

    // call() isn't covered by the manifest permission on every API level, so it's checked here too.
    override fun call(method: String, arg: String?, extras: Bundle?): Bundle? {
        val context = context ?: return null
        if (method != METHOD_REFRESH) return null
        if (context.checkCallingPermission(PERMISSION_READ_WEATHER) != PackageManager.PERMISSION_GRANTED) {
            throw SecurityException("Missing $PERMISSION_READ_WEATHER")
        }
        val refreshed = runBlocking { WeatherRepository.refresh(context, force = true, fallbackToSaved = true) }
        return Bundle().apply { putBoolean(KEY_SUCCESS, refreshed) }
    }

    override fun getType(uri: Uri): String? = null

    override fun insert(uri: Uri, values: ContentValues?): Uri? = null

    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int = 0

    override fun update(uri: Uri, values: ContentValues?, selection: String?, selectionArgs: Array<out String>?): Int = 0

    companion object {
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

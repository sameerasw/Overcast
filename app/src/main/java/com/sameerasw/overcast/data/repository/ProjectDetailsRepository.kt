/*
 * Copyright (c) 2026 sameerasw.com
 * License: MIT License
 *
 * Feature Module: Data & Repository Layer
 * File: ProjectDetailsRepository.kt
 * Description: Repository to fetch and cache project details from sameerasw.com.
 */

package com.sameerasw.overcast.data.repository

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.sameerasw.overcast.domain.model.github.ProjectDetails
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

class ProjectDetailsRepository(context: Context) {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val gson = Gson()

    suspend fun getProjectDetails(projectId: String = "overcast"): ProjectDetails? =
        withContext(Dispatchers.IO) {
            val cachedJson = prefs.getString(KEY_PROJECT_DETAILS_CACHE, null)
            val lastFetchTime = prefs.getLong(KEY_PROJECT_DETAILS_LAST_FETCH, 0L)
            val now = System.currentTimeMillis()

            val isCacheStale = (now - lastFetchTime) > CACHE_EXPIRATION_MS

            if (!cachedJson.isNullOrEmpty() && !isCacheStale) {
                try {
                    val type = object : TypeToken<Map<String, ProjectDetails>>() {}.type
                    val cachedMap: Map<String, ProjectDetails>? = gson.fromJson(cachedJson, type)
                    if (cachedMap?.containsKey(projectId) == true) {
                        return@withContext cachedMap[projectId]
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            // Fetch remote project details
            try {
                val url = URL(PROJECT_DETAILS_URL)
                val connection =
                    (url.openConnection() as HttpURLConnection).apply {
                        connectTimeout = 5000
                        readTimeout = 5000
                        requestMethod = "GET"
                        setRequestProperty("User-Agent", "Overcast-Android")
                    }

                if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                    val response = connection.inputStream.bufferedReader().use { it.readText() }
                    val type = object : TypeToken<Map<String, ProjectDetails>>() {}.type
                    val freshMap: Map<String, ProjectDetails>? = gson.fromJson(response, type)

                    if (freshMap != null) {
                        prefs.edit()
                            .putString(KEY_PROJECT_DETAILS_CACHE, response)
                            .putLong(KEY_PROJECT_DETAILS_LAST_FETCH, now)
                            .apply()
                        return@withContext freshMap[projectId]
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }

            // Fallback to cache if available
            if (!cachedJson.isNullOrEmpty()) {
                try {
                    val type = object : TypeToken<Map<String, ProjectDetails>>() {}.type
                    val cachedMap: Map<String, ProjectDetails>? = gson.fromJson(cachedJson, type)
                    if (cachedMap?.containsKey(projectId) == true) {
                        return@withContext cachedMap[projectId]
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            null
        }

    companion object {
        private const val PREFS_NAME = "project_details_prefs"
        private const val KEY_PROJECT_DETAILS_CACHE = "project_details_cache"
        private const val KEY_PROJECT_DETAILS_LAST_FETCH = "project_details_last_fetch"
        private const val CACHE_EXPIRATION_MS = 24 * 60 * 60 * 1000L // 24 hours
        private const val PROJECT_DETAILS_URL = "https://sameerasw.com/project-details.json"

        @Volatile
        private var instance: ProjectDetailsRepository? = null

        fun getInstance(context: Context): ProjectDetailsRepository {
            return instance ?: synchronized(this) {
                instance ?: ProjectDetailsRepository(context.applicationContext).also { instance = it }
            }
        }
    }
}

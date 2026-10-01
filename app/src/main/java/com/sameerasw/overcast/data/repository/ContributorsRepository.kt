/*
 * Copyright (c) 2026 sameerasw.com
 * License: MIT License
 *
 * Feature Module: Data & Repository Layer
 * File: ContributorsRepository.kt
 * Description: Data repository for fetching and caching repository contributors.
 */

package com.sameerasw.overcast.data.repository

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.sameerasw.overcast.domain.model.github.GitHubContributor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

class ContributorsRepository(context: Context) {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val gson = Gson()

    suspend fun getContributors(forceRefresh: Boolean = false): List<GitHubContributor> =
        withContext(Dispatchers.IO) {
            val cachedJson = prefs.getString(KEY_CONTRIBUTORS_CACHE, null)
            val lastFetchTime = prefs.getLong(KEY_CONTRIBUTORS_LAST_FETCH, 0L)
            val now = System.currentTimeMillis()

            val isCacheStale = (now - lastFetchTime) > CACHE_EXPIRATION_MS

            if (!forceRefresh && !cachedJson.isNullOrEmpty() && !isCacheStale) {
                try {
                    val type = object : TypeToken<List<GitHubContributor>>() {}.type
                    val cachedList: List<GitHubContributor>? = gson.fromJson(cachedJson, type)
                    if (!cachedList.isNullOrEmpty()) {
                        return@withContext cachedList
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            // Direct GitHub API
            try {
                val url = URL(GITHUB_API_URL)
                val connection =
                    (url.openConnection() as HttpURLConnection).apply {
                        connectTimeout = 5000
                        readTimeout = 5000
                        requestMethod = "GET"
                        setRequestProperty("Accept", "application/vnd.github+json")
                        setRequestProperty("User-Agent", "Overcast-Android")
                    }

                if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                    val response = connection.inputStream.bufferedReader().use { it.readText() }
                    val type = object : TypeToken<List<GitHubContributor>>() {}.type
                    val rawList: List<GitHubContributor>? = gson.fromJson(response, type)

                    if (!rawList.isNullOrEmpty()) {
                        val filteredList =
                            rawList
                                .filter {
                                    !it.login.equals("sameerasw", ignoreCase = true) &&
                                        !it.login.endsWith("[bot]", ignoreCase = true)
                                }
                                .take(15)

                        val json = gson.toJson(filteredList)
                        prefs.edit()
                            .putString(KEY_CONTRIBUTORS_CACHE, json)
                            .putLong(KEY_CONTRIBUTORS_LAST_FETCH, now)
                            .apply()
                        return@withContext filteredList
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }

            // Return cached data if available on network failure
            if (!cachedJson.isNullOrEmpty()) {
                try {
                    val type = object : TypeToken<List<GitHubContributor>>() {}.type
                    val cachedList: List<GitHubContributor>? = gson.fromJson(cachedJson, type)
                    if (!cachedList.isNullOrEmpty()) {
                        return@withContext cachedList
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            emptyList()
        }

    companion object {
        private const val PREFS_NAME = "contributors_prefs"
        private const val KEY_CONTRIBUTORS_CACHE = "contributors_cache"
        private const val KEY_CONTRIBUTORS_LAST_FETCH = "contributors_last_fetch"
        private const val CACHE_EXPIRATION_MS = 24 * 60 * 60 * 1000L // 24 hours
        private const val GITHUB_API_URL = "https://api.github.com/repos/sameerasw/Overcast/contributors?per_page=30"

        @Volatile
        private var instance: ContributorsRepository? = null

        fun getInstance(context: Context): ContributorsRepository {
            return instance ?: synchronized(this) {
                instance ?: ContributorsRepository(context.applicationContext).also { instance = it }
            }
        }
    }
}

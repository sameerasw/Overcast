/*
 * Copyright (c) 2026 sameerasw.com
 * License: MIT License
 *
 * Feature Module: Domain Layer - GitHub
 * File: GitHubContributor.kt
 * Description: Data model representing a repository contributor.
 */

package com.sameerasw.overcast.domain.model.github

import com.google.gson.annotations.SerializedName

data class GitHubContributor(
    @SerializedName("login")
    val login: String,
    @SerializedName("avatar_url")
    val avatarUrl: String,
    @SerializedName("html_url")
    val htmlUrl: String,
    @SerializedName("contributions")
    val contributions: Int = 0,
)

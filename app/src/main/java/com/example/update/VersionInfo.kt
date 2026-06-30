package com.example.update

data class VersionInfo(
    val latestVersion: String = "",
    val versionCode: Long = 0,
    val minVersion: String = "",
    val forceUpdate: Boolean = false,
    val apkUrl: String = "",
    val releaseNotes: String = "",
    val published: Boolean = true
)

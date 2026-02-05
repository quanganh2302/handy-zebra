package com.example.civn26t01.helper

data class SettingsData(
    val deviceName: String,
    val serverIp: String,
    val serverPort: String,
    val useHttps: Boolean,
    val baseUrl: String
)
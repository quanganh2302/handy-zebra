package com.example.civn26t01.core.settings

import android.content.Context
import android.content.SharedPreferences

object SettingsManager {
    private const val PREFS_NAME = "app_settings"
    private const val KEY_SERVER_IP = "server_ip"
    private const val KEY_SERVER_PORT = "server_port"
    private const val KEY_USE_HTTPS = "use_https"
    private const val KEY_DEVICE_NAME = "device_name"

    // Default values
    private const val DEFAULT_SERVER_IP = "192.168.50.29"
    private const val DEFAULT_SERVER_PORT = "5000"
    private const val DEFAULT_USE_HTTPS = false
    private const val DEFAULT_DEVICE_NAME = "Device1"
    private const val KEY_PRINTERS = "printers"

    // Use Gson for array
    private val gson = com.google.gson.Gson()

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    // Getters
    fun getServerIp(context: Context): String {
        return getPrefs(context).getString(KEY_SERVER_IP, DEFAULT_SERVER_IP) ?: DEFAULT_SERVER_IP
    }

    fun getServerPort(context: Context): String {
        return getPrefs(context).getString(KEY_SERVER_PORT, DEFAULT_SERVER_PORT) ?: DEFAULT_SERVER_PORT
    }

    fun getUseHttps(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_USE_HTTPS, DEFAULT_USE_HTTPS)
    }

    fun getDeviceName(context: Context): String {
        return getPrefs(context).getString(KEY_DEVICE_NAME, DEFAULT_DEVICE_NAME) ?: DEFAULT_DEVICE_NAME
    }

    fun getPrinters(context: Context): List<String> {
        val json = getPrefs(context).getString(KEY_PRINTERS, null)
        if (json.isNullOrEmpty()) {
            return listOf("Microsoft Print to PDF") // Default minimum 1 printer
        }
        val type = object : com.google.gson.reflect.TypeToken<List<String>>() {}.type
        return gson.fromJson(json, type)
    }

    // Setters
    fun setServerIp(context: Context, ip: String) {
        getPrefs(context).edit().putString(KEY_SERVER_IP, ip).apply()
    }

    fun setServerPort(context: Context, port: String) {
        getPrefs(context).edit().putString(KEY_SERVER_PORT, port).apply()
    }

    fun setUseHttps(context: Context, useHttps: Boolean) {
        getPrefs(context).edit().putBoolean(KEY_USE_HTTPS, useHttps).apply()
    }

    fun setDeviceName(context: Context, deviceName: String) {
        getPrefs(context).edit().putString(KEY_DEVICE_NAME, deviceName).apply()
    }

    fun setPrinters(context: Context, printers: List<String>) {
        val json = gson.toJson(printers)
        getPrefs(context).edit().putString(KEY_PRINTERS, json).apply()
    }

    // Get full base URL
    fun getBaseUrl(context: Context): String {
        val protocol = if (getUseHttps(context)) "https" else "http"
        val ip = getServerIp(context)
        val port = getServerPort(context)
        return "$protocol://$ip:$port"
    }

    // Reset to defaults
    fun resetToDefaults(context: Context) {
        getPrefs(context).edit().clear().apply()
    }
}
package com.example.civn26t01.helper

import android.content.Context
import androidx.lifecycle.LiveData
import com.example.civn26t01.core.settings.SettingsManager

class SettingsLiveData private constructor(
    private val context: Context
) : LiveData<SettingsData>() {

    companion object {
        @Volatile
        private var INSTANCE: SettingsLiveData? = null

        fun getInstance(context: Context): SettingsLiveData {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: SettingsLiveData(context.applicationContext).also {
                    INSTANCE = it
                }
            }
        }
    }

    init {
        // Load initial value
        value = loadSettings()
    }

    private fun loadSettings() = SettingsData(
        deviceName = SettingsManager.getDeviceName(context),
        serverIp = SettingsManager.getServerIp(context),
        serverPort = SettingsManager.getServerPort(context),
        useHttps = SettingsManager.getUseHttps(context),
        baseUrl = SettingsManager.getBaseUrl(context)
    )

    fun refresh() {
        value = loadSettings()
    }
}
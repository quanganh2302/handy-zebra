package com.example.civn26t01.core.network

import android.content.Context
import android.util.Log
import com.example.civn26t01.core.settings.SettingsManager
import com.microsoft.signalr.HubConnection
import com.microsoft.signalr.HubConnectionBuilder
import com.microsoft.signalr.HubConnectionState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

enum class SignalRStatus {
    DISCONNECTED,
    CONNECTING,
    CONNECTED
}

class SignalRConnectionManager(private val context: Context) {

    private var hubConnection: HubConnection? = null

    private val _connectionStatus = MutableStateFlow(SignalRStatus.DISCONNECTED)
    val connectionStatus: StateFlow<SignalRStatus> = _connectionStatus.asStateFlow()

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val connectMutex = Mutex()

    @Volatile
    private var isIntentionalDisconnect = false

    // ─────────────────────────────────────────────
    // Public API
    // ─────────────────────────────────────────────

    fun start() {
        isIntentionalDisconnect = false

        if (hubConnection?.connectionState == HubConnectionState.CONNECTED ||
            _connectionStatus.value == SignalRStatus.CONNECTING
        ) return

        val baseUrl = SettingsManager.getBaseUrl(context)
        val hubUrl = "${baseUrl.trimEnd('/')}/uihub" // ← map với app.MapHub<UiHub>("/uihub") trong Program.cs

        // ── Debug URL ──────────────────────────────────────────
        Log.d("SignalR", "=== CONNECTION INFO ===")
        Log.d("SignalR", "Base URL  : $baseUrl")
        Log.d("SignalR", "Hub URL   : $hubUrl")
        Log.d("SignalR", "Negotiate : $hubUrl/negotiate")
        Log.d("SignalR", "========================")

        hubConnection = HubConnectionBuilder.create(hubUrl).build()
        hubConnection?.onClosed { error ->
            Log.w("SignalR", "Connection closed. Error: ${error?.message}")
            if (!isIntentionalDisconnect) {
                _connectionStatus.value = SignalRStatus.DISCONNECTED
                scheduleReconnect()
            }
        }

        connect()
    }

    fun stop() {
        isIntentionalDisconnect = true
        _connectionStatus.value = SignalRStatus.DISCONNECTED
        try {
            hubConnection?.stop()
            Log.d("SignalR", "Connection stopped intentionally")
        } catch (e: Exception) {
            Log.e("SignalR", "Error stopping connection: ${e.message}")
        }
        hubConnection = null
    }

    // ─────────────────────────────────────────────
    // Internal logic
    // ─────────────────────────────────────────────

    private fun connect() {
        if (isIntentionalDisconnect) return

        scope.launch {
            connectMutex.withLock {
                // Double-check inside the lock
                if (isIntentionalDisconnect ||
                    _connectionStatus.value == SignalRStatus.CONNECTING ||
                    hubConnection?.connectionState == HubConnectionState.CONNECTED
                ) return@withLock

                _connectionStatus.value = SignalRStatus.CONNECTING
                Log.d("SignalR", "Attempting to connect...")

                hubConnection?.start()?.subscribe(
                    {
                        // onComplete
                        _connectionStatus.value = SignalRStatus.CONNECTED
                        Log.d("SignalR", "=== CONNECTED ===")
                        Log.d("SignalR", "State: ${hubConnection?.connectionState}")
                        Log.d("SignalR", "Connection ID: ${hubConnection?.connectionId}")
                    },
                    { error ->
                        // onError - log chi tiết lỗi
                        Log.e("SignalR", "=== CONNECTION FAILED ===")
                        Log.e("SignalR", "Error type   : ${error::class.java.simpleName}")
                        Log.e("SignalR", "Error message: ${error.message}")
                        Log.e("SignalR", "Cause        : ${error.cause?.message}")
                        Log.e("SignalR", "Stack trace  : ${Log.getStackTraceString(error)}")
                        _connectionStatus.value = SignalRStatus.DISCONNECTED
                        scheduleReconnect()
                    }
                )
            }
        }
    }

    private fun scheduleReconnect() {
        scope.launch {
            if (isIntentionalDisconnect) return@launch

            Log.d("SignalR", "Reconnecting in 5 seconds...")
            delay(5_000)

            if (!isIntentionalDisconnect &&
                _connectionStatus.value != SignalRStatus.CONNECTING &&
                hubConnection?.connectionState != HubConnectionState.CONNECTED
            ) {
                connect()
            }
        }
    }
}
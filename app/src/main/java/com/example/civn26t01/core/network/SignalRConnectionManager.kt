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

        connect()
    }

    fun stop() {
        isIntentionalDisconnect = true
        _connectionStatus.value = SignalRStatus.DISCONNECTED
        val conn = hubConnection
        hubConnection = null // Null out first to prevent reuse of a corrupt instance
        try {
            if (conn?.connectionState == HubConnectionState.CONNECTED ||
                conn?.connectionState == HubConnectionState.CONNECTING
            ) {
                conn.stop()
            }
            Log.d("SignalR", "Connection stopped intentionally")
        } catch (e: Exception) {
            Log.e("SignalR", "Error stopping connection: ${e.message}")
        }
    }

    // ─────────────────────────────────────────────
    // Internal logic
    // ─────────────────────────────────────────────

    private fun buildNewConnection(): HubConnection {
        val baseUrl = SettingsManager.getBaseUrl(context)
        val hubUrl = "${baseUrl.trimEnd('/')}/uihub"

        Log.d("SignalR", "=== CONNECTION INFO ===")
        Log.d("SignalR", "Base URL  : $baseUrl")
        Log.d("SignalR", "Hub URL   : $hubUrl")
        Log.d("SignalR", "Negotiate : $hubUrl/negotiate")
        Log.d("SignalR", "========================")

        val conn = HubConnectionBuilder.create(hubUrl).build()
        conn.onClosed { error ->
            Log.w("SignalR", "Connection closed. Error: ${error?.message}")
            if (!isIntentionalDisconnect) {
                _connectionStatus.value = SignalRStatus.DISCONNECTED
                scheduleReconnect()
            }
        }
        return conn
    }

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

                // Always create a fresh HubConnection instance.
                // A failed connection leaves the instance in a corrupt state
                // (transport=null internally) so it CANNOT be safely reused —
                // calling start() or stop() on it causes a NullPointerException
                // inside the SignalR library (HubConnection.java:448).
                hubConnection = buildNewConnection()
                Log.d("SignalR", "Attempting to connect...")

                try {
                    hubConnection?.start()?.subscribe(
                        {
                            // onComplete
                            _connectionStatus.value = SignalRStatus.CONNECTED
                            Log.d("SignalR", "=== CONNECTED ===")
                            Log.d("SignalR", "State: ${hubConnection?.connectionState}")
                            Log.d("SignalR", "Connection ID: ${hubConnection?.connectionId}")
                        },
                        { error ->
                            // onError — do NOT call hubConnection.stop() here!
                            // The transport is null at this point (connection never
                            // established), so stop() throws NullPointerException.
                            Log.e("SignalR", "=== CONNECTION FAILED ===")
                            Log.e("SignalR", "Error type   : ${error::class.java.simpleName}")
                            Log.e("SignalR", "Error message: ${error.message}")
                            Log.e("SignalR", "Cause        : ${error.cause?.message}")
                            Log.e("SignalR", "Stack trace  : ${Log.getStackTraceString(error)}")
                            _connectionStatus.value = SignalRStatus.DISCONNECTED
                            hubConnection = null // Discard the corrupt instance
                            scheduleReconnect()
                        }
                    )
                } catch (e: Exception) {
                    // Catch any unexpected exception from subscribe() itself
                    Log.e("SignalR", "Unexpected error during connect: ${e.message}")
                    _connectionStatus.value = SignalRStatus.DISCONNECTED
                    hubConnection = null
                    scheduleReconnect()
                }
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
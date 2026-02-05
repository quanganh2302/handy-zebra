package com.example.civn26t01.core.scanner

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.util.Log
import com.example.civn26t01.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

object ScannerConfig {
    private const val TAG = "ScannerConfig"

    // Create a coroutine scope for this object
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private val _scanEventFlow = MutableSharedFlow<ScanEvent>(
        replay = 0,
        extraBufferCapacity = 1
    )
    val scanEventFlow = _scanEventFlow.asSharedFlow()

    private var receiver: BroadcastReceiver? = null
    private var isInitialized = false

    suspend fun initialize(context: Context) {
        if (isInitialized) {
            Log.d(TAG, "Scanner already initialized")
            return
        }

        try {
            receiver = object : BroadcastReceiver() {
                override fun onReceive(context: Context?, intent: Intent?) {
                    // Launch coroutine to handle intent
                    scope.launch {
                        intent?.let { handleIntent(it) }
                    }
                }
            }

            val filter = IntentFilter().apply {
                addCategory(Intent.CATEGORY_DEFAULT)
                addAction(context.getString(R.string.activity_intent_filter_action))
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.registerReceiver(
                    receiver,
                    filter,
                    Context.RECEIVER_EXPORTED
                )
            } else {
                context.registerReceiver(receiver, filter)
            }

            isInitialized = true
            Log.d(TAG, "Scanner initialized successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize scanner", e)
            _scanEventFlow.emit(ScanEvent.Failed("Initialization failed: ${e.message}"))
        }
    }

    private suspend fun handleIntent(intent: Intent) {
        try {
            val data = intent.getStringExtra("com.symbol.datawedge.data_string")
            val codeType = intent.getStringExtra("com.symbol.datawedge.label_type")

            Log.d(TAG, "Received scan data: $data, type: $codeType")

            if (data != null) {
                _scanEventFlow.emit(ScanEvent.Success(data, codeType ?: "UNKNOWN"))
            } else {
                _scanEventFlow.emit(ScanEvent.Failed("No scan data received"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error handling scan intent", e)
            _scanEventFlow.emit(ScanEvent.Failed(e.message ?: "Unknown error"))
        }
    }

    fun cleanup(context: Context) {
        try {
            if (isInitialized) {
                receiver?.let { context.unregisterReceiver(it) }
                isInitialized = false
                Log.d(TAG, "Scanner cleaned up")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error during cleanup", e)
        }
    }
}
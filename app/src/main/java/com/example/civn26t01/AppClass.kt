package com.example.civn26t01

import android.app.Application
import android.util.Log
import io.reactivex.rxjava3.exceptions.UndeliverableException
import io.reactivex.rxjava3.plugins.RxJavaPlugins
import java.io.IOException
import java.net.SocketException
import java.net.SocketTimeoutException

class AppClass : Application() {

    override fun onCreate() {
        super.onCreate()
        setupRxJavaErrorHandler()
    }

    /**
     * The SignalR Java client (com.microsoft.signalr) uses RxJava 3 internally.
     * When a connection fails (e.g. SocketTimeoutException), SignalR's internal
     * error-handling chain calls stop() on a null transport object, which throws
     * a NullPointerException on the OkHttp thread. Since this exception is thrown
     * inside an RxJava `onComplete` callback after the chain has already terminated,
     * RxJava 3 cannot deliver it to any subscriber — it becomes an
     * "Undeliverable Exception" and by default crashes the app.
     *
     * This handler intercepts those undeliverable exceptions and logs them instead
     * of crashing.
     *
     * See: https://github.com/ReactiveX/RxJava/wiki/What's-different-in-3.0#error-handling
     */
    private fun setupRxJavaErrorHandler() {
        RxJavaPlugins.setErrorHandler { throwable ->
            val cause = if (throwable is UndeliverableException) throwable.cause else throwable

            when (cause) {
                // Network errors that come in after the subscriber has already finished
                is IOException,
                is SocketException,
                is SocketTimeoutException -> {
                    Log.w("RxJava", "Ignored network UndeliverableException: ${cause.message}")
                }

                // The exact bug from SignalR: NPE calling Transport.stop()
                is NullPointerException -> {
                    // Only suppress if it looks like the known SignalR bug
                    val stackTrace = cause.stackTraceToString()
                    if (stackTrace.contains("HubConnection") || stackTrace.contains("signalr")) {
                        Log.w("RxJava", "Suppressed SignalR internal NPE (known library bug): ${cause.message}")
                    } else {
                        // Unknown NPE — log as error but still don't crash
                        Log.e("RxJava", "Undeliverable NullPointerException (unknown source)", cause)
                    }
                }

                else -> {
                    // Other undeliverable errors — log but don't crash
                    Log.e("RxJava", "Undeliverable exception: ${cause?.message}", cause)
                }
            }
        }
    }
}

package com.example.civn26t01.ui.activities

import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatDelegate
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.lifecycleScope
import com.example.civn26t01.R
import com.example.civn26t01.core.scanner.ScanViewModel
import com.example.civn26t01.core.scanner.ScannerConfig
import com.example.civn26t01.ui.fragments.HomeFragment
import kotlinx.coroutines.launch
import com.example.civn26t01.core.network.SignalRConnectionManager
import com.example.civn26t01.core.network.SignalRStatus
import com.example.civn26t01.ui.navigation.HeaderFragment
import com.example.civn26t01.ui.utils.ToastManager

class MainActivity : BaseActivity() {
    private lateinit var drawerLayout: DrawerLayout
    private val scanViewModel: ScanViewModel by viewModels()
    private lateinit var signalRManager: SignalRConnectionManager
    private var lastConnectionState: SignalRStatus? = null

    override fun hasDrawer(): Boolean = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)

        drawerLayout = findViewById(R.id.drawerLayout)
        attachHeader()

        // Initialize scanner
        lifecycleScope.launch {
            ScannerConfig.initialize(this@MainActivity)

            // Forward scan events to ViewModel
            ScannerConfig.scanEventFlow.collect { event ->
                scanViewModel.emitScanEvent(event)
            }
        }

        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, HomeFragment())
                .commit()
        }

        signalRManager = SignalRConnectionManager(this)
        signalRManager.start()

        lifecycleScope.launch {
            signalRManager.connectionStatus.collect { status ->
                val headerFragment = supportFragmentManager.findFragmentById(R.id.headerFragment) as? HeaderFragment
                headerFragment?.updateConnectionStatus(status == SignalRStatus.CONNECTED)

                if (status == SignalRStatus.DISCONNECTED && lastConnectionState == SignalRStatus.CONNECTED) {
                    ToastManager.error(this@MainActivity, "Mất kết nối với máy chủ RPA")
                } else if (status == SignalRStatus.CONNECTED && lastConnectionState == SignalRStatus.DISCONNECTED) {
                    ToastManager.success(this@MainActivity, "Đã kết nối máy chủ RPA")
                }
                lastConnectionState = status
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        signalRManager.stop()
        ScannerConfig.cleanup(this)
    }

    override fun onMenuClicked() {
        drawerLayout.open()
    }

    override fun onAfterDrawerNavigate() {
        drawerLayout.closeDrawers()
    }
}
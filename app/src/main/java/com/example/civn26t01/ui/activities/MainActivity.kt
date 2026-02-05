package com.example.civn26t01.ui.activities

import android.os.Bundle
import androidx.activity.viewModels
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.lifecycleScope
import com.example.civn26t01.R
import com.example.civn26t01.core.scanner.ScanViewModel
import com.example.civn26t01.core.scanner.ScannerConfig
import com.example.civn26t01.ui.fragments.HomeFragment
import kotlinx.coroutines.launch

class MainActivity : BaseActivity() {
    private lateinit var drawerLayout: DrawerLayout
    private val scanViewModel: ScanViewModel by viewModels()

    override fun hasDrawer(): Boolean = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

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
    }

    override fun onDestroy() {
        super.onDestroy()
        ScannerConfig.cleanup(this)
    }

    override fun onMenuClicked() {
        drawerLayout.open()
    }

    override fun onAfterDrawerNavigate() {
        drawerLayout.closeDrawers()
    }
}
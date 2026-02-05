package com.example.civn26t01.ui.activities

import android.content.Context
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.civn26t01.R
import com.example.civn26t01.ui.navigation.AppDrawerNavigator
import com.example.civn26t01.ui.navigation.DrawerFragment
import com.example.civn26t01.ui.navigation.DrawerNavigator
import com.example.civn26t01.ui.navigation.DrawerPage
import com.example.civn26t01.ui.navigation.HeaderFragment
import com.example.civn26t01.ui.utils.LanguageManager

abstract class BaseActivity : AppCompatActivity(), HeaderFragment.HeaderListener, DrawerFragment.DrawerListener {

    protected lateinit var drawerNavigator: DrawerNavigator

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        drawerNavigator = AppDrawerNavigator(this)
    }

    open fun hasDrawer(): Boolean = false

    override fun onMenuClicked() {
        // Activity có drawer sẽ override
    }

    override fun onDrawerItemSelected(action: DrawerPage) {
        drawerNavigator.navigate(action)
        onAfterDrawerNavigate()
    }

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LanguageManager.applyLanguage(newBase))
    }

    override fun onLanguageChanged(lang: String) {
        LanguageManager.setLanguage(this, lang)
        recreate()
    }

    protected fun attachHeader() {
        supportFragmentManager.beginTransaction()
            .replace(R.id.headerFragment, HeaderFragment())
            .commit()
    }

    open fun onAfterDrawerNavigate() {

    }
}
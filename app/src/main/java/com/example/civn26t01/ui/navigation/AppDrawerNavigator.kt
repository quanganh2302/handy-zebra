package com.example.civn26t01.ui.navigation

import androidx.fragment.app.FragmentActivity
import com.example.civn26t01.R
import com.example.civn26t01.ui.fragments.SettingsFragment
import com.example.civn26t01.ui.fragments.HomeFragment

class AppDrawerNavigator(
    private val activity: FragmentActivity
) : DrawerNavigator {

    override fun navigate(action: DrawerPage) {
        when (action) {
            DrawerPage.HOME ->
                activity.supportFragmentManager.beginTransaction()
                    .replace(R.id.fragment_container, HomeFragment())
                    .commit()

            DrawerPage.SETTINGS ->
                activity.supportFragmentManager.beginTransaction()
                    .replace(R.id.fragment_container, SettingsFragment())
                    .commit()

            DrawerPage.EXIT ->
                activity.finish()
        }
    }
}

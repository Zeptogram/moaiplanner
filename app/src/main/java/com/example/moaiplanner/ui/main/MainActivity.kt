package com.example.moaiplanner.ui.main


import android.annotation.SuppressLint
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.view.menu.MenuBuilder
import androidx.appcompat.widget.Toolbar
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.NavigationUI
import androidx.navigation.ui.setupWithNavController
import com.example.moaiplanner.R
import com.example.moaiplanner.data.repository.settings.SettingsRepository
import com.example.moaiplanner.model.SettingsViewModel
import com.example.moaiplanner.model.SettingsViewModelFactory
import com.example.moaiplanner.util.NavigationHelper
import com.example.moaiplanner.util.Utils
import com.google.android.material.bottomnavigation.BottomNavigationView


class MainActivity : AppCompatActivity() {

    private lateinit var navHostFragment : NavHostFragment
    private lateinit var settingsViewModel: SettingsViewModel
    private lateinit var settingsRepository: SettingsRepository
    private lateinit var navController: NavController
    private lateinit var bottomNav: BottomNavigationView


    @SuppressLint("RestrictedApi")
    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)
        val toolbar = findViewById<Toolbar>(R.id.topAppBar)


        navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment

        navController = navHostFragment.navController
        //navController.popBackStack(R.id.welcomeFragment, true)

        // Leva la freccia per il back
        val appBarConfiguration: AppBarConfiguration = AppBarConfiguration.Builder(
            R.id.noteFragment, R.id.tomatoFragment,
            R.id.homeFragment, R.id.toDoListFragment
        ).build()

        toolbar.setupWithNavController(navController, appBarConfiguration)
        bottomNav = findViewById(R.id.bottom_navigation)
        // Mette la home come main
        bottomNav.menu.getItem(0).isChecked = true
        NavigationUI.setupWithNavController(bottomNav, navController)

        settingsRepository = SettingsRepository(this)
        val factory = SettingsViewModelFactory(SettingsRepository(this))
        settingsViewModel = ViewModelProvider(this, factory)[SettingsViewModel::class.java]

        settingsViewModel.restoreSettings()

        settingsViewModel.lightMode.value?.let { Utils.enableLight(it) }
        settingsViewModel.notifiche.value?.let { Utils.disableNotifications(it, this) }


        bottomNav.setOnItemSelectedListener { item ->
            when(item.itemId) {
                R.id.note -> {
                    NavigationHelper.navigateFromActivity(navHostFragment, R.id.noteFragment)
                    true
                }
                R.id.home -> {
                    NavigationHelper.navigateFromActivity(navHostFragment, R.id.homeFragment)
                    true
                }
                R.id.tomato -> {
                    NavigationHelper.navigateFromActivity(navHostFragment, R.id.tomatoFragment)
                    true
                }
                R.id.todo -> {
                    NavigationHelper.navigateFromActivity(navHostFragment, R.id.toDoListFragment)
                    true
                }
                else -> false
            }
        }
        bottomNav.setOnItemReselectedListener {}

        val m = toolbar.menu as MenuBuilder
        m.setOptionalIconsVisible(true)

    }




}
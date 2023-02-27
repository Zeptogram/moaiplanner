package com.example.moaiplanner.ui.main


import android.R.menu
import android.annotation.SuppressLint
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.view.menu.MenuBuilder
import androidx.appcompat.widget.Toolbar
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.navOptions
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.NavigationUI
import androidx.navigation.ui.setupWithNavController
import com.example.moaiplanner.R
import com.example.moaiplanner.data.repository.settings.SettingsRepository
import com.example.moaiplanner.databinding.OptionsFragmentBinding
import com.example.moaiplanner.model.SettingsViewModel
import com.example.moaiplanner.model.SettingsViewModelFactory
import com.example.moaiplanner.util.disableNotifications
import com.example.moaiplanner.util.enableLight
import com.google.android.material.bottomnavigation.BottomNavigationView


class MainActivity : AppCompatActivity() {

    private lateinit var navHostFragment : NavHostFragment
    private lateinit var settingsViewModel: SettingsViewModel
    private lateinit var settingsRepository: SettingsRepository


    @SuppressLint("RestrictedApi")
    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val toolbar = findViewById<Toolbar>(R.id.topAppBar)


        navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment

        val navController = navHostFragment.navController

        // Leva la freccia per il back
        val appBarConfiguration: AppBarConfiguration = AppBarConfiguration.Builder(
            R.id.noteFragment, R.id.tomatoFragment,
            R.id.homeFragment, R.id.registerFragment, R.id.toDoListFragment
        ).build()

        toolbar.setupWithNavController(navController, appBarConfiguration)
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottom_navigation)
        // Mette la home come main
        bottomNav.menu.getItem(2).isChecked = true;
        NavigationUI.setupWithNavController(bottomNav, navController);

        //toolbar?.menu?.setGroupVisible(R.id.edit, false)
        //toolbar?.menu?.setGroupVisible(R.id.sett, true)

        settingsRepository = SettingsRepository(this)
        val factory = SettingsViewModelFactory(SettingsRepository(this))
        settingsViewModel = ViewModelProvider(this, factory)[SettingsViewModel::class.java]

        settingsViewModel.restoreSettings()

        settingsViewModel.lightMode.value?.let { enableLight(it) }
        settingsViewModel.notifiche.value?.let { disableNotifications(it, this) }


        bottomNav.setOnItemSelectedListener { item ->
            when(item.itemId) {
                R.id.note -> {
                    navHostFragment.findNavController().popBackStack()
                    navHostFragment.findNavController().navigate(R.id.noteFragment, null,
                        navOptions {
                            anim {
                                enter = android.R.anim.fade_in
                                popEnter = android.R.anim.fade_in
                            }
                        }, null)
                    true
                }
                R.id.calendar -> {
                    // Respond to navigation item 2 click
                    true
                }
                R.id.home -> {
                    navHostFragment.findNavController().popBackStack()
                    navHostFragment.findNavController().navigate(R.id.homeFragment, null,
                        navOptions {
                            anim {
                                enter = android.R.anim.fade_in
                                popEnter = android.R.anim.fade_in
                            }
                        })
                    true
                }
                R.id.tomato -> {
                    navHostFragment.findNavController().popBackStack()
                    navHostFragment.findNavController().navigate(R.id.tomatoFragment, null,
                        navOptions {
                            anim {
                                enter = android.R.anim.fade_in
                                popEnter = android.R.anim.fade_in
                            }
                        }, null)
                    true
                }
                R.id.todo -> {
                    navHostFragment.findNavController().popBackStack()
                    navHostFragment.findNavController().navigate(R.id.toDoListFragment, null,
                        navOptions {
                            anim {
                                enter = android.R.anim.fade_in
                                popEnter = android.R.anim.fade_in
                            }
                        }, null)
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
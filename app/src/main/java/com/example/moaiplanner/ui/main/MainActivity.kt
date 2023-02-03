package com.example.moaiplanner.ui.main

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.navigation.findNavController

import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.navOptions
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.NavigationUI

import androidx.navigation.ui.setupWithNavController
import com.example.moaiplanner.R
import com.google.android.material.bottomnavigation.BottomNavigationView


class MainActivity : AppCompatActivity() {

    private lateinit var navHostFragment : NavHostFragment


    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val toolbar = findViewById<Toolbar>(R.id.topAppBar)


        navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment

        val navController = navHostFragment.navController

        // Leva la freccia per il back
        val appBarConfiguration: AppBarConfiguration = AppBarConfiguration.Builder(
            R.id.noteFragment, R.id.tomatoFragment,
            R.id.homeFragment, R.id.registerFragment, R.id.todo
        ).build()

        toolbar.setupWithNavController(navController, appBarConfiguration)
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottom_navigation)
        // Mette la home come main
        bottomNav.menu.getItem(2).isChecked = true;
        NavigationUI.setupWithNavController(bottomNav, navController);

        //toolbar?.menu?.setGroupVisible(R.id.edit, false)
        //toolbar?.menu?.setGroupVisible(R.id.sett, true)






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
                    // Respond to navigation item 2 click
                    true
                }
                else -> false
            }
        }
        bottomNav.setOnItemReselectedListener {}
    }
}
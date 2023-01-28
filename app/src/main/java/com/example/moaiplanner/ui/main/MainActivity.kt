package com.example.moaiplanner.ui.main

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.navOptions
import androidx.navigation.ui.AppBarConfiguration

import androidx.navigation.ui.setupWithNavController
import com.example.moaiplanner.R
import com.google.android.material.bottomnavigation.BottomNavigationView


class MainActivity : AppCompatActivity() {

    lateinit var navHostFragment : NavHostFragment

    //Aggiornare tutto con DataBinding
    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)


        val toolbar = findViewById<Toolbar>(R.id.topAppBar)
        //setSupportActionBar(toolbar)
        //var tabLayout = findViewById<TabLayout>(R.id.tabLayout)




        navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment

        val navController = navHostFragment.navController
        /*val navGraph = navController.navInflater.inflate(R.navigation.main_nav_graph)
        navGraph.setStartDestination(R.id.homeFragment)
        navController.graph = navGraph
        toolbar.setupWithNavController(navController)*/
        // Leva la freccia per il back
        val appBarConfiguration: AppBarConfiguration = AppBarConfiguration.Builder(
            R.id.note, R.id.tomatoFragment,
            R.id.homeFragment, R.id.registerFragment, R.id.todo
        ).build()


        toolbar.setupWithNavController(navController, appBarConfiguration)
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottom_navigation)

       // bottomNav.setupWithNavController(navController)









        navHostFragment.findNavController().addOnDestinationChangedListener { _, destination, _ ->
            when (destination.id) {
                R.id.optionsFragment -> {
                    toolbar.menu.findItem(R.id.settings).isVisible = false
                }
                else -> {
                    toolbar.menu.findItem(R.id.settings).isVisible = true
                }
            }
        }



        toolbar.setOnMenuItemClickListener {
            when(it.itemId){
                R.id.settings -> navHostFragment.findNavController().navigate(R.id.optionsFragment, null,
                    navOptions {
                        anim {
                            enter = android.R.anim.fade_in
                            popEnter = android.R.anim.fade_in
                        }
                    }
                )

            }
            true
        }


        bottomNav.setOnItemSelectedListener { item ->
            when(item.itemId) {
                R.id.note -> {
                    navHostFragment.findNavController().navigate(R.id.registerFragment, null,
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

        bottomNav.setOnItemReselectedListener { item ->
            when(item.itemId) {
                R.id.note -> {

                    true
                }
                R.id.calendar -> {

                    true
                }
                R.id.home -> {

                    true
                }
                R.id.tomato -> {

                    true
                }
                R.id.todo -> {

                    true
                }

            }
        }







        /*val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.fragmentContainerView) as NavHostFragment?*/
        //val navController = navHostFragment!!.navController

        // For the Toolbar
        //setupActionBarWithNavController(this, navController)
        /*tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {

            override fun onTabSelected(tab: TabLayout.Tab?) {
                Log.d("TAB", "Tab selected")
            }

            override fun onTabReselected(tab: TabLayout.Tab?) {
                Log.d("TAB", "Tab reselected")

            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {
                // Handle tab unselect
            }
        })*/


    }







}
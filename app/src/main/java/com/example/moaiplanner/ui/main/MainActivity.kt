package com.example.moaiplanner.ui.main

import android.os.Bundle
import android.view.Menu
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.NavigationUI.setupActionBarWithNavController
import com.example.moaiplanner.R


class MainActivity : AppCompatActivity() {


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)


        val toolbar = findViewById<Toolbar>(R.id.topAppBar)
        //var tabLayout = findViewById<TabLayout>(R.id.tabLayout)
        setSupportActionBar(toolbar)
        supportActionBar?.apply {

            // show back button on toolbar
            // on back button press, it will navigate to parent activity
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowHomeEnabled(true)
        }

        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHostFragment.navController

        // For the Toolbar
        setupActionBarWithNavController(this, navController)


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

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.top_app_bar, menu)
        return true
    }

}
package com.example.moaiplanner.ui.welcome

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.NavigationUI.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.example.moaiplanner.R

class WelcomeActivity : AppCompatActivity() {
    lateinit var navHostFragment : NavHostFragment

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_welcome)

        val toolbar = findViewById<Toolbar>(R.id.topAppBarWelcome)
        navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_welcome_fragment) as NavHostFragment
        val navController = navHostFragment.navController
        toolbar.setupWithNavController(navController)
    }
}
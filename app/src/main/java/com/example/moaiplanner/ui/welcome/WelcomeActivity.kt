package com.example.moaiplanner.ui.welcome

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.NavigationUI.setupActionBarWithNavController
import com.example.moaiplanner.R

class WelcomeActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_welcome)

        val navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_welcome_fragment) as NavHostFragment?
        val navController = navHostFragment!!.navController
    }
}
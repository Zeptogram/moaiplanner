package com.example.moaiplanner.ui.welcome

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.example.moaiplanner.R
import com.example.moaiplanner.data.repository.settings.SettingsRepository
import com.example.moaiplanner.model.SettingsViewModel
import com.example.moaiplanner.model.SettingsViewModelFactory
import com.example.moaiplanner.ui.main.MainActivity
import com.example.moaiplanner.util.disableNotifications
import com.example.moaiplanner.util.enableLight

class WelcomeActivity : AppCompatActivity() {
    private lateinit var navHostFragment : NavHostFragment
    private lateinit var settingsViewModel: SettingsViewModel
    private lateinit var settingsRepository: SettingsRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_welcome)

        val toolbar = findViewById<Toolbar>(R.id.topAppBarWelcome)
        navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_welcome_fragment) as NavHostFragment
        val navController = navHostFragment.navController
        toolbar.setupWithNavController(navController)
        settingsRepository = SettingsRepository(this)
        val factory = SettingsViewModelFactory(SettingsRepository(this))
        settingsViewModel = ViewModelProvider(this, factory)[SettingsViewModel::class.java]
        val sharedPref: SharedPreferences = this.getSharedPreferences("user", Context.MODE_PRIVATE)
        val logged = sharedPref.getBoolean("auth", false)

        settingsViewModel.restoreSettings()
        settingsViewModel.lightMode.value?.let { enableLight(it) }
        settingsViewModel.notifiche.value?.let { disableNotifications(it, this) }

        if(logged) {
            Log.d("WELCOME-A", "User logged")
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish()
        }
    }
}
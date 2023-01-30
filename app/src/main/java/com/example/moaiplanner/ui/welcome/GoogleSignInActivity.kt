package com.example.moaiplanner.ui.welcome

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.fragment.NavHostFragment
import com.example.moaiplanner.R
import com.example.moaiplanner.data.repository.user.AuthRepository
import com.example.moaiplanner.ui.main.MainActivity
import com.google.android.gms.auth.api.signin.GoogleSignInClient


class GoogleSignInActivity : AppCompatActivity() {
    private lateinit var firebase: AuthRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_google_sign_in)
    }

    override fun onStart() {
        super.onStart()

        firebase = AuthRepository(application)
        firebase.signInGoogle(this)

        if (firebase.isUserAuthenticated()) {
            val i = Intent(this@GoogleSignInActivity, MainActivity::class.java)
            startActivity(i)
            finish()
        } else {
            // navController.navigate(R.id.nav_welcome_fragment)
        }
    }
}
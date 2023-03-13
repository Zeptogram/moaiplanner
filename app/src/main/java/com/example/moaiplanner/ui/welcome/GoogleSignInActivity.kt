package com.example.moaiplanner.ui.welcome

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.example.moaiplanner.R
import com.example.moaiplanner.data.repository.user.AuthRepository
import com.example.moaiplanner.ui.main.MainActivity
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.tasks.Task


class GoogleSignInActivity : AppCompatActivity() {
    private lateinit var firebase: AuthRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_google_sign_in)
    }

    override fun onStart() {
        super.onStart()

        /*firebase = AuthRepository(application)
        firebase.signInGoogle(this)*/
        val account = GoogleSignIn.getLastSignedInAccount(this)
        if(account != null) {
            val i = Intent(this@GoogleSignInActivity, MainActivity::class.java)
            startActivity(i)
            finish()
        }




        val googleSignInOptions = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken("50394403115-k98o4pct0grf3dveb47a4ncl4gauuifd.apps.googleusercontent.com")
            .requestEmail()
            .build()


        var googleSignInClient = GoogleSignIn.getClient(this, googleSignInOptions)
        val intent = googleSignInClient.signInIntent
        startActivityForResult(intent, 1000)



        /*if (firebase.isUserAuthenticated()) {
            val i = Intent(this@GoogleSignInActivity, MainActivity::class.java)
            startActivity(i)
            finish()
        } else {
            // navController.navigate(R.id.nav_welcome_fragment)
        }*/
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        // Result returned from launching the Intent from GoogleSignInClient.getSignInIntent(...);
        if (requestCode == 1000) {
            // The Task returned from this call is always completed, no need to attach
            // a listener.
            val task: Task<GoogleSignInAccount> = GoogleSignIn.getSignedInAccountFromIntent(data)
            handleSignInResult(task)
        }
    }

    private fun handleSignInResult(completedTask: Task<GoogleSignInAccount>) {
        try {
            val account = completedTask.getResult(ApiException::class.java)
            // Signed in successfully, show authenticated UI.
            Log.d("GOOGLE", "Ok")
            val i = Intent(this@GoogleSignInActivity, MainActivity::class.java)
            startActivity(i)
            finish()
        } catch (e: ApiException) {
            // The ApiException status code indicates the detailed failure reason.
            // Please refer to the GoogleSignInStatusCodes class reference for more information.
            Log.w("GOOGLE", "signInResult:failed code=" + e.statusCode)
        }
    }

}
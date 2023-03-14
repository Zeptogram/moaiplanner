package com.example.moaiplanner.ui.welcome

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.edit
import com.example.moaiplanner.R
import com.example.moaiplanner.data.repository.user.AuthRepository
import com.example.moaiplanner.ui.main.MainActivity
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase


class GoogleSignInActivity : AppCompatActivity() {
    private lateinit var firebase: AuthRepository
    private lateinit var googleSignInClient : GoogleSignInClient
    private var firebaseAuth: FirebaseAuth = Firebase.auth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_google_sign_in)
    }

    override fun onStart() {
        super.onStart()

        /*firebase = AuthRepository(application)
        firebase.signInGoogle(this)*/
        /*val account = GoogleSignIn.getLastSignedInAccount(this)
        if(account != null) {
            val i = Intent(this@GoogleSignInActivity, MainActivity::class.java)
            startActivity(i)
            finish()
        } */




        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()


        googleSignInClient = GoogleSignIn.getClient(this , gso)
        googleSignInClient.signOut()
        val intent = googleSignInClient.signInIntent
        launcher.launch(intent)




        /*if (firebase.isUserAuthenticated()) {
            val i = Intent(this@GoogleSignInActivity, MainActivity::class.java)
            startActivity(i)
            finish()
        } else {
            // navController.navigate(R.id.nav_welcome_fragment)
        }*/
    }

    private val launcher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()){
            result ->
        if (result.resultCode == Activity.RESULT_OK){
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            handleResults(task)
        }
    }

    /*override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        // Result returned from launching the Intent from GoogleSignInClient.getSignInIntent(...);
        if (requestCode == 1000) {
            // The Task returned from this call is always completed, no need to attach
            // a listener.
            val task: Task<GoogleSignInAccount> = GoogleSignIn.getSignedInAccountFromIntent(data)
            handleSignInResult(task)
        }
    }*/

    private fun handleResults(task: Task<GoogleSignInAccount>) {
        if (task.isSuccessful){
            val account : GoogleSignInAccount? = task.result
            if (account != null){
                updateUI(account)
                /*val intent = Intent(this, MainActivity::class.java)
                startActivity(intent)
                finish()*/
            }
        }else{
            Log.d("GOOGLE", "not ok")
        }
    }

    private fun updateUI(account: GoogleSignInAccount) {
        val credential = GoogleAuthProvider.getCredential(account.idToken , null)
        firebaseAuth.signInWithCredential(credential).addOnCompleteListener {
            if (it.isSuccessful) {
                val intent: Intent = Intent(this, MainActivity::class.java)
                /*intent.putExtra("email", account.email)
                intent.putExtra("name", account.displayName)*/
                val sharedPref: SharedPreferences =
                    (getSharedPreferences("user", Context.MODE_PRIVATE) ?: null) as SharedPreferences
                sharedPref.edit {
                    putBoolean("auth", it.isSuccessful)
                    apply()
                }
                startActivity(intent)
                finish()
            } else {
                Toast.makeText(this, it.exception.toString(), Toast.LENGTH_SHORT).show()
            }
        }
    }

}
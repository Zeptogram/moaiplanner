package com.moai.planner.data.user

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.util.Log
import android.view.View
import androidx.activity.result.ActivityResultLauncher
import androidx.core.content.edit
import com.moai.planner.R
import com.moai.planner.ui.main.MainActivity
import com.moai.planner.util.Utils
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase

class GoogleSignInHelper(private val activity: Activity, private val launcher: ActivityResultLauncher<Intent>, private val view: View) {
    private var googleSignInClient: GoogleSignInClient
    private val firebaseAuth: FirebaseAuth = Firebase.auth


    init {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(activity.getString(R.string.default_web_client_id))
            .requestEmail()
            .build()

        googleSignInClient = GoogleSignIn.getClient(activity, gso)
    }

    fun signInGoogle() {
        googleSignInClient.signOut()
        val intent = googleSignInClient.signInIntent
        launcher.launch(intent)
    }

    fun handleActivityResult(resultCode: Int, data: Intent?) {
        if (resultCode == Activity.RESULT_OK) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            handleResults(task)
        } else {
            Utils.showPopup(view, activity, activity.getString(R.string.sign_in_error))
        }
    }

    private fun handleResults(task: Task<GoogleSignInAccount>) {
        if (task.isSuccessful){
            val account : GoogleSignInAccount? = task.result
            if (account != null){
                updateUI(account)
            }
        }else{
            Log.d("GOOGLE", "Error")
        }
    }

    private fun updateUI(account: GoogleSignInAccount) {
        val credential = GoogleAuthProvider.getCredential(account.idToken , null)
        firebaseAuth.signInWithCredential(credential).addOnCompleteListener {
            if (it.isSuccessful) {
                val intent = Intent(activity, MainActivity::class.java)
                val sharedPref: SharedPreferences =
                    (activity.getSharedPreferences("user", Context.MODE_PRIVATE) ?: null) as SharedPreferences
                sharedPref.edit {
                    putBoolean("auth", it.isSuccessful)
                    apply()
                }
                activity.startActivity(intent)
                activity.finish()
            } else {
                Log.d("GOOGLE", it.exception.toString())
            }
        }
    }
}

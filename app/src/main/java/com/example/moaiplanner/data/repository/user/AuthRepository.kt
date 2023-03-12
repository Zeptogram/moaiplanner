package com.example.moaiplanner.data.repository.user

import android.app.Activity
import android.app.Application
import android.content.ContentValues.TAG
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.util.Log
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat.startActivityForResult
import androidx.core.content.edit
import androidx.lifecycle.lifecycleScope
import com.example.moaiplanner.R
import com.google.android.gms.auth.api.identity.BeginSignInRequest
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.AuthCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlin.coroutines.coroutineContext
import kotlin.math.sign

class AuthRepository(app: Application) {
    private var application: Application
    private var firebaseAuth: FirebaseAuth
    private lateinit var googleSignInClient: GoogleSignInClient

    private companion object {
        private const val RC_SIGN_IN = 100
        private const val TAG = "GOOGLE_SIGN_IN_TAG"
    }

    init {
        this.application = app
        this.firebaseAuth = Firebase.auth
    }

    fun createAccount(email: String, password: String) {
        firebaseAuth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    // Sign in success
                    Log.d(TAG, "createUserWithEmail:success")
                    firebaseAuth.currentUser?.sendEmailVerification()
                    Toast.makeText(application, "Verification email sent", Toast.LENGTH_SHORT).show()
                    firebaseAuth.signOut()
                } else {
                    // If sign in fails, display a message to the user.
                    Log.w(TAG, "createUserWithEmail:failure", task.exception)
                    Toast.makeText(application, "Authentication failed", Toast.LENGTH_SHORT).show()
                }
            }
    }

/*fun signIn(email: String, password: String) {
        firebaseAuth.signInWithEmailAndPassword(email, password)
            .addOnSuccessListener { task ->
                // Sign in success
                Log.d(TAG, "signInWithEmail:success")
                Toast.makeText(application, "Authentication successful", Toast.LENGTH_SHORT).show()

            }
            .addOnFailureListener { task ->
                // If sign in fails, display a message to the user.
                Log.w(TAG, "signInWithEmail:failure", task)
                Toast.makeText(application, "Authentication failed", Toast.LENGTH_SHORT).show()

            }
    }*/

    suspend fun signIn(email: String, password: String): Boolean {
        return try {
            firebaseAuth.signInWithEmailAndPassword(email, password).await()
            Log.d("USER", firebaseAuth.currentUser.toString())
            true
        } catch (e: Exception) {
            false
        }
    }


    // TODO Rimuovere questa parte
    fun signInGoogle(activity: Activity) {
        val googleSignInOptions = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(R.string.default_web_client_id.toString())
            .requestEmail()
            .build()

        googleSignInClient = GoogleSignIn.getClient(activity, googleSignInOptions)
        val intent = googleSignInClient.signInIntent

        val accountTask = GoogleSignIn.getSignedInAccountFromIntent(intent)
        try {
            val account = accountTask.getResult(ApiException::class.java)
            authGoogleAccount(account)
        } catch (e: java.lang.Exception) {
            Log.d(TAG, "Google Account Task failed")
        }
    }

    fun authGoogleAccount(account: GoogleSignInAccount) {
        val credential = GoogleAuthProvider.getCredential(account.idToken, null)
        firebaseAuth.signInWithCredential(credential)
            .addOnSuccessListener { authResult ->
                // Sign in success
                Log.d(TAG, "signInWithGoogle:success")
                Toast.makeText(application, "Authentication with Google successful", Toast.LENGTH_SHORT).show()
                // val firebaseUser = firebaseAuth.currentUser
                // val uid = firebaseUser.uid
                // val email = firebaseUser.email

                if (authResult.additionalUserInfo!!.isNewUser) {
                    Log.d(TAG, "signInWithGoogle:accountCreated")
                    Toast.makeText(application, "Account with Google created", Toast.LENGTH_SHORT).show()
                } else {
                    Log.d(TAG, "signInWithGoogle:existingUserLoggedIn")
                    Toast.makeText(application, "Signed in with Google", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { e ->
                Log.d(TAG, "signInWithGoogle:failed due to ${e.message}")
                Toast.makeText(application, "Authentication with Google failed", Toast.LENGTH_SHORT).show()
            }
    }

    fun signOut(context: Context) {
        val sharedPref: SharedPreferences = context.getSharedPreferences("user", Context.MODE_PRIVATE)
        sharedPref.edit {
            putBoolean("auth", false)
            apply()
        }
        firebaseAuth.signOut()

    }

    fun isUserAuthenticated(): Boolean {
        if (firebaseAuth.currentUser != null)
            return true
        return false
    }

    fun getCurretUid(): String? {
        return firebaseAuth.uid
    }


}
package com.example.moaiplanner.data.repository.user

import android.app.Application
import android.content.ContentValues.TAG
import android.util.Log
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase

class AuthRepository(app: Application) {
    private var application: Application
    private var firebaseAuth: FirebaseAuth

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

    fun signIn(email: String, password: String) {
        firebaseAuth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    // Sign in success
                    Log.d(TAG, "signInWithEmail:success")
                    Toast.makeText(application, "Authentication successful", Toast.LENGTH_SHORT).show()
                } else {
                    // If sign in fails, display a message to the user.
                    Log.w(TAG, "signInWithEmail:failure", task.exception)
                    Toast.makeText(application, "Authentication failed", Toast.LENGTH_SHORT).show()
                }
            }
    }

    fun signOut() {
        firebaseAuth.signOut()
        Toast.makeText(application, "Sign out successful", Toast.LENGTH_SHORT).show()
    }

    fun isUserAuthenticated(): Boolean {
        if (firebaseAuth.currentUser != null)
            return true
        return false
    }
}
package com.example.moaiplanner.data.user

import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.util.Log
import android.util.Patterns
import android.view.View
import android.widget.TextView
import androidx.core.content.edit
import androidx.preference.PreferenceManager
import com.example.moaiplanner.R
import com.example.moaiplanner.model.PREF_KEY_AUTOSAVE_URI
import com.example.moaiplanner.ui.main.MainActivity
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.ktx.auth
import com.google.firebase.auth.ktx.userProfileChangeRequest
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class UserAuthentication(app: Application, view: View? = null) {
    private var application: Application
    private var firebaseAuth: FirebaseAuth
    private var view: View?

    private companion object {
        private const val TAG = "GOOGLE_SIGN_IN_TAG"
    }

    init {
        this.application = app
        this.firebaseAuth = Firebase.auth
        this.view = view
    }

    fun createAccount(email: String, password: String, username: String) {
        firebaseAuth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    // Sign in success
                    Log.d(TAG, "createUserWithEmail:success")
                    setDisplayName(username)
                    firebaseAuth.currentUser?.sendEmailVerification()
                    view?.let {
                        Snackbar.make(it,"Verification email sent", Snackbar.LENGTH_SHORT)
                            .setAction("OK") {
                                // Responds to click on the action
                            }
                            .setActionTextColor(application.getColor(R.color.primary))
                            .show()
                    }
                    //Toast.makeText(application, "Verification email sent", Toast.LENGTH_SHORT).show()
                    firebaseAuth.signOut()
                } else {
                    // If sign in fails, display a message to the user.
                    Log.w(TAG, "createUserWithEmail:failure", task.exception)
                    view?.let {
                        Snackbar.make(it,"Authentication failed", Snackbar.LENGTH_SHORT)
                            .setAction("OK") {
                                // Responds to click on the action
                            }
                            .setActionTextColor(application.getColor(R.color.primary))
                            .show()
                    }
                }
            }
    }

    suspend fun signIn(email: String, password: String): Boolean {
        return try {
            firebaseAuth.signInWithEmailAndPassword(email, password).await()
            Log.d("USER", firebaseAuth.currentUser.toString())
            true
        } catch (e: Exception) {
            false
        }
    }

    suspend fun signInAndSaveUser(email: String, password: String, activity: Activity?, view: View?) {

        try {
            val authResult = withContext(Dispatchers.IO) {
                firebaseAuth.signInWithEmailAndPassword(email, password).await()
            }

            if (authResult?.user != null) {
                val sharedPref: SharedPreferences =
                    activity?.getSharedPreferences("user", Context.MODE_PRIVATE) as SharedPreferences
                sharedPref.edit {
                    putBoolean("auth", true)
                    apply()
                }
                withContext(Dispatchers.Main) {
                    val intent = Intent(activity, MainActivity::class.java)
                    activity.startActivity(intent)
                    activity.finish()
                }
            } else {
                Log.d("SIGN-IN", "Error")
            }
        } catch (e: Exception) {
            view?.let {
                Snackbar.make(it,"Authentication failed", Snackbar.LENGTH_SHORT)
                    .setAction("OK") {
                        // Responds to click on the action
                    }
                    .setActionTextColor(application.getColor(R.color.primary))
                    .show()
            }
        } catch (e: FirebaseAuthInvalidCredentialsException) {
            view?.let {
                Snackbar.make(it,"Authentication failed", Snackbar.LENGTH_SHORT)
                    .setAction("OK") {
                        // Responds to click on the action
                    }
                    .setActionTextColor(application.getColor(R.color.primary))
                    .show()
            }
        }
    }




    fun signOut(context: Context) {
        var sharedPref: SharedPreferences = context.getSharedPreferences("user", Context.MODE_PRIVATE)
        sharedPref.edit {
            putBoolean("auth", false)
            apply()
        }
        sharedPref = PreferenceManager.getDefaultSharedPreferences(context)
        sharedPref.edit {
            remove(PREF_KEY_AUTOSAVE_URI)
            apply()
        }

        firebaseAuth.signOut()

    }

    fun isUserAuthenticated(): Boolean {
        if (firebaseAuth.currentUser != null)
            return true
        return false
    }

    fun getCurrentUid(): String? {
        return firebaseAuth.uid
    }

    fun getEmail(): String? {
        var email = ""
        firebaseAuth.currentUser?.let {
            email = it.email.toString()
        }
        return email;
    }

    fun setEmail(email: String, txt: TextView) {
        if(validateEmail(email) && isUserAuthenticated()) {
            firebaseAuth.currentUser!!.updateEmail(email)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        Log.d(TAG, "User email address updated.")
                        txt.text = email
                        firebaseAuth.currentUser!!.sendEmailVerification()
                            .addOnCompleteListener { verificationTask ->
                                if (verificationTask.isSuccessful) {
                                    Log.d(TAG, "Email verification sent.")
                                }
                            }
                    }
                }
        }
        else {
            view?.let {
                Snackbar.make(it,"Utente non autenticato", Snackbar.LENGTH_SHORT)
                    .setAction("OK") {
                        // Responds to click on the action
                    }
                    .setActionTextColor(application.getColor(R.color.primary))
                    .show()
            }
        }
    }

    fun setPassword(old: String, new: String) {
        if(validatePassword(new)) {
            // verifica che l'utente sia autenticato
            val user = firebaseAuth.currentUser
            if (isUserAuthenticated()) {
                // crea le credenziali dell'utente
                val credential = user?.email?.let { EmailAuthProvider.getCredential(it, old) }
                // riautentica l'utente con le credenziali inserite
                if (credential != null) {
                    user.reauthenticate(credential)
                        .addOnCompleteListener { task ->
                            if (task.isSuccessful) {
                                // se l'autenticazione è avvenuta con successo, cambia la password
                                user.updatePassword(new)
                                    .addOnCompleteListener { updateTask ->
                                        if (updateTask.isSuccessful) {
                                            // la password è stata cambiata con successo
                                            view?.let {
                                                Snackbar.make(it,"Password Aggiornata", Snackbar.LENGTH_SHORT)
                                                    .setAction("OK") {
                                                        // Responds to click on the action
                                                    }
                                                    .setActionTextColor(application.getColor(R.color.primary))
                                                    .show()
                                            }
                                        } else {
                                            // si è verificato un errore durante l'aggiornamento della password
                                            view?.let {
                                                Snackbar.make(it,"Errore durante il cambiamento password", Snackbar.LENGTH_SHORT)
                                                    .setAction("OK") {
                                                        // Responds to click on the action
                                                    }
                                                    .setActionTextColor(application.getColor(R.color.primary))
                                                    .show()
                                            }
                                        }
                                    }
                            } else {
                                // la vecchia password non è corretta
                                view?.let {
                                    Snackbar.make(it,"Vecchia Password non corretta", Snackbar.LENGTH_SHORT)
                                        .setAction("OK") {
                                            // Responds to click on the action
                                        }
                                        .setActionTextColor(application.getColor(R.color.primary))
                                        .show()
                                }
                            }
                        }
                }
            } else {
                    // l'utente non è autenticato, esegui la reautenticazione
                    val email = firebaseAuth.currentUser?.email
                    val credential = email?.let { EmailAuthProvider.getCredential(it, old) }
                    if (credential != null) {
                        firebaseAuth.signInWithCredential(credential)
                            .addOnCompleteListener { task ->
                                if (task.isSuccessful) {
                                    setPassword(old, new)
                                } else {
                                    // le credenziali non sono corrette
                                    view?.let {
                                        Snackbar.make(it,"Vecchia Password non corretta", Snackbar.LENGTH_SHORT)
                                            .setAction("OK") {
                                                // Responds to click on the action
                                            }
                                            .setActionTextColor(application.getColor(R.color.primary))
                                            .show()
                                    }
                                }
                            }
                    }
            }
        }
        else {
            // weak password
            view?.let {
                Snackbar.make(it,"Inserire una Password Sicura", Snackbar.LENGTH_SHORT)
                    .setAction("OK") {
                        // Responds to click on the action
                    }
                    .setActionTextColor(application.getColor(R.color.primary))
                    .show()
            }
        }
    }

    private fun setDisplayName(username: String) {
        val profileUpdates = userProfileChangeRequest {
            displayName = username
        }
        firebaseAuth.currentUser!!.updateProfile(profileUpdates)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Log.d(TAG, "User profile updated.")
                }
            }
    }
    // Overload
    fun setDisplayName(username: String, name: TextView) {
        val profileUpdates = userProfileChangeRequest {
            displayName = username
        }
        firebaseAuth.currentUser!!.updateProfile(profileUpdates)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Log.d(TAG, "User profile updated.")
                    name.text = username
                }
            }
    }

    fun getDisplayName(): String? {
        var username = ""
        firebaseAuth.currentUser?.let {
            username = it.displayName.toString()
        }
        return username;
    }


    fun resetPassword(email: String) {
            Firebase.auth.sendPasswordResetEmail(email)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        Log.d(TAG, "Email sent.")
                    }
                }
    }

    fun validateData(email: String, password: String): Boolean {
        return validateEmail(email) && validatePassword(password)
    }

    private fun validateEmail(email: String): Boolean {
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            view?.let {
                Snackbar.make(it,"Inserire una mail valida", Snackbar.LENGTH_SHORT)
                    .setAction("OK") {
                        // Responds to click on the action
                    }
                    .setActionTextColor(application.getColor(R.color.primary))
                    .show()
            }
            return false
        }
        return true
    }

    private fun validatePassword(password: String): Boolean {
        if (password.length < 6) {
            view?.let {
                Snackbar.make(it,"Inserire una password valida", Snackbar.LENGTH_SHORT)
                    .setAction("OK") {
                        // Responds to click on the action
                    }
                    .setActionTextColor(application.getColor(R.color.primary))
                    .show()
            }
            return false
        }
        val hasLowerCase = password.matches(Regex(".*[a-z].*"))
        val hasUpperCase = password.matches(Regex(".*[A-Z].*"))
        val hasDigit = password.matches(Regex(".*\\d.*"))
        val hasSpecialChar = password.matches(Regex(".*[!@#\$%^&*()_+\\-\\[\\]{};':\"\\\\|,.<>\\/?].*"))
        return hasLowerCase && hasUpperCase && hasDigit && hasSpecialChar
    }

    fun getProvider(): String {
        val user = Firebase.auth.currentUser
        var providerId: String = ""
        user?.let {
            for (profile in it.providerData) {
                providerId = profile.providerId

            }
        }
        return providerId
    }

    fun getGoogleImage(): Uri?{
        val user = Firebase.auth.currentUser
        var photo: Uri? = null
        user?.let {
            for (profile in it.providerData) {
                photo = profile.photoUrl

            }
        }
        return photo
    }


}
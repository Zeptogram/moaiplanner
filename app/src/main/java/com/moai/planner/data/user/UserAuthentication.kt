package com.moai.planner.data.user

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
import com.moai.planner.R
import com.moai.planner.model.PREF_KEY_AUTOSAVE_URI
import com.moai.planner.ui.main.MainActivity
import com.moai.planner.util.Utils
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.ktx.auth
import com.google.firebase.auth.ktx.userProfileChangeRequest
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class UserAuthentication(app: Application, view: View? = null, activity: Activity? = null) {
    private var application: Application
    private var firebaseAuth: FirebaseAuth
    private var view: View?
    private var activity: Activity?

    private companion object {
        private const val TAG = "GOOGLE_SIGN_IN_TAG"
    }

    init {
        this.application = app
        this.firebaseAuth = Firebase.auth
        this.view = view
        this.activity = activity
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
                        activity?.let { it1 ->
                            Utils.showPopup(it,
                                it1, activity!!.getString(R.string.verification_sent))
                        }
                    }
                    firebaseAuth.signOut()
                } else {
                    // If sign in fails, display a message to the user.
                    Log.w(TAG, "createUserWithEmail:failure", task.exception)
                    view?.let {
                        activity?.let { it1 ->
                            Utils.showPopup(it,
                                it1, activity!!.getString(R.string.authentication_failed))
                        }
                    }
                }
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
                activity?.let { it1 ->
                    Utils.showPopup(it,
                        it1, activity.getString(R.string.authentication_failed))
                }
            }
        } catch (e: FirebaseAuthInvalidCredentialsException) {
            view?.let {
                activity?.let { it1 ->
                    Utils.showPopup(it,
                        it1, activity.getString(R.string.authentication_failed))
                }
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

    fun getEmail(): String {
        var email = ""
        firebaseAuth.currentUser?.let {
            email = it.email.toString()
        }
        return email
    }

    fun setEmail(email: String, txt: TextView) {
        if(validateEmail(email) && isUserAuthenticated()) {
            firebaseAuth.currentUser!!.updateEmail(email).addOnCompleteListener { task ->
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
                activity?.let { it1 ->
                    Utils.showPopup(it,
                        it1, activity!!.getString(R.string.user_not_authenticated))
                }
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
                                                activity?.let { it1 ->
                                                    Utils.showPopup(it,
                                                        it1, activity!!.getString(R.string.password_updated))
                                                }
                                            }
                                        } else {
                                            // si è verificato un errore durante l'aggiornamento della password
                                            view?.let {
                                                activity?.let { it1 ->
                                                    Utils.showPopup(it,
                                                        it1, activity!!.getString(R.string.error_password_update))
                                                }
                                            }
                                        }
                                    }
                            } else {
                                // la vecchia password non è corretta
                                view?.let {
                                    activity?.let { it1 ->
                                        Utils.showPopup(it,
                                            it1, activity!!.getString(R.string.old_password_not_correct))
                                    }
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
                                        activity?.let { it1 ->
                                            Utils.showPopup(it,
                                                it1, activity!!.getString(R.string.authentication_failed))
                                        }
                                    }
                                }
                            }
                    }
            }
        }
        else {
            // weak password
            view?.let {
                activity?.let { it1 ->
                    Utils.showPopup(it,
                        it1, activity!!.getString(R.string.secure_pass))
                }
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

    fun getDisplayName(): String {
        var username = ""
        firebaseAuth.currentUser?.let {
            username = it.displayName.toString()
        }
        return username
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
                activity?.let { it1 ->
                    Utils.showPopup(it,
                        it1, activity!!.getString(R.string.valid_email))
                }
            }
            return false
        }
        return true
    }

    private fun validatePassword(password: String): Boolean {
        if (password.length < 6) {
            view?.let {
                activity?.let { it1 ->
                    Utils.showPopup(it,
                        it1, activity!!.getString(R.string.secure_pass))
                }
            }
            return false
        }
        val hasLowerCase = password.matches(Regex(".*[a-z].*"))
        val hasUpperCase = password.matches(Regex(".*[A-Z].*"))
        val hasDigit = password.matches(Regex(".*\\d.*"))
        val hasSpecialChar = password.matches(Regex(".*[!@#\$%^&*()_+\\-\\[\\]{};':\"\\\\|,.<>?].*"))
        if(!(hasLowerCase && hasUpperCase && hasDigit && hasSpecialChar)){
            view?.let {
                activity?.let { it1 ->
                    Utils.showPopup(it,
                        it1, activity!!.getString(R.string.secure_pass))
                }
            }
        }
        return hasLowerCase && hasUpperCase && hasDigit && hasSpecialChar
    }

    fun getProvider(): String {
        val user = Firebase.auth.currentUser
        var providerId = ""
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
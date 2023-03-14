package com.example.moaiplanner.ui.welcome

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Patterns
import android.view.*
import android.widget.EditText
import androidx.core.content.edit
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.moaiplanner.R
import com.example.moaiplanner.data.repository.user.AuthRepository
import com.example.moaiplanner.databinding.SigninFragmentBinding
import com.example.moaiplanner.ui.main.MainActivity
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch


class SigninFragment : Fragment() {

    lateinit var binding: SigninFragmentBinding
    lateinit var firebase: AuthRepository

    fun newInstance(): SigninFragment? {
        return SigninFragment()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = SigninFragmentBinding.inflate(inflater, container, false)
        // Inflate il layout per il fragment

        binding.buttonGoogleLogin.setOnClickListener {
            findNavController().navigate(R.id.googleSignInActivity)
            requireActivity().finish()
        }


        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        firebase = AuthRepository(requireActivity().application)

        binding.forgotPassword.setOnClickListener {
            showResetDialog()
        }

        var isAuthenticated: Boolean = false
        binding.buttonSignIn.setOnClickListener {
            lifecycleScope.launch(Dispatchers.IO) {
                val email = binding.editTextEmail.text.toString()
                val password = binding.editTextPassword.text.toString()
                lifecycleScope.launch(Dispatchers.IO) {
                    isAuthenticated = firebase.signIn(email, password)
                }.invokeOnCompletion {
                        lifecycleScope.launch(Dispatchers.Main) {
                            if (isAuthenticated) {
                                val sharedPref: SharedPreferences =
                                    (activity?.getSharedPreferences("user", Context.MODE_PRIVATE) ?: null) as SharedPreferences
                                sharedPref.edit {
                                    putBoolean("auth", isAuthenticated)
                                    apply()
                                }
                                val intent = Intent(requireActivity(), MainActivity::class.java)
                                startActivity(intent)
                                requireActivity().finish()
                            }
                            else
                                Snackbar.make(view,"Authentication failed", Snackbar.LENGTH_SHORT)
                                    .setAction("OK") {
                                        // Responds to click on the action
                                    }
                                    .setActionTextColor(resources.getColor(R.color.primary, null))
                                    .show()
                        }
                }
            }
        }
    }

    private fun showResetDialog() {
        val layoutInflater = LayoutInflater.from(context)
        val dialogView = layoutInflater.inflate(R.layout.dialog_reset_password, null)
        val editEmail = dialogView.findViewById<EditText>(R.id.emailText)

        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setTitle("Inserire la Email")
            .setView(dialogView)
            .setPositiveButton("Invia mail") { dialog, which ->
                val text = editEmail.text.toString()

                if (text.isNotBlank()) {
                    firebase.resetPassword(text)
                }
            }
            .setNegativeButton("Annulla") { dialog, which ->
            }
            .create()

        dialog.show()

    }

  /*  fun validateData(email: String, password: String): Boolean {
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.editTextEmail.error = "Email is invalid"
            return false
        }

        if (password.length < 6) {
            binding.editTextPassword.error = "Password length is invalid"
            return false
        }

        return true
    }*/

    /*fun signIn() {
        val email = binding.editTextEmail.text.toString()
        val password = binding.editTextPassword.text.toString()
        var validate: Boolean = false

        lifecycleScope.launch(Dispatchers.Main) {
            validate = validateData(email, password)
        }.invokeOnCompletion {
            if (validate) {
                lifecycleScope.launch(Dispatchers.IO) {
                    firebase.signIn(email, password)
                }
            }
        }
    }*/
}
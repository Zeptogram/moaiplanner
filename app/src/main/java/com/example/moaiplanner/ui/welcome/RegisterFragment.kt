package com.example.moaiplanner.ui.welcome

import android.os.Bundle
import android.util.Log
import android.util.Patterns
import android.view.*
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.ActionBar
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.Fragment
import androidx.navigation.Navigation.findNavController
import androidx.navigation.findNavController
import androidx.navigation.fragment.findNavController
import androidx.navigation.navOptions
import com.example.moaiplanner.R
import com.example.moaiplanner.data.repository.user.AuthRepository
import com.example.moaiplanner.databinding.RegisterFragmentBinding


class RegisterFragment : Fragment() {

    lateinit var binding: RegisterFragmentBinding
    lateinit var firebase: AuthRepository

    fun newInstance(): RegisterFragment? {
        return RegisterFragment()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = RegisterFragmentBinding.inflate(inflater, container, false)

        binding.buttonGoogleLogin.setOnClickListener {
            findNavController().navigate(R.id.googleSignInActivity)
            requireActivity().finish()
        }

        // Inflate il layout per il fragment
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        firebase = AuthRepository(requireActivity().application)
        binding.buttonSignUp.setOnClickListener {
            createAccount()
        }
    }

    fun validateData(email: String, password: String, confirmPassword: String): Boolean {
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.editTextEmail.error = "Email is invalid"
            return false
        }

        if (password.length < 6) {
            binding.editTextPassword.error = "Password length is invalid"
            return false
        }

        if (!password.equals(confirmPassword)) {
            binding.editTextConfirmPassword.error = "Password not matched"
            return false
        }

        return true
    }

    fun createAccount() {
        val email = binding.editTextEmail.text.toString()
        val password = binding.editTextPassword.text.toString()
        if (validateData(email, password, binding.editTextConfirmPassword.text.toString())) {
            firebase.createAccount(email, password)
        }
    }
}
package com.example.moaiplanner.ui.welcome

import android.os.Bundle
import android.util.Patterns
import android.view.*
import androidx.fragment.app.Fragment
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
        firebase = AuthRepository(requireActivity().application, view)
        binding.buttonSignUp.setOnClickListener {
            createAccount()
        }
    }



    private fun createAccount() {
        val email = binding.editTextEmail.text.toString()
        val password = binding.editTextPassword.text.toString()
        var username = binding.editTextUsername.text.toString()
        if (firebase.validateData(email, password) && firebase.validateData(email, binding.editTextConfirmPassword.text.toString())) {
            firebase.createAccount(email, password, username)
        }
    }
}
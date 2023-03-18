package com.example.moaiplanner.ui.welcome

import GoogleSignInHelper
import android.os.Bundle
import android.util.Patterns
import android.view.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.navOptions
import com.example.moaiplanner.R
import com.example.moaiplanner.data.user.AuthRepository
import com.example.moaiplanner.databinding.RegisterFragmentBinding
import com.example.moaiplanner.util.NetworkUtils
import com.google.android.material.bottomnavigation.BottomNavigationView


class RegisterFragment : Fragment() {

    lateinit var binding: RegisterFragmentBinding
    lateinit var firebase: AuthRepository
    private lateinit var googleSignInHelper: GoogleSignInHelper
    private val signInLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        googleSignInHelper.handleActivityResult(result.resultCode, result.data)
    }

    fun newInstance(): RegisterFragment? {
        return RegisterFragment()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = RegisterFragmentBinding.inflate(inflater, container, false)
        // Inflate il layout per il fragment
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        NetworkUtils.notifyMissingNetwork(requireContext(), view, activity)

        binding.buttonGoogleLogin.setOnClickListener {
            googleSignInHelper = GoogleSignInHelper(requireActivity(), signInLauncher, view)
            googleSignInHelper.signInGoogle()
        }

        binding.buttonSignUp.setOnClickListener {
            firebase = AuthRepository(requireActivity().application, view)
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
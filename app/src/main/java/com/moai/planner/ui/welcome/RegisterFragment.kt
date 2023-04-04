package com.moai.planner.ui.welcome

import android.os.Bundle
import android.view.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import com.moai.planner.data.user.GoogleSignInHelper
import com.moai.planner.data.user.UserAuthentication
import com.moai.planner.databinding.RegisterFragmentBinding
import com.moai.planner.util.NetworkUtils


class RegisterFragment : Fragment() {

    lateinit var binding: RegisterFragmentBinding
    lateinit var firebase: UserAuthentication
    private lateinit var googleSignInHelper: GoogleSignInHelper
    private val signInLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        googleSignInHelper.handleActivityResult(result.resultCode, result.data)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = RegisterFragmentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        NetworkUtils.notifyMissingNetwork(requireContext(), requireActivity())

        binding.buttonGoogleLogin.setOnClickListener {
            googleSignInHelper = GoogleSignInHelper(requireActivity(), signInLauncher, view)
            googleSignInHelper.signInGoogle()
        }

        binding.buttonSignUp.setOnClickListener {
            firebase = UserAuthentication(requireActivity().application, view, requireActivity())
            createAccount()
        }
    }



    private fun createAccount() {
        val email = binding.editTextEmail.text.toString()
        val password = binding.editTextPassword.text.toString()
        val username = binding.editTextUsername.text.toString()
        if (firebase.validateData(email, password) && firebase.validateData(email, binding.editTextConfirmPassword.text.toString())) {
            firebase.createAccount(email, password, username)
        }
    }
}
package com.example.moaiplanner.ui.welcome

import android.os.Bundle
import android.util.Patterns
import android.view.*
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.fragment.findNavController
import com.example.moaiplanner.R
import com.example.moaiplanner.data.repository.user.AuthRepository
import com.example.moaiplanner.databinding.SigninFragmentBinding


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
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        firebase = AuthRepository(requireActivity().application)
        binding.buttonSignIn.setOnClickListener {
            signIn()
            // TODO: Utilizzare corutines per evitare che finish accada prima di sigIn().
            if (firebase.isUserAuthenticated()) {
                findNavController().navigate(R.id.mainActivity)
            }
            activity?.finish()
        }
    }

    fun validateData(email: String, password: String): Boolean {
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.editTextEmail.error = "Email is invalid"
            return false
        }

        if (password.length < 6) {
            binding.editTextPassword.error = "Password length is invalid"
            return false
        }

        return true
    }

    fun signIn() {
        val email = binding.editTextEmail.text.toString()
        val password = binding.editTextPassword.text.toString()
        if (validateData(email, password)) {
            firebase.signIn(email, password)
        }
    }
}
package com.example.moaiplanner.ui.welcome

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.moaiplanner.R
import com.example.moaiplanner.data.repository.user.AuthRepository
import com.example.moaiplanner.databinding.WelcomeFragmentBinding

class WelcomeFragment : Fragment() {
    lateinit var binding: WelcomeFragmentBinding
    lateinit var firebase: AuthRepository
    private val REQ_ONE_TAP = 2
    private var showOneTapUI = true

    fun newInstance(): SigninFragment? {
        return SigninFragment()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = WelcomeFragmentBinding.inflate(inflater, container, false)

        // Inflate il layout per il fragment
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        firebase = AuthRepository(requireActivity().application)

        binding.buttonEmail.setOnClickListener {
            findNavController().navigate(R.id.signinFragment)
        }

        binding.buttonGoogle.setOnClickListener {
            findNavController().navigate(R.id.googleSignInActivity)
        }

        binding.textViewSignUpNow.setOnClickListener {
            findNavController().navigate(R.id.registerFragment)
        }
    }
}
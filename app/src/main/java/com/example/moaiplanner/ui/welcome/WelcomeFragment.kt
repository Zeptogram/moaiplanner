package com.example.moaiplanner.ui.welcome

import GoogleSignInHelper
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.navOptions
import com.example.moaiplanner.R
import com.example.moaiplanner.databinding.WelcomeFragmentBinding
import com.example.moaiplanner.util.NetworkUtils

class WelcomeFragment : Fragment() {
    lateinit var binding: WelcomeFragmentBinding
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
        binding = WelcomeFragmentBinding.inflate(inflater, container, false)
        return binding.root
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        NetworkUtils.notifyMissingNetwork(requireContext(), view, activity)

        binding.buttonEmail.setOnClickListener {
            findNavController().navigate(R.id.signinFragment, null,
                navOptions {
                    anim {
                        enter = android.R.anim.fade_in
                        popEnter = android.R.anim.fade_in
                    }
                }, null)
        }

        binding.buttonGoogleLogin.setOnClickListener {
            googleSignInHelper = GoogleSignInHelper(requireActivity(), signInLauncher, view)
            googleSignInHelper.signInGoogle()
        }

        binding.textViewSignUpNow.setOnClickListener {
            findNavController().navigate(R.id.registerFragment, null,
                navOptions {
                    anim {
                        enter = android.R.anim.fade_in
                        popEnter = android.R.anim.fade_in

                    }
                }, null)
        }
    }
}
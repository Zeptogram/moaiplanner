package com.moai.planner.ui.welcome

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import com.moai.planner.R
import com.moai.planner.data.user.GoogleSignInHelper
import com.moai.planner.databinding.WelcomeFragmentBinding
import com.moai.planner.util.NavigationHelper
import com.moai.planner.util.NetworkUtils

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
        NetworkUtils.notifyMissingNetwork(requireContext(), requireActivity())

        binding.buttonEmail.setOnClickListener {
            NavigationHelper.navigateTo(view, R.id.signinFragment)
        }

        binding.buttonGoogleLogin.setOnClickListener {
            googleSignInHelper = GoogleSignInHelper(requireActivity(), signInLauncher, view)
            googleSignInHelper.signInGoogle()
        }

        binding.textViewSignUpNow.setOnClickListener {
            NavigationHelper.navigateTo(view, R.id.registerFragment)
        }
    }
}
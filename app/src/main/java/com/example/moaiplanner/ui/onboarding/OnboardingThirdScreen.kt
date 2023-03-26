package com.example.moaiplanner.ui.onboarding

import android.content.Context
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import androidx.navigation.navOptions
import com.example.moaiplanner.R
import com.example.moaiplanner.databinding.OnboardingThirdScreenBinding


class OnboardingThirdScreen : Fragment() {

    private lateinit var binding: OnboardingThirdScreenBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        binding = OnboardingThirdScreenBinding.inflate(layoutInflater, container, false)

        binding.finish.setOnClickListener {
            findNavController().popBackStack()
            findNavController().navigate(R.id.welcomeFragment, null,
                navOptions {
                    anim {
                        enter = android.R.anim.fade_in
                        popEnter = android.R.anim.fade_in
                    }
                }, null)
            onBoardingFinished()
        }
        return binding.root
    }

    private fun onBoardingFinished() {
        val sharedPref = requireActivity().getSharedPreferences("onBoarding",Context.MODE_PRIVATE)
        val editor = sharedPref.edit()
        editor.putBoolean("Finished",true)
        editor.apply()
    }

}
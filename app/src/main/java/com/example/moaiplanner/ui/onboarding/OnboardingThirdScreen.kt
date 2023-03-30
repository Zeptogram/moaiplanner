package com.example.moaiplanner.ui.onboarding

import android.content.Context
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.moaiplanner.R
import com.example.moaiplanner.databinding.OnboardingThirdScreenBinding
import com.example.moaiplanner.util.NavigationHelper


class OnboardingThirdScreen : Fragment() {

    private lateinit var binding: OnboardingThirdScreenBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        binding = OnboardingThirdScreenBinding.inflate(layoutInflater, container, false)

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.finish.setOnClickListener {
            NavigationHelper.navigateToAndPop(view, R.id.welcomeFragment)
            onBoardingFinished()
        }
    }
    private fun onBoardingFinished() {
        val sharedPref = requireActivity().getSharedPreferences("onBoarding",Context.MODE_PRIVATE)
        val editor = sharedPref.edit()
        editor.putBoolean("Finished",true)
        editor.apply()
    }

}
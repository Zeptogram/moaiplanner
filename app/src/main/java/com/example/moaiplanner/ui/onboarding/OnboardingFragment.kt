package com.example.moaiplanner.ui.onboarding

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.moaiplanner.adapter.OnboardingAdapter
import com.example.moaiplanner.databinding.OnboardingViewPagerBinding

class OnboardingFragment : Fragment() {

    private lateinit var binding: OnboardingViewPagerBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        binding = OnboardingViewPagerBinding.inflate(inflater, container, false)

        val fragmentList = arrayListOf<Fragment>(
            OnboardingFirstScreen(),
            OnboardingSecondScreen(),
            OnboardingThirdScreen()
        )

        val adapter = OnboardingAdapter(
            fragmentList,
            requireActivity().supportFragmentManager,
            lifecycle
        )

        binding.viewPager.adapter = adapter


        return binding.root
    }

}
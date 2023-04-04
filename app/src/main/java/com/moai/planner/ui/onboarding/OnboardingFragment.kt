package com.moai.planner.ui.onboarding

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.moai.planner.adapter.OnboardingAdapter
import com.moai.planner.databinding.OnboardingViewPagerBinding

class OnboardingFragment : Fragment() {

    private lateinit var binding: OnboardingViewPagerBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        binding = OnboardingViewPagerBinding.inflate(inflater, container, false)

        val fragmentList = arrayListOf(
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
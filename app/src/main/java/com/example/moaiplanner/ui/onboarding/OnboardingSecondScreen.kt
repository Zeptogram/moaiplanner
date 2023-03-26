package com.example.moaiplanner.ui.onboarding


import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.viewpager2.widget.ViewPager2
import com.example.moaiplanner.R
import com.example.moaiplanner.databinding.OnboardingSecondScreenBinding

class OnboardingSecondScreen : Fragment() {

    private lateinit var binding: OnboardingSecondScreenBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        binding = OnboardingSecondScreenBinding.inflate(layoutInflater, container, false)

        val viewPager = activity?.findViewById<ViewPager2>(R.id.viewPager)

        binding.next2.setOnClickListener {
            viewPager?.currentItem = 2
        }
        return binding.root
    }

}
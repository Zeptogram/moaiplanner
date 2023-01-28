package com.example.moaiplanner.ui.main

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.example.moaiplanner.R
import com.example.moaiplanner.ui.repository.SettingsRepository
import com.example.moaiplanner.ui.utils.SettingsViewModelFactory
import com.example.moaiplanner.ui.view.SettingsViewModel

class TomatoFragment : Fragment() {
    private lateinit var settingsViewModel: SettingsViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {



        return inflater.inflate(R.layout.tomato_fragment, container, false)

    }

    override fun onStart() {
        super.onStart()
        settingsViewModel.restoreSettings()

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val factory = SettingsViewModelFactory(SettingsRepository(requireActivity()))
        settingsViewModel = ViewModelProvider(requireActivity(), factory)[SettingsViewModel::class.java]
        val sessionTextView = activity?.findViewById<TextView>(R.id.sessione)
        val breakTextView = activity?.findViewById<TextView>(R.id.pausa)

        // osservazione sui valori delle impostazioni
        settingsViewModel.session.observe(viewLifecycleOwner, Observer {
            sessionTextView?.text = settingsViewModel.session.value.toString()
        })
        settingsViewModel.pausa.observe(viewLifecycleOwner, Observer {
            breakTextView?.text = settingsViewModel.pausa.value.toString()
        })



    }


}



package com.example.moaiplanner.ui.main


import android.app.NotificationManager
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.example.moaiplanner.R
import com.example.moaiplanner.databinding.OptionsFragmentBinding
import com.example.moaiplanner.data.repository.settings.SettingsRepository
import com.example.moaiplanner.model.SettingsViewModelFactory
import com.example.moaiplanner.model.SettingsViewModel


class OptionsFragment : Fragment() {

    private lateinit var settingsViewModel: SettingsViewModel
    private lateinit var binding: OptionsFragmentBinding
    private lateinit var settingsRepository: SettingsRepository

    fun newInstance(): OptionsFragment? {
        return OptionsFragment()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        settingsViewModel.onSaveInstanceState(outState)
    }

    override fun onStart() {
        super.onStart()
        settingsViewModel.restoreSettings()
    }


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val toolbar = activity?.findViewById<androidx.appcompat.widget.Toolbar>(R.id.topAppBar)
        toolbar?.menu?.setGroupVisible(R.id.edit, false)
        toolbar?.menu?.setGroupVisible(R.id.sett, false)
        settingsRepository = SettingsRepository(requireActivity())
        val factory = SettingsViewModelFactory(SettingsRepository(requireActivity()))
        settingsViewModel = ViewModelProvider(requireActivity(), factory)[SettingsViewModel::class.java]
        if (savedInstanceState != null) {
            settingsViewModel.onRestoreInstanceState(savedInstanceState)
        }
        binding = OptionsFragmentBinding.inflate(inflater, container, false)
        binding.viewModel = settingsViewModel
        return binding.root


        // Inflate il layout per il fragment
        //return inflater.inflate(R.layout.options_fragment, container, false)



    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)


        // imposta valore sessione quando viene modificato
        binding.durataPomodoro.addTextChangedListener {
            //Log.d("SETTINGS", it.toString())
            if(it.toString().isBlank()) {
                settingsViewModel.session.value = "5"
            }

            else
                settingsViewModel.session.value = it.toString()
        }

        // imposta valore break quando viene modificato
        binding.durataPausa.addTextChangedListener {
            //Log.d("SETTINGS", it.toString())
            if(it.toString().isBlank()) {
                settingsViewModel.pausa.value = "1"
            }
            else
                settingsViewModel.pausa.value = it.toString()
        }

        binding.notificationSwitch.setOnCheckedChangeListener { _, isChecked ->
            settingsViewModel.notifiche.value = isChecked
            if (!isChecked) {
                val notificationManager = ContextCompat.getSystemService(requireActivity(), NotificationManager::class.java)
                notificationManager?.cancelAll()
            }
        }

    }

    override fun onStop() {
        super.onStop()
        settingsViewModel.saveSettings()
    }

    override fun onViewStateRestored(savedInstanceState: Bundle?) {
        super.onViewStateRestored(savedInstanceState)
        savedInstanceState?.let { settingsViewModel.onRestoreInstanceState(it) }
    }




}


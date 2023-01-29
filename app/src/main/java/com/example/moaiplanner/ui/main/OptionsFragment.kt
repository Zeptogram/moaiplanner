package com.example.moaiplanner.ui.main


import android.os.Bundle
import android.view.*
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.example.moaiplanner.R
import com.example.moaiplanner.databinding.OptionsFragmentBinding
import com.example.moaiplanner.data.repository.settings.SettingsRepository
import com.example.moaiplanner.data.repository.user.AuthRepository
import com.example.moaiplanner.model.SettingsViewModelFactory
import com.example.moaiplanner.model.SettingsViewModel
import com.example.moaiplanner.ui.welcome.SigninFragment


class OptionsFragment : Fragment() {

    private lateinit var settingsViewModel: SettingsViewModel
    private lateinit var binding: OptionsFragmentBinding
    private lateinit var settingsRepository: SettingsRepository
    private lateinit var firebase: AuthRepository

    fun newInstance(): OptionsFragment? {
        return OptionsFragment()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        settingsViewModel.onSaveInstanceState(outState)
    }


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        settingsRepository = SettingsRepository(requireActivity())
        val factory = SettingsViewModelFactory(SettingsRepository(requireActivity()))
        settingsViewModel = ViewModelProvider(requireActivity(), factory)[SettingsViewModel::class.java]
        if (savedInstanceState != null) {
            settingsViewModel.onRestoreInstanceState(savedInstanceState)
        }
        binding = OptionsFragmentBinding.inflate(inflater, container, false)
        binding.viewModel = settingsViewModel
        return binding.root
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        firebase = AuthRepository(requireActivity().application)
        
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
            if (!isChecked) {

            }
        }

        binding.buttonLogout.setOnClickListener() {
            firebase.signOut()
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


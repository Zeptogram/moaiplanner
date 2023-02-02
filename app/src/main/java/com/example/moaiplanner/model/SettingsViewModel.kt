package com.example.moaiplanner.model

import android.os.Bundle
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.moaiplanner.data.repository.settings.SettingsRepository



class SettingsViewModel(private val settingsRepository: SettingsRepository) : ViewModel() {

    var session: MutableLiveData<String?> = MutableLiveData("5")
    var pausa: MutableLiveData<String?> = MutableLiveData("1")
    var notifiche: MutableLiveData<Boolean?> = MutableLiveData(true)


    // Metodo per salvare i valori delle proprietà
    fun onSaveInstanceState(outState: Bundle) {
        outState.putString("session", session.value!!)
        outState.putString("pausa", pausa.value!!)
        outState.putBoolean("notifications_enabled", notifiche.value!!)
    }

    // Metodo per ripristinare i valori delle proprietà
    fun onRestoreInstanceState(savedInstanceState: Bundle) {
        session.value = savedInstanceState.getString("session")
        pausa.value = savedInstanceState.getString("pausa")
        notifiche.value = savedInstanceState.getBoolean("notifications_enabled")
    }

    fun saveSettings() {
        settingsRepository.saveSettings(session.value!!, pausa.value!!, notifiche.value!!)
    }

    // Metodo per ripristinare i valori delle proprietà
    fun restoreSettings() {
        val settings = settingsRepository.loadSettings()
        session.value = settings.first
        pausa.value = settings.second
        notifiche.value = settings.third
    }
}

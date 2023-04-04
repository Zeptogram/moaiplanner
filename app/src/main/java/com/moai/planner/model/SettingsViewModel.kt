package com.moai.planner.model

import android.os.Bundle
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.moai.planner.data.repository.settings.SettingsRepository



class SettingsViewModel(private val settingsRepository: SettingsRepository) : ViewModel() {

    var session: MutableLiveData<String?> = MutableLiveData("25")
    var pausa: MutableLiveData<String?> = MutableLiveData("5")
    var round: MutableLiveData<String?> = MutableLiveData("1")
    var notifiche: MutableLiveData<Boolean?> = MutableLiveData(true)
    var lightMode: MutableLiveData<Boolean?> = MutableLiveData(false)


    // Metodo per salvare i valori delle proprietà
    fun onSaveInstanceState(outState: Bundle) {
        outState.putString("session", session.value!!)
        outState.putString("pausa", pausa.value!!)
        outState.putString("round", round.value!!)
        outState.putBoolean("notifications_enabled", notifiche.value!!)
        outState.putBoolean("light_enabled", lightMode.value!!)
    }

    // Metodo per ripristinare i valori delle proprietà
    fun onRestoreInstanceState(savedInstanceState: Bundle) {
        session.value = savedInstanceState.getString("session")
        pausa.value = savedInstanceState.getString("pausa")
        round.value = savedInstanceState.getString("round")
        notifiche.value = savedInstanceState.getBoolean("notifications_enabled")
        lightMode.value = savedInstanceState.getBoolean("light_enabled")
    }

    fun saveSettings() {
        settingsRepository.saveSettings(session.value!!, pausa.value!!, round.value!!, notifiche.value!!, lightMode.value!!)
    }

    // Metodo per ripristinare i valori delle proprietà
    fun restoreSettings() {
        val settings = settingsRepository.loadSettings()
        session.value = settings.first
        pausa.value = settings.second
        round.value = settings.third
        notifiche.value = settings.fourth
        lightMode.value = settings.fifth
    }
}

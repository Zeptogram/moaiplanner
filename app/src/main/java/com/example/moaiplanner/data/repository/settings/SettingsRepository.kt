package com.example.moaiplanner.data.repository.settings

import android.content.Context
import com.example.moaiplanner.util.Quintuple

class SettingsRepository(context: Context) {
    private val sharedPreferences = context.getSharedPreferences("Options", Context.MODE_PRIVATE)

    fun saveSettings(session: String, pausa: String, round: String, notifiche: Boolean, lightMode: Boolean) {
        sharedPreferences.edit().apply {
            putString("session", session)
            putString("pausa", pausa)
            putString("round", round)
            putBoolean("notifications_enabled", notifiche)
            putBoolean("light_enabled", lightMode)
            apply()
        }
    }

    fun loadSettings(): Quintuple<String, String, String, Boolean, Boolean> {
        val session = sharedPreferences.getString("session", "25")
        val pausa = sharedPreferences.getString("pausa", "5")
        val round = sharedPreferences.getString("round", "1")
        var notifiche = sharedPreferences.getBoolean("notifications_enabled", true)
        var lightMode = sharedPreferences.getBoolean("light_enabled", false)


        return Quintuple(session ?: "25", pausa ?: "5", round?: "1", notifiche ?: true, lightMode ?: false)
    }
}


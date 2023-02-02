package com.example.moaiplanner.data.repository.settings

import android.content.Context


class SettingsRepository(context: Context) {
    private val sharedPreferences = context.getSharedPreferences("Options", Context.MODE_PRIVATE)

    fun saveSettings(session: String, pausa: String, notifiche: Boolean) {
        sharedPreferences.edit().apply {
            putString("session", session)
            putString("pausa", pausa)
            putBoolean("notifications_enabled", notifiche)
            apply()
        }
    }

    fun loadSettings(): Triple<String, String, Boolean> {
        val session = sharedPreferences.getString("session", "5")
        val pausa = sharedPreferences.getString("pausa", "1")
        var notifiche = sharedPreferences.getBoolean("notifications_enabled", true)

        return Triple(session ?: "5", pausa ?: "1", notifiche ?: true)
    }
}


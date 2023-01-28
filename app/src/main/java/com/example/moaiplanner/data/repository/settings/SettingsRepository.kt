package com.example.moaiplanner.data.repository.settings

import android.content.Context

class SettingsRepository(context: Context) {
    private val sharedPreferences = context.getSharedPreferences("Options", Context.MODE_PRIVATE)

    fun saveSettings(session: String, pausa: String) {
        sharedPreferences.edit().apply {
            putString("session", session)
            putString("pausa", pausa)
            apply()
        }
    }

    fun loadSettings(): Pair<String, String> {
        val session = sharedPreferences.getString("session", "")
        val pausa = sharedPreferences.getString("pausa", "")
        return Pair(session ?: "", pausa ?: "")
    }
}


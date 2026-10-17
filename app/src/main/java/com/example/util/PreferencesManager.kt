package com.example.util

import android.content.Context
import android.content.SharedPreferences

class PreferencesManager(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("deepfinder_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_THEME_MODE = "theme_mode" // "SYSTEM", "DARK", "LIGHT"
        private const val KEY_BIOMETRIC_ENABLED = "biometric_enabled"
        private const val KEY_ONBOARDING_COMPLETED = "onboarding_completed"
        private const val KEY_LAST_INDEX_TIME = "last_index_time"
        private const val KEY_EXCLUDE_HIDDEN = "exclude_hidden"
    }

    var themeMode: String
        get() = prefs.getString(KEY_THEME_MODE, "SYSTEM") ?: "SYSTEM"
        set(value) = prefs.edit().putString(KEY_THEME_MODE, value).apply()

    var isBiometricEnabled: Boolean
        get() = prefs.getBoolean(KEY_BIOMETRIC_ENABLED, false)
        set(value) = prefs.edit().putBoolean(KEY_BIOMETRIC_ENABLED, value).apply()

    var isOnboardingCompleted: Boolean
        get() = prefs.getBoolean(KEY_ONBOARDING_COMPLETED, false)
        set(value) = prefs.edit().putBoolean(KEY_ONBOARDING_COMPLETED, value).apply()

    var lastIndexTime: Long
        get() = prefs.getLong(KEY_LAST_INDEX_TIME, 0L)
        set(value) = prefs.edit().putLong(KEY_LAST_INDEX_TIME, value).apply()

    var excludeHidden: Boolean
        get() = prefs.getBoolean(KEY_EXCLUDE_HIDDEN, true)
        set(value) = prefs.edit().putBoolean(KEY_EXCLUDE_HIDDEN, value).apply()
}

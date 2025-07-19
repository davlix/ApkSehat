package com.davlix.apksehat.utils


import android.content.Context
import android.content.SharedPreferences

class AppPreferences(context: Context) {

    private val PREFS_NAME = "AppSehatPrefs"
    private val KEY_CYCLE_LENGTH = "cycle_length"
    private val KEY_PERIOD_LENGTH = "period_length"
    private val KEY_NOTIFICATION_ENABLED = "notification_enabled"

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    var cycleLength: Int
        get() = prefs.getInt(KEY_CYCLE_LENGTH, 28) // Default 28 hari
        set(value) = prefs.edit().putInt(KEY_CYCLE_LENGTH, value).apply()

    var periodLength: Int
        get() = prefs.getInt(KEY_PERIOD_LENGTH, 7) // Default 7 hari
        set(value) = prefs.edit().putInt(KEY_PERIOD_LENGTH, value).apply()

    var notificationsEnabled: Boolean
        get() = prefs.getBoolean(KEY_NOTIFICATION_ENABLED, true) // Default true
        set(value) = prefs.edit().putBoolean(KEY_NOTIFICATION_ENABLED, value).apply()
}
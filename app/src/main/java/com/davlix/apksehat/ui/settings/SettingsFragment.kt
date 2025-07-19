package com.davlix.apksehat.ui.settings


import android.os.Bundle
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceManager
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.davlix.apksehat.R
import com.davlix.apksehat.utils.AppPreferences
import com.davlix.apksehat.worker.NotificationWorker
import java.util.concurrent.TimeUnit

class SettingsFragment : PreferenceFragmentCompat() {
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.root_preferences, rootKey)

        // Listener untuk perubahan preferensi notifikasi
        findPreference<androidx.preference.SwitchPreferenceCompat>("notification_enabled")?.setOnPreferenceChangeListener { preference, newValue ->
            val isEnabled = newValue as Boolean
            val appPreferences = AppPreferences(requireContext())
            appPreferences.notificationsEnabled = isEnabled
            scheduleOrCancelNotificationWork(isEnabled, appPreferences.cycleLength)
            true
        }

        // Listener untuk perubahan panjang siklus
        findPreference<androidx.preference.EditTextPreference>("cycle_length")?.setOnPreferenceChangeListener { preference, newValue ->
            val newCycleLength = (newValue as String).toIntOrNull()
            if (newCycleLength != null && newCycleLength > 0) {
                val appPreferences = AppPreferences(requireContext())
                appPreferences.cycleLength = newCycleLength
                // Perbarui jadwal notifikasi jika notifikasi aktif
                if (appPreferences.notificationsEnabled) {
                    scheduleOrCancelNotificationWork(true, newCycleLength)
                }
                true
            } else {
                // Tampilkan pesan error jika input tidak valid
                false
            }
        }
    }

    private fun scheduleOrCancelNotificationWork(isEnabled: Boolean, cycleLength: Int) {
        if (isEnabled) {
            val notificationIntervalDays = cycleLength.toLong()
            val notificationWorkRequest = PeriodicWorkRequestBuilder<NotificationWorker>(
                notificationIntervalDays, TimeUnit.DAYS
            )
                .addTag("notification_work_tag")
                .build()

            WorkManager.getInstance(requireContext()).enqueueUniquePeriodicWork(
                "PeriodNotificationWork",
                ExistingPeriodicWorkPolicy.UPDATE,
                notificationWorkRequest
            )
        } else {
            WorkManager.getInstance(requireContext()).cancelUniqueWork("PeriodNotificationWork")
        }
    }
}
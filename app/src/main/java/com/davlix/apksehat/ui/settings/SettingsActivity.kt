package com.davlix.apksehat.ui.settings

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.davlix.apksehat.R

class SettingsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        if (savedInstanceState == null) {
            supportFragmentManager
                .beginTransaction()
                .replace(R.id.settings_container, SettingsFragment())
                .commit()
        }
        supportActionBar?.setDisplayHomeAsUpEnabled(true) // Menampilkan tombol kembali di ActionBar
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed() // Kembali ke activity sebelumnya
        return true
    }
}
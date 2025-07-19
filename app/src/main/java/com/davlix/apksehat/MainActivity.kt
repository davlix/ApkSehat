package com.davlix.apksehat

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.text.InputType
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.ViewModelProvider
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.davlix.apksehat.data.AppDatabase
import com.davlix.apksehat.data.MenstrualPeriod
import com.davlix.apksehat.databinding.ActivityMainBinding
import com.davlix.apksehat.ui.MenstrualPeriodViewModel
import com.davlix.apksehat.ui.MenstrualPeriodViewModelFactory
import com.davlix.apksehat.ui.settings.SettingsActivity
import com.davlix.apksehat.utils.AppPreferences
import com.davlix.apksehat.worker.NotificationWorker
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding // Deklarasi binding
    private lateinit var menstrualPeriodViewModel: MenstrualPeriodViewModel
    private lateinit var appPreferences: AppPreferences

    // Untuk meminta izin notifikasi di Android 13+
    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            if (isGranted) {
                Toast.makeText(this, "Izin notifikasi diberikan.", Toast.LENGTH_SHORT).show()
                scheduleNotificationWork()
            } else {
                Toast.makeText(this, "Izin notifikasi ditolak. Pengingat mungkin tidak berfungsi.", Toast.LENGTH_LONG).show()
            }
        }

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // Inisialisasi View Binding
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root) // Set content view dari root binding
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Inisialisasi ViewModel dan Preferences
        val database = AppDatabase.getDatabase(applicationContext)
        val dao = database.menstrualPeriodDao()
        val factory = MenstrualPeriodViewModelFactory(dao)
        menstrualPeriodViewModel = ViewModelProvider(this, factory)[MenstrualPeriodViewModel::class.java]
        appPreferences = AppPreferences(applicationContext)

        // Amati semua periode dari ViewModel
        menstrualPeriodViewModel.allPeriods.observe(this) { periods ->
            if (periods.isNotEmpty()) {
                val latestPeriod = periods.first() // Ambil periode terakhir
                val startDateFormatted = SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(latestPeriod.startDate))
                val endDateFormatted = latestPeriod.endDate?.let {
                    SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(it))
                } ?: "Belum Selesai"
                binding.tvCurrentDate.text = "Terakhir: $startDateFormatted - $endDateFormatted"

                // Tampilkan semua periode dalam TextView (contoh sederhana)
                val allPeriodsText = StringBuilder("Daftar Periode:\n")
                periods.forEach { period ->
                    val start = SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(period.startDate))
                    val end = period.endDate?.let { SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(it)) } ?: "Belum Selesai"
                    allPeriodsText.append("ID: ${period.id}, Mulai: $start, Selesai: $end, Catatan: ${period.notes ?: "Tidak ada"}\n")
                }
                binding.tvAllPeriods.text = allPeriodsText.toString()
            } else {
                binding.tvCurrentDate.text = "Belum ada data periode."
                binding.tvAllPeriods.text = "Daftar Periode:\nBelum ada data."
            }
        }

        // Listener untuk tombol "Tambah Periode"
        binding.btnAddPeriod.setOnClickListener {
            showAddPeriodDialog()
        }

        // Listener untuk tombol "Pengaturan"
        binding.btnSettings.setOnClickListener {
            val intent = Intent(this, SettingsActivity::class.java)
            startActivity(intent)
        }

        // Jadwalkan notifikasi saat aplikasi dimulai (setelah mendapatkan izin jika diperlukan)
        askNotificationPermission()
    }

    private fun showAddPeriodDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Tambah Periode Menstruasi Baru")

        val layout = LinearLayout(this)
        layout.orientation = LinearLayout.VERTICAL
        layout.setPadding(50, 20, 50, 20)

        val startDateInput = EditText(this).apply {
            hint = "Tanggal Mulai (YYYY-MM-DD)"
            inputType = InputType.TYPE_CLASS_DATETIME or InputType.TYPE_DATETIME_VARIATION_DATE
        }
        layout.addView(startDateInput)

        val endDateInput = EditText(this).apply {
            hint = "Tanggal Selesai (Opsional, YYYY-MM-DD)"
            inputType = InputType.TYPE_CLASS_DATETIME or InputType.TYPE_DATETIME_VARIATION_DATE
        }
        layout.addView(endDateInput)

        val notesInput = EditText(this).apply {
            hint = "Catatan (Opsional)"
            inputType = InputType.TYPE_CLASS_TEXT
        }
        layout.addView(notesInput)

        builder.setView(layout)

        builder.setPositiveButton("Tambah") { dialog, _ ->
            val startDateStr = startDateInput.text.toString()
            val endDateStr = endDateInput.text.toString()
            val notes = notesInput.text.toString().ifEmpty { null }

            try {
                val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                val startDateMillis = dateFormat.parse(startDateStr)?.time
                    ?: throw IllegalArgumentException("Tanggal mulai tidak valid.")

                val endDateMillis = if (endDateStr.isNotEmpty()) {
                    dateFormat.parse(endDateStr)?.time
                        ?: throw IllegalArgumentException("Tanggal selesai tidak valid.")
                } else {
                    null
                }

                val newPeriod = MenstrualPeriod(
                    startDate = startDateMillis,
                    endDate = endDateMillis,
                    notes = notes
                )
                menstrualPeriodViewModel.insert(newPeriod)
                Toast.makeText(this, "Periode berhasil ditambahkan!", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_LONG).show()
            }
            dialog.dismiss()
        }

        builder.setNegativeButton("Batal") { dialog, _ ->
            dialog.cancel()
        }

        builder.show()
    }

    private fun askNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) { // Android 13 (API 33)
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) ==
                PackageManager.PERMISSION_GRANTED
            ) {
                // Izin sudah diberikan
                scheduleNotificationWork()
            } else if (shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS)) {
                // Tampilkan penjelasan mengapa izin diperlukan
                AlertDialog.Builder(this)
                    .setTitle("Izin Notifikasi Diperlukan")
                    .setMessage("AppSehat membutuhkan izin notifikasi untuk mengirimkan pengingat siklus menstruasi Anda.")
                    .setPositiveButton("Berikan Izin") { _, _ ->
                        requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                    .setNegativeButton("Tidak Sekarang") { _, _ ->
                        Toast.makeText(this, "Pengingat dinonaktifkan tanpa izin notifikasi.", Toast.LENGTH_LONG).show()
                    }
                    .show()
            } else {
                // Langsung meminta izin
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        } else {
            // Untuk versi Android di bawah API 33, izin notifikasi tidak diperlukan secara eksplisit
            scheduleNotificationWork()
        }
    }

    private fun scheduleNotificationWork() {
        // Hanya jadwalkan jika notifikasi diaktifkan di pengaturan
        if (!appPreferences.notificationsEnabled) {
            WorkManager.getInstance(applicationContext).cancelUniqueWork("PeriodNotificationWork")
            return
        }

        val cycleLength = appPreferences.cycleLength // Ambil panjang siklus dari Shared Preferences

        val notificationIntervalDays = cycleLength.toLong()

        val notificationWorkRequest = PeriodicWorkRequestBuilder<NotificationWorker>(
            notificationIntervalDays, TimeUnit.DAYS
        )
            .addTag("notification_work_tag")
            .build()

        WorkManager.getInstance(applicationContext).enqueueUniquePeriodicWork(
            "PeriodNotificationWork",
            ExistingPeriodicWorkPolicy.UPDATE,
            notificationWorkRequest
        )
        Toast.makeText(this, "Pengingat dijadwalkan setiap $cycleLength hari.", Toast.LENGTH_SHORT).show()
    }
}
package com.davlix.apksehat.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.davlix.apksehat.MainActivity
import com.davlix.apksehat.R
import com.davlix.apksehat.utils.AppPreferences
import java.util.concurrent.TimeUnit
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class NotificationWorker(appContext: Context, workerParams: WorkerParameters) : Worker(appContext, workerParams) {

    override fun doWork(): Result {
        val appPreferences = AppPreferences(applicationContext)
        if (!appPreferences.notificationsEnabled) {
            return Result.success() // Jangan kirim notifikasi jika dinonaktifkan
        }

        // Dapatkan panjang siklus dari preferensi
        val cycleLength = appPreferences.cycleLength

        // Asumsi: Kita akan memicu notifikasi untuk "periode berikutnya"
        // Di aplikasi nyata, Anda akan mengambil *tanggal terakhir* periode dari database Room
        // dan menghitung tanggal berikutnya berdasarkan 'cycleLength'.
        // Untuk demo ini, kita hanya akan memprediksi tanggal di masa depan dari waktu sekarang.
        val nextPeriodDateMillis = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, cycleLength) // Tambah panjang siklus dari hari ini
        }.timeInMillis

        val formattedDate = SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(nextPeriodDateMillis)

        val title = "Pengingat Periode AppSehat"
        val message = "Periode berikutnya Anda diperkirakan pada $formattedDate."

        sendNotification(title, message)
        return Result.success()
    }

    private fun sendNotification(title: String, message: String) {
        val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "app_sehat_channel"
        val channelName = "Pengingat Siklus Menstruasi"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) { // Android 8.0 (Oreo) ke atas
            val channel = NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_HIGH).apply {
                description = "Channel untuk notifikasi pengingat periode AppSehat"
            }
            notificationManager.createNotificationChannel(channel)
        }

        val intent = Intent(applicationContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        // FLAG_IMMUTABLE diperlukan untuk Android 12 (API 31) ke atas
        val pendingIntent: PendingIntent = PendingIntent.getActivity(applicationContext, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        val notificationBuilder = NotificationCompat.Builder(applicationContext, channelId)
            .setSmallIcon(R.drawable.ic_notification) // Anda harus membuat resource ini!
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        notificationManager.notify(1, notificationBuilder.build())
    }
}
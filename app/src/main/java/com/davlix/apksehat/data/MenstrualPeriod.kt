package com.davlix.apksehat.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "menstrual_periods")
data class MenstrualPeriod(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val startDate: Long, // Menggunakan Long untuk timestamp (milidetik sejak epoch)
    val endDate: Long?, // Nullable jika periode belum berakhir
    val notes: String?
)
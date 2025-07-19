package com.davlix.apksehat.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface MenstrualPeriodDao {
    @Insert
    suspend fun insert(period: MenstrualPeriod)

    @Update
    suspend fun update(period: MenstrualPeriod)

    @Query("SELECT * FROM menstrual_periods ORDER BY startDate DESC")
    fun getAllPeriods(): Flow<List<MenstrualPeriod>>

    @Query("SELECT * FROM menstrual_periods WHERE id = :id")
    suspend fun getPeriodById(id: Int): MenstrualPeriod?

    @Query("DELETE FROM menstrual_periods WHERE id = :id")
    suspend fun deletePeriodById(id: Int)

    @Query("SELECT * FROM menstrual_periods WHERE startDate BETWEEN :startOfDay AND :endOfDay")
    fun getPeriodsForDay(startOfDay: Long, endOfDay: Long): Flow<List<MenstrualPeriod>>
}
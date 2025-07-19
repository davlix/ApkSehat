package com.davlix.apksehat.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.davlix.apksehat.data.MenstrualPeriod
import com.davlix.apksehat.data.MenstrualPeriodDao
import kotlinx.coroutines.launch

class MenstrualPeriodViewModel(private val dao: MenstrualPeriodDao) : ViewModel() {

    val allPeriods = dao.getAllPeriods().asLiveData()

    fun insert(period: MenstrualPeriod) {
        viewModelScope.launch {
            dao.insert(period)
        }
    }

    fun update(period: MenstrualPeriod) {
        viewModelScope.launch {
            dao.update(period)
        }
    }

    fun deletePeriodById(id: Int) {
        viewModelScope.launch {
            dao.deletePeriodById(id)
        }
    }

    fun getPeriodById(id: Int) = viewModelScope.launch {
        dao.getPeriodById(id)
    }

    fun getPeriodsForDay(startOfDay: Long, endOfDay: Long) =
        dao.getPeriodsForDay(startOfDay, endOfDay).asLiveData()
}

// Factory untuk membuat ViewModel dengan argumen kustom (DAO)
class MenstrualPeriodViewModelFactory(private val dao: MenstrualPeriodDao) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MenstrualPeriodViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return MenstrualPeriodViewModel(dao) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
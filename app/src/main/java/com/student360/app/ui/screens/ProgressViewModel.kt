package com.student360.app.ui.screens

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.student360.app.data.local.entity.AttendanceRecord
import com.student360.app.data.local.entity.AttendanceStatus
import com.student360.app.data.local.entity.CollegeDay
import com.student360.app.data.local.entity.DayStatus
import com.student360.app.data.local.entity.Subject
import com.student360.app.data.repository.StudentRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.util.*

class ProgressViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = StudentRepository(application)

    private val _attendanceRecords = MutableStateFlow<List<AttendanceRecord>>(emptyList())
    val attendanceRecords: StateFlow<List<AttendanceRecord>> = _attendanceRecords.asStateFlow()

    private val _collegeDays = MutableStateFlow<List<CollegeDay>>(emptyList())
    val collegeDays: StateFlow<List<CollegeDay>> = _collegeDays.asStateFlow()

    private val _subjects = MutableStateFlow<List<Subject>>(emptyList())
    val subjects: StateFlow<List<Subject>> = _subjects.asStateFlow()

    private val _trendsText = MutableStateFlow("")
    val trendsText: StateFlow<String> = _trendsText.asStateFlow()

    init {
        viewModelScope.launch {
            repository.allAttendanceFlow.collectLatest {
                _attendanceRecords.value = it
                calculateTrends()
            }
        }
        viewModelScope.launch {
            repository.collegeDaysFlow.collectLatest {
                _collegeDays.value = it
                calculateTrends()
            }
        }
        viewModelScope.launch {
            repository.subjectsFlow.collectLatest {
                _subjects.value = it
                calculateTrends()
            }
        }
    }

    private fun calculateTrends() {
        val records = _attendanceRecords.value
        if (records.isEmpty()) {
            _trendsText.value = "Not enough attendance data to compute trends yet."
            return
        }

        val now = Calendar.getInstance()
        val currentMonth = now.get(Calendar.MONTH)
        val currentYear = now.get(Calendar.YEAR)

        val lastMonthCal = Calendar.getInstance().apply {
            add(Calendar.MONTH, -1)
        }
        val prevMonth = lastMonthCal.get(Calendar.MONTH)
        val prevYear = lastMonthCal.get(Calendar.YEAR)

        val currentMonthRecords = records.filter {
            val cal = Calendar.getInstance().apply { timeInMillis = it.date }
            cal.get(Calendar.MONTH) == currentMonth && cal.get(Calendar.YEAR) == currentYear
        }

        val prevMonthRecords = records.filter {
            val cal = Calendar.getInstance().apply { timeInMillis = it.date }
            cal.get(Calendar.MONTH) == prevMonth && cal.get(Calendar.YEAR) == prevYear
        }

        fun getPercentage(list: List<AttendanceRecord>): Double {
            val attended = list.count { it.status == AttendanceStatus.PRESENT }
            val missed = list.count { it.status == AttendanceStatus.ABSENT }
            val total = attended + missed
            return if (total > 0) (attended.toDouble() / total.toDouble()) * 100.0 else 0.0
        }

        val currentPercent = getPercentage(currentMonthRecords)
        val prevPercent = getPercentage(prevMonthRecords)

        if (prevMonthRecords.isEmpty()) {
            _trendsText.value = "Current month attendance is ${String.format("%.1f", currentPercent)}%."
        } else {
            val diff = currentPercent - prevPercent
            val statusStr = if (diff >= 0) "increased" else "decreased"
            _trendsText.value = "Attendance $statusStr by ${String.format("%.1f", Math.abs(diff))}% this month compared to last month."
        }
    }

    fun setCollegeDayStatus(date: Long, status: DayStatus, notes: String?) {
        viewModelScope.launch {
            val normalizedDate = normalizeToMidnight(date)
            repository.insertCollegeDay(CollegeDay(normalizedDate, status, notes))
        }
    }

    fun normalizeToMidnight(timeMillis: Long): Long {
        val cal = Calendar.getInstance().apply {
            timeInMillis = timeMillis
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return cal.timeInMillis
    }
}

package com.student360.app.ui.screens

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.student360.app.data.local.entity.Alert
import com.student360.app.data.repository.StudentRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class AlertsViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = StudentRepository(application)

    private val _alerts = MutableStateFlow<List<Alert>>(emptyList())
    val alerts: StateFlow<List<Alert>> = _alerts.asStateFlow()

    init {
        viewModelScope.launch {
            repository.alertsFlow.collectLatest {
                _alerts.value = it
            }
        }
    }

    fun markAsRead(alertId: Int) {
        viewModelScope.launch {
            repository.markAlertAsRead(alertId)
        }
    }

    fun markAllAsRead() {
        viewModelScope.launch {
            repository.markAllAlertsAsRead()
        }
    }

    fun clearAll() {
        viewModelScope.launch {
            repository.deleteAllAlerts()
        }
    }
}

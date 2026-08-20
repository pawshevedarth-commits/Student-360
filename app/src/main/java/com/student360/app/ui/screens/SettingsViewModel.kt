package com.student360.app.ui.screens

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.student360.app.data.local.entity.StudentProfile
import com.student360.app.data.repository.StudentRepository
import com.student360.app.service.BackupRestoreManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.io.InputStream
import java.io.OutputStream

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = StudentRepository(application)

    private val _profile = MutableStateFlow<StudentProfile?>(null)
    val profile: StateFlow<StudentProfile?> = _profile.asStateFlow()

    init {
        viewModelScope.launch {
            repository.profileFlow.collectLatest {
                _profile.value = it
            }
        }
    }

    fun updateProfile(
        name: String,
        rollNumber: String,
        branch: String,
        semester: Int,
        division: String,
        collegeName: String
    ) {
        viewModelScope.launch {
            val current = _profile.value
            val updated = StudentProfile(
                id = 1,
                name = name,
                rollNumber = rollNumber,
                branch = branch,
                semester = semester,
                division = division,
                collegeName = collegeName,
                onboarded = current?.onboarded ?: true
            )
            repository.saveProfile(updated)
        }
    }

    fun backupData(outputStream: OutputStream, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val success = BackupRestoreManager.exportBackup(repository, outputStream)
            onResult(success)
        }
    }

    fun restoreData(inputStream: InputStream, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val success = BackupRestoreManager.importBackup(getApplication(), repository, inputStream)
            onResult(success)
        }
    }
}

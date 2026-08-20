package com.student360.app.data.local.dao

import androidx.room.*
import com.student360.app.data.local.entity.Alert
import kotlinx.coroutines.flow.Flow

@Dao
interface AlertDao {
    @Query("SELECT * FROM alerts ORDER BY timestamp DESC")
    fun getAllAlertsFlow(): Flow<List<Alert>>

    @Query("SELECT * FROM alerts ORDER BY timestamp DESC")
    suspend fun getAllAlerts(): List<Alert>

    @Query("SELECT * FROM alerts WHERE isRead = 0 ORDER BY timestamp DESC")
    fun getUnreadAlertsFlow(): Flow<List<Alert>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAlert(alert: Alert): Long

    @Query("UPDATE alerts SET isRead = 1 WHERE id = :alertId")
    suspend fun markAsRead(alertId: Int)

    @Query("UPDATE alerts SET isRead = 1")
    suspend fun markAllAsRead()

    @Delete
    suspend fun deleteAlert(alert: Alert)

    @Query("DELETE FROM alerts")
    suspend fun deleteAllAlerts()
}

package com.student360.app.data.local.dao

import androidx.room.*
import com.student360.app.data.local.entity.Goal
import kotlinx.coroutines.flow.Flow

@Dao
interface GoalDao {
    @Query("SELECT * FROM goals ORDER BY deadline ASC")
    fun getAllGoalsFlow(): Flow<List<Goal>>

    @Query("SELECT * FROM goals ORDER BY deadline ASC")
    suspend fun getAllGoals(): List<Goal>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGoal(goal: Goal): Long

    @Update
    suspend fun updateGoal(goal: Goal)

    @Delete
    suspend fun deleteGoal(goal: Goal)
}

package com.student360.app.data.local.dao

import androidx.room.*
import com.student360.app.data.local.entity.Task
import com.student360.app.data.local.entity.Assignment
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {
    // Tasks queries
    @Query("SELECT * FROM tasks ORDER BY dueDate ASC, priority DESC")
    fun getAllTasksFlow(): Flow<List<Task>>

    @Query("SELECT * FROM tasks ORDER BY dueDate ASC, priority DESC")
    suspend fun getAllTasks(): List<Task>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: Task): Long

    @Update
    suspend fun updateTask(task: Task)

    @Delete
    suspend fun deleteTask(task: Task)

    // Assignments queries
    @Query("SELECT * FROM assignments ORDER BY dueDate ASC, priority DESC")
    fun getAllAssignmentsFlow(): Flow<List<Assignment>>

    @Query("SELECT * FROM assignments ORDER BY dueDate ASC, priority DESC")
    suspend fun getAllAssignments(): List<Assignment>

    @Query("SELECT * FROM assignments WHERE id = :id")
    suspend fun getAssignmentById(id: Int): Assignment?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAssignment(assignment: Assignment): Long

    @Update
    suspend fun updateAssignment(assignment: Assignment)

    @Delete
    suspend fun deleteAssignment(assignment: Assignment)
}

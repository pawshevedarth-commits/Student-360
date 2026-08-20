package com.student360.app.data.local.dao

import androidx.room.*
import com.student360.app.data.local.entity.Exam
import com.student360.app.data.local.entity.ExamTopic
import kotlinx.coroutines.flow.Flow

@Dao
interface ExamDao {
    @Query("SELECT * FROM exams ORDER BY date ASC")
    fun getAllExamsFlow(): Flow<List<Exam>>

    @Query("SELECT * FROM exams ORDER BY date ASC")
    suspend fun getAllExams(): List<Exam>

    @Query("SELECT * FROM exams WHERE id = :id")
    suspend fun getExamById(id: Int): Exam?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExam(exam: Exam): Long

    @Update
    suspend fun updateExam(exam: Exam)

    @Delete
    suspend fun deleteExam(exam: Exam)

    // Exam syllabus topics
    @Query("SELECT * FROM exam_topics WHERE examId = :examId")
    fun getTopicsForExamFlow(examId: Int): Flow<List<ExamTopic>>

    @Query("SELECT * FROM exam_topics WHERE examId = :examId")
    suspend fun getTopicsForExam(examId: Int): List<ExamTopic>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTopic(topic: ExamTopic): Long

    @Update
    suspend fun updateTopic(topic: ExamTopic)

    @Delete
    suspend fun deleteTopic(topic: ExamTopic)
}

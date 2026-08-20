package com.student360.app.data.local

import androidx.room.TypeConverter
import com.student360.app.data.local.entity.*

class Converters {
    @TypeConverter
    fun toAttendanceStatus(value: String) = enumValueOf<AttendanceStatus>(value)
    @TypeConverter
    fun fromAttendanceStatus(status: AttendanceStatus) = status.name

    @TypeConverter
    fun toDayStatus(value: String) = enumValueOf<DayStatus>(value)
    @TypeConverter
    fun fromDayStatus(status: DayStatus) = status.name

    @TypeConverter
    fun toExamType(value: String) = enumValueOf<ExamType>(value)
    @TypeConverter
    fun fromExamType(type: ExamType) = type.name

    @TypeConverter
    fun toTopicStatus(value: String) = enumValueOf<TopicStatus>(value)
    @TypeConverter
    fun fromTopicStatus(status: TopicStatus) = status.name

    @TypeConverter
    fun toAssignmentPriority(value: String) = enumValueOf<AssignmentPriority>(value)
    @TypeConverter
    fun fromAssignmentPriority(priority: AssignmentPriority) = priority.name

    @TypeConverter
    fun toAssignmentStatus(value: String) = enumValueOf<AssignmentStatus>(value)
    @TypeConverter
    fun fromAssignmentStatus(status: AssignmentStatus) = status.name

    @TypeConverter
    fun toTaskCategory(value: String) = enumValueOf<TaskCategory>(value)
    @TypeConverter
    fun fromTaskCategory(category: TaskCategory) = category.name

    @TypeConverter
    fun toTaskPriority(value: String) = enumValueOf<TaskPriority>(value)
    @TypeConverter
    fun fromTaskPriority(priority: TaskPriority) = priority.name

    @TypeConverter
    fun toGoalStatus(value: String) = enumValueOf<GoalStatus>(value)
    @TypeConverter
    fun fromGoalStatus(status: GoalStatus) = status.name

    @TypeConverter
    fun toAlertType(value: String) = enumValueOf<AlertType>(value)
    @TypeConverter
    fun fromAlertType(type: AlertType) = type.name
}

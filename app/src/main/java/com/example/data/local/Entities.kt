package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import java.util.UUID

class RoomConverters {
  @TypeConverter
  fun fromStringList(list: List<String>): String = list.joinToString("\u001F")

  @TypeConverter
  fun toStringList(raw: String): List<String> =
    if (raw.isEmpty()) emptyList() else raw.split("\u001F")

  @TypeConverter
  fun fromIntList(list: List<Int>): String = list.joinToString(",")

  @TypeConverter
  fun toIntList(raw: String): List<Int> =
    if (raw.isBlank()) emptyList() else raw.split(",").mapNotNull { it.toIntOrNull() }
}

@Entity(tableName = "user_profiles")
data class UserProfileEntity(
  @PrimaryKey val id: String = "local_user",
  val userId: String = "",
  val displayName: String = "",
  val email: String = "",
  val educationLevel: String = "school",
  val categoryDetail: String = "",
  val photoUri: String = "",
  val dailyGoalMinutes: Int = 180,
  val weeklyGoalMinutes: Int = 1260,
  val personalGoalNote: String = "",
  val updatedAtMillis: Long = System.currentTimeMillis()
)

@Entity(tableName = "subjects")
data class SubjectEntity(
  @PrimaryKey val id: String = UUID.randomUUID().toString(),
  val userId: String = "",
  val name: String = "",
  val colorHex: Long = 0xFF2563EB,
  val priority: String = "medium",
  val chapters: List<String> = emptyList(),
  val completedChapters: List<String> = emptyList(),
  val examDateMillis: Long = 0L,
  val progressPercent: Int = 0,
  val createdAtMillis: Long = System.currentTimeMillis(),
  val updatedAtMillis: Long = System.currentTimeMillis()
)

@Entity(tableName = "routines")
data class RoutineEntity(
  @PrimaryKey val id: String = UUID.randomUUID().toString(),
  val userId: String = "",
  val subjectId: String = "",
  val subjectName: String = "",
  val colorHex: Long = 0xFF2563EB,
  val dateMillis: Long = System.currentTimeMillis(),
  val startTimeMinutes: Int = 480,
  val endTimeMinutes: Int = 540,
  val durationMinutes: Int = 60,
  val breakMinutes: Int = 10,
  val repeatMode: String = "daily",
  val repeatDays: List<Int> = listOf(1, 2, 3, 4, 5, 6, 7),
  val notes: String = "",
  val sortOrder: Int = 0,
  val isCompleted: Boolean = false,
  val createdAtMillis: Long = System.currentTimeMillis(),
  val updatedAtMillis: Long = System.currentTimeMillis()
)

@Entity(tableName = "study_tasks")
data class StudyTaskEntity(
  @PrimaryKey val id: String = UUID.randomUUID().toString(),
  val userId: String = "",
  val subjectId: String = "",
  val subjectName: String = "",
  val chapterName: String = "",
  val title: String = "",
  val taskType: String = "homework",
  val deadlineMillis: Long = System.currentTimeMillis(),
  val isCompleted: Boolean = false,
  val priority: String = "medium",
  val createdAtMillis: Long = System.currentTimeMillis(),
  val updatedAtMillis: Long = System.currentTimeMillis()
)

@Entity(tableName = "study_notes")
data class StudyNoteEntity(
  @PrimaryKey val id: String = UUID.randomUUID().toString(),
  val userId: String = "",
  val subjectId: String = "",
  val subjectName: String = "",
  val title: String = "",
  val content: String = "",
  val isPinned: Boolean = false,
  val createdAtMillis: Long = System.currentTimeMillis(),
  val updatedAtMillis: Long = System.currentTimeMillis()
)

@Entity(tableName = "exam_schedules")
data class ExamEntity(
  @PrimaryKey val id: String = UUID.randomUUID().toString(),
  val userId: String = "",
  val subjectId: String = "",
  val subjectName: String = "",
  val title: String = "",
  val examType: String = "board",
  val examDateMillis: Long = System.currentTimeMillis(),
  val syllabusNotes: String = "",
  val preparationPercent: Int = 0,
  val mockScorePercent: Int = 0,
  val createdAtMillis: Long = System.currentTimeMillis(),
  val updatedAtMillis: Long = System.currentTimeMillis()
)

@Entity(tableName = "focus_sessions")
data class FocusSessionEntity(
  @PrimaryKey val id: String = UUID.randomUUID().toString(),
  val userId: String = "",
  val subjectId: String = "",
  val subjectName: String = "",
  val durationMinutes: Int = 25,
  val mode: String = "pomodoro",
  val dateKey: String = "",
  val completedAtMillis: Long = System.currentTimeMillis(),
  val createdAtMillis: Long = System.currentTimeMillis(),
  val updatedAtMillis: Long = System.currentTimeMillis()
)

package com.example.data.remote

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@IgnoreExtraProperties
data class UserProfileDoc(
  @DocumentId val id: String = "",
  val userId: String = "",
  val displayName: String = "",
  val email: String = "",
  val educationLevel: String = "school",
  val categoryDetail: String = "",
  val photoUri: String = "",
  val dailyGoalMinutes: Long = 180L,
  val weeklyGoalMinutes: Long = 1260L,
  val personalGoalNote: String = "",
  val language: String = "bn",
  val themeMode: String = "system",
  val accentTheme: String = "indigo",
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class SubjectDoc(
  @DocumentId val id: String = "",
  val userId: String = "",
  val name: String = "",
  val colorHex: Long = 0xFF2563EBL,
  val priority: String = "medium",
  val chapters: List<String> = emptyList(),
  val completedChapters: List<String> = emptyList(),
  val examDateMillis: Long = 0L,
  val progressPercent: Long = 0L,
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class RoutineDoc(
  @DocumentId val id: String = "",
  val userId: String = "",
  val subjectId: String = "",
  val subjectName: String = "",
  val colorHex: Long = 0xFF2563EBL,
  val dateMillis: Long = 0L,
  val startTimeMinutes: Long = 480L,
  val endTimeMinutes: Long = 540L,
  val durationMinutes: Long = 60L,
  val breakMinutes: Long = 10L,
  val repeatMode: String = "daily",
  val repeatDays: List<Long> = listOf(1L, 2L, 3L, 4L, 5L, 6L, 7L),
  val notes: String = "",
  val sortOrder: Long = 0L,
  val isCompleted: Boolean = false,
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class StudyTaskDoc(
  @DocumentId val id: String = "",
  val userId: String = "",
  val subjectId: String = "",
  val subjectName: String = "",
  val chapterName: String = "",
  val title: String = "",
  val taskType: String = "homework",
  val deadlineMillis: Long = 0L,
  val isCompleted: Boolean = false,
  val priority: String = "medium",
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class StudyNoteDoc(
  @DocumentId val id: String = "",
  val userId: String = "",
  val subjectId: String = "",
  val subjectName: String = "",
  val title: String = "",
  val content: String = "",
  val isPinned: Boolean = false,
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class ExamDoc(
  @DocumentId val id: String = "",
  val userId: String = "",
  val subjectId: String = "",
  val subjectName: String = "",
  val title: String = "",
  val examType: String = "board",
  val examDateMillis: Long = 0L,
  val syllabusNotes: String = "",
  val preparationPercent: Long = 0L,
  val mockScorePercent: Long = 0L,
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class FocusSessionDoc(
  @DocumentId val id: String = "",
  val userId: String = "",
  val subjectId: String = "",
  val subjectName: String = "",
  val durationMinutes: Long = 25L,
  val mode: String = "pomodoro",
  val dateKey: String = "",
  val completedAtMillis: Long = 0L,
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null
)

package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface StudyDao {
  // User Profile
  @Query("SELECT * FROM user_profiles LIMIT 1")
  fun observeProfile(): Flow<UserProfileEntity?>

  @Query("SELECT * FROM user_profiles LIMIT 1")
  suspend fun getProfileOnce(): UserProfileEntity?

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsertProfile(profile: UserProfileEntity)

  @Query("DELETE FROM user_profiles")
  suspend fun clearProfile()

  // Subjects
  @Query("SELECT * FROM subjects ORDER BY createdAtMillis DESC")
  fun observeSubjects(): Flow<List<SubjectEntity>>

  @Query("SELECT * FROM subjects")
  suspend fun getSubjectsOnce(): List<SubjectEntity>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsertSubject(subject: SubjectEntity)

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsertSubjects(subjects: List<SubjectEntity>)

  @Query("DELETE FROM subjects WHERE id = :id")
  suspend fun deleteSubjectById(id: String)

  @Query("DELETE FROM subjects")
  suspend fun clearSubjects()

  // Routines
  @Query("SELECT * FROM routines ORDER BY sortOrder ASC, startTimeMinutes ASC")
  fun observeRoutines(): Flow<List<RoutineEntity>>

  @Query("SELECT * FROM routines ORDER BY sortOrder ASC, startTimeMinutes ASC")
  suspend fun getRoutinesOnce(): List<RoutineEntity>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsertRoutine(routine: RoutineEntity)

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsertRoutines(routines: List<RoutineEntity>)

  @Query("DELETE FROM routines WHERE id = :id")
  suspend fun deleteRoutineById(id: String)

  @Query("DELETE FROM routines")
  suspend fun clearRoutines()

  // Study Tasks
  @Query("SELECT * FROM study_tasks ORDER BY isCompleted ASC, deadlineMillis ASC")
  fun observeTasks(): Flow<List<StudyTaskEntity>>

  @Query("SELECT * FROM study_tasks")
  suspend fun getTasksOnce(): List<StudyTaskEntity>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsertTask(task: StudyTaskEntity)

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsertTasks(tasks: List<StudyTaskEntity>)

  @Query("DELETE FROM study_tasks WHERE id = :id")
  suspend fun deleteTaskById(id: String)

  @Query("DELETE FROM study_tasks")
  suspend fun clearTasks()

  // Study Notes
  @Query("SELECT * FROM study_notes ORDER BY isPinned DESC, updatedAtMillis DESC")
  fun observeNotes(): Flow<List<StudyNoteEntity>>

  @Query("SELECT * FROM study_notes")
  suspend fun getNotesOnce(): List<StudyNoteEntity>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsertNote(note: StudyNoteEntity)

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsertNotes(notes: List<StudyNoteEntity>)

  @Query("DELETE FROM study_notes WHERE id = :id")
  suspend fun deleteNoteById(id: String)

  @Query("DELETE FROM study_notes")
  suspend fun clearNotes()

  // Exams
  @Query("SELECT * FROM exam_schedules ORDER BY examDateMillis ASC")
  fun observeExams(): Flow<List<ExamEntity>>

  @Query("SELECT * FROM exam_schedules")
  suspend fun getExamsOnce(): List<ExamEntity>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsertExam(exam: ExamEntity)

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsertExams(exams: List<ExamEntity>)

  @Query("DELETE FROM exam_schedules WHERE id = :id")
  suspend fun deleteExamById(id: String)

  @Query("DELETE FROM exam_schedules")
  suspend fun clearExams()

  // Focus Sessions
  @Query("SELECT * FROM focus_sessions ORDER BY completedAtMillis DESC")
  fun observeFocusSessions(): Flow<List<FocusSessionEntity>>

  @Query("SELECT * FROM focus_sessions")
  suspend fun getFocusSessionsOnce(): List<FocusSessionEntity>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsertFocusSession(session: FocusSessionEntity)

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsertFocusSessions(sessions: List<FocusSessionEntity>)

  @Query("DELETE FROM focus_sessions")
  suspend fun clearFocusSessions()
}

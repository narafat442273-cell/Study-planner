package com.example.data.remote

import android.content.Context
import com.example.R
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class StudyFirestoreRepository(
  private val db: FirebaseFirestore,
  private val auth: FirebaseAuth = Firebase.auth
) {
  constructor(context: Context) : this(
    FirebaseFirestore.getInstance(
      context.applicationContext.getString(R.string.firestore_database_id)
    ),
    Firebase.auth
  )

  private fun requireUserId(): String =
    auth.currentUser?.uid
      ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")

  private fun userDoc(uid: String) = db.collection("users").document(uid)

  // --- Profile ---
  suspend fun upsertProfile(profile: UserProfileDoc): Result<Unit> = runCatching {
    val uid = requireUserId()
    val ref = userDoc(uid)
    val safeName = profile.displayName.trim().ifEmpty { "Student" }.take(80)
    try {
      val exists = ref.get().await().exists()
      if (exists) {
        val updateMap = mapOf(
          "displayName" to safeName,
          "email" to profile.email.take(120),
          "educationLevel" to profile.educationLevel.ifBlank { "school" }.take(40),
          "categoryDetail" to profile.categoryDetail.take(100),
          "photoUri" to profile.photoUri.take(500),
          "dailyGoalMinutes" to profile.dailyGoalMinutes.coerceIn(10L, 960L),
          "weeklyGoalMinutes" to profile.weeklyGoalMinutes.coerceIn(60L, 6000L),
          "personalGoalNote" to profile.personalGoalNote.take(300),
          "language" to if (profile.language == "en") "en" else "bn",
          "themeMode" to profile.themeMode.takeIf { it in listOf("system", "light", "dark") }.orEmpty().ifEmpty { "system" },
          "accentTheme" to profile.accentTheme.takeIf { it in listOf("indigo", "emerald", "amber", "rose") }.orEmpty().ifEmpty { "indigo" },
          "updatedAt" to FieldValue.serverTimestamp()
        )
        ref.update(updateMap).await()
      } else {
        val createMap = mapOf(
          "userId" to uid,
          "displayName" to safeName,
          "email" to profile.email.take(120),
          "educationLevel" to profile.educationLevel.ifBlank { "school" }.take(40),
          "categoryDetail" to profile.categoryDetail.take(100),
          "photoUri" to profile.photoUri.take(500),
          "dailyGoalMinutes" to profile.dailyGoalMinutes.coerceIn(10L, 960L),
          "weeklyGoalMinutes" to profile.weeklyGoalMinutes.coerceIn(60L, 6000L),
          "personalGoalNote" to profile.personalGoalNote.take(300),
          "language" to if (profile.language == "en") "en" else "bn",
          "themeMode" to profile.themeMode.takeIf { it in listOf("system", "light", "dark") }.orEmpty().ifEmpty { "system" },
          "accentTheme" to profile.accentTheme.takeIf { it in listOf("indigo", "emerald", "amber", "rose") }.orEmpty().ifEmpty { "indigo" },
          "createdAt" to FieldValue.serverTimestamp(),
          "updatedAt" to FieldValue.serverTimestamp()
        )
        ref.set(createMap).await()
      }
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.WRITE, ref.path, auth)
      throw e
    }
  }

  suspend fun getProfile(): Result<UserProfileDoc?> = runCatching {
    val uid = requireUserId()
    val ref = userDoc(uid)
    try {
      ref.get().await().toObject(UserProfileDoc::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.GET, ref.path, auth)
      throw e
    }
  }

  // --- Subjects ---
  fun observeSubjects(): Flow<List<SubjectDoc>> = observeCollection("subjects", SubjectDoc::class.java)

  suspend fun getSubjects(): Result<List<SubjectDoc>> = getCollection("subjects", SubjectDoc::class.java)

  suspend fun upsertSubject(subject: SubjectDoc): Result<String> = runCatching {
    val uid = requireUserId()
    val docId = subject.id.ifBlank { userDoc(uid).collection("subjects").document().id }
    val ref = userDoc(uid).collection("subjects").document(docId)
    val safeChapters = subject.chapters.take(50)
    val safeCompleted = subject.completedChapters.take(50)
    val commonFields = mapOf(
      "name" to subject.name.trim().ifEmpty { "Subject" }.take(100),
      "colorHex" to subject.colorHex,
      "priority" to subject.priority.takeIf { it in listOf("high", "medium", "low") }.orEmpty().ifEmpty { "medium" },
      "chapters" to safeChapters,
      "completedChapters" to safeCompleted,
      "examDateMillis" to subject.examDateMillis.coerceAtLeast(0L),
      "progressPercent" to subject.progressPercent.coerceIn(0L, 100L),
      "updatedAt" to FieldValue.serverTimestamp()
    )
    upsertMapWithTimestamp(ref, uid, commonFields)
    docId
  }

  suspend fun deleteSubject(subjectId: String): Result<Unit> = deleteDoc("subjects", subjectId)

  // --- Routines ---
  fun observeRoutines(): Flow<List<RoutineDoc>> = observeCollection("routines", RoutineDoc::class.java)

  suspend fun getRoutines(): Result<List<RoutineDoc>> = getCollection("routines", RoutineDoc::class.java)

  suspend fun upsertRoutine(routine: RoutineDoc): Result<String> = runCatching {
    val uid = requireUserId()
    val docId = routine.id.ifBlank { userDoc(uid).collection("routines").document().id }
    val ref = userDoc(uid).collection("routines").document(docId)
    val commonFields = mapOf(
      "subjectId" to routine.subjectId.take(128),
      "subjectName" to routine.subjectName.trim().ifEmpty { "Study" }.take(100),
      "colorHex" to routine.colorHex,
      "dateMillis" to routine.dateMillis.coerceAtLeast(0L),
      "startTimeMinutes" to routine.startTimeMinutes.coerceIn(0L, 1439L),
      "endTimeMinutes" to routine.endTimeMinutes.coerceIn(1L, 1440L),
      "durationMinutes" to routine.durationMinutes.coerceIn(5L, 720L),
      "breakMinutes" to routine.breakMinutes.coerceIn(0L, 180L),
      "repeatMode" to routine.repeatMode.takeIf { it in listOf("once", "daily", "weekly", "custom") }.orEmpty().ifEmpty { "daily" },
      "repeatDays" to routine.repeatDays.take(7),
      "notes" to routine.notes.take(500),
      "sortOrder" to routine.sortOrder.coerceIn(0L, 10000L),
      "isCompleted" to routine.isCompleted,
      "updatedAt" to FieldValue.serverTimestamp()
    )
    upsertMapWithTimestamp(ref, uid, commonFields)
    docId
  }

  suspend fun deleteRoutine(routineId: String): Result<Unit> = deleteDoc("routines", routineId)

  // --- Tasks ---
  fun observeTasks(): Flow<List<StudyTaskDoc>> = observeCollection("tasks", StudyTaskDoc::class.java)

  suspend fun getTasks(): Result<List<StudyTaskDoc>> = getCollection("tasks", StudyTaskDoc::class.java)

  suspend fun upsertTask(task: StudyTaskDoc): Result<String> = runCatching {
    val uid = requireUserId()
    val docId = task.id.ifBlank { userDoc(uid).collection("tasks").document().id }
    val ref = userDoc(uid).collection("tasks").document(docId)
    val commonFields = mapOf(
      "subjectId" to task.subjectId.take(128),
      "subjectName" to task.subjectName.take(100),
      "chapterName" to task.chapterName.take(120),
      "title" to task.title.trim().ifEmpty { "Task" }.take(150),
      "taskType" to task.taskType.takeIf { it in listOf("homework", "assignment", "chapter_task", "revision") }.orEmpty().ifEmpty { "homework" },
      "deadlineMillis" to task.deadlineMillis.coerceAtLeast(0L),
      "isCompleted" to task.isCompleted,
      "priority" to task.priority.takeIf { it in listOf("high", "medium", "low") }.orEmpty().ifEmpty { "medium" },
      "updatedAt" to FieldValue.serverTimestamp()
    )
    upsertMapWithTimestamp(ref, uid, commonFields)
    docId
  }

  suspend fun deleteTask(taskId: String): Result<Unit> = deleteDoc("tasks", taskId)

  // --- Notes ---
  fun observeNotes(): Flow<List<StudyNoteDoc>> = observeCollection("notes", StudyNoteDoc::class.java)

  suspend fun getNotes(): Result<List<StudyNoteDoc>> = getCollection("notes", StudyNoteDoc::class.java)

  suspend fun upsertNote(note: StudyNoteDoc): Result<String> = runCatching {
    val uid = requireUserId()
    val docId = note.id.ifBlank { userDoc(uid).collection("notes").document().id }
    val ref = userDoc(uid).collection("notes").document(docId)
    val commonFields = mapOf(
      "subjectId" to note.subjectId.take(128),
      "subjectName" to note.subjectName.take(100),
      "title" to note.title.trim().ifEmpty { "Note" }.take(150),
      "content" to note.content.take(10000),
      "isPinned" to note.isPinned,
      "updatedAt" to FieldValue.serverTimestamp()
    )
    upsertMapWithTimestamp(ref, uid, commonFields)
    docId
  }

  suspend fun deleteNote(noteId: String): Result<Unit> = deleteDoc("notes", noteId)

  // --- Exams ---
  fun observeExams(): Flow<List<ExamDoc>> = observeCollection("exams", ExamDoc::class.java)

  suspend fun getExams(): Result<List<ExamDoc>> = getCollection("exams", ExamDoc::class.java)

  suspend fun upsertExam(exam: ExamDoc): Result<String> = runCatching {
    val uid = requireUserId()
    val docId = exam.id.ifBlank { userDoc(uid).collection("exams").document().id }
    val ref = userDoc(uid).collection("exams").document(docId)
    val commonFields = mapOf(
      "subjectId" to exam.subjectId.take(128),
      "subjectName" to exam.subjectName.take(100),
      "title" to exam.title.trim().ifEmpty { "Exam" }.take(150),
      "examType" to exam.examType.takeIf { it in listOf("board", "semester", "mock_test", "class_test", "admission") }.orEmpty().ifEmpty { "board" },
      "examDateMillis" to exam.examDateMillis.coerceAtLeast(0L),
      "syllabusNotes" to exam.syllabusNotes.take(1000),
      "preparationPercent" to exam.preparationPercent.coerceIn(0L, 100L),
      "mockScorePercent" to exam.mockScorePercent.coerceIn(0L, 100L),
      "updatedAt" to FieldValue.serverTimestamp()
    )
    upsertMapWithTimestamp(ref, uid, commonFields)
    docId
  }

  suspend fun deleteExam(examId: String): Result<Unit> = deleteDoc("exams", examId)

  // --- Focus Sessions ---
  fun observeFocusSessions(): Flow<List<FocusSessionDoc>> = observeCollection("sessions", FocusSessionDoc::class.java)

  suspend fun getFocusSessions(): Result<List<FocusSessionDoc>> = getCollection("sessions", FocusSessionDoc::class.java)

  suspend fun upsertFocusSession(session: FocusSessionDoc): Result<String> = runCatching {
    val uid = requireUserId()
    val docId = session.id.ifBlank { userDoc(uid).collection("sessions").document().id }
    val ref = userDoc(uid).collection("sessions").document(docId)
    val commonFields = mapOf(
      "subjectId" to session.subjectId.take(128),
      "subjectName" to session.subjectName.take(100),
      "durationMinutes" to session.durationMinutes.coerceIn(1L, 600L),
      "mode" to session.mode.takeIf { it in listOf("pomodoro", "short_break", "long_break", "custom") }.orEmpty().ifEmpty { "pomodoro" },
      "dateKey" to session.dateKey.ifBlank { "1970-01-01" }.take(20),
      "completedAtMillis" to session.completedAtMillis.coerceAtLeast(0L),
      "updatedAt" to FieldValue.serverTimestamp()
    )
    upsertMapWithTimestamp(ref, uid, commonFields)
    docId
  }

  // --- Helpers ---
  private suspend fun upsertMapWithTimestamp(
    ref: com.google.firebase.firestore.DocumentReference,
    uid: String,
    updateFields: Map<String, Any>
  ) {
    try {
      val exists = ref.get().await().exists()
      if (exists) {
        ref.update(updateFields).await()
      } else {
        val createMap = updateFields.toMutableMap().apply {
          put("userId", uid)
          put("createdAt", FieldValue.serverTimestamp())
        }
        ref.set(createMap).await()
      }
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.WRITE, ref.path, auth)
      throw e
    }
  }

  private fun <T : Any> observeCollection(subcollection: String, clazz: Class<T>): Flow<List<T>> = flow {
    val uid = requireUserId()
    val colRef = userDoc(uid).collection(subcollection)
    emitAll(
      colRef.snapshots()
        .map { snapshot ->
          snapshot.toObjects(clazz, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
        }
        .catch { error ->
          val firestoreError = generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
          if (firestoreError == null && error is CancellationException) throw error
          val exceptionToLog = firestoreError ?: (error as? Exception)
          if (exceptionToLog != null && auth.currentUser?.uid == uid) {
            handleFirestoreError(exceptionToLog, OperationType.LIST, colRef.path, auth)
          }
          throw error
        }
    )
  }

  private suspend fun <T : Any> getCollection(subcollection: String, clazz: Class<T>): Result<List<T>> = runCatching {
    val uid = requireUserId()
    val colRef = userDoc(uid).collection(subcollection)
    try {
      colRef.get().await().toObjects(clazz, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.LIST, colRef.path, auth)
      throw e
    }
  }

  private suspend fun deleteDoc(subcollection: String, docId: String): Result<Unit> = runCatching {
    val uid = requireUserId()
    val ref = userDoc(uid).collection(subcollection).document(docId)
    try {
      ref.delete().await()
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.DELETE, ref.path, auth)
      throw e
    }
  }
}

package com.example.data.repository

import com.example.data.local.ExamEntity
import com.example.data.local.FocusSessionEntity
import com.example.data.local.PreferencesManager
import com.example.data.local.RoutineEntity
import com.example.data.local.StudyDao
import com.example.data.local.StudyNoteEntity
import com.example.data.local.StudyTaskEntity
import com.example.data.local.SubjectEntity
import com.example.data.local.UserProfileEntity
import com.example.data.remote.ExamDoc
import com.example.data.remote.FocusSessionDoc
import com.example.data.remote.RoutineDoc
import com.example.data.remote.StudyFirestoreRepository
import com.example.data.remote.StudyNoteDoc
import com.example.data.remote.StudyTaskDoc
import com.example.data.remote.SubjectDoc
import com.example.data.remote.UserProfileDoc
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import org.json.JSONArray
import org.json.JSONObject
import java.util.Calendar
import java.util.UUID

data class RoutineTemplate(
  val id: String,
  val titleBn: String,
  val titleEn: String,
  val subtitleBn: String,
  val subtitleEn: String,
  val level: String,
  val items: List<TemplateSlot>
)

data class TemplateSlot(
  val subjectBn: String,
  val subjectEn: String,
  val colorHex: Long,
  val startMinutes: Int,
  val durationMinutes: Int,
  val breakMinutes: Int,
  val notesBn: String,
  val notesEn: String
)

class StudyPlannerRepository(
  private val dao: StudyDao,
  private val firestoreRepo: StudyFirestoreRepository,
  private val prefs: PreferencesManager,
  private val auth: FirebaseAuth
) {
  val profileFlow: Flow<UserProfileEntity?> = dao.observeProfile()
  val subjectsFlow: Flow<List<SubjectEntity>> = dao.observeSubjects()
  val routinesFlow: Flow<List<RoutineEntity>> = dao.observeRoutines()
  val tasksFlow: Flow<List<StudyTaskEntity>> = dao.observeTasks()
  val notesFlow: Flow<List<StudyNoteEntity>> = dao.observeNotes()
  val examsFlow: Flow<List<ExamEntity>> = dao.observeExams()
  val focusSessionsFlow: Flow<List<FocusSessionEntity>> = dao.observeFocusSessions()

  private fun activeUid(): String? = auth.currentUser?.uid

  suspend fun saveProfile(profile: UserProfileEntity) {
    val uid = activeUid().orEmpty()
    val entity = profile.copy(
      id = "local_user",
      userId = uid,
      updatedAtMillis = System.currentTimeMillis()
    )
    dao.upsertProfile(entity)
    if (uid.isNotBlank()) {
      val appPrefs = prefs.preferencesFlow.first()
      firestoreRepo.upsertProfile(
        UserProfileDoc(
          userId = uid,
          displayName = entity.displayName,
          email = entity.email,
          educationLevel = entity.educationLevel,
          categoryDetail = entity.categoryDetail,
          photoUri = entity.photoUri,
          dailyGoalMinutes = entity.dailyGoalMinutes.toLong(),
          weeklyGoalMinutes = entity.weeklyGoalMinutes.toLong(),
          personalGoalNote = entity.personalGoalNote,
          language = appPrefs.language,
          themeMode = appPrefs.themeMode,
          accentTheme = appPrefs.accentTheme
        )
      )
    }
  }

  suspend fun saveSubject(subject: SubjectEntity) {
    val uid = activeUid().orEmpty()
    val chapters = subject.chapters.take(50)
    val completed = subject.completedChapters.filter { it in chapters }.take(50)
    val computedProgress = if (chapters.isNotEmpty()) {
      ((completed.size.toFloat() / chapters.size.toFloat()) * 100f).toInt().coerceIn(0, 100)
    } else {
      subject.progressPercent.coerceIn(0, 100)
    }
    val entity = subject.copy(
      id = subject.id.ifBlank { UUID.randomUUID().toString() },
      userId = uid,
      chapters = chapters,
      completedChapters = completed,
      progressPercent = computedProgress,
      updatedAtMillis = System.currentTimeMillis()
    )
    dao.upsertSubject(entity)
    if (uid.isNotBlank()) {
      firestoreRepo.upsertSubject(
        SubjectDoc(
          id = entity.id,
          userId = uid,
          name = entity.name,
          colorHex = entity.colorHex,
          priority = entity.priority,
          chapters = entity.chapters,
          completedChapters = entity.completedChapters,
          examDateMillis = entity.examDateMillis,
          progressPercent = entity.progressPercent.toLong()
        )
      )
    }
  }

  suspend fun deleteSubject(id: String) {
    dao.deleteSubjectById(id)
    if (activeUid() != null) {
      firestoreRepo.deleteSubject(id)
    }
  }

  suspend fun saveRoutine(routine: RoutineEntity) {
    val uid = activeUid().orEmpty()
    val entity = routine.copy(
      id = routine.id.ifBlank { UUID.randomUUID().toString() },
      userId = uid,
      updatedAtMillis = System.currentTimeMillis()
    )
    dao.upsertRoutine(entity)
    if (uid.isNotBlank()) {
      firestoreRepo.upsertRoutine(entity.toDoc(uid))
    }
  }

  suspend fun reorderRoutines(reordered: List<RoutineEntity>) {
    val uid = activeUid().orEmpty()
    val updated = reordered.mapIndexed { index, item ->
      item.copy(sortOrder = index, userId = uid, updatedAtMillis = System.currentTimeMillis())
    }
    dao.upsertRoutines(updated)
    if (uid.isNotBlank()) {
      updated.forEach { firestoreRepo.upsertRoutine(it.toDoc(uid)) }
    }
  }

  suspend fun deleteRoutine(id: String) {
    dao.deleteRoutineById(id)
    if (activeUid() != null) {
      firestoreRepo.deleteRoutine(id)
    }
  }

  suspend fun saveTask(task: StudyTaskEntity) {
    val uid = activeUid().orEmpty()
    val entity = task.copy(
      id = task.id.ifBlank { UUID.randomUUID().toString() },
      userId = uid,
      updatedAtMillis = System.currentTimeMillis()
    )
    dao.upsertTask(entity)
    if (uid.isNotBlank()) {
      firestoreRepo.upsertTask(
        StudyTaskDoc(
          id = entity.id,
          userId = uid,
          subjectId = entity.subjectId,
          subjectName = entity.subjectName,
          chapterName = entity.chapterName,
          title = entity.title,
          taskType = entity.taskType,
          deadlineMillis = entity.deadlineMillis,
          isCompleted = entity.isCompleted,
          priority = entity.priority
        )
      )
    }
  }

  suspend fun deleteTask(id: String) {
    dao.deleteTaskById(id)
    if (activeUid() != null) {
      firestoreRepo.deleteTask(id)
    }
  }

  suspend fun saveNote(note: StudyNoteEntity) {
    val uid = activeUid().orEmpty()
    val entity = note.copy(
      id = note.id.ifBlank { UUID.randomUUID().toString() },
      userId = uid,
      updatedAtMillis = System.currentTimeMillis()
    )
    dao.upsertNote(entity)
    if (uid.isNotBlank()) {
      firestoreRepo.upsertNote(
        StudyNoteDoc(
          id = entity.id,
          userId = uid,
          subjectId = entity.subjectId,
          subjectName = entity.subjectName,
          title = entity.title,
          content = entity.content,
          isPinned = entity.isPinned
        )
      )
    }
  }

  suspend fun deleteNote(id: String) {
    dao.deleteNoteById(id)
    if (activeUid() != null) {
      firestoreRepo.deleteNote(id)
    }
  }

  suspend fun saveExam(exam: ExamEntity) {
    val uid = activeUid().orEmpty()
    val entity = exam.copy(
      id = exam.id.ifBlank { UUID.randomUUID().toString() },
      userId = uid,
      updatedAtMillis = System.currentTimeMillis()
    )
    dao.upsertExam(entity)
    if (uid.isNotBlank()) {
      firestoreRepo.upsertExam(
        ExamDoc(
          id = entity.id,
          userId = uid,
          subjectId = entity.subjectId,
          subjectName = entity.subjectName,
          title = entity.title,
          examType = entity.examType,
          examDateMillis = entity.examDateMillis,
          syllabusNotes = entity.syllabusNotes,
          preparationPercent = entity.preparationPercent.toLong(),
          mockScorePercent = entity.mockScorePercent.toLong()
        )
      )
    }
  }

  suspend fun deleteExam(id: String) {
    dao.deleteExamById(id)
    if (activeUid() != null) {
      firestoreRepo.deleteExam(id)
    }
  }

  suspend fun recordFocusSession(session: FocusSessionEntity) {
    val uid = activeUid().orEmpty()
    val entity = session.copy(
      id = session.id.ifBlank { UUID.randomUUID().toString() },
      userId = uid,
      updatedAtMillis = System.currentTimeMillis()
    )
    dao.upsertFocusSession(entity)
    if (uid.isNotBlank()) {
      firestoreRepo.upsertFocusSession(
        FocusSessionDoc(
          id = entity.id,
          userId = uid,
          subjectId = entity.subjectId,
          subjectName = entity.subjectName,
          durationMinutes = entity.durationMinutes.toLong(),
          mode = entity.mode,
          dateKey = entity.dateKey,
          completedAtMillis = entity.completedAtMillis
        )
      )
    }
  }

  suspend fun applyRoutineTemplate(template: RoutineTemplate, isBengali: Boolean) {
    val existingSubjects = dao.getSubjectsOnce()
    val existingRoutines = dao.getRoutinesOnce()
    var nextOrder = (existingRoutines.maxOfOrNull { it.sortOrder } ?: -1) + 1
    val todayMillis = startOfDayMillis(System.currentTimeMillis())

    for (slot in template.items) {
      val subjName = if (isBengali) slot.subjectBn else slot.subjectEn
      val matchedSubject = existingSubjects.firstOrNull { it.name.equals(subjName, ignoreCase = true) }
        ?: SubjectEntity(
          name = subjName,
          colorHex = slot.colorHex,
          priority = "high",
          chapters = if (isBengali) listOf("অধ্যায় ১", "অধ্যায় ২", "অনুশীলন") else listOf("Chapter 1", "Chapter 2", "Practice")
        ).also { saveSubject(it) }

      saveRoutine(
        RoutineEntity(
          subjectId = matchedSubject.id,
          subjectName = matchedSubject.name,
          colorHex = matchedSubject.colorHex,
          dateMillis = todayMillis,
          startTimeMinutes = slot.startMinutes,
          endTimeMinutes = slot.startMinutes + slot.durationMinutes,
          durationMinutes = slot.durationMinutes,
          breakMinutes = slot.breakMinutes,
          repeatMode = "daily",
          repeatDays = listOf(1, 2, 3, 4, 5, 6, 7),
          notes = if (isBengali) slot.notesBn else slot.notesEn,
          sortOrder = nextOrder++
        )
      )
    }
  }

  suspend fun backupAllToCloud(): Result<Int> = runCatching {
    val uid = activeUid() ?: throw IllegalStateException("Google Sign-In required for cloud backup")
    var count = 0
    dao.getProfileOnce()?.let { profile ->
      saveProfile(profile.copy(userId = uid))
      count++
    }
    dao.getSubjectsOnce().forEach {
      firestoreRepo.upsertSubject(
        SubjectDoc(
          id = it.id,
          userId = uid,
          name = it.name,
          colorHex = it.colorHex,
          priority = it.priority,
          chapters = it.chapters,
          completedChapters = it.completedChapters,
          examDateMillis = it.examDateMillis,
          progressPercent = it.progressPercent.toLong()
        )
      ).getOrThrow()
      count++
    }
    dao.getRoutinesOnce().forEach {
      firestoreRepo.upsertRoutine(it.toDoc(uid)).getOrThrow()
      count++
    }
    dao.getTasksOnce().forEach {
      firestoreRepo.upsertTask(
        StudyTaskDoc(
          id = it.id,
          userId = uid,
          subjectId = it.subjectId,
          subjectName = it.subjectName,
          chapterName = it.chapterName,
          title = it.title,
          taskType = it.taskType,
          deadlineMillis = it.deadlineMillis,
          isCompleted = it.isCompleted,
          priority = it.priority
        )
      ).getOrThrow()
      count++
    }
    dao.getNotesOnce().forEach {
      firestoreRepo.upsertNote(
        StudyNoteDoc(
          id = it.id,
          userId = uid,
          subjectId = it.subjectId,
          subjectName = it.subjectName,
          title = it.title,
          content = it.content,
          isPinned = it.isPinned
        )
      ).getOrThrow()
      count++
    }
    dao.getExamsOnce().forEach {
      firestoreRepo.upsertExam(
        ExamDoc(
          id = it.id,
          userId = uid,
          subjectId = it.subjectId,
          subjectName = it.subjectName,
          title = it.title,
          examType = it.examType,
          examDateMillis = it.examDateMillis,
          syllabusNotes = it.syllabusNotes,
          preparationPercent = it.preparationPercent.toLong(),
          mockScorePercent = it.mockScorePercent.toLong()
        )
      ).getOrThrow()
      count++
    }
    dao.getFocusSessionsOnce().forEach {
      firestoreRepo.upsertFocusSession(
        FocusSessionDoc(
          id = it.id,
          userId = uid,
          subjectId = it.subjectId,
          subjectName = it.subjectName,
          durationMinutes = it.durationMinutes.toLong(),
          mode = it.mode,
          dateKey = it.dateKey,
          completedAtMillis = it.completedAtMillis
        )
      ).getOrThrow()
      count++
    }
    count
  }

  suspend fun restoreAllFromCloud(): Result<Int> = runCatching {
    val uid = activeUid() ?: throw IllegalStateException("Google Sign-In required for cloud restore")
    var count = 0

    firestoreRepo.getProfile().getOrNull()?.let { doc ->
      dao.upsertProfile(
        UserProfileEntity(
          id = "local_user",
          userId = uid,
          displayName = doc.displayName,
          email = doc.email,
          educationLevel = doc.educationLevel,
          categoryDetail = doc.categoryDetail,
          photoUri = doc.photoUri,
          dailyGoalMinutes = doc.dailyGoalMinutes.toInt(),
          weeklyGoalMinutes = doc.weeklyGoalMinutes.toInt(),
          personalGoalNote = doc.personalGoalNote
        )
      )
      prefs.setLanguage(doc.language)
      prefs.setThemeMode(doc.themeMode)
      prefs.setAccentTheme(doc.accentTheme)
      count++
    }

    val subjects = firestoreRepo.getSubjects().getOrThrow().map { doc ->
      SubjectEntity(
        id = doc.id,
        userId = uid,
        name = doc.name,
        colorHex = doc.colorHex,
        priority = doc.priority,
        chapters = doc.chapters,
        completedChapters = doc.completedChapters,
        examDateMillis = doc.examDateMillis,
        progressPercent = doc.progressPercent.toInt()
      )
    }
    if (subjects.isNotEmpty()) {
      dao.upsertSubjects(subjects)
      count += subjects.size
    }

    val routines = firestoreRepo.getRoutines().getOrThrow().map { doc ->
      RoutineEntity(
        id = doc.id,
        userId = uid,
        subjectId = doc.subjectId,
        subjectName = doc.subjectName,
        colorHex = doc.colorHex,
        dateMillis = doc.dateMillis,
        startTimeMinutes = doc.startTimeMinutes.toInt(),
        endTimeMinutes = doc.endTimeMinutes.toInt(),
        durationMinutes = doc.durationMinutes.toInt(),
        breakMinutes = doc.breakMinutes.toInt(),
        repeatMode = doc.repeatMode,
        repeatDays = doc.repeatDays.map { it.toInt() },
        notes = doc.notes,
        sortOrder = doc.sortOrder.toInt(),
        isCompleted = doc.isCompleted
      )
    }
    if (routines.isNotEmpty()) {
      dao.upsertRoutines(routines)
      count += routines.size
    }

    val tasks = firestoreRepo.getTasks().getOrThrow().map { doc ->
      StudyTaskEntity(
        id = doc.id,
        userId = uid,
        subjectId = doc.subjectId,
        subjectName = doc.subjectName,
        chapterName = doc.chapterName,
        title = doc.title,
        taskType = doc.taskType,
        deadlineMillis = doc.deadlineMillis,
        isCompleted = doc.isCompleted,
        priority = doc.priority
      )
    }
    if (tasks.isNotEmpty()) {
      dao.upsertTasks(tasks)
      count += tasks.size
    }

    val notes = firestoreRepo.getNotes().getOrThrow().map { doc ->
      StudyNoteEntity(
        id = doc.id,
        userId = uid,
        subjectId = doc.subjectId,
        subjectName = doc.subjectName,
        title = doc.title,
        content = doc.content,
        isPinned = doc.isPinned
      )
    }
    if (notes.isNotEmpty()) {
      dao.upsertNotes(notes)
      count += notes.size
    }

    val exams = firestoreRepo.getExams().getOrThrow().map { doc ->
      ExamEntity(
        id = doc.id,
        userId = uid,
        subjectId = doc.subjectId,
        subjectName = doc.subjectName,
        title = doc.title,
        examType = doc.examType,
        examDateMillis = doc.examDateMillis,
        syllabusNotes = doc.syllabusNotes,
        preparationPercent = doc.preparationPercent.toInt(),
        mockScorePercent = doc.mockScorePercent.toInt()
      )
    }
    if (exams.isNotEmpty()) {
      dao.upsertExams(exams)
      count += exams.size
    }

    val sessions = firestoreRepo.getFocusSessions().getOrThrow().map { doc ->
      FocusSessionEntity(
        id = doc.id,
        userId = uid,
        subjectId = doc.subjectId,
        subjectName = doc.subjectName,
        durationMinutes = doc.durationMinutes.toInt(),
        mode = doc.mode,
        dateKey = doc.dateKey,
        completedAtMillis = doc.completedAtMillis
      )
    }
    if (sessions.isNotEmpty()) {
      dao.upsertFocusSessions(sessions)
      count += sessions.size
    }

    count
  }

  suspend fun exportLocalBackupJson(): String {
    val root = JSONObject()
    val subjectsArr = JSONArray()
    dao.getSubjectsOnce().forEach { s ->
      subjectsArr.put(JSONObject().apply {
        put("id", s.id)
        put("name", s.name)
        put("colorHex", s.colorHex)
        put("priority", s.priority)
        put("chapters", JSONArray(s.chapters))
        put("completedChapters", JSONArray(s.completedChapters))
        put("examDateMillis", s.examDateMillis)
        put("progressPercent", s.progressPercent)
      })
    }
    val routinesArr = JSONArray()
    dao.getRoutinesOnce().forEach { r ->
      routinesArr.put(JSONObject().apply {
        put("id", r.id)
        put("subjectId", r.subjectId)
        put("subjectName", r.subjectName)
        put("colorHex", r.colorHex)
        put("dateMillis", r.dateMillis)
        put("startTimeMinutes", r.startTimeMinutes)
        put("endTimeMinutes", r.endTimeMinutes)
        put("durationMinutes", r.durationMinutes)
        put("breakMinutes", r.breakMinutes)
        put("repeatMode", r.repeatMode)
        put("repeatDays", JSONArray(r.repeatDays))
        put("notes", r.notes)
        put("sortOrder", r.sortOrder)
        put("isCompleted", r.isCompleted)
      })
    }
    root.put("subjects", subjectsArr)
    root.put("routines", routinesArr)
    return root.toString(2)
  }

  suspend fun clearLocalUserOnSignOut() {
    prefs.setGuestMode(false)
  }

  private fun RoutineEntity.toDoc(uid: String) = RoutineDoc(
    id = id,
    userId = uid,
    subjectId = subjectId,
    subjectName = subjectName,
    colorHex = colorHex,
    dateMillis = dateMillis,
    startTimeMinutes = startTimeMinutes.toLong(),
    endTimeMinutes = endTimeMinutes.toLong(),
    durationMinutes = durationMinutes.toLong(),
    breakMinutes = breakMinutes.toLong(),
    repeatMode = repeatMode,
    repeatDays = repeatDays.map { it.toLong() },
    notes = notes,
    sortOrder = sortOrder.toLong(),
    isCompleted = isCompleted
  )

  companion object {
    fun startOfDayMillis(millis: Long): Long {
      val cal = Calendar.getInstance().apply {
        timeInMillis = millis
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
      }
      return cal.timeInMillis
    }

    val BuiltInTemplates: List<RoutineTemplate> = listOf(
      RoutineTemplate(
        id = "board_science",
        titleBn = "স্কুল ও কলেজ বিজ্ঞান রুটিন",
        titleEn = "School & College Science Routine",
        subtitleBn = "গণিত, পদার্থবিজ্ঞান, রসায়ন ও ইংরেজি অনুশীলন",
        subtitleEn = "Math, Physics, Chemistry & English daily blocks",
        level = "college",
        items = listOf(
          TemplateSlot("গণিত", "Mathematics", 0xFF2563EB, 390, 60, 10, "অনুশীলনী ও সূত্র রিভিশন", "Exercises & formula review"),
          TemplateSlot("পদার্থবিজ্ঞান", "Physics", 0xFF0D9488, 1020, 60, 10, "ধারণা ও গাণিতিক সমস্যা", "Concepts & numerical problems"),
          TemplateSlot("রসায়ন", "Chemistry", 0xFFD97706, 1140, 50, 10, "বিক্রিয়া ও নোট অনুশীলন", "Reactions & notes practice"),
          TemplateSlot("ইংরেজি", "English", 0xFF7C3AED, 1230, 45, 5, "গ্রামার ও রাইটিং", "Grammar & writing")
        )
      ),
      RoutineTemplate(
        id = "madrasa_routine",
        titleBn = "মাদরাসা ও সাধারণ বিষয় রুটিন",
        titleEn = "Madrasa & Core Subjects Routine",
        subtitleBn = "কুরআন, হাদিস, আরবি, বাংলা ও গণিত সমন্বয়",
        subtitleEn = "Quran, Hadith, Arabic, Bangla & Math schedule",
        level = "madrasa",
        items = listOf(
          TemplateSlot("আল-কুরআন ও হাদিস", "Al-Quran & Hadith", 0xFF059669, 360, 60, 10, "তাজবিদ, তর্জমা ও তাফসির পাঠ", "Tajwid, translation & tafsir"),
          TemplateSlot("আরবি সাহিত্য ও ব্যাকরণ", "Arabic Literature & Grammar", 0xFF0284C7, 990, 50, 10, "নাহু-সরফ অনুশীলন", "Nahw & Sarf practice"),
          TemplateSlot("বাংলা ও গণিত", "Bangla & Mathematics", 0xFFD97706, 1140, 60, 10, "সৃজনশীল ও অনুশীলনী সমাধান", "Creative & math problem solving")
        )
      ),
      RoutineTemplate(
        id = "job_competitive",
        titleBn = "বিসিএস ও চাকরি প্রস্তুতি রুটিন",
        titleEn = "BCS & Job Preparation Routine",
        subtitleBn = "বাংলা, ইংরেজি, সাধারণ জ্ঞান, গণিত ও মডেল টেস্ট",
        subtitleEn = "Bangla, English, GK, Math & Model Test blocks",
        level = "job_prep",
        items = listOf(
          TemplateSlot("বাংলাদেশ ও আন্তর্জাতিক বিষয়াবলি", "General Knowledge", 0xFF0D9488, 420, 60, 10, "সাম্প্রতিক ও মৌলিক তথ্য রিভিশন", "Current affairs & core GK"),
          TemplateSlot("গাণিতিক যুক্তি ও মানসিক দক্ষতা", "Math & Mental Ability", 0xFF2563EB, 600, 75, 15, "শর্টকাট ও বিগত প্রশ্ন সমাধান", "Shortcuts & past questions"),
          TemplateSlot("বাংলা ও ইংরেজি সাহিত্য", "Bangla & English", 0xFF7C3AED, 1080, 60, 10, "ভোকাবুলারি ও সাহিত্য", "Vocabulary & literature"),
          TemplateSlot("দৈনিক মডেল টেস্ট", "Daily Model Test", 0xFFDC2626, 1260, 45, 5, "১০০ নম্বরের মূল্যায়ন ও বিশ্লেষণ", "Timed MCQ review & analysis")
        )
      ),
      RoutineTemplate(
        id = "university_focus",
        titleBn = "বিশ্ববিদ্যালয় ও অনার্স রুটিন",
        titleEn = "University & Honours Study Plan",
        subtitleBn = "কোর কোর্স, রিসার্চ/অ্যাসাইনমেন্ট ও ল্যাব রিভিউ",
        subtitleEn = "Core courses, assignments & lab/research review",
        level = "university",
        items = listOf(
          TemplateSlot("কোর মেজর কোর্স", "Core Major Course", 0xFF2563EB, 540, 90, 15, "লেকচার স্লাইড ও রেফারেন্স বই", "Lecture slides & reference text"),
          TemplateSlot("অ্যাসাইনমেন্ট ও ল্যাব প্র্যাকটিস", "Assignment & Lab Prep", 0xFF059669, 960, 75, 10, "সমস্যা সমাধান ও রিপোর্ট", "Problem sets & lab reports"),
          TemplateSlot("সেমিস্টার রিভিশন", "Semester Revision", 0xFFD97706, 1200, 60, 10, "বিগত বছরের প্রশ্ন পর্যালোচনা", "Past paper review")
        )
      )
    )
  }
}

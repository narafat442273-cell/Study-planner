package com.example.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.local.AppPreferences
import com.example.data.local.ExamEntity
import com.example.data.local.FocusSessionEntity
import com.example.data.local.PreferencesManager
import com.example.data.local.RoutineEntity
import com.example.data.local.StudyNoteEntity
import com.example.data.local.StudyTaskEntity
import com.example.data.local.SubjectEntity
import com.example.data.local.UserProfileEntity
import com.example.data.remote.StudyFirestoreRepository
import com.example.data.repository.RoutineTemplate
import com.example.data.repository.StudyPlannerRepository
import com.example.worker.NotificationHelper
import com.example.worker.StudyReminderWorker
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

data class TimerState(
  val mode: String = "pomodoro", // pomodoro, short_break, long_break, custom
  val totalSeconds: Int = 25 * 60,
  val remainingSeconds: Int = 25 * 60,
  val isRunning: Boolean = false,
  val customMinutes: Int = 45,
  val selectedSubjectId: String = "",
  val selectedSubjectName: String = ""
)

data class StudyUiState(
  val prefs: AppPreferences = AppPreferences(),
  val profile: UserProfileEntity? = null,
  val subjects: List<SubjectEntity> = emptyList(),
  val routines: List<RoutineEntity> = emptyList(),
  val tasks: List<StudyTaskEntity> = emptyList(),
  val notes: List<StudyNoteEntity> = emptyList(),
  val exams: List<ExamEntity> = emptyList(),
  val focusSessions: List<FocusSessionEntity> = emptyList(),
  val todayFocusMinutes: Int = 0,
  val weeklyMinutesByDay: List<Pair<String, Int>> = emptyList(),
  val monthlyFocusMinutes: Int = 0,
  val studyStreakDays: Int = 0,
  val dailyProgressPercent: Int = 0,
  val statusMessage: String? = null,
  val isSyncing: Boolean = false
)

class StudyViewModel(
  private val repo: StudyPlannerRepository,
  private val prefsManager: PreferencesManager
) : ViewModel() {

  private val _timerState = MutableStateFlow(TimerState())
  val timerState: StateFlow<TimerState> = _timerState.asStateFlow()

  private val _statusMessage = MutableStateFlow<String?>(null)
  private val _isSyncing = MutableStateFlow(false)
  private var timerJob: Job? = null

  private val coreDataFlow = combine(
    prefsManager.preferencesFlow,
    repo.profileFlow,
    repo.subjectsFlow,
    repo.routinesFlow,
    repo.tasksFlow
  ) { prefs, profile, subjects, routines, tasks ->
    CoreTuple(prefs, profile, subjects, routines, tasks)
  }

  private val extraDataFlow = combine(
    repo.notesFlow,
    repo.examsFlow,
    repo.focusSessionsFlow,
    _statusMessage,
    _isSyncing
  ) { notes, exams, sessions, msg, syncing ->
    ExtraTuple(notes, exams, sessions, msg, syncing)
  }

  val uiState: StateFlow<StudyUiState> = combine(coreDataFlow, extraDataFlow) { core, extra ->
    val todayKey = dateKeyFor(System.currentTimeMillis())
    val todayFocus = extra.sessions
      .filter { it.dateKey == todayKey && (it.mode == "pomodoro" || it.mode == "custom") }
      .sumOf { it.durationMinutes }

    val completedRoutinesMinutes = core.routines
      .filter { it.isCompleted }
      .sumOf { it.durationMinutes }

    val totalGoal = (core.profile?.dailyGoalMinutes ?: 180).coerceAtLeast(1)
    val achievedMinutes = todayFocus + completedRoutinesMinutes
    val progressPct = ((achievedMinutes.toFloat() / totalGoal.toFloat()) * 100f).toInt().coerceIn(0, 100)

    val weeklyStats = computeWeeklyStats(extra.sessions, core.prefs.language == "bn")
    val monthlyMins = computeMonthlyMinutes(extra.sessions)
    val streak = computeStreak(extra.sessions, core.routines)

    StudyUiState(
      prefs = core.prefs,
      profile = core.profile,
      subjects = core.subjects,
      routines = core.routines,
      tasks = core.tasks,
      notes = extra.notes,
      exams = extra.exams,
      focusSessions = extra.sessions,
      todayFocusMinutes = todayFocus,
      weeklyMinutesByDay = weeklyStats,
      monthlyFocusMinutes = monthlyMins,
      studyStreakDays = streak,
      dailyProgressPercent = progressPct,
      statusMessage = extra.statusMessage,
      isSyncing = extra.isSyncing
    )
  }.stateIn(
    scope = viewModelScope,
    started = SharingStarted.WhileSubscribed(5000L),
    initialValue = StudyUiState()
  )

  fun clearStatusMessage() {
    _statusMessage.value = null
  }

  fun completeOnboarding() {
    viewModelScope.launch {
      prefsManager.setOnboardingCompleted(true)
    }
  }

  fun enterGuestMode(profile: UserProfileEntity) {
    viewModelScope.launch {
      prefsManager.setGuestMode(true)
      prefsManager.setOnboardingCompleted(true)
      repo.saveProfile(profile)
    }
  }

  fun saveProfile(profile: UserProfileEntity) {
    viewModelScope.launch {
      repo.saveProfile(profile)
    }
  }

  fun setLanguage(lang: String) {
    viewModelScope.launch {
      prefsManager.setLanguage(lang)
      uiState.value.profile?.let { repo.saveProfile(it) }
    }
  }

  fun setThemeMode(mode: String) {
    viewModelScope.launch {
      prefsManager.setThemeMode(mode)
      uiState.value.profile?.let { repo.saveProfile(it) }
    }
  }

  fun setAccentTheme(accent: String) {
    viewModelScope.launch {
      prefsManager.setAccentTheme(accent)
      uiState.value.profile?.let { repo.saveProfile(it) }
    }
  }

  fun updateNotifications(
    context: Context,
    enabled: Boolean? = null,
    routine: Boolean? = null,
    exam: Boolean? = null,
    assignment: Boolean? = null,
    breaks: Boolean? = null,
    sound: String? = null
  ) {
    viewModelScope.launch {
      prefsManager.updateNotificationSettings(enabled, routine, exam, assignment, breaks, sound)
      val current = uiState.value.prefs
      val effectiveEnabled = enabled ?: current.notificationsEnabled
      StudyReminderWorker.scheduleDailyRoutineWorker(
        context,
        current.dailyReminderHour,
        current.dailyReminderMinute,
        effectiveEnabled && (routine ?: current.routineRemindersEnabled)
      )
    }
  }

  fun updateDashboardCards(quote: Boolean? = null, progress: Boolean? = null, upcoming: Boolean? = null) {
    viewModelScope.launch {
      prefsManager.updateDashboardCustomization(quote, progress, upcoming)
    }
  }

  // --- Subjects ---
  fun saveSubject(subject: SubjectEntity) {
    viewModelScope.launch {
      repo.saveSubject(subject)
    }
  }

  fun toggleSubjectChapter(subject: SubjectEntity, chapter: String) {
    viewModelScope.launch {
      val updatedCompleted = if (chapter in subject.completedChapters) {
        subject.completedChapters - chapter
      } else {
        subject.completedChapters + chapter
      }
      repo.saveSubject(subject.copy(completedChapters = updatedCompleted))
    }
  }

  fun deleteSubject(id: String) {
    viewModelScope.launch { repo.deleteSubject(id) }
  }

  // --- Routines ---
  fun saveRoutine(routine: RoutineEntity, context: Context? = null) {
    viewModelScope.launch {
      val nextOrder = if (routine.id.isBlank()) {
        (uiState.value.routines.maxOfOrNull { it.sortOrder } ?: -1) + 1
      } else {
        routine.sortOrder
      }
      val saved = routine.copy(sortOrder = nextOrder)
      repo.saveRoutine(saved)
      if (context != null && uiState.value.prefs.notificationsEnabled && uiState.value.prefs.routineRemindersEnabled) {
        val isBn = uiState.value.prefs.language == "bn"
        val title = if (isBn) "পড়াশোনার সময়: ${saved.subjectName}" else "Study Time: ${saved.subjectName}"
        val body = if (isBn) {
          "${saved.durationMinutes} মিনিটের সেশন নির্ধারিত আছে।"
        } else {
          "${saved.durationMinutes} min study session scheduled."
        }
        StudyReminderWorker.scheduleOneTimeReminder(
          context = context,
          uniqueId = "routine_${saved.id}",
          delayMinutes = 1L,
          title = title,
          body = body
        )
      }
    }
  }

  fun toggleRoutineCompleted(routine: RoutineEntity) {
    viewModelScope.launch {
      repo.saveRoutine(routine.copy(isCompleted = !routine.isCompleted))
    }
  }

  fun moveRoutine(fromIndex: Int, toIndex: Int) {
    val list = uiState.value.routines.toMutableList()
    if (fromIndex !in list.indices || toIndex !in list.indices || fromIndex == toIndex) return
    val item = list.removeAt(fromIndex)
    list.add(toIndex, item)
    viewModelScope.launch {
      repo.reorderRoutines(list)
    }
  }

  fun deleteRoutine(id: String) {
    viewModelScope.launch { repo.deleteRoutine(id) }
  }

  fun applyTemplate(template: RoutineTemplate) {
    viewModelScope.launch {
      repo.applyRoutineTemplate(template, uiState.value.prefs.language == "bn")
      _statusMessage.value = if (uiState.value.prefs.language == "bn") {
        "টেমপ্লেট রুটিনে যুক্ত হয়েছে!"
      } else {
        "Template added to your routine!"
      }
    }
  }

  // --- Tasks & Notes ---
  fun saveTask(task: StudyTaskEntity) {
    viewModelScope.launch { repo.saveTask(task) }
  }

  fun toggleTaskCompleted(task: StudyTaskEntity) {
    viewModelScope.launch { repo.saveTask(task.copy(isCompleted = !task.isCompleted)) }
  }

  fun deleteTask(id: String) {
    viewModelScope.launch { repo.deleteTask(id) }
  }

  fun saveNote(note: StudyNoteEntity) {
    viewModelScope.launch { repo.saveNote(note) }
  }

  fun toggleNotePin(note: StudyNoteEntity) {
    viewModelScope.launch { repo.saveNote(note.copy(isPinned = !note.isPinned)) }
  }

  fun deleteNote(id: String) {
    viewModelScope.launch { repo.deleteNote(id) }
  }

  // --- Exams ---
  fun saveExam(exam: ExamEntity) {
    viewModelScope.launch { repo.saveExam(exam) }
  }

  fun deleteExam(id: String) {
    viewModelScope.launch { repo.deleteExam(id) }
  }

  // --- Timer ---
  fun selectTimerMode(mode: String, customMinutes: Int? = null) {
    timerJob?.cancel()
    val mins = when (mode) {
      "pomodoro" -> 25
      "short_break" -> 5
      "long_break" -> 15
      else -> (customMinutes ?: _timerState.value.customMinutes).coerceIn(1, 180)
    }
    _timerState.value = _timerState.value.copy(
      mode = mode,
      customMinutes = if (mode == "custom") mins else _timerState.value.customMinutes,
      totalSeconds = mins * 60,
      remainingSeconds = mins * 60,
      isRunning = false
    )
  }

  fun selectTimerSubject(subject: SubjectEntity?) {
    _timerState.value = _timerState.value.copy(
      selectedSubjectId = subject?.id.orEmpty(),
      selectedSubjectName = subject?.name.orEmpty()
    )
  }

  fun startOrResumeTimer(context: Context) {
    if (_timerState.value.isRunning) return
    _timerState.value = _timerState.value.copy(isRunning = true)
    timerJob?.cancel()
    timerJob = viewModelScope.launch {
      while (_timerState.value.isRunning && _timerState.value.remainingSeconds > 0) {
        delay(1000L)
        val next = _timerState.value.remainingSeconds - 1
        _timerState.value = _timerState.value.copy(remainingSeconds = next)
        if (next == 0) {
          onTimerFinished(context)
        }
      }
    }
  }

  fun pauseTimer() {
    timerJob?.cancel()
    _timerState.value = _timerState.value.copy(isRunning = false)
  }

  fun resetTimer() {
    timerJob?.cancel()
    val state = _timerState.value
    _timerState.value = state.copy(
      remainingSeconds = state.totalSeconds,
      isRunning = false
    )
  }

  fun completeAndLogTimer(context: Context) {
    timerJob?.cancel()
    val state = _timerState.value
    val elapsedSeconds = (state.totalSeconds - state.remainingSeconds).coerceAtLeast(60)
    val loggedMinutes = (elapsedSeconds / 60).coerceAtLeast(1)
    viewModelScope.launch {
      logSessionInternal(loggedMinutes, state)
      notifyTimerComplete(context, state.mode)
      _timerState.value = state.copy(remainingSeconds = state.totalSeconds, isRunning = false)
    }
  }

  private suspend fun onTimerFinished(context: Context) {
    val state = _timerState.value
    val mins = (state.totalSeconds / 60).coerceAtLeast(1)
    logSessionInternal(mins, state)
    notifyTimerComplete(context, state.mode)
    _timerState.value = state.copy(isRunning = false, remainingSeconds = state.totalSeconds)
  }

  private suspend fun logSessionInternal(minutes: Int, state: TimerState) {
    val isBn = uiState.value.prefs.language == "bn"
    val fallbackSubject = if (isBn) "সাধারণ পড়াশোনা" else "General Study"
    repo.recordFocusSession(
      FocusSessionEntity(
        subjectId = state.selectedSubjectId,
        subjectName = state.selectedSubjectName.ifBlank { fallbackSubject },
        durationMinutes = minutes,
        mode = state.mode,
        dateKey = dateKeyFor(System.currentTimeMillis()),
        completedAtMillis = System.currentTimeMillis()
      )
    )
    _statusMessage.value = if (isBn) {
      "$minutes মিনিট ফোকাস সেশন সংরক্ষিত হয়েছে!"
    } else {
      "$minutes min focus session saved!"
    }
  }

  private fun notifyTimerComplete(context: Context, mode: String) {
    val prefs = uiState.value.prefs
    if (!prefs.notificationsEnabled) return
    if ((mode == "short_break" || mode == "long_break") && !prefs.breakRemindersEnabled) return
    val isBn = prefs.language == "bn"
    val title = if (mode == "pomodoro" || mode == "custom") {
      if (isBn) "স্টাডি সেশন সম্পন্ন!" else "Study Session Complete!"
    } else {
      if (isBn) "বিরতি শেষ! পড়াশোনায় ফিরুন" else "Break Over! Back to Study"
    }
    val body = if (mode == "pomodoro" || mode == "custom") {
      if (isBn) "দারুণ হয়েছে! এখন ৫ মিনিট বিরতি নিতে পারেন।" else "Great focus! Take a short 5-minute break."
    } else {
      if (isBn) "নতুন ফোকাস সেশন শুরু করার সময় হয়েছে।" else "Ready to start your next focus block."
    }
    NotificationHelper.showNotification(context, 2001, title, body, prefs.notificationSound)
  }

  // --- Cloud Backup & Restore ---
  fun backupToCloud() {
    viewModelScope.launch {
      _isSyncing.value = true
      val res = repo.backupAllToCloud()
      _isSyncing.value = false
      val isBn = uiState.value.prefs.language == "bn"
      _statusMessage.value = res.fold(
        onSuccess = { count ->
          if (isBn) "ক্লাউডে ${count}টি আইটেম সফলভাবে ব্যাকআপ হয়েছে!" else "Backed up $count items to Cloud Firestore!"
        },
        onFailure = { e ->
          e.localizedMessage ?: if (isBn) "ব্যাকআপ ব্যর্থ হয়েছে" else "Cloud backup failed"
        }
      )
    }
  }

  fun restoreFromCloud() {
    viewModelScope.launch {
      _isSyncing.value = true
      val res = repo.restoreAllFromCloud()
      _isSyncing.value = false
      val isBn = uiState.value.prefs.language == "bn"
      _statusMessage.value = res.fold(
        onSuccess = { count ->
          if (isBn) "ক্লাউড থেকে ${count}টি আইটেম রিস্টোর হয়েছে!" else "Restored $count items from Cloud Firestore!"
        },
        onFailure = { e ->
          e.localizedMessage ?: if (isBn) "রিস্টোর ব্যর্থ হয়েছে" else "Cloud restore failed"
        }
      )
    }
  }

  suspend fun exportJsonBackup(): String = repo.exportLocalBackupJson()

  fun onSignedOut() {
    viewModelScope.launch {
      repo.clearLocalUserOnSignOut()
    }
  }

  override fun onCleared() {
    super.onCleared()
    timerJob?.cancel()
  }

  private fun computeWeeklyStats(sessions: List<FocusSessionEntity>, isBn: Boolean): List<Pair<String, Int>> {
    val result = mutableListOf<Pair<String, Int>>()
    val cal = Calendar.getInstance()
    val fmt = SimpleDateFormat("EEE", if (isBn) Locale("bn", "BD") else Locale.ENGLISH)
    cal.add(Calendar.DAY_OF_YEAR, -6)
    repeat(7) {
      val key = dateKeyFor(cal.timeInMillis)
      val label = fmt.format(Date(cal.timeInMillis))
      val mins = sessions
        .filter { it.dateKey == key && (it.mode == "pomodoro" || it.mode == "custom") }
        .sumOf { it.durationMinutes }
      result.add(label to mins)
      cal.add(Calendar.DAY_OF_YEAR, 1)
    }
    return result
  }

  private fun computeMonthlyMinutes(sessions: List<FocusSessionEntity>): Int {
    val monthPrefix = SimpleDateFormat("yyyy-MM", Locale.US).format(Date())
    return sessions
      .filter { it.dateKey.startsWith(monthPrefix) && (it.mode == "pomodoro" || it.mode == "custom") }
      .sumOf { it.durationMinutes }
  }

  private fun computeStreak(sessions: List<FocusSessionEntity>, routines: List<RoutineEntity>): Int {
    val activeDates = sessions.map { it.dateKey }.toMutableSet()
    if (routines.any { it.isCompleted }) {
      activeDates.add(dateKeyFor(System.currentTimeMillis()))
    }
    var streak = 0
    val cal = Calendar.getInstance()
    while (true) {
      val key = dateKeyFor(cal.timeInMillis)
      if (key in activeDates) {
        streak++
        cal.add(Calendar.DAY_OF_YEAR, -1)
      } else {
        break
      }
    }
    return streak
  }

  private data class CoreTuple(
    val prefs: AppPreferences,
    val profile: UserProfileEntity?,
    val subjects: List<SubjectEntity>,
    val routines: List<RoutineEntity>,
    val tasks: List<StudyTaskEntity>
  )

  private data class ExtraTuple(
    val notes: List<StudyNoteEntity>,
    val exams: List<ExamEntity>,
    val sessions: List<FocusSessionEntity>,
    val statusMessage: String?,
    val isSyncing: Boolean
  )

  companion object {
    fun dateKeyFor(millis: Long): String =
      SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date(millis))

    fun provideFactory(context: Context): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
      @Suppress("UNCHECKED_CAST")
      override fun <T : ViewModel> create(modelClass: Class<T>): T {
        val appCtx = context.applicationContext
        val db = AppDatabase.getInstance(appCtx)
        val prefs = PreferencesManager(appCtx)
        val firestoreRepo = StudyFirestoreRepository(appCtx)
        val repo = StudyPlannerRepository(db.studyDao(), firestoreRepo, prefs, Firebase.auth)
        return StudyViewModel(repo, prefs) as T
      }
    }
  }
}

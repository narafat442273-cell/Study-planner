package com.example.ui.navigation

import android.Manifest
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.auth.attemptAutoSignIn
import com.example.data.auth.signOutUser
import com.example.ui.i18n.StudyStrings
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.CalendarScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.ExamPrepScreen
import com.example.ui.screens.MoreHubScreen
import com.example.ui.screens.NotesTasksScreen
import com.example.ui.screens.OnboardingScreen
import com.example.ui.screens.ProfileScreen
import com.example.ui.screens.ProgressReportScreen
import com.example.ui.screens.RoutineBuilderScreen
import com.example.ui.screens.RoutineEditorDialog
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.SplashScreen
import com.example.ui.screens.SubjectsScreen
import com.example.ui.screens.TimerScreen
import com.example.ui.theme.StudyPlannerTheme
import com.example.ui.viewmodel.StudyViewModel
import com.example.worker.StudyReminderWorker
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth

enum class AppRoute {
  SPLASH,
  ONBOARDING,
  AUTH,
  HOME,
  ROUTINE,
  TIMER,
  TASKS,
  MORE,
  SUBJECTS,
  CALENDAR,
  EXAMS,
  PROGRESS,
  PROFILE,
  SETTINGS
}

@Composable
fun StudyAppRoot() {
  val context = LocalContext.current
  val scope = rememberCoroutineScope()
  val credentialManager = remember { CredentialManager.create(context) }

  var currentUser by remember { mutableStateOf(Firebase.auth.currentUser) }
  DisposableEffect(Unit) {
    val listener = FirebaseAuth.AuthStateListener { auth ->
      currentUser = auth.currentUser
    }
    Firebase.auth.addAuthStateListener(listener)
    onDispose { Firebase.auth.removeAuthStateListener(listener) }
  }

  val viewModel: StudyViewModel = viewModel(
    key = currentUser?.uid ?: "guest",
    factory = StudyViewModel.provideFactory(context)
  )
  val state by viewModel.uiState.collectAsStateWithLifecycle()
  val timerState by viewModel.timerState.collectAsStateWithLifecycle()
  val strings = remember(state.prefs.language) { StudyStrings.forLang(state.prefs.language) }

  var splashFinished by remember { mutableStateOf(false) }
  var currentTab by remember { mutableStateOf(AppRoute.HOME) }
  var showQuickAddRoutine by remember { mutableStateOf(false) }
  val snackbarHostState = remember { SnackbarHostState() }

  val notificationPermissionLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.RequestPermission(),
    onResult = { }
  )

  LaunchedEffect(Unit) {
    attemptAutoSignIn(
      context = context,
      credentialManager = credentialManager,
      onAuthSuccess = { currentUser = Firebase.auth.currentUser },
      onUnauthenticated = { },
      scope = scope
    )
  }

  LaunchedEffect(state.statusMessage) {
    val msg = state.statusMessage
    if (!msg.isNullOrBlank()) {
      snackbarHostState.showSnackbar(msg)
      viewModel.clearStatusMessage()
    }
  }

  StudyPlannerTheme(
    themeMode = state.prefs.themeMode,
    accentTheme = state.prefs.accentTheme,
    isBengali = state.prefs.language != "en"
  ) {
    when {
      !splashFinished -> {
        SplashScreen(strings = strings, onFinished = { splashFinished = true })
      }
      !state.prefs.onboardingCompleted -> {
        OnboardingScreen(
          strings = strings,
          onLanguageChange = { viewModel.setLanguage(it) },
          onComplete = { viewModel.completeOnboarding() }
        )
      }
      currentUser == null && !state.prefs.isGuestMode -> {
        AuthScreen(
          strings = strings,
          initialProfile = state.profile,
          onLanguageChange = { viewModel.setLanguage(it) },
          onSaveProfileAndEnterGoogle = { profile ->
            viewModel.saveProfile(profile)
            currentUser = Firebase.auth.currentUser
          },
          onContinueAsGuest = { profile ->
            viewModel.enterGuestMode(profile)
          }
        )
      }
      else -> {
        // Request POST_NOTIFICATIONS smoothly once inside main workspace
        LaunchedEffect(Unit) {
          if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
          }
          StudyReminderWorker.scheduleDailyRoutineWorker(
            context,
            state.prefs.dailyReminderHour,
            state.prefs.dailyReminderMinute,
            state.prefs.notificationsEnabled && state.prefs.routineRemindersEnabled
          )
        }

        // Handle Back from secondary sub-screens cleanly
        if (currentTab in listOf(
            AppRoute.SUBJECTS,
            AppRoute.CALENDAR,
            AppRoute.EXAMS,
            AppRoute.PROGRESS,
            AppRoute.PROFILE,
            AppRoute.SETTINGS
          )
        ) {
          BackHandler { currentTab = AppRoute.MORE }
        } else if (currentTab != AppRoute.HOME) {
          BackHandler { currentTab = AppRoute.HOME }
        }

        val navItems = listOf(
          Triple(AppRoute.HOME, strings.navHome, Icons.Filled.Home),
          Triple(AppRoute.ROUTINE, strings.navRoutine, Icons.Filled.CalendarToday),
          Triple(AppRoute.TIMER, strings.navTimer, Icons.Filled.Timer),
          Triple(AppRoute.TASKS, strings.navTasks, Icons.AutoMirrored.Filled.Notes),
          Triple(AppRoute.MORE, strings.navMore, Icons.Filled.Apps)
        )

        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
          val isWide = maxWidth >= 600.dp

          Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) },
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            bottomBar = {
              if (!isWide) {
                NavigationBar {
                  navItems.forEach { (route, label, icon) ->
                    val selected = currentTab == route ||
                      (route == AppRoute.MORE && currentTab in listOf(
                        AppRoute.SUBJECTS,
                        AppRoute.CALENDAR,
                        AppRoute.EXAMS,
                        AppRoute.PROGRESS,
                        AppRoute.PROFILE,
                        AppRoute.SETTINGS
                      ))
                    NavigationBarItem(
                      selected = selected,
                      onClick = { currentTab = route },
                      icon = { Icon(icon, contentDescription = label) },
                      label = { Text(label, maxLines = 1) },
                      modifier = Modifier.testTag("nav_${route.name.lowercase()}")
                    )
                  }
                }
              }
            }
          ) { innerPadding ->
            Row(
              modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
            ) {
              if (isWide) {
                NavigationRail {
                  navItems.forEach { (route, label, icon) ->
                    NavigationRailItem(
                      selected = currentTab == route,
                      onClick = { currentTab = route },
                      icon = { Icon(icon, contentDescription = label) },
                      label = { Text(label) }
                    )
                  }
                }
              }

              Box(modifier = Modifier.weight(1f).fillMaxSize()) {
                when (currentTab) {
                  AppRoute.HOME -> DashboardScreen(
                    strings = strings,
                    state = state,
                    isGuestMode = currentUser == null,
                    onQuickAddRoutine = { showQuickAddRoutine = true },
                    onToggleRoutineComplete = { viewModel.toggleRoutineCompleted(it) },
                    onOpenRoutineBuilder = { currentTab = AppRoute.ROUTINE },
                    onOpenTimer = { currentTab = AppRoute.TIMER },
                    onOpenProfile = { currentTab = AppRoute.PROFILE }
                  )
                  AppRoute.ROUTINE -> RoutineBuilderScreen(
                    strings = strings,
                    studentName = state.profile?.displayName.orEmpty(),
                    routines = state.routines,
                    subjects = state.subjects,
                    onSaveRoutine = { viewModel.saveRoutine(it, context) },
                    onDeleteRoutine = { viewModel.deleteRoutine(it) },
                    onToggleCompleted = { viewModel.toggleRoutineCompleted(it) },
                    onMoveRoutine = { from, to -> viewModel.moveRoutine(from, to) },
                    onApplyTemplate = { viewModel.applyTemplate(it) }
                  )
                  AppRoute.TIMER -> TimerScreen(
                    strings = strings,
                    timerState = timerState,
                    subjects = state.subjects,
                    todayFocusMinutes = state.todayFocusMinutes,
                    recentSessions = state.focusSessions,
                    onSelectMode = { mode, custom -> viewModel.selectTimerMode(mode, custom) },
                    onSelectSubject = { viewModel.selectTimerSubject(it) },
                    onStartResume = { viewModel.startOrResumeTimer(context) },
                    onPause = { viewModel.pauseTimer() },
                    onReset = { viewModel.resetTimer() },
                    onCompleteAndLog = { viewModel.completeAndLogTimer(context) }
                  )
                  AppRoute.TASKS -> NotesTasksScreen(
                    strings = strings,
                    tasks = state.tasks,
                    notes = state.notes,
                    subjects = state.subjects,
                    onSaveTask = { viewModel.saveTask(it) },
                    onToggleTask = { viewModel.toggleTaskCompleted(it) },
                    onDeleteTask = { viewModel.deleteTask(it) },
                    onSaveNote = { viewModel.saveNote(it) },
                    onTogglePinNote = { viewModel.toggleNotePin(it) },
                    onDeleteNote = { viewModel.deleteNote(it) }
                  )
                  AppRoute.MORE -> MoreHubScreen(
                    strings = strings,
                    onNavigateSubjects = { currentTab = AppRoute.SUBJECTS },
                    onNavigateCalendar = { currentTab = AppRoute.CALENDAR },
                    onNavigateExams = { currentTab = AppRoute.EXAMS },
                    onNavigateProgress = { currentTab = AppRoute.PROGRESS },
                    onNavigateProfile = { currentTab = AppRoute.PROFILE },
                    onNavigateSettings = { currentTab = AppRoute.SETTINGS }
                  )
                  AppRoute.SUBJECTS -> SubjectsScreen(
                    strings = strings,
                    subjects = state.subjects,
                    onSaveSubject = { viewModel.saveSubject(it) },
                    onToggleChapter = { sub, ch -> viewModel.toggleSubjectChapter(sub, ch) },
                    onDeleteSubject = { viewModel.deleteSubject(it) }
                  )
                  AppRoute.CALENDAR -> CalendarScreen(
                    strings = strings,
                    routines = state.routines,
                    exams = state.exams,
                    tasks = state.tasks
                  )
                  AppRoute.EXAMS -> ExamPrepScreen(
                    strings = strings,
                    exams = state.exams,
                    subjects = state.subjects,
                    onSaveExam = { viewModel.saveExam(it) },
                    onDeleteExam = { viewModel.deleteExam(it) }
                  )
                  AppRoute.PROGRESS -> ProgressReportScreen(
                    strings = strings,
                    state = state
                  )
                  AppRoute.PROFILE -> ProfileScreen(
                    strings = strings,
                    profile = state.profile,
                    isGuestMode = currentUser == null,
                    onSaveProfile = { viewModel.saveProfile(it) },
                    onGoogleConnected = { profile ->
                      viewModel.saveProfile(profile)
                      currentUser = Firebase.auth.currentUser
                    }
                  )
                  AppRoute.SETTINGS -> SettingsScreen(
                    strings = strings,
                    state = state,
                    onLanguageChange = { viewModel.setLanguage(it) },
                    onThemeModeChange = { viewModel.setThemeMode(it) },
                    onAccentChange = { viewModel.setAccentTheme(it) },
                    onUpdateNotifications = { en, rt, ex, asg, br, snd ->
                      viewModel.updateNotifications(context, en, rt, ex, asg, br, snd)
                    },
                    onUpdateDashboard = { q, p, u ->
                      viewModel.updateDashboardCards(q, p, u)
                    },
                    onBackupNow = { viewModel.backupToCloud() },
                    onRestoreFromCloud = { viewModel.restoreFromCloud() },
                    onExportBackupJson = { viewModel.exportJsonBackup() },
                    onSignOut = {
                      viewModel.onSignedOut()
                      signOutUser(credentialManager, scope) {
                        currentUser = null
                        currentTab = AppRoute.HOME
                      }
                    }
                  )
                  else -> Unit
                }
              }
            }
          }

          if (showQuickAddRoutine) {
            RoutineEditorDialog(
              strings = strings,
              existing = null,
              subjects = state.subjects,
              onDismiss = { showQuickAddRoutine = false },
              onConfirm = { routine ->
                viewModel.saveRoutine(routine, context)
                showQuickAddRoutine = false
              }
            )
          }
        }
      }
    }
  }
}

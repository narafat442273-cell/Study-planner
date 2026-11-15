package com.example.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "study_planner_prefs")

data class AppPreferences(
  val onboardingCompleted: Boolean = false,
  val isGuestMode: Boolean = false,
  val language: String = "bn", // Bengali is default
  val themeMode: String = "system", // system, light, dark
  val accentTheme: String = "indigo", // indigo, emerald, amber, rose
  val notificationsEnabled: Boolean = true,
  val routineRemindersEnabled: Boolean = true,
  val examRemindersEnabled: Boolean = true,
  val assignmentRemindersEnabled: Boolean = true,
  val breakRemindersEnabled: Boolean = true,
  val notificationSound: String = "default", // default, gentle_chime, silent
  val showQuoteCard: Boolean = true,
  val showProgressCard: Boolean = true,
  val showUpcomingCard: Boolean = true,
  val dailyReminderHour: Int = 8,
  val dailyReminderMinute: Int = 0
)

class PreferencesManager(private val context: Context) {
  private object Keys {
    val ONBOARDING_DONE = booleanPreferencesKey("onboarding_done")
    val IS_GUEST = booleanPreferencesKey("is_guest")
    val LANGUAGE = stringPreferencesKey("language")
    val THEME_MODE = stringPreferencesKey("theme_mode")
    val ACCENT_THEME = stringPreferencesKey("accent_theme")
    val NOTIFICATIONS_ENABLED = booleanPreferencesKey("notifications_enabled")
    val ROUTINE_REMINDERS = booleanPreferencesKey("routine_reminders")
    val EXAM_REMINDERS = booleanPreferencesKey("exam_reminders")
    val ASSIGNMENT_REMINDERS = booleanPreferencesKey("assignment_reminders")
    val BREAK_REMINDERS = booleanPreferencesKey("break_reminders")
    val NOTIFICATION_SOUND = stringPreferencesKey("notification_sound")
    val SHOW_QUOTE = booleanPreferencesKey("show_quote")
    val SHOW_PROGRESS = booleanPreferencesKey("show_progress")
    val SHOW_UPCOMING = booleanPreferencesKey("show_upcoming")
    val REMINDER_HOUR = intPreferencesKey("reminder_hour")
    val REMINDER_MINUTE = intPreferencesKey("reminder_minute")
  }

  val preferencesFlow: Flow<AppPreferences> = context.dataStore.data.map { prefs ->
    AppPreferences(
      onboardingCompleted = prefs[Keys.ONBOARDING_DONE] ?: false,
      isGuestMode = prefs[Keys.IS_GUEST] ?: false,
      language = prefs[Keys.LANGUAGE] ?: "bn",
      themeMode = prefs[Keys.THEME_MODE] ?: "system",
      accentTheme = prefs[Keys.ACCENT_THEME] ?: "indigo",
      notificationsEnabled = prefs[Keys.NOTIFICATIONS_ENABLED] ?: true,
      routineRemindersEnabled = prefs[Keys.ROUTINE_REMINDERS] ?: true,
      examRemindersEnabled = prefs[Keys.EXAM_REMINDERS] ?: true,
      assignmentRemindersEnabled = prefs[Keys.ASSIGNMENT_REMINDERS] ?: true,
      breakRemindersEnabled = prefs[Keys.BREAK_REMINDERS] ?: true,
      notificationSound = prefs[Keys.NOTIFICATION_SOUND] ?: "default",
      showQuoteCard = prefs[Keys.SHOW_QUOTE] ?: true,
      showProgressCard = prefs[Keys.SHOW_PROGRESS] ?: true,
      showUpcomingCard = prefs[Keys.SHOW_UPCOMING] ?: true,
      dailyReminderHour = prefs[Keys.REMINDER_HOUR] ?: 8,
      dailyReminderMinute = prefs[Keys.REMINDER_MINUTE] ?: 0
    )
  }

  suspend fun setOnboardingCompleted(completed: Boolean) {
    context.dataStore.edit { it[Keys.ONBOARDING_DONE] = completed }
  }

  suspend fun setGuestMode(isGuest: Boolean) {
    context.dataStore.edit { it[Keys.IS_GUEST] = isGuest }
  }

  suspend fun setLanguage(language: String) {
    context.dataStore.edit { it[Keys.LANGUAGE] = language }
  }

  suspend fun setThemeMode(mode: String) {
    context.dataStore.edit { it[Keys.THEME_MODE] = mode }
  }

  suspend fun setAccentTheme(accent: String) {
    context.dataStore.edit { it[Keys.ACCENT_THEME] = accent }
  }

  suspend fun updateNotificationSettings(
    enabled: Boolean? = null,
    routine: Boolean? = null,
    exam: Boolean? = null,
    assignment: Boolean? = null,
    breaks: Boolean? = null,
    sound: String? = null,
    hour: Int? = null,
    minute: Int? = null
  ) {
    context.dataStore.edit { prefs ->
      enabled?.let { prefs[Keys.NOTIFICATIONS_ENABLED] = it }
      routine?.let { prefs[Keys.ROUTINE_REMINDERS] = it }
      exam?.let { prefs[Keys.EXAM_REMINDERS] = it }
      assignment?.let { prefs[Keys.ASSIGNMENT_REMINDERS] = it }
      breaks?.let { prefs[Keys.BREAK_REMINDERS] = it }
      sound?.let { prefs[Keys.NOTIFICATION_SOUND] = it }
      hour?.let { prefs[Keys.REMINDER_HOUR] = it }
      minute?.let { prefs[Keys.REMINDER_MINUTE] = it }
    }
  }

  suspend fun updateDashboardCustomization(
    showQuote: Boolean? = null,
    showProgress: Boolean? = null,
    showUpcoming: Boolean? = null
  ) {
    context.dataStore.edit { prefs ->
      showQuote?.let { prefs[Keys.SHOW_QUOTE] = it }
      showProgress?.let { prefs[Keys.SHOW_PROGRESS] = it }
      showUpcoming?.let { prefs[Keys.SHOW_UPCOMING] = it }
    }
  }
}

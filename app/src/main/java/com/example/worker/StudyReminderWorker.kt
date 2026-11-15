package com.example.worker

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.example.MainActivity
import com.example.R
import com.example.data.local.AppDatabase
import com.example.data.local.PreferencesManager
import kotlinx.coroutines.flow.first
import java.util.Calendar
import java.util.concurrent.TimeUnit

object NotificationHelper {
  const val CHANNEL_STUDY_ID = "study_reminders_channel"
  const val CHANNEL_SILENT_ID = "study_reminders_silent"

  fun ensureChannels(context: Context) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

      val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
      val audioAttrs = AudioAttributes.Builder()
        .setUsage(AudioAttributes.USAGE_NOTIFICATION)
        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
        .build()

      val defaultChannel = NotificationChannel(
        CHANNEL_STUDY_ID,
        "Study & Exam Reminders",
        NotificationManager.IMPORTANCE_HIGH
      ).apply {
        description = "Study routine, break, assignment deadline, and exam reminders"
        setSound(soundUri, audioAttrs)
        enableVibration(true)
      }

      val silentChannel = NotificationChannel(
        CHANNEL_SILENT_ID,
        "Silent Study Reminders",
        NotificationManager.IMPORTANCE_DEFAULT
      ).apply {
        description = "Gentle visual reminders without notification sound"
        setSound(null, null)
        enableVibration(false)
      }

      manager.createNotificationChannel(defaultChannel)
      manager.createNotificationChannel(silentChannel)
    }
  }

  fun showNotification(
    context: Context,
    notificationId: Int,
    title: String,
    message: String,
    soundMode: String = "default"
  ) {
    ensureChannels(context)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
      val granted = ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.POST_NOTIFICATIONS
      ) == PackageManager.PERMISSION_GRANTED
      if (!granted) return
    }

    val intent = Intent(context, MainActivity::class.java).apply {
      flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
    }
    val pendingIntent = PendingIntent.getActivity(
      context,
      notificationId,
      intent,
      PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    val channelId = if (soundMode == "silent") CHANNEL_SILENT_ID else CHANNEL_STUDY_ID
    val soundUri = when (soundMode) {
      "silent" -> null
      "gentle_chime" -> RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
        ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
      else -> RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
    }

    val builder = NotificationCompat.Builder(context, channelId)
      .setSmallIcon(R.mipmap.ic_launcher)
      .setContentTitle(title)
      .setContentText(message)
      .setStyle(NotificationCompat.BigTextStyle().bigText(message))
      .setPriority(NotificationCompat.PRIORITY_HIGH)
      .setAutoCancel(true)
      .setContentIntent(pendingIntent)

    if (soundUri != null) {
      builder.setSound(soundUri)
    } else {
      builder.setSilent(true)
    }

    NotificationManagerCompat.from(context).notify(notificationId, builder.build())
  }
}

class StudyReminderWorker(
  appContext: Context,
  params: WorkerParameters
) : CoroutineWorker(appContext, params) {

  override suspend fun doWork(): Result {
    val prefs = PreferencesManager(applicationContext).preferencesFlow.first()
    if (!prefs.notificationsEnabled) return Result.success()

    val isBn = prefs.language == "bn"
    val kind = inputData.getString(KEY_KIND) ?: KIND_DAILY_CHECK
    val customTitle = inputData.getString(KEY_TITLE)
    val customBody = inputData.getString(KEY_BODY)

    if (!customTitle.isNullOrBlank() && !customBody.isNullOrBlank()) {
      NotificationHelper.showNotification(
        applicationContext,
        notificationId = (System.currentTimeMillis() % 100000).toInt(),
        title = customTitle,
        message = customBody,
        soundMode = prefs.notificationSound
      )
      return Result.success()
    }

    val dao = AppDatabase.getInstance(applicationContext).studyDao()
    val routines = dao.getRoutinesOnce()
    val tasks = dao.getTasksOnce()
    val exams = dao.getExamsOnce()

    val now = System.currentTimeMillis()
    val pendingTasks = tasks.count { !it.isCompleted }
    val upcomingExam = exams.firstOrNull { it.examDateMillis >= now }

    if (kind == KIND_DAILY_CHECK && prefs.routineRemindersEnabled) {
      val title = if (isBn) "আজকের পড়াশোনার রুটিন" else "Today's Study Routine"
      val body = if (isBn) {
        "আজ আপনার ${routines.size}টি সেশন ও ${pendingTasks}টি কাজ বাকি আছে। মনোযোগ দিয়ে শুরু করুন!"
      } else {
        "You have ${routines.size} study sessions and $pendingTasks pending tasks today. Let's focus!"
      }
      NotificationHelper.showNotification(applicationContext, 1001, title, body, prefs.notificationSound)
    }

    if (prefs.examRemindersEnabled && upcomingExam != null) {
      val daysLeft = ((upcomingExam.examDateMillis - now) / (1000L * 60 * 60 * 24)).coerceAtLeast(0L)
      if (daysLeft <= 7L) {
        val title = if (isBn) "পরীক্ষার রিমাইন্ডার: ${upcomingExam.title}" else "Exam Reminder: ${upcomingExam.title}"
        val body = if (isBn) {
          "${upcomingExam.subjectName} পরীক্ষা আর মাত্র $daysLeft দিন বাকি।"
        } else {
          "${upcomingExam.subjectName} exam is in $daysLeft days."
        }
        NotificationHelper.showNotification(applicationContext, 1002, title, body, prefs.notificationSound)
      }
    }

    if (prefs.assignmentRemindersEnabled) {
      val dueSoon = tasks.firstOrNull { !it.isCompleted && it.deadlineMillis in now..(now + 86_400_000L) }
      if (dueSoon != null) {
        val title = if (isBn) "অ্যাসাইনমেন্ট ডেডলাইন" else "Assignment Deadline"
        val body = if (isBn) {
          "\"${dueSoon.title}\" আগামী ২৪ ঘণ্টার মধ্যে জমা দিতে হবে।"
        } else {
          "\"${dueSoon.title}\" is due within 24 hours."
        }
        NotificationHelper.showNotification(applicationContext, 1003, title, body, prefs.notificationSound)
      }
    }

    return Result.success()
  }

  companion object {
    const val KEY_KIND = "kind"
    const val KEY_TITLE = "title"
    const val KEY_BODY = "body"
    const val KIND_DAILY_CHECK = "daily_check"
    const val KIND_SESSION = "session_reminder"
    const val KIND_BREAK = "break_reminder"

    private const val DAILY_WORK_NAME = "study_planner_daily_reminder"

    fun scheduleDailyRoutineWorker(context: Context, hour: Int, minute: Int, enabled: Boolean) {
      val wm = WorkManager.getInstance(context.applicationContext)
      if (!enabled) {
        wm.cancelUniqueWork(DAILY_WORK_NAME)
        return
      }
      val now = Calendar.getInstance()
      val target = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, hour.coerceIn(0, 23))
        set(Calendar.MINUTE, minute.coerceIn(0, 59))
        set(Calendar.SECOND, 0)
        if (before(now)) add(Calendar.DAY_OF_YEAR, 1)
      }
      val initialDelayMinutes = ((target.timeInMillis - now.timeInMillis) / 60_000L).coerceAtLeast(1L)

      val request = PeriodicWorkRequestBuilder<StudyReminderWorker>(24, TimeUnit.HOURS)
        .setInitialDelay(initialDelayMinutes, TimeUnit.MINUTES)
        .setInputData(workDataOf(KEY_KIND to KIND_DAILY_CHECK))
        .build()

      wm.enqueueUniquePeriodicWork(
        DAILY_WORK_NAME,
        ExistingPeriodicWorkPolicy.UPDATE,
        request
      )
    }

    fun scheduleOneTimeReminder(
      context: Context,
      uniqueId: String,
      delayMinutes: Long,
      title: String,
      body: String,
      kind: String = KIND_SESSION
    ) {
      val wm = WorkManager.getInstance(context.applicationContext)
      val request = OneTimeWorkRequestBuilder<StudyReminderWorker>()
        .setInitialDelay(delayMinutes.coerceAtLeast(0L), TimeUnit.MINUTES)
        .setInputData(
          workDataOf(
            KEY_KIND to kind,
            KEY_TITLE to title,
            KEY_BODY to body
          )
        )
        .build()
      wm.enqueueUniqueWork(uniqueId, ExistingWorkPolicy.REPLACE, request)
    }
  }
}

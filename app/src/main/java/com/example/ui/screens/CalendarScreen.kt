package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.data.local.ExamEntity
import com.example.data.local.RoutineEntity
import com.example.data.local.StudyTaskEntity
import com.example.data.repository.StudyPlannerRepository
import com.example.ui.i18n.AppStrings
import com.example.util.PdfRoutineExporter
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun CalendarScreen(
  strings: AppStrings,
  routines: List<RoutineEntity>,
  exams: List<ExamEntity>,
  tasks: List<StudyTaskEntity>
) {
  var viewMode by remember { mutableStateOf("monthly") } // daily, weekly, monthly
  var selectedDayMillis by remember { mutableLongStateOf(StudyPlannerRepository.startOfDayMillis(System.currentTimeMillis())) }

  val selectedCal = remember(selectedDayMillis) {
    Calendar.getInstance().apply { timeInMillis = selectedDayMillis }
  }
  val locale = if (strings.isBengali) Locale("bn", "BD") else Locale.ENGLISH
  val monthTitle = SimpleDateFormat("MMMM yyyy", locale).format(Date(selectedDayMillis))

  val dayStart = StudyPlannerRepository.startOfDayMillis(selectedDayMillis)
  val dayEnd = dayStart + 86_400_000L

  val matchingRoutines = routines.filter { r ->
    when (r.repeatMode) {
      "daily" -> true
      "weekly" -> {
        val rCal = Calendar.getInstance().apply { timeInMillis = r.dateMillis }
        rCal.get(Calendar.DAY_OF_WEEK) == selectedCal.get(Calendar.DAY_OF_WEEK)
      }
      "custom" -> {
        val isoDay = ((selectedCal.get(Calendar.DAY_OF_WEEK) + 5) % 7) + 1
        isoDay in r.repeatDays
      }
      else -> r.dateMillis in dayStart until dayEnd
    }
  }

  val dayExams = exams.filter { exam ->
    when (viewMode) {
      "daily" -> exam.examDateMillis in dayStart until dayEnd
      "weekly" -> exam.examDateMillis in dayStart until (dayStart + 7 * 86_400_000L)
      else -> {
        val c = Calendar.getInstance().apply { timeInMillis = exam.examDateMillis }
        c.get(Calendar.MONTH) == selectedCal.get(Calendar.MONTH) && c.get(Calendar.YEAR) == selectedCal.get(Calendar.YEAR)
      }
    }
  }

  val dayDeadlines = tasks.filter { task ->
    when (viewMode) {
      "daily" -> task.deadlineMillis in dayStart until dayEnd
      "weekly" -> task.deadlineMillis in dayStart until (dayStart + 7 * 86_400_000L)
      else -> {
        val c = Calendar.getInstance().apply { timeInMillis = task.deadlineMillis }
        c.get(Calendar.MONTH) == selectedCal.get(Calendar.MONTH) && c.get(Calendar.YEAR) == selectedCal.get(Calendar.YEAR)
      }
    }
  }

  LazyColumn(
    modifier = Modifier.fillMaxSize(),
    contentPadding = PaddingValues(16.dp),
    verticalArrangement = Arrangement.spacedBy(14.dp)
  ) {
    item {
      Text(strings.calendarTitle, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
    }

    item {
      Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf(
          "daily" to strings.viewDaily,
          "weekly" to strings.viewWeekly,
          "monthly" to strings.viewMonthly
        ).forEach { (key, label) ->
          FilterChip(
            selected = viewMode == key,
            onClick = { viewMode = key },
            label = { Text(label) }
          )
        }
      }
    }

    // Month / Week Header + Grid
    item {
      Card(shape = RoundedCornerShape(20.dp), elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            IconButton(onClick = {
              val c = Calendar.getInstance().apply {
                timeInMillis = selectedDayMillis
                add(if (viewMode == "monthly") Calendar.MONTH else Calendar.WEEK_OF_YEAR, -1)
              }
              selectedDayMillis = StudyPlannerRepository.startOfDayMillis(c.timeInMillis)
            }) {
              Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Previous")
            }
            Text(monthTitle, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            IconButton(onClick = {
              val c = Calendar.getInstance().apply {
                timeInMillis = selectedDayMillis
                add(if (viewMode == "monthly") Calendar.MONTH else Calendar.WEEK_OF_YEAR, 1)
              }
              selectedDayMillis = StudyPlannerRepository.startOfDayMillis(c.timeInMillis)
            }) {
              Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Next")
            }
          }

          if (viewMode == "monthly") {
            MonthGrid(
              selectedDayMillis = selectedDayMillis,
              exams = exams,
              tasks = tasks,
              onSelectDay = { selectedDayMillis = it }
            )
          } else {
            WeekStrip(
              selectedDayMillis = selectedDayMillis,
              locale = locale,
              onSelectDay = { selectedDayMillis = it }
            )
          }
        }
      }
    }

    // Exam Schedule
    item {
      Text(
        text = if (strings.isBengali) "পরীক্ষার সময়সূচি (${dayExams.size})" else "Exam Schedule (${dayExams.size})",
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold
      )
    }
    items(dayExams, key = { "ex_${it.id}" }) { exam ->
      Card(shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
        Row(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
          Icon(Icons.Filled.Event, contentDescription = null, tint = MaterialTheme.colorScheme.onErrorContainer)
          Column {
            Text(exam.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onErrorContainer)
            Text(
              "${exam.subjectName} • ${SimpleDateFormat("dd MMM yyyy", locale).format(Date(exam.examDateMillis))}",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onErrorContainer
            )
          }
        }
      }
    }

    // Assignment Deadlines
    item {
      Text(
        text = if (strings.isBengali) "অ্যাসাইনমেন্ট ও কাজের ডেডলাইন (${dayDeadlines.size})" else "Assignment Deadlines (${dayDeadlines.size})",
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold
      )
    }
    items(dayDeadlines, key = { "tk_${it.id}" }) { task ->
      Card(shape = RoundedCornerShape(14.dp)) {
        Row(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
          Icon(Icons.Filled.Assignment, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary)
          Column {
            Text(task.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            Text(
              "${task.subjectName} • ${SimpleDateFormat("dd MMM", locale).format(Date(task.deadlineMillis))}",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
        }
      }
    }

    // Study Routine Overview
    item {
      Text(
        text = "${strings.todaysRoutine} (${matchingRoutines.size})",
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold
      )
    }
    items(matchingRoutines, key = { "rt_${it.id}" }) { routine ->
      Card(shape = RoundedCornerShape(14.dp)) {
        Row(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
          Icon(Icons.Filled.Schedule, contentDescription = null, tint = Color(routine.colorHex))
          Column {
            Text(routine.subjectName, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            Text(
              "${PdfRoutineExporter.formatTime(routine.startTimeMinutes)} - ${PdfRoutineExporter.formatTime(routine.endTimeMinutes)}",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
        }
      }
    }
  }
}

@Composable
private fun MonthGrid(
  selectedDayMillis: Long,
  exams: List<ExamEntity>,
  tasks: List<StudyTaskEntity>,
  onSelectDay: (Long) -> Unit
) {
  val cal = Calendar.getInstance().apply {
    timeInMillis = selectedDayMillis
    set(Calendar.DAY_OF_MONTH, 1)
  }
  val daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
  val firstDayOffset = (cal.get(Calendar.DAY_OF_WEEK) - 1)
  val selectedDay = Calendar.getInstance().apply { timeInMillis = selectedDayMillis }.get(Calendar.DAY_OF_MONTH)

  val cells = List(firstDayOffset) { null } + (1..daysInMonth).toList()
  val rows = cells.chunked(7)

  Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
    Row(modifier = Modifier.fillMaxWidth()) {
      listOf("S", "M", "T", "W", "T", "F", "S").forEach { d ->
        Text(
          text = d,
          modifier = Modifier.weight(1f),
          textAlign = TextAlign.Center,
          style = MaterialTheme.typography.labelSmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
      }
    }
    rows.forEach { week ->
      Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        for (i in 0 until 7) {
          val dayNum = week.getOrNull(i)
          if (dayNum == null) {
            Spacer(modifier = Modifier.weight(1f).aspectRatio(1f))
          } else {
            val dayCal = Calendar.getInstance().apply {
              timeInMillis = selectedDayMillis
              set(Calendar.DAY_OF_MONTH, dayNum)
            }
            val start = StudyPlannerRepository.startOfDayMillis(dayCal.timeInMillis)
            val end = start + 86_400_000L
            val hasEvent = exams.any { it.examDateMillis in start until end } ||
              tasks.any { it.deadlineMillis in start until end }
            val isSelected = dayNum == selectedDay

            Box(
              modifier = Modifier
                .weight(1f)
                .aspectRatio(1f)
                .clip(CircleShape)
                .background(if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent)
                .clickable { onSelectDay(start) },
              contentAlignment = Alignment.Center
            ) {
              Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                  text = "$dayNum",
                  style = MaterialTheme.typography.bodySmall,
                  fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                  color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                )
                if (hasEvent) {
                  Box(
                    modifier = Modifier
                      .size(5.dp)
                      .clip(CircleShape)
                      .background(if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.tertiary)
                  )
                }
              }
            }
          }
        }
      }
    }
  }
}

@Composable
private fun WeekStrip(
  selectedDayMillis: Long,
  locale: Locale,
  onSelectDay: (Long) -> Unit
) {
  val baseCal = Calendar.getInstance().apply {
    timeInMillis = selectedDayMillis
    set(Calendar.DAY_OF_WEEK, firstDayOfWeek)
  }
  Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
    repeat(7) { offset ->
      val dayCal = Calendar.getInstance().apply {
        timeInMillis = baseCal.timeInMillis
        add(Calendar.DAY_OF_YEAR, offset)
      }
      val start = StudyPlannerRepository.startOfDayMillis(dayCal.timeInMillis)
      val isSelected = start == StudyPlannerRepository.startOfDayMillis(selectedDayMillis)
      Surface(
        shape = RoundedCornerShape(12.dp),
        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier
          .weight(1f)
          .clickable { onSelectDay(start) }
      ) {
        Column(
          modifier = Modifier.padding(vertical = 8.dp),
          horizontalAlignment = Alignment.CenterHorizontally
        ) {
          Text(
            text = SimpleDateFormat("EEE", locale).format(Date(start)),
            style = MaterialTheme.typography.labelSmall,
            color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
          )
          Text(
            text = "${dayCal.get(Calendar.DAY_OF_MONTH)}",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
          )
        }
      }
    }
  }
}
